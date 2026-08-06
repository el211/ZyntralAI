package com.zyntral.modules.billing.web.dto;

import com.zyntral.common.domain.PlanCode;
import com.zyntral.modules.billing.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class BillingDtos {

    private BillingDtos() {}

    public record CreateCheckoutRequest(
            @NotNull PaymentProviderKind provider,
            @NotNull PlanCode plan,
            @NotNull BillingInterval interval,
            @NotBlank String successUrl,
            @NotBlank String cancelUrl
    ) {}

    public record CheckoutResponse(String url, String externalId) {}

    public record CancelRequest(boolean atPeriodEnd) {}

    public record SubscriptionResponse(
            UUID id, PlanCode plan, PaymentProviderKind provider, SubscriptionStatus status,
            BillingInterval interval, Instant currentPeriodEnd, boolean cancelAtPeriodEnd
    ) {
        public static SubscriptionResponse from(Subscription s) {
            return new SubscriptionResponse(s.getId(), s.getPlan(), s.getProvider(), s.getStatus(),
                    s.getInterval(), s.getCurrentPeriodEnd(), s.isCancelAtPeriodEnd());
        }
    }

    public record InvoiceResponse(
            UUID id, String number, int amountCents, String currency, InvoiceStatus status,
            String hostedUrl, String pdfUrl, Instant createdAt
    ) {
        public static InvoiceResponse from(Invoice i) {
            return new InvoiceResponse(i.getId(), i.getNumber(), i.getAmountCents(), i.getCurrency(),
                    i.getStatus(), i.getHostedUrl(), i.getPdfUrl(), i.getCreatedAt());
        }
    }

    public record PlanResponse(
            PlanCode code, String name, int monthlyPriceCents, int annualPriceCents,
            int aiCreditsMonthly, int maxTeamMembers, int maxSocialAccounts, int maxWorkspaces
    ) {
        public static PlanResponse from(Plan p) {
            return new PlanResponse(p.getCode(), p.getName(), p.getMonthlyPriceCents(),
                    p.getAnnualPriceCents(), p.getAiCreditsMonthly(), p.getMaxTeamMembers(),
                    p.getMaxSocialAccounts(), p.getMaxWorkspaces());
        }
    }
}
