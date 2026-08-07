package com.zyntral.modules.ai.application;

import com.zyntral.modules.ai.domain.AiProviderKind;

/**
 * Port over an LLM vendor. Adding a provider = implementing this interface in the
 * infrastructure layer; callers (generation, support agent) never change.
 */
public interface AiProvider {

    AiProviderKind kind();

    AiCompletion complete(AiRequest request);

    /** Provider input. {@code model} null → the adapter's configured default.
     *  {@code apiKey} null → use the platform's configured key (full credit cost);
     *  non-null → workspace BYOK key (discounted credit cost). */
    record AiRequest(
            String systemPrompt,
            String userPrompt,
            String model,
            int maxTokens,
            double temperature,
            String apiKey          // null = platform key, non-null = BYOK
    ) {
        /** Convenience constructor for internal (non-BYOK) calls. */
        public AiRequest(String systemPrompt, String userPrompt, String model,
                         int maxTokens, double temperature) {
            this(systemPrompt, userPrompt, model, maxTokens, temperature, null);
        }
    }

    /** Provider output, including token accounting for the credit ledger and analytics. */
    record AiCompletion(
            String text,
            String model,
            int promptTokens,
            int outputTokens
    ) {}
}
