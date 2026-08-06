package com.zyntral.modules.workspace.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository
        extends JpaRepository<WorkspaceMember, WorkspaceMember.Id> {

    List<WorkspaceMember> findByIdWorkspaceId(UUID workspaceId);

    Optional<WorkspaceMember> findByIdWorkspaceIdAndIdUserId(UUID workspaceId, UUID userId);

    boolean existsByIdWorkspaceIdAndIdUserId(UUID workspaceId, UUID userId);

    long countByIdWorkspaceId(UUID workspaceId);
}
