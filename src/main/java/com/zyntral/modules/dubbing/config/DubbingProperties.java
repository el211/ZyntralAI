package com.zyntral.modules.dubbing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code zyntral.dubbing.*}. The API key is per-workspace (BYOK), not configured here. */
@ConfigurationProperties(prefix = "zyntral.dubbing")
public record DubbingProperties(String baseUrl) {

    public DubbingProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.elevenlabs.io";
        }
    }
}
