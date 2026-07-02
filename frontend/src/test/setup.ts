// frontend/src/test/setup.ts
// Step 29 · Vitest setup — registers jest-dom matchers (toBeInTheDocument, …) and resets state between tests
// so they're independent. (The Step-29 localStorage shim is GONE — Step 32 removed the token from
// localStorage, and nothing else touches it.)
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { beforeAll, afterEach, afterAll } from 'vitest';
import { server } from '../mocks/server';

import { tokenStore } from '../auth/tokenStore';
import i18n from '../i18n/i18n'; // Step 31 · the shared i18n instance (initialized on import; synchronous resources)

// jsdom doesn't implement EventSource (Step 30 SSE). Install a no-op stub so components that mount the SSE hook
// don't crash; tests that exercise streaming install their own controllable EventSource via vi.stubGlobal.
if (typeof globalThis.EventSource === 'undefined') {
  class NoopEventSource {
    onopen: (() => void) | null = null;
    onerror: (() => void) | null = null;
    addEventListener(): void {}
    removeEventListener(): void {}
    close(): void {}
  }
  globalThis.EventSource = NoopEventSource as unknown as typeof EventSource;
}

// Establish API mocking before all tests.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

afterEach(() => {
  cleanup();
  tokenStore.clear(); // Step 32: the in-memory access token is module state — reset it between tests
  void i18n.changeLanguage('en'); // reset locale so a language-switch test doesn't leak into the next
  // Reset any request handlers that we may add during the tests,
  // so they don't affect other tests.
  server.resetHandlers();
});

// Clean up after the tests are finished.
afterAll(() => server.close());
