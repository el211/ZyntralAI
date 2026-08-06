package com.zyntral.modules.content.domain;

/** Per-platform publish state of a post. Mirrors PostgreSQL {@code target_status}. */
public enum TargetStatus {
    PENDING, PUBLISHING, PUBLISHED, FAILED
}
