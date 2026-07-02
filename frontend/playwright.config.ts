// frontend/playwright.config.ts
// Step 31 · Playwright E2E. Boots the Vite dev server itself (webServer) and runs specs from e2e/ in a real
// Chromium. VITE_API_BASE_URL is set to '' so the app issues SAME-ORIGIN relative requests — the specs
// intercept them with page.route(), keeping the E2E hermetic (no backend needed) and CORS-free.
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  fullyParallel: true,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    env: { VITE_API_BASE_URL: '' },
    timeout: 60_000,
  },
});
