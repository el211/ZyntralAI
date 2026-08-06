package com.zyntral.modules.user.domain;

/** Mirrors the PostgreSQL {@code user_status} enum. */
public enum UserStatus {
    PENDING, ACTIVE, SUSPENDED, DELETED
}
