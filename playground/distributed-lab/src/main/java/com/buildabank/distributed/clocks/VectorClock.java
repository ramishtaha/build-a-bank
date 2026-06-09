// playground/distributed-lab/src/main/java/com/buildabank/distributed/clocks/VectorClock.java
package com.buildabank.distributed.clocks;

import java.util.HashMap;
import java.util.Map;

/**
 * A <strong>vector clock</strong> — one counter <em>per process</em>, carried as a vector. Unlike a
 * {@link LamportClock}, a vector clock can tell whether two events are causally ordered OR
 * <strong>concurrent</strong> (causally independent) — the thing Lamport clocks cannot do.
 *
 * <p>Rules for a process {@code p}:
 * <ol>
 *   <li><strong>Local event:</strong> {@code V[p] += 1} ({@link #tick()}).</li>
 *   <li><strong>Receive</strong> a message carrying vector {@code W}: {@code V[i] = max(V[i], W[i])} for all
 *       {@code i}, then {@code V[p] += 1} ({@link #onReceive(VectorClock)}).</li>
 * </ol>
 *
 * <p><strong>Comparison</strong> of two vectors {@code a}, {@code b}:
 * <ul>
 *   <li>{@code a → b} (a happens-before b) iff {@code a[i] <= b[i]} for all {@code i} AND {@code a != b};</li>
 *   <li>{@code a || b} (concurrent) iff neither {@code a → b} nor {@code b → a}.</li>
 * </ul>
 *
 * <p>Instances are immutable snapshots: {@link #tick()} / {@link #onReceive} return a NEW clock, so a message
 * can carry the exact vector it was sent with. Missing entries are treated as 0.
 */
public final class VectorClock {

    private final String process;
    private final Map<String, Long> vector;

    public VectorClock(String process) {
        this(process, new HashMap<>());
    }

    private VectorClock(String process, Map<String, Long> vector) {
        this.process = process;
        this.vector = vector;
    }

    private VectorClock copyVector() {
        return new VectorClock(process, new HashMap<>(vector));
    }

    public long get(String node) {
        return vector.getOrDefault(node, 0L);
    }

    /** A local event: bump this process's own component. Returns a new snapshot. */
    public VectorClock tick() {
        VectorClock next = copyVector();
        next.vector.merge(process, 1L, Long::sum);
        return next;
    }

    /** Receiving {@code message}'s vector: take the componentwise max, then bump our own component. */
    public VectorClock onReceive(VectorClock message) {
        VectorClock next = copyVector();
        for (Map.Entry<String, Long> e : message.vector.entrySet()) {
            next.vector.merge(e.getKey(), e.getValue(), Math::max);
        }
        next.vector.merge(process, 1L, Long::sum);
        return next;
    }

    /** {@code this → other}: this causally precedes other (≤ in every component, and not equal). */
    public boolean happensBefore(VectorClock other) {
        boolean strictlyLess = false;
        for (String node : keys(other)) {
            long a = this.get(node);
            long b = other.get(node);
            if (a > b) {
                return false;           // some component exceeds → not ≤ everywhere
            }
            if (a < b) {
                strictlyLess = true;
            }
        }
        return strictlyLess;            // ≤ everywhere AND < somewhere ⇒ strictly precedes
    }

    /** Concurrent: causally independent — neither happens-before the other. */
    public boolean isConcurrentWith(VectorClock other) {
        return !this.happensBefore(other) && !other.happensBefore(this) && !this.equalsVector(other);
    }

    private boolean equalsVector(VectorClock other) {
        for (String node : keys(other)) {
            if (this.get(node) != other.get(node)) {
                return false;
            }
        }
        return true;
    }

    private java.util.Set<String> keys(VectorClock other) {
        java.util.Set<String> all = new java.util.HashSet<>(this.vector.keySet());
        all.addAll(other.vector.keySet());
        return all;
    }

    @Override
    public String toString() {
        return process + vector;
    }
}
