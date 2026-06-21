package com.zyntral.modules.workspace.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Join entity: a user's membership and role in a workspace. Composite PK. */
@Entity
@Table(name = "workspace_members")
public class WorkspaceMember {

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "workspace_id")
        private UUID workspaceId;
        @Column(name = "user_id")
        private UUID userId;

        protected Id() {}
        public Id(UUID workspaceId, UUID userId) {
            this.workspaceId = workspaceId;
            this.userId = userId;
        }
        public UUID getWorkspaceId() { return workspaceId; }
        public UUID getUserId() { return userId; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id id)) return false;
            return Objects.equals(workspaceId, id.workspaceId) && Objects.equals(userId, id.userId);
        }
        @Override public int hashCode() { return Objects.hash(workspaceId, userId); }
    }

    @EmbeddedId
    private Id id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "workspace_role")
    private WorkspaceRole role = WorkspaceRole.EDITOR;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    protected WorkspaceMember() {}

    public static WorkspaceMember of(UUID workspaceId, UUID userId, WorkspaceRole role) {
        WorkspaceMember m = new WorkspaceMember();
        m.id = new Id(workspaceId, userId);
        m.role = role;
        return m;
    }

    public void changeRole(WorkspaceRole role) { this.role = role; }

    public Id getId() { return id; }
    public UUID getWorkspaceId() { return id.getWorkspaceId(); }
    public UUID getUserId() { return id.getUserId(); }
    public WorkspaceRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
}
