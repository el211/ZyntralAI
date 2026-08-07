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

    /** Half-credit scale: 1 credit = 2 internal units, matching AiGenerationService.CREDIT_SCALE. */
    private static final int CREDIT_SCALE = 2;

    public record Usage(double limit, double used, double remaining) {}

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
        int rawLimit = monthlyLimit(workspaceId);
        ledger.ensureLedger(workspaceId, month, rawLimit);
        return ledger.findByIdWorkspaceIdAndIdPeriodMonth(workspaceId, month)
                .map(l -> new Usage(
                        l.getCreditsLimit() / (double) CREDIT_SCALE,
                        l.getCreditsUsed()  / (double) CREDIT_SCALE,
                        l.getRemaining()    / (double) CREDIT_SCALE))
                .orElse(new Usage(rawLimit / (double) CREDIT_SCALE, 0, rawLimit / (double) CREDIT_SCALE));
    }

    private int monthlyLimit(UUID workspaceId) {
        Workspace ws = workspaces.findById(workspaceId)
                .orElseThrow(() -> ApiException.notFound("workspace", workspaceId));
        Plan plan = plans.findById(ws.getPlan()).orElseThrow();
        // Stored in half-credit units (CREDIT_SCALE = 2) so 50 display credits = 100 units
        return plan.getAiCreditsMonthly() * CREDIT_SCALE;
    }

    private LocalDate currentMonth() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
    }
}
