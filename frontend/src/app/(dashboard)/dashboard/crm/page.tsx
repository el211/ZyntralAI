"use client";

import { useState, useMemo, useRef, useEffect } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api, unwrap, apiErrorMessage } from "@/lib/api";
import { useWorkspace } from "@/lib/workspace";
import {
  Plus, Search, Globe, Mail, Phone, Linkedin, Trash2,
  ChevronRight, X, Copy, Check, Sparkles, ExternalLink,
  Building2, MapPin, Tag, StickyNote, MessageSquare,
  BarChart3, Users, TrendingUp, Clock, Telescope, ArrowLeft,
  Loader2, CheckSquare, Square,
} from "lucide-react";

// ── Types ─────────────────────────────────────────────────────────────────────

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

interface DiscoveredProspect {
  company: string;
  website?: string;
  description?: string;
  country?: string;
  industry?: string;
  estimatedSize?: string;
  firstName?: string;
  lastName?: string;
  contactEmail?: string;
}

interface EnrichmentResult {
  email?: string;
  metaDescription?: string;
}

// ── Constants ─────────────────────────────────────────────────────────────────

const STATUS_OPTIONS = ["NEW", "CONTACTED", "RESPONDED", "MEETING_BOOKED", "CONVERTED", "LOST"];

const STATUS_STYLE: Record<string, { color: string; bg: string; border: string; label: string }> = {
  NEW:            { color: "#818cf8", bg: "rgba(129,140,248,0.1)", border: "rgba(129,140,248,0.2)", label: "New" },
  CONTACTED:      { color: "#f5b319", bg: "rgba(245,179,25,0.1)",  border: "rgba(245,179,25,0.2)",  label: "Contacted" },
  RESPONDED:      { color: "#34d399", bg: "rgba(52,211,153,0.1)",  border: "rgba(52,211,153,0.2)",  label: "Responded" },
  MEETING_BOOKED: { color: "#a78bfa", bg: "rgba(167,139,250,0.1)", border: "rgba(167,139,250,0.2)", label: "Meeting Booked" },
  CONVERTED:      { color: "#34d399", bg: "rgba(52,211,153,0.12)", border: "rgba(52,211,153,0.25)", label: "Converted" },
  LOST:           { color: "#f87171", bg: "rgba(248,113,113,0.08)", border: "rgba(248,113,113,0.2)", label: "Lost" },
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
];

const EMPTY_FORM = {
  firstName: "", lastName: "", company: "", website: "",
  email: "", phone: "", linkedinUrl: "", country: "",
  industry: "", notes: "", status: "NEW",
};

