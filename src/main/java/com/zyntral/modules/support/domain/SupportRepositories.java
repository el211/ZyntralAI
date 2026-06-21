package com.zyntral.modules.support.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Support module repositories grouped for locality. */
public final class SupportRepositories {

    private SupportRepositories() {}

    public interface SupportAgentRepository extends JpaRepository<SupportAgent, UUID> {
        List<SupportAgent> findByWorkspaceId(UUID workspaceId);
        Optional<SupportAgent> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
        Optional<SupportAgent> findByPublicKey(String publicKey);
    }

    public interface SupportKnowledgeRepository extends JpaRepository<SupportKnowledge, UUID> {
        List<SupportKnowledge> findByAgentId(UUID agentId);
        Optional<SupportKnowledge> findByIdAndAgentId(UUID id, UUID agentId);
    }

    public interface SupportConversationRepository extends JpaRepository<SupportConversation, UUID> {
    }

    public interface SupportMessageRepository extends JpaRepository<SupportMessage, UUID> {
        List<SupportMessage> findByConversationIdOrderByCreatedAt(UUID conversationId);
    }
}
