package com.zyntral.modules.workspace.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceInvitationRepository
        extends JpaRepository<WorkspaceInvitation, UUID> {

    Optional<WorkspaceInvitation> findByTokenHash(String tokenHash);

    List<WorkspaceInvitation> findByWorkspaceIdAndStatus(UUID workspaceId, InvitationStatus status);

    boolean existsByWorkspaceIdAndEmailAndStatus(UUID workspaceId, String email, InvitationStatus status);
}
