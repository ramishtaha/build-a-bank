# 🧳 Capsule - Step 29

**Exists now:** Backend complete through Phase E — 14 Maven modules, full `./mvnw verify` BUILD SUCCESS, quality gates green. New `frontend/` React 19 + TS + Vite 6 SPA (npm, committed `package-lock.json`): `npm run build` (tsc+vite, 46 modules), `npm run lint` (ESLint 9, 1 benign react-refresh warning, exit 0), `npm test` → 7 tests (client 3, LoginPage 2, ProtectedRoute 2; API mocked). Gateway (:8080) fronts auth (:8083), cif (:8081), demand-account (:8082); `GatewayRoutingTest` → 4 tests. SPA dev server :5173.

**This step added:**
- `frontend/` SPA: typed `api/client.ts` (base URL = gateway via `VITE_API_BASE_URL`, default :8080), `AuthContext` (JWT login/logout, persisted in localStorage key `bab.token`), `ProtectedRoute` guard, LoginPage + DashboardPage, BrowserRouter route table.
- Gateway `auth` route `/api/auth/**` with **no StripPrefix** (auth's paths already start with `/api/auth`).
- `GatewayCorsConfig` `CorsFilter`, deny-by-default via `app.security.cors.allowed-origins` (dev default `http://localhost:5173`; empty ⇒ deny all); allowed headers: Authorization, Content-Type, Idempotency-Key.
- 3 new `GatewayRoutingTest` tests (auth no-strip; CORS allow → ACA-Origin; CORS deny → 403).
- Vitest + Testing Library suite (7 tests) + `src/test/setup.ts` localStorage shim; `steps/step-29/smoke.sh`; ADR-0020.

**Gotchas:**
- jsdom's localStorage getter throws for an opaque origin → Vitest leaves the global `undefined`; fixed by an in-memory shim in `src/test/setup.ts` + jsdom url `http://localhost:5173` in `vite.config.ts`.
- `fetch` does not throw on 4xx/5xx — the client checks `response.ok` and throws `ApiError(status)`.
- `APP_CORS_ALLOWED_ORIGINS` is consumed by the **gateway** (and demand-account), not the auth service.
- One benign ESLint `react-refresh` warning on AuthContext.tsx (DX-only; lint exits 0).
- §12.8: no browser in the sandbox — the live browser flow is verify-adjacent; headless CORS preflight verified instead.
- JWT in localStorage is XSS-exposed — a teaching simplification, hardened in Step 32.

**Callback hooks:**
- Token key `bab.token` in localStorage; SPA's only env knob is `VITE_API_BASE_URL` (build-time, `VITE_`-prefixed only).
- API mocked at the module seam for now — MSW arrives Step 31; TanStack Query + RHF/Zod arrive Step 30 (CORS already allows `Idempotency-Key`).
- §12.3 proof: disabling the ProtectedRoute guard made the redirect test fail (rendered protected content), then reverted.

**Next step starts:** `step-29-end == step-30-start`. Green: npm build/lint/test (7 tests), `GatewayRoutingTest` (4 tests), full `./mvnw verify` (14 modules), `smoke.sh` PASSED, clean-room `npm ci`.
