# ADR-0022 — Frontend testing strategy, accessibility & i18n (Step 31)

Date: 2026-07-02 · Status: Accepted

## Context

Step 30 left the SPA's tests mocking the API at the **module** level (`vi.mock('../api/client')`) — fast, but blind to everything between the component and the wire (headers, URLs, serialization, error bodies). Step 31 needs: realistic API mocking, E2E in a real browser, WCAG basics, and i18n + multi-currency formatting — while keeping the suite hermetic (CI-safe, no backend).

## Decision

1. **MSW (Mock Service Worker) at the network layer** for the Vitest suite. Handlers in `src/mocks/handlers.ts` mirror the gateway's real contracts (auth, `/bank` data routes, transfers incl. `Idempotency-Key`); `server.listen({ onUnhandledRequest: 'error' })` turns any unmocked call into a test failure. Module mocks remain only where a browser API (EventSource) — not HTTP — is the boundary.
2. **Playwright for E2E, hermetic by route-mocking.** `playwright.config.ts` boots Vite via `webServer` with `VITE_API_BASE_URL=''` so the app makes same-origin relative calls; specs intercept them with `page.route()` (no CORS, no backend). The full stack behind the gateway stays the Phase-F capstone's job (Step 32). Chromium-only project; browsers verified installed and the suite green in this environment (2/2).
3. **Vitest and Playwright are separate runners:** `e2e/**` excluded from Vitest (`test.exclude`) — Vitest loading a Playwright spec fails the suite (hit and fixed during the step).
4. **Accessibility:** axe-driven checks (`src/a11y.test.tsx`) + labeled controls (explicit label on the account selector, `role="alert"`/`role="status"` on error/success messages) as the enforced baseline.
5. **i18n via react-i18next with synchronous bundled resources** (`src/i18n/i18n.ts`), a `LanguageSwitcher`, and locale reset to `en` in test setup so language-switch tests can't leak. **Money formatting via `Intl.NumberFormat`** (`src/i18n/format.ts`, `src/utils/currency.ts`) — locale + currency come from data, never hardcoded strings.

## Consequences

- Tests exercise real fetch/URL/header behavior; contract drift between frontend and gateway surfaces as failures in one place (handlers).
- E2E proves browser-real flows (forms, SSE notification render, idempotency header) in seconds, without infrastructure; it does NOT prove the deployed chain — Step 32's capstone does.
- Two mock sources (MSW handlers + Playwright routes) mirror the same contract; a shared-fixtures refactor is a noted stretch goal.
- The dev-server E2E bootstrapping makes `npx playwright test` a one-command gate, added to `smoke.sh`.
