// libs/common/src/test/java/com/buildabank/common/money/MoneyFormatterTest.java
package com.buildabank.common.money;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/** Step 28 · the formatter's behaviour: two-digit scale, currency prefix, banker's (HALF_EVEN) rounding. */
class MoneyFormatterTest {

    @Test
    void formatsWithCurrencyPrefixAndTwoDecimals() {
        assertThat(new MoneyFormatter("USD").format(new BigDecimal("1234.5"))).isEqualTo("USD 1234.50");
    }

    @Test
    void usesBankersRoundingHalfEven() {
        // 1.005 → preceding digit 0 (even) → rounds down; 1.015 → preceding digit 1 (odd) → rounds up
        assertThat(new MoneyFormatter("USD").format(new BigDecimal("1.005"))).isEqualTo("USD 1.00");
        assertThat(new MoneyFormatter("USD").format(new BigDecimal("1.015"))).isEqualTo("USD 1.02");
    }

    @Test
    void respectsTheConfiguredCurrency() {
        assertThat(new MoneyFormatter("EUR").format(new BigDecimal("9.9"))).isEqualTo("EUR 9.90");
    }
}
