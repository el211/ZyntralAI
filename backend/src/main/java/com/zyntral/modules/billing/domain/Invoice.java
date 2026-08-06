package com.zyntral.modules.billing.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** A billing invoice synced from the payment provider (billing/invoice history). */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_provider")
    private PaymentProviderKind provider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    private String number;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(nullable = false)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "invoice_status")
    private InvoiceStatus status = InvoiceStatus.OPEN;

    @Column(name = "hosted_url")
    private String hostedUrl;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Invoice() {}

    public static Invoice of(UUID workspaceId, UUID subscriptionId, PaymentProviderKind provider,
                             String externalId, String number, int amountCents, String currency,
                             InvoiceStatus status, String hostedUrl, String pdfUrl, Instant issuedAt) {
        Invoice i = new Invoice();
        i.workspaceId = workspaceId;
        i.subscriptionId = subscriptionId;
        i.provider = provider;
        i.externalId = externalId;
        i.number = number;
        i.amountCents = amountCents;
        i.currency = currency == null ? "USD" : currency;
        i.status = status;
        i.hostedUrl = hostedUrl;
        i.pdfUrl = pdfUrl;
        i.issuedAt = issuedAt;
        if (status == InvoiceStatus.PAID) i.paidAt = Instant.now();
        return i;
    }

    public void markPaid() {
        this.status = InvoiceStatus.PAID;
        this.paidAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public PaymentProviderKind getProvider() { return provider; }
    public String getExternalId() { return externalId; }
    public String getNumber() { return number; }
    public int getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public InvoiceStatus getStatus() { return status; }
    public String getHostedUrl() { return hostedUrl; }
    public String getPdfUrl() { return pdfUrl; }
    public Instant getCreatedAt() { return createdAt; }
}
