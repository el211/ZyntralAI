package com.zyntral.modules.social.domain;

/** Connection health of a social account. Mirrors PostgreSQL {@code social_acct_status}. */
public enum SocialAccountStatus {
    CONNECTED, EXPIRED, REVOKED, ERROR
}
