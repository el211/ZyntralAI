package com.zyntral.modules.content.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Content aggregate. A post owns its media and its per-platform targets and is the single
 * place status transitions are expressed, so invalid lifecycle moves are impossible from
 * outside the domain.
 */
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body = "";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "post_status")
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "ai_generation_id")
    private UUID aiGenerationId;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTarget> targets = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PostMedia> media = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Post() {}

    public static Post draft(UUID workspaceId, UUID authorId, String title, String body,
                             UUID aiGenerationId) {
        Post p = new Post();
        p.workspaceId = workspaceId;
        p.authorId = authorId;
        p.title = title;
        p.body = body == null ? "" : body;
        p.aiGenerationId = aiGenerationId;
        p.status = PostStatus.DRAFT;
        return p;
    }

    @PreUpdate
    void touch() { this.updatedAt = Instant.now(); }

    // ---- content edits ----

    public void edit(String title, String body) {
        ensureMutable();
        if (title != null) this.title = title;
        if (body != null) this.body = body;
    }

    public void retarget(List<UUID> socialAccountIds) {
        ensureMutable();
        targets.clear();
        socialAccountIds.forEach(id -> targets.add(PostTarget.forAccount(this, id)));
    }

    public void addMedia(String url, String mediaType, String altText) {
        ensureMutable();
        media.add(PostMedia.of(this, url, mediaType, altText, (short) media.size()));
    }

    // ---- lifecycle ----

    public void schedule(Instant when) {
        ensureMutable();
        requireTargets();
        if (when == null || when.isBefore(Instant.now())) {
            throw new IllegalArgumentException("scheduledAt must be in the future");
        }
        this.scheduledAt = when;
        this.status = PostStatus.SCHEDULED;
    }

    /** Queue for immediate publishing by the worker. */
    public void queueNow() {
        ensureMutable();
        requireTargets();
        this.scheduledAt = Instant.now();
        this.status = PostStatus.QUEUED;
    }

    public void markPublishing() { this.status = PostStatus.PUBLISHING; }

    /** Called after all targets are attempted; status reflects whether any succeeded. */
    public void settlePublishResult() {
        boolean anyPublished = targets.stream().anyMatch(t -> t.getStatus() == TargetStatus.PUBLISHED);
        boolean anyFailed = targets.stream().anyMatch(t -> t.getStatus() == TargetStatus.FAILED);
        if (anyPublished) {
            this.status = PostStatus.PUBLISHED;
            this.publishedAt = Instant.now();
        } else if (anyFailed) {
            this.status = PostStatus.FAILED;
        }
    }

    public void cancel() {
        if (status == PostStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot cancel an already-published post");
        }
        this.status = PostStatus.CANCELLED;
    }

    private void ensureMutable() {
        if (status == PostStatus.PUBLISHING || status == PostStatus.PUBLISHED) {
            throw new IllegalStateException("Post can no longer be edited (" + status + ")");
        }
    }

    private void requireTargets() {
        if (targets.isEmpty()) {
            throw new IllegalStateException("Post has no target accounts");
        }
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getAuthorId() { return authorId; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public PostStatus getStatus() { return status; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public UUID getAiGenerationId() { return aiGenerationId; }
    public List<PostTarget> getTargets() { return targets; }
    public List<PostMedia> getMedia() { return media; }
    public Instant getCreatedAt() { return createdAt; }
}
