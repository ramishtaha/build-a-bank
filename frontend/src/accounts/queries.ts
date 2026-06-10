// frontend/src/accounts/queries.ts
// Step 30 · TanStack Query hooks — server state (cache, loading/error, refetch) kept OUT of component state.
// The account balance + ledger are queries; a transfer is a mutation that, on success, INVALIDATES those
// queries so the balance/history refetch automatically (no manual re-fetch wiring).
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import * as api from '../api/client';
import { useAuth } from '../auth/AuthContext';

export const accountKeys = {
  account: (accountNumber: string) => ['account', accountNumber] as const,
  entries: (accountNumber: string) => ['entries', accountNumber] as const,
};

/** The current balance for an account. Disabled until we have a token + an account number. */
export function useAccount(accountNumber: string) {
  const { token } = useAuth();
  return useQuery({
    queryKey: accountKeys.account(accountNumber),
    queryFn: () => {
      if (token === null) return Promise.reject(new Error('not authenticated'));
      return api.getAccount(token, accountNumber);
    },
    enabled: token !== null && accountNumber.length > 0,
  });
}

/** The recent ledger entries (paginated; newest first). */
export function useEntries(accountNumber: string) {
  const { token } = useAuth();
  return useQuery({
    queryKey: accountKeys.entries(accountNumber),
    queryFn: () => {
      if (token === null) return Promise.reject(new Error('not authenticated'));
      return api.listEntries(token, accountNumber);
    },
    enabled: token !== null && accountNumber.length > 0,
  });
}

export interface TransferVars {
  request: api.TransferRequest;
  idempotencyKey: string;
}

/** Make a transfer; on success, invalidate every account + entries query so balances/history refresh. */
export function useTransfer() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (vars: TransferVars) => {
      if (token === null) return Promise.reject(new Error('not authenticated'));
      return api.transfer(token, vars.request, vars.idempotencyKey);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['account'] });
      void queryClient.invalidateQueries({ queryKey: ['entries'] });
    },
  });
}
