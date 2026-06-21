package com.zyntral.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues and validates signed access tokens (HS256). Refresh tokens are opaque,
 * random, and stored hashed in the DB (see refresh_tokens) — JWT is only the
 * short-lived access token.
 */
@Service
public class JwtService {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_EMAIL = "email";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTokenTtl())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Parses & verifies the token, returning the principal, or throws if invalid/expired. */
    @SuppressWarnings("unchecked")
    public AppPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get(CLAIM_EMAIL, String.class);
        List<String> roles = claims.get(CLAIM_ROLES, List.class);
        return new AppPrincipal(userId, email, roles == null ? List.of() : roles);
    }
}
