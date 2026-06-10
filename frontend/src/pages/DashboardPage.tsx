// frontend/src/pages/DashboardPage.tsx
// Step 30 · the protected dashboard: pick an account, see balance + recent activity (TanStack Query), make a
// transfer (RHF + Zod → mutation), and watch live notifications (SSE).
// Step 31 · localized labels (react-i18next) + a language switcher; the account selector has an explicit label.
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { AccountPanel } from '../accounts/AccountPanel';
import { TransferForm } from '../accounts/TransferForm';
import { useAuth } from '../auth/AuthContext';
import { LanguageSwitcher } from '../i18n/LanguageSwitcher';
import { LiveNotifications } from '../notifications/LiveNotifications';

export function DashboardPage() {
  const { user, logout } = useAuth();
  const { t } = useTranslation();
  const [accountNumber, setAccountNumber] = useState('ACC-A');

  return (
    <main>
      <header>
        <h1>{t('dashboard.title')}</h1>
        <p>
          {t('dashboard.signedInAs')} <strong>{user?.username ?? '…'}</strong>
          {user !== null && user.roles.length > 0 ? ` (${user.roles.join(', ')})` : ''}
        </p>
        <LanguageSwitcher />
        <button type="button" onClick={logout}>
          {t('dashboard.signOut')}
        </button>
      </header>

      <label>
        {t('dashboard.viewAccount')}
        <input
          aria-label={t('dashboard.viewAccount')}
          value={accountNumber}
          onChange={(event) => setAccountNumber(event.target.value)}
        />
      </label>

      <AccountPanel accountNumber={accountNumber} />
      <TransferForm defaultFrom={accountNumber} />
      <LiveNotifications />
    </main>
  );
}
