package com.zyntral.modules.ai.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** A generated image (logo or banner). Bytes are stored in-DB and served by id. */
@Entity
@Table(name = "ai_images")
public class AiImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private String prompt;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AiImage() {}

    public static AiImage create(UUID workspaceId, UUID userId, String kind, String prompt,
                                 String contentType, byte[] data) {
        AiImage i = new AiImage();
        i.workspaceId = workspaceId;
        i.userId = userId;
        i.kind = kind;
        i.prompt = prompt;
        i.contentType = contentType;
        i.data = data;
        return i;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getKind() { return kind; }
    public String getPrompt() { return prompt; }
    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
    public Instant getCreatedAt() { return createdAt; }
}
