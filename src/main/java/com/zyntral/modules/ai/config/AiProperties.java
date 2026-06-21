package com.zyntral.modules.ai.config;

import com.zyntral.modules.ai.domain.AiProviderKind;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code zyntral.ai.*}. */
@ConfigurationProperties(prefix = "zyntral.ai")
public record AiProperties(
        AiProviderKind defaultProvider,
        Provider anthropic,
        Provider openai,
        Provider gemini
) {
    public record Provider(String apiKey, String model, String baseUrl) {}
}
