// frontend/src/mocks/handlers.ts
import { http, HttpResponse } from 'msw';
import type { Account, Page, LedgerEntry, LoginResponse, CurrentUser } from '../api/client';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const handlers = [
  // Auth. Step 32: login also plants the refresh cookie — MSW keeps a virtual cookie jar in Node, so a later
  // /refresh in the same test really receives it (network-level fidelity, same as the browser).
  http.post(`${API_BASE}/api/auth/login`, async ({ request }) => {
    const { username, password } = (await request.json()) as { username: string; password: string };
    if (username === 'alice' && password === 'password123') {
      return HttpResponse.json<LoginResponse>(
        { token: 'mock-jwt', expiresInSeconds: 3600 },
        { headers: { 'Set-Cookie': 'bab_refresh=mock-refresh-1; Path=/api/auth; HttpOnly; SameSite=Strict' } },
      );
    }
    return HttpResponse.json({ detail: 'Bad credentials' }, { status: 401 });
  }),

  // Step 32: silent refresh — with a refresh cookie you get a fresh access token + a ROTATED cookie;
  // without one (the default in a fresh test) you're anonymous: 401.
  http.post(`${API_BASE}/api/auth/refresh`, ({ cookies }) => {
    const refresh = cookies.bab_refresh;
    if (!refresh) {
      return HttpResponse.json({ detail: 'No session' }, { status: 401 });
    }
    return HttpResponse.json<LoginResponse>(
      { token: 'mock-jwt', expiresInSeconds: 3600 },
      { headers: { 'Set-Cookie': `bab_refresh=${refresh}-rotated; Path=/api/auth; HttpOnly; SameSite=Strict` } },
    );
  }),

  // Step 32: logout clears the cookie (Max-Age=0) and returns 204.
  http.post(
    `${API_BASE}/api/auth/logout`,
    () =>
      new HttpResponse(null, {
        status: 204,
        headers: { 'Set-Cookie': 'bab_refresh=; Path=/api/auth; HttpOnly; Max-Age=0' },
      }),
  ),

  http.get(`${API_BASE}/api/auth/me`, ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth || !auth.startsWith('Bearer ')) {
      return new HttpResponse(null, { status: 401 });
    }
    return HttpResponse.json<CurrentUser>({ username: 'alice', roles: ['ROLE_USER'] });
  }),

  // Account
  http.get(`${API_BASE}/bank/api/accounts/:id`, ({ params, request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth) return new HttpResponse(null, { status: 401 });
    if (params.id === 'ACC-ERROR') {
      return HttpResponse.json({ detail: 'Account not found' }, { status: 404 });
    }
    return HttpResponse.json<Account>({
      accountNumber: String(params.id),
      currency: 'USD',
      balance: 1500.00,
    });
  }),

  // Ledger Entries
  http.get(`${API_BASE}/bank/api/v1/accounts/:id/entries`, ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth) return new HttpResponse(null, { status: 401 });
    
    const page: Page<LedgerEntry> = {
      content: [
        { transactionId: 'tx-1', direction: 'CREDIT', amount: 500.00, description: 'Deposit', createdAt: '2026-01-01T10:00:00Z' },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    };
    return HttpResponse.json(page);
  }),

  // Transfer
  http.post(`${API_BASE}/bank/api/v1/transfers`, async ({ request }) => {
    const auth = request.headers.get('Authorization');
    if (!auth) return new HttpResponse(null, { status: 401 });
    
    const body = (await request.json()) as { amount: number };
    if (body.amount > 1000000) {
      return HttpResponse.json({ detail: 'Insufficient funds' }, { status: 422 });
    }
    
    return HttpResponse.json({ transactionId: 'mock-tx-uuid' }, { status: 201 });
  }),
];
