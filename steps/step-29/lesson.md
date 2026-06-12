# Step 29 · Frontend pt.1 — React + TypeScript + Vite Foundations (login & routing)
### Phase F — Full-Stack Frontend 🔵 · Step 29 of 67 · 🎬 opens Phase F

> *The backend is done through Phase E; now the bank gets a face. Step 29 lays the **frontend foundations**: a
> **React + TypeScript + Vite** SPA that talks to the **gateway** (the single front door), with **client-side
> routing**, a **login/auth flow** against the auth service (the JWT you built in Step 16), and a **route
> guard**. We make the gateway genuinely the one front door (it now fronts `auth` too, with CORS), and we test
> the SPA with **Vitest + Testing Library**. This is a deliberate stack shift — JVM → Node/TypeScript.*

---

<a id="toc"></a>
## 🧭 The Six Movements of This Step

| | Movement | What happens |
|---|---|---|
| **A** | [🧭 Orient](#orient) | 30-second overview · skip-test · cheat card · why it matters · before you start |
| **B** | [🧠 Understand](#understand) | SPA + Vite/ESM · client-side routing · the auth flow (JWT, context, guard) · the gateway as front door |
| **C** | [🛠️ Build](#build) | scaffold Vite+React+TS · the API client · AuthContext · routes + guard · login page · gateway auth route + CORS · tests |
| **D** | [🔬 Prove](#prove) | the Verification Log — build/lint/test green; §12.3 break the guard; gateway CORS preflight; real output |
| **E** | [🎓 Apply](#apply) | go deeper · interview prep · your-turn challenges |
| **F** | [🏆 Review](#review) | troubleshooting (jsdom localStorage, CORS) · resources · recap, flashcards & what's next |

---

<a id="orient"></a>

# A · 🧭 Orient

## 📋 This Step in 30 Seconds

| | |
|---|---|
| **Title** | React + TypeScript + Vite foundations — routing, calling the gateway, and the login/auth flow |
| **Step** | 29 of 67 · **Phase F — Full-Stack Frontend** 🔵 · **opens Phase F** |
| **Effort** | ≈ 16 hours focused. A stack shift (JVM → Node/TS) + a new build tool (Vite) + the auth flow. |
| **What you'll run this step** | **Node 22 + npm** for the SPA (`npm install/build/lint/test` — no Docker). To see it live end-to-end you also run the **gateway + auth** (JVM); a real browser flow is the one thing the course's sandbox can't self-verify. |
| **Buildable artifact** | `frontend/` — a Vite + React 19 + TS SPA: a typed **API client** (base URL = the gateway), an **AuthContext** (JWT login/logout, persisted), **react-router** routes with a **`ProtectedRoute`** guard, a **LoginPage** + **DashboardPage**, and **Vitest + Testing Library** tests. Plus the **gateway** now fronts `auth` (`/api/auth/**`) with **CORS**. `step-29-start == step-28-end`. |
| **Verification tier** | 🔴 **Full** — a new app + a gateway (build) change. `npm run build` + `npm run lint` + `npm test` green; the gateway's new route + CORS tested; a §12.3 (break the guard → a test fails); full `./mvnw verify` still green; clean-room `npm ci`. |
| **Depends on** | **[Step 16](../step-16/lesson.md)** (the auth service / JWT login), **[Step 15](../step-15/lesson.md)** (the gateway), **[Step 18](../step-18/lesson.md)** (CORS posture). |

By the end you'll **scaffold a Vite React-TS app**, **route** client-side, implement a **JWT login flow** with a **route guard**, call an API through a typed client, and **test** components/routes with Vitest + Testing Library.

### ⏭️ Can You Skip This Step? (5-minute self-check)

If you can confidently do **all** of this, skim 🛠️ Build and jump to **[Step 30 — state, data & forms](../step-30/lesson.md)**.

- [ ] I can scaffold and explain a **Vite + React + TypeScript** project (dev server, build, why Vite is fast).
- [ ] I can set up **client-side routing** (public vs protected routes) and a **route guard**.
- [ ] I can implement a **JWT login flow**: call the API, store the token, attach it, expose auth via context.
- [ ] I can test a component and a route with **Vitest + Testing Library** (and mock the API).
- [ ] I can explain why the SPA talks to **one origin (the gateway)** and what **CORS** is doing.

> [!TIP]
> Not 100%? Stay. "Walk me through your auth flow on the frontend" and "how do you protect routes / store a JWT" are standard full-stack interview questions — you'll have built it.

## 📇 Cheat Card

> **What this step delivers (one sentence):** a React+TS SPA that logs in against the gateway-fronted auth service, stores the JWT, and guards routes — built and tested with Vite + Vitest.

**Key commands** (run in `frontend/`; or `npm --prefix frontend <script>`):

```bash
npm install            # one-time; writes package-lock.json (the version pin)
npm run dev            # Vite dev server on http://localhost:5173
npm run build          # tsc typecheck + vite production build → dist/
npm run lint           # ESLint (the SPA's quality gate)
npm test               # Vitest + Testing Library
bash steps/step-29/smoke.sh
```

**The headline — one origin, a guarded route, a JWT in context:**

```
  Browser (SPA :5173) ──fetch──> Gateway (:8080, single front door + CORS)
     LoginPage → AuthContext.login()  → POST /api/auth/login  → {token, expiresInSeconds}
     (token saved) → GET /api/auth/me → {username, roles}     → DashboardPage
     ProtectedRoute: no token? → <Navigate to="/login">
```

**The one sentence to remember:** *The SPA holds the JWT in an AuthContext (persisted), sends it to the gateway as `Authorization: Bearer …`, and a `ProtectedRoute` redirects anyone without a token to /login.*

## 🎯 Why This Matters

A backend without a UI is invisible to most stakeholders. The frontend is where auth, routing, and API integration become real — and "show me how you handle login and protected routes in React" is a near-universal full-stack interview question. Vite + TypeScript + Testing Library is the modern default stack, and wiring the SPA to a single gateway origin (with CORS) is exactly how real systems are structured.

## ✅ What You'll Be Able to Do

- Scaffold and build a Vite + React + TypeScript SPA.
- Implement client-side routing with a protected-route guard.
- Build a JWT login flow (typed API client + auth context + persistence).
- Test components and routes with Vitest + Testing Library.

## 🧰 Before You Start

- **Prereqs:** Node 22 + npm (`node -v`, `npm -v`). The bank's `auth` + `gateway` build green (`git describe` → `step-28-end`). No Docker for the SPA itself.
- **Connects to what you know:** the SPA calls the **auth** service (Step 16's JWT) through the **gateway** (Step 15); CORS echoes Step 18's deny-by-default posture.
- **Depends on:** Steps **16, 15, 18**.

---

<a id="understand"></a>

# B · 🧠 Understand

## 🧠 The Big Idea — a single-page app served by Vite, talking to one front door

A **single-page app (SPA)** loads one HTML shell + a JS bundle; **client-side routing** swaps views without full
page reloads, and data comes from API calls. **Vite** is the build tool: in dev it serves your source as native
**ES modules** (no bundling → near-instant start and hot updates); for production it bundles with Rollup/esbuild.
**TypeScript** gives you types across the whole app (the API responses, the auth state) — the frontend
counterpart to the type-safety you rely on in Java.

The SPA talks to **one origin — the gateway** (Step 15), which routes to `auth`, `cif`, and `demand-account`
behind it. One origin means one base URL and one CORS policy, instead of the browser juggling four.

```mermaid
flowchart LR
    subgraph Browser["Browser · SPA (Vite dev :5173)"]
      LP["LoginPage"] --> AC["AuthContext\n(token + user)"]
      PR["ProtectedRoute"] --> DP["DashboardPage"]
      AC --> API["api/client.ts\n(fetch, one base URL)"]
    end
    API -- "Bearer JWT" --> GW["Gateway :8080\n(front door + CORS)"]
    GW --> AUTH["auth :8083"]
    GW --> CIF["cif :8081"]
    GW --> DA["demand-account :8082"]
```

## 🧩 Pattern Spotlight — the auth flow (context + guard)

1. **LoginPage** collects credentials and calls `AuthContext.login()`.
2. **AuthContext** calls `POST /api/auth/login` → gets `{token, expiresInSeconds}`, saves the token
   (`localStorage`, so a refresh stays signed in), then `GET /api/auth/me` for the username/roles.
3. **ProtectedRoute** reads `isAuthenticated` from context; no token → `<Navigate to="/login" replace />`.
4. Authenticated API calls attach `Authorization: Bearer <token>`.

Context is React's built-in dependency injection — one provider at the top, any component reads it via the
`useAuth()` hook. (This is the same "depend on an abstraction, inject it" idea as Spring's DI.)

## 🌱 Under the Hood: why the gateway needed two changes

The gateway already routed `/cif/**` and `/bank/**`, but **not** auth — and had **no CORS**. For the SPA we
added:
- an **`auth` route**: `Path=/api/auth/**` → the auth service, with **no `StripPrefix`** (auth's own paths
  already start with `/api/auth`, so stripping would break them);
- a **`CorsFilter`** (deny-by-default, `app.security.cors.allowed-origins`) so the browser's preflight from
  `http://localhost:5173` is answered with `Access-Control-Allow-Origin` (and any other origin gets 403).

## 🛡️ Security Lens: where do you put a JWT?

We store the JWT in `localStorage` — simplest to teach, but **readable by any script on the page, so it's
exposed to XSS**. The hardened alternatives (an **httpOnly cookie** the JS can't read, or an **in-memory** access
token + a refresh-token rotation) come in **Step 32**. The CORS allow-list is **deny-by-default** (Step 18): only
the origins you name can call the gateway from a browser.

## 🕰️ Then vs. Now

- **Build tooling:** Create-React-App (Webpack, slow) → **Vite** (native ESM in dev, esbuild/Rollup for prod).
- **Routing:** react-router's APIs evolved; v7 uses `<Routes>/<Route>` (and data routers) — we use the simple
  declarative form for foundations.
- **React:** function components + hooks (since 16.8); React 19 adds more, but the foundations here are stable.
- **Testing:** Enzyme → **Testing Library** (test what the user sees/does, not internals) + **Vitest** (Vite-native,
  Jest-compatible API).

---

# B→C bridge: 🌳 files we'll touch

```
frontend/
  package.json · tsconfig.json · vite.config.ts · eslint.config.js · index.html · .env.example
  src/
    main.tsx                  BrowserRouter → AuthProvider → App
    App.tsx                   the route table (/login public, / protected)
    api/client.ts             typed fetch wrapper (one base URL = the gateway) + login/getCurrentUser
    auth/AuthContext.tsx      JWT state, login()/logout(), useAuth() — persisted in localStorage
    auth/ProtectedRoute.tsx   redirect to /login when unauthenticated
    pages/LoginPage.tsx       the sign-in form
    pages/DashboardPage.tsx   the protected landing page (greets the user)
    test/setup.ts             jest-dom matchers + a localStorage shim
    **/*.test.ts(x)           Vitest + Testing Library
gateway/  (+ auth route /api/auth/**, + GatewayCorsConfig CorsFilter)
```

<a id="build"></a>

# C · 🛠️ Let's Build It — Step by Step

## 📦 Your Starting Point

`step-29-start == step-28-end`. The backend is complete through Phase E. There is no `frontend/` yet; the gateway fronts cif + demand-account but not auth.

## Sub-step 1 — scaffold Vite + React + TypeScript

🎯 **Goal:** Scaffold the Vite + React + TypeScript SPA foundations (`package.json`, TS config, Vite config, ESLint flat config, `index.html`).
📁 **Path:** `frontend/` (`package.json`, `tsconfig.json`, `vite.config.ts`, `eslint.config.js`, `index.html`)
⌨️ **Code:**
```json
{
  "name": "build-a-bank-frontend",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "description": "Build-a-Bank ΓÇö React + TypeScript SPA (Phase F, Step 29). Talks to the gateway (single front door).",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint .",
    "test": "vitest run"
  },
  "dependencies": {
    "react": "^19.1.0",
    "react-dom": "^19.1.0",
    "react-router-dom": "^7.6.0"
  },
  "devDependencies": {
    "@eslint/js": "^9.17.0",
    "@testing-library/dom": "^10.4.0",
    "@testing-library/jest-dom": "^6.6.3",
    "@testing-library/react": "^16.1.0",
    "@testing-library/user-event": "^14.5.2",
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "@vitejs/plugin-react": "^4.3.4",
    "eslint": "^9.17.0",
    "eslint-plugin-react-hooks": "^5.1.0",
    "eslint-plugin-react-refresh": "^0.4.16",
    "globals": "^15.14.0",
    "jsdom": "^25.0.1",
    "typescript": "^5.7.2",
    "typescript-eslint": "^8.18.0",
    "vite": "^6.0.5",
    "vitest": "^3.0.0"
  }
}
```
```json
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "useDefineForClassFields": true,
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noEmit": true,
    "skipLibCheck": true,
    "esModuleInterop": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "verbatimModuleSyntax": true,
    "types": ["vitest/globals"]
  },
  "include": ["src", "vite.config.ts", "eslint.config.js"]
}
```
```ts
/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// Step 29 ┬╖ Vite config + Vitest (jsdom) for component/route tests. The dev server runs on 5173 (the origin
// the gateway's CORS allow-list expects). Tests run in jsdom with Testing Library matchers from src/test/setup.ts.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    // A real (non-opaque) origin so jsdom exposes localStorage ΓÇö about:blank's opaque origin leaves the
    // localStorage getter throwing, which Vitest skips, making the global `undefined` (AuthContext needs it).
    environmentOptions: {
      jsdom: { url: 'http://localhost:5173' },
    },
    setupFiles: './src/test/setup.ts',
    css: false,
  },
});
```
```javascript
import js from '@eslint/js';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import globals from 'globals';
import tseslint from 'typescript-eslint';

// Step 29 ┬╖ ESLint flat config (ESLint 9) ΓÇö the frontend's quality gate, the SPA counterpart to the backend's
// Spotless/Checkstyle (Step 28). JS recommended + typescript-eslint recommended + React hooks rules.
export default tseslint.config(
  { ignores: ['dist'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    },
  },
);
```
```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Build-a-Bank</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

🔍 **Line-by-line:**
- `type: "module"` in `package.json` enables native ES modules.
- `vitest` environment is `jsdom` with `url: 'http://localhost:5173'` so `localStorage` doesn't throw on an opaque origin.
- `eslint.config.js` uses the new ESLint 9 flat config API, combining JS recommended, TS recommended, and React hooks rules.

💭 **Under the hood:** Vite skips bundling in dev; it serves native ESM to the browser, relying on the browser to fetch dependencies. Esbuild transpiles TS to JS almost instantly.
🔮 **Predict:** why does `npm install` commit a `package-lock.json` even though `package.json` already lists versions? <details><summary>Answer</summary>`package.json` uses ranges (`^19.1.0`); the **lockfile pins the exact resolved versions of the whole tree** (incl. transitive deps) so every machine + CI builds identical bits. "Pinned, never latest" (§12.6) = commit the lockfile.</details>
▶️ **Run & See:** Run `npm install` to write the `package-lock.json`. (See Verification Log)
✋ **Checkpoint:** `frontend/` exists with `node_modules`.

## Sub-step 2 — the typed API client (one base URL = the gateway)

🎯 **Goal:** Create a typed `fetch` wrapper targeting the gateway (the single front door).
📁 **Path:** `frontend/src/api/client.ts`
⌨️ **Code:**
```typescript
// frontend/src/api/client.ts
// Step 29 ┬╖ the typed HTTP client. ONE base URL ΓÇö the gateway (the single front door, Step 15) ΓÇö configurable
// via VITE_API_BASE_URL. Matches the auth service's contracts exactly: POST /api/auth/login {username,password}
// ΓåÆ {token, expiresInSeconds}; GET /api/auth/me (Bearer) ΓåÆ {username, roles}. TanStack Query arrives in Step 30.

export interface LoginResponse {
  token: string;
  expiresInSeconds: number;
}

export interface CurrentUser {
  username: string;
  roles: string[];
}

/** A failed HTTP response, carrying the status so callers (and tests) can react to 401 vs 500. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init.headers },
  });
  if (!response.ok) {
    throw new ApiError(response.status, `Request to ${path} failed (${response.status})`);
  }
  return (await response.json()) as T;
}

/** Exchange credentials for a JWT (the auth service signs RS256, 30-min TTL). */
export function login(username: string, password: string): Promise<LoginResponse> {
  return request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}

/** Who am I? ΓÇö the backend reads the Bearer token and returns the username + roles. */
export function getCurrentUser(token: string): Promise<CurrentUser> {
  return request<CurrentUser>('/api/auth/me', {
    headers: { Authorization: `Bearer ${token}` },
  });
}
```

🔍 **Line-by-line:**
- `import.meta.env.VITE_API_BASE_URL` is Vite's syntax for environment variables.
- `ApiError` captures the HTTP status code (401 vs 500) so UI logic can react appropriately.
- `login()` matches the auth service's exact JSON contract (`{username, password}` → `{token, expiresInSeconds}`).
- `getCurrentUser()` attaches the `Authorization: Bearer <token>` header.

💭 **Under the hood:** The gateway routes `/api/auth/**` to the auth service. The SPA doesn't know about the `auth` service port, only the gateway's base URL.
🔮 **Predict:** What happens if `fetch` gets a 401 response? <details><summary>Answer</summary>`response.ok` is false, so it throws `ApiError(401)`. The caller (e.g. `login()`) catches this and can show a "Login failed" message.</details>
✋ **Checkpoint:** Typed HTTP client is ready for the context to use.

## Sub-step 3 — AuthContext + the route guard

🎯 **Goal:** Manage the JWT state globally and guard protected routes.
📁 **Path:** `frontend/src/auth/AuthContext.tsx` and `frontend/src/auth/ProtectedRoute.tsx`
⌨️ **Code:**
```tsx
// frontend/src/auth/AuthContext.tsx
// Step 29 ┬╖ the auth flow's single source of truth. Holds the JWT + current user, exposes login()/logout(),
// and persists the token in localStorage so a refresh keeps you signed in. (Token refresh + route guards are
// hardened in Step 32; secure storage trade-offs are discussed in the lesson.)
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';

import * as api from '../api/client';

const TOKEN_KEY = 'bab.token';

export interface AuthState {
  token: string | null;
  user: api.CurrentUser | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState<api.CurrentUser | null>(null);

  const login = useCallback(async (username: string, password: string) => {
    const { token: issued } = await api.login(username, password);
    localStorage.setItem(TOKEN_KEY, issued);
    setToken(issued);
    setUser(await api.getCurrentUser(issued)); // resolve who we are for the dashboard greeting
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    setToken(null);
    setUser(null);
  }, []);

  const value = useMemo<AuthState>(
    () => ({ token, user, isAuthenticated: token !== null, login, logout }),
    [token, user, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (context === null) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
```
```tsx
// frontend/src/auth/ProtectedRoute.tsx
// Step 29 ┬╖ a route guard ΓÇö wrap any element that requires a signed-in user. If there's no token, redirect to
// /login (replace, so Back doesn't bounce back into the guard). Hardened with token-refresh in Step 32.
import { Navigate } from 'react-router-dom';
import type { ReactNode } from 'react';

import { useAuth } from './AuthContext';

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}
```

🔍 **Line-by-line:**
- `useState(() => localStorage.getItem(TOKEN_KEY))` lazy-initializes the state from persistence so it survives reloads.
- `login` saves to both React state and `localStorage`.
- `ProtectedRoute` reads `isAuthenticated`; if false, returns `<Navigate to="/login" replace />`. `replace` prevents the back button from looping back into the guard.

💭 **Under the hood:** React Context provides dependency injection for state. Any component calling `useAuth()` rerenders when the token or user changes. Storing a JWT in `localStorage` is simple but XSS-exposed (hardened in Step 32).

## Sub-step 4 — pages + the route table

🎯 **Goal:** Build the Login and Dashboard pages, and wire up React Router.
📁 **Path:** `frontend/src/pages/LoginPage.tsx`, `frontend/src/pages/DashboardPage.tsx`, `frontend/src/App.tsx`, `frontend/src/main.tsx`
⌨️ **Code:**
```tsx
// frontend/src/pages/LoginPage.tsx
// Step 29 ┬╖ the sign-in form. Submits credentials through the AuthContext (which calls the gateway ΓåÆ auth),
// then navigates to the dashboard. Plain controlled inputs here; React Hook Form + Zod arrive in Step 30.
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { useAuth } from '../auth/AuthContext';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username, password);
      navigate('/');
    } catch {
      setError('Login failed ΓÇö check your username and password.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <h1>Build-a-Bank ΓÇö Sign in</h1>
      <form onSubmit={onSubmit} aria-label="Sign in">
        <label>
          Username
          <input
            name="username"
            value={username}
            autoComplete="username"
            onChange={(event) => setUsername(event.target.value)}
          />
        </label>
        <label>
          Password
          <input
            name="password"
            type="password"
            value={password}
            autoComplete="current-password"
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>
        <button type="submit" disabled={submitting}>
          {submitting ? 'Signing inΓÇª' : 'Sign in'}
        </button>
        {error !== null && <p role="alert">{error}</p>}
      </form>
    </main>
  );
}
```
```tsx
// frontend/src/pages/DashboardPage.tsx
// Step 29 ┬╖ the protected landing page ΓÇö proves the auth flow end-to-end by greeting the signed-in user
// (username + roles came from GET /api/auth/me). Real account/transfer screens arrive in Step 30.
import { useAuth } from '../auth/AuthContext';

export function DashboardPage() {
  const { user, logout } = useAuth();
  return (
    <main>
      <h1>Welcome to Build-a-Bank ≡ƒÅª</h1>
      <p>
        Signed in as <strong>{user?.username ?? 'ΓÇª'}</strong>
        {user !== null && user.roles.length > 0 ? ` (${user.roles.join(', ')})` : ''}
      </p>
      <button type="button" onClick={logout}>
        Sign out
      </button>
    </main>
  );
}
```
```tsx
// frontend/src/App.tsx
// Step 29 ┬╖ the route table. /login is public; / is protected (the dashboard); anything else redirects home
// (the guard then sends you to /login if you're not signed in). Nested layouts/feature routes grow in Step 30.
import { Navigate, Route, Routes } from 'react-router-dom';

import { ProtectedRoute } from './auth/ProtectedRoute';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
```
```tsx
// frontend/src/main.tsx
// Step 29 ┬╖ the SPA entry point. BrowserRouter (real URL routing) wraps AuthProvider (auth state) wraps the App.
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import { App } from './App';
import { AuthProvider } from './auth/AuthContext';
import './index.css';

const rootElement = document.getElementById('root');
if (rootElement === null) {
  throw new Error('Root element #root not found in index.html');
}

createRoot(rootElement).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
```

🔍 **Line-by-line:**
- `LoginPage` is a controlled form (`value={username} onChange=...`). Plain state here; React Hook Form arrives in Step 30.
- `DashboardPage` reads `useAuth().user` to greet the user with their loaded roles.
- `App.tsx` defines the route table: `/login` (public), `/` (wrapped in `<ProtectedRoute>`), and a catch-all `*`.
- `main.tsx` wraps the app in `<BrowserRouter>` (history API) and `<AuthProvider>`.

## Sub-step 5 — make the gateway the single front door

🎯 **Goal:** Add the auth route and CORS to the gateway so the SPA can call it cross-origin.
📁 **Path:** `gateway/src/main/resources/application.yml` and `gateway/src/main/java/com/buildabank/gateway/GatewayCorsConfig.java`
⌨️ **Code:**
```yaml
# Spring Cloud Gateway Server WebMVC (servlet) ΓÇö the single front door. Routes by a service prefix and
# strips it before forwarding, so external /cif/api/customers/1 ΓåÆ cif's /api/customers/1.
# (Config prefix is spring.cloud.gateway.server.webmvc.* since Spring Cloud 2025; the old
#  spring.cloud.gateway.mvc.* is deprecated.)
spring:
  application:
    name: gateway
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: cif
              uri: ${services.cif.uri:http://localhost:8081}
              predicates:
                - Path=/cif/**
              filters:
                - StripPrefix=1                          # /cif/api/customers/1 ΓåÆ /api/customers/1
                - AddResponseHeader=X-Gateway, build-a-bank
            - id: demand-account
              uri: ${services.demand-account.uri:http://localhost:8082}
              predicates:
                - Path=/bank/**
              filters:
                - StripPrefix=1                          # /bank/api/v1/transfers ΓåÆ /api/v1/transfers
                - AddResponseHeader=X-Gateway, build-a-bank
            # Step 29: front the auth service so the React app has ONE base URL (the gateway). Auth's own
            # paths already start with /api/auth, so we DON'T strip ΓÇö /api/auth/login ΓåÆ auth's /api/auth/login.
            - id: auth
              uri: ${services.auth.uri:http://localhost:8083}
              predicates:
                - Path=/api/auth/**
              filters:
                - AddResponseHeader=X-Gateway, build-a-bank

server:
  port: 8080                                             # the gateway is the front door

# Step 29: CORS so the React dev app (Vite, http://localhost:5173) can call the gateway from the browser.
# Comma-separated origins; override per environment (tighten/replace in prod). Consumed by GatewayCorsConfig.
app:
  security:
    cors:
      allowed-origins: ${APP_CORS_ALLOWED_ORIGINS:http://localhost:5173}

management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway                     # /actuator/gateway lists the routes

logging:
  level:
    com.buildabank.gateway: INFO
```
```java
package com.buildabank.gateway;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Step 29 ┬╖ CORS at the gateway (the front door), so the browser-based React app (Vite dev server,
 * http://localhost:5173) can call it cross-origin. A standard servlet {@link CorsFilter} ΓÇö it answers the
 * browser's OPTIONS preflight and adds {@code Access-Control-Allow-Origin} to responses.
 *
 * <p><strong>Deny-by-default</strong> (same posture as demand-account, Step 18): only the origins listed in
 * {@code app.security.cors.allowed-origins} are allowed. The dev default is the Vite origin; override
 * {@code APP_CORS_ALLOWED_ORIGINS} per environment and tighten for production.
 */
@Configuration
class GatewayCorsConfig {

    @Bean
    CorsFilter corsFilter(@Value("${app.security.cors.allowed-origins:}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins.stream().filter(o -> !o.isBlank()).toList()); // empty ΓçÆ deny all
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

🔍 **Line-by-line:**
- The `auth` route lacks `StripPrefix=1` because the auth service's endpoints actually start with `/api/auth`.
- `CorsFilter` allows the Vite dev origin `http://localhost:5173` but defaults to denying others (deny-by-default posture).

💭 **Under the hood:** When the browser makes a cross-origin POST with `application/json`, it first sends an `OPTIONS` preflight. The `CorsFilter` intercepts this and answers with `Access-Control-Allow-Origin: http://localhost:5173`.
🔮 **Predict:** What happens if the SPA runs on port `3000` but calls the gateway? <details><summary>Answer</summary>The preflight fails (403 or missing ACA-O header) because `localhost:3000` is not in `allowed-origins`. The browser blocks the fetch.</details>

## Sub-step 6 — test the SPA (Vitest + Testing Library)

🎯 **Goal:** Unit test the API client, route guard, and login page. Update the gateway routing test.
📁 **Path:** `frontend/src/test/setup.ts`, `frontend/src/api/client.test.ts`, `frontend/src/auth/ProtectedRoute.test.tsx`, `frontend/src/pages/LoginPage.test.tsx`, `gateway/src/test/java/com/buildabank/gateway/GatewayRoutingTest.java`
⌨️ **Code:**
```typescript
// frontend/src/test/setup.ts
// Step 29 ┬╖ Vitest setup ΓÇö registers jest-dom matchers (toBeInTheDocument, ΓÇª) and resets DOM + localStorage
// between tests so they're independent.
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// jsdom's localStorage getter throws for an opaque origin, so Vitest leaves the global `undefined`. The app
// uses localStorage (AuthContext), so install a tiny in-memory Storage for tests ΓÇö deterministic and isolated.
if (typeof globalThis.localStorage === 'undefined') {
  const store = new Map<string, string>();
  globalThis.localStorage = {
    get length() {
      return store.size;
    },
    clear: () => store.clear(),
    getItem: (key: string) => store.get(key) ?? null,
    key: (index: number) => Array.from(store.keys())[index] ?? null,
    removeItem: (key: string) => store.delete(key),
    setItem: (key: string, value: string) => store.set(key, String(value)),
  } as Storage;
}

afterEach(() => {
  cleanup();
  localStorage.clear();
});
```
```typescript
// frontend/src/api/client.test.ts
// Step 29 ┬╖ unit-test the HTTP client by stubbing global fetch ΓÇö proves the request shape (path, method, body,
// bearer header) and error handling match the auth contract, with no network.
import { afterEach, describe, expect, it, vi } from 'vitest';

import { ApiError, getCurrentUser, login } from './client';

function jsonResponse(body: unknown, ok = true, status = 200): Response {
  return { ok, status, json: () => Promise.resolve(body) } as Response;
}

describe('api client', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('login POSTs the credentials and returns the token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ token: 'jwt-123', expiresInSeconds: 1800 }));
    vi.stubGlobal('fetch', fetchMock);

    const result = await login('alice', 'password');

    expect(result.token).toBe('jwt-123');
    expect(result.expiresInSeconds).toBe(1800);
    expect(fetchMock).toHaveBeenCalledOnce();
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain('/api/auth/login');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body as string)).toEqual({ username: 'alice', password: 'password' });
  });

  it('getCurrentUser sends the Bearer token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ username: 'alice', roles: ['ROLE_USER'] }));
    vi.stubGlobal('fetch', fetchMock);

    const user = await getCurrentUser('jwt-123');

    expect(user.username).toBe('alice');
    expect(user.roles).toContain('ROLE_USER');
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer jwt-123');
  });

  it('throws ApiError carrying the status on a non-ok response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({}, false, 401)));
    await expect(login('alice', 'wrong')).rejects.toBeInstanceOf(ApiError);
  });
});
```
```tsx
// frontend/src/auth/ProtectedRoute.test.tsx
// Step 29 ┬╖ route-guard test. With no token the guard redirects to /login; with a token it renders the
// protected content. (AuthProvider seeds its token from localStorage, so we drive auth state via localStorage.)
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';

import { AuthProvider } from '../auth/AuthContext';
import { ProtectedRoute } from './ProtectedRoute';

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<p>Login page</p>} />
          <Route
            path="/secret"
            element={
              <ProtectedRoute>
                <p>Secret area</p>
              </ProtectedRoute>
            }
          />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  beforeEach(() => localStorage.clear());

  it('redirects to /login when there is no token', () => {
    renderAt('/secret');
    expect(screen.getByText('Login page')).toBeInTheDocument();
    expect(screen.queryByText('Secret area')).not.toBeInTheDocument();
  });

  it('renders the protected content when a token is present', () => {
    localStorage.setItem('bab.token', 'jwt-123');
    renderAt('/secret');
    expect(screen.getByText('Secret area')).toBeInTheDocument();
  });
});
```
```tsx
// frontend/src/pages/LoginPage.test.tsx
// Step 29 ┬╖ component test (Testing Library + user-event). The api/client module is mocked (MSW arrives in
// Step 31), so we assert the page wires the form to the auth call and surfaces errors ΓÇö no real network.
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';

import * as api from '../api/client';
import { AuthProvider } from '../auth/AuthContext';
import { LoginPage } from './LoginPage';

vi.mock('../api/client');

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('LoginPage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('submits the typed credentials through the auth API', async () => {
    vi.mocked(api.login).mockResolvedValue({ token: 'jwt-123', expiresInSeconds: 1800 });
    vi.mocked(api.getCurrentUser).mockResolvedValue({ username: 'alice', roles: ['ROLE_USER'] });
    renderLogin();

    await userEvent.type(screen.getByLabelText(/username/i), 'alice');
    await userEvent.type(screen.getByLabelText(/password/i), 'password');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(api.login).toHaveBeenCalledWith('alice', 'password');
  });

  it('shows an error message when login fails', async () => {
    vi.mocked(api.login).mockRejectedValue(new Error('bad credentials'));
    renderLogin();

    await userEvent.type(screen.getByLabelText(/username/i), 'alice');
    await userEvent.type(screen.getByLabelText(/password/i), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/login failed/i);
  });
});
```
```diff
diff --git a/gateway/src/test/java/com/buildabank/gateway/GatewayRoutingTest.java b/gateway/src/test/java/com/buildabank/gateway/GatewayRoutingTest.java
index d0549bd..fe4867d 100644
--- a/gateway/src/test/java/com/buildabank/gateway/GatewayRoutingTest.java
+++ b/gateway/src/test/java/com/buildabank/gateway/GatewayRoutingTest.java
@@ -56,6 +56,8 @@ class GatewayRoutingTest {
         String stubUri = "http://localhost:" + stub.getAddress().getPort();
         registry.add("services.cif.uri", () -> stubUri);
         registry.add("services.demand-account.uri", () -> stubUri);
+        registry.add("services.auth.uri", () -> stubUri);                                  // Step 29: auth route target
+        registry.add("app.security.cors.allowed-origins", () -> "http://localhost:5173");  // Step 29: allowed dev origin
     }
 
     @AfterAll
@@ -77,4 +79,50 @@ class GatewayRoutingTest {
         assertThat(receivedPath.get()).isEqualTo("/api/customers/1");               // StripPrefix removed "/cif"
         assertThat(response.headers().firstValue("X-Gateway")).hasValue("build-a-bank");   // gateway filter ran
     }
+
+    @Test
+    void routesAuthWithoutStrippingPrefix() throws Exception {
+        // Step 29: the React app calls the gateway for login; auth's paths already start with /api/auth,
+        // so the auth route does NOT strip ΓÇö the downstream receives the path unchanged.
+        HttpResponse<String> response = http.send(
+                HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort + "/api/auth/me"))
+                        .GET().build(),
+                HttpResponse.BodyHandlers.ofString());
+
+        assertThat(response.statusCode()).isEqualTo(200);
+        assertThat(receivedPath.get()).isEqualTo("/api/auth/me");                   // NOT stripped
+        assertThat(response.headers().firstValue("X-Gateway")).hasValue("build-a-bank");
+    }
+
+    @Test
+    void corsPreflightFromTheAllowedOriginIsAllowed() throws Exception {
+        // A browser preflight: OPTIONS + Origin + Access-Control-Request-Method. The gateway's CorsFilter
+        // answers it directly with the matching Access-Control-Allow-Origin (the route is never reached).
+        HttpResponse<String> preflight = http.send(
+                HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort + "/api/auth/login"))
+                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
+                        .header("Origin", "http://localhost:5173")
+                        .header("Access-Control-Request-Method", "POST")
+                        .build(),
+                HttpResponse.BodyHandlers.ofString());
+
+        assertThat(preflight.statusCode()).isEqualTo(200);
+        assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin"))
+                .hasValue("http://localhost:5173");
+    }
+
+    @Test
+    void corsPreflightFromADisallowedOriginIsRejected() throws Exception {
+        // deny-by-default: an origin not on the allow-list gets no Access-Control-Allow-Origin (browser blocks it).
+        HttpResponse<String> preflight = http.send(
+                HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort + "/api/auth/login"))
+                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
+                        .header("Origin", "http://evil.example")
+                        .header("Access-Control-Request-Method", "POST")
+                        .build(),
+                HttpResponse.BodyHandlers.ofString());
+
+        assertThat(preflight.statusCode()).isEqualTo(403);
+        assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
+    }
 }
```

🔍 **Line-by-line:**
- `setup.ts` shims `localStorage` because jsdom's native one throws on `about:blank`.
- `client.test.ts` uses `vi.stubGlobal('fetch', ...)` to assert the network shape without real network calls.
- `LoginPage.test.tsx` uses `userEvent` (simulates real keystrokes) and mocks `api.login`.
- `GatewayRoutingTest` adds cases for the `auth` non-stripped route and CORS preflights (`OPTIONS` request asserts `Access-Control-Allow-Origin`).

⚠️ **Pitfall (we hit it):** jsdom's `localStorage` getter throws for an opaque origin, so Vitest leaves the global `undefined` (and your app breaks in tests). Fix: install a tiny in-memory `localStorage` in `src/test/setup.ts`.

🔬 **Break-it (the §12.3 proof):** disable the guard (`if (false && !isAuthenticated)`) → the "redirects to /login" test fails (it renders the protected content). Put it back.

💾 **Commit:** `feat(frontend): Step 29 React+TS+Vite SPA — login/auth flow + routing; gateway fronts auth + CORS`

## 🎮 Play With It

```bash
# Terminal 1 — backend (the SPA's front door + auth). From the repo root:
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173 ./mvnw -pl services/auth spring-boot:run   # :8083
./mvnw -pl gateway spring-boot:run                                                          # :8080 (new shell)
# Terminal 2 — the SPA:
npm --prefix frontend run dev      # open http://localhost:5173 → sign in as alice / password
```

🧪 **Little experiments:** sign in as `alice`/`password` then `admin`/`admin123` — the dashboard shows different roles. Open DevTools → Application → Local Storage: see `bab.token`. Delete it, refresh: you're bounced to `/login` (the guard). Stop the gateway and try to log in: the error message appears.

> [!NOTE]
> The **live browser** flow is the one thing this course's sandbox can't self-verify (no browser). Everything else — build, lint, the component/route tests, and the gateway's CORS preflight — is verified with real output below (§12.8 honesty).

## 🏁 The Finished Result

`step-29-end`: a working React+TS SPA with login, routing, and a guard, talking to the gateway; the gateway is the single front door (auth + CORS). **✅ Definition of Done:** `npm run build`, `npm run lint`, `npm test` green; the gateway tests pass; `./mvnw verify` green; `bash steps/step-29/smoke.sh` passes; committed/tagged `step-29-end`.

---

<a id="prove"></a>

# D · 🔬 Prove It Works — Verification Log

> **Tier: 🔴 Full** (new app + gateway/build change). Real pasted output. The SPA needs no Docker; the full
> `./mvnw verify` does (existing integration tests). A live *browser* flow is verify-adjacent (§12.8).

**1 · `npm install` — reproducible deps (lockfile committed):**

```
added 289 packages, and audited 290 packages in 2m
found 0 vulnerabilities
```

**2 · `npm run build` — TypeScript typecheck + Vite production build:**

```
> tsc && vite build
vite v6.4.3 building for production...
✓ 46 modules transformed.
dist/index.html                   0.40 kB
dist/assets/index-*.css           0.60 kB
dist/assets/index-*.js          234.72 kB │ gzip: 75.15 kB
✓ built in 931ms
```

**3 · `npm test` — Vitest + Testing Library (7 tests, no Docker):**

```
✓ src/api/client.test.ts (3 tests)
✓ src/auth/ProtectedRoute.test.tsx (2 tests)
✓ src/pages/LoginPage.test.tsx (2 tests)
 Test Files  3 passed (3)
      Tests  7 passed (7)
```

**4 · `npm run lint` — the SPA's quality gate:**

```
✖ 1 problem (0 errors, 1 warning)   # react-refresh DX hint on AuthContext.tsx — benign; lint exits 0
```

**5 · Gateway — the new auth route + CORS (headless, no Docker):**

```
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.buildabank.gateway.GatewayRoutingTest
```
covering: routes-to-downstream (StripPrefix), **routes `/api/auth/me` WITHOUT stripping**, **CORS preflight from
`http://localhost:5173` → `Access-Control-Allow-Origin: http://localhost:5173`**, and **a disallowed origin → 403**.

**6 · §12.3 Mutation sanity-check — prove the guard test means something.** Disabled the guard
(`if (false && !isAuthenticated)`) and re-ran:

```
× ProtectedRoute > redirects to /login when there is no token
  → Unable to find an element with the text: Login page    [rendered "Secret area" instead]
 Tests  1 failed | 6 passed (7)
```
→ With the guard off, the protected content rendered for an unauthenticated user — the test caught it. **Reverted** → 7/7 green.

**7 · `smoke.sh`** — `bash steps/step-29/smoke.sh` runs the SPA build + lint + tests and the gateway route/CORS test → `✅ Step 29 smoke test PASSED`.

**8 · Build** — full-repo `./mvnw verify` → BUILD SUCCESS (14 modules; gateway change included; quality gates green). Clean-room: fresh clone + `npm ci` + build + test green.

**§12.8 honesty:** the **live browser** cross-origin flow can't run in this sandbox (no browser) — verified instead by the headless CORS preflight test + the component/route tests. The JWT lives in `localStorage` (XSS-exposed) — a teaching simplification hardened in Step 32. One benign ESLint `react-refresh` warning remains (DX-only; lint exits 0).

---

<a id="apply"></a>

# E · 🎓 Apply

## 🚀 Go Deeper (Optional)

<details><summary>Why is Vite's dev server so fast?</summary>In dev it doesn't bundle — it serves your source as native ES modules and lets the browser request them on demand, transpiling each file with esbuild (Go, very fast). Only what's imported is processed; HMR swaps a single module. Production still bundles (Rollup) for fewer requests + tree-shaking.</details>

<details><summary>localStorage vs httpOnly cookie vs in-memory for the JWT</summary>localStorage: survives refresh, simple — but any injected script can read it (XSS). httpOnly cookie: JS can't read it (XSS-safe for the token) but you must defend CSRF and set SameSite. In-memory: safest from XSS exfiltration but lost on refresh → pair with a refresh-token rotation. Step 32 implements refresh + guards; the choice is a real trade-off, not a default.</details>

<details><summary>Why one origin (the gateway) instead of calling each service?</summary>One base URL, one CORS policy, one place for cross-cutting concerns (auth, rate limits, headers). The browser otherwise needs a CORS handshake with every service. This is the BFF/API-gateway pattern (Step 15) paying off for the UI.</details>

## 💼 Interview Prep

1. **Walk me through your frontend auth flow.** *Login posts credentials → JWT; store it (localStorage here, with the XSS caveat); expose auth via context; attach `Bearer` to API calls; a ProtectedRoute redirects unauthenticated users. /me resolves the current user.* **(Common.)**
2. **How do you protect a route in React Router?** *A guard component reads auth state from context and either renders the children or `<Navigate to="/login" replace/>`. `replace` so Back doesn't re-enter the guard.*
3. **Where do you store a JWT and why?** *Trade-off: localStorage (simple, XSS-exposed) vs httpOnly cookie (XSS-safe, CSRF to handle) vs in-memory + refresh token (safest, lost on refresh). State the threat model.*
4. **What is CORS and why did the gateway need it?** *Browsers block cross-origin requests unless the server opts in via `Access-Control-Allow-Origin`. The SPA (:5173) and gateway (:8080) are different origins, so the gateway must allow the SPA's origin (deny-by-default).*
5. **(Gotcha) Why does Vite commit a lockfile if package.json has versions?** *package.json has ranges; the lockfile pins exact resolved versions of the whole tree for reproducible `npm ci`.*

## 🏋️ Your Turn: Practice & Challenges

- **Quick:** add a `roles` check to `ProtectedRoute` (a `requiredRole` prop) and a test that a USER is redirected from an admin-only route.
- **Quick:** add a `client.test.ts` case asserting `getCurrentUser` throws `ApiError(401)` on an expired token.
- 🎯 **Stretch (reference solution in `solutions/step-29/`):** add an Axios-free request interceptor that auto-attaches the token from `AuthContext` to every call (so pages don't pass it manually), with a test that the header is present.

---

<a id="review"></a>

# F · 🏆 Review

## 🩺 Stuck? Troubleshooting & Fixes

- **Tests fail with `Cannot read properties of undefined (reading 'clear')` / `localStorage is not defined`.** jsdom's localStorage getter throws for an opaque origin → Vitest leaves the global undefined. Install an in-memory `localStorage` in `src/test/setup.ts` (this lesson does).
- **Browser console: `... has been blocked by CORS policy`.** The gateway isn't allowing your origin. Start it with `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173` (or check `GatewayCorsConfig`).
- **Login returns 404 at the gateway.** The gateway needs the `auth` route (`/api/auth/**`, no StripPrefix) — and the auth service must be running on :8083.
- **`npm run build` fails on a type error.** That's `tsc` doing its job — fix the type; the build won't ship broken types.
- **ESLint `react-refresh` warning on AuthContext.** Benign (DX-only); lint still exits 0. Split the hook into its own file if you want it gone.
- **Reset:** `git checkout step-29-end` then `npm --prefix frontend ci`.

## 📚 Learn More & Glossary

- Vite guide; React docs (you-might-not-need-an-effect, hooks); React Router docs; Testing Library (guiding principles); Vitest docs; MDN on CORS and Web Storage.
- **Glossary:** *SPA*, *Vite / ESM dev server*, *client-side routing*, *route guard*, *React context*, *JWT / Bearer*, *CORS / preflight*, *Vitest / jsdom*, *Testing Library*, *lockfile / `npm ci`*.

## 🏆 Recap & Study Notes

**(a) Key points:** A **Vite + React + TypeScript** SPA with **client-side routing** and a **JWT login flow**:
`LoginPage` → `AuthContext.login()` → the gateway's `/api/auth/login` → store the token → `/me` →
`DashboardPage`; a **`ProtectedRoute`** redirects unauthenticated users. The SPA talks to **one origin — the
gateway**, which we extended to front `auth` (no StripPrefix) with **deny-by-default CORS**. Tested with
**Vitest + Testing Library** (API mocked; MSW in Step 31). The JWT in `localStorage` is a teaching simplification
(XSS-exposed), hardened in Step 32.

**(b) Key terms:** SPA, Vite, ESM dev server, client-side routing, route guard, React context, JWT/Bearer, CORS/preflight, Vitest, Testing Library, lockfile.

**(c) 🧠 Test Yourself:** ① Why is Vite's dev server fast? ② How does ProtectedRoute work? ③ Where do you store a JWT and the trade-off? ④ Why does the SPA use one origin? ⑤ How did you prove the guard test is meaningful? <details><summary>Answers</summary>① Native ESM in dev (no bundling), esbuild transpile, per-module HMR. ② Reads auth from context; renders children or `<Navigate to="/login" replace/>`. ③ localStorage (simple, XSS-exposed) vs httpOnly cookie vs in-memory+refresh — threat-model-dependent. ④ One base URL + one CORS policy + cross-cutting concerns at the gateway. ⑤ Disabled the guard → the redirect test failed (rendered protected content) → reverted.</details>

**(d) 🔗 How this connects:** consumes the **auth** (16) + **gateway** (15) backend with the **CORS** posture from 18. **Next: Step 30** — TanStack Query (data fetching/caching), React Hook Form + Zod (forms/validation), and live WebSocket updates; then Step 31 (Playwright E2E + MSW + a11y/i18n) and Step 32 (token refresh, bundling, Dockerize + serve via the gateway).

**(e) 🏆 Résumé line:** *"Built a React + TypeScript (Vite) SPA with a JWT login flow, protected routing, and a typed API client against a Spring Cloud Gateway front door — tested with Vitest + Testing Library."*

**(f) ✅ You can now:** scaffold a Vite React-TS app · route client-side with a guard · implement a JWT login flow · test components/routes · reason about CORS and token storage.

**(g) 🃏 Flashcards** appended to `docs/flashcards.md` · 🔁 revisit token storage at Step 32 (hardening) and CORS whenever you add an origin.

**(h) ✍️ One-line reflection:** *Which part of the auth flow would break first under XSS — and what would Step 32's hardening change?*

**(i)** 🎉 The bank has a face. Next: real data, forms, and live updates.
