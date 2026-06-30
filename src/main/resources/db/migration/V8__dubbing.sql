-- Video dubbing (ElevenLabs). See modules/dubbing. The ElevenLabs API key is the workspace's
-- own BYOK key, reused from workspace_provider_keys (provider 'ELEVENLABS') — no separate table.

CREATE TYPE dubbing_status AS ENUM ('QUEUED','DUBBING','DUBBED','FAILED');

-- Dubbing job metadata; media stays at ElevenLabs and is streamed through on download.
CREATE TABLE dubbing_jobs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL,
    dubbing_id   TEXT NOT NULL,
    name         TEXT,
    source_lang  TEXT,
    target_lang  TEXT NOT NULL,
    status       dubbing_status NOT NULL DEFAULT 'QUEUED',
    error        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_dubbing_jobs_updated BEFORE UPDATE ON dubbing_jobs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE INDEX idx_dubbing_ws_time ON dubbing_jobs(workspace_id, created_at DESC);
