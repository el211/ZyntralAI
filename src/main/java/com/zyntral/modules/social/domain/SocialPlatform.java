package com.zyntral.modules.social.domain;

/**
 * Supported social networks. Mirrors the PostgreSQL {@code social_platform} enum. Adding a
 * platform = add a value here (+ migration {@code ALTER TYPE}) and a publisher adapter.
 */
public enum SocialPlatform {
    LINKEDIN, TWITTER, FACEBOOK, INSTAGRAM, TIKTOK, YOUTUBE, PINTEREST;

    /** Per-platform hard character cap (0 = effectively unlimited), used for validation. */
    public int characterLimit() {
        return switch (this) {
            case TWITTER -> 280;
            case INSTAGRAM -> 2200;
            case TIKTOK -> 2200;
            case PINTEREST -> 500;
            case LINKEDIN -> 3000;
            case FACEBOOK, YOUTUBE -> 0;
        };
    }
}
