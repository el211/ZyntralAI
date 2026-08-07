"use client";

import { useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { WorkspaceProvider, useWorkspace } from "@/lib/workspace";
import { Sidebar } from "@/components/sidebar";
import { CreateWorkspaceModal } from "@/components/create-workspace";

const PAGE_TITLES: Record<string, string> = {
  "/dashboard":           "Overview",
  "/dashboard/ai":        "AI Studio",
  "/dashboard/library":   "Library",
  "/dashboard/posts":     "Posts",
  "/dashboard/calendar":  "Calendar",
  "/dashboard/social":    "Social accounts",
  "/dashboard/billing":   "Billing",
  "/dashboard/api-keys":  "API Keys",
  "/dashboard/settings":  "Settings",
  "/dashboard/admin":     "Admin",
};

function getTitle(pathname: string): string {
  if (PAGE_TITLES[pathname]) return PAGE_TITLES[pathname];
  const match = Object.keys(PAGE_TITLES)
    .filter((k) => k !== "/dashboard")
    .find((k) => pathname.startsWith(k));
  return match ? PAGE_TITLES[match] : "Zyntral AI";
}

function DashboardShell({ children }: { children: React.ReactNode }) {
  const { workspaces, isLoading } = useWorkspace();
  const [modalOpen, setModalOpen] = useState(false);
  const pathname = usePathname();
  const title = getTitle(pathname);

  if (isLoading) {
    return (
      <div style={{ display: "flex", minHeight: "100vh", alignItems: "center", justifyContent: "center", background: "#0d0d0f" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "10px", fontSize: "13px", color: "rgba(240,238,235,0.35)" }}>
          <div style={{
            width: "14px", height: "14px", borderRadius: "50%",
            border: "2px solid rgba(91,91,214,0.6)", borderTopColor: "#5b5bd6",
            animation: "spin 0.7s linear infinite",
          }} />
          Loading…
        </div>
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      </div>
    );
  }

  if (workspaces.length === 0) {
    return (
      <>
        <div style={{
          display: "flex", minHeight: "100vh", alignItems: "center",
          justifyContent: "center", background: "#0d0d0f",
        }}>
          <div style={{ textAlign: "center", maxWidth: "320px" }}>
            <div style={{
              width: "48px", height: "48px", borderRadius: "12px",
              background: "rgba(91,91,214,0.1)", border: "1px solid rgba(91,91,214,0.2)",
              display: "flex", alignItems: "center", justifyContent: "center",
              margin: "0 auto 20px",
            }}>
              <span style={{ fontSize: "22px" }}>+</span>
            </div>
            <h1 style={{ fontSize: "17px", fontWeight: 600, color: "#f0eeeb", marginBottom: "8px" }}>
              Create your first workspace
            </h1>
            <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.4)", lineHeight: 1.6, marginBottom: "20px" }}>
              A workspace is where your content, team, and billing live.
            </p>
            <button
              onClick={() => setModalOpen(true)}
              style={{
                padding: "8px 18px", background: "#5b5bd6", color: "#fff",
                border: "none", borderRadius: "6px", cursor: "pointer",
                fontSize: "13px", fontWeight: 500, fontFamily: "inherit",
              }}
            >
              Create workspace
            </button>
          </div>
        </div>
        <CreateWorkspaceModal open={modalOpen} onClose={() => setModalOpen(false)} />
      </>
    );
  }

  return (
    <div style={{ display: "flex", minHeight: "100vh", background: "#0d0d0f" }}>
      <Sidebar onNewWorkspace={() => setModalOpen(true)} />

      <div style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
        {/* Linear-style top bar */}
        <header style={{
          height: "48px",
          display: "flex",
          alignItems: "center",
          padding: "0 24px",
          borderBottom: "1px solid rgba(255,255,255,0.06)",
          background: "#0d0d0f",
          position: "sticky",
          top: 0,
          zIndex: 10,
        }}>
          <h1 style={{
            fontSize: "14px",
            fontWeight: 600,
            color: "#f0eeeb",
            letterSpacing: "-0.01em",
            margin: 0,
          }}>
            {title}
          </h1>
        </header>

        {/* Main content */}
        <main style={{
          flex: 1,
          overflowY: "auto",
          padding: "28px 32px",
          color: "#f0eeeb",
          fontFamily: "'Inter', system-ui, sans-serif",
        }}>
          {children}
        </main>
      </div>

      <CreateWorkspaceModal open={modalOpen} onClose={() => setModalOpen(false)} />
    </div>
  );
}

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, user, router]);

  if (loading || !user) {
    return (
      <div style={{ display: "flex", minHeight: "100vh", alignItems: "center", justifyContent: "center", background: "#0d0d0f" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "10px", fontSize: "13px", color: "rgba(240,238,235,0.35)" }}>
          <div style={{
            width: "14px", height: "14px", borderRadius: "50%",
            border: "2px solid rgba(91,91,214,0.6)", borderTopColor: "#5b5bd6",
            animation: "spin 0.7s linear infinite",
          }} />
          Loading…
        </div>
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      </div>
    );
  }

  return (
    <WorkspaceProvider>
      <DashboardShell>{children}</DashboardShell>
    </WorkspaceProvider>
  );
}
