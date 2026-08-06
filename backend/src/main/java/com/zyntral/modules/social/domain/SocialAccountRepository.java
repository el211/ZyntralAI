package com.zyntral.modules.social.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

    List<SocialAccount> findByWorkspaceId(UUID workspaceId);

    long countByWorkspaceId(UUID workspaceId);

    Optional<SocialAccount> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<SocialAccount> findByWorkspaceIdAndPlatformAndExternalId(
            UUID workspaceId, SocialPlatform platform, String externalId);

    /** [platform, count] breakdown for admin social-account statistics. */
    @Query("SELECT a.platform, count(a) FROM SocialAccount a GROUP BY a.platform")
    List<Object[]> countByPlatform();
}
