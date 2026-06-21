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

    // Statuses bound as a parameter (not inline enum literals) so Hibernate casts to the
    // real Postgres type (subscription_status) instead of the Java class name.
    @Query("SELECT count(s) FROM Subscription s WHERE s.status IN :statuses")
    long countByStatusIn(Collection<SubscriptionStatus> statuses);

    default long countLive() {
        return countByStatusIn(java.util.List.of(
                SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING, SubscriptionStatus.PAST_DUE));
    }
}
