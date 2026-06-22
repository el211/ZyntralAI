package com.zyntral.modules.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.ai.config.AiProperties;
import com.zyntral.modules.ai.domain.AiImage;
import com.zyntral.modules.ai.domain.AiImageRepository;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates logos and banners via OpenAI's image API and stores the bytes in-DB. Reuses the
 * configured OpenAI key/base-url; the model is {@code zyntral.ai.openai.image-model}
 * (default gpt-image-1).
 */
@Service
public class ImageGenerationService {

    public enum ImageKind { LOGO, BANNER }

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationService.class);

    private final AiImageRepository images;
    private final WorkspaceAccess access;
    private final RestClient client;
    private final String model;

    public ImageGenerationService(AiImageRepository images, WorkspaceAccess access, AiProperties props,
                                  @Value("${zyntral.ai.openai.image-model:gpt-image-1}") String model) {
        this.images = images;
        this.access = access;
        this.model = model;
        this.client = RestClient.builder()
                .baseUrl(props.openai().baseUrl())
                .defaultHeader("Authorization",
                        "Bearer " + (props.openai().apiKey() == null ? "" : props.openai().apiKey()))
                .build();
    }

    @Transactional
    public AiImage generate(UUID workspaceId, UUID userId, String kindRaw, String prompt) {
        access.requireCanEdit(workspaceId, userId);
        ImageKind kind = parseKind(kindRaw);
        String size = kind == ImageKind.BANNER ? "1536x1024" : "1024x1024";

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", frame(kind, prompt));
        body.put("size", size);
        body.put("n", 1);
        if (model.startsWith("dall-e")) body.put("response_format", "b64_json");

        byte[] data;
        try {
            JsonNode res = client.post().uri("/images/generations").body(body)
                    .retrieve().body(JsonNode.class);
            String b64 = res.path("data").path(0).path("b64_json").asText(null);
            if (b64 == null || b64.isBlank()) throw new IllegalStateException("no image data returned");
            data = Base64.getDecoder().decode(b64);
        } catch (Exception e) {
            log.error("Image generation failed (kind={}, model={})", kind, model, e);
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"Image generation failed"});
        }
        return images.save(AiImage.create(workspaceId, userId, kind.name(), prompt, "image/png", data));
    }

    @Transactional(readOnly = true)
    public List<AiImage> list(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return images.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    private ImageKind parseKind(String raw) {
        try {
            return ImageKind.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"kind must be LOGO or BANNER"});
        }
    }

    private String frame(ImageKind kind, String prompt) {
        return kind == ImageKind.BANNER
                ? "A wide, high-quality social-media banner/header image. " + prompt
                  + " Clean composition, leave some empty space, no watermark, no text artifacts."
                : "A clean, modern, simple brand logo. " + prompt
                  + " Centered on a plain background, no watermark, no extra text.";
    }
}
