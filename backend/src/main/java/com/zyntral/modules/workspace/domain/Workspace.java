package com.zyntral.modules.workspace.domain;

import com.zyntral.common.domain.PlanCode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant root. Every tenant-owned row references {@code workspace_id}. A workspace has
 * one owner and a plan that drives quota enforcement.
 */
@Entity
@Table(name = "workspaces")
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "plan_code")
    private PlanCode plan = PlanCode.FREE;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Workspace() {}

    public static Workspace create(String name, String slug, UUID ownerId) {
        Workspace w = new Workspace();
        w.name = name;
        w.slug = slug;
        w.ownerId = ownerId;
        w.plan = PlanCode.FREE;
        return w;
    }

    @PreUpdate
    void touch() { this.updatedAt = Instant.now(); }

    public void rename(String name) { this.name = name; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void changePlan(PlanCode plan) { this.plan = plan; }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public UUID getOwnerId() { return ownerId; }
    public PlanCode getPlan() { return plan; }
    public String getImageUrl() { return imageUrl; }
    public Instant getCreatedAt() { return createdAt; }
}
