// frontend/src/App.tsx
// Step 29 · the route table. /login is public; / is protected (the dashboard); anything else redirects home
// (the guard then sends you to /login if you're not signed in).
// Step 32 · route-level code-splitting: each page is a lazy() dynamic import, so Rollup emits it as its own
// chunk and the browser downloads a page's JS the first time someone NAVIGATES there — the login screen no
// longer pays for TanStack Query, the forms stack, or the dashboard. Suspense shows a fallback during the
// (first-visit-only) chunk fetch. The .then() re-shapes our named exports into the default export lazy() wants.
import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';

import { ProtectedRoute } from './auth/ProtectedRoute';

const LoginPage = lazy(() => import('./pages/LoginPage').then((m) => ({ default: m.LoginPage })));
const DashboardPage = lazy(() =>
  import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage })),
);

export function App() {
  return (
    <Suspense fallback={<p aria-busy="true">Loading…</p>}>
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
    </Suspense>
  );
}
