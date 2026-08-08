"use client";

import { useState, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api, unwrap, apiErrorMessage } from "@/lib/api";
import { useWorkspace } from "@/lib/workspace";
import {
  Plus, Search, Globe, Mail, Phone, Linkedin, Trash2,
  ChevronRight, X, Copy, Check, Sparkles, ExternalLink,
  Building2, MapPin, Tag, StickyNote, MessageSquare,
  BarChart3, Users, TrendingUp, Clock,
} from "lucide-react";

interface Prospect {
  id: string;
  firstName: string;
  lastName?: string;
  company?: string;
  website?: string;
  email?: string;
  phone?: string;
  linkedinUrl?: string;
  country?: string;
  industry?: string;
  status: string;
  notes?: string;
  lastContactedAt?: string;
  createdAt: string;
  updatedAt: string;
}

const STATUS_OPTIONS = ["NEW", "CONTACTED", "RESPONDED", "MEETING_BOOKED", "CONVERTED", "LOST"];

const STATUS_STYLE: Record<string, { color: string; bg: string; border: string; label: string }> = {
  NEW:           { color: "#818cf8", bg: "rgba(129,140,248,0.1)", border: "rgba(129,140,248,0.2)", label: "New" },
  CONTACTED:     { color: "#f5b319", bg: "rgba(245,179,25,0.1)",  border: "rgba(245,179,25,0.2)",  label: "Contacted" },
  RESPONDED:     { color: "#34d399", bg: "rgba(52,211,153,0.1)",  border: "rgba(52,211,153,0.2)",  label: "Responded" },
  MEETING_BOOKED:{ color: "#a78bfa", bg: "rgba(167,139,250,0.1)", border: "rgba(167,139,250,0.2)", label: "Meeting Booked" },
  CONVERTED:     { color: "#34d399", bg: "rgba(52,211,153,0.12)", border: "rgba(52,211,153,0.25)", label: "Converted" },
  LOST:          { color: "#f87171", bg: "rgba(248,113,113,0.08)", border: "rgba(248,113,113,0.2)", label: "Lost" },
};

const INDUSTRIES = [
  "Technology", "SaaS / Software", "E-commerce", "Marketing / Agency",
  "Finance / Fintech", "Healthcare", "Education", "Real Estate",
  "Consulting", "Media / Entertainment", "Manufacturing", "Retail",
  "Logistics", "Legal", "Non-profit", "Other",
];

const COUNTRIES = [
  "International", "France", "United States", "United Kingdom", "Germany",
  "Spain", "Italy", "Netherlands", "Belgium", "Switzerland", "Canada",
  "Australia", "Brazil", "Mexico", "Argentina", "Colombia", "Portugal",
  "Sweden", "Norway", "Denmark", "Finland", "Poland", "Czech Republic",
  "Austria", "Japan", "South Korea", "Singapore", "India", "UAE",
  "South Africa", "Nigeria", "Morocco", "Egypt", "Israel", "Turkey",
  "Saudi Arabia", "China", "Indonesia", "Thailand", "Malaysia", "Vietnam",
  "New Zealand", "Ireland", "Luxembourg", "Romania", "Hungary", "Ukraine",
  "Russia", "Pakistan", "Bangladesh", "Philippines",
];

const EMPTY_FORM = {
  firstName: "", lastName: "", company: "", website: "",
  email: "", phone: "", linkedinUrl: "", country: "",
  industry: "", notes: "", status: "NEW",
};

function StatusBadge({ status }: { status: string }) {
  const s = STATUS_STYLE[status] ?? STATUS_STYLE.NEW;
  return (
    <span style={{
      fontSize: "11px", fontWeight: 600,
      color: s.color, background: s.bg, border: `1px solid ${s.border}`,
      padding: "2px 8px", borderRadius: "20px",
      display: "inline-block", whiteSpace: "nowrap",
    }}>
      {s.label ?? status}
    </span>
  );
}

function FieldInput({ label, value, onChange, type = "text", placeholder = "" }: {
  label: string; value: string; onChange: (v: string) => void;
  type?: string; placeholder?: string;
}) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "5px" }}>
      <label style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.35)", letterSpacing: "0.05em", textTransform: "uppercase" }}>
        {label}
      </label>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        style={{
          height: "34px", padding: "0 10px", borderRadius: "6px",
          border: "1px solid rgba(255,255,255,0.09)",
          background: "rgba(255,255,255,0.04)",
          color: "#f0eeeb", fontSize: "13px", fontFamily: "inherit",
          outline: "none",
        }}
      />
    </div>
  );
}

