package com.zyntral.modules.ai.web;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.modules.ai.domain.AiImage;
import com.zyntral.modules.ai.domain.AiImageRepository;
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
 * Serves generated image bytes by id. Public (no JWT) so the bytes can be used directly in
 * {@code <img>} tags and attached to posts; the id is an unguessable UUID.
 */
@RestController
@RequestMapping(ApiConstants.API_V1 + "/ai-images")
public class PublicImageController {

    private final AiImageRepository images;

    public PublicImageController(AiImageRepository images) {
        this.images = images;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable UUID id) {
        AiImage img = images.findById(id).orElseThrow(() -> ApiException.notFound("image", id));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                .body(img.getData());
    }
}
