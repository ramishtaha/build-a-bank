# ADR-0023 — Frontend session hardening (rotating refresh cookie) and shipping the SPA behind the gateway

Date: 2026-07-03 · Status: Accepted · Step: 32 (Phase-F capstone)

## Context

Since Step 29 the SPA kept its JWT in `localStorage` — a deliberately carried teaching debt (risk register:
JWT-in-localStorage, XSS-exposed; hardening promised for Step 32). Separately, the SPA only existed as a Vite
dev server; "ship it" was unresolved: where do the static files live, who serves them, and how does the
browser reach the APIs without CORS gymnastics?

## Decision

**1. Session = short-lived access JWT in memory + opaque rotating refresh token in an httpOnly cookie.**

- Access JWT (RS256, unchanged validation path) TTL **30 → 10 min**; it lives ONLY in a module-private
  variable (`tokenStore`) — never in `localStorage`/`sessionStorage`/a readable cookie. Reload loses it by
  design; a **silent refresh** on app mount restores the session from the cookie.
- Refresh token: **opaque 256-bit random value**, not a JWT — its meaning is server-side state
  (`RefreshTokenService`, in-memory map keyed by SHA-256 of the token; prod would use Redis/DB). That state
  is what enables **revocation** (logout kills the family), **rotation** (every `/refresh` consumes the
  token and issues a successor in the same login family), and **reuse detection** (a token replayed after
  its consumption revokes the entire family — fail closed; the thief AND the victim are signed out).
- **Concurrent-rotation grace (3 s, configurable):** all tabs share one cookie, so two tabs refreshing
  simultaneously would look like theft and nuke the session (a guaranteed logout-loop on browser session
  restore — caught in adversarial design review). Within the grace window a replay returns **409** without
  revocation; the client retries once with the (already-rotated) cookie it now holds. Outcomes are a sealed
  `RotationResult` (Rotated / ConcurrentRotation / Invalid) so the controller's mapping is compiler-checked.
- Cookie attributes: `HttpOnly; SameSite=Strict; Path=/api/auth; Max-Age=12h; Secure` per config (false only
  for plain-http local dev). `Path=/api/auth` works ONLY because the gateway's auth route does not strip the
  prefix — commented as load-bearing in both places.
- **Honest scope:** this defeats credential *theft/persistence*. An XSS payload running in the live page can
  still call `/refresh` and act as the user while the page is open — the defenses for that are CSP, output
  encoding, and dependency hygiene, not storage choice. Logout also cannot un-sign already-issued access
  JWTs (stateless); the 10-min TTL bounds that window.
- Client plumbing: the API layer owns the token (components never see it); 401 → single-flight refresh →
  one retry; refresh failure → session-expired event → AuthContext renders anonymous; `ProtectedRoute` holds
  ('initializing') during bootstrap instead of bouncing reloads to /login.

**2. Ship the SPA as a container, served through the gateway (single origin).**

- `frontend/Dockerfile`: multi-stage — `node:22.20.0-alpine` builds (`VITE_API_BASE_URL=""` ⇒ same-origin
  relative fetches), `nginx:1.28.3-alpine` serves (immutable-cache hashed assets, `no-cache` index.html,
  `try_files` SPA fallback for deep links). Pinned tags + digests in VERSIONS.md.
- Gateway gains a **catch-all route** (`Path=/**` → SPA container) placed **last** — position is the ONLY
  ordering mechanism in the MVC gateway (the `order` attribute is ignored; guarded by a route-order
  regression test). One origin ⇒ no CORS in production and `SameSite=Strict` simply works.
- Dev CORS (5173 → 8080) now requires `allowCredentials(true)`; a wildcard origin with credentialed CORS
  would let any website read `/refresh`'s access-token response, so the gateway **fails startup on `*`**.
- Full local deployment: `deploy/compose.fullstack.yaml` (Postgres on host-port 5433, Redis, single-listener
  Redpanda, SPA build) + the four services on the host. Real cloud/CDN stays verify-adjacent (CAPABILITIES).

**3. Capstone verification is part of the contract.** A separate Playwright config (`e2e-fullstack/`, no
mocks, `baseURL` = the gateway) drives login → live balance → transfer → outbox → Kafka → SSE → UI, plus
reload-survives-via-silent-refresh. It immediately caught a real bug the MSW mocks had masked since Step 30
(`GET /api/accounts/{id}` returned `currency: null`) and a proxied-CORS 403 (browsers send `Origin` on every
POST; the gateway forwards it; demand-account's deny-by-default CORS must allow the public origin).

## Alternatives considered

- **Access token in an httpOnly cookie too** (pure-cookie auth): removes the in-memory token but makes every
  API call cookie-authenticated → full CSRF surface on all mutating endpoints (double-submit tokens or
  SameSite-everywhere), and cross-service `Authorization: Bearer` (Steps 17–23) would need reworking. The
  bearer-token model stays; only the *session anchor* moves to the cookie.
- **Refresh token as a JWT**: self-contained but unrevocable — the whole point here is server-side state.
- **BroadcastChannel/Web Locks cross-tab single-flight**: solves the two-tab race client-side; heavier to
  teach and still doesn't cover two *browsers*. The server-side grace is smaller and universal. Noted as a
  production enhancement.
- **nginx as the front door** (proxying APIs): inverts the course's gateway-is-the-front-door architecture
  (ADR-0007); rejected.

## Consequences

- The Step-29 localStorage debt is retired (risk register updated); the jsdom localStorage test shim is gone.
- Auth service is now stateful (refresh store) — restart signs everyone out (acceptable locally; Redis in a
  later phase would fix it), and horizontal scaling needs a shared store.
- Route list order in `gateway/application.yml` is load-bearing (regression-tested).
- MSW handlers mirror the new contract (cookie-aware refresh/logout); the suite proves wiring, not browser
  cookie security — that claim belongs to the backend wire tests + the full-stack browser run only.
