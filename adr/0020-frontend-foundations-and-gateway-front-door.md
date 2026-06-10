# ADR-0020: React + TypeScript + Vite frontend (Phase F opener); the gateway as the single front door

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 29 — Frontend pt.1 (foundations); **opens Phase F** (full-stack frontend, Steps 29–32)

## Context
Phase F adds the bank's UI. Step 29 is foundations: a React + TypeScript + Vite SPA with routing and the
login/auth flow against the existing backend (the `auth` service from Step 16 + the gateway from Step 15). This
is a deliberate **stack shift** (JVM → Node/TS) — the first non-Maven build in the repo. Node 22.20.0 / npm
11.16.0 are present (CAPABILITIES.md, verified at Step 0), and the npm registry is reachable, so the SPA is
**fully buildable and testable here** (not verify-adjacent) — except a real *browser* cross-origin call, which
the sandbox can't run.

## Decision

### 1. Stack: React 19 + TypeScript (strict) + Vite 6, tested with Vitest + Testing Library
Pinned via `package.json` ranges + a **committed `package-lock.json`** (the real pin — `npm ci` reproduces it
exactly, satisfying §12.6). React 19.2, react-router-dom 7, Vite 6.4, Vitest 3.2, Testing Library 16, ESLint 9
(flat config). TanStack Query + React Hook Form + Zod arrive in Step 30; Playwright + MSW in Step 31; hardening
(token refresh, bundling, Dockerize) in Step 32 — so Step 29 stays foundational on purpose.

### 2. The gateway is the single front door — extended to front `auth`
The SPA has ONE base URL (`VITE_API_BASE_URL`, default the gateway `http://localhost:8080`). The gateway
(Step 15) routed `/cif/**` and `/bank/**` but **not** auth. We added an `auth` route: `Path=/api/auth/**` →
`${services.auth.uri:http://localhost:8083}` with **no StripPrefix** (auth's paths already begin `/api/auth`),
so `POST /api/auth/login` and `GET /api/auth/me` flow through the gateway unchanged. This keeps the SPA on a
single origin and matches the BFF/front-door architecture (§5).

### 3. Gateway CORS — deny-by-default, env-configurable
A browser SPA on `http://localhost:5173` calling the gateway is cross-origin, so the gateway needs CORS. Added a
standard servlet `CorsFilter` bean (`GatewayCorsConfig`) reading `app.security.cors.allowed-origins`
(deny-by-default, same posture as demand-account/Step 18; dev default = the Vite origin; override
`APP_CORS_ALLOWED_ORIGINS`). Verified headlessly: an allowed-origin preflight gets
`Access-Control-Allow-Origin`; a disallowed origin gets **403** with none. (A real browser flow is
verify-adjacent — no browser in the sandbox — §12.8.)

### 4. Auth flow: JWT in an AuthContext, persisted, behind a route guard
`AuthContext` holds the token + current user, exposes `login()`/`logout()`, and persists the token in
`localStorage` (so a refresh stays signed-in). `ProtectedRoute` redirects unauthenticated users to `/login`.
The login response field is `token` (+ `expiresInSeconds`) — matched exactly to the auth contract. **Honest
note:** `localStorage` JWT storage is the simplest teaching choice but is XSS-exposed; httpOnly-cookie / in-memory
+ refresh-token trade-offs are taught and hardened in Step 32.

### 5. Quality gate: ESLint (flat config) — the SPA's Spotless/Checkstyle
`npm run lint` (eslint 9 flat config: JS + typescript-eslint + react-hooks). One benign
`react-refresh/only-export-components` **warning** on `AuthContext.tsx` (it exports the provider component
*and* the `useAuth` hook) — a DX-only fast-refresh hint that the official Vite template also carries; lint exits
0. Kept the auth file cohesive for learner clarity rather than split it to silence a non-error.

### 6. Testing: Vitest + Testing Library, with the API mocked (MSW deferred to Step 31)
`client.test.ts` stubs global `fetch` to assert the request shape; `LoginPage.test.tsx` mocks the `api/client`
module to assert form→auth wiring + error UX; `ProtectedRoute.test.tsx` drives auth via `localStorage` to assert
redirect vs render. **jsdom gotcha (documented):** jsdom's `localStorage` getter throws for an opaque origin, so
Vitest leaves the global `undefined`; we install a tiny in-memory `localStorage` in `src/test/setup.ts`
(deterministic, isolated). §12.3: disabling the guard made the redirect test fail (rendered the protected
content) — reverted.

## Consequences
- ✅ A real, buildable, tested SPA: `npm run build` (tsc + vite) + `npm run lint` + `npm test` (7 tests) all green.
- ✅ The gateway is genuinely the single front door (auth + cif + demand-account) with deny-by-default CORS.
- ✅ Reproducible via the committed lockfile (`npm ci`); the frontend is its own build, separate from `./mvnw`.
- ⚠️ A live *browser* cross-origin flow is verify-adjacent (no browser in sandbox) — headless CORS preflight is verified instead.
- ⚠️ `localStorage` token storage is a teaching simplification (XSS-exposed) — hardened in Step 32.
- 🔁 Step 30 (TanStack Query + RHF/Zod forms + live WebSocket), Step 31 (Testing Library/Playwright/MSW + a11y/i18n), Step 32 (token refresh, bundling, Dockerize + serve via gateway).
