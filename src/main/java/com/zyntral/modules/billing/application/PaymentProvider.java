package com.zyntral.modules.billing.application;

import com.zyntral.common.domain.PlanCode;
import com.zyntral.modules.billing.domain.BillingInterval;
import com.zyntral.modules.billing.domain.InvoiceStatus;
import com.zyntral.modules.billing.domain.PaymentProviderKind;
import com.zyntral.modules.billing.domain.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Provider-agnostic payment port. Each vendor adapter (Stripe, PayPal) verifies its own
 * webhook signatures and parses its own payloads, but returns a <em>normalized</em>
 * {@link WebhookOutcome} so {@link BillingService} never contains vendor-specific logic.
 * Adding a provider = implementing this interface.
 */
public interface PaymentProvider {

    PaymentProviderKind kind();

    /** Starts a hosted subscription checkout; returns the redirect URL. */
    CheckoutSession createSubscriptionCheckout(CheckoutCommand command);

    /** Cancels a subscription, either immediately or at period end. */
    void cancelSubscription(String externalSubscriptionId, boolean atPeriodEnd);

    /**
     * Verifies the signature, parses the event, and normalizes it. Throws if the signature
     * is invalid. Returns {@code null} for events that don't affect billing state.
     */
    WebhookOutcome handleWebhook(String rawPayload, String signatureHeader);

    // ---- value objects ----

    record CheckoutCommand(
            UUID workspaceId,
            String customerEmail,
            PlanCode plan,
            BillingInterval interval,
            long amountCents,
            String currency,
            String successUrl,
            String cancelUrl
    ) {}

    record CheckoutSession(String externalId, String url) {}

    record WebhookOutcome(
            String eventId,
            String eventType,
            UUID workspaceId,                  // nullable — resolved from subscription if absent
            String subscriptionExternalId,     // nullable — used to resolve workspace for invoices
            SubscriptionChange subscription,   // nullable
            InvoiceChange invoice              // nullable
    ) {}

    record SubscriptionChange(
            String externalId, PlanCode plan, SubscriptionStatus status, BillingInterval interval,
            Instant periodStart, Instant periodEnd, boolean canceled
    ) {}

    record InvoiceChange(
            String externalId, String number, int amountCents, String currency,
            InvoiceStatus status, String hostedUrl, String pdfUrl
    ) {}
}
