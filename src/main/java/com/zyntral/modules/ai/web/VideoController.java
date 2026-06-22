package com.zyntral.modules.ai.web;

import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.ai.application.VideoGenerationService;
import com.zyntral.modules.ai.domain.AiVideo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.zyntral.modules.ai.application.VideoGenerationService.VideoOptions;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "AI Videos", description = "Generate short videos (Veo)")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/workspaces/{workspaceId}/ai/videos")
public class VideoController {

    private final VideoGenerationService service;

    public VideoController(VideoGenerationService service) {
        this.service = service;
    }

    public record GenerateVideoRequest(@NotBlank @Size(max = 2000) String prompt, String aspectRatio,
                                       String resolution, Integer durationSeconds) {}

    public record ExtendVideoRequest(@Size(max = 2000) String prompt) {}

    public record VideoResponse(UUID id, String status, String prompt, String error, Instant createdAt) {
        static VideoResponse of(AiVideo v) {
            return new VideoResponse(v.getId(), v.getStatus(), v.getPrompt(), v.getError(), v.getCreatedAt());
        }
    }

    @Operation(summary = "Start a text-to-video generation (async; poll for status)")
    @PostMapping
    public ApiResponse<VideoResponse> generate(@PathVariable UUID workspaceId,
                                               @Valid @RequestBody GenerateVideoRequest req) {
        VideoOptions opts = new VideoOptions(req.aspectRatio(), req.resolution(), req.durationSeconds(), null, null);
        return ApiResponse.ok(VideoResponse.of(
                service.submit(workspaceId, SecurityUtils.currentUserId(), req.prompt(), opts)));
    }

    @Operation(summary = "Image-to-video: animate a starting image (async)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VideoResponse> generateFromImage(@PathVariable UUID workspaceId,
                                                        @RequestParam String prompt,
                                                        @RequestParam(required = false) String aspectRatio,
                                                        @RequestParam(required = false) String resolution,
                                                        @RequestParam(required = false) Integer durationSeconds,
                                                        @RequestParam("image") MultipartFile image) throws IOException {
        VideoOptions opts = new VideoOptions(aspectRatio, resolution, durationSeconds,
                image.getBytes(), image.getContentType());
        return ApiResponse.ok(VideoResponse.of(
                service.submit(workspaceId, SecurityUtils.currentUserId(), prompt, opts)));
    }

    @Operation(summary = "Extend a previously generated video (async)")
    @PostMapping("/{id}/extend")
    public ApiResponse<VideoResponse> extend(@PathVariable UUID workspaceId, @PathVariable UUID id,
                                             @Valid @RequestBody ExtendVideoRequest req) {
        return ApiResponse.ok(VideoResponse.of(
                service.extend(workspaceId, SecurityUtils.currentUserId(), id, req.prompt())));
    }

    @Operation(summary = "List videos for the workspace")
    @GetMapping
    public ApiResponse<List<VideoResponse>> list(@PathVariable UUID workspaceId) {
        return ApiResponse.ok(service.list(workspaceId, SecurityUtils.currentUserId())
                .stream().map(VideoResponse::of).toList());
    }

    @Operation(summary = "Get one video's status")
    @GetMapping("/{id}")
    public ApiResponse<VideoResponse> get(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        return ApiResponse.ok(VideoResponse.of(
                service.get(workspaceId, SecurityUtils.currentUserId(), id)));
    }
}
