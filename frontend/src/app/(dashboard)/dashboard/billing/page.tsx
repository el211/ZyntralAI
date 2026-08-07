"use client";

import { useState } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";
import { api, unwrap, apiErrorMessage } from "@/lib/api";
import { useWorkspace } from "@/lib/workspace";
import { Plan, PlanCode } from "@/lib/types";
import { formatCents } from "@/lib/utils";
import { Check } from "lucide-react";

type Interval = "MONTHLY" | "ANNUAL";

export default function BillingPage() {
  const { current } = useWorkspace();
  const [interval, setInterval] = useState<Interval>("MONTHLY");
  const [error, setError] = useState<string | null>(null);

  const { data: plans = [] } = useQuery({
    queryKey: ["plans"],
    queryFn: async () => unwrap<Plan[]>((await api.get("/billing/plans")).data),
  });

  const checkout = useMutation({
    mutationFn: async (plan: PlanCode) => {
      const origin = window.location.origin;
      const res = await api.post(`/workspaces/${current!.id}/billing/checkout`, {
        provider: "STRIPE",
        plan,
        interval,
        successUrl: `${origin}/dashboard/billing?status=success`,
        cancelUrl: `${origin}/dashboard/billing?status=cancel`,
      });
      return unwrap<{ url: string }>(res.data);
    },
    onSuccess: (data) => { if (data.url) window.location.href = data.url; },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  if (!current) return <p style={{ color: "rgba(240,238,235,0.4)", fontSize: "14px" }}>Select a workspace first.</p>;

  return (
    <div style={{ maxWidth: "1100px" }}>
      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "32px" }}>
        <div>
          <h1 style={{ fontSize: "22px", fontWeight: 600, color: "#f0eeeb", letterSpacing: "-0.02em", margin: 0 }}>
            Billing
          </h1>
          <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.4)", marginTop: "4px" }}>
            Current plan: {current.plan}
          </p>
        </div>
        <div style={{
          display: "flex", background: "rgba(255,255,255,0.05)",
          border: "1px solid rgba(255,255,255,0.08)", borderRadius: "7px", padding: "3px",
        }}>
          {(["MONTHLY", "ANNUAL"] as Interval[]).map((i) => (
            <button key={i} onClick={() => setInterval(i)} style={{
              padding: "5px 16px", borderRadius: "5px", border: "none", cursor: "pointer",
              fontSize: "13px", fontWeight: 500, fontFamily: "inherit",
              background: interval === i ? "#5b5bd6" : "transparent",
              color: interval === i ? "#fff" : "rgba(240,238,235,0.45)",
              transition: "background 0.15s, color 0.15s",
            }}>
              {i === "MONTHLY" ? "Monthly" : "Annual"}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div style={{
          marginBottom: "20px", padding: "10px 14px", borderRadius: "6px",
          background: "rgba(239,68,68,0.08)", border: "1px solid rgba(239,68,68,0.2)",
          fontSize: "13px", color: "rgba(239,68,68,0.9)",
        }}>
          {error}
        </div>
      )}

      {/* Plan cards */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "16px", marginBottom: "48px" }}>
        {plans.map((plan) => {
          const price = interval === "ANNUAL" ? plan.annualPriceCents : plan.monthlyPriceCents;
          const isCurrent = current.plan === plan.code;
          const isPro = plan.code === "PRO";
          return (
            <div key={plan.code} style={{
              borderRadius: "10px",
              border: isCurrent
                ? "1px solid rgba(91,91,214,0.5)"
                : isPro
                ? "1px solid rgba(91,91,214,0.25)"
                : "1px solid rgba(255,255,255,0.08)",
              background: isCurrent
                ? "rgba(91,91,214,0.06)"
                : isPro
                ? "rgba(91,91,214,0.04)"
                : "rgba(255,255,255,0.02)",
              padding: "28px",
              display: "flex",
              flexDirection: "column",
              gap: "0",
              position: "relative",
            }}>
              {isPro && !isCurrent && (
                <div style={{
                  position: "absolute", top: "-1px", left: "50%", transform: "translateX(-50%)",
                  background: "#5b5bd6", color: "#fff", fontSize: "11px", fontWeight: 600,
                  padding: "3px 12px", borderRadius: "0 0 6px 6px", letterSpacing: "0.04em",
                }}>
                  POPULAR
                </div>
              )}

              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "20px" }}>
                <span style={{ fontSize: "15px", fontWeight: 600, color: "#f0eeeb" }}>{plan.name}</span>
                {isCurrent && (
                  <span style={{
                    fontSize: "11px", fontWeight: 600, color: "#5b5bd6",
                    background: "rgba(91,91,214,0.12)", padding: "2px 8px", borderRadius: "4px",
                  }}>
                    Current
                  </span>
                )}
              </div>

              <div style={{ marginBottom: "24px" }}>
                <span style={{ fontSize: "40px", fontWeight: 700, color: "#f0eeeb", letterSpacing: "-0.03em" }}>
                  {price === 0 ? "Free" : formatCents(price)}
                </span>
                {price > 0 && (
                  <span style={{ fontSize: "13px", color: "rgba(240,238,235,0.4)", marginLeft: "4px" }}>
                    /{interval === "ANNUAL" ? "yr" : "mo"}
                  </span>
                )}
              </div>

              <ul style={{ listStyle: "none", padding: 0, margin: "0 0 28px", display: "flex", flexDirection: "column", gap: "10px" }}>
                <FeatureItem>{plan.aiCreditsMonthly.toLocaleString()} AI credits / mo</FeatureItem>
                <FeatureItem>{plan.maxTeamMembers} team member{plan.maxTeamMembers !== 1 ? "s" : ""}</FeatureItem>
                <FeatureItem>{plan.maxSocialAccounts} social accounts</FeatureItem>
                <FeatureItem>{plan.maxWorkspaces} workspace{plan.maxWorkspaces !== 1 ? "s" : ""}</FeatureItem>
              </ul>

              <button
                disabled={isCurrent || plan.code === "FREE" || checkout.isPending}
                onClick={() => checkout.mutate(plan.code)}
                style={{
                  width: "100%", padding: "10px", borderRadius: "7px", border: "none",
                  cursor: isCurrent || plan.code === "FREE" ? "default" : "pointer",
                  fontSize: "13px", fontWeight: 500, fontFamily: "inherit",
                  background: isCurrent
                    ? "rgba(255,255,255,0.04)"
                    : plan.code === "FREE"
                    ? "rgba(255,255,255,0.04)"
                    : "#5b5bd6",
                  color: isCurrent || plan.code === "FREE"
                    ? "rgba(240,238,235,0.3)"
                    : "#fff",
                  transition: "opacity 0.15s",
                  opacity: checkout.isPending ? 0.6 : 1,
                }}
              >
                {isCurrent ? "Current plan" : plan.code === "FREE" ? "Free" : "Upgrade"}
              </button>
            </div>
          );
        })}
      </div>

      {/* Usage summary */}
      <div style={{
        borderRadius: "10px", border: "1px solid rgba(255,255,255,0.07)",
        background: "rgba(255,255,255,0.02)", padding: "24px 28px",
      }}>
        <h2 style={{ fontSize: "14px", fontWeight: 600, color: "#f0eeeb", margin: "0 0 16px" }}>
          Plan includes
        </h2>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "20px" }}>
          {[
            { label: "AI Credits", value: current.plan === "FREE" ? "50 / mo" : current.plan === "PRO" ? "2,000 / mo" : "20,000 / mo" },
            { label: "Team Members", value: current.plan === "FREE" ? "1" : current.plan === "PRO" ? "5" : "25" },
            { label: "Social Accounts", value: current.plan === "FREE" ? "2" : current.plan === "PRO" ? "10" : "50" },
            { label: "Workspaces", value: current.plan === "FREE" ? "1" : current.plan === "PRO" ? "3" : "10" },
          ].map(({ label, value }) => (
            <div key={label} style={{ textAlign: "center", padding: "16px", borderRadius: "8px", background: "rgba(255,255,255,0.03)" }}>
              <div style={{ fontSize: "22px", fontWeight: 700, color: "#f0eeeb", letterSpacing: "-0.02em" }}>{value}</div>
              <div style={{ fontSize: "12px", color: "rgba(240,238,235,0.4)", marginTop: "4px" }}>{label}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function FeatureItem({ children }: { children: React.ReactNode }) {
  return (
    <li style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "13px", color: "rgba(240,238,235,0.6)" }}>
      <Check size={14} style={{ color: "#5b5bd6", flexShrink: 0 }} />
      {children}
    </li>
  );
}
