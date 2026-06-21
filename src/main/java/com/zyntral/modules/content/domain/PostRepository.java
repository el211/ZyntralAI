package com.zyntral.modules.content.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    Optional<Post> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Page<Post> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);

    Page<Post> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(
            UUID workspaceId, PostStatus status, Pageable pageable);

    /** Calendar view: posts scheduled within a window. */
    @Query("""
           SELECT p FROM Post p
           WHERE p.workspaceId = :workspaceId
             AND p.scheduledAt BETWEEN :from AND :to
           ORDER BY p.scheduledAt
           """)
    List<Post> findCalendar(@Param("workspaceId") UUID workspaceId,
                            @Param("from") Instant from, @Param("to") Instant to);

    /** Due jobs for the publisher worker (uses the partial index idx_posts_due). */
    @Query("""
           SELECT p.id FROM Post p
           WHERE p.status IN (com.zyntral.modules.content.domain.PostStatus.SCHEDULED,
                              com.zyntral.modules.content.domain.PostStatus.QUEUED)
             AND p.scheduledAt <= :now
           ORDER BY p.scheduledAt
           """)
    List<UUID> findDuePostIds(@Param("now") Instant now, Pageable pageable);
}
