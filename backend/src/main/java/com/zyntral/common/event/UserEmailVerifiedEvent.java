package com.zyntral.common.event;

import java.util.UUID;

/**
 * Published by the auth module after a user verifies their email. Consumed by the
 * workspace module to bootstrap a personal workspace. Keeps auth and workspace
 * decoupled — neither references the other directly.
 */
public record UserEmailVerifiedEvent(UUID userId, String email, String fullName, String locale) {}
