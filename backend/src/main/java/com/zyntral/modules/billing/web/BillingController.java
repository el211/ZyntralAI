package com.zyntral.modules.billing.web;

import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.common.web.PageResponse;
import com.zyntral.modules.billing.application.BillingService;
import com.zyntral.modules.billing.application.PaymentProvider.CheckoutSession;
import com.zyntral.modules.billing.web.dto.BillingDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Billing", description = "Subscriptions, checkout, cancellation, invoices")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/workspaces/{workspaceId}/billing")
public class BillingController {

    private final BillingService billing;

    public BillingController(BillingService billing) {
        this.billing = billing;
    }

    @Operation(summary = "Start a subscription checkout (OWNER)")
    @PostMapping("/checkout")
    public ApiResponse<CheckoutResponse> checkout(@PathVariable UUID workspaceId,
                                                  @Valid @RequestBody CreateCheckoutRequest req) {
        CheckoutSession session = billing.createCheckout(workspaceId, SecurityUtils.currentUserId(),
                req.provider(), req.plan(), req.interval(), req.successUrl(), req.cancelUrl());
        return ApiResponse.ok(new CheckoutResponse(session.url(), session.externalId()));
    }

    @Operation(summary = "Cancel the active subscription (OWNER)")
    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID workspaceId, @RequestBody CancelRequest req) {
        billing.cancel(workspaceId, SecurityUtils.currentUserId(), req.atPeriodEnd());
    }

    @Operation(summary = "Get the current subscription")
    @GetMapping("/subscription")
    public ApiResponse<SubscriptionResponse> subscription(@PathVariable UUID workspaceId) {
        return ApiResponse.ok(SubscriptionResponse.from(
                billing.getCurrentSubscription(workspaceId, SecurityUtils.currentUserId())));
    }

    @Operation(summary = "List invoices / billing history (ADMIN+)")
    @GetMapping("/invoices")
    public ApiResponse<PageResponse<InvoiceResponse>> invoices(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = billing.listInvoices(workspaceId, SecurityUtils.currentUserId(),
                page, Math.min(size, 100));
        return ApiResponse.ok(new PageResponse<>(
                result.items().stream().map(InvoiceResponse::from).toList(),
                result.page(), result.size(), result.totalElements(),
                result.totalPages(), result.hasNext()));
    }
}
