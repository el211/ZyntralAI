package com.zyntral.modules.billing.domain;

import com.zyntral.common.domain.PlanCode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Reference data for a subscription tier: pricing and the per-plan limits that gate
 * AI credits, team size, connected accounts, and workspaces. Seeded in V1 migration.
 */
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "plan_code")
    private PlanCode code;

    @Column(nullable = false)
    private String name;

    @Column(name = "monthly_price_cents", nullable = false)
    private int monthlyPriceCents;

    @Column(name = "annual_price_cents", nullable = false)
    private int annualPriceCents;

    @Column(name = "ai_credits_monthly", nullable = false)
    private int aiCreditsMonthly;

    @Column(name = "max_team_members", nullable = false)
    private int maxTeamMembers;

    @Column(name = "max_social_accounts", nullable = false)
    private int maxSocialAccounts;

    @Column(name = "max_workspaces", nullable = false)
    private int maxWorkspaces;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected Plan() {}

    public PlanCode getCode() { return code; }
    public String getName() { return name; }
    public int getMonthlyPriceCents() { return monthlyPriceCents; }
    public int getAnnualPriceCents() { return annualPriceCents; }
    public int getAiCreditsMonthly() { return aiCreditsMonthly; }
    public int getMaxTeamMembers() { return maxTeamMembers; }
    public int getMaxSocialAccounts() { return maxSocialAccounts; }
    public int getMaxWorkspaces() { return maxWorkspaces; }
    public boolean isActive() { return active; }
}
