// frontend/src/i18n/format.test.ts
// Step 31 · multi-currency formatting is locale-correct (symbol, grouping, decimal places) via Intl.
import { describe, expect, it } from 'vitest';

import { formatMoney } from './format';

describe('formatMoney', () => {
  it('formats USD in en-US', () => {
    expect(formatMoney(1234.5, 'USD', 'en-US')).toBe('$1,234.50');
  });

  it('formats EUR in de-DE (German grouping + trailing symbol)', () => {
    const formatted = formatMoney(1234.5, 'EUR', 'de-DE');
    expect(formatted).toContain('1.234,50'); // dot-grouping, comma-decimal
    expect(formatted).toContain('€');
  });

  it('formats JPY with no decimal places', () => {
    expect(formatMoney(1234, 'JPY', 'en-US')).toBe('¥1,234');
  });
});
