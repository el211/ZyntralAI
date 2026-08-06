package com.zyntral.modules.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.common.storage.StorageService;
import com.zyntral.modules.ai.config.AiProperties;
import com.zyntral.modules.ai.domain.AiVideo;
import com.zyntral.modules.ai.domain.AiVideoRepository;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates short videos with Google Veo (Gemini API). Veo is a long-running operation: we submit
 * the job (charging {@link #VIDEO_COST} credits), persist the operation, and a scheduled poller
 * advances it — downloading the finished clip and storing it in S3.
 */
@Service
public class VideoGenerationService {

    static final int VIDEO_COST = 50;

    private static final Logger log = LoggerFactory.getLogger(VideoGenerationService.class);

    private final AiVideoRepository videos;
    private final WorkspaceAccess access;
    private final AiCreditService credits;
    private final StorageService storage;
    private final RestClient veo;
    private final RestClient http;   // for downloading the produced video
    private final String model;

    public VideoGenerationService(AiVideoRepository videos, WorkspaceAccess access, AiCreditService credits,
                                  StorageService storage, AiProperties props) {
        this.videos = videos;
        this.access = access;
        this.credits = credits;
        this.storage = storage;
        this.model = props.veo().model();
        String key = props.veo().apiKey() == null ? "" : props.veo().apiKey();
        this.veo = RestClient.builder().baseUrl(props.veo().baseUrl())
                .defaultHeader("x-goog-api-key", key).build();
        this.http = RestClient.builder().defaultHeader("x-goog-api-key", key).build();
    }

    public record VideoOptions(String aspectRatio, String resolution, Integer durationSeconds,
                               byte[] imageBytes, String imageMime) {}

    /** Text-to-video or image-to-video with aspect ratio / resolution / duration. */
    public AiVideo submit(UUID workspaceId, UUID userId, String prompt, VideoOptions opts) {
        access.requireCanEdit(workspaceId, userId);
        requireStorage();

        Map<String, Object> instance = new HashMap<>();
        instance.put("prompt", prompt);
        if (opts.imageBytes() != null && opts.imageBytes().length > 0) {
            instance.put("image", Map.of(
                    "bytesBase64Encoded", Base64.getEncoder().encodeToString(opts.imageBytes()),
                    "mimeType", opts.imageMime() == null ? "image/png" : opts.imageMime()));
        }
        Map<String, Object> body = Map.of(
                "instances", List.of(instance),
                "parameters", buildParameters(opts.aspectRatio(), opts.resolution(), opts.durationSeconds()));

        return start(workspaceId, userId, prompt, body, opts.resolution(), opts.durationSeconds());
    }

    /** Extend a previously generated Veo clip (valid ~2 days) by a few seconds. */
    public AiVideo extend(UUID workspaceId, UUID userId, UUID sourceId, String prompt) {
        access.requireCanEdit(workspaceId, userId);
        requireStorage();
        AiVideo src = videos.findByIdAndWorkspaceId(sourceId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("video", sourceId));
        if (!"COMPLETED".equals(src.getStatus()) || src.getVeoVideoUri() == null) {
            throw new ApiException(ErrorCode.BUSINESS_RULE,
                    new Object[]{"This video can't be extended (Veo keeps source clips ~2 days)"});
        }
        Map<String, Object> instance = new HashMap<>();
        instance.put("prompt", prompt == null ? "" : prompt);
        instance.put("video", Map.of("uri", src.getVeoVideoUri()));
        Map<String, Object> body = Map.of(
                "instances", List.of(instance),
                "parameters", Map.of("resolution", "720p", "durationSeconds", 8));

        return start(workspaceId, userId,
                (prompt == null || prompt.isBlank()) ? src.getPrompt() : prompt, body, "720p", 8);
    }

    private AiVideo start(UUID workspaceId, UUID userId, String prompt, Map<String, Object> body,
                          String resolution, Integer duration) {
        credits.charge(workspaceId, VIDEO_COST);
        try {
            JsonNode res = veo.post()
                    .uri("/models/" + model + ":predictLongRunning")
                    .body(body).retrieve().body(JsonNode.class);
            String op = res.path("name").asText(null);
            if (op == null || op.isBlank()) throw new IllegalStateException("no operation name returned");
            AiVideo v = AiVideo.pending(workspaceId, userId, prompt, op, VIDEO_COST);
            v.setResolution(resolution);
            v.setDurationSeconds(duration);
            return videos.save(v);
        } catch (RuntimeException ex) {
            credits.refund(workspaceId, VIDEO_COST);
            log.error("Veo submit failed (model={})", model, ex);
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"Video generation could not start"});
        }
    }

    private Map<String, Object> buildParameters(String aspectRatio, String resolution, Integer duration) {
        Map<String, Object> p = new HashMap<>();
        p.put("aspectRatio", "9:16".equals(aspectRatio) ? "9:16" : "16:9");
        String res = ("1080p".equals(resolution) || "4k".equals(resolution)) ? resolution : "720p";
        p.put("resolution", res);
        // 1080p/4k require an 8s duration; otherwise allow 4/6/8.
        int dur = (duration != null && (duration == 4 || duration == 6 || duration == 8)) ? duration : 8;
        if (!"720p".equals(res)) dur = 8;
        p.put("durationSeconds", dur);
        return p;
    }

    private void requireStorage() {
        if (!storage.isEnabled()) {
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"Video storage (S3) is not configured"});
        }
    }

    @Transactional(readOnly = true)
    public List<AiVideo> list(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return videos.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    @Transactional(readOnly = true)
    public AiVideo get(UUID workspaceId, UUID userId, UUID id) {
        access.requireMember(workspaceId, userId);
        return videos.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> ApiException.notFound("video", id));
    }

    /** Advance every in-flight job. Called by the scheduled poller. */
    public void pollPending() {
        if (!storage.isEnabled()) return;
        for (AiVideo v : videos.findByStatusIn(List.of("PENDING", "PROCESSING"))) {
            try {
                pollOne(v);
            } catch (RuntimeException e) {
                log.warn("Video poll failed for {}", v.getId(), e);
            }
        }
    }

    private void pollOne(AiVideo v) {
        JsonNode op = veo.get().uri("/" + v.getOperationName()).retrieve().body(JsonNode.class);
        if (!op.path("done").asBoolean(false)) {
            if ("PENDING".equals(v.getStatus())) { v.markProcessing(); videos.save(v); }
            return;
        }
        JsonNode err = op.path("error");
        if (err.isObject() && err.path("code").asInt(0) != 0) {
            fail(v, err.path("message").asText("Veo reported an error"));
            return;
        }
        String uri = extractVideoUri(op);
        if (uri == null) {
            log.error("Veo op {} done but no video uri. Full response: {}", v.getOperationName(), op);
            fail(v, "No video was returned");
            return;
        }
        byte[] bytes = http.get().uri(uri).retrieve().body(byte[].class);
        if (bytes == null || bytes.length == 0) { fail(v, "Downloaded video was empty"); return; }
        String key = "videos/" + v.getWorkspaceId() + "/" + v.getId() + ".mp4";
        storage.put(key, bytes, "video/mp4");
        v.markCompleted(key, "video/mp4", uri);   // keep the Veo uri so the clip can be extended (~2 days)
        videos.save(v);
        log.info("Video {} completed → s3://{}", v.getId(), key);
    }

    private void fail(AiVideo v, String message) {
        v.markFailed(message);
        videos.save(v);
        credits.refund(v.getWorkspaceId(), v.getCreditsCost());
    }

    private String extractVideoUri(JsonNode op) {
        JsonNode resp = op.path("response");
        JsonNode sample = resp.path("generateVideoResponse").path("generatedSamples").path(0).path("video");
        String uri = sample.path("uri").asText(null);
        if (uri == null) uri = sample.path("videoUri").asText(null);
        if (uri == null) uri = resp.path("generatedVideos").path(0).path("video").path("uri").asText(null);
        return uri;
    }
}
