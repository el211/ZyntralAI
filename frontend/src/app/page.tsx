"use client";

import Link from "next/link";
import Image from "next/image";
import { ArrowRight, Sparkles, CalendarClock, Users, BarChart3, Github, Zap } from "lucide-react";

const features = [
  { icon: Sparkles,     label: "AI Generation",      desc: "Posts, captions & copy for every network" },
  { icon: CalendarClock,label: "Smart Scheduling",   desc: "Visual calendar with auto-publish" },
  { icon: Users,        label: "Team Workspaces",    desc: "Roles, permissions & collaboration" },
  { icon: BarChart3,    label: "Analytics",          desc: "Track reach, engagement & growth" },
  { icon: Github,       label: "GitHub Integration", desc: "Turn commits into LinkedIn posts" },
];

export default function LandingPage() {
  return (
    <div style={{
      background: "#0c0c0e",
      minHeight: "100vh",
      display: "flex",
      flexDirection: "column",
      fontFamily: "'Inter', system-ui, sans-serif",
      color: "#ededec",
      overflowX: "hidden",
    }}>

      {/* ── NAV ── */}
      <header style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "0 32px",
        height: "56px",
        borderBottom: "1px solid rgba(255,255,255,0.06)",
        position: "sticky",
        top: 0,
        zIndex: 50,
        background: "rgba(12,12,14,0.9)",
        backdropFilter: "blur(10px)",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          <div style={{
            width: "26px", height: "26px", borderRadius: "6px",
            background: "linear-gradient(135deg, #5b5bd6 0%, #7c3aed 100%)",
            display: "flex", alignItems: "center", justifyContent: "center",
          }}>
            <Zap size={13} color="#fff" fill="#fff" />
          </div>
          <span style={{ fontWeight: 600, fontSize: "14px", letterSpacing: "-0.01em" }}>Zyntral AI</span>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <Link href="/login" style={{
            fontSize: "13px", color: "rgba(237,237,236,0.55)",
            textDecoration: "none", padding: "6px 12px", borderRadius: "6px",
          }}>
            Sign in
          </Link>
          <Link href="/register" style={{
            fontSize: "13px", fontWeight: 500, color: "#fff",
            textDecoration: "none", padding: "6px 14px", borderRadius: "6px",
            background: "#5b5bd6",
            display: "flex", alignItems: "center", gap: "5px",
          }}>
            Get started <ArrowRight size={12} />
          </Link>
        </div>
      </header>

      {/* ── HERO ── */}
      <section style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        textAlign: "center",
        padding: "80px 24px 60px",
      }}>
        {/* Badge */}
        <div style={{
          display: "inline-flex", alignItems: "center", gap: "6px",
          fontSize: "12px", color: "rgba(237,237,236,0.45)",
          border: "1px solid rgba(255,255,255,0.09)", borderRadius: "999px",
          padding: "4px 12px", marginBottom: "28px",
        }}>
          <span style={{ width: "5px", height: "5px", borderRadius: "50%", background: "#6366f1", display: "inline-block" }} />
          AI marketing automation, done right
        </div>

        {/* Headline */}
        <h1 style={{
          fontSize: "clamp(38px, 5vw, 58px)",
          fontWeight: 700,
          lineHeight: 1.1,
          letterSpacing: "-0.03em",
          margin: "0 0 20px",
          maxWidth: "680px",
          color: "#ededec",
        }}>
          Create, schedule & publish content with{" "}
          <span style={{
            background: "linear-gradient(135deg, #818cf8 0%, #a78bfa 100%)",
            WebkitBackgroundClip: "text",
            WebkitTextFillColor: "transparent",
            backgroundClip: "text",
          }}>
            artificial intelligence
          </span>
        </h1>

        {/* Subtitle */}
        <p style={{
          fontSize: "16px",
          lineHeight: 1.65,
          color: "rgba(237,237,236,0.45)",
          maxWidth: "440px",
          margin: "0 0 36px",
        }}>
          Generate on-brand posts for every network, plan your calendar,
          collaborate with your team — all from one workspace.
        </p>

        {/* CTAs */}
        <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
          <Link href="/register" style={{
            display: "inline-flex", alignItems: "center", gap: "6px",
            padding: "10px 20px", background: "#5b5bd6", color: "#fff",
            borderRadius: "7px", textDecoration: "none", fontWeight: 500,
            fontSize: "14px",
            boxShadow: "0 0 20px rgba(91,91,214,0.35)",
          }}>
            Start free <ArrowRight size={14} />
          </Link>
          <Link href="/login" style={{
            display: "inline-flex", alignItems: "center",
            padding: "10px 20px",
            background: "rgba(255,255,255,0.05)",
            color: "rgba(237,237,236,0.65)",
            borderRadius: "7px", textDecoration: "none",
            fontSize: "14px",
            border: "1px solid rgba(255,255,255,0.08)",
          }}>
            Sign in
          </Link>
        </div>
      </section>

      {/* ── PRODUCT SCREENSHOT ── */}
      <section style={{
        padding: "0 24px 80px",
        maxWidth: "1100px",
        width: "100%",
        margin: "0 auto",
        position: "relative",
      }}>
        {/* Purple glow */}
        <div style={{
          position: "absolute",
          top: "30%", left: "50%",
          transform: "translate(-50%, -50%)",
          width: "600px", height: "300px",
          background: "radial-gradient(ellipse, rgba(91,91,214,0.15) 0%, transparent 70%)",
          filter: "blur(40px)",
          pointerEvents: "none",
          zIndex: 0,
        }} />

        {/* Browser frame */}
        <div style={{
          position: "relative", zIndex: 1,
          borderRadius: "10px",
          border: "1px solid rgba(255,255,255,0.1)",
          overflow: "hidden",
          boxShadow: "0 4px 6px rgba(0,0,0,0.3), 0 20px 60px rgba(0,0,0,0.5), 0 0 0 1px rgba(91,91,214,0.1)",
          background: "#111113",
        }}>
          {/* Browser chrome */}
          <div style={{
            display: "flex", alignItems: "center", gap: "6px",
            padding: "10px 14px",
            background: "#111113",
            borderBottom: "1px solid rgba(255,255,255,0.06)",
          }}>
            <span style={{ width: "10px", height: "10px", borderRadius: "50%", background: "#ff5f57", display: "inline-block" }} />
            <span style={{ width: "10px", height: "10px", borderRadius: "50%", background: "#febc2e", display: "inline-block" }} />
            <span style={{ width: "10px", height: "10px", borderRadius: "50%", background: "#28c840", display: "inline-block" }} />
            <div style={{
              marginLeft: "10px",
              background: "rgba(255,255,255,0.04)",
              borderRadius: "4px",
              padding: "3px 10px",
              fontSize: "11px",
              color: "rgba(237,237,236,0.25)",
              maxWidth: "200px",
            }}>
              app.zyntralai.com
            </div>
          </div>
          <Image
            src="/screenshots/app-1.png"
            alt="Zyntral AI dashboard"
            width={1100}
            height={640}
            style={{ width: "100%", height: "auto", display: "block" }}
            priority
          />
        </div>
      </section>

      {/* ── FEATURES ── */}
      <section style={{
        padding: "0 24px 100px",
        maxWidth: "1100px",
        width: "100%",
        margin: "0 auto",
      }}>
        <p style={{
          fontSize: "11px", fontWeight: 600, letterSpacing: "0.08em",
          textTransform: "uppercase", color: "rgba(237,237,236,0.25)",
          marginBottom: "24px", textAlign: "center",
        }}>
          Everything you need
        </p>

        <div style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
          gap: "1px",
          background: "rgba(255,255,255,0.06)",
          borderRadius: "10px",
          overflow: "hidden",
          border: "1px solid rgba(255,255,255,0.06)",
        }}>
          {features.map((f) => (
            <div key={f.label} style={{
              padding: "24px 20px",
              background: "#0c0c0e",
              display: "flex",
              flexDirection: "column",
              gap: "8px",
            }}>
              <f.icon size={16} color="#6366f1" />
              <div style={{ fontSize: "13px", fontWeight: 600, color: "#ededec" }}>{f.label}</div>
              <div style={{ fontSize: "12px", color: "rgba(237,237,236,0.4)", lineHeight: 1.5 }}>{f.desc}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ── FOOTER ── */}
      <footer style={{
        borderTop: "1px solid rgba(255,255,255,0.06)",
        padding: "20px 32px",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontSize: "12px",
        color: "rgba(237,237,236,0.2)",
        marginTop: "auto",
      }}>
        © {new Date().getFullYear()} Zyntral AI
      </footer>
    </div>
  );
}
