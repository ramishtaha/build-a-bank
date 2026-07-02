// frontend/src/accounts/queries.ts
// Step 30 · TanStack Query hooks — server state (cache, loading/error, refetch) kept OUT of component state.
// The account balance + ledger are queries; a transfer is a mutation that, on success, INVALIDATES those
// queries so the balance/history refetch automatically (no manual re-fetch wiring).
// Step 32 · no more token threading: the api client attaches the in-memory token (and refreshes on 401)
// itself. Queries just gate on "is anyone signed in".
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import * as api from '../api/client';
import { useAuth } from '../auth/AuthContext';

export const accountKeys = {
  account: (accountNumber: string) => ['account', accountNumber] as const,
  entries: (accountNumber: string) => ['entries', accountNumber] as const,
};

/** The current balance for an account. Disabled until the session is established + we have an account number. */
export function useAccount(accountNumber: string) {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: accountKeys.account(accountNumber),
    queryFn: () => api.getAccount(accountNumber),
    enabled: isAuthenticated && accountNumber.length > 0,
  });
}

/** The recent ledger entries (paginated; newest first). */
export function useEntries(accountNumber: string) {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: accountKeys.entries(accountNumber),
    queryFn: () => api.listEntries(accountNumber),
    enabled: isAuthenticated && accountNumber.length > 0,
  });
}

export interface TransferVars {
  request: api.TransferRequest;
  idempotencyKey: string;
}

/** Make a transfer; on success, invalidate every account + entries query so balances/history refresh. */
export function useTransfer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (vars: TransferVars) => api.transfer(vars.request, vars.idempotencyKey),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['account'] });
      void queryClient.invalidateQueries({ queryKey: ['entries'] });
    },
  });
}
