package com.zyntral.modules.ai.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AiGenerationRepository extends JpaRepository<AiGeneration, UUID> {

    Page<AiGeneration> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);

    /** [provider, count] breakdown for admin AI-usage analytics. */
    @Query("SELECT g.provider, count(g) FROM AiGeneration g GROUP BY g.provider")
    List<Object[]> countByProvider();
}
