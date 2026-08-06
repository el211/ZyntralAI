# Social OAuth — LinkedIn (wired end-to-end)

LinkedIn is connected via a real OAuth 2.0 authorization-code flow. The same port
(`SocialOAuthProvider`) lets other networks be added without touching the flow.

## Flow

```
 SPA  ── GET /workspaces/{id}/social-accounts/connect/linkedin ──▶ backend
 backend: requireAtLeast(ADMIN); store random `state` → {workspace,user} in Redis (10 min, single-use)
 backend ── { authorizationUrl } ──▶ SPA ── full-page redirect ──▶ LinkedIn consent
 LinkedIn ── 302 ?code&state ──▶ GET /api/v1/social/oauth/callback/linkedin   (PUBLIC, state-secured)
 backend: validate+consume state → exchange code for tokens → GET /v2/userinfo (member id)
          → upsert SocialAccount (tokens encrypted at rest) → 302 ──▶ SPA /dashboard/social?connected=linkedin
```

Publishing uses the stored token: `POST https://api.linkedin.com/rest/posts`
(`LinkedInPublisher`), gated on the `w_member_social` scope.

## Setup

1. Create an app at <https://www.linkedin.com/developers/apps>.
2. Add the **Sign In with LinkedIn using OpenID Connect** and **Share on LinkedIn**
   products (scopes: `openid profile email w_member_social`).
3. Register the redirect URL:
   `${SOCIAL_REDIRECT_BASE_URL}/api/v1/social/oauth/callback/linkedin`
   (locally `http://localhost:8080/api/v1/social/oauth/callback/linkedin`).
4. Set env vars:
   ```
   LINKEDIN_CLIENT_ID=...
   LINKEDIN_CLIENT_SECRET=...
   SOCIAL_REDIRECT_BASE_URL=http://localhost:8080   # public backend base in prod
   WEB_URL=http://localhost:3000                    # SPA base for the post-connect redirect
   ```
5. In the app: **Settings → Social accounts → Connect** (LinkedIn), approve, and you're
   redirected back connected. Create a post targeting that account and **Publish now**.

## Security notes

- The callback is public (the browser arrives without a JWT) but is bound to the initiating
  workspace+user by a **server-side, single-use `state`** token in Redis — a leaked/forged
  callback can't attach an account to someone else's workspace.
- Access tokens are **encrypted at rest** (AES-256-GCM) via `StringCryptoConverter`.
- Connect/disconnect require **ADMIN+** in the workspace.

## Adding another network

Implement `SocialOAuthProvider` (authorization URL + code exchange + profile) and a
`SocialPublisher.doPublish`; register the platform in `OAUTH_ENABLED` on the frontend. No
changes to the flow, controller, or content module.
