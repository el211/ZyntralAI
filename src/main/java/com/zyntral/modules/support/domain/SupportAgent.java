package com.zyntral.modules.support.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A workspace's embeddable AI support assistant. The {@code publicKey} identifies the agent to
 * the website widget without exposing internal IDs or requiring auth.
 */
@Entity
@Table(name = "support_agents")
public class SupportAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String name;

    @Column(name = "public_key", nullable = false, unique = true)
    private String publicKey;

    @Column(name = "system_prompt", columnDefinition = "text")
    private String systemPrompt;

    @Column(nullable = false)
    private String model = "claude-opus-4-8";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected SupportAgent() {}

    public static SupportAgent create(UUID workspaceId, String name, String publicKey,
                                      String systemPrompt, String model) {
        SupportAgent a = new SupportAgent();
        a.workspaceId = workspaceId;
        a.name = name;
        a.publicKey = publicKey;
        a.systemPrompt = systemPrompt;
        if (model != null) a.model = model;
        return a;
    }

    @PreUpdate
    void touch() { this.updatedAt = Instant.now(); }

    public void update(String name, String systemPrompt, String model, Boolean active) {
        if (name != null) this.name = name;
        if (systemPrompt != null) this.systemPrompt = systemPrompt;
        if (model != null) this.model = model;
        if (active != null) this.active = active;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getName() { return name; }
    public String getPublicKey() { return publicKey; }
    public String getSystemPrompt() { return systemPrompt; }
    public String getModel() { return model; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
