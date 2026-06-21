package com.zyntral.modules.billing.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.zyntral.common.domain.PlanCode;
import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.billing.application.PaymentProvider;
import com.zyntral.modules.billing.config.PaymentProperties;
import com.zyntral.modules.billing.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stripe adapter. Uses Checkout (subscription mode) with inline recurring price_data — no
 * pre-created Stripe prices required. Webhooks are signature-verified, then parsed from raw
 * JSON (robust to SDK version differences) and normalized to {@link WebhookOutcome}.
 */
@Component
public class StripePaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentProvider.class);

    private final PaymentProperties.Stripe config;
    private final ObjectMapper mapper;

    public StripePaymentProvider(PaymentProperties props, ObjectMapper mapper) {
        this.config = props.stripe();
        this.mapper = mapper;
        if (config != null && config.secretKey() != null) {
            Stripe.apiKey = config.secretKey();
        }
    }

    @Override
    public PaymentProviderKind kind() {
        return PaymentProviderKind.STRIPE;
    }

    @Override
    public CheckoutSession createSubscriptionCheckout(CheckoutCommand cmd) {
        Map<String, Object> meta = Map.of(
                "workspaceId", cmd.workspaceId().toString(),
                "plan", cmd.plan().name(),
                "interval", cmd.interval().name());
        Map<String, Object> priceData = Map.of(
                "currency", cmd.currency().toLowerCase(),
                "unit_amount", cmd.amountCents(),
                "recurring", Map.of("interval", cmd.interval() == BillingInterval.ANNUAL ? "year" : "month"),
                "product_data", Map.of("name", "Zyntral " + cmd.plan().name() + " (" + cmd.interval().name() + ")"));
        Map<String, Object> params = new HashMap<>();
        params.put("mode", "subscription");
        params.put("success_url", cmd.successUrl());
        params.put("cancel_url", cmd.cancelUrl());
        params.put("customer_email", cmd.customerEmail());
        params.put("client_reference_id", cmd.workspaceId().toString());
        params.put("metadata", meta);
        params.put("subscription_data", Map.of("metadata", meta));
        params.put("line_items", List.of(Map.of("quantity", 1, "price_data", priceData)));
        try {
            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (Exception e) {
            log.error("Stripe checkout creation failed", e);
            throw new ApiException(ErrorCode.INTERNAL);
        }
    }

    @Override
    public void cancelSubscription(String externalSubscriptionId, boolean atPeriodEnd) {
        try {
            com.stripe.model.Subscription sub =
                    com.stripe.model.Subscription.retrieve(externalSubscriptionId);
            if (atPeriodEnd) {
                sub.update(Map.of("cancel_at_period_end", true));
            } else {
                sub.cancel();
            }
        } catch (Exception e) {
            log.error("Stripe cancel failed for {}", externalSubscriptionId, e);
            throw new ApiException(ErrorCode.INTERNAL);
        }
    }

    @Override
    public WebhookOutcome handleWebhook(String rawPayload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(rawPayload, signatureHeader, config.webhookSecret());
        } catch (Exception e) {
            throw new ApiException(ErrorCode.TOKEN_INVALID);   // invalid signature → 400
        }
        try {
            JsonNode obj = mapper.readTree(rawPayload).path("data").path("object");
            return switch (event.getType()) {
                case "checkout.session.completed" -> fromCheckout(event, obj);
                case "customer.subscription.created", "customer.subscription.updated",
                     "customer.subscription.deleted" -> fromSubscription(event, obj);
                case "invoice.paid", "invoice.payment_succeeded", "invoice.finalized" ->
                        fromInvoice(event, obj);
                default -> null;
            };
        } catch (Exception e) {
            log.error("Failed to parse Stripe event {}", event.getId(), e);
            return null;
        }
    }

    private WebhookOutcome fromCheckout(Event event, JsonNode obj) {
        UUID workspaceId = uuid(obj.path("metadata").path("workspaceId").asText(null));
        PlanCode plan = plan(obj.path("metadata").path("plan").asText(null));
        BillingInterval interval = interval(obj.path("metadata").path("interval").asText(null));
        String subId = obj.path("subscription").asText(null);
        var change = new SubscriptionChange(subId, plan, SubscriptionStatus.ACTIVE, interval,
                null, null, false);
        return new WebhookOutcome(event.getId(), event.getType(), workspaceId, subId, change, null);
    }

    private WebhookOutcome fromSubscription(Event event, JsonNode obj) {
        String subId = obj.path("id").asText(null);
        UUID workspaceId = uuid(obj.path("metadata").path("workspaceId").asText(null));
        PlanCode plan = plan(obj.path("metadata").path("plan").asText(null));
        boolean canceled = "customer.subscription.deleted".equals(event.getType());
        SubscriptionStatus status = canceled ? SubscriptionStatus.CANCELED
                : mapStatus(obj.path("status").asText(""));
        String intervalStr = obj.path("items").path("data").path(0)
                .path("price").path("recurring").path("interval").asText("month");
        BillingInterval interval = "year".equals(intervalStr) ? BillingInterval.ANNUAL : BillingInterval.MONTHLY;
        Instant start = epoch(obj.path("current_period_start").asLong(0));
        Instant end = epoch(obj.path("current_period_end").asLong(0));
        var change = new SubscriptionChange(subId, plan, status, interval, start, end, canceled);
        return new WebhookOutcome(event.getId(), event.getType(), workspaceId, subId, change, null);
    }

    private WebhookOutcome fromInvoice(Event event, JsonNode obj) {
        String subId = obj.path("subscription").asText(null);
        boolean paid = !"invoice.finalized".equals(event.getType());
        var change = new InvoiceChange(
                obj.path("id").asText(null),
                obj.path("number").asText(null),
                obj.path("amount_paid").asInt(obj.path("amount_due").asInt(0)),
                obj.path("currency").asText("usd").toUpperCase(),
                paid ? InvoiceStatus.PAID : InvoiceStatus.OPEN,
                obj.path("hosted_invoice_url").asText(null),
                obj.path("invoice_pdf").asText(null));
        return new WebhookOutcome(event.getId(), event.getType(), null, subId, null, change);
    }

    private SubscriptionStatus mapStatus(String s) {
        return switch (s) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due", "unpaid" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "incomplete_expired" -> SubscriptionStatus.EXPIRED;
            default -> SubscriptionStatus.INCOMPLETE;
        };
    }

    private UUID uuid(String s) { return s == null || s.isBlank() ? null : UUID.fromString(s); }
    private PlanCode plan(String s) { return s == null ? null : PlanCode.valueOf(s); }
    private BillingInterval interval(String s) { return s == null ? BillingInterval.MONTHLY : BillingInterval.valueOf(s); }
    private Instant epoch(long sec) { return sec <= 0 ? null : Instant.ofEpochSecond(sec); }
}
