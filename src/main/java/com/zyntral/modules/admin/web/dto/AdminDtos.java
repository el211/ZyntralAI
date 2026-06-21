package com.zyntral.modules.admin.web.dto;

import com.zyntral.common.domain.PlanCode;
import com.zyntral.modules.billing.domain.BillingInterval;
import com.zyntral.modules.billing.domain.PaymentProviderKind;
import com.zyntral.modules.billing.domain.Subscription;
import com.zyntral.modules.billing.domain.SubscriptionStatus;
import com.zyntral.modules.user.domain.Role;
import com.zyntral.modules.user.domain.User;
import com.zyntral.modules.user.domain.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminDtos {

    private AdminDtos() {}

    public record OverviewResponse(
            long totalUsers, long totalWorkspaces, long liveSubscriptions,
            long totalRevenueCents, long totalAiGenerations, long totalSocialAccounts
    ) {}

    public record UserAdminResponse(
            UUID id, String email, String fullName, UserStatus status, boolean emailVerified,
            List<String> roles, Instant lastLoginAt, Instant createdAt
    ) {
        public static UserAdminResponse from(User u) {
            return new UserAdminResponse(u.getId(), u.getEmail(), u.getFullName(), u.getStatus(),
                    u.isEmailVerified(), u.getRoles().stream().map(Role::getCode).toList(),
                    u.getLastLoginAt(), u.getCreatedAt());
        }
    }

    public record SubscriptionAdminResponse(
            UUID id, UUID workspaceId, PlanCode plan, PaymentProviderKind provider,
            SubscriptionStatus status, BillingInterval interval, Instant currentPeriodEnd
    ) {
        public static SubscriptionAdminResponse from(Subscription s) {
            return new SubscriptionAdminResponse(s.getId(), s.getWorkspaceId(), s.getPlan(),
                    s.getProvider(), s.getStatus(), s.getInterval(), s.getCurrentPeriodEnd());
        }
    }
}
