package com.zyntral.modules.workspace.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    boolean existsBySlug(String slug);

    long countByOwnerId(UUID ownerId);

    /** Workspaces the user belongs to (owner or member), for the workspace switcher. */
    @Query("""
           SELECT w FROM Workspace w
           WHERE w.id IN (SELECT m.id.workspaceId FROM WorkspaceMember m WHERE m.id.userId = :userId)
           ORDER BY w.createdAt
           """)
    List<Workspace> findAllForUser(@Param("userId") UUID userId);
}
