package com.zyntral.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Uniform success envelope so every endpoint returns {@code {data, meta}}.
 * Errors use {@link com.zyntral.common.error.ApiError} instead.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, Object meta) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> ok(T data, Object meta) {
        return new ApiResponse<>(data, meta);
    }
}
