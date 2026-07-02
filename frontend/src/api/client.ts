// frontend/src/api/client.ts
// The typed HTTP client. ONE base URL — the gateway (the single front door, Step 15) — via VITE_API_BASE_URL.
// Auth (Step 29): /api/auth/* (no prefix). Demand-account (Step 30): under /bank/* (the gateway strips /bank).
// Errors are parsed from RFC 9457 ProblemDetail bodies so the UI can show "Insufficient funds", etc.
//
// Step 32 · the client owns the session plumbing: the access token lives in tokenStore (in-memory), every
// protected call attaches it itself, and a 401 triggers ONE silent refresh (single-flight) + retry. React
// components never see a token — they just call typed functions.

import { tokenStore } from '../auth/tokenStore';

export interface LoginResponse {
  token: string;
  expiresInSeconds: number;
}

export interface CurrentUser {
  username: string;
  roles: string[];
}

export interface Account {
  accountNumber: string;
  currency: string;
  balance: number;
}

export type EntryDirection = 'DEBIT' | 'CREDIT';

export interface LedgerEntry {
  transactionId: string;
  direction: EntryDirection;
  amount: number;
  description: string | null;
  createdAt: string;
}

/** The demand-account paginated envelope (Step 14's PageResponse). */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TransferRequest {
  from: string;
  to: string;
  amount: number;
  description?: string;
}

export interface TransferResult {
  transactionId: string;
}

/** A failed HTTP response, carrying the status + a human message (from the ProblemDetail body when present). */
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
    let message = `Request to ${path} failed (${response.status})`;
    try {
      const problem = (await response.json()) as { detail?: string; title?: string };
      message = problem.detail ?? problem.title ?? message; // RFC 9457 ProblemDetail
    } catch {
      // non-JSON / empty body (e.g. a 404 with no body) — keep the default message
    }
    throw new ApiError(response.status, message);
  }
  if (response.status === 204) {
    return undefined as T; // logout has no body
  }
  return (await response.json()) as T;
}

// ── Session plumbing (Step 32) ──────────────────────────────────────────────

let refreshInFlight: Promise<string | null> | null = null;

/**
 * Trade the httpOnly refresh cookie for a fresh access token (and a rotated cookie). SINGLE-FLIGHT: if five
 * queries hit 401 at once, they all await the SAME refresh — five parallel refreshes would rotate the cookie
 * five times and trip the server's reuse detection (which revokes the whole session family).
 * Resolves null when there is no live session (no cookie / revoked / expired).
 */
export function refreshAccessToken(): Promise<string | null> {
  // credentials:'include' — in cross-origin dev (5173 → 8080) fetch neither sends nor stores cookies
  // without it. Same-origin (the shipped topology) it's a harmless no-op.
  const attempt = () =>
    request<LoginResponse>('/api/auth/refresh', { method: 'POST', credentials: 'include' });

  refreshInFlight ??= (async () => {
    try {
      let response: LoginResponse;
      try {
        response = await attempt();
      } catch (error) {
        // 409 = another tab rotated the shared cookie mid-flight (a benign race the server distinguishes
        // from theft). The browser already holds the successor cookie — retry ONCE with it.
        if (error instanceof ApiError && error.status === 409) {
          response = await attempt();
        } else {
          throw error;
        }
      }
      tokenStore.set(response.token);
      return response.token;
    } catch {
      tokenStore.clear();
      return null;
    } finally {
      refreshInFlight = null; // next 401 starts a fresh attempt
    }
  })();
  return refreshInFlight;
}

function withBearer(init: RequestInit, token: string): RequestInit {
  return { ...init, headers: { ...init.headers, Authorization: `Bearer ${token}` } };
}

/**
 * A protected call: attach the in-memory token; on 401 (access token expired), refresh ONCE and retry.
 * If the refresh itself fails, the session is over — announce it (AuthContext listens and logs out).
 */
async function authorizedRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = tokenStore.get() ?? (await refreshAccessToken());
  if (token === null) {
    tokenStore.notifySessionExpired();
    throw new ApiError(401, 'Not signed in');
  }
  try {
    return await request<T>(path, withBearer(init, token));
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) {
      throw error; // only a 401 means "token expired" — anything else is the caller's problem
    }
    const fresh = await refreshAccessToken();
    if (fresh === null) {
      tokenStore.notifySessionExpired();
      throw error;
    }
    return request<T>(path, withBearer(init, fresh)); // one retry — never loop
  }
}

// ── Auth (Step 29; session lifecycle Step 32) ───────────────────────────────

/** Sign in: the body carries the access token (kept in memory); the refresh cookie rides in httpOnly. */
export async function login(username: string, password: string): Promise<LoginResponse> {
  const response = await request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    credentials: 'include', // accept the Set-Cookie in cross-origin dev
    body: JSON.stringify({ username, password }),
  });
  tokenStore.set(response.token);
  return response;
}

/** Sign out: revoke the refresh family server-side + clear the cookie, then drop the in-memory token. */
export async function logout(): Promise<void> {
  try {
    await request<void>('/api/auth/logout', { method: 'POST', credentials: 'include' });
  } finally {
    tokenStore.clear();
  }
}

export function getCurrentUser(): Promise<CurrentUser> {
  return authorizedRequest<CurrentUser>('/api/auth/me');
}

// ── Demand-account (Step 30, via the gateway's /bank prefix; all protected) ─────────────────────────────
export function getAccount(accountNumber: string): Promise<Account> {
  return authorizedRequest<Account>(`/bank/api/accounts/${encodeURIComponent(accountNumber)}`);
}

export function listEntries(accountNumber: string, page = 0, size = 10): Promise<Page<LedgerEntry>> {
  const query = new URLSearchParams({ page: String(page), size: String(size), sort: 'createdAt,desc' });
  return authorizedRequest<Page<LedgerEntry>>(
    `/bank/api/v1/accounts/${encodeURIComponent(accountNumber)}/entries?${query.toString()}`,
  );
}

/** Idempotent v1 transfer — a fresh Idempotency-Key per attempt (retrying with the same key won't double-pay). */
export function transfer(body: TransferRequest, idempotencyKey: string): Promise<TransferResult> {
  return authorizedRequest<TransferResult>('/bank/api/v1/transfers', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(body),
  });
}
