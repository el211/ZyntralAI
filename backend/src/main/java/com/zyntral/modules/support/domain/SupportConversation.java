package com.zyntral.modules.support.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** A visitor's conversation with a support agent (history is retained). */
@Entity
@Table(name = "support_conversations")
public class SupportConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "visitor_id")
    private String visitorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected SupportConversation() {}

    public static SupportConversation start(UUID agentId, String visitorId) {
        SupportConversation c = new SupportConversation();
        c.agentId = agentId;
        c.visitorId = visitorId;
        return c;
    }

    public UUID getId() { return id; }
    public UUID getAgentId() { return agentId; }
    public Instant getCreatedAt() { return createdAt; }
}
