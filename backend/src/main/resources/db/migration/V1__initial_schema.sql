-- =====================================================================
-- Zyntral AI — Initial schema (V1)
-- PostgreSQL 16+  |  multi-tenant by workspace_id  |  UUID primary keys
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()
-- Emails are stored lower-cased by the application, so a plain VARCHAR UNIQUE gives
-- case-insensitive identity without the citext extension (and keeps JPA mapping simple).

-- ---------- enumerated types -----------------------------------------
CREATE TYPE user_status        AS ENUM ('PENDING','ACTIVE','SUSPENDED','DELETED');
CREATE TYPE workspace_role     AS ENUM ('OWNER','ADMIN','EDITOR','VIEWER');
CREATE TYPE invitation_status  AS ENUM ('PENDING','ACCEPTED','EXPIRED','REVOKED');
CREATE TYPE social_platform    AS ENUM ('LINKEDIN','TWITTER','FACEBOOK','INSTAGRAM','TIKTOK','YOUTUBE','PINTEREST');
CREATE TYPE social_acct_status AS ENUM ('CONNECTED','EXPIRED','REVOKED','ERROR');
CREATE TYPE post_status        AS ENUM ('DRAFT','SCHEDULED','QUEUED','PUBLISHING','PUBLISHED','FAILED','CANCELLED');
CREATE TYPE target_status      AS ENUM ('PENDING','PUBLISHING','PUBLISHED','FAILED');
CREATE TYPE ai_provider_kind   AS ENUM ('OPENAI','ANTHROPIC');
CREATE TYPE ai_content_kind    AS ENUM ('LINKEDIN_POST','X_POST','INSTAGRAM_CAPTION','TIKTOK_IDEA','FACEBOOK_POST',
                                         'MARKETING_COPY','PRODUCT_DESCRIPTION','EMAIL_CAMPAIGN','BLOG_OUTLINE',
                                         'CTA','HASHTAGS');
CREATE TYPE ai_tone            AS ENUM ('PROFESSIONAL','FRIENDLY','CORPORATE','CASUAL','SALES','MARKETING','TECHNICAL');
CREATE TYPE ai_length          AS ENUM ('SHORT','MEDIUM','LONG');
CREATE TYPE plan_code          AS ENUM ('FREE','PRO','BUSINESS');
CREATE TYPE billing_interval   AS ENUM ('MONTHLY','ANNUAL');
CREATE TYPE payment_provider   AS ENUM ('STRIPE','PAYPAL');
CREATE TYPE subscription_status AS ENUM ('TRIALING','ACTIVE','PAST_DUE','CANCELED','INCOMPLETE','EXPIRED');
CREATE TYPE invoice_status     AS ENUM ('DRAFT','OPEN','PAID','VOID','UNCOLLECTIBLE','REFUNDED');
CREATE TYPE conversation_role  AS ENUM ('USER','ASSISTANT','SYSTEM');

-- ---------- shared trigger: maintain updated_at -----------------------
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- IDENTITY & ACCESS
-- =====================================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   TEXT,                         -- null for pure-OAuth users
    full_name       VARCHAR(160),
    avatar_url      TEXT,
    locale          VARCHAR(8)  NOT NULL DEFAULT 'en',
    status          user_status NOT NULL DEFAULT 'PENDING',
    email_verified  BOOLEAN     NOT NULL DEFAULT FALSE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_users_updated BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE roles (
    id    SMALLINT PRIMARY KEY,
    code  VARCHAR(32) NOT NULL UNIQUE          -- USER, ADMIN
);
INSERT INTO roles (id, code) VALUES (1,'USER'), (2,'ADMIN');

