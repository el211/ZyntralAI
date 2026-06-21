package com.zyntral.modules.social.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.modules.social.config.SocialOAuthProperties;
import com.zyntral.modules.social.domain.SocialPlatform;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

/** TikTok OAuth 2.0 with PKCE. Note: TikTok uses {@code client_key} (mapped from clientId). */
@Component
public class TikTokOAuthProvider extends AbstractRestOAuthProvider {

    private static final String AUTH = "https://www.tiktok.com/v2/auth/authorize/";
    private static final String TOKEN = "https://open.tiktokapis.com/v2/oauth/token/";
    private static final String ME =
            "https://open.tiktokapis.com/v2/user/info/?fields=open_id,display_name,avatar_url";
    private static final String SCOPES = "user.info.basic,video.publish";

    private final SocialOAuthProperties.Provider cfg;

    public TikTokOAuthProvider(SocialOAuthProperties props) { this.cfg = props.tiktok(); }

    @Override public SocialPlatform platform() { return SocialPlatform.TIKTOK; }

    @Override
    public String authorizationUrl(String state, String redirectUri, String codeChallenge) {
        return UriComponentsBuilder.fromUriString(AUTH)
                .queryParam("client_key", cfg.clientId())
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPES)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .encode().build().toUriString();
    }

    @Override
    public OAuthResult exchangeCode(String code, String redirectUri, String codeVerifier) {
        try {
            JsonNode token = postForm(TOKEN, form(
                    "client_key", cfg.clientId(),
                    "client_secret", cfg.clientSecret(),
                    "code", code,
                    "grant_type", "authorization_code",
                    "redirect_uri", redirectUri,
                    "code_verifier", codeVerifier), null);

            String accessToken = token.path("access_token").asText();
            String openId = token.path("open_id").asText(null);
            JsonNode user = getJson(ME, accessToken).path("data").path("user");
            return new OAuthResult(
                    openId != null ? openId : user.path("open_id").asText(),
                    user.path("display_name").asText(null),
                    null,
                    user.path("avatar_url").asText(null),
                    accessToken,
                    token.path("refresh_token").asText(null),
                    token.path("scope").asText(SCOPES),
                    Instant.now().plusSeconds(token.path("expires_in").asLong(86400)));
        } catch (Exception e) {
            throw connectFailed("TikTok", e);
        }
    }
}
