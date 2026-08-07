"use client";

import Link from "next/link";
import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api, unwrap, apiErrorMessage } from "@/lib/api";
import { useWorkspace } from "@/lib/workspace";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Key, ArrowUpRight } from "lucide-react";

interface Member { userId: string; role: string; joinedAt: string; }

export default function SettingsPage() {
  const { current } = useWorkspace();
  const qc = useQueryClient();
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("EDITOR");
  const [msg, setMsg] = useState<string | null>(null);

  const { data: members = [] } = useQuery({
    queryKey: ["members", current?.id],
    enabled: !!current,
    queryFn: async () =>
      unwrap<Member[]>((await api.get(`/workspaces/${current!.id}/members`)).data),
  });

  const invite = useMutation({
    mutationFn: async () =>
      api.post(`/workspaces/${current!.id}/invitations`, { email, role }),
    onSuccess: () => {
      setMsg(`Invitation sent to ${email}`);
      setEmail("");
      qc.invalidateQueries({ queryKey: ["members", current?.id] });
    },
    onError: (err) => setMsg(apiErrorMessage(err)),
  });

  if (!current) return <p style={{ color: "rgba(240,238,235,0.4)", fontSize: "14px" }}>Select a workspace first.</p>;
  const canManage = current.myRole === "OWNER" || current.myRole === "ADMIN";

  return (
    <div style={{ maxWidth: "640px", display: "flex", flexDirection: "column", gap: "20px" }}>
      <h1 style={{ fontSize: "22px", fontWeight: 700, color: "#f0eeeb", letterSpacing: "-0.02em", margin: 0 }}>
        Settings
      </h1>

      {/* Workspace info */}
      <div style={{
        borderRadius: "10px", border: "1px solid rgba(255,255,255,0.07)",
        background: "rgba(255,255,255,0.02)", padding: "20px 24px",
      }}>
        <p style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.3)", letterSpacing: "0.07em", textTransform: "uppercase", marginBottom: "14px" }}>
          Workspace
        </p>
        {[
          { label: "Name", value: current.name },
          { label: "Plan", value: current.plan },
          { label: "Your role", value: current.myRole },
        ].map(({ label, value }) => (
          <div key={label} style={{ display: "flex", justifyContent: "space-between", padding: "6px 0", fontSize: "13px", borderBottom: "1px solid rgba(255,255,255,0.04)" }}>
            <span style={{ color: "rgba(240,238,235,0.4)" }}>{label}</span>
            <span style={{ color: "#f0eeeb" }}>{value}</span>
          </div>
        ))}
      </div>

      {/* API Keys shortcut */}
      <Link href="/dashboard/api-keys" style={{ textDecoration: "none" }}>
        <div style={{
          borderRadius: "10px",
          border: "1px solid rgba(91,91,214,0.2)",
          background: "rgba(91,91,214,0.04)",
          padding: "16px 20px",
          display: "flex", alignItems: "center", justifyContent: "space-between",
          cursor: "pointer",
          transition: "border-color 0.15s, background 0.15s",
        }}
        onMouseEnter={(e) => {
          (e.currentTarget as HTMLDivElement).style.borderColor = "rgba(91,91,214,0.4)";
          (e.currentTarget as HTMLDivElement).style.background = "rgba(91,91,214,0.08)";
        }}
        onMouseLeave={(e) => {
          (e.currentTarget as HTMLDivElement).style.borderColor = "rgba(91,91,214,0.2)";
          (e.currentTarget as HTMLDivElement).style.background = "rgba(91,91,214,0.04)";
        }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <div style={{
              width: "32px", height: "32px", borderRadius: "8px",
              background: "rgba(91,91,214,0.15)", display: "flex", alignItems: "center", justifyContent: "center",
            }}>
              <Key size={15} style={{ color: "#818cf8" }} />
            </div>
            <div>
              <p style={{ fontSize: "13px", fontWeight: 500, color: "#f0eeeb", margin: 0 }}>Your AI API Keys</p>
              <p style={{ fontSize: "12px", color: "rgba(240,238,235,0.35)", margin: 0 }}>Use your own Anthropic / OpenAI / Gemini key — up to 50% fewer credits</p>
            </div>
          </div>
          <ArrowUpRight size={15} style={{ color: "rgba(240,238,235,0.3)", flexShrink: 0 }} />
        </div>
      </Link>

      {/* Team members */}
      <div style={{
        borderRadius: "10px", border: "1px solid rgba(255,255,255,0.07)",
        background: "rgba(255,255,255,0.02)", padding: "20px 24px",
      }}>
        <p style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.3)", letterSpacing: "0.07em", textTransform: "uppercase", marginBottom: "14px" }}>
          Team members ({members.length})
        </p>
        <div style={{ display: "flex", flexDirection: "column", gap: "6px", marginBottom: canManage ? "16px" : "0" }}>
          {members.map((m) => (
            <div key={m.userId} style={{
              display: "flex", alignItems: "center", justifyContent: "space-between",
              padding: "10px 12px", borderRadius: "7px",
              background: "rgba(255,255,255,0.03)", fontSize: "13px",
            }}>
              <span style={{ fontFamily: "monospace", fontSize: "12px", color: "rgba(240,238,235,0.5)" }}>
                {m.userId.slice(0, 8)}…
              </span>
              <span style={{ color: "#f0eeeb" }}>{m.role}</span>
            </div>
          ))}
        </div>

        {canManage && (
          <div style={{ borderTop: "1px solid rgba(255,255,255,0.06)", paddingTop: "14px", display: "flex", flexDirection: "column", gap: "8px" }}>
            <Label>Invite by email</Label>
            <div style={{ display: "flex", gap: "8px" }}>
              <Input type="email" placeholder="teammate@company.com"
                value={email} onChange={(e) => setEmail(e.target.value)} style={{ flex: 1 }} />
              <Select className="w-32" value={role} onChange={(e) => setRole(e.target.value)}>
                <option value="ADMIN">Admin</option>
                <option value="EDITOR">Editor</option>
                <option value="VIEWER">Viewer</option>
              </Select>
              <Button disabled={!email || invite.isPending} onClick={() => invite.mutate()}>Invite</Button>
            </div>
            {msg && <p style={{ fontSize: "12px", color: "rgba(240,238,235,0.45)" }}>{msg}</p>}
          </div>
        )}
      </div>
    </div>
  );
}
