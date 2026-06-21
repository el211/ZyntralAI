package com.zyntral.modules.content.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** One platform destination for a post (post fans out to N targets). */
@Entity
@Table(name = "post_targets")
public class PostTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "social_account_id", nullable = false)
    private UUID socialAccountId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "target_status")
    private TargetStatus status = TargetStatus.PENDING;

    @Column(name = "external_post_id")
    private String externalPostId;

    @Column(name = "permalink")
    private String permalink;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(nullable = false)
    private short attempts = 0;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected PostTarget() {}

    static PostTarget forAccount(Post post, UUID socialAccountId) {
        PostTarget t = new PostTarget();
        t.post = post;
        t.socialAccountId = socialAccountId;
        return t;
    }

    public void markPublishing() {
        this.status = TargetStatus.PUBLISHING;
        this.attempts++;
    }

    public void markPublished(String externalPostId, String permalink) {
        this.status = TargetStatus.PUBLISHED;
        this.externalPostId = externalPostId;
        this.permalink = permalink;
        this.publishedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markFailed(String error) {
        this.status = TargetStatus.FAILED;
        this.errorMessage = error;
    }

    public UUID getId() { return id; }
    public UUID getSocialAccountId() { return socialAccountId; }
    public TargetStatus getStatus() { return status; }
    public String getExternalPostId() { return externalPostId; }
    public String getPermalink() { return permalink; }
    public String getErrorMessage() { return errorMessage; }
    public short getAttempts() { return attempts; }
}
