package com.zyntral.modules.auth.application;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.common.event.UserEmailVerifiedEvent;
import com.zyntral.common.security.JwtProperties;
import com.zyntral.common.security.JwtService;
import org.springframework.context.ApplicationEventPublisher;
import com.zyntral.modules.auth.domain.AuthToken;
import com.zyntral.modules.auth.domain.AuthTokenRepository;
import com.zyntral.modules.auth.domain.RefreshToken;
import com.zyntral.modules.auth.domain.RefreshTokenRepository;
import com.zyntral.modules.auth.web.dto.AuthDtos.*;
import com.zyntral.modules.user.domain.Role;
import com.zyntral.modules.user.domain.RoleRepository;
import com.zyntral.modules.user.domain.User;
import com.zyntral.modules.user.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Auth use-cases: register, login, token refresh (with rotation), logout, email
 * verification, and password reset. Access tokens are short-lived JWTs; refresh tokens
 * are opaque, hashed, rotated, and revocable.
 */
@Service
public class AuthService {

    private static final String ROLE_USER = "USER";

    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
    private final AuthTokenRepository authTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProps;
    private final TokenGenerator tokens;
    private final EmailSender email;
    private final ApplicationEventPublisher events;

    public AuthService(UserRepository users, RoleRepository roles,
                       RefreshTokenRepository refreshTokens, AuthTokenRepository authTokens,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       JwtProperties jwtProps, TokenGenerator tokens, EmailSender email,
                       ApplicationEventPublisher events) {
        this.users = users;
        this.roles = roles;
        this.refreshTokens = refreshTokens;
        this.authTokens = authTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProps = jwtProps;
        this.tokens = tokens;
        this.email = email;
        this.events = events;
    }

    @Transactional
    public void register(RegisterRequest req) {
        String email = User.normalizeEmail(req.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }
        User user = User.createLocal(email,
                passwordEncoder.encode(req.password()), req.fullName(), req.locale());
        Role userRole = roles.findByCode(ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("USER role missing — check seed data"));
        user.addRole(userRole);
        users.save(user);

        sendVerification(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req, String userAgent, String ip) {
        User user = users.findByEmail(User.normalizeEmail(req.email()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (user.getStatus() == com.zyntral.modules.user.domain.UserStatus.SUSPENDED
                || user.getStatus() == com.zyntral.modules.user.domain.UserStatus.DELETED) {
            throw new ApiException(ErrorCode.FORBIDDEN);   // banned / suspended / deleted
        }
        user.recordLogin();
        return issueTokens(user, userAgent, ip);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken, String userAgent, String ip) {
        String hash = tokens.hash(rawRefreshToken);
        RefreshToken current = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));

        if (!current.isActive()) {
            // reuse of a revoked/expired token → revoke the whole family (theft response)
            refreshTokens.revokeAllForUser(current.getUserId(), Instant.now());
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        }
        User user = users.findById(current.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));

        TokenResponse response = issueTokens(user, userAgent, ip);
        // link rotation: revoke old, point at successor
        current.revoke();
        return response;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.findByTokenHash(tokens.hash(rawRefreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        AuthToken token = consume(rawToken, AuthToken.Purpose.EMAIL_VERIFY);
        User user = users.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
        user.markEmailVerified();
        events.publishEvent(new UserEmailVerifiedEvent(
                user.getId(), user.getEmail(), user.getFullName(), user.getLocale()));
    }

    /** Always returns silently — never reveals whether an email exists. */
    @Transactional
    public void requestPasswordReset(String emailAddr) {
        users.findByEmail(User.normalizeEmail(emailAddr)).ifPresent(user -> {
            var secret = tokens.generate();
            authTokens.save(AuthToken.create(user.getId(), secret.hash(),
                    AuthToken.Purpose.PASSWORD_RESET, Instant.now().plusSeconds(3600)));
            email.sendPasswordReset(user.getEmail(), user.getFullName(), secret.raw());
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        AuthToken token = consume(rawToken, AuthToken.Purpose.PASSWORD_RESET);
        User user = users.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
        user.changePassword(passwordEncoder.encode(newPassword));
        // invalidate all sessions after a password change
        refreshTokens.revokeAllForUser(user.getId(), Instant.now());
    }

    // ---- helpers ----

    private AuthToken consume(String rawToken, AuthToken.Purpose purpose) {
        AuthToken token = authTokens
                .findByTokenHashAndPurpose(tokens.hash(rawToken), purpose.name())
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
        if (!token.isUsable()) {
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        }
        token.consume();
        return token;
    }

    private void sendVerification(User user) {
        var secret = tokens.generate();
        authTokens.save(AuthToken.create(user.getId(), secret.hash(),
                AuthToken.Purpose.EMAIL_VERIFY, Instant.now().plusSeconds(86_400)));
        email.sendEmailVerification(user.getEmail(), user.getFullName(), secret.raw());
    }

    private TokenResponse issueTokens(User user, String userAgent, String ip) {
        List<String> roleCodes = user.getRoles().stream().map(Role::getCode).toList();
        String accessToken = jwtService.issueAccessToken(user.getId(), user.getEmail(), roleCodes);

        var secret = tokens.generate();
        Instant expiresAt = Instant.now().plus(jwtProps.refreshTokenTtl());
        RefreshToken rt = RefreshToken.issue(user.getId(), secret.hash(), expiresAt, userAgent, ip);
        refreshTokens.save(rt);

        return new TokenResponse(
                accessToken,
                secret.raw(),
                "Bearer",
                jwtProps.accessTokenTtl().getSeconds(),
                new UserSummary(user.getId(), user.getEmail(), user.getFullName(),
                        user.isEmailVerified(), roleCodes)
        );
    }
}
