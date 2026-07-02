// frontend/src/auth/session.test.ts
// Step 32 · the session machinery at the NETWORK level (MSW): an expired access token triggers exactly one
// silent refresh and a retry; concurrent 401s share that refresh (single-flight); a dead session notifies
// the expiry listeners; and the MSW virtual cookie jar proves the login → refresh cookie round-trip.
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { getAccount, login, refreshAccessToken } from '../api/client';
import { server } from '../mocks/server';
import { tokenStore } from './tokenStore';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

/** An /accounts handler that 401s stale tokens and serves fresh ones — the "access token expired" world. */
function accountRespectsToken() {
  server.use(
    http.get(`${API_BASE}/bank/api/accounts/:id`, ({ request, params }) => {
      const auth = request.headers.get('Authorization');
      if (auth !== 'Bearer fresh-jwt') {
        return HttpResponse.json({ detail: 'Token expired' }, { status: 401 });
      }
      return HttpResponse.json({ accountNumber: String(params.id), currency: 'USD', balance: 42 });
    }),
  );
}

/** A /refresh that always succeeds (as if a live cookie were present), counting how often it's hit. */
function refreshAlwaysSucceeds() {
  let calls = 0;
  server.use(
    http.post(`${API_BASE}/api/auth/refresh`, () => {
      calls += 1;
      return HttpResponse.json({ token: 'fresh-jwt', expiresInSeconds: 1800 });
    }),
  );
  return () => calls;
}

describe('session machinery (401 → refresh → retry)', () => {
  afterEach(() => tokenStore.clear());

  it('retries a 401 request once after a silent refresh', async () => {
    tokenStore.set('stale-jwt');
    accountRespectsToken();
    const refreshCalls = refreshAlwaysSucceeds();

    const account = await getAccount('ACC-A');

    expect(account.balance).toBe(42); // the RETRY succeeded
    expect(tokenStore.get()).toBe('fresh-jwt'); // and the new token replaced the stale one
    expect(refreshCalls()).toBe(1);
  });

  it('shares ONE refresh across concurrent 401s (single-flight)', async () => {
    tokenStore.set('stale-jwt');
    accountRespectsToken();
    const refreshCalls = refreshAlwaysSucceeds();

    const [a, b, c] = await Promise.all([getAccount('ACC-A'), getAccount('ACC-B'), getAccount('ACC-C')]);

    expect([a.balance, b.balance, c.balance]).toEqual([42, 42, 42]);
    expect(refreshCalls()).toBe(1); // three 401s, one refresh — not three rotations
  });

  it('announces session expiry when the refresh itself fails', async () => {
    tokenStore.set('stale-jwt');
    accountRespectsToken(); // default /refresh handler 401s (no cookie in the jar)
    const expired = vi.fn();
    const unsubscribe = tokenStore.onSessionExpired(expired);

    await expect(getAccount('ACC-A')).rejects.toMatchObject({ status: 401 });
    expect(expired).toHaveBeenCalledOnce(); // AuthContext listens to this and logs out
    expect(tokenStore.get()).toBeNull();
    unsubscribe();
  });

  it('round-trips the refresh cookie: login plants it, refresh finds it (MSW virtual cookie jar)', async () => {
    await login('alice', 'password123'); // handler sets bab_refresh=mock-refresh-1
    tokenStore.clear(); // simulate a page reload: memory gone, cookie survives

    const token = await refreshAccessToken(); // sends credentials:'include' → the jar attaches the cookie

    expect(token).toBe('mock-jwt'); // the default /refresh handler honored the cookie
  });
});
