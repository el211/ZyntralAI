package com.zyntral.modules.social.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.modules.social.application.SocialOAuthProvider;
import com.zyntral.modules.social.config.SocialOAuthProperties;
import com.zyntral.modules.social.domain.SocialPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

/**
 * LinkedIn OAuth 2.0 (authorization-code) adapter. Scopes:
 * {@code openid profile email} (OpenID Connect → member id via {@code /v2/userinfo}) and
 * {@code w_member_social} (required to publish on the member's behalf).
 */
@Component
public class LinkedInOAuthProvider implements SocialOAuthProvider {

    private static final Logger log = LoggerFactory.getLogger(LinkedInOAuthProvider.class);

    private static final String AUTH_URL = "https://www.linkedin.com/oauth/v2/authorization";
    private static final String TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String USERINFO_URL = "https://api.linkedin.com/v2/userinfo";
    private static final String SCOPES = "openid profile email w_member_social";

    private final SocialOAuthProperties.Provider config;
    private final RestClient http = RestClient.create();

    public LinkedInOAuthProvider(SocialOAuthProperties props) {
        this.config = props.linkedin();
    }

    @Override
    public SocialPlatform platform() {
        return SocialPlatform.LINKEDIN;
    }

    @Override
    public String authorizationUrl(String state, String redirectUri, String codeChallenge) {
        // LinkedIn does not use PKCE — codeChallenge is ignored.
        return UriComponentsBuilder.fromUriString(AUTH_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", config.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("scope", SCOPES)
                .encode().build()
                .toUriString();
    }

    @Override
    public OAuthResult exchangeCode(String code, String redirectUri, String codeVerifier) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("code", code);
            form.add("redirect_uri", redirectUri);
            form.add("client_id", config.clientId());
            form.add("client_secret", config.clientSecret());

            JsonNode token = http.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);

            String accessToken = token.path("access_token").asText();
            String refreshToken = token.path("refresh_token").asText(null);
            String scope = token.path("scope").asText(SCOPES);
            Instant expiresAt = Instant.now().plusSeconds(token.path("expires_in").asLong(3600));

            JsonNode profile = http.get()
                    .uri(USERINFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);

            return new OAuthResult(
                    profile.path("sub").asText(),
                    profile.path("name").asText(null),
                    null,                                   // LinkedIn doesn't expose a public handle here
                    profile.path("picture").asText(null),
                    accessToken, refreshToken, scope, expiresAt);
        } catch (Exception e) {
            log.error("LinkedIn OAuth exchange failed", e);
            throw new ApiException(ErrorCode.BUSINESS_RULE,
                    new Object[]{"Could not connect LinkedIn account"});
        }
    }
}
