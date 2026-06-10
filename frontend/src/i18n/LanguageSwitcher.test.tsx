// frontend/src/i18n/LanguageSwitcher.test.tsx
// Step 31 · switching the locale re-renders translated text (English → Spanish). i18n is initialized in the test
// setup and reset to English after each test.
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useTranslation } from 'react-i18next';
import { describe, expect, it } from 'vitest';

import { LanguageSwitcher } from './LanguageSwitcher';

function Sample() {
  const { t } = useTranslation();
  return (
    <div>
      <LanguageSwitcher />
      <p>{t('dashboard.signOut')}</p>
    </div>
  );
}

describe('LanguageSwitcher', () => {
  it('switches translated text from English to Spanish', async () => {
    render(<Sample />);

    expect(screen.getByText('Sign out')).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText(/language|idioma/i), 'es');

    expect(await screen.findByText('Cerrar sesión')).toBeInTheDocument();
    expect(screen.queryByText('Sign out')).not.toBeInTheDocument();
  });
});
