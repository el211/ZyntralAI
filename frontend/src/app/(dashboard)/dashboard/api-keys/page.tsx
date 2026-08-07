"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api, unwrap, apiErrorMessage } from "@/lib/api";
import { useWorkspace } from "@/lib/workspace";
import { Check, Trash2, Sparkles, ExternalLink } from "lucide-react";

interface ProviderStatus { provider: string; configured: boolean; }

const PROVIDERS = [
  {
    id: "ANTHROPIC",
    label: "Anthropic",
    sublabel: "Claude (Haiku, Sonnet, Opus)",
    placeholder: "sk-ant-api03-…",
    docsUrl: "https://console.anthropic.com/settings/keys",
    docsLabel: "console.anthropic.com",
    color: "#F5B319",
    colorBg: "rgba(245,179,25,0.08)",
    colorBorder: "rgba(245,179,25,0.2)",
  },
  {
    id: "OPENAI",
    label: "OpenAI",
    sublabel: "GPT-4o, GPT-4, GPT-3.5",
    placeholder: "sk-proj-…",
    docsUrl: "https://platform.openai.com/api-keys",
    docsLabel: "platform.openai.com",
    color: "#34d399",
    colorBg: "rgba(52,211,153,0.08)",
    colorBorder: "rgba(52,211,153,0.2)",
  },
  {
    id: "GEMINI",
    label: "Google Gemini",
    sublabel: "Gemini 1.5 Pro, Flash",
    placeholder: "AIzaSy…",
    docsUrl: "https://aistudio.google.com/app/apikey",
    docsLabel: "aistudio.google.com",
    color: "#818cf8",
    colorBg: "rgba(129,140,248,0.08)",
    colorBorder: "rgba(129,140,248,0.2)",
  },
];

