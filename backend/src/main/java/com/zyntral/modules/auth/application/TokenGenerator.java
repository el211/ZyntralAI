package com.zyntral.modules.auth.application;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generates opaque secrets (refresh / verification / reset tokens) and their SHA-256
 * hashes. The raw secret is returned to the caller once (sent to the client / email);
 * only the hash is ever persisted.
 */
@Component
public class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public record Secret(String raw, String hash) {}

    public Secret generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Secret(raw, hash(raw));
    }

    public String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
