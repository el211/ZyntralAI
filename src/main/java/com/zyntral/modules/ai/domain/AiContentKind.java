package com.zyntral.modules.ai.domain;

/** What the user wants generated. Mirrors the PostgreSQL {@code ai_content_kind} enum. */
public enum AiContentKind {
    LINKEDIN_POST, X_POST, INSTAGRAM_CAPTION, TIKTOK_IDEA, FACEBOOK_POST,
    MARKETING_COPY, PRODUCT_DESCRIPTION, EMAIL_CAMPAIGN, BLOG_OUTLINE, CTA, HASHTAGS
}
