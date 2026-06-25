package com.zyntral.modules.ai.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Generated speech (TTS). Bytes are stored in-DB and served by id. */
@Entity
@Table(name = "ai_audio")
public class AiAudio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "text_excerpt", nullable = false)
    private String textExcerpt;

    @Column
    private String voice;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AiAudio() {}

    public static AiAudio create(UUID workspaceId, UUID userId, String text, String voice,
                                 String contentType, byte[] data) {
        AiAudio a = new AiAudio();
        a.workspaceId = workspaceId;
        a.userId = userId;
        a.textExcerpt = text == null ? "" : text.substring(0, Math.min(text.length(), 280));
        a.voice = voice;
        a.contentType = contentType;
        a.data = data;
        return a;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getTextExcerpt() { return textExcerpt; }
    public String getVoice() { return voice; }
    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
    public Instant getCreatedAt() { return createdAt; }
}
