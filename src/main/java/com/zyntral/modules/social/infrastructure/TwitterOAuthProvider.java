package com.zyntral.modules.social.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.modules.social.config.SocialOAuthProperties;
import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

/** X (Twitter) OAuth 2.0 with PKCE. Scopes: tweet.read/write, users.read, offline.access. */
@Component
public class TwitterOAuthProvider extends AbstractRestOAuthProvider {

    private static final String AUTH = "https://twitter.com/i/oauth2/authorize";
    private static final String TOKEN = "https://api.twitter.com/2/oauth2/token";
    private static final String ME = "https://api.twitter.com/2/users/me?user.fields=profile_image_url,username";
    private static final String SCOPES = "tweet.read tweet.write users.read offline.access";

    private final SocialOAuthProperties.Provider cfg;

    public TwitterOAuthProvider(SocialOAuthProperties props) { this.cfg = props.twitter(); }

    @Override public SocialPlatform platform() { return SocialPlatform.TWITTER; }

    @Override
    public String authorizationUrl(String state, String redirectUri, String codeChallenge) {
        return UriComponentsBuilder.fromUriString(AUTH)
                .queryParam("response_type", "code")
                .queryParam("client_id", cfg.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", SCOPES)
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .encode().build().toUriString();
    }

    @Override
    public OAuthResult exchangeCode(String code, String redirectUri, String codeVerifier) {
        try {
            JsonNode token = postForm(TOKEN, form(
                    "grant_type", "authorization_code",
                    "code", code,
                    "redirect_uri", redirectUri,
                    "code_verifier", codeVerifier,
                    "client_id", cfg.clientId()), basic(cfg.clientId(), cfg.clientSecret()));

            String accessToken = token.path("access_token").asText();
            JsonNode user = getJson(ME, accessToken).path("data");
            return new OAuthResult(
                    user.path("id").asText(),
                    user.path("name").asText(null),
                    user.path("username").asText(null),
                    user.path("profile_image_url").asText(null),
                    accessToken,
                    token.path("refresh_token").asText(null),
                    token.path("scope").asText(SCOPES),
                    Instant.now().plusSeconds(token.path("expires_in").asLong(7200)));
        } catch (Exception e) {
            throw connectFailed("X", e);
        }
    }
}
