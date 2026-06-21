package com.zyntral.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Binds {@code zyntral.jwt.*} from application.yml. */
@ConfigurationProperties(prefix = "zyntral.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {}
