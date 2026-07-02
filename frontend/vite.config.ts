/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react';
import { visualizer } from 'rollup-plugin-visualizer';
import { defineConfig } from 'vite';

// Step 29 · Vite config + Vitest (jsdom) for component/route tests. The dev server runs on 5173 (the origin
// the gateway's CORS allow-list expects). Tests run in jsdom with Testing Library matchers from src/test/setup.ts.
// Step 32 · production-build shaping: a stable react-vendor chunk (framework code changes ~never — returning
// visitors keep it cached across app deploys) + a treemap report (dist/stats.html) to SEE what's in the bundle.
export default defineConfig({
  plugins: [
    react(),
    visualizer({ filename: 'dist/stats.html', gzipSize: true }), // open dist/stats.html after `npm run build`
  ],
  server: {
    port: 5173,
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
        },
      },
    },
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
    // Step 32 · …and e2e-fullstack/ (the capstone suite needs the real stack; playwright.fullstack.config.ts).
    exclude: ['**/node_modules/**', '**/dist/**', 'e2e/**', 'e2e-fullstack/**'],
  },
});
