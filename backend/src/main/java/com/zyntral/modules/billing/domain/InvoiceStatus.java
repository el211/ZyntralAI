package com.zyntral.modules.billing.domain;

/** Mirrors the PostgreSQL {@code invoice_status} enum. */
public enum InvoiceStatus {
    DRAFT, OPEN, PAID, VOID, UNCOLLECTIBLE, REFUNDED
}
