// frontend/src/api/client.test.ts
// Step 29 · unit-test the HTTP client by stubbing global fetch — proves the request shape (path, method, body,
// bearer header) and error handling match the auth contract, with no network.
// Step 32 · the client now OWNS the token: login stores it, protected calls attach it from tokenStore
// (callers no longer pass it), and auth endpoints send credentials:'include' for the refresh cookie.
import { afterEach, describe, expect, it, vi } from 'vitest';

import { tokenStore } from '../auth/tokenStore';
import { ApiError, getAccount, getCurrentUser, login, transfer } from './client';

function jsonResponse(body: unknown, ok = true, status = 200): Response {
  return { ok, status, json: () => Promise.resolve(body) } as Response;
}

describe('api client', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    tokenStore.clear();
  });

  it('login POSTs the credentials with cookie credentials and stores the token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ token: 'jwt-123', expiresInSeconds: 1800 }));
    vi.stubGlobal('fetch', fetchMock);

    const result = await login('alice', 'password');

    expect(result.token).toBe('jwt-123');
    expect(tokenStore.get()).toBe('jwt-123'); // Step 32: the client keeps the access token in memory
    expect(fetchMock).toHaveBeenCalledOnce();
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain('/api/auth/login');
    expect(init.method).toBe('POST');
    expect(init.credentials).toBe('include'); // accept the httpOnly refresh cookie in cross-origin dev
    expect(JSON.parse(init.body as string)).toEqual({ username: 'alice', password: 'password' });
  });

  it('getCurrentUser attaches the in-memory token as a Bearer header', async () => {
    tokenStore.set('jwt-123');
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ username: 'alice', roles: ['ROLE_USER'] }));
    vi.stubGlobal('fetch', fetchMock);

    const user = await getCurrentUser();

    expect(user.username).toBe('alice');
    expect(user.roles).toContain('ROLE_USER');
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer jwt-123');
  });

  it('throws ApiError carrying the status on a non-ok response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({}, false, 401)));
    await expect(login('alice', 'wrong')).rejects.toBeInstanceOf(ApiError);
  });

  it('getAccount hits the gateway /bank prefix with the Bearer token', async () => {
    tokenStore.set('jwt-123');
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ accountNumber: 'ACC-A', currency: 'USD', balance: 200 }));
    vi.stubGlobal('fetch', fetchMock);

    const account = await getAccount('ACC-A');

    expect(account.balance).toBe(200);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain('/bank/api/accounts/ACC-A');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer jwt-123');
  });

  it('transfer POSTs to /bank/api/v1/transfers with an Idempotency-Key', async () => {
    tokenStore.set('jwt-123');
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ transactionId: 'txn-1' }));
    vi.stubGlobal('fetch', fetchMock);

    const result = await transfer({ from: 'ACC-A', to: 'ACC-B', amount: 50 }, 'key-1');

    expect(result.transactionId).toBe('txn-1');
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain('/bank/api/v1/transfers');
    expect(init.method).toBe('POST');
    const headers = init.headers as Record<string, string>;
    expect(headers.Authorization).toBe('Bearer jwt-123');
    expect(headers['Idempotency-Key']).toBe('key-1');
    expect(JSON.parse(init.body as string)).toMatchObject({ from: 'ACC-A', to: 'ACC-B', amount: 50 });
  });

  it('parses an RFC 9457 ProblemDetail body into the ApiError message', async () => {
    tokenStore.set('jwt-123');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ detail: 'Insufficient funds' }, false, 422)));
    await expect(transfer({ from: 'ACC-A', to: 'ACC-B', amount: 999 }, 'key-2')).rejects.toThrow(
      /Insufficient funds/,
    );
  });
});
