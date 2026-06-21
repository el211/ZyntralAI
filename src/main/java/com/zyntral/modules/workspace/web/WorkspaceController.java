package com.zyntral.modules.workspace.web;

import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.workspace.application.WorkspaceService;
import com.zyntral.modules.workspace.web.dto.WorkspaceDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Workspaces", description = "Tenant management: workspaces, members, invitations")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/workspaces")
public class WorkspaceController {

    private final WorkspaceService service;

    public WorkspaceController(WorkspaceService service) {
        this.service = service;
    }

    @Operation(summary = "Create a workspace")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkspaceResponse> create(@Valid @RequestBody CreateWorkspaceRequest req) {
        return ApiResponse.ok(service.create(SecurityUtils.currentUserId(), req));
    }

    @Operation(summary = "List the workspaces I belong to")
    @GetMapping
    public ApiResponse<List<WorkspaceResponse>> listMine() {
        return ApiResponse.ok(service.listMine(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "Get a workspace")
    @GetMapping("/{id}")
    public ApiResponse<WorkspaceResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "Update a workspace (ADMIN+)")
    @PatchMapping("/{id}")
    public ApiResponse<WorkspaceResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateWorkspaceRequest req) {
        return ApiResponse.ok(service.update(id, SecurityUtils.currentUserId(), req));
    }

    @Operation(summary = "List members")
    @GetMapping("/{id}/members")
    public ApiResponse<List<MemberResponse>> members(@PathVariable UUID id) {
        return ApiResponse.ok(service.listMembers(id, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "Change a member's role (manager only)")
    @PatchMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMemberRole(@PathVariable UUID id, @PathVariable UUID userId,
                                 @Valid @RequestBody UpdateMemberRoleRequest req) {
        service.updateMemberRole(id, SecurityUtils.currentUserId(), userId, req);
    }

    @Operation(summary = "Remove a member (manager only)")
    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        service.removeMember(id, SecurityUtils.currentUserId(), userId);
    }

    @Operation(summary = "Leave a workspace")
    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable UUID id) {
        service.leave(id, SecurityUtils.currentUserId());
    }

    @Operation(summary = "Invite a member by email (manager only)")
    @PostMapping("/{id}/invitations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void invite(@PathVariable UUID id, @Valid @RequestBody InviteMemberRequest req) {
        service.invite(id, SecurityUtils.currentUserId(), req);
    }

    @Operation(summary = "List pending invitations (manager only)")
    @GetMapping("/{id}/invitations")
    public ApiResponse<List<InvitationResponse>> invitations(@PathVariable UUID id) {
        return ApiResponse.ok(service.listInvitations(id, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "Accept a workspace invitation")
    @PostMapping("/invitations/accept")
    public ApiResponse<WorkspaceResponse> accept(@Valid @RequestBody AcceptInvitationRequest req) {
        return ApiResponse.ok(service.acceptInvitation(SecurityUtils.currentUserId(), req.token()));
    }
}
