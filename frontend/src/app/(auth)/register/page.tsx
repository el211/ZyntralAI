"use client";

import { useState } from "react";
import Link from "next/link";
import { api, apiErrorMessage } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Zap, ArrowRight, CheckCircle2, Mail } from "lucide-react";

export default function RegisterPage() {
  const [form, setForm] = useState({ fullName: "", email: "", password: "" });
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await api.post("/auth/register", form);
      setDone(true);
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
        <div className="absolute inset-0 auth-grid-bg" />
        <div className="absolute inset-0 auth-glow" />
        <div className="absolute top-0 right-0 w-px h-full opacity-20"
          style={{ background: "linear-gradient(to bottom, transparent, #F5B319, transparent)" }} />

        {/* Logo */}
        <div className="relative z-10 animate-fade-in">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl"
              style={{ background: "#F5B319" }}>
              <Zap className="h-4 w-4" style={{ color: "hsl(230,22%,5%)" }} />
            </div>
            <span className="font-syne font-bold text-xl tracking-tight text-white">Zyntral AI</span>
          </div>
        </div>

        {/* Steps */}
        <div className="relative z-10 space-y-8 animate-fade-in-up animate-delay-100">
          <div className="space-y-3">
            <p className="text-xs font-semibold tracking-widest uppercase"
              style={{ color: "#F5B319" }}>Get started in minutes</p>
            <h2 className="font-syne text-3xl font-bold leading-tight text-white">
              Create your workspace,<br />start generating.
            </h2>
          </div>
          <ol className="space-y-4">
            {[
              ["01", "Create your account", "Takes less than 60 seconds"],
              ["02", "Set up your workspace", "Invite your team and connect socials"],
              ["03", "Generate & publish", "Let AI handle the content creation"],
            ].map(([num, title, sub]) => (
              <li key={num} className="flex items-start gap-4">
                <span className="font-syne font-bold text-sm shrink-0 mt-0.5"
                  style={{ color: "rgba(245,179,25,0.5)" }}>{num}</span>
                <div>
                  <p className="text-sm font-medium text-white">{title}</p>
                  <p className="text-xs mt-0.5" style={{ color: "rgba(255,255,255,0.35)" }}>{sub}</p>
                </div>
              </li>
            ))}
          </ol>
        </div>

        <div className="relative z-10 rounded-xl p-5 animate-fade-in-up animate-delay-200"
          style={{ background: "rgba(255,255,255,0.03)", border: "1px solid rgba(255,255,255,0.07)" }}>
          <p className="text-xs" style={{ color: "rgba(255,255,255,0.35)" }}>
            Free plan includes 100 AI credits/month, 1 workspace, and 3 social accounts. No credit card required.
          </p>
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

        <div className="w-full max-w-[400px]">
          {done ? (
            <div className="space-y-6 text-center animate-fade-in-up">
              <div className="flex justify-center">
                <div className="flex h-16 w-16 items-center justify-center rounded-2xl"
                  style={{ background: "rgba(245,179,25,0.12)", border: "1px solid rgba(245,179,25,0.25)" }}>
                  <Mail className="h-7 w-7" style={{ color: "#F5B319" }} />
                </div>
              </div>
              <div className="space-y-2">
                <h1 className="font-syne text-2xl font-bold text-white">Check your inbox</h1>
                <p className="text-sm leading-relaxed" style={{ color: "rgba(255,255,255,0.45)" }}>
                  We sent a verification link to{" "}
                  <span className="font-medium text-white">{form.email}</span>.
                  Verify it to activate your workspace.
                </p>
              </div>
              <Link href="/login">
                <Button variant="outline"
                  className="border-white/10 text-white hover:bg-white/5 hover:text-white">
                  Back to sign in
                </Button>
              </Link>
            </div>
          ) : (
            <div className="space-y-8">
              <div className="space-y-2 animate-fade-in-up">
                <h1 className="font-syne text-[2rem] font-bold tracking-tight text-white">
                  Create account
                </h1>
                <p className="text-sm" style={{ color: "rgba(255,255,255,0.45)" }}>
                  Start automating your marketing today
                </p>
              </div>

              <form onSubmit={onSubmit} className="space-y-5 animate-fade-in-up animate-delay-100">
                <div className="space-y-2">
                  <Label htmlFor="fullName" className="text-sm font-medium text-white/80">Full name</Label>
                  <Input
                    id="fullName" required
                    placeholder="Alex Johnson"
                    value={form.fullName}
                    onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                    className="h-11 bg-white/5 border-white/10 text-white placeholder:text-white/25 focus-visible:ring-primary focus-visible:border-primary/50"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email" className="text-sm font-medium text-white/80">Email address</Label>
                  <Input
                    id="email" type="email" required
                    placeholder="you@company.com"
                    value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                    className="h-11 bg-white/5 border-white/10 text-white placeholder:text-white/25 focus-visible:ring-primary focus-visible:border-primary/50"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="password" className="text-sm font-medium text-white/80">Password</Label>
                  <Input
                    id="password" type="password" required minLength={8}
                    placeholder="Min. 8 characters"
                    value={form.password}
                    onChange={(e) => setForm({ ...form, password: e.target.value })}
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
                  {loading ? "Creating account…" : (
                    <><span>Create account</span><ArrowRight className="h-4 w-4" /></>
                  )}
                </Button>

                <p className="text-center text-xs" style={{ color: "rgba(255,255,255,0.3)" }}>
                  By creating an account you agree to our Terms of Service and Privacy Policy.
                </p>
              </form>

              <p className="text-center text-sm animate-fade-in-up animate-delay-200"
                style={{ color: "rgba(255,255,255,0.4)" }}>
                Already have an account?{" "}
                <Link href="/login" className="font-medium text-white hover:text-primary transition-colors">
                  Sign in
                </Link>
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
