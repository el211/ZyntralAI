"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { useWorkspace } from "@/lib/workspace";
import {
  LayoutDashboard, Sparkles, FileText, CalendarDays, Share2,
  CreditCard, Settings, Shield, LogOut, Images, Zap, ChevronDown,
  Search, SquarePen,
} from "lucide-react";

const mainNav = [
  { href: "/dashboard",          label: "Overview",    icon: LayoutDashboard },
  { href: "/dashboard/ai",       label: "AI Studio",   icon: Sparkles },
  { href: "/dashboard/library",  label: "Library",     icon: Images },
  { href: "/dashboard/posts",    label: "Posts",       icon: FileText },
  { href: "/dashboard/calendar", label: "Calendar",    icon: CalendarDays },
];

const accountNav = [
  { href: "/dashboard/social",   label: "Social",    icon: Share2 },
  { href: "/dashboard/billing",  label: "Billing",   icon: CreditCard },
  { href: "/dashboard/settings", label: "Settings",  icon: Settings },
];

function NavItem({ href, label, icon: Icon, pathname }: {
  href: string; label: string; icon: React.ElementType; pathname: string;
}) {
  const active = pathname === href || (href !== "/dashboard" && pathname.startsWith(href));

  return (
    <Link
      href={href}
      style={{
        display: "flex",
        alignItems: "center",
        gap: "8px",
        padding: "4px 8px",
        borderRadius: "5px",
        fontSize: "13px",
        fontWeight: active ? 500 : 400,
        color: active ? "#f0eeeb" : "rgba(240,238,235,0.45)",
        background: active ? "rgba(255,255,255,0.07)" : "transparent",
        textDecoration: "none",
        transition: "background 0.1s, color 0.1s",
        lineHeight: "20px",
        userSelect: "none",
      }}
      onMouseEnter={(e) => {
        if (!active) {
          (e.currentTarget as HTMLAnchorElement).style.background = "rgba(255,255,255,0.04)";
          (e.currentTarget as HTMLAnchorElement).style.color = "rgba(240,238,235,0.7)";
        }
      }}
      onMouseLeave={(e) => {
        if (!active) {
          (e.currentTarget as HTMLAnchorElement).style.background = "transparent";
          (e.currentTarget as HTMLAnchorElement).style.color = "rgba(240,238,235,0.45)";
        }
      }}
    >
      <Icon size={14} style={{ flexShrink: 0, opacity: active ? 1 : 0.7 }} />
      {label}
    </Link>
  );
}

