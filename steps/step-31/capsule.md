# 🧳 Capsule — Step 31

**Exists now:** 14 Maven modules (unchanged) + `frontend/` SPA with full quality gates: Vitest **24 tests / 10 files** (MSW network-level mocks), **Playwright E2E 2 specs** (real Chromium, hermetic route mocks), axe a11y checks, i18n (en/es) + `Intl.NumberFormat` multi-currency. Tag `step-31-end`.

**This step added:**
- MSW: `src/mocks/{handlers,server}.ts` mirror the gateway contract; `setup.ts` runs `server.listen({onUnhandledRequest:'error'})`
- a11y: `src/a11y.test.tsx` (axe), labeled account selector, `role=alert/status` on messages
- i18n: `src/i18n/{i18n,LanguageSwitcher,format}.ts(x)` — synchronous resources, locale reset in test setup
- Multi-currency: `src/utils/currency.ts` (locale from user, currency from data)
- Playwright: `playwright.config.ts` (webServer + `VITE_API_BASE_URL=''` same-origin trick) + `e2e/transfer-flow.spec.ts` (login→balance→transfer→live SSE; Idempotency-Key asserted)
- `steps/step-31/{smoke.sh,requests.http}`; ADR-0022

**Gotchas:**
- Vitest picks up `e2e/*.spec.ts` unless excluded → `vite.config.ts` `test.exclude: ['e2e/**']`
- SSE route mock: `retry: 600000` line stops EventSource reconnect-hammering after a fulfilled body
- i18n leaks across tests → `i18n.changeLanguage('en')` in `afterEach`

**Callback hooks:** MSW handlers = the frontend's copy of the gateway contract (update both when APIs change); Playwright browsers ARE installed in this sandbox (capability re-checked 2026-07-02); E2E is hermetic by design — full-stack browser flow is Step-32 capstone material.

**Next step starts:** `step-31-end` == `step-32-start`; all green (backend 14-module verify + npm build/lint/test/e2e + smoke.sh). Step 32 = token refresh + route-guard hardening (JWT out of localStorage), bundle/perf, Dockerize SPA + serve via gateway, deploy, Phase-F capstone.
