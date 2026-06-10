// frontend/src/accounts/AccountPanel.tsx
// Step 30 · reads server state via TanStack Query and renders the three states every data view needs:
// loading, error, and data. The balance and the ledger are independent queries (they load/refresh on their own).
import { useAccount, useEntries } from './queries';

function money(amount: number, currency: string): string {
  return `${currency} ${amount.toFixed(2)}`;
}

export function AccountPanel({ accountNumber }: { accountNumber: string }) {
  const account = useAccount(accountNumber);
  const entries = useEntries(accountNumber);

  return (
    <section aria-label="Account">
      <h2>Account {accountNumber}</h2>

      {account.isLoading && <p>Loading balance…</p>}
      {account.isError && <p role="alert">Couldn’t load the account: {account.error?.message}</p>}
      {account.data && (
        <p>
          Balance: <strong>{money(account.data.balance, account.data.currency)}</strong>
        </p>
      )}

      <h3>Recent activity</h3>
      {entries.isLoading && <p>Loading activity…</p>}
      {entries.isError && <p role="alert">Couldn’t load activity: {entries.error?.message}</p>}
      {entries.data &&
        (entries.data.content.length === 0 ? (
          <p>No transactions yet.</p>
        ) : (
          <ul>
            {entries.data.content.map((entry) => (
              <li key={`${entry.transactionId}-${entry.direction}`}>
                {entry.direction === 'DEBIT' ? '−' : '+'}
                {entry.amount.toFixed(2)} · {entry.description ?? '—'}
              </li>
            ))}
          </ul>
        ))}
    </section>
  );
}
