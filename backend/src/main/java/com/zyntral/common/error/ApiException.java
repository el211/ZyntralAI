package com.zyntral.common.error;

/**
 * Base class for all expected (handled) application errors. Carries an {@link ErrorCode}
 * and optional message arguments for i18n interpolation. The {@link GlobalExceptionHandler}
 * turns these into the uniform error envelope.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final transient Object[] args;

    public ApiException(ErrorCode code) {
        this(code, null);
    }

    public ApiException(ErrorCode code, Object[] args) {
        super(code.name());
        this.code = code;
        this.args = args;
    }

    public ErrorCode code() {
        return code;
    }

    public Object[] args() {
        return args;
    }

    // --- convenient factories used across modules ---

    public static ApiException notFound(String resource, Object id) {
        return new ApiException(ErrorCode.NOT_FOUND, new Object[]{resource, id});
    }

    public static ApiException conflict(ErrorCode code) {
        return new ApiException(code);
    }

    public static ApiException forbidden() {
        return new ApiException(ErrorCode.FORBIDDEN);
    }

    public static ApiException business(ErrorCode code, Object... args) {
        return new ApiException(code, args);
    }
}
