// frontend/src/auth/ProtectedRoute.tsx
// Step 29 · a route guard — wrap any element that requires a signed-in user.
// Step 32 · session-aware: while the silent-refresh bootstrap is deciding ('initializing') we hold the door
// with a placeholder — redirecting straight to /login would bounce every reload even when the httpOnly cookie
// is about to restore the session. Only a settled 'anonymous' redirects (replace, so Back doesn't loop).
import { Navigate } from 'react-router-dom';
import type { ReactNode } from 'react';

import { useAuth } from './AuthContext';

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  if (status === 'initializing') {
    return <p aria-busy="true">Checking your session…</p>;
  }
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}
