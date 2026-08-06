package com.zyntral.modules.social.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LinkedIn adapter — publishes to the member feed via the Posts API
 * ({@code POST /rest/posts}) using the connected account's OAuth token. Requires the
 * {@code w_member_social} scope (granted during the OAuth connect flow).
 */
@Component
public class LinkedInPublisher extends AbstractSocialPublisher {

    private static final String POSTS_URL = "https://api.linkedin.com/rest/posts";
    private static final String IMAGES_INIT_URL =
            "https://api.linkedin.com/rest/images?action=initializeUpload";

    // LinkedIn retires API versions after ~12 months; keep this current via config.
    @Value("${zyntral.social.linkedin.api-version:202605}")
    private String linkedInVersion;

    private final RestClient http = RestClient.create();

    @Override
    public SocialPlatform platform() {
        return SocialPlatform.LINKEDIN;
    }

    @Override
    protected PublishResult doPublish(PublishContext ctx) {
        String token = ctx.account().getAccessToken();
        if (token == null || token.isBlank()) {
            return PublishResult.failed("LinkedIn account has no access token; reconnect required");
        }
        String authorUrn = "urn:li:person:" + ctx.account().getExternalId();
        Map<String, Object> body = new HashMap<>();
        body.put("author", authorUrn);
        body.put("commentary", ctx.body() == null ? "" : ctx.body());
        body.put("visibility", "PUBLIC");
        body.put("distribution", Map.of(
                "feedDistribution", "MAIN_FEED",
                "targetEntities", List.of(),
                "thirdPartyDistributionChannels", List.of()));
        body.put("lifecycleState", "PUBLISHED");
        body.put("isReshareDisabledByAuthor", false);

        // Attach the first image, if any. Best-effort: a failed upload still posts the text.
        if (ctx.mediaUrls() != null && !ctx.mediaUrls().isEmpty()) {
            try {
                String imageUrn = uploadImage(token, authorUrn, ctx.mediaUrls().get(0));
                body.put("content", Map.of("media", Map.of("id", imageUrn)));
            } catch (RuntimeException ex) {
                log.warn("LinkedIn image upload failed for account {}; posting text only",
                        ctx.account().getId(), ex);
            }
        }

        ResponseEntity<Void> response = http.post()
                .uri(POSTS_URL)
                .header("Authorization", "Bearer " + token)
                .header("LinkedIn-Version", linkedInVersion)
                .header("X-Restli-Protocol-Version", "2.0.0")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        String postUrn = response.getHeaders().getFirst("x-restli-id");
        if (postUrn == null) postUrn = response.getHeaders().getFirst("x-linkedin-id");
        String permalink = postUrn != null
                ? "https://www.linkedin.com/feed/update/" + postUrn + "/" : null;
        log.info("Published LinkedIn post {} for account {}", postUrn, ctx.account().getId());
        return PublishResult.ok(postUrn, permalink);
    }

    /** LinkedIn image upload: download the media, initialize the upload, PUT the bytes, return the URN. */
    private String uploadImage(String token, String ownerUrn, String mediaUrl) {
        byte[] bytes = http.get().uri(mediaUrl).retrieve().body(byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("empty image at " + mediaUrl);
        }

        JsonNode init = http.post()
                .uri(IMAGES_INIT_URL)
                .header("Authorization", "Bearer " + token)
                .header("LinkedIn-Version", linkedInVersion)
                .header("X-Restli-Protocol-Version", "2.0.0")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("initializeUploadRequest", Map.of("owner", ownerUrn)))
                .retrieve()
                .body(JsonNode.class);

        String uploadUrl = init.path("value").path("uploadUrl").asText(null);
        String imageUrn = init.path("value").path("image").asText(null);
        if (uploadUrl == null || imageUrn == null) {
            throw new IllegalStateException("LinkedIn initializeUpload returned no uploadUrl/image");
        }

        http.put()
                .uri(uploadUrl)
                .header("Authorization", "Bearer " + token)
                .body(bytes)
                .retrieve()
                .toBodilessEntity();

        return imageUrn;
    }
}
