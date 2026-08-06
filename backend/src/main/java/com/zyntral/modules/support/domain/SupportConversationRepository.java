package com.zyntral.modules.support.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SupportConversationRepository extends JpaRepository<SupportConversation, UUID> {
}
