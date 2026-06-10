# ADR-0021: Frontend state, data & forms — TanStack Query, React Hook Form + Zod, and SSE live updates

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 30 — Frontend pt.2 (state, data & forms); Phase F

## Context
Step 29 gave the SPA auth + routing. Step 30 makes it *do* things: read account data, move money, and react to
live events — the curriculum's "data fetching/caching (TanStack Query), forms + validation (React Hook Form +
Zod), loading/error UX, WebSocket live updates." It consumes the existing backend (demand-account `/bank/**`,
auth, and the notification SSE stream) through the gateway.

## Decision

### 1. Server state via TanStack Query (not component state / useEffect)
Account balance + ledger are **queries** (`useAccount`, `useEntries`) — TanStack Query owns caching, loading/error
flags, and refetch. A transfer is a **mutation** (`useTransfer`) whose `onSuccess` **invalidates** the
`['account', …]` and `['entries', …]` query keys, so the balance + history refetch automatically with no manual
wiring. This is the standard separation: *server* state in the query cache, *UI* state in React. `react-query`
5.101 (queryClient retry kept on in the app; **off in tests** so error states surface immediately).

### 2. Forms via React Hook Form + Zod (one typed schema = validation + types)
The transfer form uses **react-hook-form** for state/submission and **Zod** (`@hookform/resolvers/zod`) for
validation: `from`/`to` required, `amount` a positive number (`z.coerce.number().positive()` — the number input
yields a string, coerced + validated in one place). `useForm` infers its types from the resolver (avoids the
coerce input/output generic friction). Field errors render with `role="alert"`; the submit fires the mutation
with a fresh `crypto.randomUUID()` **Idempotency-Key** (retries never double-pay — Step 21's guarantee, now
honoured by the UI).

### 3. Live updates via SSE through the gateway
The notification service (Step 20) pushes `transfer` events over `text/event-stream`. A `useNotificationStream`
hook holds one `EventSource` and accumulates the latest events; `LiveNotifications` renders them. **SSE, not
WebSocket:** the bank's push is one-way (server→client), so SSE is the simpler, auto-reconnecting fit (matches
the backend). We **route the notification stream through the gateway** (`/notifications/**` → notification:8084,
StripPrefix) so the SPA stays on **one origin** and the gateway's Step-29 CORS covers it. (`EventSource` can't
send an `Authorization` header anyway, and notification has no app-auth — R-002 — so direct browser access is
acceptable; routing it via the gateway keeps the single-front-door story.)

### 4. The single front door now fronts four services
Added the gateway `notification` route (tested in `GatewayRoutingTest` → 5 tests). The SPA's one base URL
(`VITE_API_BASE_URL`) reaches auth (`/api/auth/**`), cif (`/cif/**`), demand-account (`/bank/**`), and the
notification stream (`/notifications/**`).

### 5. Testing: Vitest + Testing Library, API + EventSource mocked (MSW still Step 31)
A `renderWithProviders` helper wraps components in a retry-off QueryClient + MemoryRouter + AuthProvider.
`AccountPanel` test: loading → data, and an error path. `TransferForm` test: Zod blocks a bad submit (API not
called) + a valid submit fires `transfer` with the typed values + a UUID key. `useNotificationStream` test: a
**controllable `EventSource`** (jsdom has none) emits a `transfer` event → the hook accumulates it. The client
test gains `getAccount`/`transfer` request-shape + ProblemDetail-parsing cases. **15 tests** total.

## Consequences
- ✅ The SPA reads real account data, submits idempotent transfers (cache auto-refreshes), and shows live events.
- ✅ ProblemDetail (RFC 9457) bodies surface as human messages ("Insufficient funds") in the UI.
- ✅ One origin (the gateway) for REST + SSE; CORS handled once.
- ⚠️ The **live browser** flow (incl. SSE streaming through the servlet gateway, which may buffer) is
  verify-adjacent — no browser in the sandbox; verified via the controllable-EventSource unit test + the gateway
  routing test instead (§12.8). For a live demo, demand-account needs `APP_CORS_ALLOWED_ORIGINS` and the SSE may
  need the gateway's response buffering disabled.
- ⚠️ No account↔user mapping yet — the dashboard uses a typed account-number selector (default ACC-A).
- ⚠️ JWT still in localStorage (XSS) — hardened in Step 32; the one benign ESLint react-refresh warning remains.
- 🔁 Step 31 (Testing Library deepening + Playwright E2E + **MSW** + a11y + i18n/multi-currency), Step 32 (token refresh, route-guard hardening, bundle perf, Dockerize + serve via the gateway).
