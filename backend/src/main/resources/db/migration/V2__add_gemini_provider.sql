-- Add Gemini as a third AI provider. The enum value must exist before the app can
-- persist a generation served by Gemini (ai_generations.provider is ai_provider_kind).
ALTER TYPE ai_provider_kind ADD VALUE IF NOT EXISTS 'GEMINI';