function FieldSelect({ label, value, onChange, options }: {
  label: string; value: string; onChange: (v: string) => void;
  options: { value: string; label: string }[];
}) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "5px" }}>
      <label style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.35)", letterSpacing: "0.05em", textTransform: "uppercase" }}>
        {label}
      </label>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        style={{
          height: "34px", padding: "0 10px", borderRadius: "6px",
          border: "1px solid rgba(255,255,255,0.09)",
          background: "rgba(255,255,255,0.04)",
          color: value ? "#f0eeeb" : "rgba(240,238,235,0.35)",
          fontSize: "13px", fontFamily: "inherit", outline: "none",
        }}
      >
        <option value="">— Select —</option>
        {options.map((o) => <option key={o.value} value={o.value} style={{ background: "#111113" }}>{o.label}</option>)}
      </select>
    </div>
  );
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <button
      onClick={() => { navigator.clipboard.writeText(text); setCopied(true); setTimeout(() => setCopied(false), 2000); }}
      style={{
        display: "inline-flex", alignItems: "center", gap: "5px",
        padding: "5px 10px", borderRadius: "5px",
        border: "1px solid rgba(255,255,255,0.09)",
        background: "rgba(255,255,255,0.04)",
        color: copied ? "#34d399" : "rgba(240,238,235,0.5)",
        fontSize: "12px", cursor: "pointer", fontFamily: "inherit",
      }}
    >
      {copied ? <Check size={12} /> : <Copy size={12} />}
      {copied ? "Copied!" : "Copy"}
    </button>
  );
}

