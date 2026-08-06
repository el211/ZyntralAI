# Zyntral AI — Database Documentation

**Engine:** PostgreSQL 16+ · **Migrations:** Flyway (`backend/src/main/resources/db/migration`) · **IDs:** UUID (`gen_random_uuid()`) except `audit_log` (BIGINT identity).

The schema is **multi-tenant by row**: every tenant-owned table carries a `workspace_id` FK and all queries are scoped to it.

## ER overview (text)

```
users ──< user_roles >── roles
users ──1:N── refresh_tokens, auth_tokens
users ──1:N── workspaces (as owner)
workspaces ──< workspace_members >── users
workspaces ──1:N── workspace_invitations
workspaces ──1:N── social_accounts ──1:N── post_targets
workspaces ──1:N── posts ──1:N── post_media
                    posts ──1:N── post_targets >── social_accounts
                    posts ──N:1── ai_generations           (provenance)
workspaces ──1:N── ai_generations  (PARTITIONED BY MONTH)
workspaces ──1:1*── ai_credit_ledger (per period_month)
workspaces ──1:N── prompt_templates
workspaces ──1:N── support_agents ──1:N── support_knowledge
                   support_agents ──1:N── support_conversations ──1:N── support_messages
plans (FREE/PRO/BUSINESS) ──1:N── subscriptions
workspaces ──1:N── subscriptions ──1:N── invoices ──1:N── payment_transactions
workspaces ──1:N── billing_customers
webhook_events            (provider idempotency, standalone)
users/workspaces ──1:N── notifications, audit_log
```

## Table groups

| Group | Tables |
|-------|--------|
| Identity & access | `users`, `roles`, `user_roles`, `auth_tokens`, `refresh_tokens` |
| Tenancy | `workspaces`, `workspace_members`, `workspace_invitations` |
| Social | `social_accounts` |
| Content | `posts`, `post_media`, `post_targets` |
| AI | `prompt_templates`, `ai_generations`, `ai_credit_ledger` |
| Support agent | `support_agents`, `support_knowledge`, `support_conversations`, `support_messages` |
| Billing | `plans`, `billing_customers`, `subscriptions`, `invoices`, `payment_transactions`, `webhook_events` |
| Platform | `notifications`, `audit_log` |

## Notable design choices

- **Partitioning:** `ai_generations` is `PARTITION BY RANGE (created_at)` (monthly). High write volume + time-bounded analytics → cheap pruning and retention. Ops/app creates next month's partition ahead of time. `audit_log` and `support_messages` are candidates for the same treatment as volume grows.
- **Atomic AI credits:** `ai_credit_ledger` is keyed `(workspace_id, period_month)` with a `CHECK (credits_used <= credits_limit)`. A generation does `UPDATE ... SET credits_used = credits_used + :cost WHERE ... AND credits_used + :cost <= credits_limit` — the check + conditional update enforce limits without races.
- **One active subscription per workspace:** partial unique index `uq_active_subscription` on `subscriptions(workspace_id) WHERE status IN ('TRIALING','ACTIVE','PAST_DUE')`.
- **Idempotent webhooks:** `webhook_events (provider, event_id)` unique; `processed_at` null until applied. Replays are no-ops.
- **Token hygiene:** `auth_tokens`, `refresh_tokens`, and invitation tokens store **hashes**, never raw values. Social OAuth tokens are app-level encrypted at rest.
- **Soft enums via PG `ENUM` types** for stable, validated domains (platforms, statuses, plans). Adding a platform = `ALTER TYPE social_platform ADD VALUE '...'` in a migration — the schema is built to extend.

## Index strategy (hot paths)

| Query | Index |
|-------|-------|
| Login by email | `users.email` UNIQUE (citext) |
| Active refresh tokens for user | `idx_refresh_user_active` (partial) |
| Content calendar / queue | `idx_posts_ws_status`, `idx_posts_ws_scheduled` |
| Publisher due-job scan | `idx_posts_due` (partial: SCHEDULED/QUEUED) |
| AI usage analytics | `idx_ai_gen_ws_time`, `idx_ai_gen_user` |
| Unprocessed webhooks | `idx_webhook_unprocessed` (partial) |
| Unread notifications | `idx_notifications_user_unread` (partial) |

See `V1__initial_schema.sql` for the authoritative DDL, constraints, and triggers.
