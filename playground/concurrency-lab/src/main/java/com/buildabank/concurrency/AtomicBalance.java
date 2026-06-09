// playground/concurrency-lab/src/main/java/com/buildabank/concurrency/AtomicBalance.java
package com.buildabank.concurrency;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Correct via <strong>lock-free</strong> atomics. {@link AtomicLong#addAndGet} is a single hardware
 * compare-and-swap (CAS) loop: read the value, compute the new one, and atomically swap it in only if it
 * hasn't changed — retrying if it has. No lock is held, so threads don't block each other; under very high
 * contention the CAS retries can spin (that's what {@link LongAdderBalance} improves on).
 */
public class AtomicBalance implements Balance {

    private final AtomicLong balance = new AtomicLong();

    @Override
    public void deposit(long amount) {
        balance.addAndGet(amount);   // atomic CAS — no lock
    }

    @Override
    public long get() {
        return balance.get();
    }
}