CREATE TABLE user_roles (
    user_id UUID     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id SMALLINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- security tokens (email verification / password reset)
CREATE TABLE auth_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL,                   -- store hash, never raw token
    purpose     VARCHAR(32) NOT NULL,            -- EMAIL_VERIFY | PASSWORD_RESET
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_auth_tokens_user ON auth_tokens(user_id, purpose);
CREATE UNIQUE INDEX uq_auth_tokens_hash ON auth_tokens(token_hash);

-- rotating refresh tokens (revocable JWT refresh)
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,
    user_agent  TEXT,
    ip_address  VARCHAR(45),
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_tokens(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_user_active ON refresh_tokens(user_id) WHERE revoked_at IS NULL;

-- =====================================================================
-- WORKSPACES (TENANTS)
-- =====================================================================
CREATE TABLE workspaces (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(140) NOT NULL UNIQUE,
    owner_id    UUID NOT NULL REFERENCES users(id),
    plan        plan_code NOT NULL DEFAULT 'FREE',
    image_url   TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_workspaces_updated BEFORE UPDATE ON workspaces
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE INDEX idx_workspaces_owner ON workspaces(owner_id);

CREATE TABLE workspace_members (
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role         workspace_role NOT NULL DEFAULT 'EDITOR',
    joined_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (workspace_id, user_id)
);
CREATE INDEX idx_members_user ON workspace_members(user_id);

CREATE TABLE workspace_invitations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    email        VARCHAR(255) NOT NULL,
    role         workspace_role NOT NULL DEFAULT 'EDITOR',
    token_hash   TEXT NOT NULL UNIQUE,
    invited_by   UUID NOT NULL REFERENCES users(id),
    status       invitation_status NOT NULL DEFAULT 'PENDING',
    expires_at   TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_invitations_ws ON workspace_invitations(workspace_id, status);

-- =====================================================================
-- SOCIAL ACCOUNTS
-- =====================================================================
CREATE TABLE social_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    platform        social_platform NOT NULL,
    external_id     VARCHAR(190) NOT NULL,        -- platform account id
    display_name    VARCHAR(190),
    handle          VARCHAR(190),
    avatar_url      TEXT,
    access_token    TEXT,                         -- encrypted at rest (app-level)
    refresh_token   TEXT,                         -- encrypted at rest
    scopes          TEXT,
    token_expires_at TIMESTAMPTZ,
    status          social_acct_status NOT NULL DEFAULT 'CONNECTED',
    connected_by    UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_social_account UNIQUE (workspace_id, platform, external_id)
);
CREATE TRIGGER trg_social_updated BEFORE UPDATE ON social_accounts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE INDEX idx_social_ws_platform ON social_accounts(workspace_id, platform);

-- =====================================================================
-- CONTENT: posts, media, targets, schedules
-- =====================================================================
CREATE TABLE posts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    author_id     UUID NOT NULL REFERENCES users(id),
    title         VARCHAR(200),
    body          TEXT NOT NULL DEFAULT '',
    status        post_status NOT NULL DEFAULT 'DRAFT',
    scheduled_at  TIMESTAMPTZ,                    -- null = immediate/draft
    published_at  TIMESTAMPTZ,
    ai_generation_id UUID,                        -- provenance (set below via FK)
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_posts_updated BEFORE UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
-- calendar / queue lookups
CREATE INDEX idx_posts_ws_status      ON posts(workspace_id, status);
CREATE INDEX idx_posts_ws_scheduled   ON posts(workspace_id, scheduled_at);
-- due-job scan for the publisher worker
CREATE INDEX idx_posts_due ON posts(scheduled_at)
    WHERE status IN ('SCHEDULED','QUEUED');

CREATE TABLE post_media (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    url         TEXT NOT NULL,
    media_type  VARCHAR(32) NOT NULL,             -- IMAGE | VIDEO | GIF
    alt_text    VARCHAR(420),
    position    SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_post_media_post ON post_media(post_id);

-- one post fans out to N platform targets
CREATE TABLE post_targets (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id           UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    social_account_id UUID NOT NULL REFERENCES social_accounts(id) ON DELETE CASCADE,
    status            target_status NOT NULL DEFAULT 'PENDING',
    external_post_id  VARCHAR(190),               -- id returned by the platform
    permalink         TEXT,
    error_message     TEXT,
    attempts          SMALLINT NOT NULL DEFAULT 0,
    published_at      TIMESTAMPTZ,
    CONSTRAINT uq_post_target UNIQUE (post_id, social_account_id)
);
CREATE INDEX idx_targets_account ON post_targets(social_account_id);

-- =====================================================================
-- AI: prompt templates, generations, credit ledger
-- =====================================================================
CREATE TABLE prompt_templates (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  UUID REFERENCES workspaces(id) ON DELETE CASCADE,  -- null = system template
    content_kind  ai_content_kind NOT NULL,
    name          VARCHAR(140) NOT NULL,
    template      TEXT NOT NULL,
    is_system     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_templates_ws_kind ON prompt_templates(workspace_id, content_kind);

CREATE TABLE ai_generations (
    id             UUID NOT NULL DEFAULT gen_random_uuid(),
    workspace_id   UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id        UUID NOT NULL REFERENCES users(id),
    provider       ai_provider_kind NOT NULL,
    model          VARCHAR(80) NOT NULL,
    content_kind   ai_content_kind NOT NULL,
    tone           ai_tone,
    length         ai_length,
    language       VARCHAR(8) NOT NULL DEFAULT 'en',
    prompt         TEXT NOT NULL,
    output         TEXT,
    prompt_tokens  INTEGER NOT NULL DEFAULT 0,
    output_tokens  INTEGER NOT NULL DEFAULT 0,
    credits_cost   INTEGER NOT NULL DEFAULT 1,
    status         VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',  -- SUCCESS|FAILED
    latency_ms     INTEGER,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- partition key (created_at) must be part of the PK on a partitioned table
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
-- first monthly partitions (the app/ops create future ones)
CREATE TABLE ai_generations_2026_06 PARTITION OF ai_generations
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE ai_generations_2026_07 PARTITION OF ai_generations
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE INDEX idx_ai_gen_ws_time ON ai_generations(workspace_id, created_at DESC);
CREATE INDEX idx_ai_gen_user    ON ai_generations(user_id, created_at DESC);

-- NOTE: posts.ai_generation_id is a soft provenance reference (no FK): a partitioned
-- table's PK is composite (id, created_at), so it cannot be the target of a simple FK.

-- monthly credit ledger per workspace (atomic decrement target)
CREATE TABLE ai_credit_ledger (
    workspace_id   UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    period_month   DATE NOT NULL,                 -- first day of month
    credits_limit  INTEGER NOT NULL,
    credits_used   INTEGER NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (workspace_id, period_month),
    CONSTRAINT chk_credits_nonneg CHECK (credits_used >= 0 AND credits_used <= credits_limit)
);

-- =====================================================================
-- AI CUSTOMER SUPPORT AGENT
-- =====================================================================
CREATE TABLE support_agents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name          VARCHAR(140) NOT NULL,
    public_key    VARCHAR(64) NOT NULL UNIQUE,    -- used by the embed widget
    system_prompt TEXT,
    model         VARCHAR(80) NOT NULL DEFAULT 'claude-opus-4-8',
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_support_agents_updated BEFORE UPDATE ON support_agents
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE support_knowledge (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id    UUID NOT NULL REFERENCES support_agents(id) ON DELETE CASCADE,
    title       VARCHAR(200),
    content     TEXT NOT NULL,
    source_url  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_knowledge_agent ON support_knowledge(agent_id);

CREATE TABLE support_conversations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id    UUID NOT NULL REFERENCES support_agents(id) ON DELETE CASCADE,
    visitor_id  VARCHAR(64),                      -- anonymous cookie id
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_conv_agent ON support_conversations(agent_id, created_at DESC);

CREATE TABLE support_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES support_conversations(id) ON DELETE CASCADE,
    role            conversation_role NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_conv ON support_messages(conversation_id, created_at);

-- =====================================================================
-- BILLING: plans, subscriptions, payments, invoices, webhooks
-- =====================================================================
CREATE TABLE plans (
    code              plan_code PRIMARY KEY,
    name              VARCHAR(60) NOT NULL,
    monthly_price_cents INTEGER NOT NULL DEFAULT 0,
    annual_price_cents  INTEGER NOT NULL DEFAULT 0,
    ai_credits_monthly  INTEGER NOT NULL,
    max_team_members    INTEGER NOT NULL,
    max_social_accounts INTEGER NOT NULL,
    max_workspaces      INTEGER NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE
);
INSERT INTO plans (code,name,monthly_price_cents,annual_price_cents,ai_credits_monthly,max_team_members,max_social_accounts,max_workspaces) VALUES
 ('FREE',    'Free',         0,     0,      50,   1,  2,  1),
 ('PRO',     'Pro',       2900, 29000,    2000,   5, 10,  3),
 ('BUSINESS','Business', 9900, 99000,   20000,  25, 50, 10);

CREATE TABLE billing_customers (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    provider      payment_provider NOT NULL,
    external_id   VARCHAR(190) NOT NULL,          -- stripe/paypal customer id
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_billing_customer UNIQUE (provider, external_id),
    CONSTRAINT uq_ws_provider UNIQUE (workspace_id, provider)
);

CREATE TABLE subscriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id        UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    plan                plan_code NOT NULL REFERENCES plans(code),
    provider            payment_provider NOT NULL,
    external_id         VARCHAR(190) NOT NULL,    -- provider subscription id
    status              subscription_status NOT NULL DEFAULT 'INCOMPLETE',
    interval            billing_interval NOT NULL DEFAULT 'MONTHLY',
    current_period_start TIMESTAMPTZ,
    current_period_end   TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_subscription_external UNIQUE (provider, external_id)
);
CREATE TRIGGER trg_subscriptions_updated BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
-- a workspace has at most one active subscription
CREATE UNIQUE INDEX uq_active_subscription ON subscriptions(workspace_id)
    WHERE status IN ('TRIALING','ACTIVE','PAST_DUE');

CREATE TABLE invoices (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES subscriptions(id) ON DELETE SET NULL,
    provider      payment_provider NOT NULL,
    external_id   VARCHAR(190) NOT NULL,
    number        VARCHAR(60),
    amount_cents  INTEGER NOT NULL,
    currency      VARCHAR(3) NOT NULL DEFAULT 'USD',
    status        invoice_status NOT NULL DEFAULT 'OPEN',
    hosted_url    TEXT,
    pdf_url       TEXT,
    issued_at     TIMESTAMPTZ,
    paid_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_invoice_external UNIQUE (provider, external_id)
);
CREATE INDEX idx_invoices_ws ON invoices(workspace_id, created_at DESC);

CREATE TABLE payment_transactions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    invoice_id    UUID REFERENCES invoices(id) ON DELETE SET NULL,
    provider      payment_provider NOT NULL,
    external_id   VARCHAR(190) NOT NULL,
    amount_cents  INTEGER NOT NULL,
    currency      VARCHAR(3) NOT NULL DEFAULT 'USD',
    status        VARCHAR(32) NOT NULL,           -- SUCCEEDED|FAILED|REFUNDED|PENDING
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_txn_external UNIQUE (provider, external_id)
);
CREATE INDEX idx_txn_ws ON payment_transactions(workspace_id, created_at DESC);

-- idempotent webhook processing (Stripe & PayPal)
CREATE TABLE webhook_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider      payment_provider NOT NULL,
    event_id      VARCHAR(190) NOT NULL,
    event_type    VARCHAR(120) NOT NULL,
    payload       JSONB NOT NULL,
    processed_at  TIMESTAMPTZ,
    received_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_webhook_event UNIQUE (provider, event_id)
);
CREATE INDEX idx_webhook_unprocessed ON webhook_events(received_at) WHERE processed_at IS NULL;

-- =====================================================================
-- PLATFORM: notifications & audit
-- =====================================================================
CREATE TABLE notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
    type         VARCHAR(60) NOT NULL,
    title        VARCHAR(200) NOT NULL,
    body         TEXT,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id) WHERE read_at IS NULL;

CREATE TABLE audit_log (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL,
    action       VARCHAR(80) NOT NULL,
    entity_type  VARCHAR(60),
    entity_id    UUID,
    metadata     JSONB,
    ip_address   INET,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_ws_time    ON audit_log(workspace_id, created_at DESC);
CREATE INDEX idx_audit_actor_time ON audit_log(actor_id, created_at DESC);
