# 🧳 Capsule - Step 30

**Exists now:** 14 Maven modules (`./mvnw verify` BUILD SUCCESS, gates green). Gateway :8080 is the single front door for 4 services: auth :8083, cif, demand-account :8082 (`/bank/**`), notification :8084 (`/notifications/**`). SPA `frontend/` (React+TS+Vite, :5173): login/AuthContext (Step 29) + Query-cached dashboard. Frontend: 15 Vitest tests in 6 files green; gateway: `GatewayRoutingTest` 5 tests green.

**This step added:**
- TanStack Query: `useAccount`/`useEntries` (queries, token-gated via `enabled`) + `useTransfer` (mutation → invalidates `['account']`/`['entries']`) in `frontend/src/accounts/queries.ts`.
- `AccountPanel` (loading/error/data; balance + ledger) and `TransferForm` = RHF + Zod (`z.coerce.number().positive()`, fresh UUID Idempotency-Key).
- SSE: `useNotificationStream` (EventSource → `transfer` events) + `LiveNotifications`.
- API client: `getAccount`/`listEntries`/`transfer` (`/bank` prefix, Bearer, ProblemDetail parsed).
- Gateway `/notifications/**` route (StripPrefix) → notification:8084.
- Tests: 15 Vitest (API + EventSource mocked) + `test/renderWithProviders.tsx` + `setup.ts` EventSource stub; §12.3 = dropped Zod `to` rule → TransferForm test FAILED → reverted.
- ADR-0021. Deps pinned: react-query 5.101 / RHF 7.78 / zod 3.25.76 / resolvers 3.10.

**Gotchas:**
- jsdom has no `EventSource` — no-op stub in `src/test/setup.ts`, controllable one in the hook test.
- Number `<input>` yields a string → `z.coerce.number()`; let `useForm` infer types from the resolver (explicit `z.infer` fights coerce in/out types).
- `EventSource` can't send custom headers (no Bearer) — stream is non-user-scoped by design.
- Live browser flow is verify-adjacent (§12.8): SSE through the servlet gateway may buffer; demand-account needs `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173`.
- JWT still in localStorage (hardened Step 32). Lint: 1 benign react-refresh warning on `AuthContext.tsx`.

**Callback hooks:** mutation → invalidate `['account']`/`['entries']` is the cache-coherence pattern Step 31 refines (optimistic updates); Step 31 adds Playwright E2E + MSW + a11y + i18n; Step 32 hardens token storage/refresh + Dockerizes the SPA behind the gateway.

**Next step starts:** `step-30-end == step-31-start`. Green: `npm run build` (tsc+vite, 111 modules) / lint / 15 tests, `GatewayRoutingTest` (5), full `./mvnw verify` (14 modules), `smoke.sh` PASSED, clean-room `npm ci`.
