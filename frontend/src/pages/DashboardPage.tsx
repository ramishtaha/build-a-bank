// frontend/src/pages/DashboardPage.tsx
// Step 30 · the protected dashboard, now composing real features: pick an account, see its balance + recent
// activity (TanStack Query), make a transfer (React Hook Form + Zod → mutation), and watch live notifications
// (SSE). The account selector is a plain text input (default ACC-A) — there's no user↔account mapping yet.
import { useState } from 'react';

import { AccountPanel } from '../accounts/AccountPanel';
import { TransferForm } from '../accounts/TransferForm';
import { useAuth } from '../auth/AuthContext';
import { LiveNotifications } from '../notifications/LiveNotifications';

export function DashboardPage() {
  const { user, logout } = useAuth();
  const [accountNumber, setAccountNumber] = useState('ACC-A');

  return (
    <main>
      <header>
        <h1>Build-a-Bank 🏦</h1>
        <p>
          Signed in as <strong>{user?.username ?? '…'}</strong>
          {user !== null && user.roles.length > 0 ? ` (${user.roles.join(', ')})` : ''}
        </p>
        <button type="button" onClick={logout}>
          Sign out
        </button>
      </header>

      <label>
        View account
        <input value={accountNumber} onChange={(event) => setAccountNumber(event.target.value)} />
      </label>

      <AccountPanel accountNumber={accountNumber} />
      <TransferForm defaultFrom={accountNumber} />
      <LiveNotifications />
    </main>
  );
}
