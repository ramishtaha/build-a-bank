/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// Step 29 · Vite config + Vitest (jsdom) for component/route tests. The dev server runs on 5173 (the origin
// the gateway's CORS allow-list expects). Tests run in jsdom with Testing Library matchers from src/test/setup.ts.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    // A real (non-opaque) origin so jsdom exposes localStorage — about:blank's opaque origin leaves the
    // localStorage getter throwing, which Vitest skips, making the global `undefined` (AuthContext needs it).
    environmentOptions: {
      jsdom: { url: 'http://localhost:5173' },
    },
    setupFiles: './src/test/setup.ts',
    css: false,
    // Step 31 · Playwright owns e2e/ — Vitest must not try to load those specs (different runner, different APIs).
    exclude: ['**/node_modules/**', '**/dist/**', 'e2e/**'],
  },
});
