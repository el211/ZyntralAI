package com.zyntral.modules.ai.web;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.storage.StorageService;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.modules.ai.domain.AiVideo;
import com.zyntral.modules.ai.domain.AiVideoRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

/**
 * Streams a completed video's bytes through the API (so the browser only ever talks to the
 * HTTPS API origin — no public S3 domain or mixed-content issues). Public (no JWT): the id is
 * an unguessable UUID.
 */
@RestController
@RequestMapping(ApiConstants.API_V1 + "/videos")
public class PublicVideoController {

    private final AiVideoRepository videos;
    private final StorageService storage;

    public PublicVideoController(AiVideoRepository videos, StorageService storage) {
        this.videos = videos;
        this.storage = storage;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable UUID id) {
        AiVideo v = videos.findById(id).orElseThrow(() -> ApiException.notFound("video", id));
        if (!"COMPLETED".equals(v.getStatus()) || v.getStorageKey() == null) {
            throw ApiException.notFound("video", id);
        }
        byte[] data = storage.getBytes(v.getStorageKey());
        String ct = v.getContentType() == null ? "video/mp4" : v.getContentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .header("Accept-Ranges", "none")
                .body(data);
    }
}
