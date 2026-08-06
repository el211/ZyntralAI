"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { useAuth } from "@/lib/auth";
import {
  LayoutDashboard, Sparkles, FileText, CalendarDays, Share2,
  CreditCard, Settings, Shield, LogOut, Images, Zap,
} from "lucide-react";

const nav = [
  { href: "/dashboard",          label: "Overview",        icon: LayoutDashboard },
  { href: "/dashboard/ai",       label: "AI Studio",       icon: Sparkles },
  { href: "/dashboard/library",  label: "Library",         icon: Images },
  { href: "/dashboard/posts",    label: "Posts",           icon: FileText },
  { href: "/dashboard/calendar", label: "Calendar",        icon: CalendarDays },
  { href: "/dashboard/social",   label: "Social",          icon: Share2 },
  { href: "/dashboard/billing",  label: "Billing",         icon: CreditCard },
  { href: "/dashboard/settings", label: "Settings",        icon: Settings },
];

export function Sidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const isAdmin = user?.roles?.includes("ADMIN");

  const initial = user?.email?.[0]?.toUpperCase() ?? "U";

  return (
    <aside className="flex w-56 shrink-0 flex-col border-r"
      style={{ borderColor: "hsl(var(--border))", background: "hsl(var(--card))" }}>
      {/* Logo */}
      <div className="flex items-center gap-2.5 px-5 py-5">
        <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg"
          style={{ background: "#F5B319" }}>
          <Zap className="h-3.5 w-3.5" style={{ color: "hsl(230,22%,5%)" }} />
        </div>
        <span className="font-syne font-bold text-[15px] tracking-tight">Zyntral AI</span>
      </div>

      {/* Divider */}
      <div className="mx-4 mb-3 h-px" style={{ background: "hsl(var(--border))" }} />

      {/* Navigation */}
      <nav className="flex-1 space-y-0.5 px-3">
        {nav.map((item) => {
          const active = pathname === item.href || (item.href !== "/dashboard" && pathname.startsWith(item.href));
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "group relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-all duration-150",
                active
                  ? "font-medium"
                  : "text-muted-foreground hover:text-foreground hover:bg-secondary/50",
              )}
              style={active ? {
                background: "rgba(245,179,25,0.1)",
                color: "#F5B319",
              } : undefined}
            >
              {active && (
                <div className="absolute left-0 top-1/2 -translate-y-1/2 h-5 w-[3px] rounded-r-full"
                  style={{ background: "#F5B319" }} />
              )}
              <item.icon className="h-4 w-4 shrink-0" />
              {item.label}
            </Link>
          );
        })}

        {isAdmin && (
          <Link
            href="/dashboard/admin"
            className={cn(
              "group relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-all duration-150",
              pathname.startsWith("/dashboard/admin")
                ? "font-medium"
                : "text-muted-foreground hover:text-foreground hover:bg-secondary/50",
            )}
            style={pathname.startsWith("/dashboard/admin") ? {
              background: "rgba(245,179,25,0.1)",
              color: "#F5B319",
            } : undefined}
          >
            {pathname.startsWith("/dashboard/admin") && (
              <div className="absolute left-0 top-1/2 -translate-y-1/2 h-5 w-[3px] rounded-r-full"
                style={{ background: "#F5B319" }} />
            )}
            <Shield className="h-4 w-4 shrink-0" />
            Admin
          </Link>
        )}
      </nav>

      {/* Divider */}
      <div className="mx-4 mt-3 h-px" style={{ background: "hsl(var(--border))" }} />

      {/* User + Logout */}
      <div className="p-3 space-y-1">
        <div className="flex items-center gap-3 rounded-lg px-3 py-2">
          <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold"
            style={{ background: "rgba(245,179,25,0.15)", color: "#F5B319" }}>
            {initial}
          </div>
          <span className="truncate text-xs text-muted-foreground">{user?.email}</span>
        </div>
        <button
          onClick={logout}
          className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
        >
          <LogOut className="h-4 w-4 shrink-0" />
          Sign out
        </button>
      </div>
    </aside>
  );
}
