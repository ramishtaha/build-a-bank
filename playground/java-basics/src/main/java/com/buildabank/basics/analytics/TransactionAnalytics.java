// playground/java-basics/src/main/java/com/buildabank/basics/analytics/TransactionAnalytics.java
package com.buildabank.basics.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.buildabank.basics.txn.Transaction;
import com.buildabank.basics.txn.TransactionType;

/**
 * Read-only analytics over a list of {@link Transaction}, built with the <strong>Stream API</strong>.
 *
 * <p>Streams express "what" (filter → map → reduce/collect) instead of "how" (index loops). Each method
 * here is a tiny worked example of a different stream idiom: {@code filter}+{@code reduce}, {@code groupingBy}
 * with {@code counting}, {@code max} returning an {@link Optional}, and {@code map}+{@code toList}.
 *
 * <p>Note: summing money correctly needs a common currency; these helpers assume a single currency for the
 * teaching example (the real ledger enforces that in Step 12).
 */
public final class TransactionAnalytics {

    private TransactionAnalytics() { }

    /** Sum of amounts for one type, as a raw {@link BigDecimal} (filter → map → reduce). */
    public static BigDecimal totalAmount(List<Transaction> txns, TransactionType type) {
        return txns.stream()
                .filter(t -> t.type() == type)
                .map(t -> t.amount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Net movement = credits − debits, as a raw {@link BigDecimal}. */
    public static BigDecimal net(List<Transaction> txns) {
        return totalAmount(txns, TransactionType.CREDIT).subtract(totalAmount(txns, TransactionType.DEBIT));
    }

    /** How many of each type (groupingBy + counting). */
    public static Map<TransactionType, Long> countByType(List<Transaction> txns) {
        return txns.stream()
                .collect(Collectors.groupingBy(Transaction::type, Collectors.counting()));
    }

    /** The single largest transaction by amount, or empty if the list is empty (max → Optional). */
    public static Optional<Transaction> largest(List<Transaction> txns) {
        return txns.stream()
                .max(Comparator.comparing(t -> t.amount().amount()));
    }

    /** Transactions at or after {@code from}, newest first (filter + sorted). */
    public static List<Transaction> since(List<Transaction> txns, Instant from) {
        return txns.stream()
                .filter(t -> !t.timestamp().isBefore(from))
                .sorted(Comparator.comparing(Transaction::timestamp).reversed())
                .toList();
    }
}
