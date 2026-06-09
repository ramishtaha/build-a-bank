// playground/java-basics/src/test/java/com/buildabank/basics/analytics/TransactionAnalyticsTest.java
package com.buildabank.basics.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.buildabank.basics.money.Money;
import com.buildabank.basics.txn.Transaction;
import com.buildabank.basics.txn.TransactionType;

class TransactionAnalyticsTest {

    private static final Instant T0 = Instant.parse("2026-06-01T09:00:00Z");

    private final List<Transaction> txns = List.of(
            new Transaction("T1", Money.of("2000.00", "USD"), TransactionType.CREDIT, T0),
            new Transaction("T2", Money.of("750.00", "USD"), TransactionType.DEBIT, T0.plus(1, ChronoUnit.DAYS)),
            new Transaction("T3", Money.of("125.50", "USD"), TransactionType.DEBIT, T0.plus(2, ChronoUnit.DAYS)));

    @Test
    void sumsAmountsByType() {
        assertThat(TransactionAnalytics.totalAmount(txns, TransactionType.CREDIT)).isEqualByComparingTo("2000.00");
        assertThat(TransactionAnalytics.totalAmount(txns, TransactionType.DEBIT)).isEqualByComparingTo("875.50");
    }

    @Test
    void computesNetMovement() {
        assertThat(TransactionAnalytics.net(txns)).isEqualByComparingTo(new BigDecimal("1124.50"));
    }

    @Test
    void countsByType() {
        assertThat(TransactionAnalytics.countByType(txns))
                .containsEntry(TransactionType.CREDIT, 1L)
                .containsEntry(TransactionType.DEBIT, 2L);
    }

    @Test
    void findsLargestTransaction() {
        assertThat(TransactionAnalytics.largest(txns)).get()
                .extracting(Transaction::id).isEqualTo("T1");
    }

    @Test
    void largestOfEmptyIsEmptyOptional() {
        assertThat(TransactionAnalytics.largest(List.of())).isEmpty();
    }

    @Test
    void filtersSinceAndSortsNewestFirst() {
        var recent = TransactionAnalytics.since(txns, T0.plus(1, ChronoUnit.DAYS));
        assertThat(recent).extracting(Transaction::id).containsExactly("T3", "T2");
    }
}
