package com.zyntral.modules.billing.domain;

/** Billing cadence. Mirrors the PostgreSQL {@code billing_interval} enum. */
public enum BillingInterval {
    MONTHLY, ANNUAL
}
