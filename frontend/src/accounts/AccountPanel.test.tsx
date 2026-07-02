// frontend/src/accounts/AccountPanel.test.tsx
// Step 30 · the data view goes loading → data. The api/client module is mocked (MSW arrives Step 31); the token
// is seeded into localStorage so the queries are enabled.
import { screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import * as api from '../api/client';
import { renderWithProviders } from '../test/renderWithProviders';
import { AccountPanel } from './AccountPanel';

vi.mock('../api/client');

describe('AccountPanel', () => {
  beforeEach(() => {
    // Step 32: queries are enabled once the session bootstrap succeeds — mock the silent refresh + identity.
    vi.mocked(api.refreshAccessToken).mockResolvedValue('jwt-123');
    vi.mocked(api.getCurrentUser).mockResolvedValue({ username: 'alice', roles: ['ROLE_USER'] });
    vi.mocked(api.getAccount).mockResolvedValue({ accountNumber: 'ACC-A', currency: 'USD', balance: 200 });
    vi.mocked(api.listEntries).mockResolvedValue({
      content: [{ transactionId: 't1', direction: 'CREDIT', amount: 50, description: 'pay', createdAt: '2026-06-10T00:00:00Z' }],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    });
  });

  it('renders the balance and recent activity once loaded', async () => {
    renderWithProviders(<AccountPanel accountNumber="ACC-A" />);

    expect(await screen.findByText(/\$200\.00/)).toBeInTheDocument(); // Intl en-US currency formatting
    expect(await screen.findByText(/pay/)).toBeInTheDocument();
    expect(api.getAccount).toHaveBeenCalledWith('ACC-A'); // Step 32: no token arg — the client attaches it
  });

  it('shows an error message when the account fails to load', async () => {
    vi.mocked(api.getAccount).mockRejectedValue(new Error('account ACC-X not found'));
    renderWithProviders(<AccountPanel accountNumber="ACC-A" />);

    expect(await screen.findByText(/account ACC-X not found/)).toBeInTheDocument();
  });
});
