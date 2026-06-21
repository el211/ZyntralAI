package com.zyntral.modules.auth.web;

import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.auth.application.AuthService;
import com.zyntral.modules.auth.web.dto.AuthDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Registration, login, token refresh, verification, password reset")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @Operation(summary = "Register a new account (sends a verification email)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest req) {
        auth.register(req);
    }

    @Operation(summary = "Authenticate and receive access + refresh tokens")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req,
                                            HttpServletRequest http) {
        return ApiResponse.ok(auth.login(req, userAgent(http), clientIp(http)));
    }

    @Operation(summary = "Exchange a refresh token for a new token pair (rotation)")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req,
                                              HttpServletRequest http) {
        return ApiResponse.ok(auth.refresh(req.refreshToken(), userAgent(http), clientIp(http)));
    }

    @Operation(summary = "Revoke a refresh token (logout)")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req.refreshToken());
    }

    @Operation(summary = "Verify an email address with the emailed token")
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        auth.verifyEmail(req.token());
    }

    @Operation(summary = "Request a password-reset email (always succeeds)")
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        auth.requestPasswordReset(req.email());
    }

    @Operation(summary = "Reset a password using the emailed token")
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        auth.resetPassword(req.token(), req.newPassword());
    }

    private String userAgent(HttpServletRequest req) {
        return req.getHeader("User-Agent");
    }

    private String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
