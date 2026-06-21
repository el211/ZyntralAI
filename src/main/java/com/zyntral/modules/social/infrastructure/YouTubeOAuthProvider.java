package com.zyntral.modules.social.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.modules.social.config.SocialOAuthProperties;
import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

/** YouTube via Google OAuth 2.0 (with PKCE + offline access for refresh tokens). */
@Component
public class YouTubeOAuthProvider extends AbstractRestOAuthProvider {

    private static final String AUTH = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN = "https://oauth2.googleapis.com/token";
    private static final String USERINFO = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String SCOPES =
            "openid profile https://www.googleapis.com/auth/youtube.readonly";

    private final SocialOAuthProperties.Provider cfg;

    public YouTubeOAuthProvider(SocialOAuthProperties props) { this.cfg = props.youtube(); }

    @Override public SocialPlatform platform() { return SocialPlatform.YOUTUBE; }

    @Override
    public String authorizationUrl(String state, String redirectUri, String codeChallenge) {
        return UriComponentsBuilder.fromUriString(AUTH)
                .queryParam("client_id", cfg.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPES)
                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .encode().build().toUriString();
    }

    @Override
    public OAuthResult exchangeCode(String code, String redirectUri, String codeVerifier) {
        try {
            JsonNode token = postForm(TOKEN, form(
                    "client_id", cfg.clientId(),
                    "client_secret", cfg.clientSecret(),
                    "code", code,
                    "redirect_uri", redirectUri,
                    "grant_type", "authorization_code",
                    "code_verifier", codeVerifier), null);

            String accessToken = token.path("access_token").asText();
            JsonNode me = getJson(USERINFO, accessToken);
            return new OAuthResult(
                    me.path("sub").asText(),
                    me.path("name").asText(null),
                    null,
                    me.path("picture").asText(null),
                    accessToken,
                    token.path("refresh_token").asText(null),
                    token.path("scope").asText(SCOPES),
                    Instant.now().plusSeconds(token.path("expires_in").asLong(3600)));
        } catch (Exception e) {
            throw connectFailed("YouTube", e);
        }
    }
}
