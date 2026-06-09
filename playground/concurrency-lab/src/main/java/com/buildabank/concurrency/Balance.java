// playground/concurrency-lab/src/main/java/com/buildabank/concurrency/Balance.java
package com.buildabank.concurrency;

/**
 * A shared, mutable account balance hammered by many threads at once — the simplest possible model of the
 * bank's central thread-safety problem. Four implementations show the spectrum from broken to correct:
 * {@link UnsafeBalance}, {@link SynchronizedBalance}, {@link AtomicBalance}, {@link LongAdderBalance}.
 *
 * <p>{@code deposit} is a read-modify-write ("read the balance, add, write it back") — the classic
 * operation that loses updates when two threads interleave without coordination.
 */
public interface Balance {

    /** Add {@code amount} minor units to the balance. */
    void deposit(long amount);

    /** The current balance in minor units. */
    long get();
}
