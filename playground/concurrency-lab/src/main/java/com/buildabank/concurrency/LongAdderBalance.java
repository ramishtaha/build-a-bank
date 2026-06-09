// playground/concurrency-lab/src/main/java/com/buildabank/concurrency/LongAdderBalance.java
package com.buildabank.concurrency;

import java.util.concurrent.atomic.LongAdder;

/**
 * Correct and <strong>contention-friendly</strong>. {@link LongAdder} spreads the count across multiple
 * internal cells, so concurrent threads usually update <em>different</em> cells (no CAS contention, no
 * false sharing of one hot field); {@link LongAdder#sum} adds the cells when you read. The trade-off vs
 * {@link AtomicLong}: faster writes under heavy contention, but {@code sum()} is a (still cheap) aggregate
 * rather than a single read — ideal for high-throughput counters/metrics where reads are rare.
 */
public class LongAdderBalance implements Balance {

    private final LongAdder balance = new LongAdder();

    @Override
    public void deposit(long amount) {
        balance.add(amount);
    }

    @Override
    public long get() {
        return balance.sum();
    }
}
