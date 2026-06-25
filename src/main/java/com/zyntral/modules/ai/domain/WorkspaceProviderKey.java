package com.zyntral.modules.ai.domain;

import com.zyntral.common.crypto.StringCryptoConverter;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A workspace's own API key for an external AI provider (BYOK). Key encrypted at rest. */
@Entity
@Table(name = "workspace_provider_keys")
public class WorkspaceProviderKey {

    @EmbeddedId
    private Id id;

    @Convert(converter = StringCryptoConverter.class)
    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected WorkspaceProviderKey() {}

    public WorkspaceProviderKey(UUID workspaceId, String provider, String apiKey) {
        this.id = new Id(workspaceId, provider);
        this.apiKey = apiKey;
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "workspace_id") private UUID workspaceId;
        @Column(name = "provider") private String provider;

        protected Id() {}
        public Id(UUID workspaceId, String provider) { this.workspaceId = workspaceId; this.provider = provider; }

        @Override public boolean equals(Object o) {
            if (!(o instanceof Id i)) return false;
            return Objects.equals(workspaceId, i.workspaceId) && Objects.equals(provider, i.provider);
        }
        @Override public int hashCode() { return Objects.hash(workspaceId, provider); }
    }
}
