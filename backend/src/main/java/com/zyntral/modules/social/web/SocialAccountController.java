package com.zyntral.modules.social.web;

import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.social.application.SocialAccountService;
import com.zyntral.modules.social.web.dto.SocialDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Social accounts", description = "Connect and manage social media accounts")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/workspaces/{workspaceId}/social-accounts")
public class SocialAccountController {

    private final SocialAccountService service;

    public SocialAccountController(SocialAccountService service) {
        this.service = service;
    }

    @Operation(summary = "Connect a social account (ADMIN+)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SocialAccountResponse> connect(@PathVariable UUID workspaceId,
                                                      @Valid @RequestBody ConnectAccountRequest req) {
        return ApiResponse.ok(service.connect(workspaceId, SecurityUtils.currentUserId(), req));
    }

    @Operation(summary = "List connected accounts")
    @GetMapping
    public ApiResponse<List<SocialAccountResponse>> list(@PathVariable UUID workspaceId) {
        return ApiResponse.ok(service.list(workspaceId, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "Disconnect a social account (ADMIN+)")
    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(@PathVariable UUID workspaceId, @PathVariable UUID accountId) {
        service.disconnect(workspaceId, SecurityUtils.currentUserId(), accountId);
    }
}
