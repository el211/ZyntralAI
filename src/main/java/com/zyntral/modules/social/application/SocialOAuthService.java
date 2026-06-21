package com.zyntral.modules.social.application;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.social.application.SocialOAuthProvider.OAuthResult;
import com.zyntral.modules.social.config.SocialOAuthProperties;
import com.zyntral.modules.social.domain.SocialPlatform;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import com.zyntral.modules.workspace.domain.WorkspaceRole;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the OAuth connect flow. The CSRF/binding {@code state} token is stored server-side in
 * Redis (single-use, 10-min TTL) mapping to the initiating workspace+user — so the public
 * callback can be trusted without a JWT.
 */
@Service
public class SocialOAuthService {

    private static final String STATE_PREFIX = "social_oauth:";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SocialOAuthRegistry registry;
    private final SocialOAuthProperties props;
    private final RedisTemplate<String, Object> redis;
    private final WorkspaceAccess access;
    private final SocialAccountService accounts;

    public SocialOAuthService(SocialOAuthRegistry registry, SocialOAuthProperties props,
                              RedisTemplate<String, Object> redis, WorkspaceAccess access,
                              SocialAccountService accounts) {
        this.registry = registry;
        this.props = props;
        this.redis = redis;
        this.access = access;
        this.accounts = accounts;
    }

    /** Returns the provider consent URL the SPA should navigate to. */
    public String startConnect(UUID workspaceId, UUID userId, SocialPlatform platform) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);
        SocialOAuthProvider provider = registry.resolve(platform);

        String state = randomUrlSafe(24);
        String codeVerifier = randomUrlSafe(48);   // PKCE verifier (used by X, TikTok, …)
        redis.opsForValue().set(STATE_PREFIX + state, Map.of(
                "workspaceId", workspaceId.toString(),
                "userId", userId.toString(),
                "platform", platform.name(),
                "codeVerifier", codeVerifier), STATE_TTL);

        return provider.authorizationUrl(state, redirectUri(platform), pkceChallenge(codeVerifier));
    }

    /** Handles the provider callback; returns the SPA URL to redirect the browser to. */
    @SuppressWarnings("unchecked")
    public String handleCallback(SocialPlatform platform, String code, String state) {
        Object raw = state == null ? null : redis.opsForValue().get(STATE_PREFIX + state);
        if (!(raw instanceof Map<?, ?> stored)) {
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        }
        redis.delete(STATE_PREFIX + state);   // single use

        Map<String, String> data = (Map<String, String>) stored;
        if (!platform.name().equals(data.get("platform"))) {
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        }
        UUID workspaceId = UUID.fromString(data.get("workspaceId"));
        UUID userId = UUID.fromString(data.get("userId"));

        OAuthResult result = registry.resolve(platform)
                .exchangeCode(code, redirectUri(platform), data.get("codeVerifier"));
        accounts.completeOAuthConnection(workspaceId, userId, platform, result);

        return props.webRedirectUrl() + "?connected=" + platform.name().toLowerCase();
    }

    private String redirectUri(SocialPlatform platform) {
        return props.redirectBaseUrl() + "/api/v1/social/oauth/callback/"
                + platform.name().toLowerCase();
    }

    private String randomUrlSafe(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    /** PKCE S256 challenge = base64url(SHA-256(verifier)). */
    private String pkceChallenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
