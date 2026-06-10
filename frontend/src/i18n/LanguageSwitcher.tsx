// frontend/src/i18n/LanguageSwitcher.tsx
// Step 31 · switches the active locale at runtime (react-i18next). Changing it re-renders translated text AND
// re-formats money (AccountPanel formats with the current language), so the whole UI follows the choice.
import { useTranslation } from 'react-i18next';

export function LanguageSwitcher() {
  const { i18n, t } = useTranslation();
  return (
    <label>
      {t('lang.label')}
      <select value={i18n.language} onChange={(event) => void i18n.changeLanguage(event.target.value)}>
        <option value="en">English</option>
        <option value="es">Español</option>
      </select>
    </label>
  );
}
