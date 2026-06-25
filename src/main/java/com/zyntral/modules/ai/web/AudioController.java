package com.zyntral.modules.ai.web;

import com.zyntral.common.security.SecurityUtils;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.common.web.ApiResponse;
import com.zyntral.modules.ai.application.TtsService;
import com.zyntral.modules.ai.domain.AiAudio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "AI Audio", description = "Text-to-speech (ElevenLabs)")
@RestController
@RequestMapping(ApiConstants.API_V1 + "/workspaces/{workspaceId}/ai/audio")
public class AudioController {

    private final TtsService service;

    public AudioController(TtsService service) {
        this.service = service;
    }

    public record GenerateAudioRequest(@NotBlank @Size(max = 5000) String text, String voiceId) {}

    public record SetKeyRequest(@NotBlank String apiKey) {}

    public record KeyStatusResponse(boolean usingOwnKey) {}

    public record AudioResponse(UUID id, String textExcerpt, String voice, Instant createdAt) {
        static AudioResponse of(AiAudio a) {
            return new AudioResponse(a.getId(), a.getTextExcerpt(), a.getVoice(), a.getCreatedAt());
        }
    }

    @Operation(summary = "Generate speech from text")
    @PostMapping
    public ApiResponse<AudioResponse> generate(@PathVariable UUID workspaceId,
                                               @Valid @RequestBody GenerateAudioRequest req) {
        return ApiResponse.ok(AudioResponse.of(
                service.generate(workspaceId, SecurityUtils.currentUserId(), req.text(), req.voiceId())));
    }

    @Operation(summary = "List generated speech for the workspace")
    @GetMapping
    public ApiResponse<List<AudioResponse>> list(@PathVariable UUID workspaceId) {
        return ApiResponse.ok(service.list(workspaceId, SecurityUtils.currentUserId())
                .stream().map(AudioResponse::of).toList());
    }

    @Operation(summary = "List available voices")
    @GetMapping("/voices")
    public ApiResponse<List<Map<String, String>>> voices(@PathVariable UUID workspaceId) {
        return ApiResponse.ok(service.voices(workspaceId, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "Whether this workspace uses its own ElevenLabs key")
    @GetMapping("/key")
    public ApiResponse<KeyStatusResponse> keyStatus(@PathVariable UUID workspaceId) {
        return ApiResponse.ok(new KeyStatusResponse(
                service.hasWorkspaceKey(workspaceId, SecurityUtils.currentUserId())));
    }

    @Operation(summary = "Set this workspace's own ElevenLabs key (ADMIN+) — uses no Zyntral credits")
    @PutMapping("/key")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setKey(@PathVariable UUID workspaceId, @Valid @RequestBody SetKeyRequest req) {
        service.setWorkspaceKey(workspaceId, SecurityUtils.currentUserId(), req.apiKey());
    }

    @Operation(summary = "Remove this workspace's own ElevenLabs key (ADMIN+)")
    @DeleteMapping("/key")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearKey(@PathVariable UUID workspaceId) {
        service.clearWorkspaceKey(workspaceId, SecurityUtils.currentUserId());
    }
}
