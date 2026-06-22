package com.zyntral.modules.ai.web;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.storage.StorageService;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.modules.ai.domain.AiVideo;
import com.zyntral.modules.ai.domain.AiVideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/**
 * Serves a completed video by redirecting to a short-lived presigned S3 URL. Public (no JWT) so
 * the bytes can stream into a {@code <video>} tag; the id is an unguessable UUID.
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
    public ResponseEntity<Void> get(@PathVariable UUID id) {
        AiVideo v = videos.findById(id).orElseThrow(() -> ApiException.notFound("video", id));
        if (!"COMPLETED".equals(v.getStatus()) || v.getStorageKey() == null) {
            throw ApiException.notFound("video", id);
        }
        String url = storage.presignedGetUrl(v.getStorageKey(), Duration.ofHours(6));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}
