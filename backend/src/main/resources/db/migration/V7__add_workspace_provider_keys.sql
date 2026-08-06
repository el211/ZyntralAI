-- Per-workspace "bring your own key" credentials for AI providers (e.g. ElevenLabs).
-- The api_key is encrypted at rest by StringCryptoConverter (AES-256-GCM).
CREATE TABLE workspace_provider_keys (
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    provider     VARCHAR(40) NOT NULL,
    api_key      TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (workspace_id, provider)
);
