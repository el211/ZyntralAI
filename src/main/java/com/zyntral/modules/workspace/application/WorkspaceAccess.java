package com.zyntral.modules.workspace.application;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.workspace.domain.WorkspaceMember;
import com.zyntral.modules.workspace.domain.WorkspaceMemberRepository;
import com.zyntral.modules.workspace.domain.WorkspaceRole;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Reusable tenant-scoping guard. Other modules (content, social, ai, billing) call this
 * to assert the current user may act within a workspace and at a sufficient role — the
 * single enforcement point for multi-tenant isolation.
 */
@Component
public class WorkspaceAccess {

    private final WorkspaceMemberRepository members;

    public WorkspaceAccess(WorkspaceMemberRepository members) {
        this.members = members;
    }

    public WorkspaceRole roleOf(UUID workspaceId, UUID userId) {
        return members.findByIdWorkspaceIdAndIdUserId(workspaceId, userId)
                .map(WorkspaceMember::getRole)
                .orElseThrow(ApiException::forbidden);   // not a member → 403, don't leak existence
    }

    public void requireMember(UUID workspaceId, UUID userId) {
        roleOf(workspaceId, userId);
    }

    /** Asserts the user's role is at least {@code minimum} (OWNER > ADMIN > EDITOR > VIEWER). */
    public void requireAtLeast(UUID workspaceId, UUID userId, WorkspaceRole minimum) {
        WorkspaceRole actual = roleOf(workspaceId, userId);
        if (actual.ordinal() > minimum.ordinal()) {   // lower privilege = higher ordinal
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    public void requireCanEdit(UUID workspaceId, UUID userId) {
        if (!roleOf(workspaceId, userId).canEditContent()) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    public void requireCanManageMembers(UUID workspaceId, UUID userId) {
        if (!roleOf(workspaceId, userId).canManageMembers()) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }
}
