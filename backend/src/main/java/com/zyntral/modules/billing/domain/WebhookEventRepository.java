package com.zyntral.modules.billing.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, java.util.UUID> {

    Optional<WebhookEvent> findByProviderAndEventId(PaymentProviderKind provider, String eventId);
}
