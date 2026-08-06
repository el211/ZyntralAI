package com.zyntral.modules.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/** Binds {@code zyntral.payments.*}. */
@ConfigurationProperties(prefix = "zyntral.payments")
public record PaymentProperties(Stripe stripe, PayPal paypal) {

    public record Stripe(String secretKey, String webhookSecret) {}

    /**
     * PayPal subscriptions require pre-created billing plans. {@code planIds} maps
     * {@code "<PLAN>_<INTERVAL>"} (e.g. {@code PRO_MONTHLY}) to a PayPal plan id.
     */
    public record PayPal(String clientId, String clientSecret, String baseUrl,
                         String webhookId, Map<String, String> planIds) {}
}
