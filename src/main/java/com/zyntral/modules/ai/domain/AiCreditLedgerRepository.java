package com.zyntral.modules.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AiCreditLedgerRepository
        extends JpaRepository<AiCreditLedger, AiCreditLedger.Id> {

    Optional<AiCreditLedger> findByIdWorkspaceIdAndIdPeriodMonth(UUID workspaceId, LocalDate month);

    /** Creates the ledger row for the period if absent. Idempotent (ON CONFLICT DO NOTHING). */
    @Modifying
    @Query(value = """
            INSERT INTO ai_credit_ledger (workspace_id, period_month, credits_limit, credits_used)
            VALUES (:workspaceId, :month, :limit, 0)
            ON CONFLICT (workspace_id, period_month) DO NOTHING
            """, nativeQuery = true)
    void ensureLedger(@Param("workspaceId") UUID workspaceId,
                      @Param("month") LocalDate month,
                      @Param("limit") int limit);

    /**
     * Atomically charges {@code cost} credits iff the balance allows it. Returns the number of
     * rows updated: 1 = charged, 0 = insufficient credits. No row-level locking needed — the
     * WHERE clause is the guard.
     */
    @Modifying
    @Query(value = """
            UPDATE ai_credit_ledger
               SET credits_used = credits_used + :cost, updated_at = now()
             WHERE workspace_id = :workspaceId AND period_month = :month
               AND credits_used + :cost <= credits_limit
            """, nativeQuery = true)
    int tryConsume(@Param("workspaceId") UUID workspaceId,
                   @Param("month") LocalDate month,
                   @Param("cost") int cost);

    /** Returns credits to the balance (e.g. when a provider call fails after charging). */
    @Modifying
    @Query(value = """
            UPDATE ai_credit_ledger
               SET credits_used = GREATEST(0, credits_used - :cost), updated_at = now()
             WHERE workspace_id = :workspaceId AND period_month = :month
            """, nativeQuery = true)
    int refund(@Param("workspaceId") UUID workspaceId,
               @Param("month") LocalDate month,
               @Param("cost") int cost);
}
