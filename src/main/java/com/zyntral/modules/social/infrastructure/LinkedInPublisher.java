package com.zyntral.modules.social.infrastructure;

import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
        Map<String, Object> body = Map.of(
                "author", authorUrn,
                "commentary", ctx.body() == null ? "" : ctx.body(),
                "visibility", "PUBLIC",
                "distribution", Map.of(
                        "feedDistribution", "MAIN_FEED",
                        "targetEntities", List.of(),
                        "thirdPartyDistributionChannels", List.of()),
                "lifecycleState", "PUBLISHED",
                "isReshareDisabledByAuthor", false);

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
}
