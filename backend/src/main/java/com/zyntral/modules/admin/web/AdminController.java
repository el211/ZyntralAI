package com.zyntral.modules.admin.web;

import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.common.web.PageResponse;
import com.zyntral.modules.admin.application.AdminService;
import com.zyntral.modules.admin.web.dto.AdminDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Admin", description = "Cross-tenant administration & analytics (ADMIN only)")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/admin")
@PreAuthorize("hasRole('ADMIN')")     // defence in depth (also enforced by the security chain)
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @Operation(summary = "Platform overview metrics")
    @GetMapping("/overview")
    public ApiResponse<OverviewResponse> overview() {
        return ApiResponse.ok(admin.overview());
    }

    @Operation(summary = "List all users")
    @GetMapping("/users")
    public ApiResponse<PageResponse<UserAdminResponse>> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.ok(admin.listUsers(page, Math.min(size, 200)));
    }

    @Operation(summary = "Suspend a user")
    @PostMapping("/users/{userId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(@PathVariable UUID userId) {
        admin.setUserSuspended(userId, true);
    }

    @Operation(summary = "Reactivate a user")
    @PostMapping("/users/{userId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable UUID userId) {
        admin.setUserSuspended(userId, false);
    }

    @Operation(summary = "Delete (deactivate) a user")
    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId) {
        admin.deleteUser(userId);
    }

    @Operation(summary = "Grant AI credits to a user's workspaces")
    @PostMapping("/users/{userId}/credits")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grantCredits(@PathVariable UUID userId, @Valid @RequestBody GrantCreditsRequest req) {
        admin.grantCredits(userId, req.amount());
    }

    @Operation(summary = "Set a user's plan (free upgrade/downgrade)")
    @PostMapping("/users/{userId}/plan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPlan(@PathVariable UUID userId, @Valid @RequestBody SetPlanRequest req) {
        admin.setPlan(userId, req.plan());
    }

    @Operation(summary = "Impersonate a user (force-login)")
    @PostMapping("/users/{userId}/impersonate")
    public ApiResponse<ImpersonateResponse> impersonate(@PathVariable UUID userId) {
        return ApiResponse.ok(admin.impersonate(userId));
    }

    @Operation(summary = "List all subscriptions")
    @GetMapping("/subscriptions")
    public ApiResponse<PageResponse<SubscriptionAdminResponse>> subscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.ok(admin.listSubscriptions(page, Math.min(size, 200)));
    }

    @Operation(summary = "AI usage by provider")
    @GetMapping("/analytics/ai")
    public ApiResponse<Map<String, Long>> aiUsage() {
        return ApiResponse.ok(admin.aiUsageByProvider());
    }

    @Operation(summary = "Social account statistics by platform")
    @GetMapping("/analytics/social")
    public ApiResponse<Map<String, Long>> socialStats() {
        return ApiResponse.ok(admin.socialStatsByPlatform());
    }
}
