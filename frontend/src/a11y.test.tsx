// frontend/src/a11y.test.tsx
// Step 31 · automated accessibility checks with axe-core. We scan the key forms for WCAG A/AA violations
// (labels, names, roles). color-contrast is disabled — it needs real layout, which jsdom doesn't compute.
import axe from 'axe-core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { TransferForm } from './accounts/TransferForm';
import * as api from './api/client';
import { LoginPage } from './pages/LoginPage';
import { renderWithProviders } from './test/renderWithProviders';

// Step 32: AuthProvider now bootstraps with an async silent refresh; if it settled mid-scan React would warn
// about an un-act()ed state update. a11y doesn't care about auth — hold the bootstrap open (never settles).
vi.mock('./api/client');

beforeEach(() => {
  vi.mocked(api.refreshAccessToken).mockReturnValue(new Promise(() => undefined));
});

async function wcagViolations(container: HTMLElement) {
  const results = await axe.run(container, {
    runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] },
    rules: { 'color-contrast': { enabled: false } },
  });
  return results.violations.map((violation) => violation.id);
}

describe('accessibility (axe-core, WCAG A/AA)', () => {
  it('the login page has no violations', async () => {
    const { container } = renderWithProviders(<LoginPage />);
    expect(await wcagViolations(container)).toEqual([]);
  });

  it('the transfer form has no violations', async () => {
    const { container } = renderWithProviders(<TransferForm defaultFrom="ACC-A" />);
    expect(await wcagViolations(container)).toEqual([]);
  });
});
