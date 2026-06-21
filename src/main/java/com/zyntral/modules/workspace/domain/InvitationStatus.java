package com.zyntral.modules.workspace.domain;

/** Mirrors the PostgreSQL {@code invitation_status} enum. */
public enum InvitationStatus {
    PENDING, ACCEPTED, EXPIRED, REVOKED
}
