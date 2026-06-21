package com.zyntral.modules.ai.domain;

/** Which upstream LLM vendor served a generation. Mirrors PostgreSQL {@code ai_provider_kind}. */
public enum AiProviderKind {
    OPENAI, ANTHROPIC
}
