"use client";

import Link from "next/link";
import Image from "next/image";
import { ArrowRight, Zap, Sparkles, Share2, CalendarDays, BarChart3, Check } from "lucide-react";

export default function LandingPage() {
  return (
    <div style={{
      background: "#080808",
      minHeight: "100vh",
      display: "flex",
      flexDirection: "column",
      fontFamily: "'Inter', system-ui, sans-serif",
      color: "#e8e8e6",
      overflowX: "hidden",
    }}>

      {/* ── NAV ── */}
      <header style={{
        display: "flex",
        alignItems: "center",
        padding: "0 48px",
        height: "56px",
        borderBottom: "1px solid rgba(255,255,255,0.06)",
        position: "sticky",
        top: 0,
        zIndex: 50,
        background: "rgba(8,8,8,0.9)",
        backdropFilter: "blur(16px)",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: "8px", flexShrink: 0 }}>
          <div style={{
            width: "22px", height: "22px", borderRadius: "5px",
            background: "linear-gradient(135deg, #5b5bd6 0%, #7c3aed 100%)",
            display: "flex", alignItems: "center", justifyContent: "center",
          }}>
            <Zap size={11} color="#fff" fill="#fff" />
          </div>
          <span style={{ fontWeight: 600, fontSize: "14px", letterSpacing: "-0.01em", color: "#e8e8e6" }}>
            Zyntral AI
          </span>
        </div>

        <nav style={{ display: "flex", alignItems: "center", gap: "0", flex: 1, marginLeft: "48px" }}>
          {["Features", "Pricing", "Blog"].map((label) => (
            <span key={label} style={{
              fontSize: "13px",
              color: "rgba(232,232,230,0.45)",
              padding: "5px 12px",
              borderRadius: "5px",
              cursor: "default",
              userSelect: "none",
            }}>
              {label}
            </span>
          ))}
        </nav>

        <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
          <Link href="/login" style={{
            fontSize: "13px", color: "rgba(232,232,230,0.5)",
            textDecoration: "none", padding: "6px 14px", borderRadius: "6px",
          }}>
            Log in
          </Link>
          <Link href="/register" style={{
            fontSize: "13px", fontWeight: 500, color: "#fff",
            textDecoration: "none", padding: "6px 16px", borderRadius: "6px",
            background: "#5b5bd6",
          }}>
            Get started
          </Link>
        </div>
      </header>

      {/* ── HERO ── */}
      <section style={{
        padding: "96px 48px 0",
        maxWidth: "1100px",
        width: "100%",
        margin: "0 auto",
        boxSizing: "border-box",
        textAlign: "center",
      }}>
        {/* Badge */}
        <div style={{ display: "inline-flex", alignItems: "center", gap: "8px", marginBottom: "28px" }}>
          <span style={{
            display: "inline-flex", alignItems: "center", gap: "6px",
            background: "rgba(91,91,214,0.12)",
            border: "1px solid rgba(91,91,214,0.25)",
            borderRadius: "20px",
            padding: "4px 12px",
            fontSize: "12px",
            fontWeight: 500,
            color: "#818cf8",
            letterSpacing: "0.01em",
          }}>
            <Sparkles size={11} />
            AI-powered social media automation
          </span>
        </div>

        <h1 style={{
          fontSize: "clamp(40px, 5vw, 68px)",
          fontWeight: 700,
          lineHeight: 1.08,
          letterSpacing: "-0.04em",
          color: "#f0eeeb",
          margin: "0 auto 24px",
          maxWidth: "720px",
        }}>
          Create. Schedule.{" "}
          <span style={{ color: "rgba(240,238,235,0.35)" }}>Grow.</span>
        </h1>

        <p style={{
          fontSize: "17px",
          lineHeight: 1.65,
          color: "rgba(232,232,230,0.45)",
          margin: "0 auto 40px",
          maxWidth: "460px",
        }}>
          Generate, schedule, and publish content across every platform — powered by AI, built for teams.
        </p>

        <div style={{ display: "flex", gap: "10px", alignItems: "center", justifyContent: "center", marginBottom: "80px" }}>
          <Link href="/register" style={{
            display: "inline-flex", alignItems: "center", gap: "7px",
            padding: "11px 22px", background: "#5b5bd6", color: "#fff",
            borderRadius: "8px", textDecoration: "none", fontWeight: 500,
            fontSize: "14px", letterSpacing: "-0.01em",
          }}>
            Start for free <ArrowRight size={14} />
          </Link>
          <Link href="/login" style={{
            display: "inline-flex", alignItems: "center",
            padding: "11px 22px",
            background: "rgba(255,255,255,0.05)",
            color: "rgba(232,232,230,0.65)",
            borderRadius: "8px", textDecoration: "none",
            fontSize: "14px",
            border: "1px solid rgba(255,255,255,0.08)",
          }}>
            Sign in
          </Link>
        </div>
      </section>

      {/* ── PRODUCT SCREENSHOT ── */}
      <section style={{
        padding: "0 32px",
        maxWidth: "1200px",
        width: "100%",
        margin: "0 auto",
        boxSizing: "border-box",
      }}>
        <div style={{
          borderRadius: "12px 12px 0 0",
          border: "1px solid rgba(255,255,255,0.09)",
          borderBottom: "none",
          overflow: "hidden",
          boxShadow: "0 -8px 80px rgba(91,91,214,0.12), 0 0 0 1px rgba(91,91,214,0.06)",
          background: "#0d0d10",
          position: "relative",
        }}>
          {/* Browser chrome */}
          <div style={{
            height: "36px",
            background: "#111114",
            borderBottom: "1px solid rgba(255,255,255,0.06)",
            display: "flex",
            alignItems: "center",
            padding: "0 14px",
            gap: "6px",
            flexShrink: 0,
          }}>
            {["#EF4444", "#F5B319", "#34d399"].map((c) => (
              <div key={c} style={{ width: "10px", height: "10px", borderRadius: "50%", background: c, opacity: 0.7 }} />
            ))}
            <div style={{
              flex: 1, marginLeft: "12px",
              background: "rgba(255,255,255,0.04)",
              borderRadius: "5px", height: "20px",
              maxWidth: "240px",
              display: "flex", alignItems: "center", paddingLeft: "10px",
            }}>
              <span style={{ fontSize: "11px", color: "rgba(255,255,255,0.2)" }}>app.zyntralai.com</span>
            </div>
          </div>
          <Image
            src="/screenshots/dashboard-preview.png"
            alt="Zyntral AI dashboard"
            width={1200}
            height={750}
            style={{ width: "100%", height: "auto", display: "block" }}
            priority
          />
        </div>
      </section>

      {/* ── FEATURES ── */}
      <section style={{
        padding: "120px 48px",
        maxWidth: "1100px",
        width: "100%",
        margin: "0 auto",
        boxSizing: "border-box",
      }}>
        <div style={{ textAlign: "center", marginBottom: "64px" }}>
          <p style={{ fontSize: "12px", fontWeight: 600, letterSpacing: "0.08em", color: "rgba(232,232,230,0.3)", textTransform: "uppercase", marginBottom: "12px" }}>
            Everything you need
          </p>
          <h2 style={{ fontSize: "clamp(28px, 3vw, 40px)", fontWeight: 700, letterSpacing: "-0.03em", color: "#f0eeeb", margin: 0 }}>
            Built for the AI era
          </h2>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "2px" }}>
          {[
            {
              icon: <Sparkles size={18} style={{ color: "#F5B319" }} />,
              iconBg: "rgba(245,179,25,0.1)",
              title: "AI Content Generation",
              desc: "Generate platform-optimized posts, captions, and threads in seconds using state-of-the-art language models.",
            },
            {
              icon: <CalendarDays size={18} style={{ color: "#818cf8" }} />,
              iconBg: "rgba(129,140,248,0.1)",
              title: "Smart Scheduling",
              desc: "Plan and schedule content across LinkedIn, Twitter, TikTok, and more from a single calendar view.",
            },
            {
              icon: <Share2 size={18} style={{ color: "#34d399" }} />,
              iconBg: "rgba(52,211,153,0.1)",
              title: "Multi-Platform Publishing",
              desc: "Connect your accounts once and publish everywhere simultaneously — no copy-pasting required.",
            },
            {
              icon: <BarChart3 size={18} style={{ color: "#f87171" }} />,
              iconBg: "rgba(248,113,113,0.1)",
              title: "Performance Analytics",
              desc: "Track engagement, reach, and growth across all connected platforms in one unified dashboard.",
            },
            {
              icon: <Zap size={18} style={{ color: "#a78bfa" }} />,
              iconBg: "rgba(167,139,250,0.1)",
              title: "AI Credits System",
              desc: "Flexible usage-based credits that scale with your team. Start free, upgrade when you need more.",
            },
            {
              icon: <Check size={18} style={{ color: "#5b5bd6" }} />,
              iconBg: "rgba(91,91,214,0.1)",
              title: "Team Workspaces",
              desc: "Collaborate with your team in shared workspaces with role-based access control and approval flows.",
            },
          ].map(({ icon, iconBg, title, desc }) => (
            <div key={title} style={{
              padding: "32px",
              border: "1px solid rgba(255,255,255,0.05)",
              background: "rgba(255,255,255,0.01)",
            }}>
              <div style={{
                width: "36px", height: "36px", borderRadius: "9px",
                background: iconBg,
                display: "flex", alignItems: "center", justifyContent: "center",
                marginBottom: "18px",
              }}>
                {icon}
              </div>
              <h3 style={{ fontSize: "14px", fontWeight: 600, color: "#f0eeeb", margin: "0 0 8px", letterSpacing: "-0.01em" }}>
                {title}
              </h3>
              <p style={{ fontSize: "13px", lineHeight: 1.65, color: "rgba(232,232,230,0.4)", margin: 0 }}>
                {desc}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* ── PRICING TEASER ── */}
      <section style={{
        padding: "0 48px 120px",
        maxWidth: "1100px",
        width: "100%",
        margin: "0 auto",
        boxSizing: "border-box",
      }}>
        <div style={{
          borderRadius: "14px",
          border: "1px solid rgba(91,91,214,0.2)",
          background: "linear-gradient(135deg, rgba(91,91,214,0.06) 0%, rgba(124,58,237,0.04) 100%)",
          padding: "64px",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          gap: "40px",
        }}>
          <div>
            <h2 style={{ fontSize: "28px", fontWeight: 700, letterSpacing: "-0.03em", color: "#f0eeeb", margin: "0 0 12px" }}>
              Start free. Scale when ready.
            </h2>
            <p style={{ fontSize: "14px", color: "rgba(232,232,230,0.45)", margin: "0 0 28px", lineHeight: 1.6 }}>
              50 AI credits per month, forever free. No credit card required.
            </p>
            <div style={{ display: "flex", gap: "20px", flexWrap: "wrap" }}>
              {["50 AI credits / mo", "2 social accounts", "1 workspace"].map((item) => (
                <div key={item} style={{ display: "flex", alignItems: "center", gap: "7px", fontSize: "13px", color: "rgba(232,232,230,0.55)" }}>
                  <Check size={13} style={{ color: "#5b5bd6" }} />
                  {item}
                </div>
              ))}
            </div>
          </div>
          <div style={{ flexShrink: 0 }}>
            <Link href="/register" style={{
              display: "inline-flex", alignItems: "center", gap: "7px",
              padding: "12px 24px", background: "#5b5bd6", color: "#fff",
              borderRadius: "8px", textDecoration: "none", fontWeight: 500,
              fontSize: "14px", whiteSpace: "nowrap",
            }}>
              Get started free <ArrowRight size={14} />
            </Link>
          </div>
        </div>
      </section>

      {/* ── FOOTER ── */}
      <footer style={{
        borderTop: "1px solid rgba(255,255,255,0.05)",
        padding: "32px 48px",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        flexWrap: "wrap",
        gap: "16px",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          <div style={{
            width: "18px", height: "18px", borderRadius: "4px",
            background: "linear-gradient(135deg, #5b5bd6 0%, #7c3aed 100%)",
            display: "flex", alignItems: "center", justifyContent: "center",
          }}>
            <Zap size={9} color="#fff" fill="#fff" />
          </div>
          <span style={{ fontSize: "13px", fontWeight: 500, color: "rgba(232,232,230,0.35)" }}>Zyntral AI</span>
          <span style={{ fontSize: "13px", color: "rgba(232,232,230,0.2)" }}>·</span>
          <span style={{ fontSize: "12px", color: "rgba(232,232,230,0.2)" }}>© 2026 OreoStudios</span>
        </div>
        <div style={{ display: "flex", gap: "24px" }}>
          {[["Terms", "/terms"], ["Privacy", "/privacy"], ["Log in", "/login"]].map(([label, href]) => (
            <Link key={label} href={href} style={{
              fontSize: "12px", color: "rgba(232,232,230,0.3)",
              textDecoration: "none",
            }}>
              {label}
            </Link>
          ))}
        </div>
      </footer>

    </div>
  );
}
