package com.zyntral.modules.social.web.dto;

import com.zyntral.modules.social.domain.SocialAccountStatus;
import com.zyntral.modules.social.domain.SocialPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class SocialDtos {

    private SocialDtos() {}

    /**
     * Result of an OAuth connection (the frontend completes the OAuth dance and posts the
     * resulting tokens + account identity here).
     */
    public record ConnectAccountRequest(
            @NotNull SocialPlatform platform,
            @NotBlank String externalId,
            String displayName,
            String handle,
            String avatarUrl,
            @NotBlank String accessToken,
            String refreshToken,
            String scopes,
            Instant tokenExpiresAt
    ) {}

    public record SocialAccountResponse(
            UUID id, SocialPlatform platform, String externalId, String displayName,
            String handle, String avatarUrl, SocialAccountStatus status, Instant createdAt
    ) {}
}
