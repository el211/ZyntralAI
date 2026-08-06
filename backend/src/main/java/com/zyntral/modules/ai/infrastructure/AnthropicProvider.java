package com.zyntral.modules.ai.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.modules.ai.application.AiProvider;
import com.zyntral.modules.ai.config.AiProperties;
import com.zyntral.modules.ai.domain.AiProviderKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Claude adapter (Messages API). Default provider — Claude is the highest-quality
 * option and {@code claude-opus-4-8} is the configured default model.
 */
@Component
public class AnthropicProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicProvider.class);
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final AiProperties.Provider config;
    private final RestClient client;

    public AnthropicProvider(AiProperties props) {
        this.config = props.anthropic();
        this.client = RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("x-api-key", config.apiKey() == null ? "" : config.apiKey())
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();
    }

    @Override
    public AiProviderKind kind() {
        return AiProviderKind.ANTHROPIC;
    }

    @Override
    public AiCompletion complete(AiRequest request) {
        String model = request.model() != null ? request.model() : config.model();
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", request.maxTokens(),
                "temperature", request.temperature(),
                "system", request.systemPrompt() == null ? "" : request.systemPrompt(),
                "messages", List.of(Map.of("role", "user", "content", request.userPrompt()))
        );
        try {
            JsonNode res = client.post()
                    .uri("/messages")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String text = res.path("content").path(0).path("text").asText("");
            int inTok = res.path("usage").path("input_tokens").asInt(0);
            int outTok = res.path("usage").path("output_tokens").asInt(0);
            String usedModel = res.path("model").asText(model);
            return new AiCompletion(text, usedModel, inTok, outTok);
        } catch (Exception ex) {
            log.error("Anthropic generation failed (model={})", model, ex);
            throw new AiProviderException("Anthropic request failed", ex);
        }
    }
}
