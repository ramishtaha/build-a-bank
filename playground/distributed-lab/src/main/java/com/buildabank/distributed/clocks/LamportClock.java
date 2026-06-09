// playground/distributed-lab/src/main/java/com/buildabank/distributed/clocks/LamportClock.java
package com.buildabank.distributed.clocks;

/**
 * A <strong>Lamport logical clock</strong> (Leslie Lamport, 1978) — a single monotonically increasing
 * counter per process that orders events <em>causally</em> without any synchronized wall clock.
 *
 * <p>The two rules:
 * <ol>
 *   <li><strong>Local event</strong> (incl. sending a message): increment the counter ({@link #tick()}).</li>
 *   <li><strong>Receive a message</strong> stamped {@code t}: set the counter to
 *       {@code max(local, t) + 1} ({@link #onReceive(long)}).</li>
 * </ol>
 *
 * <p><strong>Clock condition:</strong> if event {@code a} <em>happens-before</em> {@code b} (written
 * {@code a → b}) then {@code L(a) < L(b)}. The converse does <em>not</em> hold: {@code L(a) < L(b)} does
 * NOT imply {@code a → b} — two unrelated (concurrent) events can still get ordered numbers. Detecting
 * concurrency needs a {@link VectorClock}; Lamport only gives a consistent <em>total-ish</em> order.
 *
 * <p>Not thread-safe by design — one clock belongs to one logical process (single-threaded actor). The bank
 * uses this idea wherever it needs a causal order without trusting machine clocks (event ordering, Step 20).
 */
public final class LamportClock {

    private final String process;
    private long time;

    public LamportClock(String process) {
        this.process = process;
    }

    /** A local event (or a send). Increments and returns the new timestamp. */
    public long tick() {
        return ++time;
    }

    /**
     * Receiving a message stamped {@code receivedTimestamp}. Advances the clock past both our own time and
     * the sender's, then ticks for the receive event itself — guaranteeing the receive is ordered after the
     * send.
     */
    public long onReceive(long receivedTimestamp) {
        time = Math.max(time, receivedTimestamp) + 1;
        return time;
    }

    public long time() {
        return time;
    }

    public String process() {
        return process;
    }
}
