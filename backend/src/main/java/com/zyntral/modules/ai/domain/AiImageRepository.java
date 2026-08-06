package com.zyntral.modules.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiImageRepository extends JpaRepository<AiImage, UUID> {

    List<AiImage> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
