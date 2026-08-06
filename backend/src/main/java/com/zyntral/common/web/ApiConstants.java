package com.zyntral.common.web;

/** Central place for API versioning so the prefix is never hard-coded ad hoc. */
public final class ApiConstants {

    private ApiConstants() {}

    public static final String API_V1 = "/api/v1";

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
}
