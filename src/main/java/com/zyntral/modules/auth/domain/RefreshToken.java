package com.zyntral.modules.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Opaque, rotating refresh token. Only the SHA-256 hash is stored, never the raw value.
 * Rotation: on use, the old token is revoked and {@code replacedById} points at its successor,
 * so token reuse after rotation is detectable (theft signal).
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected RefreshToken() {}

    public static RefreshToken issue(UUID userId, String tokenHash, Instant expiresAt,
                                     String userAgent, String ipAddress) {
        RefreshToken t = new RefreshToken();
        t.userId = userId;
        t.tokenHash = tokenHash;
        t.expiresAt = expiresAt;
        t.userAgent = userAgent;
        t.ipAddress = ipAddress;
        return t;
    }

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }

    public void revoke() { this.revokedAt = Instant.now(); }

    public void revokeAndReplace(UUID successor) {
        revoke();
        this.replacedById = successor;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
