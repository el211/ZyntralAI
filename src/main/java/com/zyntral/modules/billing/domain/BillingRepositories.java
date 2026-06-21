package com.zyntral.modules.billing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/** Billing repositories grouped in one file for locality. */
public final class BillingRepositories {

    private BillingRepositories() {}

    public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
        Optional<Subscription> findByExternalId(String externalId);
        Optional<Subscription> findFirstByWorkspaceIdAndStatusIn(
                UUID workspaceId, java.util.Collection<SubscriptionStatus> statuses);
        Optional<Subscription> findFirstByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

        @Query("SELECT count(s) FROM Subscription s WHERE s.status IN "
                + "(com.zyntral.modules.billing.domain.SubscriptionStatus.ACTIVE,"
                + " com.zyntral.modules.billing.domain.SubscriptionStatus.TRIALING,"
                + " com.zyntral.modules.billing.domain.SubscriptionStatus.PAST_DUE)")
        long countLive();
    }

    public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
        Optional<Invoice> findByProviderAndExternalId(PaymentProviderKind provider, String externalId);
        Page<Invoice> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);

        @Query("SELECT coalesce(sum(i.amountCents), 0) FROM Invoice i "
                + "WHERE i.status = com.zyntral.modules.billing.domain.InvoiceStatus.PAID")
        long sumPaidAmountCents();
    }

    public interface BillingCustomerRepository extends JpaRepository<BillingCustomer, UUID> {
        Optional<BillingCustomer> findByWorkspaceIdAndProvider(UUID workspaceId, PaymentProviderKind provider);
    }

    public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
        Optional<PaymentTransaction> findByProviderAndExternalId(PaymentProviderKind provider, String externalId);
    }

    public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
        Optional<WebhookEvent> findByProviderAndEventId(PaymentProviderKind provider, String eventId);
    }
}
