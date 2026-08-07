"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api, unwrap, apiErrorMessage } from "@/lib/api";
import { useWorkspace } from "@/lib/workspace";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Check, Trash2, Key } from "lucide-react";

interface Member { userId: string; role: string; joinedAt: string; }
interface ProviderStatus { provider: string; configured: boolean; }

const PROVIDERS: { id: string; label: string; placeholder: string; hint: string }[] = [
  {
    id: "ANTHROPIC",
    label: "Anthropic (Claude)",
    placeholder: "sk-ant-…",
    hint: "console.anthropic.com → API keys",
  },
  {
    id: "OPENAI",
    label: "OpenAI (ChatGPT)",
    placeholder: "sk-…",
    hint: "platform.openai.com → API keys",
  },
  {
    id: "GEMINI",
    label: "Google (Gemini)",
    placeholder: "AIza…",
    hint: "aistudio.google.com → Get API key",
  },
];

export default function SettingsPage() {
  const { current } = useWorkspace();
  const qc = useQueryClient();
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("EDITOR");
  const [msg, setMsg] = useState<string | null>(null);

  // Per-provider key input state
  const [keyInputs, setKeyInputs] = useState<Record<string, string>>({});
  const [keyMsg, setKeyMsg] = useState<{ provider: string; text: string; ok: boolean } | null>(null);

  const { data: members = [] } = useQuery({
    queryKey: ["members", current?.id],
    enabled: !!current,
    queryFn: async () =>
      unwrap<Member[]>((await api.get(`/workspaces/${current!.id}/members`)).data),
  });

  const { data: providerStatuses = [] } = useQuery({
    queryKey: ["providerKeys", current?.id],
    enabled: !!current,
    queryFn: async () =>
      unwrap<ProviderStatus[]>((await api.get(`/workspaces/${current!.id}/ai/provider-keys`)).data),
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

  const saveKey = useMutation({
    mutationFn: async ({ provider, apiKey }: { provider: string; apiKey: string }) =>
      api.put(`/workspaces/${current!.id}/ai/provider-keys/${provider}`, { apiKey }),
    onSuccess: (_, { provider }) => {
      setKeyMsg({ provider, text: "Key saved.", ok: true });
      setKeyInputs((prev) => ({ ...prev, [provider]: "" }));
      qc.invalidateQueries({ queryKey: ["providerKeys", current?.id] });
    },
    onError: (err, { provider }) =>
      setKeyMsg({ provider, text: apiErrorMessage(err), ok: false }),
  });

  const deleteKey = useMutation({
    mutationFn: async (provider: string) =>
      api.delete(`/workspaces/${current!.id}/ai/provider-keys/${provider}`),
    onSuccess: (_, provider) => {
      setKeyMsg({ provider, text: "Key removed.", ok: true });
      qc.invalidateQueries({ queryKey: ["providerKeys", current?.id] });
    },
    onError: (err, provider) =>
      setKeyMsg({ provider, text: apiErrorMessage(err), ok: false }),
  });

  if (!current) return <p className="text-muted-foreground">Select a workspace first.</p>;
  const canManage = current.myRole === "OWNER" || current.myRole === "ADMIN";

  const statusMap = Object.fromEntries(providerStatuses.map((s) => [s.provider, s.configured]));

  return (
    <div style={{ maxWidth: "680px", display: "flex", flexDirection: "column", gap: "20px" }}>
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

      {/* AI API Keys — BYOK */}
      <div style={{
        borderRadius: "10px", border: "1px solid rgba(91,91,214,0.2)",
        background: "rgba(91,91,214,0.03)", padding: "20px 24px",
      }}>
        <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: "6px", gap: "12px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <Key size={15} style={{ color: "#818cf8" }} />
            <p style={{ fontSize: "14px", fontWeight: 600, color: "#f0eeeb", margin: 0 }}>
              Your AI API Keys
            </p>
          </div>
          <span style={{
            fontSize: "11px", fontWeight: 600, padding: "2px 10px",
            borderRadius: "20px",
            background: "rgba(52,211,153,0.1)", color: "#34d399",
            border: "1px solid rgba(52,211,153,0.2)",
            whiteSpace: "nowrap",
          }}>
            Up to 50% fewer credits
          </span>
        </div>
        <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.4)", marginBottom: "20px", lineHeight: 1.6 }}>
          Add your own API keys and Zyntral AI uses them instead of the platform keys. You still pay
          a small infrastructure credit (1 credit flat) — that&apos;s it.
        </p>

        {/* Credit cost comparison */}
        <div style={{
          display: "grid", gridTemplateColumns: "1fr 1fr", gap: "8px",
          marginBottom: "20px",
          padding: "12px 16px",
          borderRadius: "8px",
          background: "rgba(255,255,255,0.03)",
          border: "1px solid rgba(255,255,255,0.05)",
        }}>
          <div>
            <p style={{ fontSize: "11px", color: "rgba(240,238,235,0.3)", fontWeight: 600, letterSpacing: "0.06em", textTransform: "uppercase", marginBottom: "8px" }}>
              Platform key (default)
            </p>
            {[["Short", "1 credit"], ["Medium", "2 credits"], ["Long", "3 credits"]].map(([l, v]) => (
              <div key={l} style={{ display: "flex", justifyContent: "space-between", fontSize: "12px", marginBottom: "4px" }}>
                <span style={{ color: "rgba(240,238,235,0.4)" }}>{l}</span>
                <span style={{ color: "rgba(240,238,235,0.6)" }}>{v}</span>
              </div>
            ))}
          </div>
          <div>
            <p style={{ fontSize: "11px", color: "#34d399", fontWeight: 600, letterSpacing: "0.06em", textTransform: "uppercase", marginBottom: "8px" }}>
              Your own key
            </p>
            {[["Short", "0 credits"], ["Medium", "1 credit"], ["Long", "1 credit"]].map(([l, v]) => (
              <div key={l} style={{ display: "flex", justifyContent: "space-between", fontSize: "12px", marginBottom: "4px" }}>
                <span style={{ color: "rgba(240,238,235,0.4)" }}>{l}</span>
                <span style={{ color: "#34d399", fontWeight: 600 }}>{v}</span>
              </div>
            ))}
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          {PROVIDERS.map(({ id, label, placeholder, hint }) => {
            const isConfigured = statusMap[id] ?? false;
            const inputVal = keyInputs[id] ?? "";
            const myMsg = keyMsg?.provider === id ? keyMsg : null;

            return (
              <div key={id} style={{
                padding: "14px 16px",
                borderRadius: "8px",
                border: isConfigured
                  ? "1px solid rgba(52,211,153,0.2)"
                  : "1px solid rgba(255,255,255,0.06)",
                background: isConfigured
                  ? "rgba(52,211,153,0.04)"
                  : "rgba(255,255,255,0.02)",
              }}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "10px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <span style={{ fontSize: "13px", fontWeight: 500, color: "#f0eeeb" }}>{label}</span>
                    {isConfigured && (
                      <span style={{ display: "flex", alignItems: "center", gap: "4px", fontSize: "11px", color: "#34d399", fontWeight: 600 }}>
                        <Check size={10} /> Active
                      </span>
                    )}
                  </div>
                  {isConfigured && canManage && (
                    <button
                      onClick={() => deleteKey.mutate(id)}
                      disabled={deleteKey.isPending}
                      style={{
                        display: "flex", alignItems: "center", gap: "4px",
                        background: "none", border: "none", cursor: "pointer",
                        fontSize: "12px", color: "rgba(248,113,113,0.7)", padding: "2px 6px",
                      }}
                    >
                      <Trash2 size={12} /> Remove
                    </button>
                  )}
                </div>

                {canManage && (
                  <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                    <input
                      type="password"
                      placeholder={isConfigured ? "••••••••••••  (saved — enter new key to replace)" : placeholder}
                      value={inputVal}
                      onChange={(e) => setKeyInputs((prev) => ({ ...prev, [id]: e.target.value }))}
                      style={{
                        flex: 1, height: "34px", padding: "0 12px",
                        borderRadius: "6px", border: "1px solid rgba(255,255,255,0.1)",
                        background: "rgba(255,255,255,0.05)", color: "#f0eeeb",
                        fontSize: "13px", fontFamily: "inherit", outline: "none",
                      }}
                    />
                    <button
                      disabled={!inputVal.trim() || saveKey.isPending}
                      onClick={() => saveKey.mutate({ provider: id, apiKey: inputVal.trim() })}
                      style={{
                        padding: "0 14px", height: "34px", borderRadius: "6px",
                        border: "none", background: "#5b5bd6", color: "#fff",
                        fontSize: "13px", fontWeight: 500, cursor: "pointer",
                        fontFamily: "inherit",
                        opacity: (!inputVal.trim() || saveKey.isPending) ? 0.5 : 1,
                      }}
                    >
                      Save
                    </button>
                  </div>
                )}

                <p style={{ fontSize: "11px", color: "rgba(240,238,235,0.3)", marginTop: "6px" }}>
                  {hint}
                </p>

                {myMsg && (
                  <p style={{ fontSize: "12px", marginTop: "6px", color: myMsg.ok ? "#34d399" : "#f87171" }}>
                    {myMsg.text}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      </div>

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
              background: "rgba(255,255,255,0.03)",
              fontSize: "13px",
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
