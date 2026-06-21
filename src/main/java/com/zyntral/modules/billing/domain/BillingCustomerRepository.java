package com.zyntral.modules.billing.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillingCustomerRepository extends JpaRepository<BillingCustomer, UUID> {

    Optional<BillingCustomer> findByWorkspaceIdAndProvider(UUID workspaceId, PaymentProviderKind provider);
}
