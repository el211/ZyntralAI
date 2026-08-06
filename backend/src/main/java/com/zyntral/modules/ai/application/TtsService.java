package com.zyntral.modules.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.ai.domain.AiAudio;
import com.zyntral.modules.ai.domain.AiAudioRepository;
import com.zyntral.modules.ai.domain.WorkspaceProviderKey;
import com.zyntral.modules.ai.domain.WorkspaceProviderKeyRepository;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import com.zyntral.modules.workspace.domain.WorkspaceRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Text-to-speech via ElevenLabs. If the workspace has set its own ElevenLabs key (BYOK), that
 * key is used and <b>no Zyntral credits are charged</b>; otherwise the platform key is used and
 * {@link #TTS_COST} credits are charged (refunded on failure).
 */
@Service
public class TtsService {

    static final int TTS_COST = 3;
    public static final String PROVIDER = "ELEVENLABS";

    private static final Logger log = LoggerFactory.getLogger(TtsService.class);

    private final AiAudioRepository audio;
    private final WorkspaceProviderKeyRepository providerKeys;
    private final WorkspaceAccess access;
    private final AiCreditService credits;
    private final String baseUrl;
    private final String model;
    private final String defaultVoice;
    private final String platformKey;

    public TtsService(AiAudioRepository audio, WorkspaceProviderKeyRepository providerKeys,
                      WorkspaceAccess access, AiCreditService credits,
                      @Value("${zyntral.ai.elevenlabs.api-key:}") String apiKey,
                      @Value("${zyntral.ai.elevenlabs.model:eleven_multilingual_v2}") String model,
                      @Value("${zyntral.ai.elevenlabs.voice-id:21m00Tcm4TlvDq8ikWAM}") String defaultVoice,
                      @Value("${zyntral.ai.elevenlabs.base-url:https://api.elevenlabs.io/v1}") String baseUrl) {
        this.audio = audio;
        this.providerKeys = providerKeys;
        this.access = access;
        this.credits = credits;
        this.model = model;
        this.defaultVoice = defaultVoice;
        this.baseUrl = baseUrl;
        this.platformKey = apiKey == null ? "" : apiKey;
    }

    // ---- BYOK management ----

    @Transactional
    public void setWorkspaceKey(UUID workspaceId, UUID userId, String apiKey) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"API key is required"});
        }
        WorkspaceProviderKey existing = providerKeys.find(workspaceId, PROVIDER).orElse(null);
        if (existing != null) {
            existing.setApiKey(apiKey.trim());
        } else {
            providerKeys.save(new WorkspaceProviderKey(workspaceId, PROVIDER, apiKey.trim()));
        }
    }

    @Transactional
    public void clearWorkspaceKey(UUID workspaceId, UUID userId) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);
        providerKeys.remove(workspaceId, PROVIDER);
    }

    @Transactional(readOnly = true)
    public boolean hasWorkspaceKey(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return providerKeys.find(workspaceId, PROVIDER).isPresent();
    }

    // ---- generation ----

    public AiAudio generate(UUID workspaceId, UUID userId, String text, String voiceId) {
        access.requireCanEdit(workspaceId, userId);
        String ownKey = workspaceKey(workspaceId);
        boolean usingOwn = ownKey != null;
        String key = usingOwn ? ownKey : platformKey;
        if (key.isBlank()) {
            throw new ApiException(ErrorCode.BUSINESS_RULE,
                    new Object[]{"Text-to-speech is not configured — add your ElevenLabs key in Settings"});
        }
        String voice = (voiceId == null || voiceId.isBlank()) ? defaultVoice : voiceId;

        if (!usingOwn) credits.charge(workspaceId, TTS_COST);
        try {
            byte[] mp3 = client(key).post()
                    .uri("/text-to-speech/" + voice)
                    .accept(MediaType.parseMediaType("audio/mpeg"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text, "model_id", model))
                    .retrieve().body(byte[].class);
            if (mp3 == null || mp3.length == 0) throw new IllegalStateException("empty audio");
            return audio.save(AiAudio.create(workspaceId, userId, text, voice, "audio/mpeg", mp3));
        } catch (RuntimeException ex) {
            if (!usingOwn) credits.refund(workspaceId, TTS_COST);
            log.error("ElevenLabs TTS failed (voice={}, byok={})", voice, usingOwn, ex);
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"Speech generation failed"});
        }
    }

    /**
     * Instant Voice Cloning: upload audio samples to create a new voice. Requires the workspace's
     * OWN ElevenLabs key so the cloned voice is created in the user's account (not the platform's).
     * Returns the new voice {voiceId, name}.
     */
    public Map<String, String> cloneVoice(UUID workspaceId, UUID userId, String name, String description,
                                          List<byte[]> files, List<String> filenames) {
        access.requireCanEdit(workspaceId, userId);
        String ownKey = workspaceKey(workspaceId);
        if (ownKey == null) {
            throw new ApiException(ErrorCode.BUSINESS_RULE,
                    new Object[]{"Voice cloning requires your own ElevenLabs key (set it above first)"});
        }
        if (files == null || files.isEmpty()) {
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"Upload at least one audio sample"});
        }
        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("name", name == null || name.isBlank() ? "My voice" : name.trim());
        if (description != null && !description.isBlank()) mb.part("description", description.trim());
        for (int i = 0; i < files.size(); i++) {
            final String fn = (filenames != null && i < filenames.size() && filenames.get(i) != null)
                    ? filenames.get(i) : "sample" + i + ".mp3";
            mb.part("files", new ByteArrayResource(files.get(i)) {
                @Override public String getFilename() { return fn; }
            });
        }
        try {
            JsonNode res = client(ownKey).post()
                    .uri("/voices/add")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(mb.build())
                    .retrieve().body(JsonNode.class);
            String voiceId = res.path("voice_id").asText(null);
            if (voiceId == null || voiceId.isBlank()) throw new IllegalStateException("no voice_id returned");
            return Map.of("voiceId", voiceId, "name", name == null ? "My voice" : name);
        } catch (RuntimeException ex) {
            log.error("ElevenLabs voice clone failed", ex);
            throw new ApiException(ErrorCode.BUSINESS_RULE,
                    new Object[]{"Voice cloning failed (needs a paid ElevenLabs plan that allows cloning)"});
        }
    }

    @Transactional(readOnly = true)
    public List<AiAudio> list(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return audio.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    /** Lists the workspace's available voices (own key if set, else platform). Empty if neither. */
    @Transactional(readOnly = true)
    public List<Map<String, String>> voices(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        String ownKey = workspaceKey(workspaceId);
        String key = ownKey != null ? ownKey : platformKey;
        if (key.isBlank()) return List.of();
        try {
            JsonNode res = client(key).get().uri("/voices").retrieve().body(JsonNode.class);
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

    // ---- helpers ----

    private String workspaceKey(UUID workspaceId) {
        return providerKeys.find(workspaceId, PROVIDER).map(WorkspaceProviderKey::getApiKey).orElse(null);
    }

    private RestClient client(String key) {
        return RestClient.builder().baseUrl(baseUrl).defaultHeader("xi-api-key", key).build();
    }
}
