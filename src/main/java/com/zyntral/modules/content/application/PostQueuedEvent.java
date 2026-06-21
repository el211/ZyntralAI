package com.zyntral.modules.content.application;

import java.util.UUID;

/** Raised when a post is queued for immediate publish; dispatched to the worker after commit. */
public record PostQueuedEvent(UUID postId) {}
