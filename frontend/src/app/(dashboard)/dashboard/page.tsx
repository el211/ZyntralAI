"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api, unwrap } from "@/lib/api";
import { useWorkspace } from "@/lib/workspace";
import { CreditUsage } from "@/lib/types";
import { Sparkles, Users, CreditCard, Share2, TrendingUp, ArrowUpRight, Zap } from "lucide-react";

export default function OverviewPage() {
  const { current } = useWorkspace();

  const { data: credits } = useQuery({
    queryKey: ["credits", current?.id],
    enabled: !!current,
    queryFn: async () =>
      unwrap<CreditUsage>((await api.get(`/workspaces/${current!.id}/ai/credits`)).data),
  });

  if (!current) return (
    <p className="text-muted-foreground text-sm">Create a workspace to get started.</p>
  );

  const creditPct = credits ? Math.round((credits.remaining / credits.limit) * 100) : null;

  return (
    <div className="space-y-7 animate-fade-in-up">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="font-syne text-2xl font-bold">{current.name}</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            <span className="inline-flex items-center gap-1.5">
              <span className="inline-block h-1.5 w-1.5 rounded-full bg-emerald-500" />
              {current.plan} plan · {current.myRole}
            </span>
          </p>
        </div>
        <Link href="/dashboard/ai">
          <button className="inline-flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold transition-all hover:opacity-90"
            style={{ background: "#F5B319", color: "hsl(230,22%,5%)" }}>
            <Zap className="h-4 w-4" />
            Generate
          </button>
        </Link>
      </div>

      {/* Stat cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Credits */}
        <div className="rounded-xl p-5 space-y-3"
          style={{ background: "hsl(var(--card))", border: "1px solid hsl(var(--border))" }}>
          <div className="flex items-center justify-between">
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">AI Credits</p>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg"
              style={{ background: "rgba(245,179,25,0.1)" }}>
              <Sparkles className="h-3.5 w-3.5" style={{ color: "#F5B319" }} />
            </div>
          </div>
          <div className="space-y-2">
            <p className="font-syne text-2xl font-bold">
              {credits ? credits.remaining.toLocaleString() : "—"}
              <span className="text-sm font-normal text-muted-foreground ml-1">
                / {credits?.limit.toLocaleString() ?? "—"}
              </span>
            </p>
            {creditPct !== null && (
              <div className="space-y-1">
                <div className="h-1.5 rounded-full overflow-hidden"
                  style={{ background: "hsl(var(--secondary))" }}>
                  <div className="h-full rounded-full transition-all duration-500"
                    style={{
                      width: `${creditPct}%`,
                      background: creditPct > 30 ? "#F5B319" : "#EF4444"
                    }} />
                </div>
                <p className="text-xs text-muted-foreground">{creditPct}% remaining</p>
              </div>
            )}
          </div>
        </div>

        {/* Team */}
        <div className="rounded-xl p-5 space-y-3"
          style={{ background: "hsl(var(--card))", border: "1px solid hsl(var(--border))" }}>
          <div className="flex items-center justify-between">
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Team</p>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg"
              style={{ background: "rgba(99,102,241,0.1)" }}>
              <Users className="h-3.5 w-3.5 text-indigo-400" />
            </div>
          </div>
          <p className="font-syne text-2xl font-bold">{current.memberCount}</p>
          <p className="text-xs text-muted-foreground">members in workspace</p>
        </div>

        {/* Plan */}
        <div className="rounded-xl p-5 space-y-3"
          style={{ background: "hsl(var(--card))", border: "1px solid hsl(var(--border))" }}>
          <div className="flex items-center justify-between">
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Plan</p>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg"
              style={{ background: "rgba(16,185,129,0.1)" }}>
              <CreditCard className="h-3.5 w-3.5 text-emerald-400" />
            </div>
          </div>
          <p className="font-syne text-2xl font-bold">{current.plan}</p>
          <Link href="/dashboard/billing"
            className="inline-flex items-center gap-1 text-xs transition-colors hover:text-primary"
            style={{ color: "rgba(255,255,255,0.35)" }}>
            Manage plan <ArrowUpRight className="h-3 w-3" />
          </Link>
        </div>

        {/* Role */}
        <div className="rounded-xl p-5 space-y-3"
          style={{ background: "hsl(var(--card))", border: "1px solid hsl(var(--border))" }}>
          <div className="flex items-center justify-between">
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Your role</p>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg"
              style={{ background: "rgba(245,179,25,0.1)" }}>
              <TrendingUp className="h-3.5 w-3.5" style={{ color: "#F5B319" }} />
            </div>
          </div>
          <p className="font-syne text-2xl font-bold">{current.myRole}</p>
          <p className="text-xs text-muted-foreground">in this workspace</p>
        </div>
      </div>

      {/* Quick actions */}
      <div>
        <h2 className="font-syne font-semibold mb-3 text-sm uppercase tracking-wide text-muted-foreground">
          Quick actions
        </h2>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <QuickAction
            href="/dashboard/ai"
            icon={Sparkles}
            label="Generate content"
            desc="AI Studio"
            color="#F5B319"
            colorBg="rgba(245,179,25,0.08)"
          />
          <QuickAction
            href="/dashboard/social"
            icon={Share2}
            label="Connect accounts"
            desc="Social platforms"
            color="#818CF8"
            colorBg="rgba(129,140,248,0.08)"
          />
          <QuickAction
            href="/dashboard/settings"
            icon={Users}
            label="Invite team"
            desc="Settings"
            color="#34D399"
            colorBg="rgba(52,211,153,0.08)"
          />
          <QuickAction
            href="/dashboard/billing"
            icon={CreditCard}
            label="Upgrade plan"
            desc="Billing"
            color="#F87171"
            colorBg="rgba(248,113,113,0.08)"
          />
        </div>
      </div>
    </div>
  );
}

function QuickAction({
  href, icon: Icon, label, desc, color, colorBg,
}: {
  href: string; icon: any; label: string; desc: string; color: string; colorBg: string;
}) {
  return (
    <Link href={href}>
      <div className="group flex items-center gap-4 rounded-xl p-4 transition-all duration-150 hover:-translate-y-0.5"
        style={{
          background: "hsl(var(--card))",
          border: "1px solid hsl(var(--border))",
        }}
        onMouseEnter={(e) => {
          (e.currentTarget as HTMLDivElement).style.borderColor = color + "40";
        }}
        onMouseLeave={(e) => {
          (e.currentTarget as HTMLDivElement).style.borderColor = "hsl(var(--border))";
        }}>
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg"
          style={{ background: colorBg }}>
          <Icon className="h-4 w-4" style={{ color }} />
        </div>
        <div className="min-w-0">
          <p className="text-sm font-medium truncate">{label}</p>
          <p className="text-xs text-muted-foreground">{desc}</p>
        </div>
        <ArrowUpRight className="ml-auto h-3.5 w-3.5 shrink-0 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity" />
      </div>
    </Link>
  );
}
