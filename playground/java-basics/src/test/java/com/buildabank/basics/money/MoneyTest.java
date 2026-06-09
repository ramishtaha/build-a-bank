// playground/java-basics/src/test/java/com/buildabank/basics/money/MoneyTest.java
package com.buildabank.basics.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Money — exact decimal money arithmetic")
class MoneyTest {

    @Test
    void normalizesScaleToTheCurrency() {
        // "100" with no fraction → scaled to 2 dp for USD.
        assertThat(Money.of("100", "USD").amount().toPlainString()).isEqualTo("100.00");
    }

    @Test
    void addsAndSubtractsWithinTheSameCurrency() {
        Money balance = Money.of("1250.00", "USD");
        assertThat(balance.plus(Money.of("2000.00", "USD"))).isEqualTo(Money.of("3250.00", "USD"));
        assertThat(balance.minus(Money.of("250.50", "USD"))).isEqualTo(Money.of("999.50", "USD"));
    }

    @Test
    void rejectsCrossCurrencyArithmetic() {
        assertThatThrownBy(() -> Money.of("10.00", "USD").plus(Money.of("10.00", "EUR")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void usesBankersRoundingHalfEven() {
        // 2.125 at 2 dp → 2.12 (round half to even), not 2.13.
        assertThat(Money.of("2.125", "USD").amount().toPlainString()).isEqualTo("2.12");
    }

    @Test
    void recordEqualityIsValueBased() {
        assertThat(Money.of("5.00", "USD")).isEqualTo(Money.of("5.00", "USD"));
    }
}
