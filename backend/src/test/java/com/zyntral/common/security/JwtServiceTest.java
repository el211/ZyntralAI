package com.zyntral.common.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private final JwtProperties props = new JwtProperties(
            "zyntral-ai",
            "test-secret-test-secret-test-secret-256bit!!",
            Duration.ofMinutes(15),
            Duration.ofDays(30));
    private final JwtService jwt = new JwtService(props);

    @Test
    void issuesAndParsesRoundTrip() {
        UUID userId = UUID.randomUUID();
        String token = jwt.issueAccessToken(userId, "user@zyntral.ai", List.of("USER", "ADMIN"));

        AppPrincipal principal = jwt.parse(token);

        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal.getEmail()).isEqualTo("user@zyntral.ai");
        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtService other = new JwtService(new JwtProperties(
                "zyntral-ai", "another-secret-another-secret-another!!",
                Duration.ofMinutes(15), Duration.ofDays(30)));
        String foreign = other.issueAccessToken(UUID.randomUUID(), "x@y.z", List.of("USER"));

        assertThrows(Exception.class, () -> jwt.parse(foreign));
    }

    @Test
    void rejectsGarbageToken() {
        assertThrows(Exception.class, () -> jwt.parse("not-a-jwt"));
    }
}
