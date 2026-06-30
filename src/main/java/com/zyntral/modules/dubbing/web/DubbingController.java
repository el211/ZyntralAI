package com.zyntral.modules.dubbing.web;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.common.web.PageResponse;
import com.zyntral.modules.dubbing.application.DubbingService;
import com.zyntral.modules.dubbing.infrastructure.ElevenLabsDubbingClient.DownloadedMedia;
import com.zyntral.modules.dubbing.web.dto.DubbingDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Tag(name = "Dubbing", description = "Translate a video into another language in the speaker's own voice (ElevenLabs, BYOK)")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/workspaces/{workspaceId}/dubbing")
public class DubbingController {

    private final DubbingService dubbing;

    public DubbingController(DubbingService dubbing) {
        this.dubbing = dubbing;
    }

    @Operation(summary = "Whether this workspace has an ElevenLabs API key saved")
    @GetMapping("/credential")
    public ApiResponse<CredentialStatusResponse> credentialStatus(@PathVariable UUID workspaceId) {
        boolean configured = dubbing.hasCredential(workspaceId, SecurityUtils.currentUserId());
        return ApiResponse.ok(new CredentialStatusResponse(configured));
    }

    @Operation(summary = "Save/replace this workspace's ElevenLabs API key")
    @PutMapping("/credential")
    public ApiResponse<CredentialStatusResponse> saveCredential(@PathVariable UUID workspaceId,
                                                                @Valid @RequestBody SaveCredentialRequest req) {
        dubbing.saveCredential(workspaceId, SecurityUtils.currentUserId(), req.apiKey());
        return ApiResponse.ok(new CredentialStatusResponse(true));
    }

    @Operation(summary = "Submit a video for dubbing into a target language")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DubbingJobResponse> startDub(
            @PathVariable UUID workspaceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetLang") String targetLang,
            @RequestParam(value = "sourceLang", required = false) String sourceLang,
            @RequestParam(value = "name", required = false) String name) {
        if (file == null || file.isEmpty()) {
            throw ApiException.business(ErrorCode.BUSINESS_RULE);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.business(ErrorCode.BUSINESS_RULE);
        }
        return ApiResponse.ok(dubbing.startDub(workspaceId, SecurityUtils.currentUserId(),
                bytes, file.getOriginalFilename(), file.getContentType(), targetLang, sourceLang, name));
    }

    @Operation(summary = "Get the latest status of a dubbing job (polls ElevenLabs)")
    @GetMapping("/{jobId}")
    public ApiResponse<DubbingJobResponse> status(@PathVariable UUID workspaceId,
                                                  @PathVariable UUID jobId) {
        return ApiResponse.ok(dubbing.refreshStatus(workspaceId, SecurityUtils.currentUserId(), jobId));
    }

    @Operation(summary = "Download the dubbed video")
    @GetMapping("/{jobId}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID workspaceId, @PathVariable UUID jobId) {
        DownloadedMedia media = dubbing.download(workspaceId, SecurityUtils.currentUserId(), jobId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("dubbed-" + jobId + extensionFor(media.contentType())).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(media.contentType())
                .body(media.bytes());
    }

    @Operation(summary = "List dubbing jobs for the workspace")
    @GetMapping
    public ApiResponse<PageResponse<DubbingJobResponse>> history(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                dubbing.history(workspaceId, SecurityUtils.currentUserId(), page, Math.min(size, 100)));
    }

    private static String extensionFor(MediaType type) {
        if (type == null) return ".bin";
        String sub = type.getSubtype();
        if (sub.contains("mp4")) return ".mp4";
        if (sub.contains("mpeg") || sub.contains("mp3")) return ".mp3";
        if (sub.contains("wav")) return ".wav";
        return type.getType().equals("video") ? ".mp4" : ".bin";
    }
}
