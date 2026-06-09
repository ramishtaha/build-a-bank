// playground/concurrency-lab/src/main/java/com/buildabank/concurrency/SynchronizedBalance.java
package com.buildabank.concurrency;

/**
 * Correct via <strong>mutual exclusion</strong>. {@code synchronized} makes the whole read-modify-write
 * happen under the object's intrinsic lock, so only one thread is inside {@code deposit} at a time
 * (atomicity) and the lock's release/acquire establishes a <strong>happens-before</strong> edge, so each
 * thread sees the previous one's write (visibility). Simple and correct; the cost is that threads serialize
 * on the lock under contention.
 */
public class SynchronizedBalance implements Balance {

    private long balance;

    @Override
    public synchronized void deposit(long amount) {
        balance += amount;
    }

    @Override
    public synchronized long get() {
        return balance;
    }
}
