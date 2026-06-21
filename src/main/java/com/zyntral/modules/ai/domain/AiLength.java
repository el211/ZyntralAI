package com.zyntral.modules.ai.domain;

/** Desired output size. Mirrors the PostgreSQL {@code ai_length} enum. */
public enum AiLength {
    SHORT, MEDIUM, LONG;

    /** Rough output-token budget per length, used to cap provider cost. */
    public int maxTokens() {
        return switch (this) {
            case SHORT -> 300;
            case MEDIUM -> 700;
            case LONG -> 1500;
        };
    }
}
