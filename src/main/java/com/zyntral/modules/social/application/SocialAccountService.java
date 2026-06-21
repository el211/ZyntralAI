package com.zyntral.modules.social.application;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.billing.domain.Plan;
import com.zyntral.modules.billing.domain.PlanRepository;
import com.zyntral.modules.social.domain.SocialAccount;
import com.zyntral.modules.social.domain.SocialAccountRepository;
import com.zyntral.modules.social.web.dto.SocialDtos.*;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import com.zyntral.modules.workspace.domain.Workspace;
import com.zyntral.modules.workspace.domain.WorkspaceRepository;
import com.zyntral.modules.workspace.domain.WorkspaceRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Connect / manage / disconnect social accounts. Connecting and disconnecting require ADMIN+;
 * any member may list. Enforces the plan's connected-account limit.
 */
@Service
public class SocialAccountService {

    private final SocialAccountRepository accounts;
    private final WorkspaceAccess access;
    private final WorkspaceRepository workspaces;
    private final PlanRepository plans;

    public SocialAccountService(SocialAccountRepository accounts, WorkspaceAccess access,
                                WorkspaceRepository workspaces, PlanRepository plans) {
        this.accounts = accounts;
        this.access = access;
        this.workspaces = workspaces;
        this.plans = plans;
    }

    @Transactional
    public SocialAccountResponse connect(UUID workspaceId, UUID userId, ConnectAccountRequest req) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);

        // re-connecting an existing account just refreshes its tokens (no limit charge)
        var existing = accounts.findByWorkspaceIdAndPlatformAndExternalId(
                workspaceId, req.platform(), req.externalId());
        if (existing.isPresent()) {
            SocialAccount acct = existing.get();
            acct.refreshTokens(req.accessToken(), req.refreshToken(), req.tokenExpiresAt());
            return toResponse(acct);
        }

        enforceAccountLimit(workspaceId);
        SocialAccount acct = accounts.save(SocialAccount.connect(
                workspaceId, req.platform(), req.externalId(), req.displayName(), req.handle(),
                req.avatarUrl(), req.accessToken(), req.refreshToken(), req.scopes(),
                req.tokenExpiresAt(), userId));
        return toResponse(acct);
    }

    /**
     * Persists an account from a completed OAuth flow. Access was already checked when the flow
     * started; we re-check here (the callback is public) using the userId bound to the state.
     */
    @Transactional
    public SocialAccountResponse completeOAuthConnection(
            UUID workspaceId, UUID userId,
            com.zyntral.modules.social.domain.SocialPlatform platform,
            SocialOAuthProvider.OAuthResult oauth) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);

        var existing = accounts.findByWorkspaceIdAndPlatformAndExternalId(
                workspaceId, platform, oauth.externalId());
        if (existing.isPresent()) {
            SocialAccount acct = existing.get();
            acct.refreshTokens(oauth.accessToken(), oauth.refreshToken(), oauth.expiresAt());
            return toResponse(acct);
        }

        enforceAccountLimit(workspaceId);
        SocialAccount acct = accounts.save(SocialAccount.connect(
                workspaceId, platform, oauth.externalId(), oauth.displayName(), oauth.handle(),
                oauth.avatarUrl(), oauth.accessToken(), oauth.refreshToken(), oauth.scopes(),
                oauth.expiresAt(), userId));
        return toResponse(acct);
    }

    @Transactional(readOnly = true)
    public List<SocialAccountResponse> list(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return accounts.findByWorkspaceId(workspaceId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void disconnect(UUID workspaceId, UUID userId, UUID accountId) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);
        SocialAccount acct = accounts.findByIdAndWorkspaceId(accountId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("social account", accountId));
        accounts.delete(acct);
    }

    private void enforceAccountLimit(UUID workspaceId) {
        Workspace ws = workspaces.findById(workspaceId)
                .orElseThrow(() -> ApiException.notFound("workspace", workspaceId));
        Plan plan = plans.findById(ws.getPlan()).orElseThrow();
        if (accounts.countByWorkspaceId(workspaceId) >= plan.getMaxSocialAccounts()) {
            throw new ApiException(ErrorCode.PLAN_LIMIT_REACHED);
        }
    }

    private SocialAccountResponse toResponse(SocialAccount a) {
        return new SocialAccountResponse(a.getId(), a.getPlatform(), a.getExternalId(),
                a.getDisplayName(), a.getHandle(), a.getAvatarUrl(), a.getStatus(), a.getCreatedAt());
    }
}
