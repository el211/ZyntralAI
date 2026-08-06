package com.zyntral.modules.social.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code zyntral.social.*}. {@code redirectBaseUrl} is the backend's public base
 * (the OAuth callback lives under it); {@code webRedirectUrl} is where the user lands in the
 * SPA after connecting. One {@link Provider} per network.
 */
@ConfigurationProperties(prefix = "zyntral.social")
public record SocialOAuthProperties(
        String redirectBaseUrl,
        String webRedirectUrl,
        Provider linkedin,
        Provider twitter,
        Provider facebook,
        Provider instagram,
        Provider tiktok,
        Provider youtube,
        Provider pinterest
) {
    /** clientId doubles as TikTok's {@code client_key}. */
    public record Provider(String clientId, String clientSecret) {
        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank();
        }
    }
}
