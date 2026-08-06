package com.zyntral.modules.ai.application;

import com.zyntral.modules.ai.domain.AiProviderKind;

/**
 * Port over an LLM vendor. Adding a provider = implementing this interface in the
 * infrastructure layer; callers (generation, support agent) never change.
 */
public interface AiProvider {

    AiProviderKind kind();

    AiCompletion complete(AiRequest request);

    /** Provider input. {@code model} null → the adapter's configured default. */
    record AiRequest(
            String systemPrompt,
            String userPrompt,
            String model,
            int maxTokens,
            double temperature
    ) {}

    /** Provider output, including token accounting for the credit ledger and analytics. */
    record AiCompletion(
            String text,
            String model,
            int promptTokens,
            int outputTokens
    ) {}
}
