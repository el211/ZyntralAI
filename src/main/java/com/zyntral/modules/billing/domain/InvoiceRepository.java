package com.zyntral.modules.billing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByProviderAndExternalId(PaymentProviderKind provider, String externalId);

    Page<Invoice> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);

    @Query("SELECT coalesce(sum(i.amountCents), 0) FROM Invoice i "
            + "WHERE i.status = com.zyntral.modules.billing.domain.InvoiceStatus.PAID")
    long sumPaidAmountCents();
}
