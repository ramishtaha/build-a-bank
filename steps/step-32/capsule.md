# 🧳 Capsule — Step 32

**Exists now:** 14 Maven modules + shipped SPA. Session = 10-min RS256 access JWT (in-memory `tokenStore`) +
rotating opaque refresh token in httpOnly cookie `bab_refresh` (`Path=/api/auth`, SameSite=Strict, 12 h,
reuse detection revokes family, 3 s race grace → 409). Gateway serves the SPA (nginx container :5175) via a
`Path=/**` catch-all — ONE origin :8080, no CORS in shipped topology. auth 17 tests · gateway 7 ·
demand-account 48 · SPA 29 Vitest + 2 hermetic E2E + 2 full-stack capstone E2E. Tag `step-32-end`.

**This step added:**
- auth: `RefreshTokenService` (SHA-256-at-rest, CAS consume, sealed `RotationResult`), `/refresh` (200/409/401), `/logout`; `ttl-minutes` 30→10
- frontend: `tokenStore` + client single-flight 401→refresh→retry (409 retry); `AuthContext` silent-refresh bootstrap; 3-state `ProtectedRoute`; localStorage GONE (shim deleted from setup.ts)
- MSW cookie-aware refresh/logout handlers + `session.test.ts`; hermetic e2e mocks `/refresh`→401
- bundle: lazy routes + `react-vendor` manualChunks + visualizer → login path 100.3 kB gzip (was 127.8)
- ship: `frontend/{nginx.conf,Dockerfile(node:22.20.0-alpine→nginx:1.28.3-alpine),.dockerignore}`, `deploy/compose.fullstack.yaml` (pg **5433**, redis, redpanda single-listener, spa), gateway catch-all + credentialed CORS + wildcard-origin startup guard
- capstone: `playwright.fullstack.config.ts` + `e2e-fullstack/bank-capstone.spec.ts` (zero mocks, baseURL :8080)
- FIXED real bug: demand-account `GET /api/accounts/{id}` returned `currency: null` since ~Step 13 (MSW masked it) → `accountOf()` + regression test
- ADR-0023; risk register C-10 closes the localStorage debt

**Gotchas:**
- Gateway route order = LIST POSITION ONLY (`order:` attr ignored); catch-all MUST stay last — regression-tested
- Auth route must stay NO-strip or cookie `Path=/api/auth` silently breaks (⚠️ comments both sides)
- Browsers send `Origin` on every POST through the gateway → demand-account needs `APP_CORS_ALLOWED_ORIGINS=http://localhost:8080` in fullstack topology (else 403; curl won't reproduce)
- MSW Node cookie jar: only via `HttpResponse` (native Response drops Set-Cookie); jar survives `resetHandlers()`; proves wiring not browser security
- Duplicate `POST /api/accounts` → 500 (unhandled unique constraint) — logged in CONTRACT-DEBT
- SSE via gateway works by default; `text/event-stream;charset=UTF-8` upstream would silently re-buffer (exact MediaType match)

**Callback hooks:** full local stack = `make fullstack-up` + 4 host services (only demand-account needs env);
capstone command `npm run test:e2e:fullstack`; SPA image rebuild needed after frontend edits
(`compose … up -d --build spa`); auth restart signs everyone out (in-memory store — Redis later).

**Next step starts:** `step-32-end` == `step-33-start`; all green (full 14-module verify + 29 Vitest + 2+2
E2E + smoke 6 gates). Phase F CLOSED 🎖️. Step 33 opens Phase G (DevOps): containerize the Java services —
the SPA's multi-stage Dockerfile is the template.
