package com.zyntral.modules.billing.domain;

import com.zyntral.common.domain.PlanCode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A workspace's subscription with a payment provider. The DB enforces at most one live
 * subscription per workspace (partial unique index). State is driven by provider webhooks.
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "plan_code")
    private PlanCode plan;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_provider")
    private PaymentProviderKind provider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "subscription_status")
    private SubscriptionStatus status = SubscriptionStatus.INCOMPLETE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "\"interval\"", nullable = false, columnDefinition = "billing_interval")
    private BillingInterval interval = BillingInterval.MONTHLY;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd = false;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Subscription() {}

    public static Subscription create(UUID workspaceId, PlanCode plan, PaymentProviderKind provider,
                                      String externalId, BillingInterval interval) {
        Subscription s = new Subscription();
        s.workspaceId = workspaceId;
        s.plan = plan;
        s.provider = provider;
        s.externalId = externalId;
        s.interval = interval;
        s.status = SubscriptionStatus.INCOMPLETE;
        return s;
    }

    @PreUpdate
    void touch() { this.updatedAt = Instant.now(); }

    public void activate(PlanCode plan, Instant periodStart, Instant periodEnd) {
        this.status = SubscriptionStatus.ACTIVE;
        this.plan = plan;
        this.currentPeriodStart = periodStart;
        this.currentPeriodEnd = periodEnd;
        this.cancelAtPeriodEnd = false;
        this.canceledAt = null;
    }

    public void updateStatus(SubscriptionStatus status) { this.status = status; }

    public void markCancelAtPeriodEnd() { this.cancelAtPeriodEnd = true; }

    public void markCanceled() {
        this.status = SubscriptionStatus.CANCELED;
        this.canceledAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public PlanCode getPlan() { return plan; }
    public PaymentProviderKind getProvider() { return provider; }
    public String getExternalId() { return externalId; }
    public SubscriptionStatus getStatus() { return status; }
    public BillingInterval getInterval() { return interval; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public boolean isCancelAtPeriodEnd() { return cancelAtPeriodEnd; }
}
