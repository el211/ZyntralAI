package com.zyntral.common.domain;

/**
 * Subscription tier. Shared kernel concept: referenced by workspace (current tier) and
 * billing (subscriptions, limits). Mirrors the PostgreSQL {@code plan_code} enum.
 */
public enum PlanCode {
    FREE, PRO, BUSINESS
}
