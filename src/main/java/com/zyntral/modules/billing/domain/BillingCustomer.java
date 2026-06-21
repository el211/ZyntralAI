package com.zyntral.modules.billing.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Maps a workspace to its customer record at a payment provider (one per provider). */
@Entity
@Table(name = "billing_customers")
public class BillingCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_provider")
    private PaymentProviderKind provider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected BillingCustomer() {}

    public static BillingCustomer of(UUID workspaceId, PaymentProviderKind provider, String externalId) {
        BillingCustomer c = new BillingCustomer();
        c.workspaceId = workspaceId;
        c.provider = provider;
        c.externalId = externalId;
        return c;
    }

    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
}
