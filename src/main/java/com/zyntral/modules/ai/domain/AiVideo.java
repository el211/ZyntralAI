package com.zyntral.modules.ai.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** An async video-generation job (Veo). Lifecycle: PENDING → PROCESSING → COMPLETED / FAILED. */
@Entity
@Table(name = "ai_videos")
public class AiVideo {

    public enum Status { PENDING, PROCESSING, COMPLETED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String prompt;

    @Column(nullable = false)
    private String status;

    @Column(name = "operation_name")
    private String operationName;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "content_type")
    private String contentType;

    @Column
    private String error;

    @Column(name = "credits_cost", nullable = false)
    private int creditsCost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected AiVideo() {}

    public static AiVideo pending(UUID workspaceId, UUID userId, String prompt,
                                  String operationName, int creditsCost) {
        AiVideo v = new AiVideo();
        v.workspaceId = workspaceId;
        v.userId = userId;
        v.prompt = prompt;
        v.operationName = operationName;
        v.creditsCost = creditsCost;
        v.status = Status.PENDING.name();
        return v;
    }

    public void markProcessing() { this.status = Status.PROCESSING.name(); touch(); }
    public void markCompleted(String key, String contentType) {
        this.status = Status.COMPLETED.name();
        this.storageKey = key;
        this.contentType = contentType;
        touch();
    }
    public void markFailed(String error) { this.status = Status.FAILED.name(); this.error = error; touch(); }
    private void touch() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getPrompt() { return prompt; }
    public String getStatus() { return status; }
    public String getOperationName() { return operationName; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public String getError() { return error; }
    public int getCreditsCost() { return creditsCost; }
    public Instant getCreatedAt() { return createdAt; }
}
