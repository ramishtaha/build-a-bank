// libs/common/src/main/java/com/buildabank/common/money/MoneyFormatter.java
package com.buildabank.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Step 28 · the reusable bean this starter provides. Formats a {@link BigDecimal} money amount with a currency
 * prefix and exactly two minor-unit digits — deterministic and locale-free on purpose (no
 * {@code NumberFormat}/locale), so the bank renders money identically everywhere and tests don't depend on the
 * host locale. Money is always {@code BigDecimal} (never {@code double}); rounding is banker's rounding
 * ({@link RoundingMode#HALF_EVEN}). A plain object — the Spring wiring lives in {@link MoneyAutoConfiguration}.
 */
public class MoneyFormatter {

    private final String currencyCode;

    public MoneyFormatter(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    /** Format an amount, e.g. {@code new BigDecimal("1234.5")} → {@code "USD 1234.50"}. */
    public String format(BigDecimal amount) {
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_EVEN);
        return currencyCode + " " + scaled.toPlainString();
    }

    public String currencyCode() {
        return currencyCode;
    }
}
