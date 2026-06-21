package com.zyntral.modules.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Single-use security token for email verification and password reset. Hashed at rest;
 * {@code consumedAt} prevents replay.
 */
@Entity
@Table(name = "auth_tokens")
public class AuthToken {

    public enum Purpose { EMAIL_VERIFY, PASSWORD_RESET }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private String purpose;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AuthToken() {}

    public static AuthToken create(UUID userId, String tokenHash, Purpose purpose, Instant expiresAt) {
        AuthToken t = new AuthToken();
        t.userId = userId;
        t.tokenHash = tokenHash;
        t.purpose = purpose.name();
        t.expiresAt = expiresAt;
        return t;
    }

    public boolean isUsable() {
        return consumedAt == null && expiresAt.isAfter(Instant.now());
    }

    public void consume() { this.consumedAt = Instant.now(); }

    public UUID getUserId() { return userId; }
    public String getPurpose() { return purpose; }
}
