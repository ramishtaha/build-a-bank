// frontend/e2e-fullstack/bank-capstone.spec.ts
// Step 32 · 🎓 the Phase-F capstone: the ENTIRE course stack, one browser session, zero mocks.
// login (auth 8083, RS256 JWT + rotating refresh cookie) → dashboard (SPA container via the gateway's
// catch-all) → real balance (demand-account 8082 ← Postgres) → transfer (double-entry ledger + Outbox)
// → Kafka (Redpanda) → notification 8084 → SSE back through the gateway → the UI updates live.
// Prereq: deploy/compose.fullstack.yaml up + the four Spring services running (see the lesson / smoke.sh).
import { expect, test, type APIRequestContext } from '@playwright/test';

const GATEWAY = 'http://localhost:8080';
const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

/** Real login via the API — returns a Bearer token for seeding/reading around the UI. */
async function apiToken(request: APIRequestContext): Promise<string> {
  const response = await request.post(`${GATEWAY}/api/auth/login`, {
    data: { username: 'alice', password: 'password' }, // the auth service's seeded demo user
  });
  expect(response.ok()).toBeTruthy();
  return ((await response.json()) as { token: string }).token;
}

/** Create a demo account only if it doesn't exist yet (re-running against a warm database is normal). */
async function ensureAccount(request: APIRequestContext, token: string, accountNumber: string, opening: number) {
  const headers = { Authorization: `Bearer ${token}` };
  const existing = await request.get(`${GATEWAY}/bank/api/accounts/${accountNumber}`, { headers });
  if (existing.ok()) {
    return; // already seeded (a duplicate POST would 500 — the ledger's unique constraint is unhandled)
  }
  const created = await request.post(`${GATEWAY}/bank/api/accounts`, {
    headers,
    data: { accountNumber, currency: 'USD', openingBalance: opening },
  });
  expect(created.status()).toBe(201);
}

async function balanceOf(request: APIRequestContext, token: string, accountNumber: string): Promise<number> {
  const response = await request.get(`${GATEWAY}/bank/api/accounts/${accountNumber}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.ok()).toBeTruthy();
  return ((await response.json()) as { balance: number }).balance;
}

test.beforeAll(async ({ request }) => {
  const token = await apiToken(request);
  await ensureAccount(request, token, 'ACC-A', 1500.0);
  await ensureAccount(request, token, 'ACC-B', 500.0);
});

test('🎓 full-stack money path: login → live balance → transfer → SSE notification → balance updates', async ({
  page,
  request,
}) => {
  const before = await balanceOf(request, await apiToken(request), 'ACC-A');

  // Sign in through the real gateway → real auth service (the SPA itself came from the nginx container).
  await page.goto('/login');
  await page.getByLabel('Username').fill('alice');
  await page.getByLabel('Password').fill('password');
  await page.getByRole('button', { name: 'Sign in' }).click();

  // The dashboard shows ACC-A's REAL balance, read from Postgres through the gateway.
  await expect(page.getByText(usd.format(before))).toBeVisible({ timeout: 15_000 });

  // Send 5.25 through the real form (RHF + Zod → mutation → Idempotency-Key → double-entry ledger).
  const form = page.getByRole('form', { name: 'Transfer' });
  await form.getByLabel('To').fill('ACC-B');
  await form.getByLabel('Amount').fill('5.25');
  await form.getByRole('button', { name: 'Send transfer' }).click();
  await expect(page.getByRole('status')).toContainText('sent', { timeout: 15_000 });

  // The LIVE pipeline: ledger commit → outbox relay (2s poll) → Redpanda → notification → SSE → this DOM.
  await expect(page.getByText('Transfer of 5.25 from ACC-A to ACC-B completed.')).toBeVisible({
    timeout: 20_000,
  });

  // And the balance the user sees refreshed to the post-transfer number (query invalidation → refetch).
  await expect(page.getByText(usd.format(before - 5.25))).toBeVisible({ timeout: 15_000 });
});

test('reload keeps the session: the httpOnly refresh cookie silently restores it (no localStorage!)', async ({
  page,
  context,
}) => {
  await page.goto('/login');
  await page.getByLabel('Username').fill('alice');
  await page.getByLabel('Password').fill('password');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByText(/signed in as/i)).toBeVisible({ timeout: 15_000 });

  // The refresh cookie is in the browser jar — httpOnly (JS can't read it), scoped to /api/auth.
  // (context.cookies(url) returns only cookies that would be SENT to that url — ask with the cookie's path.)
  const cookie = (await context.cookies(`${GATEWAY}/api/auth/refresh`)).find((c) => c.name === 'bab_refresh');
  expect(cookie?.httpOnly).toBe(true);
  expect(cookie?.path).toBe('/api/auth');

  // Reload: in-memory token is GONE; the bootstrap's silent refresh must restore the session.
  await page.reload();
  await expect(page.getByText(/signed in as/i)).toBeVisible({ timeout: 15_000 });

  // And nothing auth-shaped survives in localStorage — the Step-29 debt is paid.
  expect(await page.evaluate(() => localStorage.length)).toBe(0);
});
