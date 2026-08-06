package com.zyntral.modules.billing.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Records every provider webhook for idempotent processing. The {@code (provider, event_id)}
 * unique constraint makes replays no-ops; {@code processedAt} guards re-application.
 */
@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_provider")
    private PaymentProviderKind provider;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt = Instant.now();

    protected WebhookEvent() {}

    public static WebhookEvent received(PaymentProviderKind provider, String eventId,
                                        String eventType, String payload) {
        WebhookEvent e = new WebhookEvent();
        e.provider = provider;
        e.eventId = eventId;
        e.eventType = eventType;
        e.payload = payload;
        return e;
    }

    public void markProcessed() { this.processedAt = Instant.now(); }

    public boolean isProcessed() { return processedAt != null; }

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
}
