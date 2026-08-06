package com.zyntral.modules.social.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.modules.social.config.SocialOAuthProperties;
import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

/** Pinterest OAuth 2.0 (v5). Token endpoint uses HTTP Basic auth (client_id:client_secret). */
@Component
public class PinterestOAuthProvider extends AbstractRestOAuthProvider {

    private static final String AUTH = "https://www.pinterest.com/oauth/";
    private static final String TOKEN = "https://api.pinterest.com/v5/oauth/token";
    private static final String ME = "https://api.pinterest.com/v5/user_account";
    private static final String SCOPES = "boards:read,pins:read,pins:write,user_accounts:read";

    private final SocialOAuthProperties.Provider cfg;

    public PinterestOAuthProvider(SocialOAuthProperties props) { this.cfg = props.pinterest(); }

    @Override public SocialPlatform platform() { return SocialPlatform.PINTEREST; }

    @Override
    public String authorizationUrl(String state, String redirectUri, String codeChallenge) {
        return UriComponentsBuilder.fromUriString(AUTH)
                .queryParam("client_id", cfg.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPES)
                .queryParam("state", state)
                .encode().build().toUriString();
    }

    @Override
    public OAuthResult exchangeCode(String code, String redirectUri, String codeVerifier) {
        try {
            JsonNode token = postForm(TOKEN, form(
                    "grant_type", "authorization_code",
                    "code", code,
                    "redirect_uri", redirectUri), basic(cfg.clientId(), cfg.clientSecret()));

            String accessToken = token.path("access_token").asText();
            JsonNode me = getJson(ME, accessToken);
            String username = me.path("username").asText(null);
            return new OAuthResult(
                    username != null ? username : me.path("id").asText("pinterest_user"),
                    username,
                    username,
                    me.path("profile_image").asText(null),
                    accessToken,
                    token.path("refresh_token").asText(null),
                    token.path("scope").asText(SCOPES),
                    Instant.now().plusSeconds(token.path("expires_in").asLong(2592000)));
        } catch (Exception e) {
            throw connectFailed("Pinterest", e);
        }
    }
}
