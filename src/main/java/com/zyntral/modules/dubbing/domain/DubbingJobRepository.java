package com.zyntral.modules.dubbing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DubbingJobRepository extends JpaRepository<DubbingJob, UUID> {

    Page<DubbingJob> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);

    Optional<DubbingJob> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
