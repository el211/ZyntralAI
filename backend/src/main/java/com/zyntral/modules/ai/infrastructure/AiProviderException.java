package com.zyntral.modules.ai.infrastructure;

/** Thrown when an upstream AI vendor call fails; surfaced as 500 by the global handler. */
public class AiProviderException extends RuntimeException {
    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
