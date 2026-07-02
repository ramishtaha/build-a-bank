// frontend/e2e/transfer-flow.spec.ts
// Step 31 · the money path in a real browser: sign in → see the balance → send a transfer → watch the live
// (SSE) notification arrive. The gateway is mocked at the network layer with page.route() — same contract the
// MSW handlers use in the Vitest suite, but exercised through real Chromium networking.
import { test, expect, type Page } from '@playwright/test';

const account = { accountNumber: 'ACC-A', currency: 'USD', balance: 1500.0 };

const entriesPage = {
  content: [
    {
      transactionId: 'tx-1',
      direction: 'CREDIT',
      amount: 500.0,
      description: 'Deposit',
      createdAt: '2026-01-01T10:00:00Z',
    },
  ],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
};

const sseBody = [
  'retry: 600000', // keep EventSource from hammering reconnects after the body completes
  'event: transfer',
  `data: ${JSON.stringify({
    eventId: 'evt-1',
    transactionId: 'tx-live-1',
    fromAccount: 'ACC-A',
    toAccount: 'ACC-B',
    amount: 25,
    occurredAt: '2026-07-02T10:00:00Z',
    message: 'ACC-A sent 25.00 USD to ACC-B',
  })}`,
  '',
  '',
].join('\n');

/** Wire the mocked gateway: auth, account, entries, transfers, SSE stream. */
async function mockGateway(page: Page) {
  // Step 32: the app runs a silent refresh on mount — answer 401 so these hermetic specs start anonymous.
  await page.route('**/api/auth/refresh', (route) =>
    route.fulfill({ status: 401, json: { detail: 'No session' } }),
  );
  await page.route('**/api/auth/login', async (route) => {
    const body = route.request().postDataJSON() as { username: string; password: string };
    if (body.username === 'alice' && body.password === 'password123') {
      await route.fulfill({ json: { token: 'e2e-jwt', expiresInSeconds: 3600 } });
    } else {
      await route.fulfill({ status: 401, json: { detail: 'Bad credentials' } });
    }
  });
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({ json: { username: 'alice', roles: ['ROLE_USER'] } }),
  );
  await page.route('**/bank/api/accounts/*', (route) => route.fulfill({ json: account }));
  await page.route('**/bank/api/v1/accounts/*/entries*', (route) => route.fulfill({ json: entriesPage }));
  await page.route('**/bank/api/v1/transfers', async (route) => {
    // assert the client sends the Idempotency-Key header (Step 14's public-API idempotency, from the browser)
    const idempotencyKey = route.request().headers()['idempotency-key'];
    if (!idempotencyKey) {
      await route.fulfill({ status: 400, json: { detail: 'Missing Idempotency-Key' } });
      return;
    }
    await route.fulfill({ status: 201, json: { transactionId: 'e2e-tx-42' } });
  });
  await page.route('**/notifications/api/notifications/stream', (route) =>
    route.fulfill({
      status: 200,
      headers: { 'content-type': 'text/event-stream', 'cache-control': 'no-cache' },
      body: sseBody,
    }),
  );
}

test('sign in → balance → transfer → live SSE notification', async ({ page }) => {
  await mockGateway(page);

  await page.goto('/login');
  await page.getByLabel('Username').fill('alice');
  await page.getByLabel('Password').fill('password123');
  await page.getByRole('button', { name: 'Sign in' }).click();

  // Dashboard: balance from the mocked account, formatted as localized currency.
  await expect(page.getByText('$1,500.00')).toBeVisible();

  // Live notification pushed over the (mocked) SSE stream.
  await expect(page.getByText('ACC-A sent 25.00 USD to ACC-B')).toBeVisible();

  // Send a transfer through the real form (RHF + Zod validation runs in-browser).
  const form = page.getByRole('form', { name: 'Transfer' });
  await form.getByLabel('To').fill('ACC-B');
  await form.getByLabel('Amount').fill('25');
  await form.getByRole('button', { name: 'Send transfer' }).click();

  await expect(page.getByRole('status')).toContainText('Transfer e2e-tx-42 sent');
});

test('wrong password shows an accessible error and stays on /login', async ({ page }) => {
  await mockGateway(page);

  await page.goto('/login');
  await page.getByLabel('Username').fill('alice');
  await page.getByLabel('Password').fill('wrong');
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page.getByRole('alert')).toContainText('Login failed');
  await expect(page).toHaveURL(/\/login$/);
});
