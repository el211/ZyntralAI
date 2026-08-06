package com.zyntral.modules.ai.web;

import com.zyntral.common.error.ApiException;
import com.zyntral.common.web.ApiConstants;
import com.zyntral.modules.ai.domain.AiAudio;
import com.zyntral.modules.ai.domain.AiAudioRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

/** Serves generated speech bytes by id (public; the id is an unguessable UUID). */
@RestController
@RequestMapping(ApiConstants.API_V1 + "/ai-audio")
public class PublicAudioController {

    private final AiAudioRepository audio;

    public PublicAudioController(AiAudioRepository audio) {
        this.audio = audio;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable UUID id) {
        AiAudio a = audio.findById(id).orElseThrow(() -> ApiException.notFound("audio", id));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                .body(a.getData());
    }
}
