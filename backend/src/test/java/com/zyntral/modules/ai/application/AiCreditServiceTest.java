package com.zyntral.modules.ai.application;

import com.zyntral.common.domain.PlanCode;
import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.ai.domain.AiCreditLedgerRepository;
import com.zyntral.modules.billing.domain.Plan;
import com.zyntral.modules.billing.domain.PlanRepository;
import com.zyntral.modules.workspace.domain.Workspace;
import com.zyntral.modules.workspace.domain.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCreditServiceTest {

    @Mock AiCreditLedgerRepository ledger;
    @Mock WorkspaceRepository workspaces;
    @Mock PlanRepository plans;
    @InjectMocks AiCreditService service;

    private final UUID workspaceId = UUID.randomUUID();

    private void stubPlanLimit(int monthly) {
        Workspace ws = org.mockito.Mockito.mock(Workspace.class);
        when(ws.getPlan()).thenReturn(PlanCode.FREE);
        when(workspaces.findById(workspaceId)).thenReturn(Optional.of(ws));
        Plan plan = org.mockito.Mockito.mock(Plan.class);
        when(plan.getAiCreditsMonthly()).thenReturn(monthly);
        when(plans.findById(PlanCode.FREE)).thenReturn(Optional.of(plan));
    }

    @Test
    void chargeSucceedsWhenCreditsAvailable() {
        stubPlanLimit(50);
        when(ledger.tryConsume(eq(workspaceId), any(LocalDate.class), anyInt())).thenReturn(1);

        service.charge(workspaceId, 2);   // should not throw
    }

    @Test
    void chargeThrowsWhenCreditsExhausted() {
        stubPlanLimit(50);
        when(ledger.tryConsume(eq(workspaceId), any(LocalDate.class), anyInt())).thenReturn(0);

        assertThatThrownBy(() -> service.charge(workspaceId, 2))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.AI_CREDITS_EXHAUSTED);
    }

    @Test
    void usageReportsRemaining() {
        stubPlanLimit(50);
        var l = org.mockito.Mockito.mock(com.zyntral.modules.ai.domain.AiCreditLedger.class);
        when(l.getCreditsLimit()).thenReturn(50);
        when(l.getCreditsUsed()).thenReturn(10);
        when(l.getRemaining()).thenReturn(40);
        when(ledger.findByIdWorkspaceIdAndIdPeriodMonth(eq(workspaceId), any(LocalDate.class)))
                .thenReturn(Optional.of(l));

        AiCreditService.Usage usage = service.usage(workspaceId);

        assertThat(usage.remaining()).isEqualTo(40);
        assertThat(usage.limit()).isEqualTo(50);
    }
}
