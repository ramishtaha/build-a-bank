import { describe, expect, it } from 'vitest';
import { formatCurrency } from './currency';

describe('formatCurrency', () => {
  it('formats USD correctly in en-US', () => {
    expect(formatCurrency(1234.56)).toBe('$1,234.56');
  });

  it('formats EUR correctly in de-DE', () => {
    // de-DE locale formats as 1.234,56 €
    // Note: there is a non-breaking space before the euro symbol
    const formatted = formatCurrency(1234.56, 'EUR', 'de-DE');
    expect(formatted.replace(/\s/, ' ')).toBe('1.234,56 €');
  });

  it('formats JPY correctly in ja-JP', () => {
    expect(formatCurrency(1234.56, 'JPY', 'ja-JP')).toBe('￥1,235');
  });
});
