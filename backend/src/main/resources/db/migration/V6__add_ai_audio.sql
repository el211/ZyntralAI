-- Generated speech (text-to-speech via ElevenLabs), stored in-DB and served by id.
CREATE TABLE ai_audio (
    id           UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL,
    text_excerpt TEXT NOT NULL,
    voice        VARCHAR(120),
    content_type VARCHAR(64) NOT NULL,
    data         BYTEA NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ai_audio_ws ON ai_audio(workspace_id, created_at DESC);
