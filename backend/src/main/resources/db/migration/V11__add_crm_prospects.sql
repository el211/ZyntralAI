-- CRM prospects table for Zyntral CRM module
CREATE TABLE crm_prospects (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id      UUID         NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    added_by          UUID         NOT NULL,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100),
    company           VARCHAR(200),
    website           VARCHAR(500),
    email             VARCHAR(200),
    phone             VARCHAR(50),
    linkedin_url      VARCHAR(500),
    country           VARCHAR(100),
    industry          VARCHAR(100),
    status            VARCHAR(50)  NOT NULL DEFAULT 'NEW',
    notes             TEXT,
    last_contacted_at TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX crm_prospects_workspace_idx ON crm_prospects(workspace_id, created_at DESC);
CREATE INDEX crm_prospects_status_idx    ON crm_prospects(workspace_id, status);
