package com.zyntral.modules.ai.web;

import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.ai.application.ProviderKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "AI", description = "AI content generation, history, and credit usage")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/workspaces/{workspaceId}/ai/provider-keys")
public class ProviderKeyController {

    private final ProviderKeyService service;

    public ProviderKeyController(ProviderKeyService service) {
        this.service = service;
    }

    @Operation(summary = "List which providers have a BYOK key configured (never returns key values)")
    @GetMapping
    public ApiResponse<List<ProviderKeyService.ProviderStatus>> list(@PathVariable UUID workspaceId) {
        return ApiResponse.ok(service.listStatuses(workspaceId, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "Save (or replace) a BYOK API key for a provider")
    @PutMapping("/{provider}")
    public ApiResponse<Void> save(@PathVariable UUID workspaceId,
                                  @PathVariable String provider,
                                  @Valid @RequestBody SaveKeyRequest req) {
        service.save(workspaceId, SecurityUtils.currentUserId(), provider, req.apiKey());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "Remove a BYOK API key for a provider")
    @DeleteMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID workspaceId, @PathVariable String provider) {
        service.delete(workspaceId, SecurityUtils.currentUserId(), provider);
    }

    record SaveKeyRequest(@NotBlank @Size(max = 512) String apiKey) {}
}
