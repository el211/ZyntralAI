package com.zyntral.modules.admin.web;

import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.admin.application.AdminService;
import com.zyntral.modules.admin.web.dto.AdminDtos.AdminLoginRequest;
import com.zyntral.modules.admin.web.dto.AdminDtos.AdminTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-panel login against env credentials. Public (permitted in SecurityConfig). */
@Tag(name = "Admin", description = "Admin-panel authentication")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/admin")
public class AdminAuthController {

    private final AdminService admin;

    public AdminAuthController(AdminService admin) {
        this.admin = admin;
    }

    @Operation(summary = "Admin-panel login (env credentials) → admin token")
    @PostMapping("/login")
    public ApiResponse<AdminTokenResponse> login(@Valid @RequestBody AdminLoginRequest req) {
        return ApiResponse.ok(new AdminTokenResponse(admin.login(req.username(), req.password())));
    }
}
