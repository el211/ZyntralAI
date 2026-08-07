package com.zyntral.modules.ai.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyntral.modules.ai.application.AiProvider;
import com.zyntral.modules.ai.config.AiProperties;
import com.zyntral.modules.ai.domain.AiProviderKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** OpenAI adapter (Chat Completions API). Alternate provider behind the same port. */
@Component
public class OpenAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);

    private final AiProperties.Provider config;
    private final RestClient client;

    public OpenAiProvider(AiProperties props) {
        this.config = props.openai();
        this.client = RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("Authorization", "Bearer " + (config.apiKey() == null ? "" : config.apiKey()))
                .build();
    }

    @Override
    public AiProviderKind kind() {
        return AiProviderKind.OPENAI;
    }

    /** GPT-5.x models dropped max_tokens and only allow temperature=1 (the default).
     *  We detect these by model name prefix and omit unsupported parameters. */
    private static boolean isNewGenModel(String model) {
        return model != null && (model.startsWith("gpt-5") || model.startsWith("o1") || model.startsWith("o3"));
    }

    @Override
    public AiCompletion complete(AiRequest request) {
        String model = request.model() != null ? request.model() : config.model();
        boolean newGen = isNewGenModel(model);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", request.systemPrompt() == null ? "" : request.systemPrompt()),
                Map.of("role", "user", "content", request.userPrompt())));
        body.put("max_completion_tokens", request.maxTokens());
        // temperature is fixed at 1 for GPT-5.x models and cannot be changed
        if (!newGen) {
            body.put("temperature", request.temperature());
        }
        try {
            JsonNode res = client.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String text = res.path("choices").path(0).path("message").path("content").asText("");
            int inTok = res.path("usage").path("prompt_tokens").asInt(0);
            int outTok = res.path("usage").path("completion_tokens").asInt(0);
            String usedModel = res.path("model").asText(model);
            return new AiCompletion(text, usedModel, inTok, outTok);
        } catch (Exception ex) {
            log.error("OpenAI generation failed (model={})", model, ex);
            throw new AiProviderException("OpenAI request failed", ex);
        }
    }
}
