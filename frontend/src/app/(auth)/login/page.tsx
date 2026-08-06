"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { apiErrorMessage } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Zap, ArrowRight, TrendingUp, Shield, Sparkles } from "lucide-react";

const features = [
  { icon: Sparkles, text: "AI-powered content generation" },
  { icon: TrendingUp, text: "Multi-platform scheduling & analytics" },
  { icon: Shield, text: "Enterprise-grade security" },
];

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
      router.push("/dashboard");
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen dark">
      {/* ── Left decorative panel ── */}
      <div className="relative hidden lg:flex lg:w-[46%] shrink-0 flex-col justify-between p-12 overflow-hidden"
        style={{ background: "hsl(230, 22%, 4%)" }}>
        {/* Grid */}
        <div className="absolute inset-0 auth-grid-bg" />
        {/* Glow */}
        <div className="absolute inset-0 auth-glow" />
        {/* Corner accent lines */}
        <div className="absolute top-0 right-0 w-px h-full opacity-20"
          style={{ background: "linear-gradient(to bottom, transparent, #F5B319, transparent)" }} />

        {/* Logo */}
        <div className="relative z-10 animate-fade-in">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl"
              style={{ background: "#F5B319" }}>
              <Zap className="h-4.5 w-4.5" style={{ color: "hsl(230,22%,5%)" }} />
            </div>
            <span className="font-syne font-bold text-xl tracking-tight text-white">Zyntral AI</span>
          </div>
        </div>

        {/* Center content */}
        <div className="relative z-10 space-y-10">
          <div className="space-y-4 animate-fade-in-up animate-delay-100">
            <p className="text-xs font-semibold tracking-widest uppercase"
              style={{ color: "#F5B319" }}>Trusted by 500+ teams</p>
            <h2 className="font-syne text-3xl font-bold leading-tight text-white">
              Your AI marketing<br />command centre.
            </h2>
            <p className="text-sm leading-relaxed" style={{ color: "rgba(255,255,255,0.45)" }}>
              Generate, schedule, and publish across every platform — powered by the latest AI models.
            </p>
          </div>

          <ul className="space-y-3 animate-fade-in-up animate-delay-200">
            {features.map(({ icon: Icon, text }) => (
              <li key={text} className="flex items-center gap-3">
                <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg"
                  style={{ background: "rgba(245,179,25,0.12)", border: "1px solid rgba(245,179,25,0.2)" }}>
                  <Icon className="h-3.5 w-3.5" style={{ color: "#F5B319" }} />
                </div>
                <span className="text-sm" style={{ color: "rgba(255,255,255,0.7)" }}>{text}</span>
              </li>
            ))}
          </ul>

          <div className="rounded-xl p-5 animate-fade-in-up animate-delay-300"
            style={{ background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.08)" }}>
            <p className="text-sm italic leading-relaxed" style={{ color: "rgba(255,255,255,0.7)" }}>
              &ldquo;Zyntral tripled our content output in the first week. It&apos;s become indispensable.&rdquo;
            </p>
            <div className="mt-3 flex items-center gap-2.5">
              <div className="flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold"
                style={{ background: "rgba(245,179,25,0.2)", color: "#F5B319" }}>S</div>
              <div>
                <p className="text-xs font-medium text-white">Sarah K.</p>
                <p className="text-xs" style={{ color: "rgba(255,255,255,0.35)" }}>Head of Marketing, Nexus Inc.</p>
              </div>
            </div>
          </div>
        </div>

        {/* Stats row */}
        <div className="relative z-10 grid grid-cols-3 gap-3 animate-fade-in-up animate-delay-400">
          {[["10×", "Faster output"], ["98%", "Uptime SLA"], ["500+", "Active teams"]].map(([v, l]) => (
            <div key={l} className="rounded-xl p-3 text-center"
              style={{ background: "rgba(255,255,255,0.03)", border: "1px solid rgba(255,255,255,0.07)" }}>
              <div className="font-syne text-xl font-bold" style={{ color: "#F5B319" }}>{v}</div>
              <div className="text-xs mt-0.5" style={{ color: "rgba(255,255,255,0.35)" }}>{l}</div>
            </div>
          ))}
        </div>
      </div>

      {/* ── Right form panel ── */}
      <div className="flex flex-1 flex-col items-center justify-center px-6 py-12"
        style={{ background: "hsl(230, 22%, 5%)" }}>
        {/* Mobile logo */}
        <div className="mb-10 flex items-center gap-2.5 lg:hidden animate-fade-in">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl"
            style={{ background: "#F5B319" }}>
            <Zap className="h-4 w-4" style={{ color: "hsl(230,22%,5%)" }} />
          </div>
          <span className="font-syne font-bold text-xl tracking-tight text-white">Zyntral AI</span>
        </div>

        <div className="w-full max-w-[400px] space-y-8">
          <div className="space-y-2 animate-fade-in-up">
            <h1 className="font-syne text-[2rem] font-bold tracking-tight text-white">
              Welcome back
            </h1>
            <p className="text-sm" style={{ color: "rgba(255,255,255,0.45)" }}>
              Sign in to continue to your workspace
            </p>
          </div>

          <form onSubmit={onSubmit} className="space-y-5 animate-fade-in-up animate-delay-100">
            <div className="space-y-2">
              <Label htmlFor="email" className="text-sm font-medium text-white/80">
                Email address
              </Label>
              <Input
                id="email" type="email" required
                placeholder="you@company.com"
                value={email} onChange={(e) => setEmail(e.target.value)}
                className="h-11 bg-white/5 border-white/10 text-white placeholder:text-white/25 focus-visible:ring-primary focus-visible:border-primary/50"
              />
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="password" className="text-sm font-medium text-white/80">
                  Password
                </Label>
                <Link href="/forgot-password"
                  className="text-xs font-medium transition-colors hover:text-white"
                  style={{ color: "#F5B319" }}>
                  Forgot password?
                </Link>
              </div>
              <Input
                id="password" type="password" required
                value={password} onChange={(e) => setPassword(e.target.value)}
                className="h-11 bg-white/5 border-white/10 text-white placeholder:text-white/25 focus-visible:ring-primary focus-visible:border-primary/50"
              />
            </div>

            {error && (
              <div className="rounded-lg px-4 py-3 text-sm"
                style={{ background: "rgba(239,68,68,0.1)", border: "1px solid rgba(239,68,68,0.2)", color: "#FCA5A5" }}>
                {error}
              </div>
            )}

            <Button
              type="submit"
              className="w-full h-11 font-semibold text-sm tracking-wide gap-2"
              style={{ background: "#F5B319", color: "hsl(230,22%,5%)" }}
              disabled={loading}>
              {loading ? "Signing in…" : (
                <><span>Sign in</span><ArrowRight className="h-4 w-4" /></>
              )}
            </Button>
          </form>

          <p className="text-center text-sm animate-fade-in-up animate-delay-200"
            style={{ color: "rgba(255,255,255,0.4)" }}>
            Don&apos;t have an account?{" "}
            <Link href="/register" className="font-medium text-white hover:text-primary transition-colors">
              Create one free
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
