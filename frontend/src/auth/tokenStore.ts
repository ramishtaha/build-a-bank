// frontend/src/auth/tokenStore.ts
// Step 32 · the access token's ONLY home: a module-private variable. Not localStorage (readable by any
// injected script, survives forever), not a cookie (rides on every request) — plain JS memory. It vanishes on
// reload; the silent-refresh bootstrap (AuthContext) restores it from the httpOnly refresh cookie. XSS can
// still *use* the app while it's open (no client-side storage fixes that), but it can no longer exfiltrate a
// long-lived credential from storage.
//
// Session-expiry is announced through a tiny listener list so the API layer (which discovers the 401) can
// tell the React layer (which owns rendering) without importing it — no circular dependency.

let accessToken: string | null = null;

const sessionExpiredListeners = new Set<() => void>();

export const tokenStore = {
  get(): string | null {
    return accessToken;
  },

  set(token: string): void {
    accessToken = token;
  },

  clear(): void {
    accessToken = null;
  },

  /** Subscribe to "the session is gone" (refresh failed). Returns an unsubscribe function. */
  onSessionExpired(listener: () => void): () => void {
    sessionExpiredListeners.add(listener);
    return () => sessionExpiredListeners.delete(listener);
  },

  /** Called by the API layer when a refresh attempt fails — the session is over, everywhere. */
  notifySessionExpired(): void {
    sessionExpiredListeners.forEach((listener) => listener());
  },
};
