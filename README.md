<div align="center">

# Zyntral AI

**AI-powered marketing automation & content management platform.**
Generate, schedule, and publish content across every major social network — with AI, teams, and analytics.

`Next.js` · `Spring Boot 3 / Java 21` · `PostgreSQL` · `Redis` · `Stripe + PayPal` · `OpenAI + Anthropic` · `Docker`

</div>

---

## What it does

- **AI content generation** — LinkedIn/X/Instagram/TikTok/Facebook posts, marketing copy, product descriptions, email campaigns, blog outlines, CTAs, hashtags. Tone & length controls.
- **Social management** — connect LinkedIn, X, Facebook, Instagram, TikTok, YouTube, Pinterest; draft, schedule, queue, publish; content calendar.
- **Workspaces & teams** — multi-tenant workspaces, member roles, invitations, shared content.
- **AI support agent** — embeddable website assistant with custom knowledge and conversation history.
- **Billing** — FREE / PRO / BUSINESS plans, monthly & annual, via a provider-agnostic payment layer (Stripe & PayPal).
- **Admin** — users, revenue, AI usage, system health.
- **i18n** — English & French, both tiers, extensible.

## Architecture

Modular monolith with Clean/Hexagonal boundaries. Provider **ports** (`AiProvider`, `PaymentProvider`, `SocialPublisher`) keep vendors swappable. Full write-up: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) · Schema: [`docs/DATABASE.md`](docs/DATABASE.md).

## Repository layout

```
ZyntralAI/
├── backend/                         # Spring Boot 3 · Java 21
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/zyntral/
│       │   ├── ZyntralApplication.java
│       │   ├── common/              # shared kernel
│       │   │   ├── error/           # GlobalExceptionHandler, ApiError, exceptions
│       │   │   ├── web/             # ApiResponse envelope, pagination, versioning
│       │   │   ├── security/        # JWT filter, token service, UserDetails, RBAC
│       │   │   ├── config/          # Security, OpenAPI, Redis, Cors, Async, i18n
│       │   │   └── i18n/            # MessageService
│       │   └── modules/
│       │       ├── auth/            # register, login, refresh, verify, reset
│       │       ├── user/            # profile, roles
│       │       ├── workspace/       # tenants, members, invitations
│       │       ├── social/          # accounts + per-platform publisher adapters
│       │       ├── content/         # posts, media, scheduling, calendar, queue
│       │       ├── ai/              # generation use-cases, provider port, credits
│       │       │   ├── provider/    # AiProvider, AnthropicProvider, OpenAiProvider
│       │       ├── support/         # AI support agent, knowledge, conversations
│       │       ├── billing/         # plans, subscriptions, invoices, webhooks
│       │       │   └── provider/    # PaymentProvider, Stripe*, PayPal*
│       │       └── admin/           # cross-tenant dashboards
│       │   (each module: web/ · application/ · domain/ · infrastructure/)
│       └── resources/
│           ├── application.yml + application-{dev,prod}.yml
│           ├── db/migration/        # Flyway: V1__initial_schema.sql …
│           └── i18n/                # messages_en.properties, messages_fr.properties
├── frontend/                        # Next.js · TypeScript · Tailwind · shadcn/ui
│   └── (app router, React Query, next-intl, dark mode)
├── infra/
│   ├── nginx/                       # reverse proxy config
│   └── (production docker-compose, deployment)
├── docs/                            # ARCHITECTURE.md · DATABASE.md · API.md
├── docker-compose.yml               # local dev: postgres + redis + mailhog
└── .env.example
```

> Per-module layering: `web` (controllers/DTOs/mappers) → `application` (services/use-cases/ports) → `domain` (entities/rules) → `infrastructure` (JPA impls/adapters). Dependencies point inward.

## Quick start (local)

```bash
# 1. infra
cp .env.example .env
docker compose up -d            # postgres, redis, mailhog

# 2. backend  (Java 21+, runs Flyway on boot)
cd backend && ./mvnw spring-boot:run
#   API     → http://localhost:8080/api/v1
#   Swagger → http://localhost:8080/swagger-ui.html
#   Health  → http://localhost:8080/actuator/health

# 3. frontend (added in the frontend phase)
cd frontend && npm install && npm run dev   # → http://localhost:3000
```

## Build status / roadmap

Built in reviewable phases — **all complete**:

- [x] **Phase 0** — Architecture & decisions (`docs/ARCHITECTURE.md`)
- [x] **Phase 1** — Folder structure & backend foundation (Spring Boot app, config, Docker infra)
- [x] **Phase 2** — PostgreSQL schema (`V1__initial_schema.sql`, `docs/DATABASE.md`)
- [x] **Phase 3** — Backend: common kernel (security/JWT, error model, i18n, OpenAPI)
- [x] **Phase 4** — Backend: auth + user + workspace modules (+ tests)
- [x] **Phase 5** — Backend: AI module (provider ports, generation, credits)
- [x] **Phase 6** — Backend: content/social (scheduling, publisher adapters)
- [x] **Phase 7** — Backend: billing (PaymentProvider, Stripe/PayPal, webhooks) + support agent + admin
- [x] **Phase 8** — Frontend: Next.js app (auth, dashboard, AI, calendar, billing, admin)
- [x] **Phase 9** — Production Docker, Nginx, deployment, CI

**Status:** backend = 139 source files, compiles on Java 21, `mvn test` green (16 tests). Frontend = 17 routes, `next build` clean. Production stack via `docker-compose.prod.yml`.

> Build the whole stack for production: `docker compose -f docker-compose.prod.yml --env-file .env up -d --build` (set `JWT_SECRET`, `CRYPTO_SECRET`, `POSTGRES_PASSWORD`, `REDIS_PASSWORD`).

## License

Proprietary — © Zyntral AI.
