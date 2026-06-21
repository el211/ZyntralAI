package com.zyntral.modules.support.web;

import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.support.application.SupportAgentService;
import com.zyntral.modules.support.web.dto.SupportDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Support agents", description = "Manage embeddable AI support agents and knowledge")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/workspaces/{workspaceId}/support/agents")
public class SupportAgentController {

    private final SupportAgentService service;

    public SupportAgentController(SupportAgentService service) {
        this.service = service;
    }

    @Operation(summary = "Create a support agent (ADMIN+)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AgentResponse> create(@PathVariable UUID workspaceId,
                                             @Valid @RequestBody CreateAgentRequest req) {
        return ApiResponse.ok(AgentResponse.from(service.create(workspaceId,
                SecurityUtils.currentUserId(), req.name(), req.systemPrompt(), req.model())));
    }

    @Operation(summary = "List support agents")
    @GetMapping
    public ApiResponse<List<AgentResponse>> list(@PathVariable UUID workspaceId) {
        return ApiResponse.ok(service.list(workspaceId, SecurityUtils.currentUserId())
                .stream().map(AgentResponse::from).toList());
    }

    @Operation(summary = "Update a support agent (ADMIN+)")
    @PatchMapping("/{agentId}")
    public ApiResponse<AgentResponse> update(@PathVariable UUID workspaceId, @PathVariable UUID agentId,
                                             @Valid @RequestBody UpdateAgentRequest req) {
        return ApiResponse.ok(AgentResponse.from(service.update(workspaceId,
                SecurityUtils.currentUserId(), agentId, req.name(), req.systemPrompt(),
                req.model(), req.active())));
    }

    @Operation(summary = "Delete a support agent (ADMIN+)")
    @DeleteMapping("/{agentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID workspaceId, @PathVariable UUID agentId) {
        service.delete(workspaceId, SecurityUtils.currentUserId(), agentId);
    }

    @Operation(summary = "Add a knowledge entry (ADMIN+)")
    @PostMapping("/{agentId}/knowledge")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KnowledgeResponse> addKnowledge(@PathVariable UUID workspaceId,
                                                       @PathVariable UUID agentId,
                                                       @Valid @RequestBody AddKnowledgeRequest req) {
        return ApiResponse.ok(KnowledgeResponse.from(service.addKnowledge(workspaceId,
                SecurityUtils.currentUserId(), agentId, req.title(), req.content(), req.sourceUrl())));
    }

    @Operation(summary = "List knowledge entries")
    @GetMapping("/{agentId}/knowledge")
    public ApiResponse<List<KnowledgeResponse>> listKnowledge(@PathVariable UUID workspaceId,
                                                             @PathVariable UUID agentId) {
        return ApiResponse.ok(service.listKnowledge(workspaceId, SecurityUtils.currentUserId(), agentId)
                .stream().map(KnowledgeResponse::from).toList());
    }

    @Operation(summary = "Delete a knowledge entry (ADMIN+)")
    @DeleteMapping("/{agentId}/knowledge/{knowledgeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteKnowledge(@PathVariable UUID workspaceId, @PathVariable UUID agentId,
                                @PathVariable UUID knowledgeId) {
        service.deleteKnowledge(workspaceId, SecurityUtils.currentUserId(), agentId, knowledgeId);
    }
}
