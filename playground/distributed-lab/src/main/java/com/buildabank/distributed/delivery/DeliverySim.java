// playground/distributed-lab/src/main/java/com/buildabank/distributed/delivery/DeliverySim.java
package com.buildabank.distributed.delivery;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Step 19 · <strong>delivery semantics</strong>. A network can guarantee at most one of these for free:
 * <ul>
 *   <li><strong>At-most-once</strong>: a message may be <em>lost</em> but never duplicated (fire-and-forget).</li>
 *   <li><strong>At-least-once</strong>: a message is never lost but may be <em>duplicated</em> (retries).</li>
 * </ul>
 * "Exactly-once <em>delivery</em>" is impossible in an asynchronous network with failures (FLP). What real
 * systems achieve instead is <strong>exactly-once <em>effect</em></strong>: tolerate at-least-once delivery
 * and make the consumer <strong>idempotent</strong> (dedupe by message id) so duplicates don't change state.
 * (This is the same idea as the Idempotency-Key in Step 14 / Step 21.)
 *
 * <p>This file gives a deterministic simulation: an {@link UnreliableChannel} that delivers a message a
 * chosen number of times, and a {@link BalanceProjection} that is either naive or deduplicating.
 */
public final class DeliverySim {

    private DeliverySim() {
    }

    /** A money event with a stable, unique id (the dedupe key). */
    public record Transfer(String id, long amount) {
    }

    /**
     * A running balance built from delivered {@link Transfer}s. If {@code deduplicate} is true it remembers
     * applied ids and ignores repeats — turning at-least-once delivery into exactly-once effect.
     */
    public static final class BalanceProjection {
        private final boolean deduplicate;
        private final Set<String> applied = new HashSet<>();
        private long balance;

        public BalanceProjection(boolean deduplicate) {
            this.deduplicate = deduplicate;
        }

        public void apply(Transfer t) {
            if (deduplicate && !applied.add(t.id())) {
                return;                 // seen this id before → no-op (idempotent)
            }
            balance += t.amount();
        }

        public long balance() {
            return balance;
        }

        /** How many distinct transfers actually took effect (only tracked when deduplicating). */
        public int distinctApplied() {
            return applied.size();
        }
    }

    /** Simulates a channel that delivers a message exactly {@code times} times (0 = lost, ≥2 = duplicated). */
    public static final class UnreliableChannel {
        public void deliver(Transfer message, int times, Consumer<Transfer> handler) {
            for (int i = 0; i < times; i++) {
                handler.accept(message);
            }
        }
    }
}
