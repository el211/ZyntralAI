package com.zyntral.common.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code zyntral.crypto.*}. {@code secret} should be a base64-encoded 256-bit key. */
@ConfigurationProperties(prefix = "zyntral.crypto")
public record CryptoProperties(String secret) {}
