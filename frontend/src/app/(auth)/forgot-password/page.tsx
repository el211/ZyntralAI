"use client";

import { useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Zap, ArrowLeft, ArrowRight, MailCheck } from "lucide-react";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    await api.post("/auth/forgot-password", { email }).catch(() => {});
    setSent(true);
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-6 dark"
      style={{ background: "hsl(230, 22%, 5%)" }}>
      <div className="w-full max-w-[400px] space-y-8">
        {/* Logo */}
        <div className="flex items-center gap-2.5 animate-fade-in">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl"
            style={{ background: "#F5B319" }}>
            <Zap className="h-4 w-4" style={{ color: "hsl(230,22%,5%)" }} />
          </div>
          <span className="font-syne font-bold text-xl tracking-tight text-white">Zyntral AI</span>
        </div>

        {sent ? (
          <div className="space-y-6 animate-fade-in-up">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl"
              style={{ background: "rgba(245,179,25,0.12)", border: "1px solid rgba(245,179,25,0.25)" }}>
              <MailCheck className="h-6 w-6" style={{ color: "#F5B319" }} />
            </div>
            <div className="space-y-2">
              <h1 className="font-syne text-2xl font-bold text-white">Check your email</h1>
              <p className="text-sm leading-relaxed" style={{ color: "rgba(255,255,255,0.45)" }}>
                If an account exists for{" "}
                <span className="font-medium text-white">{email}</span>,
                a password reset link is on its way. Check your spam folder too.
              </p>
            </div>
            <Link href="/login">
              <Button variant="outline"
                className="gap-2 border-white/10 text-white hover:bg-white/5 hover:text-white">
                <ArrowLeft className="h-4 w-4" /> Back to sign in
              </Button>
            </Link>
          </div>
        ) : (
          <div className="space-y-8">
            {/* Back button */}
            <Link href="/login"
              className="inline-flex items-center gap-1.5 text-sm transition-colors hover:text-white"
              style={{ color: "rgba(255,255,255,0.4)" }}>
              <ArrowLeft className="h-3.5 w-3.5" /> Back to sign in
            </Link>

            <div className="space-y-2 animate-fade-in-up">
              <h1 className="font-syne text-[2rem] font-bold tracking-tight text-white">
                Reset password
              </h1>
              <p className="text-sm" style={{ color: "rgba(255,255,255,0.45)" }}>
                Enter your email and we&apos;ll send you a reset link.
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
              <Button
                type="submit"
                className="w-full h-11 font-semibold text-sm gap-2"
                style={{ background: "#F5B319", color: "hsl(230,22%,5%)" }}>
                <span>Send reset link</span><ArrowRight className="h-4 w-4" />
              </Button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}
