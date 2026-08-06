package com.zyntral.modules.billing.web;

import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.billing.domain.PlanRepository;
import com.zyntral.modules.billing.web.dto.BillingDtos.PlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Plans", description = "Subscription plan catalogue")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/billing/plans")
public class PlanController {

    private final PlanRepository plans;

    public PlanController(PlanRepository plans) {
        this.plans = plans;
    }

    @Operation(summary = "List available subscription plans")
    @GetMapping
    public ApiResponse<List<PlanResponse>> list() {
        return ApiResponse.ok(plans.findAll().stream()
                .filter(p -> p.isActive())
                .map(PlanResponse::from)
                .toList());
    }
}
