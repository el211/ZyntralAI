package com.zyntral.modules.ai.application;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.ai.domain.AiCreditLedger;
import com.zyntral.modules.ai.domain.AiCreditLedgerRepository;
import com.zyntral.modules.billing.domain.Plan;
import com.zyntral.modules.billing.domain.PlanRepository;
import com.zyntral.modules.workspace.domain.Workspace;
import com.zyntral.modules.workspace.domain.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Enforces per-workspace monthly AI credit limits. Charging is a single conditional UPDATE
 * guarded by the DB CHECK constraint, so concurrent generations can never overspend.
 */
@Service
public class AiCreditService {

    private final AiCreditLedgerRepository ledger;
    private final WorkspaceRepository workspaces;
    private final PlanRepository plans;

    public AiCreditService(AiCreditLedgerRepository ledger, WorkspaceRepository workspaces,
                           PlanRepository plans) {
        this.ledger = ledger;
        this.workspaces = workspaces;
        this.plans = plans;
    }

    public record Usage(int limit, int used, int remaining) {}

    /** Charges {@code cost} credits or throws {@link ErrorCode#AI_CREDITS_EXHAUSTED}. */
    @Transactional
    public void charge(UUID workspaceId, int cost) {
        LocalDate month = currentMonth();
        ledger.ensureLedger(workspaceId, month, monthlyLimit(workspaceId));
        if (ledger.tryConsume(workspaceId, month, cost) == 0) {
            throw new ApiException(ErrorCode.AI_CREDITS_EXHAUSTED);
        }
    }

    /** Returns previously charged credits (used when a provider call fails post-charge). */
    @Transactional
    public void refund(UUID workspaceId, int cost) {
        ledger.refund(workspaceId, currentMonth(), cost);
    }

    /** Admin grant: add extra credits to a workspace for the current month. */
    @Transactional
    public void grant(UUID workspaceId, int amount) {
        LocalDate month = currentMonth();
        ledger.ensureLedger(workspaceId, month, monthlyLimit(workspaceId));
        ledger.grant(workspaceId, month, amount);
    }

    @Transactional
    public Usage usage(UUID workspaceId) {
        LocalDate month = currentMonth();
        int limit = monthlyLimit(workspaceId);
        ledger.ensureLedger(workspaceId, month, limit);
        return ledger.findByIdWorkspaceIdAndIdPeriodMonth(workspaceId, month)
                .map(l -> new Usage(l.getCreditsLimit(), l.getCreditsUsed(), l.getRemaining()))
                .orElse(new Usage(limit, 0, limit));
    }

    private int monthlyLimit(UUID workspaceId) {
        Workspace ws = workspaces.findById(workspaceId)
                .orElseThrow(() -> ApiException.notFound("workspace", workspaceId));
        Plan plan = plans.findById(ws.getPlan()).orElseThrow();
        return plan.getAiCreditsMonthly();
    }

    private LocalDate currentMonth() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
    }
}
