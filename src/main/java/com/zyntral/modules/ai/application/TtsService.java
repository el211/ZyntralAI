package com.zyntral.modules.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.ai.domain.AiAudio;
import com.zyntral.modules.ai.domain.AiAudioRepository;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Text-to-speech via ElevenLabs. Generates an MP3 from text using the chosen voice, stores it
 * in-DB, and charges {@link #TTS_COST} credits (refunded if the provider call fails).
 */
@Service
public class TtsService {

    static final int TTS_COST = 3;

    private static final Logger log = LoggerFactory.getLogger(TtsService.class);

    private final AiAudioRepository audio;
    private final WorkspaceAccess access;
    private final AiCreditService credits;
    private final RestClient client;
    private final String model;
    private final String defaultVoice;
    private final boolean enabled;

    public TtsService(AiAudioRepository audio, WorkspaceAccess access, AiCreditService credits,
                      @Value("${zyntral.ai.elevenlabs.api-key:}") String apiKey,
                      @Value("${zyntral.ai.elevenlabs.model:eleven_multilingual_v2}") String model,
                      @Value("${zyntral.ai.elevenlabs.voice-id:21m00Tcm4TlvDq8ikWAM}") String defaultVoice,
                      @Value("${zyntral.ai.elevenlabs.base-url:https://api.elevenlabs.io/v1}") String baseUrl) {
        this.audio = audio;
        this.access = access;
        this.credits = credits;
        this.model = model;
        this.defaultVoice = defaultVoice;
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.client = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader("xi-api-key", apiKey == null ? "" : apiKey).build();
    }

    public AiAudio generate(UUID workspaceId, UUID userId, String text, String voiceId) {
        access.requireCanEdit(workspaceId, userId);
        if (!enabled) {
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"Text-to-speech is not configured"});
        }
        String voice = (voiceId == null || voiceId.isBlank()) ? defaultVoice : voiceId;

        credits.charge(workspaceId, TTS_COST);
        try {
            byte[] mp3 = client.post()
                    .uri("/text-to-speech/" + voice)
                    .accept(MediaType.parseMediaType("audio/mpeg"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text, "model_id", model))
                    .retrieve().body(byte[].class);
            if (mp3 == null || mp3.length == 0) throw new IllegalStateException("empty audio");
            return audio.save(AiAudio.create(workspaceId, userId, text, voice, "audio/mpeg", mp3));
        } catch (RuntimeException ex) {
            credits.refund(workspaceId, TTS_COST);
            log.error("ElevenLabs TTS failed (voice={})", voice, ex);
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"Speech generation failed"});
        }
    }

    @Transactional(readOnly = true)
    public List<AiAudio> list(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return audio.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    /** Lists the account's available voices for the picker. Empty if not configured. */
    public List<Map<String, String>> voices(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        if (!enabled) return List.of();
        try {
            JsonNode res = client.get().uri("/voices").retrieve().body(JsonNode.class);
            List<Map<String, String>> out = new ArrayList<>();
            for (JsonNode v : res.path("voices")) {
                out.add(Map.of("voiceId", v.path("voice_id").asText(""),
                        "name", v.path("name").asText("Voice")));
            }
            return out;
        } catch (RuntimeException ex) {
            log.warn("Could not list ElevenLabs voices", ex);
            return List.of();
        }
    }
}
