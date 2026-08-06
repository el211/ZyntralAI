package com.zyntral.modules.content.domain;

/** Lifecycle of a post. Mirrors the PostgreSQL {@code post_status} enum. */
public enum PostStatus {
    DRAFT, SCHEDULED, QUEUED, PUBLISHING, PUBLISHED, FAILED, CANCELLED
}
