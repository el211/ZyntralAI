package com.zyntral.modules.admin.application;

import com.zyntral.common.domain.PlanCode;
import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.common.security.JwtService;
import com.zyntral.common.web.PageResponse;
import com.zyntral.modules.admin.web.dto.AdminDtos.*;
import com.zyntral.modules.ai.application.AiCreditService;
import com.zyntral.modules.ai.domain.AiGenerationRepository;
import com.zyntral.modules.auth.domain.RefreshTokenRepository;
import com.zyntral.modules.billing.domain.InvoiceRepository;
import com.zyntral.modules.billing.domain.SubscriptionRepository;
import com.zyntral.modules.social.domain.SocialAccountRepository;
import com.zyntral.modules.user.domain.Role;
import com.zyntral.modules.user.domain.User;
import com.zyntral.modules.user.domain.UserRepository;
import com.zyntral.modules.workspace.domain.Workspace;
import com.zyntral.modules.workspace.domain.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-tenant administration & analytics. Action callers are constrained to ROLE_ADMIN by the
 * security chain ({@code /api/v1/admin/**}); the admin token is minted by {@link #login} against
 * env-configured credentials (not a user account).
 */
@Service
public class AdminService {

    private static final UUID ADMIN_PRINCIPAL = new UUID(0L, 0L);
    private static final Duration ADMIN_TOKEN_TTL = Duration.ofHours(8);

    private final UserRepository users;
    private final WorkspaceRepository workspaces;
    private final SubscriptionRepository subscriptions;
    private final InvoiceRepository invoices;
    private final AiGenerationRepository aiGenerations;
    private final SocialAccountRepository socialAccounts;
    private final AiCreditService credits;
    private final RefreshTokenRepository refreshTokens;
    private final JwtService jwt;
    private final String adminUsername;
    private final String adminPassword;

    public AdminService(UserRepository users, WorkspaceRepository workspaces,
                        SubscriptionRepository subscriptions, InvoiceRepository invoices,
                        AiGenerationRepository aiGenerations, SocialAccountRepository socialAccounts,
                        AiCreditService credits, RefreshTokenRepository refreshTokens, JwtService jwt,
                        @Value("${zyntral.admin.username:admin}") String adminUsername,
                        @Value("${zyntral.admin.password:}") String adminPassword) {
        this.users = users;
        this.workspaces = workspaces;
        this.subscriptions = subscriptions;
        this.invoices = invoices;
        this.aiGenerations = aiGenerations;
        this.socialAccounts = socialAccounts;
        this.credits = credits;
        this.refreshTokens = refreshTokens;
        this.jwt = jwt;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    // ---- admin-panel login (env credentials → admin JWT) ----

    public String login(String username, String password) {
        boolean ok = adminPassword != null && !adminPassword.isBlank()
                && constantTimeEquals(adminUsername, username)
                && constantTimeEquals(adminPassword, password);
        if (!ok) throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        return jwt.issueToken(ADMIN_PRINCIPAL, "admin@panel", List.of("ADMIN"), ADMIN_TOKEN_TTL);
    }

    // ---- analytics ----

    @Transactional(readOnly = true)
    public OverviewResponse overview() {
        return new OverviewResponse(
                users.count(), workspaces.count(), subscriptions.countLive(),
                invoices.sumPaidAmountCents(), aiGenerations.count(), socialAccounts.count());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserAdminResponse> listUsers(int page, int size) {
        return PageResponse.from(users.findAll(PageRequest.of(page, size)).map(UserAdminResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionAdminResponse> listSubscriptions(int page, int size) {
        return PageResponse.from(subscriptions.findAll(PageRequest.of(page, size))
                .map(SubscriptionAdminResponse::from));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> aiUsageByProvider() { return toBreakdown(aiGenerations.countByProvider()); }

    @Transactional(readOnly = true)
    public Map<String, Long> socialStatsByPlatform() { return toBreakdown(socialAccounts.countByPlatform()); }

    // ---- user actions ----

    @Transactional
    public void setUserSuspended(UUID userId, boolean suspended) {
        User user = requireUser(userId);
        if (suspended) {
            user.suspend();
            refreshTokens.revokeAllForUser(userId, Instant.now());   // kill active sessions
        } else {
            user.reactivate();
        }
    }

    /** Soft-delete: marks DELETED and revokes sessions (avoids cross-table FK cascades). */
    @Transactional
    public void deleteUser(UUID userId) {
        User user = requireUser(userId);
        user.markDeleted();
        refreshTokens.revokeAllForUser(userId, Instant.now());
    }

    /** Give extra AI credits to every workspace the user owns (current month). */
    @Transactional
    public void grantCredits(UUID userId, int amount) {
        if (amount <= 0) throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"amount must be positive"});
        requireUser(userId);
        ownedWorkspaces(userId).forEach(ws -> credits.grant(ws.getId(), amount));
    }

    /** Free upgrade/downgrade: set the plan on every workspace the user owns. */
    @Transactional
    public void setPlan(UUID userId, PlanCode plan) {
        requireUser(userId);
        ownedWorkspaces(userId).forEach(ws -> ws.changePlan(plan));
    }

    /** Force-login: mint an access token for the target user (admin support tool). */
    @Transactional(readOnly = true)
    public ImpersonateResponse impersonate(UUID userId) {
        User user = requireUser(userId);
        List<String> roles = user.getRoles().stream().map(Role::getCode).toList();
        String token = jwt.issueToken(user.getId(), user.getEmail(), roles, Duration.ofHours(1));
        return new ImpersonateResponse(token,
                new ImpersonatedUser(user.getId(), user.getEmail(), user.getFullName(),
                        user.isEmailVerified(), roles));
    }

    // ---- helpers ----

    private List<Workspace> ownedWorkspaces(UUID userId) {
        return workspaces.findAllForUser(userId).stream()
                .filter(w -> userId.equals(w.getOwnerId())).toList();
    }

    private User requireUser(UUID userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("user", userId));
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return java.security.MessageDigest.isEqual(
                a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Map<String, Long> toBreakdown(List<Object[]> rows) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Object[] row : rows) out.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        return out;
    }
}
