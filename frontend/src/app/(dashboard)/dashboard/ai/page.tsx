"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, unwrap, apiErrorMessage } from "@/lib/api";
import { useWorkspace } from "@/lib/workspace";
import { AiContentKind, AiLength, AiTone, CreditUsage, GenerationResult } from "@/lib/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Sparkles, Copy, Send, Github } from "lucide-react";
import { ImageGenerator } from "@/components/image-generator";
import { VideoGenerator } from "@/components/video-generator";
import { SpeechGenerator } from "@/components/speech-generator";
import { DubbingTab } from "./dubbing-tab";

const TABS = [
  { id: "TEXT",           label: "Text" },
  { id: "GITHUB",         label: "GitHub → Post" },
  { id: "GITHUB_FEATURE", label: "GitHub Feature" },
  { id: "LOGO",           label: "Logo" },
  { id: "BANNER",         label: "Banner" },
  { id: "VIDEO",          label: "Video" },
  { id: "SPEECH",         label: "Speech" },
  { id: "DUBBING",        label: "Video dubbing" },
] as const;
type StudioTab = (typeof TABS)[number]["id"];

const KINDS: AiContentKind[] = [
  "LINKEDIN_POST", "X_POST", "INSTAGRAM_CAPTION", "TIKTOK_IDEA", "FACEBOOK_POST",
  "MARKETING_COPY", "PRODUCT_DESCRIPTION", "EMAIL_CAMPAIGN", "BLOG_OUTLINE", "CTA", "HASHTAGS",
];
const TONES: AiTone[] = ["PROFESSIONAL", "FRIENDLY", "CORPORATE", "CASUAL", "SALES", "MARKETING", "TECHNICAL"];
const LENGTHS: AiLength[] = ["SHORT", "MEDIUM", "LONG"];

const pretty = (s: string) => s.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());

