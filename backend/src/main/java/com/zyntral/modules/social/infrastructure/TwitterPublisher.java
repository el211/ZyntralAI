package com.zyntral.modules.social.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * X (Twitter) adapter — publishes a text post via the v2 API
 * ({@code POST /2/tweets}) using the connected account's OAuth token (scope tweet.write).
 */
@Component
public class TwitterPublisher extends AbstractSocialPublisher {

    private static final String TWEETS_URL = "https://api.twitter.com/2/tweets";

    private final RestClient http = RestClient.create();

    @Override
    public SocialPlatform platform() {
        return SocialPlatform.TWITTER;
    }

    @Override
    protected PublishResult doPublish(PublishContext ctx) {
        String token = ctx.account().getAccessToken();
        if (token == null || token.isBlank()) {
            return PublishResult.failed("X account has no access token; reconnect required");
        }
        JsonNode res = http.post()
                .uri(TWEETS_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", ctx.body() == null ? "" : ctx.body()))
                .retrieve()
                .body(JsonNode.class);

        String id = res != null ? res.path("data").path("id").asText(null) : null;
        String handle = ctx.account().getHandle();
        String permalink = (id != null && handle != null)
                ? "https://x.com/" + handle + "/status/" + id : null;
        log.info("Published X post {} for account {}", id, ctx.account().getId());
        return PublishResult.ok(id, permalink);
    }
}
