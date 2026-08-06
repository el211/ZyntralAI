# Zyntral AI — System Architecture

> AI-powered marketing automation & content management SaaS.
> Designed for 100,000+ users, multi-tenant (workspace-based), provider-pluggable for AI and payments.

---

## 1. High-level topology

```
                                  ┌─────────────────────────────┐
                                  │           Clients           │
                                  │  Browser · Embedded widget  │
                                  └──────────────┬──────────────┘
                                                 │ HTTPS
                                  ┌──────────────▼──────────────┐
                                  │      Nginx reverse proxy     │
                                  │  TLS · gzip · rate-limit ·   │
                                  │  routes /  → web, /api → api │
                                  └───────┬─────────────┬────────┘
                                          │             │
                        ┌─────────────────▼──┐     ┌────▼───────────────────┐
                        │  Next.js (frontend) │     │  Spring Boot (backend) │
                        │  SSR/ISR · React Q. │     │  REST /api/v1 · JWT    │
                        └─────────────────────┘     └───┬───────┬───────┬────┘
                                                        │       │       │
                                  ┌─────────────────────▼─┐  ┌──▼────┐ ┌▼──────────────┐
                                  │      PostgreSQL        │  │ Redis │ │  Async worker │
                                  │  primary data store    │  │ cache │ │ (scheduler /  │
                                  │  (multi-tenant rows)    │  │ +rate │ │  publisher)   │
                                  └────────────────────────┘  └───────┘ └───┬───────────┘
                                                                            │
                  ┌─────────────────────────────────────────────────────────┼──────────────┐
                  │ External integrations (called through provider ports)    │              │
                  │  AI: OpenAI · Anthropic   Payments: Stripe · PayPal       │  Social APIs │
                  │  Email: SMTP/SES                                          │  X·LinkedIn· │
                  └──────────────────────────────────────────────────────────┘  Meta·TikTok │
                                                                                  ·YT·Pin     │
                                                                                  ─────────────┘
```

---

## 2. Architectural style

**Modular monolith with Clean / Hexagonal boundaries.** Not microservices.

**Why a modular monolith for a portfolio + commercial SaaS targeting 100k users?**
- A single deployable is *far* cheaper to operate and reason about than a fleet of services, and 100k users is comfortably served by a horizontally-scaled monolith + Postgres read replicas + Redis.
- Clean module boundaries (one Java package per bounded context, each owning its own controllers/services/repositories/entities) mean any module can later be **extracted into a service without rewriting business logic** — the seams already exist.
- Demonstrates *discipline* (the hard part of enterprise code) without the operational tax of premature distributed systems.

### Layering inside each module (dependencies point inward)

```
web (Controller, DTO, Mapper)        ← depends on application
        │
application (Service, UseCase, port  ← depends on domain
             interfaces, commands)
        │
domain (Entity, value objects,       ← depends on nothing
        domain rules, repo ports)
        │
infrastructure (JPA repo impls,      ← implements ports, depends on domain/application
        provider adapters, clients)
```

- **Ports & adapters** for everything external: `AiProvider`, `PaymentProvider`, `SocialPublisher`, `EmailSender` are interfaces in the application layer; concrete `OpenAiProvider`, `StripePaymentProvider`, `LinkedInPublisher`, etc. live in infrastructure and are wired by Spring. **Adding a provider = adding one class**, never touching business logic.

---

## 3. Key architectural decisions (ADR summary)

