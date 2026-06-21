package com.zyntral.modules.ai.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Per-workspace, per-month AI credit counter. The DB {@code CHECK (credits_used <= credits_limit)}
 * plus a conditional UPDATE (see {@link AiCreditLedgerRepository#tryConsume}) make credit
 * spending atomic and race-free even under concurrent generations.
 */
@Entity
@Table(name = "ai_credit_ledger")
public class AiCreditLedger {

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "workspace_id")
        private UUID workspaceId;
        @Column(name = "period_month")
        private LocalDate periodMonth;

        protected Id() {}
        public Id(UUID workspaceId, LocalDate periodMonth) {
            this.workspaceId = workspaceId;
            this.periodMonth = periodMonth;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id id)) return false;
            return Objects.equals(workspaceId, id.workspaceId)
                    && Objects.equals(periodMonth, id.periodMonth);
        }
        @Override public int hashCode() { return Objects.hash(workspaceId, periodMonth); }
    }

    @EmbeddedId
    private Id id;

    @Column(name = "credits_limit", nullable = false)
    private int creditsLimit;

    @Column(name = "credits_used", nullable = false)
    private int creditsUsed;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected AiCreditLedger() {}

    public int getCreditsLimit() { return creditsLimit; }
    public int getCreditsUsed() { return creditsUsed; }
    public int getRemaining() { return creditsLimit - creditsUsed; }
}
