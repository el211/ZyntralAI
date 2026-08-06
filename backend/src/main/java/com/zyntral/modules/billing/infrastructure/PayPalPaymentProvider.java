package com.zyntral.modules.billing.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyntral.common.domain.PlanCode;
import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.billing.application.PaymentProvider;
import com.zyntral.modules.billing.config.PaymentProperties;
import com.zyntral.modules.billing.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PayPal adapter (REST). OAuth2 client-credentials for tokens; subscriptions are created
 * against pre-configured PayPal billing plans (see {@code zyntral.payments.paypal.planIds}).
 * Webhooks are normalized to the same {@link WebhookOutcome} as Stripe.
 */
@Component
public class PayPalPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(PayPalPaymentProvider.class);

    private final PaymentProperties.PayPal config;
    private final ObjectMapper mapper;
    private final RestClient client;

    public PayPalPaymentProvider(PaymentProperties props, ObjectMapper mapper) {
        this.config = props.paypal();
        this.mapper = mapper;
        this.client = RestClient.builder()
                .baseUrl(config != null && config.baseUrl() != null
                        ? config.baseUrl() : "https://api-m.sandbox.paypal.com")
                .build();
    }

    @Override
    public PaymentProviderKind kind() {
        return PaymentProviderKind.PAYPAL;
    }

    @Override
    public CheckoutSession createSubscriptionCheckout(CheckoutCommand cmd) {
        String planId = planId(cmd.plan(), cmd.interval());
        if (planId == null) {
            log.error("No PayPal plan id configured for {}_{}", cmd.plan(), cmd.interval());
            throw new ApiException(ErrorCode.BUSINESS_RULE,
                    new Object[]{"PayPal is not configured for this plan"});
        }
        Map<String, Object> body = Map.of(
                "plan_id", planId,
                "custom_id", cmd.workspaceId().toString(),
                "subscriber", Map.of("email_address", cmd.customerEmail()),
                "application_context", Map.of(
                        "return_url", cmd.successUrl(),
                        "cancel_url", cmd.cancelUrl()));
        try {
            JsonNode res = client.post()
                    .uri("/v1/billing/subscriptions")
                    .header("Authorization", "Bearer " + accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String id = res.path("id").asText();
            String approveUrl = null;
            for (JsonNode link : res.path("links")) {
                if ("approve".equals(link.path("rel").asText())) {
                    approveUrl = link.path("href").asText();
                }
            }
            return new CheckoutSession(id, approveUrl);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayPal subscription creation failed", e);
            throw new ApiException(ErrorCode.INTERNAL);
        }
    }

    @Override
    public void cancelSubscription(String externalSubscriptionId, boolean atPeriodEnd) {
        try {
            client.post()
                    .uri("/v1/billing/subscriptions/{id}/cancel", externalSubscriptionId)
                    .header("Authorization", "Bearer " + accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("reason", atPeriodEnd ? "Cancel at period end" : "Cancel immediately"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("PayPal cancel failed for {}", externalSubscriptionId, e);
            throw new ApiException(ErrorCode.INTERNAL);
        }
    }

    @Override
    public WebhookOutcome handleWebhook(String rawPayload, String signatureHeader) {
        try {
            JsonNode root = mapper.readTree(rawPayload);
            String eventId = root.path("id").asText();
            String type = root.path("event_type").asText();
            JsonNode resource = root.path("resource");
            return switch (type) {
                case "BILLING.SUBSCRIPTION.ACTIVATED", "BILLING.SUBSCRIPTION.CREATED" ->
                        subscriptionOutcome(eventId, type, resource, SubscriptionStatus.ACTIVE, false);
                case "BILLING.SUBSCRIPTION.CANCELLED", "BILLING.SUBSCRIPTION.EXPIRED" ->
                        subscriptionOutcome(eventId, type, resource, SubscriptionStatus.CANCELED, true);
                case "BILLING.SUBSCRIPTION.SUSPENDED" ->
                        subscriptionOutcome(eventId, type, resource, SubscriptionStatus.PAST_DUE, false);
                case "PAYMENT.SALE.COMPLETED" -> invoiceOutcome(eventId, type, resource);
                default -> null;
            };
        } catch (Exception e) {
            log.error("Failed to parse PayPal webhook", e);
            return null;
        }
    }

    private WebhookOutcome subscriptionOutcome(String eventId, String type, JsonNode resource,
                                               SubscriptionStatus status, boolean canceled) {
        String subId = resource.path("id").asText(null);
        UUID workspaceId = uuid(resource.path("custom_id").asText(null));
        var change = new SubscriptionChange(subId, null, status, BillingInterval.MONTHLY,
                null, null, canceled);
        return new WebhookOutcome(eventId, type, workspaceId, subId, change, null);
    }

    private WebhookOutcome invoiceOutcome(String eventId, String type, JsonNode resource) {
        String subId = resource.path("billing_agreement_id").asText(null);
        int amountCents = (int) Math.round(
                resource.path("amount").path("total").asDouble(0) * 100);
        var change = new InvoiceChange(resource.path("id").asText(null), null, amountCents,
                resource.path("amount").path("currency").asText("USD"),
                InvoiceStatus.PAID, null, null);
        return new WebhookOutcome(eventId, type, null, subId, null, change);
    }

    private String accessToken() {
        try {
            JsonNode res = client.post()
                    .uri("/v1/oauth2/token")
                    .headers(h -> h.setBasicAuth(config.clientId(), config.clientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(JsonNode.class);
            return res.path("access_token").asText();
        } catch (Exception e) {
            log.error("PayPal OAuth failed", e);
            throw new ApiException(ErrorCode.INTERNAL);
        }
    }

    private String planId(PlanCode plan, BillingInterval interval) {
        if (config.planIds() == null) return null;
        return config.planIds().get(plan.name() + "_" + interval.name());
    }

    private UUID uuid(String s) { return s == null || s.isBlank() ? null : UUID.fromString(s); }
}