| # | Decision | Rationale | Trade-off accepted |
|---|----------|-----------|--------------------|
| 1 | **Modular monolith** over microservices | Operational simplicity, clean seams for later extraction | Single deploy unit; must keep modules disciplined |
| 2 | **Row-based multi-tenancy** keyed by `workspace_id` | Simplest to operate at 100k users; one schema, enforced by a tenant filter + every query scoped to workspace | Requires rigorous query scoping (mitigated by a base repository + Hibernate filter) |
| 3 | **JWT access + rotating refresh tokens** (refresh stored & revocable in DB/Redis) | Stateless API scales horizontally; refresh rotation gives revocation & logout-all | Slightly more token bookkeeping than pure sessions |
| 4 | **Provider port pattern** for AI & Payments | Open/Closed — new vendors plug in without edits | One extra indirection layer |
| 5 | **Async + queue for publishing/scheduling** | Social publish & AI calls are slow/flaky; never block request threads | Needs idempotency + retry/back-off + dead-letter |
| 6 | **Redis for cache + rate-limit + AI credit counters** | Sub-ms counters; protects upstream AI spend; per-plan throttling | Redis becomes a critical dependency (HA in prod) |
| 7 | **Flyway migrations** | Versioned, reviewable, reproducible schema; never `ddl-auto=update` in prod | Must write migrations by hand (a feature, not a bug) |
| 8 | **API versioning via URI** (`/api/v1`) | Explicit, cache-friendly, trivially routable at Nginx | Slightly less elegant than header negotiation |
| 9 | **Claude (latest) as default AI**, OpenAI as alternate | Best quality; both behind the same port so callers don't care | — |
| 10 | **i18n on both tiers** (ICU messages + `next-intl`) | Backend returns message *keys/localized* via `Accept-Language`; frontend translates UI | Translation upkeep |

---

## 4. Bounded contexts (modules)

| Module | Responsibility |
|--------|----------------|
| `auth` | Register, login, logout, JWT issue/refresh, email verification, password reset, RBAC |
| `user` | Profile, roles, account settings |
| `workspace` | Workspaces (tenants), members, invitations, permissions |
| `social` | Connected social accounts, OAuth tokens, per-platform adapters |
| `content` | Posts, drafts, media, scheduling, queue, content calendar |
| `ai` | Generation use-cases, prompt templates, provider port, credit accounting |
| `support` | Embeddable AI support agent, knowledge base, conversations |
| `billing` | Plans, subscriptions, payment providers, invoices, webhooks, usage limits |
| `admin` | Cross-tenant dashboards: users, revenue, AI usage, system health |
| `common` | Shared kernel: error model, API envelope, pagination, security, i18n, auditing |

---

## 5. Cross-cutting concerns

- **Security:** Spring Security filter chain → `JwtAuthenticationFilter` → `SecurityContext`. Method-level `@PreAuthorize` for RBAC (`USER`, `ADMIN`) + workspace-scoped permission checks. BCrypt password hashing. CORS locked to the web origin.
- **Validation:** Bean Validation (`jakarta.validation`) on DTOs; a `@ControllerAdvice` `GlobalExceptionHandler` maps every exception to a consistent RFC-7807-style envelope.
- **Error envelope (uniform):**
  ```json
  { "timestamp":"…","status":422,"error":"VALIDATION_FAILED",
    "message":"…","path":"/api/v1/…","traceId":"…","fieldErrors":[…] }
  ```
- **Observability:** Spring Boot Actuator (`/health`, `/health/liveness`, `/health/readiness`, `/prometheus`), structured JSON logging with a per-request `traceId` (MDC).
- **Rate limiting & credits:** Redis token-bucket per user/plan at the gateway filter; AI credit ledger decremented atomically per generation.
- **Idempotency:** Webhooks and publish jobs carry idempotency keys; processed-event table prevents double application.

---

## 6. Scaling notes (100k+ users)

- **Stateless backend** → scale horizontally behind Nginx; sticky sessions unnecessary (JWT).
- **Postgres:** primary + read replicas; heavy analytics (admin dashboards) hit replicas; partition high-volume tables (`ai_generations`, `audit_log`, `support_messages`) by month.
- **Connection pooling:** HikariCP sized per instance; PgBouncer in front of Postgres in production.
- **Caching:** plan limits, workspace membership, and AI prompt templates cached in Redis with short TTL + explicit invalidation on write.
- **Background work:** scheduled publisher and AI jobs run on the async worker profile so request latency is unaffected; jobs are retried with exponential back-off and land in a dead-letter table after N attempts.

See [DATABASE.md](./DATABASE.md) for the full schema, indexes, and ER description.
