package com.zyntral.modules.social.application;

import com.zyntral.modules.social.domain.SocialPlatform;

import java.time.Instant;

/**
 * Port for a platform's OAuth 2.0 authorization-code flow. An adapter builds the consent URL
 * and exchanges the returned code for tokens + the connected account's profile. Adding OAuth
 * for another network = one more implementation.
 */
public interface SocialOAuthProvider {

    SocialPlatform platform();

    /**
     * Consent screen URL the user is sent to. {@code state} is an opaque CSRF/binding token;
     * {@code codeChallenge} is the PKCE S256 challenge (providers that don't use PKCE ignore it).
     */
    String authorizationUrl(String state, String redirectUri, String codeChallenge);

    /**
     * Exchanges the authorization code for tokens and fetches the account profile.
     * {@code codeVerifier} is the PKCE verifier matching the earlier challenge (ignored by
     * non-PKCE providers).
     */
    OAuthResult exchangeCode(String code, String redirectUri, String codeVerifier);

    record OAuthResult(
            String externalId,
            String displayName,
            String handle,
            String avatarUrl,
            String accessToken,
            String refreshToken,
            String scopes,
            Instant expiresAt
    ) {}
}
