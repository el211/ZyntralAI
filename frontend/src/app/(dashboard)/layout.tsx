"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { WorkspaceProvider, useWorkspace } from "@/lib/workspace";
import { Sidebar } from "@/components/sidebar";
import { ThemeToggle } from "@/components/theme-toggle";
import { Select } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { CreateWorkspaceModal } from "@/components/create-workspace";
import { Plus, ChevronDown } from "lucide-react";

const NEW_WS = "__new_workspace__";

function Topbar({ onNewWorkspace }: { onNewWorkspace: () => void }) {
  const { workspaces, current, setCurrent } = useWorkspace();
  const { user } = useAuth();
  const initial = user?.email?.[0]?.toUpperCase() ?? "U";

  return (
    <header className="flex h-13 items-center justify-between border-b px-5"
      style={{ borderColor: "hsl(var(--border))", background: "hsl(var(--card))", minHeight: "52px" }}>
      <Select
        className="h-8 w-52 text-sm border-transparent bg-secondary/50 rounded-lg"
        value={current?.id ?? ""}
        onChange={(e) => {
          if (e.target.value === NEW_WS) { onNewWorkspace(); return; }
          setCurrent(e.target.value);
        }}
      >
        {workspaces.length === 0 && <option value="">No workspace</option>}
        {workspaces.map((w) => (
          <option key={w.id} value={w.id}>{w.name} · {w.plan}</option>
        ))}
        <option value={NEW_WS}>+ New workspace</option>
      </Select>

      <div className="flex items-center gap-3">
        <ThemeToggle />
        <div className="flex items-center gap-2 rounded-lg px-2.5 py-1.5"
          style={{ background: "rgba(245,179,25,0.08)", border: "1px solid rgba(245,179,25,0.15)" }}>
          <div className="flex h-6 w-6 items-center justify-center rounded-full text-[11px] font-bold"
            style={{ background: "rgba(245,179,25,0.2)", color: "#F5B319" }}>
            {initial}
          </div>
          <span className="text-xs text-muted-foreground hidden sm:block">{user?.email}</span>
        </div>
      </div>
    </header>
  );
}

function EmptyState({ onCreate }: { onCreate: () => void }) {
  return (
    <div className="flex min-h-screen flex-1 items-center justify-center p-6 text-center">
      <div className="max-w-sm space-y-5">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl"
          style={{ background: "rgba(245,179,25,0.1)", border: "1px solid rgba(245,179,25,0.2)" }}>
          <Plus className="h-6 w-6" style={{ color: "#F5B319" }} />
        </div>
        <div className="space-y-2">
          <h1 className="font-syne text-xl font-bold">Create your first workspace</h1>
          <p className="text-sm text-muted-foreground">
            A workspace is where your content, team, and billing live. Create one to get started.
          </p>
        </div>
        <Button onClick={onCreate} className="gap-2"
          style={{ background: "#F5B319", color: "hsl(230,22%,5%)" }}>
          <Plus className="h-4 w-4" /> Create workspace
        </Button>
      </div>
    </div>
  );
}

function DashboardShell({ children }: { children: React.ReactNode }) {
  const { workspaces, isLoading } = useWorkspace();
  const [modalOpen, setModalOpen] = useState(false);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="flex items-center gap-3 text-sm text-muted-foreground">
          <div className="h-4 w-4 rounded-full border-2 border-primary border-t-transparent animate-spin" />
          Loading…
        </div>
      </div>
    );
  }

  if (workspaces.length === 0) {
    return (
      <>
        <EmptyState onCreate={() => setModalOpen(true)} />
        <CreateWorkspaceModal open={modalOpen} onClose={() => setModalOpen(false)} />
      </>
    );
  }

  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex flex-1 flex-col min-w-0">
        <Topbar onNewWorkspace={() => setModalOpen(true)} />
        <main className="flex-1 overflow-y-auto p-6">{children}</main>
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
      <div className="flex min-h-screen items-center justify-center">
        <div className="flex items-center gap-3 text-sm text-muted-foreground">
          <div className="h-4 w-4 rounded-full border-2 border-primary border-t-transparent animate-spin" />
          Loading…
        </div>
      </div>
    );
  }

  return (
    <WorkspaceProvider>
      <DashboardShell>{children}</DashboardShell>
    </WorkspaceProvider>
  );
}
