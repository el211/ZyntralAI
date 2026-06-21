package com.zyntral.modules.admin.web;

import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.common.web.PageResponse;
import com.zyntral.modules.admin.application.AdminService;
import com.zyntral.modules.admin.web.dto.AdminDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
