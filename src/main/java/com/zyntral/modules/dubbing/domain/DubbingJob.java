package com.zyntral.modules.dubbing.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One video-dubbing job submitted to ElevenLabs. We persist only metadata and the upstream
 * {@code dubbingId}; the media itself lives at ElevenLabs and is streamed through on download
 * (the workspace's own key is billed — see {@link ElevenLabsCredential}).
 */
@Entity
@Table(name = "dubbing_jobs")
public class DubbingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** ElevenLabs dubbing project id. */
    @Column(name = "dubbing_id", nullable = false)
    private String dubbingId;

    @Column(name = "name")
    private String name;

    /** Source language code, or null when ElevenLabs auto-detects it. */
    @Column(name = "source_lang")
    private String sourceLang;

    @Column(name = "target_lang", nullable = false)
    private String targetLang;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "dubbing_status")
    private DubbingStatus status = DubbingStatus.QUEUED;

    @Column(name = "error", columnDefinition = "text")
    private String error;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected DubbingJob() {}

    public static DubbingJob create(UUID workspaceId, UUID userId, String dubbingId, String name,
                                    String sourceLang, String targetLang, DubbingStatus status) {
        DubbingJob j = new DubbingJob();
        j.workspaceId = workspaceId;
        j.userId = userId;
        j.dubbingId = dubbingId;
        j.name = name;
        j.sourceLang = sourceLang;
        j.targetLang = targetLang;
        j.status = status != null ? status : DubbingStatus.QUEUED;
        return j;
    }

    /** Apply a refreshed status (and optional error) from ElevenLabs. */
    public void updateStatus(DubbingStatus status, String error) {
        this.status = status;
        this.error = error;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getUserId() { return userId; }
    public String getDubbingId() { return dubbingId; }
    public String getName() { return name; }
    public String getSourceLang() { return sourceLang; }
    public String getTargetLang() { return targetLang; }
    public DubbingStatus getStatus() { return status; }
    public String getError() { return error; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
