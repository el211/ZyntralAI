package com.zyntral.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error envelope returned for every non-2xx response (RFC-7807-flavoured).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,            // ErrorCode name, e.g. VALIDATION_FAILED
        String message,          // localized, human-readable
        String path,
        String traceId,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}

    public static ApiError of(int status, ErrorCode code, String message, String path,
                              String traceId, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status, code.name(), message, path, traceId,
                fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors);
    }
}
