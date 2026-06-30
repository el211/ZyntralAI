package com.zyntral.modules.dubbing.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.modules.dubbing.config.DubbingProperties;
import com.zyntral.modules.dubbing.domain.DubbingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Thin adapter over the ElevenLabs Dubbing API. The API key is per workspace (BYOK), so it is
 * passed on every call as the {@code xi-api-key} header rather than baked into the client.
 */
@Component
@EnableConfigurationProperties(DubbingProperties.class)
public class ElevenLabsDubbingClient {

    private static final Logger log = LoggerFactory.getLogger(ElevenLabsDubbingClient.class);

    private final RestClient client;

    public ElevenLabsDubbingClient(DubbingProperties props) {
        this.client = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    public record CreatedDub(String dubbingId, Integer expectedDurationSec) {}

    public record DubStatus(DubbingStatus status, String error) {}

    public record DownloadedMedia(byte[] bytes, MediaType contentType) {}

    /** Submit a media file for dubbing. {@code sourceLang} may be null (auto-detect). */
    public CreatedDub createDub(String apiKey, byte[] media, String filename, String contentType,
                                String targetLang, String sourceLang, String name) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new NamedByteArrayResource(media, filename, contentType));
        parts.add("target_lang", targetLang);
        if (sourceLang != null && !sourceLang.isBlank()) {
            parts.add("source_lang", sourceLang);
        }
        if (name != null && !name.isBlank()) {
            parts.add("name", name);
        }
        try {
            JsonNode res = client.post()
                    .uri("/v1/dubbing")
                    .header("xi-api-key", apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(JsonNode.class);
            String id = res == null ? "" : res.path("dubbing_id").asText("");
            if (id.isBlank()) {
                throw new DubbingException("ElevenLabs did not return a dubbing_id");
            }
            Integer dur = res.hasNonNull("expected_duration_sec")
                    ? res.path("expected_duration_sec").asInt() : null;
            return new CreatedDub(id, dur);
        } catch (DubbingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ElevenLabs createDub failed (target={})", targetLang, ex);
            throw new DubbingException("Dubbing request failed: " + ex.getMessage(), ex);
        }
    }

    public DubStatus getStatus(String apiKey, String dubbingId) {
        try {
            JsonNode res = client.get()
                    .uri("/v1/dubbing/{id}", dubbingId)
                    .header("xi-api-key", apiKey)
                    .retrieve()
                    .body(JsonNode.class);
            String raw = res == null ? "" : res.path("status").asText("");
            String error = res != null && res.hasNonNull("error") ? res.path("error").asText() : null;
            return new DubStatus(mapStatus(raw), error);
        } catch (Exception ex) {
            log.error("ElevenLabs getStatus failed (dubbingId={})", dubbingId, ex);
            throw new DubbingException("Could not fetch dubbing status: " + ex.getMessage(), ex);
        }
    }

    /** Download the rendered media for a finished dub in the given language. */
    public DownloadedMedia download(String apiKey, String dubbingId, String languageCode) {
        try {
            ResponseEntity<byte[]> res = client.get()
                    .uri("/v1/dubbing/{id}/audio/{lang}", dubbingId, languageCode)
                    .header("xi-api-key", apiKey)
                    .retrieve()
                    .toEntity(byte[].class);
            MediaType type = res.getHeaders().getContentType();
            return new DownloadedMedia(res.getBody(), type != null ? type : MediaType.APPLICATION_OCTET_STREAM);
        } catch (Exception ex) {
            log.error("ElevenLabs download failed (dubbingId={}, lang={})", dubbingId, languageCode, ex);
            throw new DubbingException("Could not download dubbed media: " + ex.getMessage(), ex);
        }
    }

    private static DubbingStatus mapStatus(String raw) {
        return switch (raw == null ? "" : raw.toLowerCase()) {
            case "dubbed" -> DubbingStatus.DUBBED;
            case "failed" -> DubbingStatus.FAILED;
            case "dubbing" -> DubbingStatus.DUBBING;
            default -> DubbingStatus.DUBBING; // still in progress / unknown-in-flight
        };
    }

    /** ByteArrayResource that reports a filename + content type so it forms a proper file part. */
    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;
        private NamedByteArrayResource(byte[] bytes, String filename, String contentType) {
            super(bytes);
            this.filename = filename != null && !filename.isBlank() ? filename : "video.mp4";
        }
        @Override
        public String getFilename() {
            return filename;
        }
    }
}
