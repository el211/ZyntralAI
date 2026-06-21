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

    // Status bound as a parameter (not an inline enum literal) so Hibernate casts to the
    // real Postgres type (invoice_status) instead of the Java class name.
    @Query("SELECT coalesce(sum(i.amountCents), 0) FROM Invoice i WHERE i.status = :status")
    long sumAmountCentsByStatus(InvoiceStatus status);

    default long sumPaidAmountCents() {
        return sumAmountCentsByStatus(InvoiceStatus.PAID);
    }
}
