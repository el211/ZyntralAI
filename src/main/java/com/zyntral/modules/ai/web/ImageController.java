package com.zyntral.modules.ai.web;

import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.ai.application.ImageGenerationService;
import com.zyntral.modules.ai.domain.AiImage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "AI Images", description = "Generate and list logos/banners")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/workspaces/{workspaceId}/ai/images")
public class ImageController {

    private final ImageGenerationService service;

    public ImageController(ImageGenerationService service) {
        this.service = service;
    }

    public record GenerateImageRequest(@NotBlank String kind,
                                       @NotBlank @Size(max = 2000) String prompt) {}

    public record ImageResponse(UUID id, String kind, String prompt, Instant createdAt) {
        static ImageResponse of(AiImage i) {
            return new ImageResponse(i.getId(), i.getKind(), i.getPrompt(), i.getCreatedAt());
        }
    }

    @Operation(summary = "Generate a logo or banner")
    @PostMapping
    public ApiResponse<ImageResponse> generate(@PathVariable UUID workspaceId,
                                               @Valid @RequestBody GenerateImageRequest req) {
        AiImage img = service.generate(workspaceId, SecurityUtils.currentUserId(), req.kind(), req.prompt());
        return ApiResponse.ok(ImageResponse.of(img));
    }

    @Operation(summary = "List generated images for the workspace")
    @GetMapping
    public ApiResponse<List<ImageResponse>> list(@PathVariable UUID workspaceId) {
        return ApiResponse.ok(service.list(workspaceId, SecurityUtils.currentUserId())
                .stream().map(ImageResponse::of).toList());
    }
}
