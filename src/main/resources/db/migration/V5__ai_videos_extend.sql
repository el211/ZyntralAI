-- Extra video options: source URI (for extension, valid ~2 days), resolution, duration.
ALTER TABLE ai_videos ADD COLUMN IF NOT EXISTS veo_video_uri   TEXT;
ALTER TABLE ai_videos ADD COLUMN IF NOT EXISTS resolution      VARCHAR(8);
ALTER TABLE ai_videos ADD COLUMN IF NOT EXISTS duration_seconds INT;
