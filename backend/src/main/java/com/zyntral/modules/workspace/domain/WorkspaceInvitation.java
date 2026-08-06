package com.zyntral.modules.workspace.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** A pending invitation to join a workspace. The raw token is emailed; only its hash is stored. */
@Entity
@Table(name = "workspace_invitations")
public class WorkspaceInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "workspace_role")
    private WorkspaceRole role = WorkspaceRole.EDITOR;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "invitation_status")
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected WorkspaceInvitation() {}

    public static WorkspaceInvitation create(UUID workspaceId, String email, WorkspaceRole role,
                                             String tokenHash, UUID invitedBy, Instant expiresAt) {
        WorkspaceInvitation i = new WorkspaceInvitation();
        i.workspaceId = workspaceId;
        i.email = email;
        i.role = role;
        i.tokenHash = tokenHash;
        i.invitedBy = invitedBy;
        i.expiresAt = expiresAt;
        return i;
    }

    public boolean isAcceptable() {
        return status == InvitationStatus.PENDING && expiresAt.isAfter(Instant.now());
    }

    public void accept() { this.status = InvitationStatus.ACCEPTED; }
    public void revoke() { this.status = InvitationStatus.REVOKED; }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getEmail() { return email; }
    public WorkspaceRole getRole() { return role; }
    public UUID getInvitedBy() { return invitedBy; }
    public InvitationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
}
