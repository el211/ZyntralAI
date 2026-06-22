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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
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

    /** Improve/restyle an uploaded image (logo, etc.) via the image-edit API. Needs gpt-image-1. */
    @Transactional
    public AiImage edit(UUID workspaceId, UUID userId, String kindRaw, String prompt,
                        byte[] imageBytes, String filename) {
        access.requireCanEdit(workspaceId, userId);
        ImageKind kind = parseKind(kindRaw);
        String size = kind == ImageKind.BANNER ? "1536x1024" : "1024x1024";
        String safeName = (filename == null || filename.isBlank()) ? "image.png" : filename;

        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("model", model);
        mb.part("prompt", improve(kind, prompt));
        mb.part("size", size);
        mb.part("image", new ByteArrayResource(imageBytes) {
            @Override public String getFilename() { return safeName; }
        });
        if (model.startsWith("dall-e")) mb.part("response_format", "b64_json");

        byte[] data;
        try {
            JsonNode res = client.post().uri("/images/edits")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(mb.build())
                    .retrieve().body(JsonNode.class);
            String b64 = res.path("data").path(0).path("b64_json").asText(null);
            if (b64 == null || b64.isBlank()) throw new IllegalStateException("no image data returned");
            data = Base64.getDecoder().decode(b64);
        } catch (Exception e) {
            log.error("Image edit failed (kind={}, model={})", kind, model, e);
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"Image improvement failed"});
        }
        return images.save(AiImage.create(workspaceId, userId, kind.name(), prompt, "image/png", data));
    }

    /** Remove the background of an uploaded image, returning a transparent PNG. Needs gpt-image-1. */
    @Transactional
    public AiImage removeBackground(UUID workspaceId, UUID userId, byte[] imageBytes, String filename) {
        access.requireCanEdit(workspaceId, userId);
        String safeName = (filename == null || filename.isBlank()) ? "image.png" : filename;

        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("model", model);
        mb.part("prompt", "Remove the background completely and keep only the main subject, cleanly "
                + "isolated on a fully transparent background. Do not add or change anything else.");
        mb.part("background", "transparent");
        mb.part("output_format", "png");
        mb.part("size", "auto");
        mb.part("image", new ByteArrayResource(imageBytes) {
            @Override public String getFilename() { return safeName; }
        });

        byte[] data;
        try {
            JsonNode res = client.post().uri("/images/edits")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(mb.build())
                    .retrieve().body(JsonNode.class);
            String b64 = res.path("data").path(0).path("b64_json").asText(null);
            if (b64 == null || b64.isBlank()) throw new IllegalStateException("no image data returned");
            data = Base64.getDecoder().decode(b64);
        } catch (Exception e) {
            log.error("Background removal failed (model={})", model, e);
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"Background removal failed"});
        }
        return images.save(AiImage.create(workspaceId, userId, "CUTOUT", "background removed",
                "image/png", data));
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

    private String improve(ImageKind kind, String prompt) {
        String base = "Improve and modernize this uploaded " + (kind == ImageKind.BANNER ? "banner" : "logo")
                + ", keeping its core identity. ";
        return (prompt == null || prompt.isBlank()) ? base + "Make it cleaner and more professional."
                : base + prompt;
    }

    private String frame(ImageKind kind, String prompt) {
        return kind == ImageKind.BANNER
                ? "A wide, high-quality social-media banner/header image. " + prompt
                  + " Clean composition, leave some empty space, no watermark, no text artifacts."
                : "A clean, modern, simple brand logo. " + prompt
                  + " Centered on a plain background, no watermark, no extra text.";
    }
}
