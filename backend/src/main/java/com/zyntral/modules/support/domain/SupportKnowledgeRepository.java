package com.zyntral.modules.support.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportKnowledgeRepository extends JpaRepository<SupportKnowledge, UUID> {

    List<SupportKnowledge> findByAgentId(UUID agentId);

    Optional<SupportKnowledge> findByIdAndAgentId(UUID id, UUID agentId);
}