// ── Reusable components ───────────────────────────────────────────────────────

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
        type={type} value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder}
        style={{
          height: "34px", padding: "0 10px", borderRadius: "6px",
          border: "1px solid rgba(255,255,255,0.09)", background: "rgba(255,255,255,0.04)",
          color: "#f0eeeb", fontSize: "13px", fontFamily: "inherit", outline: "none",
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
        value={value} onChange={(e) => onChange(e.target.value)}
        style={{
          height: "34px", padding: "0 10px", borderRadius: "6px",
          border: "1px solid rgba(255,255,255,0.09)", background: "rgba(255,255,255,0.04)",
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
        border: "1px solid rgba(255,255,255,0.09)", background: "rgba(255,255,255,0.04)",
        color: copied ? "#34d399" : "rgba(240,238,235,0.5)",
        fontSize: "12px", cursor: "pointer", fontFamily: "inherit",
      }}
    >
      {copied ? <Check size={12} /> : <Copy size={12} />}
      {copied ? "Copied!" : "Copy"}
    </button>
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

// ── Main page ─────────────────────────────────────────────────────────────────

export default function CrmPage() {
  const { current } = useWorkspace();
  const qc = useQueryClient();

  // Mode: "prospects" (default) or "discover"
  const [mode, setMode] = useState<"prospects" | "discover">("prospects");

  // ── Prospects state ──
  const [search, setSearch] = useState("");
  const [filterStatus, setFilterStatus] = useState("");
  const [filterCountry, setFilterCountry] = useState("");
  const [filterIndustry, setFilterIndustry] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [editProspect, setEditProspect] = useState<Prospect | null>(null);
  const [form, setForm] = useState({ ...EMPTY_FORM });
  const [formError, setFormError] = useState<string | null>(null);
  const [formBusy, setFormBusy] = useState(false);
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
  const analyzeResultRef = useRef<HTMLDivElement>(null);
  const msgResultRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (analyzeResult) analyzeResultRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  }, [analyzeResult]);

  useEffect(() => {
    if (msgResult) msgResultRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  }, [msgResult]);

  // ── Discover state ──
  const [discIndustry, setDiscIndustry] = useState("");
  const [discCountry, setDiscCountry] = useState("");
  const [discKeywords, setDiscKeywords] = useState("");
  const [discCount, setDiscCount] = useState(10);
  const [discovering, setDiscovering] = useState(false);
  const [discResults, setDiscResults] = useState<DiscoveredProspect[]>([]);
  const [discErr, setDiscErr] = useState("");
  const [enrichments, setEnrichments] = useState<Record<string, EnrichmentResult & { loading?: boolean }>>({});
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [importing, setImporting] = useState(false);
  const [importMsg, setImportMsg] = useState("");

  // ── Data ──
  const { data: prospects = [] } = useQuery<Prospect[]>({
    queryKey: ["crm-prospects", current?.id],
    enabled: !!current,
    queryFn: async () =>
      unwrap<Prospect[]>((await api.get(`/workspaces/${current!.id}/crm/prospects`)).data),
  });

  const stats = useMemo(() => ({
    total: prospects.length,
    contacted: prospects.filter((p) => p.status !== "NEW").length,
    converted: prospects.filter((p) => p.status === "CONVERTED").length,
    meetings: prospects.filter((p) => p.status === "MEETING_BOOKED").length,
  }), [prospects]);

  const filtered = useMemo(() => prospects.filter((p) => {
    const q = search.toLowerCase();
    if (q && !`${p.firstName} ${p.lastName ?? ""} ${p.company ?? ""} ${p.email ?? ""}`.toLowerCase().includes(q)) return false;
    if (filterStatus && p.status !== filterStatus) return false;
    if (filterCountry && p.country !== filterCountry) return false;
    if (filterIndustry && p.industry !== filterIndustry) return false;
    return true;
  }), [prospects, search, filterStatus, filterCountry, filterIndustry]);

  // ── Prospect CRUD ──
  function openAdd() { setEditProspect(null); setForm({ ...EMPTY_FORM }); setFormError(null); setShowModal(true); }
  function openEdit(p: Prospect) {
    setEditProspect(p);
    setForm({ firstName: p.firstName, lastName: p.lastName ?? "", company: p.company ?? "",
      website: p.website ?? "", email: p.email ?? "", phone: p.phone ?? "",
      linkedinUrl: p.linkedinUrl ?? "", country: p.country ?? "",
      industry: p.industry ?? "", notes: p.notes ?? "", status: p.status });
    setFormError(null);
    setShowModal(true);
  }
  function closeModal() { setShowModal(false); setEditProspect(null); setForm({ ...EMPTY_FORM }); }

  async function submitForm() {
    setFormBusy(true); setFormError(null);
    try {
      if (editProspect) {
        await api.put(`/workspaces/${current!.id}/crm/prospects/${editProspect.id}`, form);
      } else {
        await api.post(`/workspaces/${current!.id}/crm/prospects`, form);
      }
      qc.invalidateQueries({ queryKey: ["crm-prospects", current?.id] });
      closeModal();
    } catch (e) { setFormError(apiErrorMessage(e)); }
    finally { setFormBusy(false); }
  }

  async function deleteProspect(id: string) {
    if (!confirm("Delete this prospect?")) return;
    await api.delete(`/workspaces/${current!.id}/crm/prospects/${id}`);
    qc.invalidateQueries({ queryKey: ["crm-prospects", current?.id] });
    setSelectedProspect(null);
  }

  function selectProspect(p: Prospect) {
    setSelectedProspect(p); setActivePanel("info");
    setAnalyzeUrl(p.website ?? ""); setAnalyzeResult(""); setAnalyzeErr("");
    setMsgResult(""); setMsgErr(""); setMsgNote("");
  }

  async function handleAnalyze() {
    if (!current || !analyzeUrl) return;
    setAnalyzing(true); setAnalyzeResult(""); setAnalyzeErr("");
    try {
      const res = await api.post(`/workspaces/${current.id}/crm/analyze-website`, { url: analyzeUrl });
      setAnalyzeResult(unwrap<{ text: string }>(res.data).text);
    } catch (e) { setAnalyzeErr(apiErrorMessage(e)); }
    finally { setAnalyzing(false); }
  }

  async function handleDraft() {
    if (!current || !selectedProspect) return;
    setDrafting(true); setMsgResult(""); setMsgErr("");
    try {
      const res = await api.post(`/workspaces/${current.id}/crm/draft-message`, {
        prospectId: selectedProspect.id, channel: msgChannel, customNote: msgNote,
      });
      setMsgResult(unwrap<{ text: string }>(res.data).text);
      qc.invalidateQueries({ queryKey: ["crm-prospects", current?.id] });
    } catch (e) { setMsgErr(apiErrorMessage(e)); }
    finally { setDrafting(false); }
  }

  // ── Discover ──
  async function handleDiscover() {
    if (!current || !discIndustry || !discCountry) return;
    setDiscovering(true); setDiscResults([]); setDiscErr(""); setSelected(new Set()); setEnrichments({});
    try {
      const res = await api.post(`/workspaces/${current.id}/crm/discover`, {
        industry: discIndustry, country: discCountry,
        keywords: discKeywords, count: discCount,
      });
      const results = unwrap<DiscoveredProspect[]>(res.data);
      setDiscResults(results);
      setSelected(new Set(results.map((_, i) => i))); // select all by default
      // Auto-enrich all that have a website
      results.forEach((r, i) => { if (r.website) enrichOne(r.website, i); });
    } catch (e) { setDiscErr(apiErrorMessage(e)); }
    finally { setDiscovering(false); }
  }

  async function enrichOne(website: string, index: number) {
    if (!current) return;
    setEnrichments((prev) => ({ ...prev, [index]: { loading: true } }));
    try {
      const res = await api.post(`/workspaces/${current.id}/crm/enrich`, { website });
      const data = unwrap<EnrichmentResult>(res.data);
      setEnrichments((prev) => ({ ...prev, [index]: { ...data, loading: false } }));
    } catch {
      setEnrichments((prev) => ({ ...prev, [index]: { loading: false } }));
    }
  }

  function toggleSelect(i: number) {
    setSelected((prev) => { const n = new Set(prev); n.has(i) ? n.delete(i) : n.add(i); return n; });
  }

  async function importSelected() {
    if (!current || selected.size === 0) return;
    setImporting(true); setImportMsg("");
    let added = 0;
    for (const i of selected) {
      const r = discResults[i];
      const enrich = enrichments[i];
      try {
        await api.post(`/workspaces/${current.id}/crm/prospects`, {
          firstName: r.firstName || r.company?.split(" ")[0] || "Contact",
          lastName: r.lastName || null,
          company: r.company,
          website: r.website,
          email: enrich?.email || r.contactEmail || null,
          phone: null,
          linkedinUrl: null,
          country: r.country,
          industry: r.industry,
          notes: r.description || null,
          status: "NEW",
        });
        added++;
      } catch { /* skip errors */ }
    }
    qc.invalidateQueries({ queryKey: ["crm-prospects", current?.id] });
    setImportMsg(`${added} prospect${added !== 1 ? "s" : ""} added successfully.`);
    setImporting(false);
    setTimeout(() => { setMode("prospects"); setImportMsg(""); }, 1500);
  }

  if (!current) return <p style={{ color: "rgba(240,238,235,0.4)", fontSize: "14px" }}>Select a workspace first.</p>;

  const panelBorder = "1px solid rgba(255,255,255,0.07)";
  const cardBg = "rgba(255,255,255,0.02)";

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>

      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
          {mode === "discover" && (
            <button
              onClick={() => setMode("prospects")}
              style={{ background: "none", border: "none", cursor: "pointer", color: "rgba(240,238,235,0.4)", padding: "4px", display: "flex" }}
            >
              <ArrowLeft size={18} />
            </button>
          )}
          <div>
            <h1 style={{ fontSize: "22px", fontWeight: 700, color: "#f0eeeb", letterSpacing: "-0.02em", margin: "0 0 4px" }}>
              {mode === "discover" ? "Discover Prospects" : "Zyntral CRM"}
            </h1>
            <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.35)", margin: 0 }}>
              {mode === "discover"
                ? "AI searches for companies matching your criteria and auto-extracts their contact info"
                : "Manage prospects, analyze websites, and draft personalized outreach"}
            </p>
          </div>
        </div>
        <div style={{ display: "flex", gap: "8px" }}>
          {mode === "prospects" && (
            <>
              <button
                onClick={() => { setMode("discover"); setDiscResults([]); setDiscErr(""); setSelected(new Set()); }}
                style={{
                  display: "flex", alignItems: "center", gap: "6px",
                  padding: "8px 14px", borderRadius: "7px",
                  border: "1px solid rgba(91,91,214,0.3)", background: "rgba(91,91,214,0.08)",
                  color: "#818cf8", fontSize: "13px", fontWeight: 500,
                  cursor: "pointer", fontFamily: "inherit",
                }}
              >
                <Telescope size={14} /> AI Discover
              </button>
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
                <Plus size={14} /> Add Manually
              </button>
            </>
          )}
        </div>
      </div>

      {/* ── DISCOVER MODE ───────────────────────────────────────────────────── */}
      {mode === "discover" && (
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>

          {/* Discovery form */}
          <div style={{ borderRadius: "12px", border: panelBorder, background: cardBg, padding: "20px 24px" }}>
            <p style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.3)", letterSpacing: "0.07em", textTransform: "uppercase", margin: "0 0 16px" }}>
              Search criteria
            </p>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 2fr", gap: "12px", marginBottom: "14px" }}>
              <FieldSelect
                label="Industry *"
                value={discIndustry}
                onChange={setDiscIndustry}
                options={INDUSTRIES.map((i) => ({ value: i, label: i }))}
              />
              <FieldSelect
                label="Country *"
                value={discCountry}
                onChange={setDiscCountry}
                options={COUNTRIES.map((c) => ({ value: c, label: c }))}
              />
              <FieldInput
                label="Keywords / target description"
                value={discKeywords}
                onChange={setDiscKeywords}
                placeholder="e.g. startups that need content marketing, SMEs without a blog..."
              />
            </div>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                <span style={{ fontSize: "12px", color: "rgba(240,238,235,0.4)" }}>Results:</span>
                {[5, 10].map((n) => (
                  <button
                    key={n}
                    onClick={() => setDiscCount(n)}
                    style={{
                      padding: "4px 12px", borderRadius: "5px",
                      border: `1px solid ${discCount === n ? "rgba(91,91,214,0.4)" : "rgba(255,255,255,0.09)"}`,
                      background: discCount === n ? "rgba(91,91,214,0.12)" : "rgba(255,255,255,0.03)",
                      color: discCount === n ? "#f0eeeb" : "rgba(240,238,235,0.4)",
                      fontSize: "12px", cursor: "pointer", fontFamily: "inherit", fontWeight: discCount === n ? 600 : 400,
                    }}
                  >
                    {n}
                  </button>
                ))}
              </div>
              <button
                onClick={handleDiscover}
                disabled={!discIndustry || !discCountry || discovering}
                style={{
                  display: "flex", alignItems: "center", gap: "7px",
                  padding: "9px 20px", borderRadius: "7px", border: "none",
                  background: (!discIndustry || !discCountry || discovering) ? "rgba(255,255,255,0.06)" : "#5b5bd6",
                  color: (!discIndustry || !discCountry || discovering) ? "rgba(240,238,235,0.25)" : "#fff",
                  fontSize: "13px", fontWeight: 500,
                  cursor: (!discIndustry || !discCountry || discovering) ? "not-allowed" : "pointer",
                  fontFamily: "inherit",
                }}
              >
                {discovering ? <Loader2 size={14} style={{ animation: "spin 1s linear infinite" }} /> : <Sparkles size={14} />}
                {discovering ? "Searching…" : `Discover ${discCount} Prospects (1 credit)`}
              </button>
            </div>

            {discErr && <p style={{ fontSize: "12px", color: "#f87171", marginTop: "10px" }}>{discErr}</p>}
          </div>

          {/* Disclaimer */}
          {discResults.length === 0 && !discovering && (
            <div style={{ padding: "14px 18px", borderRadius: "8px", border: "1px solid rgba(245,179,25,0.12)", background: "rgba(245,179,25,0.04)", fontSize: "12px", color: "rgba(245,179,25,0.7)", lineHeight: 1.6 }}>
              AI discovers companies from its training knowledge. Results are based on AI knowledge up to its cutoff date — verify details before outreach. Email extraction is done by fetching each company website.
            </div>
          )}

          {/* Results */}
          {discResults.length > 0 && (
            <>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                  <span style={{ fontSize: "13px", color: "rgba(240,238,235,0.5)" }}>
                    {discResults.length} companies found
                  </span>
                  <button
                    onClick={() => setSelected(selected.size === discResults.length ? new Set() : new Set(discResults.map((_, i) => i)))}
                    style={{ background: "none", border: "none", cursor: "pointer", color: "#818cf8", fontSize: "12px", fontFamily: "inherit", padding: 0 }}
                  >
                    {selected.size === discResults.length ? "Deselect all" : "Select all"}
                  </button>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                  {importMsg && <span style={{ fontSize: "12px", color: "#34d399" }}>{importMsg}</span>}
                  <button
                    onClick={importSelected}
                    disabled={selected.size === 0 || importing}
                    style={{
                      display: "flex", alignItems: "center", gap: "6px",
                      padding: "8px 16px", borderRadius: "7px", border: "none",
                      background: selected.size === 0 || importing ? "rgba(255,255,255,0.06)" : "#5b5bd6",
                      color: selected.size === 0 || importing ? "rgba(240,238,235,0.25)" : "#fff",
                      fontSize: "13px", fontWeight: 500,
                      cursor: selected.size === 0 || importing ? "not-allowed" : "pointer",
                      fontFamily: "inherit",
                    }}
                  >
                    {importing ? <Loader2 size={13} style={{ animation: "spin 1s linear infinite" }} /> : <Plus size={13} />}
                    {importing ? "Adding…" : `Add ${selected.size} Selected to Prospects`}
                  </button>
                </div>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "12px" }}>
                {discResults.map((r, i) => {
                  const enrich = enrichments[i];
                  const isSelected = selected.has(i);
                  const email = enrich?.email || r.contactEmail;

                  return (
                    <div
                      key={i}
                      onClick={() => toggleSelect(i)}
                      style={{
                        borderRadius: "10px", padding: "16px",
                        border: isSelected ? "1px solid rgba(91,91,214,0.35)" : "1px solid rgba(255,255,255,0.07)",
                        background: isSelected ? "rgba(91,91,214,0.06)" : "rgba(255,255,255,0.02)",
                        cursor: "pointer", transition: "all 0.12s",
                        display: "flex", flexDirection: "column", gap: "10px",
                      }}
                    >
                      {/* Card header */}
                      <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: "8px" }}>
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <div style={{ fontSize: "14px", fontWeight: 600, color: "#f0eeeb", marginBottom: "2px" }}>
                            {r.company}
                          </div>
                          {r.estimatedSize && (
                            <span style={{ fontSize: "10px", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", color: "rgba(240,238,235,0.3)" }}>
                              {r.estimatedSize}
                            </span>
                          )}
                        </div>
                        <div style={{ color: isSelected ? "#818cf8" : "rgba(240,238,235,0.2)", flexShrink: 0 }}>
                          {isSelected ? <CheckSquare size={16} /> : <Square size={16} />}
                        </div>
                      </div>

                      {/* Description */}
                      {(r.description || enrich?.metaDescription) && (
                        <p style={{ fontSize: "12px", color: "rgba(240,238,235,0.5)", margin: 0, lineHeight: 1.5 }}>
                          {enrich?.metaDescription || r.description}
                        </p>
                      )}

                      {/* Meta */}
                      <div style={{ display: "flex", flexDirection: "column", gap: "5px" }}>
                        {r.website && (
                          <a
                            href={r.website} target="_blank" rel="noopener noreferrer"
                            onClick={(e) => e.stopPropagation()}
                            style={{ display: "flex", alignItems: "center", gap: "5px", fontSize: "12px", color: "#818cf8", textDecoration: "none" }}
                          >
                            <Globe size={11} />
                            {r.website.replace(/^https?:\/\//, "").replace(/\/$/, "")}
                            <ExternalLink size={10} />
                          </a>
                        )}

                        {/* Email from enrichment */}
                        {enrich?.loading ? (
                          <span style={{ display: "flex", alignItems: "center", gap: "5px", fontSize: "12px", color: "rgba(240,238,235,0.25)" }}>
                            <Loader2 size={11} style={{ animation: "spin 1s linear infinite" }} /> Fetching contact email…
                          </span>
                        ) : email ? (
                          <a
                            href={`mailto:${email}`}
                            onClick={(e) => e.stopPropagation()}
                            style={{ display: "flex", alignItems: "center", gap: "5px", fontSize: "12px", color: "#34d399", textDecoration: "none" }}
                          >
                            <Mail size={11} /> {email}
                          </a>
                        ) : r.website ? (
                          <span style={{ display: "flex", alignItems: "center", gap: "5px", fontSize: "12px", color: "rgba(240,238,235,0.2)" }}>
                            <Mail size={11} /> No public email found
                          </span>
                        ) : null}

                        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                          {r.country && (
                            <span style={{ display: "flex", alignItems: "center", gap: "4px", fontSize: "11px", color: "rgba(240,238,235,0.3)" }}>
                              <MapPin size={10} /> {r.country}
                            </span>
                          )}
                          {r.industry && (
                            <span style={{ display: "flex", alignItems: "center", gap: "4px", fontSize: "11px", color: "rgba(240,238,235,0.3)" }}>
                              <Tag size={10} /> {r.industry}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Bottom import bar */}
              <div style={{ position: "sticky", bottom: 0, padding: "12px 0", display: "flex", justifyContent: "flex-end" }}>
                <button
                  onClick={importSelected}
                  disabled={selected.size === 0 || importing}
                  style={{
                    display: "flex", alignItems: "center", gap: "7px",
                    padding: "10px 22px", borderRadius: "8px", border: "none",
                    background: selected.size === 0 || importing ? "rgba(255,255,255,0.06)" : "#5b5bd6",
                    color: selected.size === 0 || importing ? "rgba(240,238,235,0.25)" : "#fff",
                    fontSize: "13px", fontWeight: 500,
                    cursor: selected.size === 0 || importing ? "not-allowed" : "pointer",
                    fontFamily: "inherit",
                    boxShadow: selected.size > 0 ? "0 4px 20px rgba(91,91,214,0.3)" : "none",
                  }}
                >
                  {importing ? <Loader2 size={14} style={{ animation: "spin 1s linear infinite" }} /> : <Plus size={14} />}
                  {importing ? "Adding…" : `Add ${selected.size} of ${discResults.length} to My Prospects`}
                </button>
              </div>
            </>
          )}
        </div>
      )}

      {/* ── PROSPECTS MODE ──────────────────────────────────────────────────── */}
      {mode === "prospects" && (
        <>
          {/* Stats row */}
          <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "12px" }}>
            {[
              { label: "Total Prospects", value: stats.total, icon: Users, color: "#818cf8" },
              { label: "Engaged", value: stats.contacted, icon: TrendingUp, color: "#f5b319" },
              { label: "Meetings Booked", value: stats.meetings, icon: Clock, color: "#a78bfa" },
              { label: "Converted", value: stats.converted, icon: BarChart3, color: "#34d399" },
            ].map(({ label, value, icon: Icon, color }) => (
              <div key={label} style={{ borderRadius: "10px", border: panelBorder, background: cardBg, padding: "16px 18px", display: "flex", alignItems: "center", gap: "12px" }}>
                <div style={{ width: "34px", height: "34px", borderRadius: "8px", background: `${color}18`, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  <Icon size={15} style={{ color }} />
                </div>
                <div>
                  <div style={{ fontSize: "20px", fontWeight: 700, color: "#f0eeeb", lineHeight: 1 }}>{value}</div>
                  <div style={{ fontSize: "11px", color: "rgba(240,238,235,0.35)", marginTop: "2px" }}>{label}</div>
                </div>
              </div>
            ))}
          </div>

          {/* Main: list + detail */}
          <div style={{ display: "flex", gap: "16px", minHeight: "500px" }}>

            {/* Left: filters + list */}
            <div style={{ flex: "0 0 380px", display: "flex", flexDirection: "column", gap: "10px" }}>
              <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                <div style={{ position: "relative" }}>
                  <Search size={13} style={{ position: "absolute", left: "10px", top: "50%", transform: "translateY(-50%)", color: "rgba(240,238,235,0.25)", pointerEvents: "none" }} />
                  <input
                    placeholder="Search prospects..."
                    value={search} onChange={(e) => setSearch(e.target.value)}
                    style={{
                      width: "100%", height: "34px", padding: "0 10px 0 32px",
                      borderRadius: "7px", border: "1px solid rgba(255,255,255,0.09)",
                      background: "rgba(255,255,255,0.04)", color: "#f0eeeb",
                      fontSize: "13px", fontFamily: "inherit", outline: "none", boxSizing: "border-box",
                    }}
                  />
                </div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "6px" }}>
                  {([
                    { label: "Status",   value: filterStatus,   set: setFilterStatus,   items: STATUS_OPTIONS.map((s) => ({ value: s, label: STATUS_STYLE[s]?.label ?? s })) },
                    { label: "Country",  value: filterCountry,  set: setFilterCountry,  items: COUNTRIES.slice(1).map((c) => ({ value: c, label: c })) },
                    { label: "Industry", value: filterIndustry, set: setFilterIndustry, items: INDUSTRIES.map((i) => ({ value: i, label: i })) },
                  ] as const).map(({ label, value, set, items }) => (
                    <select key={label} value={value} onChange={(e) => set(e.target.value)}
                      style={{ height: "30px", padding: "0 6px", borderRadius: "5px", border: "1px solid rgba(255,255,255,0.09)", background: "rgba(255,255,255,0.04)", color: value ? "#f0eeeb" : "rgba(240,238,235,0.35)", fontSize: "11px", fontFamily: "inherit", outline: "none" }}>
                      <option value="" style={{ background: "#111113" }}>All {label}s</option>
                      {items.map((o) => <option key={o.value} value={o.value} style={{ background: "#111113" }}>{o.label}</option>)}
                    </select>
                  ))}
                </div>
              </div>

              <div style={{ display: "flex", flexDirection: "column", gap: "6px", overflowY: "auto", flex: 1 }}>
                {filtered.length === 0 && (
                  <div style={{ textAlign: "center", padding: "40px 20px", border: "1px dashed rgba(255,255,255,0.08)", borderRadius: "10px" }}>
                    <Users size={28} style={{ color: "rgba(240,238,235,0.15)", display: "block", margin: "0 auto 10px" }} />
                    <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.3)", margin: "0 0 10px" }}>
                      {prospects.length === 0 ? "No prospects yet" : "No prospects match your filters"}
                    </p>
                    {prospects.length === 0 && (
                      <button onClick={() => setMode("discover")} style={{ display: "inline-flex", alignItems: "center", gap: "6px", padding: "6px 14px", borderRadius: "6px", border: "1px solid rgba(91,91,214,0.3)", background: "rgba(91,91,214,0.08)", color: "#818cf8", fontSize: "12px", cursor: "pointer", fontFamily: "inherit" }}>
                        <Telescope size={12} /> Try AI Discover
                      </button>
                    )}
                  </div>
                )}
                {filtered.map((p) => {
                  const isSelected = selectedProspect?.id === p.id;
                  return (
                    <div key={p.id} onClick={() => selectProspect(p)} style={{
                      padding: "12px 14px", borderRadius: "8px",
                      border: isSelected ? "1px solid rgba(91,91,214,0.35)" : "1px solid rgba(255,255,255,0.06)",
                      background: isSelected ? "rgba(91,91,214,0.07)" : "rgba(255,255,255,0.02)",
                      cursor: "pointer", transition: "all 0.12s",
                      display: "flex", alignItems: "center", justifyContent: "space-between",
                    }}>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "4px" }}>
                          <span style={{ fontSize: "13px", fontWeight: 500, color: "#f0eeeb", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                            {p.firstName}{p.lastName ? ` ${p.lastName}` : ""}
                          </span>
                          <StatusBadge status={p.status} />
                        </div>
                        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                          {p.company && <span style={{ fontSize: "12px", color: "rgba(240,238,235,0.4)", display: "flex", alignItems: "center", gap: "4px" }}><Building2 size={11} /> {p.company}</span>}
                          {p.country && <span style={{ fontSize: "12px", color: "rgba(240,238,235,0.3)", display: "flex", alignItems: "center", gap: "4px" }}><MapPin size={11} /> {p.country}</span>}
                        </div>
                      </div>
                      <ChevronRight size={14} style={{ color: "rgba(240,238,235,0.2)", flexShrink: 0, marginLeft: "8px" }} />
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Right: detail panel */}
            <div style={{ flex: 1, borderRadius: "12px", border: panelBorder, background: cardBg, display: "flex", flexDirection: "column", overflow: "hidden" }}>
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
                  <div style={{ padding: "16px 20px", borderBottom: panelBorder, display: "flex", alignItems: "flex-start", justifyContent: "space-between" }}>
                    <div>
                      <h2 style={{ fontSize: "16px", fontWeight: 600, color: "#f0eeeb", margin: "0 0 4px" }}>
                        {selectedProspect.firstName}{selectedProspect.lastName ? ` ${selectedProspect.lastName}` : ""}
                      </h2>
                      <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                        {selectedProspect.company && <span style={{ fontSize: "12px", color: "rgba(240,238,235,0.4)", display: "flex", alignItems: "center", gap: "4px" }}><Building2 size={11} /> {selectedProspect.company}</span>}
                        <StatusBadge status={selectedProspect.status} />
                      </div>
                    </div>
                    <div style={{ display: "flex", gap: "6px" }}>
                      <button onClick={() => openEdit(selectedProspect)} style={{ padding: "5px 10px", borderRadius: "5px", border: "1px solid rgba(255,255,255,0.09)", background: "rgba(255,255,255,0.04)", color: "rgba(240,238,235,0.6)", fontSize: "12px", cursor: "pointer", fontFamily: "inherit" }}>Edit</button>
                      <button onClick={() => deleteProspect(selectedProspect.id)} style={{ width: "28px", height: "28px", borderRadius: "5px", border: "1px solid rgba(248,113,113,0.2)", background: "rgba(248,113,113,0.06)", color: "rgba(248,113,113,0.7)", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}><Trash2 size={13} /></button>
                    </div>
                  </div>

                  {/* Sub-tabs */}
                  <div style={{ display: "flex", borderBottom: panelBorder }}>
                    {(["info", "analyze", "message"] as const).map((tab) => {
                      const labels = { info: "Info", analyze: "Analyze Website", message: "Draft Message" };
                      const icons = { info: Tag, analyze: Globe, message: MessageSquare };
                      const Icon = icons[tab];
                      const active = activePanel === tab;
                      return (
                        <button key={tab} onClick={() => setActivePanel(tab)} style={{
                          display: "flex", alignItems: "center", gap: "6px",
                          padding: "10px 16px",
                          background: "none", border: "none",
                          borderBottom: active ? "2px solid #5b5bd6" : "2px solid transparent",
                          color: active ? "#f0eeeb" : "rgba(240,238,235,0.35)",
                          fontSize: "12px", fontWeight: active ? 600 : 400,
                          cursor: "pointer", fontFamily: "inherit",
                        }}>
                          <Icon size={13} /> {labels[tab]}
                        </button>
                      );
                    })}
                  </div>

                  {/* Panel body */}
                  <div style={{ flex: 1, overflowY: "auto", padding: "20px" }}>
                    {activePanel === "info" && (
                      <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                          {selectedProspect.email && (
                            <InfoField icon={<Mail size={13} />} label="Email">
                              <a href={`mailto:${selectedProspect.email}`} style={{ color: "#818cf8", fontSize: "13px", textDecoration: "none" }}>{selectedProspect.email}</a>
                            </InfoField>
                          )}
                          {selectedProspect.phone && (
                            <InfoField icon={<Phone size={13} />} label="Phone">
                              <a href={`tel:${selectedProspect.phone}`} style={{ color: "#818cf8", fontSize: "13px", textDecoration: "none" }}>{selectedProspect.phone}</a>
                            </InfoField>
                          )}
                          {selectedProspect.website && (
                            <InfoField icon={<Globe size={13} />} label="Website">
                              <a href={selectedProspect.website} target="_blank" rel="noopener noreferrer" style={{ color: "#818cf8", fontSize: "13px", textDecoration: "none", display: "flex", alignItems: "center", gap: "4px" }}>
                                {(() => { try { return new URL(selectedProspect.website.startsWith("http") ? selectedProspect.website : `https://${selectedProspect.website}`).hostname; } catch { return selectedProspect.website; } })()}
                                <ExternalLink size={11} />
                              </a>
                            </InfoField>
                          )}
                          {selectedProspect.linkedinUrl && (
                            <InfoField icon={<Linkedin size={13} />} label="LinkedIn">
                              <a href={selectedProspect.linkedinUrl} target="_blank" rel="noopener noreferrer" style={{ color: "#818cf8", fontSize: "13px", textDecoration: "none", display: "flex", alignItems: "center", gap: "4px" }}>View profile <ExternalLink size={11} /></a>
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
                            <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.7)", margin: 0, lineHeight: 1.6, whiteSpace: "pre-wrap" }}>{selectedProspect.notes}</p>
                          </div>
                        )}
                        {selectedProspect.lastContactedAt && (
                          <p style={{ fontSize: "12px", color: "rgba(240,238,235,0.3)", margin: 0 }}>Last contacted: {new Date(selectedProspect.lastContactedAt).toLocaleString()}</p>
                        )}
                        <div style={{ display: "flex", gap: "8px", marginTop: "4px" }}>
                          {selectedProspect.website && (
                            <button onClick={() => setActivePanel("analyze")} style={{ display: "flex", alignItems: "center", gap: "6px", padding: "7px 12px", borderRadius: "6px", border: "1px solid rgba(129,140,248,0.2)", background: "rgba(129,140,248,0.06)", color: "#818cf8", fontSize: "12px", cursor: "pointer", fontFamily: "inherit" }}>
                              <Globe size={12} /> Analyze Website
                            </button>
                          )}
                          <button onClick={() => setActivePanel("message")} style={{ display: "flex", alignItems: "center", gap: "6px", padding: "7px 12px", borderRadius: "6px", border: "1px solid rgba(91,91,214,0.25)", background: "rgba(91,91,214,0.08)", color: "#818cf8", fontSize: "12px", cursor: "pointer", fontFamily: "inherit" }}>
                            <Sparkles size={12} /> Draft Message
                          </button>
                        </div>
                      </div>
                    )}

                    {activePanel === "analyze" && (
                      <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                        <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.5)", margin: 0, lineHeight: 1.6 }}>
                          AI fetches the website and identifies what&apos;s missing, talking points, and opportunities.
                        </p>
                        <div style={{ display: "flex", gap: "8px" }}>
                          <input value={analyzeUrl} onChange={(e) => setAnalyzeUrl(e.target.value)} placeholder="https://example.com"
                            style={{ flex: 1, height: "36px", padding: "0 12px", borderRadius: "7px", border: "1px solid rgba(255,255,255,0.09)", background: "rgba(255,255,255,0.04)", color: "#f0eeeb", fontSize: "13px", fontFamily: "inherit", outline: "none" }} />
                          <button onClick={handleAnalyze} disabled={!analyzeUrl.trim() || analyzing}
                            style={{ display: "flex", alignItems: "center", gap: "6px", padding: "0 16px", height: "36px", borderRadius: "7px", border: "none", background: analyzing || !analyzeUrl.trim() ? "rgba(255,255,255,0.06)" : "#5b5bd6", color: analyzing || !analyzeUrl.trim() ? "rgba(240,238,235,0.25)" : "#fff", fontSize: "13px", fontWeight: 500, cursor: analyzing || !analyzeUrl.trim() ? "not-allowed" : "pointer", fontFamily: "inherit", whiteSpace: "nowrap" }}>
                            <Sparkles size={13} />
                            {analyzing ? "Analyzing…" : "Analyze (1 credit)"}
                          </button>
                        </div>
                        {analyzeErr && <p style={{ fontSize: "12px", color: "#f87171" }}>{analyzeErr}</p>}
                        {analyzing && (
                          <div style={{ display: "flex", alignItems: "center", gap: "10px", padding: "14px 16px", borderRadius: "8px", border: "1px solid rgba(129,140,248,0.12)", background: "rgba(129,140,248,0.04)" }}>
                            <Loader2 size={14} style={{ color: "#818cf8", animation: "spin 1s linear infinite", flexShrink: 0 }} />
                            <span style={{ fontSize: "13px", color: "rgba(240,238,235,0.5)" }}>Fetching website and analyzing with AI…</span>
                          </div>
                        )}
                        {analyzeResult && (
                          <div ref={analyzeResultRef} style={{ borderRadius: "8px", border: "1px solid rgba(129,140,248,0.15)", background: "rgba(129,140,248,0.04)", padding: "16px" }}>
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
                              <span style={{ fontSize: "12px", fontWeight: 600, color: "#818cf8" }}>Website Analysis</span>
                              <CopyButton text={analyzeResult} />
                            </div>
                            <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.75)", margin: 0, lineHeight: 1.7, whiteSpace: "pre-wrap" }}>{analyzeResult}</p>
                          </div>
                        )}
                      </div>
                    )}

                    {activePanel === "message" && (
                      <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                        <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.5)", margin: 0, lineHeight: 1.6 }}>
                          AI writes a personalized outreach message based on this prospect&apos;s profile.
                        </p>
                        <div style={{ display: "flex", gap: "6px" }}>
                          {["EMAIL", "LINKEDIN", "SMS"].map((ch) => (
                            <button key={ch} onClick={() => setMsgChannel(ch)}
                              style={{ padding: "6px 14px", borderRadius: "6px", border: `1px solid ${msgChannel === ch ? "rgba(91,91,214,0.4)" : "rgba(255,255,255,0.09)"}`, background: msgChannel === ch ? "rgba(91,91,214,0.12)" : "rgba(255,255,255,0.03)", color: msgChannel === ch ? "#f0eeeb" : "rgba(240,238,235,0.4)", fontSize: "12px", fontWeight: msgChannel === ch ? 600 : 400, cursor: "pointer", fontFamily: "inherit" }}>
                              {ch === "EMAIL" ? "Email" : ch === "LINKEDIN" ? "LinkedIn" : "SMS"}
                            </button>
                          ))}
                        </div>
                        {msgChannel === "LINKEDIN" && (
                          <div style={{ padding: "10px 12px", borderRadius: "7px", background: "rgba(245,179,25,0.06)", border: "1px solid rgba(245,179,25,0.15)", fontSize: "12px", color: "rgba(245,179,25,0.8)" }}>
                            Copy the generated message and send it manually via LinkedIn.
                          </div>
                        )}
                        <div style={{ display: "flex", flexDirection: "column", gap: "5px" }}>
                          <label style={{ fontSize: "11px", fontWeight: 600, color: "rgba(240,238,235,0.35)", letterSpacing: "0.05em", textTransform: "uppercase" }}>Additional context (optional)</label>
                          <textarea value={msgNote} onChange={(e) => setMsgNote(e.target.value)} placeholder="e.g. I noticed they recently launched a new product..." rows={3}
                            style={{ padding: "8px 10px", borderRadius: "6px", border: "1px solid rgba(255,255,255,0.09)", background: "rgba(255,255,255,0.04)", color: "#f0eeeb", fontSize: "13px", fontFamily: "inherit", outline: "none", resize: "vertical" }} />
                        </div>
                        <button onClick={handleDraft} disabled={drafting}
                          style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "6px", padding: "9px", borderRadius: "7px", border: "none", background: drafting ? "rgba(255,255,255,0.06)" : "#5b5bd6", color: drafting ? "rgba(240,238,235,0.25)" : "#fff", fontSize: "13px", fontWeight: 500, cursor: drafting ? "not-allowed" : "pointer", fontFamily: "inherit" }}>
                          <Sparkles size={13} />
                          {drafting ? "Drafting…" : `Draft ${msgChannel === "EMAIL" ? "Email" : msgChannel === "LINKEDIN" ? "LinkedIn Message" : "SMS"} (1 credit)`}
                        </button>
                        {msgErr && <p style={{ fontSize: "12px", color: "#f87171", margin: 0 }}>{msgErr}</p>}
                        {drafting && (
                          <div style={{ display: "flex", alignItems: "center", gap: "10px", padding: "14px 16px", borderRadius: "8px", border: "1px solid rgba(52,211,153,0.12)", background: "rgba(52,211,153,0.04)" }}>
                            <Loader2 size={14} style={{ color: "#34d399", animation: "spin 1s linear infinite", flexShrink: 0 }} />
                            <span style={{ fontSize: "13px", color: "rgba(240,238,235,0.5)" }}>Writing personalized message…</span>
                          </div>
                        )}
                        {msgResult && (
                          <div ref={msgResultRef} style={{ borderRadius: "8px", border: "1px solid rgba(52,211,153,0.15)", background: "rgba(52,211,153,0.04)", padding: "16px" }}>
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
                              <span style={{ fontSize: "12px", fontWeight: 600, color: "#34d399" }}>{msgChannel === "EMAIL" ? "Email Draft" : msgChannel === "LINKEDIN" ? "LinkedIn Message" : "SMS Draft"}</span>
                              <CopyButton text={msgResult} />
                            </div>
                            <p style={{ fontSize: "13px", color: "rgba(240,238,235,0.8)", margin: 0, lineHeight: 1.7, whiteSpace: "pre-wrap" }}>{msgResult}</p>
                            {msgChannel === "EMAIL" && selectedProspect.email && (
                              <a href={`mailto:${selectedProspect.email}?body=${encodeURIComponent(msgResult)}`}
                                style={{ display: "inline-flex", alignItems: "center", gap: "5px", marginTop: "12px", padding: "6px 12px", borderRadius: "5px", border: "1px solid rgba(52,211,153,0.2)", background: "rgba(52,211,153,0.06)", color: "#34d399", fontSize: "12px", textDecoration: "none" }}>
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
        </>
      )}

      {/* Add/Edit Modal */}
      {showModal && (
        <div style={{ position: "fixed", inset: 0, zIndex: 50, background: "rgba(0,0,0,0.6)", backdropFilter: "blur(4px)", display: "flex", alignItems: "center", justifyContent: "center", padding: "20px" }}
          onClick={(e) => { if (e.target === e.currentTarget) closeModal(); }}>
          <div style={{ background: "#15151a", borderRadius: "14px", border: "1px solid rgba(255,255,255,0.09)", width: "100%", maxWidth: "560px", maxHeight: "90vh", display: "flex", flexDirection: "column", overflow: "hidden" }}>
            <div style={{ padding: "18px 22px", borderBottom: "1px solid rgba(255,255,255,0.06)", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
              <h2 style={{ fontSize: "15px", fontWeight: 600, color: "#f0eeeb", margin: 0 }}>{editProspect ? "Edit Prospect" : "Add Prospect"}</h2>
              <button onClick={closeModal} style={{ background: "none", border: "none", cursor: "pointer", color: "rgba(240,238,235,0.4)", padding: "4px" }}><X size={16} /></button>
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
                <textarea value={form.notes} onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))} placeholder="Any additional context..." rows={3}
                  style={{ padding: "8px 10px", borderRadius: "6px", border: "1px solid rgba(255,255,255,0.09)", background: "rgba(255,255,255,0.04)", color: "#f0eeeb", fontSize: "13px", fontFamily: "inherit", outline: "none", resize: "vertical" }} />
              </div>
              {formError && <p style={{ fontSize: "12px", color: "#f87171", margin: 0 }}>{formError}</p>}
            </div>
            <div style={{ padding: "14px 22px", borderTop: "1px solid rgba(255,255,255,0.06)", display: "flex", justifyContent: "flex-end", gap: "8px" }}>
              <button onClick={closeModal} style={{ padding: "7px 14px", borderRadius: "6px", border: "1px solid rgba(255,255,255,0.09)", background: "rgba(255,255,255,0.04)", color: "rgba(240,238,235,0.5)", fontSize: "13px", cursor: "pointer", fontFamily: "inherit" }}>Cancel</button>
              <button onClick={submitForm} disabled={!form.firstName.trim() || formBusy}
                style={{ padding: "7px 16px", borderRadius: "6px", border: "none", background: !form.firstName.trim() || formBusy ? "rgba(255,255,255,0.06)" : "#5b5bd6", color: !form.firstName.trim() || formBusy ? "rgba(240,238,235,0.25)" : "#fff", fontSize: "13px", fontWeight: 500, cursor: !form.firstName.trim() || formBusy ? "not-allowed" : "pointer", fontFamily: "inherit" }}>
                {formBusy ? "Saving…" : editProspect ? "Save Changes" : "Add Prospect"}
              </button>
            </div>
          </div>
        </div>
      )}

      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
