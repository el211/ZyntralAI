package com.zyntral.modules.billing.application;

import com.zyntral.common.domain.PlanCode;
import com.zyntral.common.error.ApiException;
import com.zyntral.common.error.ErrorCode;
import com.zyntral.common.web.PageResponse;
import com.zyntral.modules.billing.application.PaymentProvider.*;
import com.zyntral.modules.billing.domain.*;
import com.zyntral.modules.user.domain.UserRepository;
import com.zyntral.modules.workspace.application.WorkspaceAccess;
import com.zyntral.modules.workspace.domain.Workspace;
import com.zyntral.modules.workspace.domain.WorkspaceRepository;
import com.zyntral.modules.workspace.domain.WorkspaceRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Billing use-cases plus idempotent webhook application. Subscriptions are created/updated by
 * provider webhooks (not at checkout), keeping the local state a faithful mirror of the provider.
 * Activating a paid plan updates the workspace tier; cancellation reverts it to FREE.
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final PaymentProviderRegistry providers;
    private final SubscriptionRepository subscriptions;
    private final InvoiceRepository invoices;
    private final WebhookEventRepository webhookEvents;
    private final WorkspaceRepository workspaces;
    private final com.zyntral.modules.billing.domain.PlanRepository plans;
    private final UserRepository users;
    private final WorkspaceAccess access;

    public BillingService(PaymentProviderRegistry providers, SubscriptionRepository subscriptions,
                          InvoiceRepository invoices, WebhookEventRepository webhookEvents,
                          WorkspaceRepository workspaces,
                          com.zyntral.modules.billing.domain.PlanRepository plans,
                          UserRepository users, WorkspaceAccess access) {
        this.providers = providers;
        this.subscriptions = subscriptions;
        this.invoices = invoices;
        this.webhookEvents = webhookEvents;
        this.workspaces = workspaces;
        this.plans = plans;
        this.users = users;
        this.access = access;
    }

    // ---- checkout / management ----

    @Transactional(readOnly = true)
    public CheckoutSession createCheckout(UUID workspaceId, UUID userId, PaymentProviderKind providerKind,
                                          PlanCode planCode, BillingInterval interval,
                                          String successUrl, String cancelUrl) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.OWNER);
        if (planCode == PlanCode.FREE) {
            throw new ApiException(ErrorCode.BUSINESS_RULE, new Object[]{"FREE plan needs no checkout"});
        }
        Plan plan = plans.findById(planCode).orElseThrow();
        long amount = interval == BillingInterval.ANNUAL
                ? plan.getAnnualPriceCents() : plan.getMonthlyPriceCents();
        String email = users.findById(userId).map(u -> u.getEmail()).orElse(null);

        return providers.resolve(providerKind).createSubscriptionCheckout(
                new CheckoutCommand(workspaceId, email, planCode, interval, amount, "USD",
                        successUrl, cancelUrl));
    }

    @Transactional
    public void cancel(UUID workspaceId, UUID userId, boolean atPeriodEnd) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.OWNER);
        Subscription sub = liveSubscription(workspaceId);
        providers.resolve(sub.getProvider()).cancelSubscription(sub.getExternalId(), atPeriodEnd);
        if (atPeriodEnd) sub.markCancelAtPeriodEnd(); else sub.markCanceled();
    }

    @Transactional(readOnly = true)
    public Subscription getCurrentSubscription(UUID workspaceId, UUID userId) {
        access.requireMember(workspaceId, userId);
        return subscriptions.findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .orElseThrow(() -> ApiException.notFound("subscription", workspaceId));
    }

    @Transactional(readOnly = true)
    public PageResponse<Invoice> listInvoices(UUID workspaceId, UUID userId, int page, int size) {
        access.requireAtLeast(workspaceId, userId, WorkspaceRole.ADMIN);
        return PageResponse.from(
                invoices.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, PageRequest.of(page, size)));
    }

    // ---- webhook application (idempotent) ----

    @Transactional
    public void applyWebhook(PaymentProviderKind providerKind, String rawPayload, String signature) {
        WebhookOutcome outcome = providers.resolve(providerKind).handleWebhook(rawPayload, signature);
        if (outcome == null) return;   // irrelevant event

        // idempotency guard — replays are no-ops
        var existing = webhookEvents.findByProviderAndEventId(providerKind, outcome.eventId());
        if (existing.isPresent() && existing.get().isProcessed()) return;
        WebhookEvent record = existing.orElseGet(() -> webhookEvents.save(
                WebhookEvent.received(providerKind, outcome.eventId(), outcome.eventType(), rawPayload)));

        UUID workspaceId = resolveWorkspace(outcome);
        if (workspaceId == null) {
            log.warn("Webhook {} could not be mapped to a workspace; skipping", outcome.eventId());
            record.markProcessed();
            return;
        }
        if (outcome.subscription() != null) {
            applySubscription(workspaceId, providerKind, outcome.subscription());
        }
        if (outcome.invoice() != null) {
            applyInvoice(workspaceId, providerKind, outcome.invoice());
        }
        record.markProcessed();
    }

    private void applySubscription(UUID workspaceId, PaymentProviderKind providerKind,
                                   SubscriptionChange change) {
        Subscription sub = subscriptions.findByExternalId(change.externalId())
                .orElseGet(() -> Subscription.create(workspaceId,
                        change.plan() != null ? change.plan() : PlanCode.FREE,
                        providerKind, change.externalId(),
                        change.interval() != null ? change.interval() : BillingInterval.MONTHLY));
        if (sub.getId() == null) subscriptions.save(sub);

        Workspace ws = workspaces.findById(workspaceId).orElse(null);
        if (change.canceled() || change.status() == SubscriptionStatus.CANCELED) {
            sub.markCanceled();
            if (ws != null) ws.changePlan(PlanCode.FREE);   // downgrade on cancellation
        } else {
            PlanCode plan = change.plan() != null ? change.plan() : sub.getPlan();
            sub.activate(plan, change.periodStart(), change.periodEnd());
            sub.updateStatus(change.status());
            if (ws != null && change.status().isLive()) ws.changePlan(plan);
        }
    }

    private void applyInvoice(UUID workspaceId, PaymentProviderKind providerKind, InvoiceChange change) {
        if (change.externalId() == null) return;
        var existing = invoices.findByProviderAndExternalId(providerKind, change.externalId());
        if (existing.isPresent()) {
            if (change.status() == InvoiceStatus.PAID) existing.get().markPaid();
            return;
        }
        UUID subId = subscriptions.findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .map(Subscription::getId).orElse(null);
        invoices.save(Invoice.of(workspaceId, subId, providerKind, change.externalId(),
                change.number(), change.amountCents(), change.currency(), change.status(),
                change.hostedUrl(), change.pdfUrl(), null));
    }

    private UUID resolveWorkspace(WebhookOutcome outcome) {
        if (outcome.workspaceId() != null) return outcome.workspaceId();
        if (outcome.subscriptionExternalId() != null) {
            return subscriptions.findByExternalId(outcome.subscriptionExternalId())
                    .map(Subscription::getWorkspaceId).orElse(null);
        }
        return null;
    }

    private Subscription liveSubscription(UUID workspaceId) {
        return subscriptions.findFirstByWorkspaceIdAndStatusIn(workspaceId,
                        EnumSet.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE,
                                SubscriptionStatus.PAST_DUE))
                .orElseThrow(() -> ApiException.notFound("active subscription", workspaceId));
    }
}
