package com.zyntral.modules.crm.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmProspectRepository extends JpaRepository<CrmProspect, UUID> {
    List<CrmProspect> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
    Optional<CrmProspect> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    void deleteByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