export default function CrmPage() {
  const { current } = useWorkspace();
  const qc = useQueryClient();

  const [search, setSearch] = useState("");
  const [filterStatus, setFilterStatus] = useState("");
  const [filterCountry, setFilterCountry] = useState("");
  const [filterIndustry, setFilterIndustry] = useState("");

  const [showModal, setShowModal] = useState(false);
  const [editProspect, setEditProspect] = useState<Prospect | null>(null);
  const [form, setForm] = useState({ ...EMPTY_FORM });

  const [selectedProspect, setSelectedProspect] = useState<Prospect | null>(null);
  const [activePanel, setActivePanel] = useState<"info" | "analyze" | "message">("info");

  const [analyzeUrl, setAnalyzeUrl] = useState("");
  const [analyzeResult, setAnalyzeResult] = useState("");
  const [analyzeErr, setAnalyzeErr] = useState("");
  const [analyzing, setAnalyzing] = useState(false);

  const [msgChannel, setMsgChannel] = useState("EMAIL");
  const [msgNote, setMsgNote] = useState("");
  const [msgResult, setMsgResult] = useState("");
  const [msgErr, setMsgErr] = useState("");
  const [drafting, setDrafting] = useState(false);

  const { data: prospects = [] } = useQuery<Prospect[]>({
    queryKey: ["crm-prospects", current?.id],
    enabled: !!current,
    queryFn: async () =>
      unwrap<Prospect[]>((await api.get(`/workspaces/${current!.id}/crm/prospects`)).data),
  });

  const createMut = useMutation({
    mutationFn: async (data: typeof EMPTY_FORM) =>
      api.post(`/workspaces/${current!.id}/crm/prospects`, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["crm-prospects", current?.id] });
      closeModal();
    },
  });

  const updateMut = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: typeof EMPTY_FORM }) =>
      api.put(`/workspaces/${current!.id}/crm/prospects/${id}`, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: ["crm-prospects", current?.id] });
      setSelectedProspect((prev) => prev?.id === id ? { ...prev!, ...form } : prev);
      closeModal();
    },
  });

  const deleteMut = useMutation({
    mutationFn: async (id: string) =>
      api.delete(`/workspaces/${current!.id}/crm/prospects/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["crm-prospects", current?.id] });
      setSelectedProspect(null);
    },
  });

  function openAdd() {
    setEditProspect(null);
    setForm({ ...EMPTY_FORM });
    setShowModal(true);
  }

  function openEdit(p: Prospect) {
    setEditProspect(p);
    setForm({
      firstName: p.firstName, lastName: p.lastName ?? "",
      company: p.company ?? "", website: p.website ?? "",
      email: p.email ?? "", phone: p.phone ?? "",
      linkedinUrl: p.linkedinUrl ?? "", country: p.country ?? "",
      industry: p.industry ?? "", notes: p.notes ?? "", status: p.status,
    });
    setShowModal(true);
  }

  function closeModal() {
    setShowModal(false);
    setEditProspect(null);
    setForm({ ...EMPTY_FORM });
  }

  function submitForm() {
    if (editProspect) {
      updateMut.mutate({ id: editProspect.id, data: form });
    } else {
      createMut.mutate(form);
    }
  }

  function selectProspect(p: Prospect) {
    setSelectedProspect(p);
    setActivePanel("info");
    setAnalyzeUrl(p.website ?? "");
    setAnalyzeResult("");
    setAnalyzeErr("");
    setMsgResult("");
    setMsgErr("");
    setMsgNote("");
  }

  async function handleAnalyze() {
    if (!current || !analyzeUrl) return;
    setAnalyzing(true);
    setAnalyzeResult("");
    setAnalyzeErr("");
    try {
      const res = await api.post(`/workspaces/${current.id}/crm/analyze-website`, { url: analyzeUrl });
      setAnalyzeResult(unwrap<{ text: string }>(res.data).text);
    } catch (e) {
      setAnalyzeErr(apiErrorMessage(e));
    } finally {
      setAnalyzing(false);
    }
  }

  async function handleDraft() {
    if (!current || !selectedProspect) return;
    setDrafting(true);
    setMsgResult("");
    setMsgErr("");
    try {
      const res = await api.post(`/workspaces/${current.id}/crm/draft-message`, {
        prospectId: selectedProspect.id,
        channel: msgChannel,
        customNote: msgNote,
      });
      setMsgResult(unwrap<{ text: string }>(res.data).text);
      qc.invalidateQueries({ queryKey: ["crm-prospects", current?.id] });
    } catch (e) {
      setMsgErr(apiErrorMessage(e));
    } finally {
      setDrafting(false);
    }
  }

  const filtered = useMemo(() => prospects.filter((p) => {
    const q = search.toLowerCase();
    if (q && !`${p.firstName} ${p.lastName ?? ""} ${p.company ?? ""} ${p.email ?? ""}`.toLowerCase().includes(q)) return false;
    if (filterStatus && p.status !== filterStatus) return false;
    if (filterCountry && p.country !== filterCountry) return false;
    if (filterIndustry && p.industry !== filterIndustry) return false;
    return true;
  }), [prospects, search, filterStatus, filterCountry, filterIndustry]);

  const stats = useMemo(() => ({
    total: prospects.length,
    contacted: prospects.filter((p) => p.status !== "NEW").length,
    converted: prospects.filter((p) => p.status === "CONVERTED").length,
    meetings: prospects.filter((p) => p.status === "MEETING_BOOKED").length,
  }), [prospects]);

  if (!current) return <p style={{ color: "rgba(240,238,235,0.4)", fontSize: "14px" }}>Select a workspace first.</p>;

  const isBusy = createMut.isPending || updateMut.isPending;
  const formError = createMut.isError ? apiErrorMessage(createMut.error) :
                    updateMut.isError ? apiErrorMessage(updateMut.error) : null;

  const panelBorder = "1px solid rgba(255,255,255,0.07)";
  const cardBg = "rgba(255,255,255,0.02)";

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px", height: "100%" }}>

      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <div>
          <h1 style={{ fontSize: "22px", fontWeight: 700, color: "#f0eeeb", letterSpacing: "-0.02em", margin: "0 0 4px" }}>
            Zyntral CRM
          </h1>
          <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.35)", margin: 0 }}>
            Manage prospects, analyze their websites, and draft personalized outreach
          </p>
        </div>
        <button
          onClick={openAdd}
          style={{
            display: "flex", alignItems: "center", gap: "6px",
            padding: "8px 14px", borderRadius: "7px",
            background: "#5b5bd6", border: "none",
            color: "#fff", fontSize: "13px", fontWeight: 500,
            cursor: "pointer", fontFamily: "inherit",
          }}
        >
          <Plus size={14} /> Add Prospect
        </button>
      </div>

      {/* Stats row */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "12px" }}>
        {[
          { label: "Total Prospects", value: stats.total, icon: Users, color: "#818cf8" },
          { label: "Engaged", value: stats.contacted, icon: TrendingUp, color: "#f5b319" },
          { label: "Meetings Booked", value: stats.meetings, icon: Clock, color: "#a78bfa" },
          { label: "Converted", value: stats.converted, icon: BarChart3, color: "#34d399" },
        ].map(({ label, value, icon: Icon, color }) => (
          <div key={label} style={{
            borderRadius: "10px", border: panelBorder, background: cardBg,
            padding: "16px 18px", display: "flex", alignItems: "center", gap: "12px",
          }}>
            <div style={{
              width: "34px", height: "34px", borderRadius: "8px",
              background: `${color}18`, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0,
            }}>
              <Icon size={15} style={{ color }} />
            </div>
            <div>
              <div style={{ fontSize: "20px", fontWeight: 700, color: "#f0eeeb", lineHeight: 1 }}>{value}</div>
              <div style={{ fontSize: "11px", color: "rgba(240,238,235,0.35)", marginTop: "2px" }}>{label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Main area: list + detail panel */}
      <div style={{ display: "flex", gap: "16px", flex: 1, minHeight: 0 }}>

        {/* Left: filters + prospect list */}
        <div style={{ flex: "0 0 380px", display: "flex", flexDirection: "column", gap: "10px" }}>

          {/* Search & Filters */}
          <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
            <div style={{ position: "relative" }}>
              <Search size={13} style={{ position: "absolute", left: "10px", top: "50%", transform: "translateY(-50%)", color: "rgba(240,238,235,0.25)", pointerEvents: "none" }} />
              <input
                placeholder="Search prospects..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                style={{
                  width: "100%", height: "34px", padding: "0 10px 0 32px",
                  borderRadius: "7px", border: "1px solid rgba(255,255,255,0.09)",
                  background: "rgba(255,255,255,0.04)", color: "#f0eeeb",
                  fontSize: "13px", fontFamily: "inherit", outline: "none",
                  boxSizing: "border-box",
                }}
              />
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "6px" }}>
              {([
                { label: "Status",   value: filterStatus,   set: setFilterStatus,   items: STATUS_OPTIONS.map((s) => ({ value: s, label: STATUS_STYLE[s]?.label ?? s })) },
                { label: "Country",  value: filterCountry,  set: setFilterCountry,  items: COUNTRIES.slice(1).map((c) => ({ value: c, label: c })) },
                { label: "Industry", value: filterIndustry, set: setFilterIndustry, items: INDUSTRIES.map((i) => ({ value: i, label: i })) },
              ] as const).map(({ label, value, set, items }) => (
                <select
                  key={label}
                  value={value}
                  onChange={(e) => set(e.target.value)}
                  style={{
                    height: "30px", padding: "0 6px", borderRadius: "5px",
                    border: "1px solid rgba(255,255,255,0.09)",
                    background: "rgba(255,255,255,0.04)",
                    color: value ? "#f0eeeb" : "rgba(240,238,235,0.35)",
                    fontSize: "11px", fontFamily: "inherit", outline: "none",
                  }}
                >
                  <option value="" style={{ background: "#111113" }}>All {label}s</option>
                  {items.map((o) => (
                    <option key={o.value} value={o.value} style={{ background: "#111113" }}>{o.label}</option>
                  ))}
                </select>
              ))}
            </div>
          </div>

          {/* Prospect list */}
          <div style={{ display: "flex", flexDirection: "column", gap: "6px", overflowY: "auto", flex: 1 }}>
            {filtered.length === 0 && (
              <div style={{
                textAlign: "center", padding: "40px 20px",
                border: "1px dashed rgba(255,255,255,0.08)", borderRadius: "10px",
              }}>
                <Users size={28} style={{ color: "rgba(240,238,235,0.15)", display: "block", margin: "0 auto 10px" }} />
                <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.3)", margin: 0 }}>
                  {prospects.length === 0 ? "No prospects yet — add your first one" : "No prospects match your filters"}
                </p>
              </div>
            )}
            {filtered.map((p) => {
              const isSelected = selectedProspect?.id === p.id;
              return (
                <div
                  key={p.id}
                  onClick={() => selectProspect(p)}
                  style={{
                    padding: "12px 14px", borderRadius: "8px",
                    border: isSelected ? "1px solid rgba(91,91,214,0.35)" : "1px solid rgba(255,255,255,0.06)",
                    background: isSelected ? "rgba(91,91,214,0.07)" : "rgba(255,255,255,0.02)",
                    cursor: "pointer", transition: "all 0.12s",
                    display: "flex", alignItems: "center", justifyContent: "space-between",
                  }}
                >
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "4px" }}>
                      <span style={{ fontSize: "13px", fontWeight: 500, color: "#f0eeeb", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                        {p.firstName}{p.lastName ? ` ${p.lastName}` : ""}
                      </span>
                      <StatusBadge status={p.status} />
                    </div>
                    <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                      {p.company && (
                        <span style={{ fontSize: "12px", color: "rgba(240,238,235,0.4)", display: "flex", alignItems: "center", gap: "4px" }}>
                          <Building2 size={11} /> {p.company}
                        </span>
                      )}
                      {p.country && (
                        <span style={{ fontSize: "12px", color: "rgba(240,238,235,0.3)", display: "flex", alignItems: "center", gap: "4px" }}>
                          <MapPin size={11} /> {p.country}
                        </span>
                      )}
                    </div>
                  </div>
                  <ChevronRight size={14} style={{ color: "rgba(240,238,235,0.2)", flexShrink: 0, marginLeft: "8px" }} />
                </div>
              );
            })}
          </div>
        </div>

        {/* Right: Detail panel */}
        <div style={{
          flex: 1, borderRadius: "12px",
          border: panelBorder, background: cardBg,
          display: "flex", flexDirection: "column", overflow: "hidden",
        }}>
          {!selectedProspect ? (
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", height: "100%", gap: "12px" }}>
              <div style={{ width: "48px", height: "48px", borderRadius: "12px", background: "rgba(91,91,214,0.08)", border: "1px solid rgba(91,91,214,0.15)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Users size={20} style={{ color: "#818cf8" }} />
              </div>
              <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.3)", margin: 0 }}>Select a prospect to view details</p>
            </div>
          ) : (
            <>
              {/* Panel header */}
              <div style={{
                padding: "16px 20px", borderBottom: panelBorder,
                display: "flex", alignItems: "flex-start", justifyContent: "space-between",
              }}>
                <div>
                  <h2 style={{ fontSize: "16px", fontWeight: 600, color: "#f0eeeb", margin: "0 0 4px" }}>
                    {selectedProspect.firstName}{selectedProspect.lastName ? ` ${selectedProspect.lastName}` : ""}
                  </h2>
                  <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                    {selectedProspect.company && (
                      <span style={{ fontSize: "12px", color: "rgba(240,238,235,0.4)", display: "flex", alignItems: "center", gap: "4px" }}>
                        <Building2 size={11} /> {selectedProspect.company}
                      </span>
                    )}
                    <StatusBadge status={selectedProspect.status} />
                  </div>
                </div>
                <div style={{ display: "flex", gap: "6px" }}>
                  <button
                    onClick={() => openEdit(selectedProspect)}
                    style={{
                      padding: "5px 10px", borderRadius: "5px",
                      border: "1px solid rgba(255,255,255,0.09)",
                      background: "rgba(255,255,255,0.04)",
                      color: "rgba(240,238,235,0.6)", fontSize: "12px",
                      cursor: "pointer", fontFamily: "inherit",
                    }}
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => { if (confirm("Delete this prospect?")) deleteMut.mutate(selectedProspect.id); }}
                    style={{
                      width: "28px", height: "28px", borderRadius: "5px",
                      border: "1px solid rgba(248,113,113,0.2)",
                      background: "rgba(248,113,113,0.06)",
                      color: "rgba(248,113,113,0.7)",
                      cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center",
                    }}
                  >
                    <Trash2 size={13} />
                  </button>
                </div>
              </div>

              {/* Sub-tabs */}
              <div style={{ display: "flex", gap: "0", borderBottom: panelBorder }}>
                {(["info", "analyze", "message"] as const).map((tab) => {
                  const labels = { info: "Info", analyze: "Analyze Website", message: "Draft Message" };
                  const icons = { info: Tag, analyze: Globe, message: MessageSquare };
                  const Icon = icons[tab];
                  const active = activePanel === tab;
                  return (
                    <button
                      key={tab}
                      onClick={() => setActivePanel(tab)}
                      style={{
                        display: "flex", alignItems: "center", gap: "6px",
                        padding: "10px 16px",
                        background: "none", border: "none", borderBottom: active ? "2px solid #5b5bd6" : "2px solid transparent",
                        color: active ? "#f0eeeb" : "rgba(240,238,235,0.35)",
                        fontSize: "12px", fontWeight: active ? 600 : 400,
                        cursor: "pointer", fontFamily: "inherit", transition: "color 0.1s",
                      }}
                    >
                      <Icon size={13} /> {labels[tab]}
                    </button>
                  );
                })}
              </div>

              {/* Panel body */}
              <div style={{ flex: 1, overflowY: "auto", padding: "20px" }}>

                {/* INFO TAB */}
                {activePanel === "info" && (
                  <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                    {/* Contact details */}
                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                      {selectedProspect.email && (
                        <InfoField icon={<Mail size={13} />} label="Email">
                          <a href={`mailto:${selectedProspect.email}`} style={{ color: "#818cf8", fontSize: "13px", textDecoration: "none" }}>
                            {selectedProspect.email}
                          </a>
                        </InfoField>
                      )}
                      {selectedProspect.phone && (
                        <InfoField icon={<Phone size={13} />} label="Phone">
                          <a href={`tel:${selectedProspect.phone}`} style={{ color: "#818cf8", fontSize: "13px", textDecoration: "none" }}>
                            {selectedProspect.phone}
                          </a>
                        </InfoField>
                      )}
                      {selectedProspect.website && (
                        <InfoField icon={<Globe size={13} />} label="Website">
                          <a href={selectedProspect.website} target="_blank" rel="noopener noreferrer"
                            style={{ color: "#818cf8", fontSize: "13px", textDecoration: "none", display: "flex", alignItems: "center", gap: "4px" }}>
                            {new URL(selectedProspect.website.startsWith("http") ? selectedProspect.website : `https://${selectedProspect.website}`).hostname}
                            <ExternalLink size={11} />
                          </a>
                        </InfoField>
                      )}
                      {selectedProspect.linkedinUrl && (
                        <InfoField icon={<Linkedin size={13} />} label="LinkedIn">
                          <a href={selectedProspect.linkedinUrl} target="_blank" rel="noopener noreferrer"
                            style={{ color: "#818cf8", fontSize: "13px", textDecoration: "none", display: "flex", alignItems: "center", gap: "4px" }}>
                            View profile <ExternalLink size={11} />
                          </a>
                        </InfoField>
                      )}
                      {selectedProspect.country && (
                        <InfoField icon={<MapPin size={13} />} label="Country">
                          <span style={{ fontSize: "13px", color: "#f0eeeb" }}>{selectedProspect.country}</span>
                        </InfoField>
                      )}
                      {selectedProspect.industry && (
                        <InfoField icon={<Tag size={13} />} label="Industry">
                          <span style={{ fontSize: "13px", color: "#f0eeeb" }}>{selectedProspect.industry}</span>
                        </InfoField>
                      )}
                    </div>

                    {selectedProspect.notes && (
                      <div style={{ borderRadius: "8px", border: panelBorder, padding: "12px 14px" }}>
                        <div style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "8px", color: "rgba(240,238,235,0.35)", fontSize: "11px", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em" }}>
                          <StickyNote size={11} /> Notes
                        </div>
                        <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.7)", margin: 0, lineHeight: 1.6, whiteSpace: "pre-wrap" }}>
                          {selectedProspect.notes}
                        </p>
                      </div>
                    )}

                    {selectedProspect.lastContactedAt && (
                      <p style={{ fontSize: "12px", color: "rgba(240,238,235,0.3)", margin: 0 }}>
                        Last contacted: {new Date(selectedProspect.lastContactedAt).toLocaleString()}
                      </p>
                    )}

                    {/* Quick actions */}
                    <div style={{ display: "flex", gap: "8px", marginTop: "4px" }}>
                      {selectedProspect.website && (
                        <button
                          onClick={() => setActivePanel("analyze")}
                          style={{
                            display: "flex", alignItems: "center", gap: "6px",
                            padding: "7px 12px", borderRadius: "6px",
                            border: "1px solid rgba(129,140,248,0.2)", background: "rgba(129,140,248,0.06)",
                            color: "#818cf8", fontSize: "12px", cursor: "pointer", fontFamily: "inherit",
                          }}
                        >
                          <Globe size={12} /> Analyze Website
                        </button>
                      )}
                      <button
                        onClick={() => setActivePanel("message")}
                        style={{
                          display: "flex", alignItems: "center", gap: "6px",
                          padding: "7px 12px", borderRadius: "6px",
                          border: "1px solid rgba(91,91,214,0.25)", background: "rgba(91,91,214,0.08)",
                          color: "#818cf8", fontSize: "12px", cursor: "pointer", fontFamily: "inherit",
                        }}
                      >
                        <Sparkles size={12} /> Draft Message
                      </button>
                    </div>
                  </div>
                )}

                {/* ANALYZE TAB */}
                {activePanel === "analyze" && (
                  <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                    <div>
                      <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.5)", margin: "0 0 12px", lineHeight: 1.6 }}>
                        Enter the prospect&apos;s website URL. Zyntral AI will fetch and analyze it, then give you talking points for outreach.
                      </p>
                      <div style={{ display: "flex", gap: "8px" }}>
                        <input
                          value={analyzeUrl}
                          onChange={(e) => setAnalyzeUrl(e.target.value)}
                          placeholder="https://example.com"
                          style={{
                            flex: 1, height: "36px", padding: "0 12px", borderRadius: "7px",
                            border: "1px solid rgba(255,255,255,0.09)",
                            background: "rgba(255,255,255,0.04)",
                            color: "#f0eeeb", fontSize: "13px", fontFamily: "inherit", outline: "none",
                          }}
                        />
                        <button
                          onClick={handleAnalyze}
                          disabled={!analyzeUrl.trim() || analyzing}
                          style={{
                            display: "flex", alignItems: "center", gap: "6px",
                            padding: "0 16px", height: "36px", borderRadius: "7px",
                            border: "none",
                            background: analyzing || !analyzeUrl.trim() ? "rgba(255,255,255,0.06)" : "#5b5bd6",
                            color: analyzing || !analyzeUrl.trim() ? "rgba(240,238,235,0.25)" : "#fff",
                            fontSize: "13px", fontWeight: 500, cursor: analyzing || !analyzeUrl.trim() ? "not-allowed" : "pointer",
                            fontFamily: "inherit", whiteSpace: "nowrap",
                          }}
                        >
                          <Sparkles size={13} />
                          {analyzing ? "Analyzing…" : "Analyze (1 credit)"}
                        </button>
                      </div>
                      {analyzeErr && <p style={{ fontSize: "12px", color: "#f87171", marginTop: "8px" }}>{analyzeErr}</p>}
                    </div>

                    {analyzeResult && (
                      <div style={{ borderRadius: "8px", border: "1px solid rgba(129,140,248,0.15)", background: "rgba(129,140,248,0.04)", padding: "16px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
                          <span style={{ fontSize: "12px", fontWeight: 600, color: "#818cf8" }}>Website Analysis</span>
                          <CopyButton text={analyzeResult} />
                        </div>
                        <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.75)", margin: 0, lineHeight: 1.7, whiteSpace: "pre-wrap" }}>
                          {analyzeResult}
                        </p>
                      </div>
                    )}
                  </div>
                )}

                {/* MESSAGE TAB */}
                {activePanel === "message" && (
                  <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                    <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.5)", margin: 0, lineHeight: 1.6 }}>
                      Zyntral AI will write a personalized outreach message based on this prospect&apos;s profile.
                    </p>

                    <div style={{ display: "flex", gap: "6px" }}>
                      {["EMAIL", "LINKEDIN", "SMS"].map((ch) => (
                        <button
                          key={ch}
                          onClick={() => setMsgChannel(ch)}
                          style={{
                            padding: "6px 14px", borderRadius: "6px",
                            border: `1px solid ${msgChannel === ch ? "rgba(91,91,214,0.4)" : "rgba(255,255,255,0.09)"}`,
                            background: msgChannel === ch ? "rgba(91,91,214,0.12)" : "rgba(255,255,255,0.03)",
                            color: msgChannel === ch ? "#f0eeeb" : "rgba(240,238,235,0.4)",
                            fontSize: "12px", fontWeight: msgChannel === ch ? 600 : 400,
                            cursor: "pointer", fontFamily: "inherit",
                          }}
                        >
                          {ch === "EMAIL" ? "Email" : ch === "LINKEDIN" ? "LinkedIn" : "SMS"}
                        </button>
                      ))}
                    </div>

                    {msgChannel === "LINKEDIN" && (
                      <div style={{ padding: "10px 12px", borderRadius: "7px", background: "rgba(245,179,25,0.06)", border: "1px solid rgba(245,179,25,0.15)", fontSize: "12px", color: "rgba(245,179,25,0.8)" }}>
                        LinkedIn direct messaging is not automated. Copy the generated message and send it manually via LinkedIn.
                      </div>
                    )}

                    <div style={{ display: "flex", flexDirection: "column", gap: "5px" }}>
                      <label style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.35)", letterSpacing: "0.05em", textTransform: "uppercase" }}>
                        Additional context (optional)
                      </label>
                      <textarea
                        value={msgNote}
                        onChange={(e) => setMsgNote(e.target.value)}
                        placeholder="e.g. I noticed they recently launched a new product..."
                        rows={3}
                        style={{
                          padding: "8px 10px", borderRadius: "6px",
                          border: "1px solid rgba(255,255,255,0.09)",
                          background: "rgba(255,255,255,0.04)",
                          color: "#f0eeeb", fontSize: "13px", fontFamily: "inherit",
                          outline: "none", resize: "vertical",
                        }}
                      />
                    </div>

                    <button
                      onClick={handleDraft}
                      disabled={drafting}
                      style={{
                        display: "flex", alignItems: "center", justifyContent: "center", gap: "6px",
                        padding: "9px", borderRadius: "7px", border: "none",
                        background: drafting ? "rgba(255,255,255,0.06)" : "#5b5bd6",
                        color: drafting ? "rgba(240,238,235,0.25)" : "#fff",
                        fontSize: "13px", fontWeight: 500, cursor: drafting ? "not-allowed" : "pointer",
                        fontFamily: "inherit",
                      }}
                    >
                      <Sparkles size={13} />
                      {drafting ? "Drafting…" : `Draft ${msgChannel === "EMAIL" ? "Email" : msgChannel === "LINKEDIN" ? "LinkedIn Message" : "SMS"} (1 credit)`}
                    </button>

                    {msgErr && <p style={{ fontSize: "12px", color: "#f87171", margin: 0 }}>{msgErr}</p>}

                    {msgResult && (
                      <div style={{ borderRadius: "8px", border: "1px solid rgba(52,211,153,0.15)", background: "rgba(52,211,153,0.04)", padding: "16px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
                          <span style={{ fontSize: "12px", fontWeight: 600, color: "#34d399" }}>
                            {msgChannel === "EMAIL" ? "Email Draft" : msgChannel === "LINKEDIN" ? "LinkedIn Message" : "SMS Draft"}
                          </span>
                          <CopyButton text={msgResult} />
                        </div>
                        <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.8)", margin: 0, lineHeight: 1.7, whiteSpace: "pre-wrap" }}>
                          {msgResult}
                        </p>
                        {msgChannel === "EMAIL" && selectedProspect.email && (
                          <a
                            href={`mailto:${selectedProspect.email}?body=${encodeURIComponent(msgResult)}`}
                            style={{
                              display: "inline-flex", alignItems: "center", gap: "5px",
                              marginTop: "12px", padding: "6px 12px",
                              borderRadius: "5px", border: "1px solid rgba(52,211,153,0.2)",
                              background: "rgba(52,211,153,0.06)",
                              color: "#34d399", fontSize: "12px", textDecoration: "none",
                            }}
                          >
                            <Mail size={12} /> Open in email client
                          </a>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>

      {/* Add/Edit Modal */}
      {showModal && (
        <div style={{
          position: "fixed", inset: 0, zIndex: 50,
          background: "rgba(0,0,0,0.6)", backdropFilter: "blur(4px)",
          display: "flex", alignItems: "center", justifyContent: "center", padding: "20px",
        }}
          onClick={(e) => { if (e.target === e.currentTarget) closeModal(); }}
        >
          <div style={{
            background: "#15151a", borderRadius: "14px",
            border: "1px solid rgba(255,255,255,0.09)",
            width: "100%", maxWidth: "560px", maxHeight: "90vh",
            display: "flex", flexDirection: "column", overflow: "hidden",
          }}>
            <div style={{ padding: "18px 22px", borderBottom: "1px solid rgba(255,255,255,0.06)", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
              <h2 style={{ fontSize: "15px", fontWeight: 600, color: "#f0eeeb", margin: 0 }}>
                {editProspect ? "Edit Prospect" : "Add Prospect"}
              </h2>
              <button onClick={closeModal} style={{ background: "none", border: "none", cursor: "pointer", color: "rgba(240,238,235,0.4)", padding: "4px" }}>
                <X size={16} />
              </button>
            </div>

            <div style={{ padding: "20px 22px", overflowY: "auto", display: "flex", flexDirection: "column", gap: "14px" }}>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                <FieldInput label="First Name *" value={form.firstName} onChange={(v) => setForm((f) => ({ ...f, firstName: v }))} placeholder="Jean" />
                <FieldInput label="Last Name" value={form.lastName} onChange={(v) => setForm((f) => ({ ...f, lastName: v }))} placeholder="Dupont" />
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                <FieldInput label="Company" value={form.company} onChange={(v) => setForm((f) => ({ ...f, company: v }))} placeholder="Acme Corp" />
                <FieldInput label="Website" value={form.website} onChange={(v) => setForm((f) => ({ ...f, website: v }))} placeholder="https://acmecorp.com" />
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                <FieldInput label="Email" type="email" value={form.email} onChange={(v) => setForm((f) => ({ ...f, email: v }))} placeholder="jean@acmecorp.com" />
                <FieldInput label="Phone" value={form.phone} onChange={(v) => setForm((f) => ({ ...f, phone: v }))} placeholder="+33 6 12 34 56 78" />
              </div>
              <FieldInput label="LinkedIn URL" value={form.linkedinUrl} onChange={(v) => setForm((f) => ({ ...f, linkedinUrl: v }))} placeholder="https://linkedin.com/in/jeandupont" />
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                <FieldSelect label="Country" value={form.country} onChange={(v) => setForm((f) => ({ ...f, country: v }))} options={COUNTRIES.map((c) => ({ value: c, label: c }))} />
                <FieldSelect label="Industry" value={form.industry} onChange={(v) => setForm((f) => ({ ...f, industry: v }))} options={INDUSTRIES.map((i) => ({ value: i, label: i }))} />
              </div>
              {editProspect && (
                <FieldSelect label="Status" value={form.status} onChange={(v) => setForm((f) => ({ ...f, status: v }))}
                  options={STATUS_OPTIONS.map((s) => ({ value: s, label: STATUS_STYLE[s]?.label ?? s }))} />
              )}
              <div style={{ display: "flex", flexDirection: "column", gap: "5px" }}>
                <label style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.35)", letterSpacing: "0.05em", textTransform: "uppercase" }}>Notes</label>
                <textarea
                  value={form.notes}
                  onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))}
                  placeholder="Any additional context about this prospect..."
                  rows={3}
                  style={{
                    padding: "8px 10px", borderRadius: "6px",
                    border: "1px solid rgba(255,255,255,0.09)",
                    background: "rgba(255,255,255,0.04)",
                    color: "#f0eeeb", fontSize: "13px", fontFamily: "inherit",
                    outline: "none", resize: "vertical",
                  }}
                />
              </div>

              {formError && <p style={{ fontSize: "12px", color: "#f87171", margin: 0 }}>{formError}</p>}
            </div>

            <div style={{ padding: "14px 22px", borderTop: "1px solid rgba(255,255,255,0.06)", display: "flex", justifyContent: "flex-end", gap: "8px" }}>
              <button onClick={closeModal} style={{
                padding: "7px 14px", borderRadius: "6px",
                border: "1px solid rgba(255,255,255,0.09)", background: "rgba(255,255,255,0.04)",
                color: "rgba(240,238,235,0.5)", fontSize: "13px", cursor: "pointer", fontFamily: "inherit",
              }}>
                Cancel
              </button>
              <button
                onClick={submitForm}
                disabled={!form.firstName.trim() || isBusy}
                style={{
                  padding: "7px 16px", borderRadius: "6px", border: "none",
                  background: !form.firstName.trim() || isBusy ? "rgba(255,255,255,0.06)" : "#5b5bd6",
                  color: !form.firstName.trim() || isBusy ? "rgba(240,238,235,0.25)" : "#fff",
                  fontSize: "13px", fontWeight: 500,
                  cursor: !form.firstName.trim() || isBusy ? "not-allowed" : "pointer",
                  fontFamily: "inherit",
                }}
              >
                {isBusy ? "Saving…" : editProspect ? "Save Changes" : "Add Prospect"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function InfoField({ icon, label, children }: { icon: React.ReactNode; label: string; children: React.ReactNode }) {
  return (
    <div style={{ borderRadius: "7px", border: "1px solid rgba(255,255,255,0.06)", padding: "10px 12px" }}>
      <div style={{ display: "flex", alignItems: "center", gap: "5px", marginBottom: "4px", color: "rgba(240,238,235,0.3)", fontSize: "11px", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.04em" }}>
        {icon} {label}
      </div>
      {children}
    </div>
  );
}
