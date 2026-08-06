package com.zyntral.modules.billing.domain;

/** Payment vendor. Mirrors the PostgreSQL {@code payment_provider} enum. */
public enum PaymentProviderKind {
    STRIPE, PAYPAL
}
