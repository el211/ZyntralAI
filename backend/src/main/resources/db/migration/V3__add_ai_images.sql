-- Generated images (logos / banners) stored in-DB and served via /api/v1/ai-images/{id}.
CREATE TABLE ai_images (
    id           UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL,
    kind         VARCHAR(16) NOT NULL,          -- LOGO | BANNER
    prompt       TEXT NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    data         BYTEA NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ai_images_ws ON ai_images(workspace_id, created_at DESC);
