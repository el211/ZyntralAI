"use client";

import Link from "next/link";
import Image from "next/image";
import { ArrowRight, Zap } from "lucide-react";

export default function LandingPage() {
  return (
    <div style={{
      background: "#000",
      minHeight: "100vh",
      display: "flex",
      flexDirection: "column",
      fontFamily: "'Inter', system-ui, sans-serif",
      color: "#e8e8e6",
    }}>

      {/* ── NAV ── */}
      <header style={{
        display: "flex",
        alignItems: "center",
        padding: "0 40px",
        height: "58px",
        borderBottom: "1px solid rgba(255,255,255,0.06)",
        position: "sticky",
        top: 0,
        zIndex: 50,
        background: "rgba(0,0,0,0.85)",
        backdropFilter: "blur(12px)",
      }}>
        {/* Logo */}
        <div style={{ display: "flex", alignItems: "center", gap: "8px", marginRight: "40px", flexShrink: 0 }}>
          <div style={{
            width: "24px", height: "24px", borderRadius: "6px",
            background: "linear-gradient(135deg, #5b5bd6 0%, #7c3aed 100%)",
            display: "flex", alignItems: "center", justifyContent: "center",
          }}>
            <Zap size={12} color="#fff" fill="#fff" />
          </div>
          <span style={{ fontWeight: 600, fontSize: "14px", letterSpacing: "-0.01em", color: "#e8e8e6" }}>
            Zyntral AI
          </span>
        </div>

        {/* Center nav links */}
        <nav style={{ display: "flex", alignItems: "center", gap: "2px", flex: 1 }}>
          {["Features", "Pricing", "Blog", "Changelog"].map((label) => (
            <span key={label} style={{
              fontSize: "13px",
              color: "rgba(232,232,230,0.5)",
              padding: "5px 10px",
              borderRadius: "5px",
              cursor: "default",
              userSelect: "none",
            }}>
              {label}
            </span>
          ))}
        </nav>

        {/* Right actions */}
        <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
          <Link href="/login" style={{
            fontSize: "13px", color: "rgba(232,232,230,0.55)",
            textDecoration: "none", padding: "6px 12px", borderRadius: "6px",
          }}>
            Log in
          </Link>
          <Link href="/register" style={{
            fontSize: "13px", fontWeight: 500, color: "#fff",
            textDecoration: "none", padding: "6px 14px", borderRadius: "6px",
            background: "rgba(255,255,255,0.12)",
            border: "1px solid rgba(255,255,255,0.14)",
          }}>
            Sign up
          </Link>
        </div>
      </header>

      {/* ── HERO ── */}
      <section style={{
        padding: "100px 40px 48px",
        maxWidth: "1100px",
        width: "100%",
        margin: "0 auto",
        boxSizing: "border-box",
      }}>
        {/* Headline row: text left, "New" pill right */}
        <div style={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", gap: "40px" }}>
          <div style={{ maxWidth: "640px" }}>
            <h1 style={{
              fontSize: "clamp(40px, 4.5vw, 60px)",
              fontWeight: 700,
              lineHeight: 1.12,
              letterSpacing: "-0.03em",
              color: "#e8e8e6",
              margin: "0 0 20px",
            }}>
              The AI content platform<br />for creators and teams
            </h1>
            <p style={{
              fontSize: "15px",
              lineHeight: 1.6,
              color: "rgba(232,232,230,0.45)",
              margin: 0,
              maxWidth: "420px",
            }}>
              Purpose-built for social media automation. Designed for the AI era.
            </p>
          </div>

          {/* "New" pill — Linear style */}
          <div style={{
            flexShrink: 0,
            display: "flex",
            alignItems: "center",
            gap: "8px",
            fontSize: "13px",
            color: "rgba(232,232,230,0.55)",
            whiteSpace: "nowrap",
          }}>
            <span style={{
              background: "rgba(99,102,241,0.15)",
              border: "1px solid rgba(99,102,241,0.25)",
              borderRadius: "4px",
              padding: "2px 8px",
              fontSize: "11px",
              fontWeight: 600,
              color: "#818cf8",
              letterSpacing: "0.02em",
            }}>
              New
            </span>
            <Link href="/register" style={{
              color: "rgba(232,232,230,0.55)",
              textDecoration: "none",
              display: "flex", alignItems: "center", gap: "4px",
            }}>
              GitHub → LinkedIn <ArrowRight size={12} />
            </Link>
          </div>
        </div>

        {/* CTAs */}
        <div style={{ display: "flex", gap: "10px", alignItems: "center", marginTop: "36px" }}>
          <Link href="/register" style={{
            display: "inline-flex", alignItems: "center", gap: "6px",
            padding: "9px 18px", background: "#5b5bd6", color: "#fff",
            borderRadius: "7px", textDecoration: "none", fontWeight: 500,
            fontSize: "13px",
          }}>
            Start for free <ArrowRight size={13} />
          </Link>
          <Link href="/login" style={{
            display: "inline-flex", alignItems: "center",
            padding: "9px 18px",
            background: "transparent",
            color: "rgba(232,232,230,0.55)",
            borderRadius: "7px", textDecoration: "none",
            fontSize: "13px",
            border: "1px solid rgba(255,255,255,0.1)",
          }}>
            Sign in
          </Link>
        </div>
      </section>

      {/* ── PRODUCT SCREENSHOT ── */}
      <section style={{
        padding: "0 40px 0",
        maxWidth: "1100px",
        width: "100%",
        margin: "0 auto",
        boxSizing: "border-box",
        flex: 1,
      }}>
        <div style={{
          borderRadius: "10px 10px 0 0",
          border: "1px solid rgba(255,255,255,0.1)",
          borderBottom: "none",
          overflow: "hidden",
          boxShadow: "0 -4px 60px rgba(91,91,214,0.08), 0 -1px 0 rgba(255,255,255,0.04)",
          background: "#0f0f11",
        }}>
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
    </div>
  );
}
