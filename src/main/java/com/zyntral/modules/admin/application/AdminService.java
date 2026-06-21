package com.zyntral.modules.admin.application;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.web.PageResponse;
import com.zyntral.modules.admin.web.dto.AdminDtos.*;
import com.zyntral.modules.ai.domain.AiGenerationRepository;
import com.zyntral.modules.billing.domain.InvoiceRepository;
import com.zyntral.modules.billing.domain.SubscriptionRepository;
import com.zyntral.modules.social.domain.SocialAccountRepository;
import com.zyntral.modules.user.domain.User;
import com.zyntral.modules.user.domain.UserRepository;
import com.zyntral.modules.workspace.domain.WorkspaceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-tenant administration & analytics. All callers are already constrained to ROLE_ADMIN
 * by the security chain ({@code /api/v1/admin/**}).
 */
@Service
public class AdminService {

    private final UserRepository users;
    private final WorkspaceRepository workspaces;
    private final SubscriptionRepository subscriptions;
    private final InvoiceRepository invoices;
    private final AiGenerationRepository aiGenerations;
    private final SocialAccountRepository socialAccounts;

    public AdminService(UserRepository users, WorkspaceRepository workspaces,
                        SubscriptionRepository subscriptions, InvoiceRepository invoices,
                        AiGenerationRepository aiGenerations, SocialAccountRepository socialAccounts) {
        this.users = users;
        this.workspaces = workspaces;
        this.subscriptions = subscriptions;
        this.invoices = invoices;
        this.aiGenerations = aiGenerations;
        this.socialAccounts = socialAccounts;
    }

    @Transactional(readOnly = true)
    public OverviewResponse overview() {
        return new OverviewResponse(
                users.count(),
                workspaces.count(),
                subscriptions.countLive(),
                invoices.sumPaidAmountCents(),
                aiGenerations.count(),
                socialAccounts.count());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserAdminResponse> listUsers(int page, int size) {
        return PageResponse.from(users.findAll(PageRequest.of(page, size))
                .map(UserAdminResponse::from));
    }

    @Transactional
    public void setUserSuspended(UUID userId, boolean suspended) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("user", userId));
        if (suspended) user.suspend(); else user.reactivate();
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionAdminResponse> listSubscriptions(int page, int size) {
        return PageResponse.from(subscriptions.findAll(PageRequest.of(page, size))
                .map(SubscriptionAdminResponse::from));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> aiUsageByProvider() {
        return toBreakdown(aiGenerations.countByProvider());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> socialStatsByPlatform() {
        return toBreakdown(socialAccounts.countByPlatform());
    }

    private Map<String, Long> toBreakdown(java.util.List<Object[]> rows) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Object[] row : rows) {
            out.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return out;
    }
}
