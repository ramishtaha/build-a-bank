// frontend/src/api/client.ts
// The typed HTTP client. ONE base URL — the gateway (the single front door, Step 15) — via VITE_API_BASE_URL.
// Auth (Step 29): /api/auth/* (no prefix). Demand-account (Step 30): under /bank/* (the gateway strips /bank).
// Errors are parsed from RFC 9457 ProblemDetail bodies so the UI can show "Insufficient funds", etc.

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

function authHeader(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` };
}

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
  return (await response.json()) as T;
}

// ── Auth (Step 29) ──────────────────────────────────────────────────────────
export function login(username: string, password: string): Promise<LoginResponse> {
  return request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}

export function getCurrentUser(token: string): Promise<CurrentUser> {
  return request<CurrentUser>('/api/auth/me', { headers: authHeader(token) });
}

// ── Demand-account (Step 30, via the gateway's /bank prefix; all require the Bearer token) ───────────────
export function getAccount(token: string, accountNumber: string): Promise<Account> {
  return request<Account>(`/bank/api/accounts/${encodeURIComponent(accountNumber)}`, {
    headers: authHeader(token),
  });
}

export function listEntries(
  token: string,
  accountNumber: string,
  page = 0,
  size = 10,
): Promise<Page<LedgerEntry>> {
  const query = new URLSearchParams({ page: String(page), size: String(size), sort: 'createdAt,desc' });
  return request<Page<LedgerEntry>>(
    `/bank/api/v1/accounts/${encodeURIComponent(accountNumber)}/entries?${query.toString()}`,
    { headers: authHeader(token) },
  );
}

/** Idempotent v1 transfer — a fresh Idempotency-Key per attempt (retrying with the same key won't double-pay). */
export function transfer(
  token: string,
  body: TransferRequest,
  idempotencyKey: string,
): Promise<TransferResult> {
  return request<TransferResult>('/bank/api/v1/transfers', {
    method: 'POST',
    headers: { ...authHeader(token), 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(body),
  });
}
