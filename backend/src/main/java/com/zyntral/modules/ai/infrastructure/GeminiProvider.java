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

/** Google Gemini adapter (Generative Language API). Alternate provider behind the same port. */
@Component
public class GeminiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final AiProperties.Provider config;
    private final RestClient client;

    public GeminiProvider(AiProperties props) {
        this.config = props.gemini();
        this.client = RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("x-goog-api-key", config.apiKey() == null ? "" : config.apiKey())
                .build();
    }

    @Override
    public AiProviderKind kind() {
        return AiProviderKind.GEMINI;
    }

    @Override
    public AiCompletion complete(AiRequest request) {
        String model = request.model() != null ? request.model() : config.model();
        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(
                        Map.of("text", request.systemPrompt() == null ? "" : request.systemPrompt()))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", request.userPrompt())))),
                "generationConfig", Map.of(
                        "maxOutputTokens", request.maxTokens(),
                        "temperature", request.temperature())
        );
        try {
            JsonNode res = client.post()
                    .uri("/models/" + model + ":generateContent")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String text = res.path("candidates").path(0).path("content")
                    .path("parts").path(0).path("text").asText("");
            int inTok = res.path("usageMetadata").path("promptTokenCount").asInt(0);
            int outTok = res.path("usageMetadata").path("candidatesTokenCount").asInt(0);
            return new AiCompletion(text, model, inTok, outTok);
        } catch (Exception ex) {
            log.error("Gemini generation failed (model={})", model, ex);
            throw new AiProviderException("Gemini request failed", ex);
        }
    }
}