export default function AiStudioPage() {
  const { current } = useWorkspace();
  const qc = useQueryClient();
  const router = useRouter();

  function sendToPost(r: GenerationResult) {
    sessionStorage.setItem("zyntral_compose", JSON.stringify({ body: r.output, aiGenerationId: r.id }));
    router.push("/dashboard/posts");
  }

  // ── Text generation form ──────────────────────────────────────────────────
  const [form, setForm] = useState({
    contentKind: "LINKEDIN_POST" as AiContentKind,
    tone: "PROFESSIONAL" as AiTone,
    length: "MEDIUM" as AiLength,
    language: "en",
    topic: "",
    provider: "ANTHROPIC" as "ANTHROPIC" | "OPENAI" | "GEMINI",
  });

  // ── GitHub commit form ────────────────────────────────────────────────────
  const [ghForm, setGhForm] = useState({
    repoUrl: "",
    commitSha: "",
    githubToken: "",
    tone: "PROFESSIONAL" as AiTone,
    length: "MEDIUM" as AiLength,
    language: "en",
    provider: "ANTHROPIC" as "ANTHROPIC" | "OPENAI" | "GEMINI",
  });

  // ── GitHub Feature form ───────────────────────────────────────────────────
  const [ghFeatureForm, setGhFeatureForm] = useState({
    repoUrl: "",
    featureName: "",
    featureDescription: "",
    filePaths: "",
    githubToken: "",
    contentKind: "LINKEDIN_POST" as AiContentKind,
    tone: "PROFESSIONAL" as AiTone,
    length: "MEDIUM" as AiLength,
    language: "en",
    provider: "ANTHROPIC" as "ANTHROPIC" | "OPENAI" | "GEMINI",
  });

  const [result, setResult] = useState<GenerationResult | null>(null);
  const [featureResult, setFeatureResult] = useState<GenerationResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<StudioTab>("TEXT");

  const { data: credits } = useQuery({
    queryKey: ["credits", current?.id],
    enabled: !!current,
    queryFn: async () =>
      unwrap<CreditUsage>((await api.get(`/workspaces/${current!.id}/ai/credits`)).data),
  });

  const generate = useMutation({
    mutationFn: async () =>
      unwrap<GenerationResult>(
        (await api.post(`/workspaces/${current!.id}/ai/generate`, form)).data,
      ),
    onSuccess: (data) => {
      setResult(data);
      setError(null);
      qc.invalidateQueries({ queryKey: ["credits", current?.id] });
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const generateGitHub = useMutation({
    mutationFn: async () =>
      unwrap<GenerationResult>(
        (await api.post(`/workspaces/${current!.id}/ai/generate/from-github`, {
          repoUrl: ghForm.repoUrl,
          commitSha: ghForm.commitSha || null,
          githubToken: ghForm.githubToken || null,
          tone: ghForm.tone,
          length: ghForm.length,
          language: ghForm.language,
          provider: ghForm.provider,
        })).data,
      ),
    onSuccess: (data) => {
      setResult(data);
      setError(null);
      qc.invalidateQueries({ queryKey: ["credits", current?.id] });
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const generateGitHubFeature = useMutation({
    mutationFn: async () =>
      unwrap<GenerationResult>(
        (await api.post(`/workspaces/${current!.id}/ai/generate/from-github-feature`, {
          repoUrl: ghFeatureForm.repoUrl,
          featureName: ghFeatureForm.featureName,
          featureDescription: ghFeatureForm.featureDescription,
          filePaths: ghFeatureForm.filePaths || null,
          githubToken: ghFeatureForm.githubToken || null,
          contentKind: ghFeatureForm.contentKind,
          tone: ghFeatureForm.tone,
          length: ghFeatureForm.length,
          language: ghFeatureForm.language,
          provider: ghFeatureForm.provider,
        })).data,
      ),
    onSuccess: (data) => {
      setFeatureResult(data);
      setError(null);
      qc.invalidateQueries({ queryKey: ["credits", current?.id] });
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  if (!current) return <p className="text-muted-foreground">Select a workspace first.</p>;

  const isPending = generate.isPending || generateGitHub.isPending || generateGitHubFeature.isPending;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">AI Studio</h1>
        {credits && (
          <span className="text-sm text-muted-foreground">
            {credits.remaining}/{credits.limit} credits left
          </span>
        )}
      </div>

      <div className="flex w-fit flex-wrap gap-1 rounded-lg border bg-secondary/50 p-1">
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => { setTab(t.id); setResult(null); setFeatureResult(null); setError(null); }}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              tab === t.id
                ? "bg-background text-foreground shadow-sm"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* ── GitHub Feature ──────────────────────────────────────────────── */}
      {tab === "GITHUB_FEATURE" && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <Github className="h-4 w-4" />
                GitHub feature showcase
              </CardTitle>
              <p className="text-sm text-muted-foreground">
                Describe a feature you built — we analyse the relevant code and write a post about it.
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-1.5">
                <Label>Repository URL</Label>
                <Input
                  placeholder="https://github.com/owner/repo"
                  value={ghFeatureForm.repoUrl}
                  onChange={(e) => setGhFeatureForm({ ...ghFeatureForm, repoUrl: e.target.value })}
                />
              </div>
              <div className="space-y-1.5">
                <Label>Feature name</Label>
                <Input
                  placeholder="e.g. AI-powered content scheduler"
                  value={ghFeatureForm.featureName}
                  onChange={(e) => setGhFeatureForm({ ...ghFeatureForm, featureName: e.target.value })}
                />
              </div>
              <div className="space-y-1.5">
                <Label>Describe the feature</Label>
                <Textarea
                  rows={4}
                  placeholder="What does it do? What problem does it solve? What makes it interesting?"
                  value={ghFeatureForm.featureDescription}
                  onChange={(e) => setGhFeatureForm({ ...ghFeatureForm, featureDescription: e.target.value })}
                />
              </div>
              <div className="space-y-1.5">
                <Label>
                  Specific file paths{" "}
                  <span className="text-xs text-muted-foreground">(optional — comma-separated, e.g. src/scheduler.ts)</span>
                </Label>
                <Input
                  placeholder="Leave blank to auto-discover relevant files"
                  value={ghFeatureForm.filePaths}
                  onChange={(e) => setGhFeatureForm({ ...ghFeatureForm, filePaths: e.target.value })}
                />
              </div>
              <div className="space-y-1.5">
                <Label>
                  GitHub token{" "}
                  <span className="text-xs text-muted-foreground">(optional — required for private repos)</span>
                </Label>
                <Input
                  type="password"
                  placeholder="ghp_…"
                  value={ghFeatureForm.githubToken}
                  onChange={(e) => setGhFeatureForm({ ...ghFeatureForm, githubToken: e.target.value })}
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label>Post format</Label>
                  <Select value={ghFeatureForm.contentKind}
                    onChange={(e) => setGhFeatureForm({ ...ghFeatureForm, contentKind: e.target.value as AiContentKind })}>
                    {KINDS.map((k) => <option key={k} value={k}>{pretty(k)}</option>)}
                  </Select>
                </div>
                <div className="space-y-1.5">
                  <Label>Tone</Label>
                  <Select value={ghFeatureForm.tone}
                    onChange={(e) => setGhFeatureForm({ ...ghFeatureForm, tone: e.target.value as AiTone })}>
                    {TONES.map((t) => <option key={t} value={t}>{pretty(t)}</option>)}
                  </Select>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-3">
                <div className="space-y-1.5">
                  <Label>Length</Label>
                  <Select value={ghFeatureForm.length}
                    onChange={(e) => setGhFeatureForm({ ...ghFeatureForm, length: e.target.value as AiLength })}>
                    {LENGTHS.map((l) => <option key={l} value={l}>{pretty(l)}</option>)}
                  </Select>
                </div>
                <div className="space-y-1.5">
                  <Label>AI model</Label>
                  <Select value={ghFeatureForm.provider}
                    onChange={(e) => setGhFeatureForm({ ...ghFeatureForm, provider: e.target.value as "ANTHROPIC" | "OPENAI" | "GEMINI" })}>
                    <option value="ANTHROPIC">Claude (Anthropic)</option>
                    <option value="OPENAI">ChatGPT (OpenAI)</option>
                    <option value="GEMINI">Gemini (Google)</option>
                  </Select>
                </div>
                <div className="space-y-1.5">
                  <Label>Language</Label>
                  <Select value={ghFeatureForm.language}
                    onChange={(e) => setGhFeatureForm({ ...ghFeatureForm, language: e.target.value })}>
                    <option value="en">English</option>
                    <option value="fr">Français</option>
                  </Select>
                </div>
              </div>
              {error && <p className="text-sm text-destructive">{error}</p>}
              <Button
                className="w-full"
                disabled={!ghFeatureForm.repoUrl.trim() || !ghFeatureForm.featureName.trim() || !ghFeatureForm.featureDescription.trim() || generateGitHubFeature.isPending}
                onClick={() => generateGitHubFeature.mutate()}
              >
                <Sparkles className="h-4 w-4" />
                {generateGitHubFeature.isPending ? "Analysing code & generating…" : "Generate post"}
              </Button>
            </CardContent>
          </Card>

          <ResultCard
            result={featureResult}
            isPending={generateGitHubFeature.isPending}
            onCopy={() => navigator.clipboard.writeText(featureResult!.output)}
            onSend={() => sendToPost(featureResult!)}
          />
        </div>
      )}

      {(tab === "LOGO" || tab === "BANNER") && <ImageGenerator kind={tab} />}
      {tab === "VIDEO"   && <VideoGenerator />}
      {tab === "SPEECH"  && <SpeechGenerator />}
      {tab === "DUBBING" && <DubbingTab />}

      {/* ── Text generation ─────────────────────────────────────────────── */}
      {tab === "TEXT" && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader><CardTitle className="text-base">Generate</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label>Content type</Label>
                  <Select value={form.contentKind}
                    onChange={(e) => setForm({ ...form, contentKind: e.target.value as AiContentKind })}>
                    {KINDS.map((k) => <option key={k} value={k}>{pretty(k)}</option>)}
                  </Select>
                </div>
                <div className="space-y-1.5">
                  <Label>AI model</Label>
                  <Select value={form.provider}
                    onChange={(e) => setForm({ ...form, provider: e.target.value as "ANTHROPIC" | "OPENAI" | "GEMINI" })}>
                    <option value="ANTHROPIC">Claude (Anthropic)</option>
                    <option value="OPENAI">ChatGPT (OpenAI)</option>
                    <option value="GEMINI">Gemini (Google)</option>
                  </Select>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-3">
                <div className="space-y-1.5">
                  <Label>Tone</Label>
                  <Select value={form.tone}
                    onChange={(e) => setForm({ ...form, tone: e.target.value as AiTone })}>
                    {TONES.map((t) => <option key={t} value={t}>{pretty(t)}</option>)}
                  </Select>
                </div>
                <div className="space-y-1.5">
                  <Label>Length</Label>
                  <Select value={form.length}
                    onChange={(e) => setForm({ ...form, length: e.target.value as AiLength })}>
                    {LENGTHS.map((l) => <option key={l} value={l}>{pretty(l)}</option>)}
                  </Select>
                </div>
                <div className="space-y-1.5">
                  <Label>Language</Label>
                  <Select value={form.language}
                    onChange={(e) => setForm({ ...form, language: e.target.value })}>
                    <option value="en">English</option>
                    <option value="fr">Français</option>
                  </Select>
                </div>
              </div>
              <div className="space-y-1.5">
                <Label>Topic / brief</Label>
                <Textarea rows={5} placeholder="What should we write about?"
                  value={form.topic}
                  onChange={(e) => setForm({ ...form, topic: e.target.value })} />
              </div>
              {error && <p className="text-sm text-destructive">{error}</p>}
              <Button className="w-full" disabled={!form.topic || isPending}
                onClick={() => generate.mutate()}>
                <Sparkles className="h-4 w-4" />
                {generate.isPending ? "Generating…" : "Generate"}
              </Button>
            </CardContent>
          </Card>

          <ResultCard result={result} isPending={isPending} onCopy={() => navigator.clipboard.writeText(result!.output)} onSend={() => sendToPost(result!)} />
        </div>
      )}

      {/* ── GitHub → LinkedIn ────────────────────────────────────────────── */}
      {tab === "GITHUB" && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <Github className="h-4 w-4" />
                GitHub commit → LinkedIn post
              </CardTitle>
              <p className="text-sm text-muted-foreground">
                Paste a repo URL — we fetch the latest commit (or a specific one) and write a LinkedIn post about it.
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-1.5">
                <Label>Repository URL</Label>
                <Input
                  placeholder="https://github.com/owner/repo"
                  value={ghForm.repoUrl}
                  onChange={(e) => setGhForm({ ...ghForm, repoUrl: e.target.value })}
                />
              </div>
              <div className="space-y-1.5">
                <Label>
                  Commit SHA{" "}
                  <span className="text-xs text-muted-foreground">(optional — defaults to latest)</span>
                </Label>
                <Input
                  placeholder="e.g. a3f2c1d"
                  value={ghForm.commitSha}
                  onChange={(e) => setGhForm({ ...ghForm, commitSha: e.target.value })}
                />
              </div>
              <div className="space-y-1.5">
                <Label>
                  GitHub token{" "}
                  <span className="text-xs text-muted-foreground">(optional — required for private repos)</span>
                </Label>
                <Input
                  type="password"
                  placeholder="ghp_…"
                  value={ghForm.githubToken}
                  onChange={(e) => setGhForm({ ...ghForm, githubToken: e.target.value })}
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label>Tone</Label>
                  <Select value={ghForm.tone}
                    onChange={(e) => setGhForm({ ...ghForm, tone: e.target.value as AiTone })}>
                    {TONES.map((t) => <option key={t} value={t}>{pretty(t)}</option>)}
                  </Select>
                </div>
                <div className="space-y-1.5">
                  <Label>Length</Label>
                  <Select value={ghForm.length}
                    onChange={(e) => setGhForm({ ...ghForm, length: e.target.value as AiLength })}>
                    {LENGTHS.map((l) => <option key={l} value={l}>{pretty(l)}</option>)}
                  </Select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label>AI model</Label>
                  <Select value={ghForm.provider}
                    onChange={(e) => setGhForm({ ...ghForm, provider: e.target.value as "ANTHROPIC" | "OPENAI" | "GEMINI" })}>
                    <option value="ANTHROPIC">Claude (Anthropic)</option>
                    <option value="OPENAI">ChatGPT (OpenAI)</option>
                    <option value="GEMINI">Gemini (Google)</option>
                  </Select>
                </div>
                <div className="space-y-1.5">
                  <Label>Language</Label>
                  <Select value={ghForm.language}
                    onChange={(e) => setGhForm({ ...ghForm, language: e.target.value })}>
                    <option value="en">English</option>
                    <option value="fr">Français</option>
                  </Select>
                </div>
              </div>
              {error && <p className="text-sm text-destructive">{error}</p>}
              <Button
                className="w-full"
                disabled={!ghForm.repoUrl.trim() || isPending}
                onClick={() => generateGitHub.mutate()}
              >
                <Github className="h-4 w-4" />
                {generateGitHub.isPending ? "Fetching commit & generating…" : "Generate LinkedIn post"}
              </Button>
            </CardContent>
          </Card>

          <ResultCard result={result} isPending={isPending} onCopy={() => navigator.clipboard.writeText(result!.output)} onSend={() => sendToPost(result!)} />
        </div>
      )}
    </div>
  );
}

function ResultCard({
  result,
  isPending,
  onCopy,
  onSend,
}: {
  result: GenerationResult | null;
  isPending: boolean;
  onCopy: () => void;
  onSend: () => void;
}) {
  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <CardTitle className="text-base">Result</CardTitle>
        {result && (
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" onClick={onCopy}>
              <Copy className="h-4 w-4" /> Copy
            </Button>
            <Button size="sm" onClick={onSend}>
              <Send className="h-4 w-4" /> Use in a post
            </Button>
          </div>
        )}
      </CardHeader>
      <CardContent>
        {isPending ? (
          <div className="flex flex-col items-center justify-center py-12 gap-3 text-muted-foreground">
            <Sparkles className="h-6 w-6 animate-pulse" />
            <p className="text-sm">Generating…</p>
          </div>
        ) : result ? (
          <div className="space-y-3">
            <pre className="whitespace-pre-wrap rounded-md bg-secondary p-4 text-sm leading-relaxed">{result.output}</pre>
            <p className="text-xs text-muted-foreground">
              {result.provider} · {result.model} · {result.outputTokens} tokens · {result.creditsCost} credit(s)
            </p>
          </div>
        ) : (
          <p className="py-10 text-center text-sm text-muted-foreground">
            Your generated content will appear here.
          </p>
        )}
      </CardContent>
    </Card>
  );
}
