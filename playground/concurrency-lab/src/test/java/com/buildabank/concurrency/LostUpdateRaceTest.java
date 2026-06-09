// playground/concurrency-lab/src/test/java/com/buildabank/concurrency/LostUpdateRaceTest.java
package com.buildabank.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CyclicBarrier;

import org.junit.jupiter.api.Test;

/**
 * Proves the <strong>lost-update race</strong> the hard way, then proves the three fixes.
 *
 * <p>The headline test forces the race <em>deterministically</em> with a {@link CyclicBarrier}: two threads
 * both read the balance (0), wait until both have read, then both write back (1) — so one of two +1 deposits
 * is silently lost, <em>every single run</em>. No flakiness, no "run it 1000 times and hope." The contention
 * tests then show {@link UnsafeBalance} can lose deposits at scale while {@code synchronized}, {@link
 * AtomicBalance}, and {@link LongAdderBalance} are exactly correct.
 */
class LostUpdateRaceTest {

    private static final int THREADS = 8;
    private static final int DEPOSITS_PER_THREAD = 100_000;
    private static final long EXPECTED = (long) THREADS * DEPOSITS_PER_THREAD;

    @Test
    void lostUpdate_isDeterministic_whenReadAndWriteAreNotAtomic() throws Exception {
        long[] balance = {0};                       // shared mutable state (array so it's effectively final)
        CyclicBarrier bothHaveRead = new CyclicBarrier(2);

        Runnable depositOne = () -> {
            long seen = balance[0];                 // 1) both threads read the SAME old value (0)
            awaitQuietly(bothHaveRead);             // 2) wait until BOTH have read before either writes
            balance[0] = seen + 1;                  // 3) both write back 1 → one deposit is lost
        };

        Thread a = new Thread(depositOne, "depositor-A");
        Thread b = new Thread(depositOne, "depositor-B");
        a.start();
        b.start();
        a.join();
        b.join();

        System.out.println("[race] two +1 deposits, interleaved read-modify-write → balance = " + balance[0]);
        assertThat(balance[0])
                .as("two deposits of 1, but the read-modify-write interleaved → exactly one was lost")
                .isEqualTo(1L);                     // NOT 2 — a deterministic lost update
    }

    @Test
    void unsafeBalance_canLoseDepositsUnderContention() throws Exception {
        long result = hammer(new UnsafeBalance());
        System.out.println("[race] UnsafeBalance under " + THREADS + "×" + DEPOSITS_PER_THREAD
                + ": expected=" + EXPECTED + " actual=" + result + "  (lost " + (EXPECTED - result) + ")");
        // Deterministic, never-flaky assertion: an unsynchronized counter can only LOSE updates, never invent
        // them — so it is always <= EXPECTED. (Run it and watch `actual` fall short of `expected`.)
        assertThat(result).isLessThanOrEqualTo(EXPECTED);
    }

    @Test
    void synchronizedBalance_isExact() throws Exception {
        assertThat(hammer(new SynchronizedBalance())).isEqualTo(EXPECTED);
    }

    @Test
    void atomicBalance_isExact() throws Exception {
        assertThat(hammer(new AtomicBalance())).isEqualTo(EXPECTED);
    }

    @Test
    void longAdderBalance_isExact() throws Exception {
        assertThat(hammer(new LongAdderBalance())).isEqualTo(EXPECTED);
    }

    /** Run THREADS threads, each depositing 1 a fixed number of times; join (which publishes the result). */
    private static long hammer(Balance balance) throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < DEPOSITS_PER_THREAD; j++) {
                    balance.deposit(1);
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();   // join establishes happens-before, so get() below sees all writes
        }
        return balance.get();
    }

    private static void awaitQuietly(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
