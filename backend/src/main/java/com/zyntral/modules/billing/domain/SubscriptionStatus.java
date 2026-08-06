package com.zyntral.modules.billing.domain;

/** Mirrors the PostgreSQL {@code subscription_status} enum. */
public enum SubscriptionStatus {
    TRIALING, ACTIVE, PAST_DUE, CANCELED, INCOMPLETE, EXPIRED;

    public boolean isLive() {
        return this == TRIALING || this == ACTIVE || this == PAST_DUE;
    }
}
