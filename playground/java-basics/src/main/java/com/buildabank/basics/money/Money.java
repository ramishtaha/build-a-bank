// playground/java-basics/src/main/java/com/buildabank/basics/money/Money.java
package com.buildabank.basics.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * A money value object — the FIRST rule of banking code: never use {@code double} for money.
 *
 * <p>Implemented as a {@code record}: an immutable, transparent data carrier. The compiler generates
 * the constructor, accessors ({@code amount()}, {@code currency()}), {@code equals}, {@code hashCode},
 * and {@code toString} for us. We add a <em>compact constructor</em> to validate and normalize.
 *
 * <p>Money is stored as {@link BigDecimal} (exact decimal arithmetic) scaled to the currency's
 * minor units (2 for USD/EUR, 0 for JPY), rounded {@link RoundingMode#HALF_EVEN} ("banker's rounding").
 */
public record Money(BigDecimal amount, Currency currency) {

    /** Compact constructor: runs before the fields are assigned. Validate + normalize here. */
    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
    }

    /** Convenience factory: {@code Money.of("100.00", "USD")}. */
    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money times(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: %s vs %s".formatted(currency.getCurrencyCode(), other.currency.getCurrencyCode()));
        }
    }

    @Override
    public String toString() {
        return "%s %s".formatted(amount.toPlainString(), currency.getCurrencyCode());
    }
}
