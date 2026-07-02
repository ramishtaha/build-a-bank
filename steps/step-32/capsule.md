# 🧳 Capsule — Step 32

**Exists now:** 14 modules + shipped SPA, ONE origin :8080. Session = 10-min access JWT in-memory (`tokenStore`) + rotating opaque refresh token in httpOnly cookie `bab_refresh` (Path=/api/auth, SameSite=Strict, 12 h; reuse→family revoked; 3 s race grace→409). Gateway serves SPA (nginx :5175) via `Path=/**` catch-all. Tests: auth 17 · gateway 7 · demand-account 48 · SPA 29 Vitest + 2 hermetic + 2 full-stack E2E. Tag `step-32-end`.

**This step added:**
- auth: `RefreshTokenService` (SHA-256-at-rest, CAS consume, sealed `RotationResult`), `/refresh` 200/409/401, `/logout`; ttl 30→10 min
- frontend: `tokenStore` + single-flight 401→refresh→retry client (409 retry); silent-refresh bootstrap; 3-state guard; localStorage GONE (shim deleted); MSW cookie-aware handlers + `session.test.ts`
- bundle: lazy routes + `react-vendor` manualChunks + visualizer → login path 100.3 kB gzip (was 127.8)
- ship: `frontend/{nginx.conf,Dockerfile,.dockerignore}` (node:22.20.0-alpine→nginx:1.28.3-alpine), `deploy/compose.fullstack.yaml` (pg **5433**/redis/redpanda single-listener/spa), gateway catch-all + credentialed CORS + wildcard startup guard
- capstone: `playwright.fullstack.config.ts` + `e2e-fullstack/` (zero mocks, baseURL :8080); FIXED real demand-account bug (`GET /api/accounts/{id}` currency:null since ~Step 13 — MSW masked it) + regression test
- ADR-0023; risk C-10 closes the localStorage debt

**Gotchas:**
- Gateway route order = LIST POSITION only (`order:` ignored); catch-all stays LAST — regression-tested
- Auth route must stay NO-strip or cookie Path silently breaks (⚠️ comments both sides)
- Browsers send `Origin` on every POST → demand-account needs `APP_CORS_ALLOWED_ORIGINS=http://localhost:8080` in this topology (curl won't reproduce the 403)
- MSW Node cookie jar: only via `HttpResponse`; survives `resetHandlers()`; proves wiring, not browser security
- Duplicate `POST /api/accounts` → 500 (CONTRACT-DEBT: map to 409 later)

**Callback hooks:** full stack = `make fullstack-up` + 4 host services (only demand-account needs env; see Makefile `fullstack-services`); capstone = `npm run test:e2e:fullstack`; SPA image rebuild after frontend edits (`… up -d --build spa`); auth restart drops all sessions (in-memory store — Redis later).

**Next step starts:** `step-32-end` == `step-33-start`; all green (14-module verify + smoke 6 gates). Phase F CLOSED 🎖️. Step 33 opens Phase G (DevOps): containerize the Java services — the SPA's multi-stage Dockerfile is the template.
