"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api, unwrap } from "@/lib/api";
import { useWorkspace } from "@/lib/workspace";
import { CreditUsage } from "@/lib/types";
import { Sparkles, Users, CreditCard, Share2, TrendingUp, ArrowUpRight, Zap, FileText, CalendarDays, CheckCircle2, Circle } from "lucide-react";

export default function OverviewPage() {
  const { current } = useWorkspace();

  const { data: credits } = useQuery({
    queryKey: ["credits", current?.id],
    enabled: !!current,
    queryFn: async () =>
      unwrap<CreditUsage>((await api.get(`/workspaces/${current!.id}/ai/credits`)).data),
  });

  const { data: socialAccounts = [] } = useQuery({
    queryKey: ["social", current?.id],
    enabled: !!current,
    queryFn: async () =>
      unwrap<any[]>((await api.get(`/workspaces/${current!.id}/social-accounts`)).data),
  });

  const { data: posts = [] } = useQuery({
    queryKey: ["posts", current?.id],
    enabled: !!current,
    queryFn: async () =>
      unwrap<any[]>((await api.get(`/workspaces/${current!.id}/posts`)).data),
  });

  if (!current) return (
    <p style={{ color: "rgba(240,238,235,0.4)", fontSize: "14px" }}>Create a workspace to get started.</p>
  );

  const creditPct = credits ? Math.round((credits.remaining / credits.limit) * 100) : null;

  const checklist = [
    { done: true, label: "Create your workspace", href: "/dashboard" },
    { done: socialAccounts.length > 0, label: "Connect a social account", href: "/dashboard/social" },
    { done: posts.length > 0, label: "Generate your first post", href: "/dashboard/ai" },
    { done: current.plan !== "FREE", label: "Upgrade to Pro for more credits", href: "/dashboard/billing" },
  ];

  return (
    <div style={{ maxWidth: "1100px", display: "flex", flexDirection: "column", gap: "28px" }}>
      {/* Header */}
      <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between" }}>
        <div>
          <h1 style={{ fontSize: "22px", fontWeight: 700, color: "#f0eeeb", letterSpacing: "-0.02em", margin: 0 }}>
            {current.name}
          </h1>
          <p style={{ marginTop: "4px", fontSize: "13px", color: "rgba(240,238,235,0.4)", display: "flex", alignItems: "center", gap: "6px" }}>
            <span style={{ width: "6px", height: "6px", borderRadius: "50%", background: "#34d399", display: "inline-block" }} />
            {current.plan} plan · {current.myRole}
          </p>
        </div>
        <Link href="/dashboard/ai" style={{ textDecoration: "none" }}>
          <button style={{
            display: "inline-flex", alignItems: "center", gap: "6px",
            padding: "8px 16px", borderRadius: "7px", border: "none",
            background: "#F5B319", color: "#0d0d0f",
            fontSize: "13px", fontWeight: 600, cursor: "pointer", fontFamily: "inherit",
          }}>
            <Zap size={14} />
            Generate
          </button>
        </Link>
      </div>

      {/* Stat cards */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "12px" }}>
        <StatCard
          label="AI Credits"
          value={credits ? credits.remaining.toLocaleString() : "—"}
          sub={`/ ${credits?.limit.toLocaleString() ?? "—"}`}
          icon={<Sparkles size={14} style={{ color: "#F5B319" }} />}
          iconBg="rgba(245,179,25,0.1)"
          extra={creditPct !== null ? (
            <div style={{ marginTop: "12px" }}>
              <div style={{ height: "4px", borderRadius: "2px", background: "rgba(255,255,255,0.06)", overflow: "hidden" }}>
                <div style={{ height: "100%", borderRadius: "2px", width: `${creditPct}%`, background: creditPct > 30 ? "#F5B319" : "#EF4444" }} />
              </div>
              <p style={{ fontSize: "11px", color: "rgba(240,238,235,0.35)", marginTop: "5px" }}>{creditPct}% remaining</p>
            </div>
          ) : null}
        />
        <StatCard
          label="Team"
          value={String(current.memberCount ?? 1)}
          sub="members"
          icon={<Users size={14} style={{ color: "#818cf8" }} />}
          iconBg="rgba(129,140,248,0.1)"
        />
        <StatCard
          label="Plan"
          value={current.plan}
          sub={<Link href="/dashboard/billing" style={{ color: "rgba(240,238,235,0.35)", textDecoration: "none", fontSize: "11px", display: "inline-flex", alignItems: "center", gap: "3px" }}>Manage plan <ArrowUpRight size={10} /></Link>}
          icon={<CreditCard size={14} style={{ color: "#34d399" }} />}
          iconBg="rgba(52,211,153,0.1)"
        />
        <StatCard
          label="Your role"
          value={current.myRole}
          sub="in this workspace"
          icon={<TrendingUp size={14} style={{ color: "#F5B319" }} />}
          iconBg="rgba(245,179,25,0.1)"
        />
      </div>

      {/* Bottom two columns */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
        {/* Quick actions */}
        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          <h2 style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.25)", letterSpacing: "0.07em", textTransform: "uppercase", margin: 0 }}>
            Quick actions
          </h2>
          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            <QuickAction href="/dashboard/ai" icon={Sparkles} label="Generate content" desc="AI Studio" color="#F5B319" colorBg="rgba(245,179,25,0.08)" />
            <QuickAction href="/dashboard/social" icon={Share2} label="Connect accounts" desc="Social platforms" color="#818CF8" colorBg="rgba(129,140,248,0.08)" />
            <QuickAction href="/dashboard/posts" icon={FileText} label="View posts" desc="All generated posts" color="#34D399" colorBg="rgba(52,211,153,0.08)" />
            <QuickAction href="/dashboard/calendar" icon={CalendarDays} label="Schedule content" desc="Calendar" color="#F87171" colorBg="rgba(248,113,113,0.08)" />
            <QuickAction href="/dashboard/billing" icon={CreditCard} label="Upgrade plan" desc="Billing" color="#a78bfa" colorBg="rgba(167,139,250,0.08)" />
          </div>
        </div>

        {/* Getting started checklist */}
        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          <h2 style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.25)", letterSpacing: "0.07em", textTransform: "uppercase", margin: 0 }}>
            Getting started
          </h2>
          <div style={{
            borderRadius: "10px", border: "1px solid rgba(255,255,255,0.07)",
            background: "rgba(255,255,255,0.02)", padding: "8px",
            display: "flex", flexDirection: "column", gap: "2px",
          }}>
            {checklist.map(({ done, label, href }) => (
              <Link key={label} href={href} style={{ textDecoration: "none" }}>
                <div style={{
                  display: "flex", alignItems: "center", gap: "10px",
                  padding: "10px 12px", borderRadius: "7px",
                  transition: "background 0.1s",
                }}
                onMouseEnter={(e) => { (e.currentTarget as HTMLDivElement).style.background = "rgba(255,255,255,0.04)"; }}
                onMouseLeave={(e) => { (e.currentTarget as HTMLDivElement).style.background = "transparent"; }}
                >
                  {done
                    ? <CheckCircle2 size={16} style={{ color: "#34d399", flexShrink: 0 }} />
                    : <Circle size={16} style={{ color: "rgba(240,238,235,0.2)", flexShrink: 0 }} />
                  }
                  <span style={{
                    fontSize: "13px",
                    color: done ? "rgba(240,238,235,0.35)" : "rgba(240,238,235,0.7)",
                    textDecoration: done ? "line-through" : "none",
                  }}>
                    {label}
                  </span>
                  {!done && <ArrowUpRight size={12} style={{ color: "rgba(240,238,235,0.2)", marginLeft: "auto" }} />}
                </div>
              </Link>
            ))}
          </div>

          {/* Connected accounts summary */}
          {socialAccounts.length > 0 && (
            <>
              <h2 style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.25)", letterSpacing: "0.07em", textTransform: "uppercase", margin: "8px 0 0" }}>
                Connected accounts
              </h2>
              <div style={{
                borderRadius: "10px", border: "1px solid rgba(255,255,255,0.07)",
                background: "rgba(255,255,255,0.02)", padding: "8px",
                display: "flex", flexDirection: "column", gap: "2px",
              }}>
                {socialAccounts.map((a: any) => (
                  <div key={a.id} style={{
                    display: "flex", alignItems: "center", justifyContent: "space-between",
                    padding: "8px 12px", borderRadius: "7px",
                  }}>
                    <span style={{ fontSize: "13px", color: "rgba(240,238,235,0.6)", textTransform: "capitalize" }}>
                      {a.platform?.toLowerCase()}
                    </span>
                    <span style={{ fontSize: "11px", color: "#34d399" }}>{a.status}</span>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function StatCard({ label, value, sub, icon, iconBg, extra }: {
  label: string; value: string; sub: React.ReactNode;
  icon: React.ReactNode; iconBg: string; extra?: React.ReactNode;
}) {
  return (
    <div style={{
      borderRadius: "10px", border: "1px solid rgba(255,255,255,0.07)",
      background: "rgba(255,255,255,0.02)", padding: "20px",
    }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "14px" }}>
        <p style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.3)", letterSpacing: "0.06em", textTransform: "uppercase" }}>
          {label}
        </p>
        <div style={{ width: "28px", height: "28px", borderRadius: "7px", background: iconBg, display: "flex", alignItems: "center", justifyContent: "center" }}>
          {icon}
        </div>
      </div>
      <p style={{ fontSize: "28px", fontWeight: 700, color: "#f0eeeb", letterSpacing: "-0.02em", margin: 0 }}>
        {value}
        {typeof sub === "string" && <span style={{ fontSize: "13px", fontWeight: 400, color: "rgba(240,238,235,0.35)", marginLeft: "4px" }}>{sub}</span>}
      </p>
      {typeof sub !== "string" && <div style={{ marginTop: "4px" }}>{sub}</div>}
      {extra}
    </div>
  );
}

function QuickAction({ href, icon: Icon, label, desc, color, colorBg }: {
  href: string; icon: any; label: string; desc: string; color: string; colorBg: string;
}) {
  return (
    <Link href={href} style={{ textDecoration: "none" }}>
      <div style={{
        display: "flex", alignItems: "center", gap: "12px",
        padding: "10px 12px", borderRadius: "8px",
        border: "1px solid rgba(255,255,255,0.06)",
        background: "rgba(255,255,255,0.02)",
        transition: "background 0.1s, border-color 0.1s",
        cursor: "pointer",
      }}
      onMouseEnter={(e) => {
        const el = e.currentTarget as HTMLDivElement;
        el.style.background = "rgba(255,255,255,0.04)";
        el.style.borderColor = color + "30";
      }}
      onMouseLeave={(e) => {
        const el = e.currentTarget as HTMLDivElement;
        el.style.background = "rgba(255,255,255,0.02)";
        el.style.borderColor = "rgba(255,255,255,0.06)";
      }}
      >
        <div style={{ width: "32px", height: "32px", borderRadius: "7px", background: colorBg, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
          <Icon size={15} style={{ color }} />
        </div>
        <div style={{ minWidth: 0 }}>
          <p style={{ fontSize: "13px", fontWeight: 500, color: "#f0eeeb", margin: 0 }}>{label}</p>
          <p style={{ fontSize: "11px", color: "rgba(240,238,235,0.35)", margin: 0 }}>{desc}</p>
        </div>
        <ArrowUpRight size={13} style={{ color: "rgba(240,238,235,0.2)", marginLeft: "auto", flexShrink: 0 }} />
      </div>
    </Link>
  );
}
