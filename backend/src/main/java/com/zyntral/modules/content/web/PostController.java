package com.zyntral.modules.content.web;

import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.common.web.PageResponse;
import com.zyntral.modules.content.application.PostService;
import com.zyntral.modules.content.domain.PostStatus;
import com.zyntral.modules.content.web.dto.ContentDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Content", description = "Posts: drafts, scheduling, publishing, calendar, queue")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/workspaces/{workspaceId}/posts")
public class PostController {

    private final PostService service;

    public PostController(PostService service) {
        this.service = service;
    }

    @Operation(summary = "Create a post (draft)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> create(@PathVariable UUID workspaceId,
                                            @Valid @RequestBody CreatePostRequest req) {
        return ApiResponse.ok(service.create(workspaceId, SecurityUtils.currentUserId(), req));
    }

    @Operation(summary = "List posts (optionally filtered by status)")
    @GetMapping
    public ApiResponse<PageResponse<PostResponse>> list(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(workspaceId, SecurityUtils.currentUserId(),
                status, page, Math.min(size, 100)));
    }

    @Operation(summary = "Content calendar: posts scheduled in a date range")
    @GetMapping("/calendar")
    public ApiResponse<List<PostResponse>> calendar(
            @PathVariable UUID workspaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ApiResponse.ok(service.calendar(workspaceId, SecurityUtils.currentUserId(), from, to));
    }

    @Operation(summary = "Get a post")
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> get(@PathVariable UUID workspaceId, @PathVariable UUID postId) {
        return ApiResponse.ok(service.get(workspaceId, SecurityUtils.currentUserId(), postId));
    }

    @Operation(summary = "Update a post (drafts/scheduled only)")
    @PatchMapping("/{postId}")
    public ApiResponse<PostResponse> update(@PathVariable UUID workspaceId, @PathVariable UUID postId,
                                            @Valid @RequestBody UpdatePostRequest req) {
        return ApiResponse.ok(service.update(workspaceId, SecurityUtils.currentUserId(), postId, req));
    }

    @Operation(summary = "Schedule a post for a future time")
    @PostMapping("/{postId}/schedule")
    public ApiResponse<PostResponse> schedule(@PathVariable UUID workspaceId, @PathVariable UUID postId,
                                              @Valid @RequestBody SchedulePostRequest req) {
        return ApiResponse.ok(service.schedule(workspaceId, SecurityUtils.currentUserId(),
                postId, req.scheduledAt()));
    }

    @Operation(summary = "Publish a post immediately")
    @PostMapping("/{postId}/publish")
    public ApiResponse<PostResponse> publishNow(@PathVariable UUID workspaceId,
                                                @PathVariable UUID postId) {
        return ApiResponse.ok(service.publishNow(workspaceId, SecurityUtils.currentUserId(), postId));
    }

    @Operation(summary = "Cancel a scheduled/queued post")
    @PostMapping("/{postId}/cancel")
    public ApiResponse<PostResponse> cancel(@PathVariable UUID workspaceId, @PathVariable UUID postId) {
        return ApiResponse.ok(service.cancel(workspaceId, SecurityUtils.currentUserId(), postId));
    }

    @Operation(summary = "Delete a post")
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID workspaceId, @PathVariable UUID postId) {
        service.delete(workspaceId, SecurityUtils.currentUserId(), postId);
    }
}
