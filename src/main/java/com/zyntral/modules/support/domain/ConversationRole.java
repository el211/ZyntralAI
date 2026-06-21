package com.zyntral.modules.support.domain;

/** Author of a support message. Mirrors the PostgreSQL {@code conversation_role} enum. */
public enum ConversationRole {
    USER, ASSISTANT, SYSTEM
}
