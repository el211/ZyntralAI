package com.zyntral.modules.workspace.web.dto;

import com.zyntral.common.domain.PlanCode;
import com.zyntral.modules.workspace.domain.InvitationStatus;
import com.zyntral.modules.workspace.domain.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class WorkspaceDtos {

    private WorkspaceDtos() {}

    public record CreateWorkspaceRequest(@NotBlank @Size(max = 120) String name) {}

    public record UpdateWorkspaceRequest(@Size(max = 120) String name, String imageUrl) {}

    public record WorkspaceResponse(
            UUID id, String name, String slug, PlanCode plan, String imageUrl,
            WorkspaceRole myRole, long memberCount, Instant createdAt
    ) {}

    public record InviteMemberRequest(
            @Email @NotBlank String email,
            @NotNull WorkspaceRole role
    ) {}

    public record AcceptInvitationRequest(@NotBlank String token) {}

    public record UpdateMemberRoleRequest(@NotNull WorkspaceRole role) {}

    public record MemberResponse(UUID userId, WorkspaceRole role, Instant joinedAt) {}

    public record InvitationResponse(
            UUID id, String email, WorkspaceRole role, InvitationStatus status, Instant expiresAt
    ) {}
}
