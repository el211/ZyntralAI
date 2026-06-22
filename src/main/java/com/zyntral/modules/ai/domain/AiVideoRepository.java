package com.zyntral.modules.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiVideoRepository extends JpaRepository<AiVideo, UUID> {

    List<AiVideo> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<AiVideo> findByStatusIn(Collection<String> statuses);

    Optional<AiVideo> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
