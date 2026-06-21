package com.zyntral.modules.billing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByExternalId(String externalId);

    Optional<Subscription> findFirstByWorkspaceIdAndStatusIn(
            UUID workspaceId, Collection<SubscriptionStatus> statuses);

    Optional<Subscription> findFirstByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    @Query("SELECT count(s) FROM Subscription s WHERE s.status IN "
            + "(com.zyntral.modules.billing.domain.SubscriptionStatus.ACTIVE,"
            + " com.zyntral.modules.billing.domain.SubscriptionStatus.TRIALING,"
            + " com.zyntral.modules.billing.domain.SubscriptionStatus.PAST_DUE)")
    long countLive();
}
