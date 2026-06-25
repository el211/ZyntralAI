package com.zyntral.modules.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiAudioRepository extends JpaRepository<AiAudio, UUID> {

    List<AiAudio> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