export function Sidebar({ onNewWorkspace }: { onNewWorkspace?: () => void }) {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const { workspaces, current, setCurrent } = useWorkspace();
  const isAdmin = user?.roles?.includes("ADMIN");
  const initial = user?.email?.[0]?.toUpperCase() ?? "U";

  return (
    <aside
      style={{
        width: "220px",
        flexShrink: 0,
        background: "#111113",
        borderRight: "1px solid rgba(255,255,255,0.06)",
        display: "flex",
        flexDirection: "column",
        height: "100vh",
        position: "sticky",
        top: 0,
        fontFamily: "'Inter', system-ui, sans-serif",
      }}
    >
      {/* ── Logo + actions row ── */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "10px 12px",
          minHeight: "48px",
        }}
      >
        {/* Workspace switcher */}
        <button
          onClick={onNewWorkspace}
          style={{
            display: "flex",
            alignItems: "center",
            gap: "7px",
            background: "none",
            border: "none",
            cursor: "pointer",
            padding: "3px 4px",
            borderRadius: "5px",
            color: "#f0eeeb",
            transition: "background 0.1s",
            flex: 1,
            minWidth: 0,
          }}
          onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.background = "rgba(255,255,255,0.06)"; }}
          onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.background = "transparent"; }}
        >
          <div
            style={{
              width: "22px",
              height: "22px",
              borderRadius: "5px",
              background: "linear-gradient(135deg, #5b5bd6 0%, #7c3aed 100%)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexShrink: 0,
            }}
          >
            <Zap size={12} color="#fff" fill="#fff" />
          </div>
          <span
            style={{
              fontSize: "13px",
              fontWeight: 600,
              letterSpacing: "-0.01em",
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis",
              flex: 1,
            }}
          >
            {current?.name ?? "Zyntral AI"}
          </span>
          <ChevronDown size={12} style={{ color: "rgba(240,238,235,0.35)", flexShrink: 0 }} />
        </button>

        {/* Icon actions */}
        <div style={{ display: "flex", alignItems: "center", gap: "2px", marginLeft: "4px" }}>
          {[Search, SquarePen].map((Icon, i) => (
            <button
              key={i}
              style={{
                background: "none",
                border: "none",
                cursor: "pointer",
                padding: "4px",
                borderRadius: "4px",
                color: "rgba(240,238,235,0.35)",
                display: "flex",
                transition: "background 0.1s, color 0.1s",
              }}
              onMouseEnter={(e) => {
                (e.currentTarget as HTMLButtonElement).style.background = "rgba(255,255,255,0.06)";
                (e.currentTarget as HTMLButtonElement).style.color = "rgba(240,238,235,0.8)";
              }}
              onMouseLeave={(e) => {
                (e.currentTarget as HTMLButtonElement).style.background = "transparent";
                (e.currentTarget as HTMLButtonElement).style.color = "rgba(240,238,235,0.35)";
              }}
            >
              <Icon size={14} />
            </button>
          ))}
        </div>
      </div>

      {/* ── Workspace plan badge ── */}
      {current && (
        <div style={{ padding: "0 12px 8px" }}>
          <select
            value={current.id}
            onChange={(e) => setCurrent(e.target.value)}
            style={{
              width: "100%",
              background: "rgba(255,255,255,0.04)",
              border: "1px solid rgba(255,255,255,0.07)",
              borderRadius: "5px",
              color: "rgba(240,238,235,0.55)",
              fontSize: "11px",
              padding: "3px 6px",
              cursor: "pointer",
              outline: "none",
              fontFamily: "inherit",
            }}
          >
            {workspaces.map((w) => (
              <option key={w.id} value={w.id} style={{ background: "#111113" }}>
                {w.name} · {w.plan}
              </option>
            ))}
            <option value="__new__" style={{ background: "#111113" }}>+ New workspace</option>
          </select>
        </div>
      )}

      {/* ── Main nav ── */}
      <div style={{ padding: "4px 8px", flex: 1, overflow: "hidden auto" }}>

        <div style={{ display: "flex", flexDirection: "column", gap: "1px" }}>
          {mainNav.map((item) => (
            <NavItem key={item.href} pathname={pathname} {...item} />
          ))}
        </div>

        {/* Section: Account */}
        <div
          style={{
            marginTop: "20px",
            marginBottom: "4px",
            padding: "0 8px",
            fontSize: "11px",
            fontWeight: 500,
            letterSpacing: "0.05em",
            textTransform: "uppercase",
            color: "rgba(240,238,235,0.2)",
            userSelect: "none",
          }}
        >
          Account
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: "1px" }}>
          {accountNav.map((item) => (
            <NavItem key={item.href} pathname={pathname} {...item} />
          ))}
          {isAdmin && (
            <NavItem href="/dashboard/admin" label="Admin" icon={Shield} pathname={pathname} />
          )}
        </div>
      </div>

      {/* ── User footer ── */}
      <div
        style={{
          borderTop: "1px solid rgba(255,255,255,0.06)",
          padding: "8px",
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "8px",
            padding: "6px 8px",
            borderRadius: "5px",
            marginBottom: "2px",
          }}
        >
          <div
            style={{
              width: "22px",
              height: "22px",
              borderRadius: "50%",
              background: "rgba(91,91,214,0.25)",
              border: "1px solid rgba(91,91,214,0.3)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: "10px",
              fontWeight: 700,
              color: "#818cf8",
              flexShrink: 0,
            }}
          >
            {initial}
          </div>
          <span
            style={{
              fontSize: "12px",
              color: "rgba(240,238,235,0.4)",
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
              flex: 1,
            }}
          >
            {user?.email}
          </span>
        </div>

        <button
          onClick={logout}
          style={{
            display: "flex",
            alignItems: "center",
            gap: "8px",
            width: "100%",
            padding: "5px 8px",
            borderRadius: "5px",
            background: "none",
            border: "none",
            cursor: "pointer",
            fontSize: "13px",
            color: "rgba(240,238,235,0.35)",
            transition: "background 0.1s, color 0.1s",
            fontFamily: "inherit",
          }}
          onMouseEnter={(e) => {
            (e.currentTarget as HTMLButtonElement).style.background = "rgba(239,68,68,0.08)";
            (e.currentTarget as HTMLButtonElement).style.color = "rgba(239,68,68,0.8)";
          }}
          onMouseLeave={(e) => {
            (e.currentTarget as HTMLButtonElement).style.background = "transparent";
            (e.currentTarget as HTMLButtonElement).style.color = "rgba(240,238,235,0.35)";
          }}
        >
          <LogOut size={14} style={{ flexShrink: 0 }} />
          Sign out
        </button>
      </div>
    </aside>
  );
}
