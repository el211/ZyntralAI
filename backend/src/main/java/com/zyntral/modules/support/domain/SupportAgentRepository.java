package com.zyntral.modules.support.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportAgentRepository extends JpaRepository<SupportAgent, UUID> {

    List<SupportAgent> findByWorkspaceId(UUID workspaceId);

    Optional<SupportAgent> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<SupportAgent> findByPublicKey(String publicKey);
}
