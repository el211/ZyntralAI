package com.zyntral.modules.ai.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A single AI generation record (audit + analytics + history). Maps the month-partitioned
 * {@code ai_generations} table; {@code createdAt} is set on creation so the row routes to
 * the correct partition.
 */
@Entity
@Table(name = "ai_generations")
public class AiGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "ai_provider_kind")
    private AiProviderKind provider;

    @Column(nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "content_kind", nullable = false, columnDefinition = "ai_content_kind")
    private AiContentKind contentKind;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "ai_tone")
    private AiTone tone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "ai_length")
    private AiLength length;

    @Column(nullable = false)
    private String language = "en";

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(columnDefinition = "text")
    private String output;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "credits_cost", nullable = false)
    private int creditsCost = 1;

    @Column(nullable = false)
    private String status = "SUCCESS";

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AiGeneration() {}

    public static AiGeneration success(UUID workspaceId, UUID userId, AiProviderKind provider,
                                       String model, AiContentKind kind, AiTone tone, AiLength length,
                                       String language, String prompt, String output,
                                       int promptTokens, int outputTokens, int creditsCost,
                                       int latencyMs) {
        AiGeneration g = new AiGeneration();
        g.workspaceId = workspaceId;
        g.userId = userId;
        g.provider = provider;
        g.model = model;
        g.contentKind = kind;
        g.tone = tone;
        g.length = length;
        g.language = language;
        g.prompt = prompt;
        g.output = output;
        g.promptTokens = promptTokens;
        g.outputTokens = outputTokens;
        g.creditsCost = creditsCost;
        g.latencyMs = latencyMs;
        g.status = "SUCCESS";
        return g;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public AiProviderKind getProvider() { return provider; }
    public String getModel() { return model; }
    public AiContentKind getContentKind() { return contentKind; }
    public AiTone getTone() { return tone; }
    public AiLength getLength() { return length; }
    public String getOutput() { return output; }
    public int getPromptTokens() { return promptTokens; }
    public int getOutputTokens() { return outputTokens; }
    public int getCreditsCost() { return creditsCost; }
    public Instant getCreatedAt() { return createdAt; }
}
