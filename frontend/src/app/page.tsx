import Link from "next/link";
import Image from "next/image";
import { ArrowRight, Sparkles, CalendarClock, Users, BarChart3, Github, Zap } from "lucide-react";

const features = [
  { icon: Sparkles, label: "AI Generation", desc: "Posts, captions & copy for every network" },
  { icon: CalendarClock, label: "Smart Scheduling", desc: "Visual calendar with auto-publish" },
  { icon: Users, label: "Team Workspaces", desc: "Roles, permissions & collaboration" },
  { icon: BarChart3, label: "Analytics", desc: "Track reach, engagement & growth" },
  { icon: Github, label: "GitHub Integration", desc: "Turn commits into LinkedIn posts" },
];

export default function LandingPage() {
  return (
    <div
      style={{
        background: "#080809",
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
        fontFamily: "'Inter', system-ui, sans-serif",
        color: "#f0eeeb",
        overflowX: "hidden",
      }}
    >
      {/* ── NAV ── */}
      <header
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "0 40px",
          height: "60px",
          borderBottom: "1px solid rgba(255,255,255,0.06)",
          position: "sticky",
          top: 0,
          zIndex: 50,
          background: "rgba(8,8,9,0.85)",
          backdropFilter: "blur(12px)",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          <div
            style={{
              width: "28px",
              height: "28px",
              borderRadius: "6px",
              background: "linear-gradient(135deg, #5b5bd6 0%, #7c3aed 100%)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <Zap size={15} color="#fff" fill="#fff" />
          </div>
          <span style={{ fontWeight: 600, fontSize: "15px", letterSpacing: "-0.01em" }}>
            Zyntral AI
          </span>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          <Link
            href="/login"
            style={{
              fontSize: "14px",
              color: "rgba(240,238,235,0.65)",
              textDecoration: "none",
              padding: "6px 14px",
              borderRadius: "6px",
              transition: "color 0.15s",
            }}
          >
            Sign in
          </Link>
          <Link
            href="/register"
            style={{
              fontSize: "14px",
              fontWeight: 500,
              color: "#fff",
              textDecoration: "none",
              padding: "6px 14px",
              borderRadius: "6px",
              background: "rgba(91,91,214,0.9)",
              border: "1px solid rgba(91,91,214,0.6)",
              display: "flex",
              alignItems: "center",
              gap: "5px",
              transition: "background 0.15s",
            }}
          >
            Get started <ArrowRight size={13} />
          </Link>
        </div>
      </header>

      {/* ── HERO ── */}
      <section
        style={{
          padding: "100px 64px 0",
          maxWidth: "1200px",
          width: "100%",
          margin: "0 auto",
        }}
      >
        {/* Badge */}
        <div
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "6px",
            fontSize: "12px",
            color: "rgba(240,238,235,0.55)",
            border: "1px solid rgba(255,255,255,0.1)",
            borderRadius: "999px",
            padding: "4px 12px",
            marginBottom: "32px",
            letterSpacing: "0.02em",
          }}
        >
          <span
            style={{
              width: "6px",
              height: "6px",
              borderRadius: "50%",
              background: "#5b5bd6",
              display: "inline-block",
            }}
          />
          AI marketing automation, done right
        </div>

        {/* Headline */}
        <h1
          style={{
            fontFamily: "'Syne', sans-serif",
            fontSize: "clamp(52px, 7vw, 88px)",
            fontWeight: 800,
            lineHeight: 1.0,
            letterSpacing: "-0.04em",
            margin: "0 0 28px",
            maxWidth: "800px",
            color: "#f0eeeb",
          }}
        >
          Create content<br />
          at the speed<br />
          <span
            style={{
              background: "linear-gradient(135deg, #818cf8 0%, #a78bfa 60%, #c4b5fd 100%)",
              WebkitBackgroundClip: "text",
              WebkitTextFillColor: "transparent",
              backgroundClip: "text",
            }}
          >
            of thought
          </span>
        </h1>

        {/* Subtitle */}
        <p
          style={{
            fontSize: "18px",
            lineHeight: 1.6,
            color: "rgba(240,238,235,0.5)",
            maxWidth: "480px",
            margin: "0 0 40px",
            fontWeight: 400,
          }}
        >
          Generate on-brand posts for every network, plan your calendar,
          collaborate with your team — all from one workspace.
        </p>

        {/* CTAs */}
        <div style={{ display: "flex", gap: "12px", alignItems: "center" }}>
          <Link
            href="/register"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "7px",
              padding: "11px 22px",
              background: "#5b5bd6",
              color: "#fff",
              borderRadius: "8px",
              textDecoration: "none",
              fontWeight: 500,
              fontSize: "15px",
              border: "1px solid rgba(139,92,246,0.4)",
              boxShadow: "0 0 24px rgba(91,91,214,0.3), inset 0 1px 0 rgba(255,255,255,0.1)",
            }}
          >
            Start free <ArrowRight size={15} />
          </Link>
          <Link
            href="/login"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "7px",
              padding: "11px 22px",
              background: "rgba(255,255,255,0.05)",
              color: "rgba(240,238,235,0.7)",
              borderRadius: "8px",
              textDecoration: "none",
              fontWeight: 500,
              fontSize: "15px",
              border: "1px solid rgba(255,255,255,0.08)",
            }}
          >
            Sign in
          </Link>
        </div>
      </section>

      {/* ── PRODUCT SCREENSHOT ── */}
      <section
        style={{
          padding: "72px 32px 0",
          maxWidth: "1200px",
          width: "100%",
          margin: "0 auto",
          position: "relative",
        }}
      >
        {/* Ambient glow behind screenshot */}
        <div
          style={{
            position: "absolute",
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -20%)",
            width: "80%",
            height: "400px",
            background: "radial-gradient(ellipse at center, rgba(91,91,214,0.18) 0%, rgba(124,58,237,0.08) 50%, transparent 80%)",
            filter: "blur(40px)",
            pointerEvents: "none",
            zIndex: 0,
          }}
        />

        {/* Browser frame */}
        <div
          style={{
            position: "relative",
            zIndex: 1,
            borderRadius: "12px",
            border: "1px solid rgba(255,255,255,0.1)",
            overflow: "hidden",
            boxShadow: "0 0 0 1px rgba(91,91,214,0.15), 0 32px 80px rgba(0,0,0,0.6), 0 0 120px rgba(91,91,214,0.08)",
            background: "#0f0f11",
          }}
        >
          {/* Browser chrome bar */}
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "6px",
              padding: "12px 16px",
              background: "#111113",
              borderBottom: "1px solid rgba(255,255,255,0.06)",
            }}
          >
            <span style={{ width: "10px", height: "10px", borderRadius: "50%", background: "#ff5f57", display: "inline-block" }} />
            <span style={{ width: "10px", height: "10px", borderRadius: "50%", background: "#febc2e", display: "inline-block" }} />
            <span style={{ width: "10px", height: "10px", borderRadius: "50%", background: "#28c840", display: "inline-block" }} />
            <div
              style={{
                marginLeft: "12px",
                flex: 1,
                background: "rgba(255,255,255,0.05)",
                borderRadius: "5px",
                padding: "4px 12px",
                fontSize: "12px",
                color: "rgba(240,238,235,0.3)",
                maxWidth: "280px",
              }}
            >
              app.zyntralai.com
            </div>
          </div>

          {/* Screenshot */}
          <Image
            src="/screenshots/app-1.png"
            alt="Zyntral AI dashboard"
            width={1200}
            height={700}
            style={{ width: "100%", height: "auto", display: "block" }}
            priority
          />
        </div>
      </section>

      {/* ── FEATURES ── */}
      <section
        style={{
          padding: "96px 64px",
          maxWidth: "1200px",
          width: "100%",
          margin: "0 auto",
        }}
      >
        <p
          style={{
            fontSize: "12px",
            fontWeight: 600,
            letterSpacing: "0.1em",
            textTransform: "uppercase",
            color: "rgba(240,238,235,0.3)",
            marginBottom: "40px",
          }}
        >
          Everything you need
        </p>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
            gap: "1px",
            background: "rgba(255,255,255,0.06)",
            borderRadius: "12px",
            overflow: "hidden",
            border: "1px solid rgba(255,255,255,0.06)",
          }}
        >
          {features.map((f) => (
            <div
              key={f.label}
              style={{
                padding: "28px 24px",
                background: "#080809",
                display: "flex",
                flexDirection: "column",
                gap: "10px",
                transition: "background 0.15s",
              }}
              onMouseEnter={(e) => {
                (e.currentTarget as HTMLDivElement).style.background = "#0f0f12";
              }}
              onMouseLeave={(e) => {
                (e.currentTarget as HTMLDivElement).style.background = "#080809";
              }}
            >
              <f.icon size={18} color="#6366f1" />
              <div>
                <div style={{ fontSize: "14px", fontWeight: 600, color: "#f0eeeb", marginBottom: "4px" }}>
                  {f.label}
                </div>
                <div style={{ fontSize: "13px", color: "rgba(240,238,235,0.4)", lineHeight: 1.5 }}>
                  {f.desc}
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ── FOOTER ── */}
      <footer
        style={{
          borderTop: "1px solid rgba(255,255,255,0.06)",
          padding: "24px 64px",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: "13px",
          color: "rgba(240,238,235,0.25)",
          marginTop: "auto",
        }}
      >
        © {new Date().getFullYear()} Zyntral AI
      </footer>
    </div>
  );
}
