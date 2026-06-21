package com.zyntral.modules.ai.web;

import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.common.web.PageResponse;
import com.zyntral.modules.ai.application.AiCreditService;
import com.zyntral.modules.ai.application.AiGenerationService;
import com.zyntral.modules.ai.application.AiGenerationService.GenerateCommand;
import com.zyntral.modules.ai.application.AiGenerationService.GenerationResult;
import com.zyntral.modules.ai.web.dto.AiDtos.*;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "AI", description = "AI content generation, history, and credit usage")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/workspaces/{workspaceId}/ai")
public class AiController {

    private final AiGenerationService generation;
    private final AiCreditService credits;
    private final WorkspaceAccess access;

    public AiController(AiGenerationService generation, AiCreditService credits,
                        WorkspaceAccess access) {
        this.generation = generation;
        this.credits = credits;
        this.access = access;
    }

    @Operation(summary = "Generate AI content (charges credits)")
    @PostMapping("/generate")
    public ApiResponse<GenerationResult> generate(@PathVariable UUID workspaceId,
                                                  @Valid @RequestBody GenerateRequest req) {
        GenerateCommand cmd = new GenerateCommand(
                workspaceId, SecurityUtils.currentUserId(),
                req.contentKind(), req.tone(), req.length(), req.language(),
                req.topic(), req.extraContext(), req.provider(), req.model());
        return ApiResponse.ok(generation.generate(cmd));
    }

    @Operation(summary = "List generation history for the workspace")
    @GetMapping("/generations")
    public ApiResponse<PageResponse<GenerationResult>> history(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                generation.history(workspaceId, SecurityUtils.currentUserId(), page, Math.min(size, 100)));
    }

    @Operation(summary = "Get this month's AI credit usage")
    @GetMapping("/credits")
    public ApiResponse<CreditUsageResponse> credits(@PathVariable UUID workspaceId) {
        access.requireMember(workspaceId, SecurityUtils.currentUserId());
        var u = credits.usage(workspaceId);
        return ApiResponse.ok(new CreditUsageResponse(u.limit(), u.used(), u.remaining()));
    }
}
