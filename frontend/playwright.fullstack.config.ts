// frontend/playwright.fullstack.config.ts
// Step 32 · the Phase-F capstone config: NO webServer, NO route mocks — the browser hits the REAL gateway
// (http://localhost:8080), which serves the shipped SPA container and fronts the real services. Start the
// stack first (deploy/compose.fullstack.yaml + the four Spring services); run with:
//   npm run test:e2e:fullstack
// Kept separate from playwright.config.ts so the hermetic suite (`npm run test:e2e`) still runs anywhere,
// Docker or not.
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e-fullstack',
  timeout: 60_000, // real network + a 2s outbox poll + Kafka + SSE — generous beats flaky
  fullyParallel: false, // sequential: the money test computes an expected balance — no concurrent transfers
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:8080', // the gateway — the ONLY origin the browser ever sees
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
