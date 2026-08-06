package com.zyntral.modules.workspace.domain;

/**
 * A member's role within a workspace (distinct from the global USER/ADMIN security role).
 * Mirrors the PostgreSQL {@code workspace_role} enum. OWNER > ADMIN > EDITOR > VIEWER.
 */
public enum WorkspaceRole {
    OWNER, ADMIN, EDITOR, VIEWER;

    public boolean canManageMembers() {
        return this == OWNER || this == ADMIN;
    }

    public boolean canEditContent() {
        return this != VIEWER;
    }
}
