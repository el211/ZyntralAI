package com.zyntral.modules.support.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** A knowledge-base entry the support agent can ground its answers in. */
@Entity
@Table(name = "support_knowledge")
public class SupportKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected SupportKnowledge() {}

    public static SupportKnowledge of(UUID agentId, String title, String content, String sourceUrl) {
        SupportKnowledge k = new SupportKnowledge();
        k.agentId = agentId;
        k.title = title;
        k.content = content;
        k.sourceUrl = sourceUrl;
        return k;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSourceUrl() { return sourceUrl; }
    public Instant getCreatedAt() { return createdAt; }
}
