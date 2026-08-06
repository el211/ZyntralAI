-- Async video-generation jobs (Veo). The video bytes live in S3 (storage_key);
-- only metadata is kept here.
CREATE TABLE ai_videos (
    id             UUID PRIMARY KEY,
    workspace_id   UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id        UUID NOT NULL,
    prompt         TEXT NOT NULL,
    status         VARCHAR(16) NOT NULL,          -- PENDING | PROCESSING | COMPLETED | FAILED
    operation_name TEXT,
    storage_key    TEXT,
    content_type   VARCHAR(64),
    error          TEXT,
    credits_cost   INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ai_videos_ws     ON ai_videos(workspace_id, created_at DESC);
CREATE INDEX idx_ai_videos_status ON ai_videos(status);
