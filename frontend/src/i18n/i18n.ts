// frontend/src/i18n/i18n.ts
// Step 31 · internationalization with react-i18next. Resources are inlined (synchronous init — no async backend),
// so translations are available immediately in the app AND in tests. English is the default + fallback; Spanish
// demonstrates a second locale. Importing this module initializes the shared i18n instance.
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

export const resources = {
  en: {
    translation: {
      'dashboard.title': 'Build-a-Bank 🏦',
      'dashboard.signedInAs': 'Signed in as',
      'dashboard.signOut': 'Sign out',
      'dashboard.viewAccount': 'View account',
      'lang.label': 'Language',
    },
  },
  es: {
    translation: {
      'dashboard.title': 'Build-a-Bank 🏦',
      'dashboard.signedInAs': 'Sesión iniciada como',
      'dashboard.signOut': 'Cerrar sesión',
      'dashboard.viewAccount': 'Ver cuenta',
      'lang.label': 'Idioma',
    },
  },
} as const;

void i18n.use(initReactI18next).init({
  resources,
  lng: 'en',
  fallbackLng: 'en',
  interpolation: { escapeValue: false }, // React already escapes
});

export default i18n;
