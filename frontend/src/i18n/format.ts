// frontend/src/i18n/format.ts
// Step 31 · locale-aware, multi-currency formatting via the built-in Intl API (no library, full ICU in Node 22 /
// modern browsers). Money is formatted per currency + locale (symbol, grouping, decimals all locale-correct).
export function formatMoney(amount: number, currency: string, locale = 'en-US'): string {
  return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(amount);
}

export function formatDateTime(iso: string, locale = 'en-US'): string {
  return new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso));
}
