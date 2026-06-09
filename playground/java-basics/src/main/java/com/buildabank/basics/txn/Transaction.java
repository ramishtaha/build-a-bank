// playground/java-basics/src/main/java/com/buildabank/basics/txn/Transaction.java
package com.buildabank.basics.txn;

import java.time.Instant;
import java.util.Objects;

import com.buildabank.basics.money.Money;

/**
 * A single ledger movement. Time is an {@link Instant} (an instant on the UTC timeline) — the bank's
 * standing rule: store time in UTC, never a zone-ambiguous {@code LocalDateTime}.
 */
public record Transaction(String id, Money amount, TransactionType type, Instant timestamp) {

    public Transaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    public boolean isCredit() {
        return type == TransactionType.CREDIT;
    }
}
