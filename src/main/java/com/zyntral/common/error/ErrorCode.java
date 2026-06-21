package com.zyntral.common.error;

import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable error codes returned in the {@code error} field of every
 * error response. Clients switch on these; the human-readable {@code message} is i18n'd.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "error.validation"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "error.unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "error.forbidden"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "error.not_found"),
    CONFLICT(HttpStatus.CONFLICT, "error.conflict"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "error.rate_limited"),
    BUSINESS_RULE(HttpStatus.BAD_REQUEST, "error.business_rule"),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal"),

    // domain-specific
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "auth.invalid_credentials"),
    EMAIL_TAKEN(HttpStatus.CONFLICT, "auth.email_taken"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "auth.email_not_verified"),
    TOKEN_INVALID(HttpStatus.BAD_REQUEST, "auth.token_invalid"),
    PLAN_LIMIT_REACHED(HttpStatus.FORBIDDEN, "billing.plan_limit_reached"),
    AI_CREDITS_EXHAUSTED(HttpStatus.FORBIDDEN, "ai.credits_exhausted");

    private final HttpStatus status;
    private final String messageKey;

    ErrorCode(HttpStatus status, String messageKey) {
        this.status = status;
        this.messageKey = messageKey;
    }

    public HttpStatus status() {
        return status;
    }

    public String messageKey() {
        return messageKey;
    }
}