export default function ApiKeysPage() {
  const { current } = useWorkspace();
  const qc = useQueryClient();
  const [keyInputs, setKeyInputs] = useState<Record<string, string>>({});
  const [keyMsg, setKeyMsg] = useState<{ provider: string; text: string; ok: boolean } | null>(null);

  const { data: statuses = [] } = useQuery({
    queryKey: ["providerKeys", current?.id],
    enabled: !!current,
    queryFn: async () =>
      unwrap<ProviderStatus[]>((await api.get(`/workspaces/${current!.id}/ai/provider-keys`)).data),
  });

  const saveKey = useMutation({
    mutationFn: async ({ provider, apiKey }: { provider: string; apiKey: string }) =>
      api.put(`/workspaces/${current!.id}/ai/provider-keys/${provider}`, { apiKey }),
    onSuccess: (_, { provider }) => {
      setKeyMsg({ provider, text: "Key saved successfully.", ok: true });
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

  if (!current) return (
    <p style={{ color: "rgba(240,238,235,0.4)", fontSize: "14px" }}>Select a workspace first.</p>
  );

  const canManage = current.myRole === "OWNER" || current.myRole === "ADMIN";
  const statusMap = Object.fromEntries(statuses.map((s) => [s.provider, s.configured]));
  const activeCount = statuses.filter((s) => s.configured).length;

  return (
    <div style={{ maxWidth: "660px", display: "flex", flexDirection: "column", gap: "24px" }}>

      {/* Header */}
      <div>
        <h1 style={{ fontSize: "22px", fontWeight: 700, color: "#f0eeeb", letterSpacing: "-0.02em", margin: "0 0 6px" }}>
          Your AI API Keys
        </h1>
        <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.4)", margin: 0, lineHeight: 1.6 }}>
          Add your own keys and Zyntral AI uses them directly — you pay your provider, not us.
          You still use Zyntral for scheduling, publishing, and generation — just at a much lower credit cost.
        </p>
      </div>

      {/* Credit cost comparison card */}
      <div style={{
        borderRadius: "10px",
        border: "1px solid rgba(52,211,153,0.15)",
        background: "rgba(52,211,153,0.04)",
        padding: "20px 24px",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "16px" }}>
          <Sparkles size={14} style={{ color: "#34d399" }} />
          <span style={{ fontSize: "13px", fontWeight: 600, color: "#34d399" }}>
            Credit discount when you use your own key
          </span>
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
          <div>
            <p style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.3)", letterSpacing: "0.07em", textTransform: "uppercase", marginBottom: "10px" }}>
              Platform key (default)
            </p>
            {[
              { label: "Short content", value: "1 credit" },
              { label: "Medium content", value: "2 credits" },
              { label: "Long content", value: "3 credits" },
            ].map(({ label, value }) => (
              <div key={label} style={{ display: "flex", justifyContent: "space-between", padding: "5px 0", fontSize: "12px", borderBottom: "1px solid rgba(255,255,255,0.04)" }}>
                <span style={{ color: "rgba(240,238,235,0.4)" }}>{label}</span>
                <span style={{ color: "rgba(240,238,235,0.6)" }}>{value}</span>
              </div>
            ))}
          </div>
          <div>
            <p style={{ fontSize: "11px", fontWeight: 600, color: "#34d399", letterSpacing: "0.07em", textTransform: "uppercase", marginBottom: "10px" }}>
              Your own key
            </p>
            {[
              { label: "Short content", value: "0.5 credits" },
              { label: "Medium content", value: "1 credit" },
              { label: "Long content", value: "1.5 credits" },
            ].map(({ label, value }) => (
              <div key={label} style={{ display: "flex", justifyContent: "space-between", padding: "5px 0", fontSize: "12px", borderBottom: "1px solid rgba(255,255,255,0.04)" }}>
                <span style={{ color: "rgba(240,238,235,0.4)" }}>{label}</span>
                <span style={{ color: "#34d399", fontWeight: 600 }}>{value}</span>
              </div>
            ))}
          </div>
        </div>
        <p style={{ fontSize: "11px", color: "rgba(240,238,235,0.25)", marginTop: "12px" }}>
          Keys are encrypted at rest and never exposed after saving. Zyntral never logs or stores them in plaintext.
        </p>
      </div>

      {/* Status summary */}
      {activeCount > 0 && (
        <div style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "13px", color: "#34d399" }}>
          <Check size={14} />
          {activeCount} provider{activeCount > 1 ? "s" : ""} configured — discount active on all generations
        </div>
      )}

      {!canManage && (
        <div style={{
          padding: "12px 16px", borderRadius: "8px",
          background: "rgba(248,113,113,0.06)", border: "1px solid rgba(248,113,113,0.15)",
          fontSize: "13px", color: "rgba(248,113,113,0.8)",
        }}>
          Only workspace owners and admins can manage API keys.
        </div>
      )}

      {/* Provider cards */}
      <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
        {PROVIDERS.map(({ id, label, sublabel, placeholder, docsUrl, docsLabel, color, colorBg, colorBorder }) => {
          const isConfigured = statusMap[id] ?? false;
          const inputVal = keyInputs[id] ?? "";
          const myMsg = keyMsg?.provider === id ? keyMsg : null;
          const isBusy = saveKey.isPending || deleteKey.isPending;

          return (
            <div key={id} style={{
              borderRadius: "10px",
              border: `1px solid ${isConfigured ? colorBorder : "rgba(255,255,255,0.07)"}`,
              background: isConfigured ? colorBg : "rgba(255,255,255,0.02)",
              padding: "20px",
              transition: "border-color 0.15s, background 0.15s",
            }}>
              {/* Provider header */}
              <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: "14px" }}>
                <div>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <span style={{ fontSize: "14px", fontWeight: 600, color: "#f0eeeb" }}>{label}</span>
                    {isConfigured && (
                      <span style={{
                        display: "inline-flex", alignItems: "center", gap: "4px",
                        fontSize: "11px", fontWeight: 600, color,
                        background: colorBg, border: `1px solid ${colorBorder}`,
                        padding: "1px 8px", borderRadius: "20px",
                      }}>
                        <Check size={10} /> Active
                      </span>
                    )}
                  </div>
                  <p style={{ fontSize: "12px", color: "rgba(240,238,235,0.35)", margin: "2px 0 0" }}>{sublabel}</p>
                </div>

                <a
                  href={docsUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{
                    display: "inline-flex", alignItems: "center", gap: "4px",
                    fontSize: "11px", color: "rgba(240,238,235,0.3)",
                    textDecoration: "none",
                  }}
                >
                  {docsLabel} <ExternalLink size={10} />
                </a>
              </div>

              {canManage && (
                <div style={{ display: "flex", gap: "8px" }}>
                  <input
                    type="password"
                    autoComplete="new-password"
                    placeholder={isConfigured ? "••••••••••••  (key saved — enter a new one to replace)" : placeholder}
                    value={inputVal}
                    onChange={(e) => {
                      setKeyInputs((prev) => ({ ...prev, [id]: e.target.value }));
                      if (myMsg) setKeyMsg(null);
                    }}
                    style={{
                      flex: 1, height: "36px", padding: "0 12px",
                      borderRadius: "7px",
                      border: `1px solid ${inputVal ? color + "40" : "rgba(255,255,255,0.09)"}`,
                      background: "rgba(255,255,255,0.04)",
                      color: "#f0eeeb", fontSize: "13px",
                      fontFamily: "monospace", outline: "none",
                      transition: "border-color 0.15s",
                    }}
                  />
                  <button
                    disabled={!inputVal.trim() || isBusy}
                    onClick={() => saveKey.mutate({ provider: id, apiKey: inputVal.trim() })}
                    style={{
                      padding: "0 16px", height: "36px", borderRadius: "7px",
                      border: "none", cursor: (!inputVal.trim() || isBusy) ? "not-allowed" : "pointer",
                      background: (!inputVal.trim() || isBusy) ? "rgba(255,255,255,0.06)" : color === "#F5B319" ? color : "#5b5bd6",
                      color: (!inputVal.trim() || isBusy) ? "rgba(240,238,235,0.25)" : "#fff",
                      fontSize: "13px", fontWeight: 500, fontFamily: "inherit",
                      transition: "background 0.15s",
                      whiteSpace: "nowrap",
                    }}
                  >
                    {saveKey.isPending && keyMsg?.provider === id ? "Saving…" : "Save key"}
                  </button>
                  {isConfigured && (
                    <button
                      disabled={isBusy}
                      onClick={() => deleteKey.mutate(id)}
                      title="Remove key"
                      style={{
                        width: "36px", height: "36px", borderRadius: "7px",
                        border: "1px solid rgba(248,113,113,0.2)",
                        background: "rgba(248,113,113,0.06)",
                        color: "rgba(248,113,113,0.7)",
                        cursor: isBusy ? "not-allowed" : "pointer",
                        display: "flex", alignItems: "center", justifyContent: "center",
                        flexShrink: 0,
                      }}
                    >
                      <Trash2 size={14} />
                    </button>
                  )}
                </div>
              )}

              {myMsg && (
                <p style={{
                  fontSize: "12px", marginTop: "8px",
                  color: myMsg.ok ? "#34d399" : "#f87171",
                }}>
                  {myMsg.text}
                </p>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
