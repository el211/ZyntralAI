# Zyntral AI — API Documentation

**Base URL:** `/api/v1` · **Auth:** `Authorization: Bearer <access-token>` (except public routes) · **Format:** JSON.

The authoritative, always-current API reference is the **live OpenAPI spec**:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Conventions

- **Success envelope:** `{ "data": <payload>, "meta": <optional> }`
- **Error envelope (RFC-7807-flavoured):**
  ```json
  { "timestamp": "...", "status": 422, "error": "VALIDATION_FAILED",
    "message": "...", "path": "/api/v1/...", "traceId": "...", "fieldErrors": [...] }
  ```
- **Pagination:** `?page=0&size=20` → `{ items, page, size, totalElements, totalPages, hasNext }`
- **Localization:** send `Accept-Language: en` or `fr`.
- **Correlation:** every response carries `X-Trace-Id` (also in error bodies).

## Endpoint groups

| Area | Base path | Auth |
|------|-----------|------|
| Auth | `/auth/**` | public |
| Workspaces & members | `/workspaces`, `/workspaces/{id}/members`, `/invitations` | JWT |
| AI generation | `/workspaces/{id}/ai/**` | JWT (member; generate = editor+) |
| Posts / scheduling / calendar | `/workspaces/{id}/posts/**` | JWT |
| Social accounts | `/workspaces/{id}/social-accounts/**` | JWT (connect = admin+) |
| Billing | `/workspaces/{id}/billing/**`, `/billing/plans` | JWT (owner for checkout/cancel) |
| Payment webhooks | `/billing/webhooks/{stripe,paypal}` | public (signature-verified) |
| Support agents | `/workspaces/{id}/support/agents/**` | JWT (admin+) |
| Support widget (public) | `/support/public/chat` | public |
| Admin | `/admin/**` | JWT + `ROLE_ADMIN` |

## Auth flow

1. `POST /auth/register` → verification email sent.
2. `POST /auth/verify-email` `{ token }` → activates account + bootstraps a personal workspace.
3. `POST /auth/login` `{ email, password }` → `{ accessToken, refreshToken, user }`.
4. Call APIs with `Authorization: Bearer <accessToken>`.
5. On 401, `POST /auth/refresh` `{ refreshToken }` → new rotated token pair.
6. `POST /auth/logout` `{ refreshToken }` → revokes the refresh token.

Password reset: `POST /auth/forgot-password` → `POST /auth/reset-password { token, newPassword }`.
