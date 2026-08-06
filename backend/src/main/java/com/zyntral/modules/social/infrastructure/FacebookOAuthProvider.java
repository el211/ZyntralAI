package com.zyntral.modules.social.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.modules.social.config.SocialOAuthProperties;
import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

/** Facebook (Meta) OAuth via Facebook Login. Page tokens are derived after connection. */
@Component
public class FacebookOAuthProvider extends AbstractRestOAuthProvider {

    static final String GRAPH = "https://graph.facebook.com/v19.0";
    private static final String DIALOG = "https://www.facebook.com/v19.0/dialog/oauth";
    private static final String SCOPES =
            "public_profile,pages_show_list,pages_manage_posts,pages_read_engagement";

    private final SocialOAuthProperties.Provider cfg;

    public FacebookOAuthProvider(SocialOAuthProperties props) { this.cfg = props.facebook(); }

    @Override public SocialPlatform platform() { return SocialPlatform.FACEBOOK; }

    protected String scopes() { return SCOPES; }
    protected SocialOAuthProperties.Provider cfg() { return cfg; }

    @Override
    public String authorizationUrl(String state, String redirectUri, String codeChallenge) {
        return UriComponentsBuilder.fromUriString(DIALOG)
                .queryParam("client_id", cfg().clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("scope", scopes())
                .queryParam("response_type", "code")
                .encode().build().toUriString();
    }

    @Override
    public OAuthResult exchangeCode(String code, String redirectUri, String codeVerifier) {
        try {
            String tokenUrl = UriComponentsBuilder.fromUriString(GRAPH + "/oauth/access_token")
                    .queryParam("client_id", cfg().clientId())
                    .queryParam("client_secret", cfg().clientSecret())
                    .queryParam("redirect_uri", redirectUri)
                    .queryParam("code", code)
                    .encode().build().toUriString();
            JsonNode token = http.get().uri(tokenUrl).retrieve().body(JsonNode.class);
            String accessToken = token.path("access_token").asText();

            String meUrl = GRAPH + "/me?fields=id,name,picture&access_token=" + accessToken;
            JsonNode me = http.get().uri(meUrl).retrieve().body(JsonNode.class);
            return new OAuthResult(
                    me.path("id").asText(),
                    me.path("name").asText(null),
                    null,
                    me.path("picture").path("data").path("url").asText(null),
                    accessToken, null, scopes(),
                    Instant.now().plusSeconds(token.path("expires_in").asLong(5184000)));
        } catch (Exception e) {
            throw connectFailed(platform().name(), e);
        }
    }
}
