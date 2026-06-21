package com.zyntral.modules.billing.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** A payment attempt/charge synced from the provider (revenue & payment monitoring). */
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_provider")
    private PaymentProviderKind provider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(nullable = false)
    private String currency = "USD";

    @Column(nullable = false)
    private String status;        // SUCCEEDED | FAILED | REFUNDED | PENDING

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected PaymentTransaction() {}

    public static PaymentTransaction of(UUID workspaceId, UUID invoiceId, PaymentProviderKind provider,
                                        String externalId, int amountCents, String currency,
                                        String status) {
        PaymentTransaction t = new PaymentTransaction();
        t.workspaceId = workspaceId;
        t.invoiceId = invoiceId;
        t.provider = provider;
        t.externalId = externalId;
        t.amountCents = amountCents;
        t.currency = currency == null ? "USD" : currency;
        t.status = status;
        return t;
    }

    public int getAmountCents() { return amountCents; }
    public String getStatus() { return status; }
}
