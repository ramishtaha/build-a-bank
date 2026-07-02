// frontend/src/auth/AuthContext.tsx
// Step 29 · the auth flow's single source of truth — WHO is signed in and WHETHER we know yet.
// Step 32 · hardened: no token in localStorage (or anywhere else JS-storage-readable). The access token lives
// in tokenStore (memory); on mount we run a SILENT REFRESH — the httpOnly cookie proves the session — so a
// page reload keeps you signed in without persisting a credential. Until that first answer comes back the
// status is 'initializing' (the route guard shows a placeholder instead of bouncing you to /login).
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import * as api from '../api/client';
import { tokenStore } from './tokenStore';

export type SessionStatus = 'initializing' | 'authenticated' | 'anonymous';

export interface AuthState {
  user: api.CurrentUser | null;
  status: SessionStatus;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<api.CurrentUser | null>(null);
  const [status, setStatus] = useState<SessionStatus>('initializing');

  // Bootstrap: one silent refresh. Success → we're signed in (reload survived); 401 → anonymous, show /login.
  useEffect(() => {
    let cancelled = false; // StrictMode double-mounts; the stale effect must not set state
    void (async () => {
      const token = await api.refreshAccessToken();
      if (cancelled) return;
      if (!token) {
        setStatus('anonymous');
        return;
      }
      try {
        const me = await api.getCurrentUser();
        if (!cancelled) {
          setUser(me);
          setStatus('authenticated');
        }
      } catch {
        if (!cancelled) setStatus('anonymous');
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  // The API layer discovers session death (a refresh that fails mid-use); we own reacting to it.
  useEffect(
    () =>
      tokenStore.onSessionExpired(() => {
        setUser(null);
        setStatus('anonymous'); // the route guard redirects to /login on next render
      }),
    [],
  );

  const login = useCallback(async (username: string, password: string) => {
    await api.login(username, password); // stores the access token + accepts the refresh cookie
    setUser(await api.getCurrentUser()); // resolve who we are for the dashboard greeting
    setStatus('authenticated');
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.logout(); // revokes the refresh family server-side + clears the cookie
    } finally {
      setUser(null);
      setStatus('anonymous'); // even if the network call failed, drop the local session
    }
  }, []);

  const value = useMemo<AuthState>(
    () => ({ user, status, isAuthenticated: status === 'authenticated', login, logout }),
    [user, status, login, logout],
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
