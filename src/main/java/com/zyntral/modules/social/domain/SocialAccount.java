package com.zyntral.modules.social.domain;

import com.zyntral.common.crypto.StringCryptoConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A social account connected to a workspace. OAuth tokens are encrypted at rest via
 * {@link StringCryptoConverter}. Uniqueness is (workspace, platform, external account id).
 */
@Entity
@Table(name = "social_accounts")
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "social_platform")
    private SocialPlatform platform;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "handle")
    private String handle;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Convert(converter = StringCryptoConverter.class)
    @Column(name = "access_token")
    private String accessToken;

    @Convert(converter = StringCryptoConverter.class)
    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "scopes")
    private String scopes;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "social_acct_status")
    private SocialAccountStatus status = SocialAccountStatus.CONNECTED;

    @Column(name = "connected_by")
    private UUID connectedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected SocialAccount() {}

    public static SocialAccount connect(UUID workspaceId, SocialPlatform platform, String externalId,
                                        String displayName, String handle, String avatarUrl,
                                        String accessToken, String refreshToken, String scopes,
                                        Instant tokenExpiresAt, UUID connectedBy) {
        SocialAccount a = new SocialAccount();
        a.workspaceId = workspaceId;
        a.platform = platform;
        a.externalId = externalId;
        a.displayName = displayName;
        a.handle = handle;
        a.avatarUrl = avatarUrl;
        a.accessToken = accessToken;
        a.refreshToken = refreshToken;
        a.scopes = scopes;
        a.tokenExpiresAt = tokenExpiresAt;
        a.connectedBy = connectedBy;
        a.status = SocialAccountStatus.CONNECTED;
        return a;
    }

    @PreUpdate
    void touch() { this.updatedAt = Instant.now(); }

    public void markRevoked() { this.status = SocialAccountStatus.REVOKED; }
    public void markError() { this.status = SocialAccountStatus.ERROR; }

    public void refreshTokens(String accessToken, String refreshToken, Instant expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = expiresAt;
        this.status = SocialAccountStatus.CONNECTED;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public SocialPlatform getPlatform() { return platform; }
    public String getExternalId() { return externalId; }
    public String getDisplayName() { return displayName; }
    public String getHandle() { return handle; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getAccessToken() { return accessToken; }
    public SocialAccountStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
