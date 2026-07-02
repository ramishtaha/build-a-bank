// frontend/src/accounts/TransferForm.test.tsx
// Step 30 · forms: Zod validation blocks a bad submit (and the API is never called); a valid submit fires the
// transfer with the typed values + an Idempotency-Key. The api/client module is mocked.
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import * as api from '../api/client';
import { renderWithProviders } from '../test/renderWithProviders';
import { TransferForm } from './TransferForm';

vi.mock('../api/client');

describe('TransferForm', () => {
  beforeEach(() => {
    vi.mocked(api.transfer).mockResolvedValue({ transactionId: 'txn-1' });
  });

  it('blocks an invalid submit and shows validation errors (API not called)', async () => {
    renderWithProviders(<TransferForm defaultFrom="ACC-A" />);

    // leave "to" empty and amount at 0 (not > 0), then submit
    await userEvent.click(screen.getByRole('button', { name: /send transfer/i }));

    expect(await screen.findByText(/to account is required/i)).toBeInTheDocument();
    expect(screen.getByText(/amount must be greater than 0/i)).toBeInTheDocument();
    expect(api.transfer).not.toHaveBeenCalled();
  });

  it('submits a valid transfer with the typed values + an Idempotency-Key', async () => {
    renderWithProviders(<TransferForm defaultFrom="ACC-A" />);

    await userEvent.type(screen.getByLabelText(/^to$/i), 'ACC-B');
    const amount = screen.getByLabelText(/amount/i);
    await userEvent.clear(amount);
    await userEvent.type(amount, '50');
    await userEvent.click(screen.getByRole('button', { name: /send transfer/i }));

    await waitFor(() => expect(api.transfer).toHaveBeenCalledTimes(1));
    const [body, idempotencyKey] = vi.mocked(api.transfer).mock.calls[0]; // Step 32: no token arg
    expect(body).toMatchObject({ from: 'ACC-A', to: 'ACC-B', amount: 50 });
    expect(idempotencyKey).toMatch(/[0-9a-f-]{36}/i); // a UUID
  });
});
