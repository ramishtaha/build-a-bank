// playground/distributed-lab/src/main/java/com/buildabank/distributed/cap/ReplicatedRegister.java
package com.buildabank.distributed.cap;

import java.util.HashMap;
import java.util.Map;

/**
 * Step 19 · a two-replica register that makes <strong>CAP</strong> and <strong>PACELC</strong> concrete.
 *
 * <p><strong>CAP:</strong> when the network <em>partitions</em> (the replicas can't talk), a system can keep
 * either <strong>C</strong>onsistency or <strong>A</strong>vailability, not both:
 * <ul>
 *   <li>{@link Mode#CP}: refuse writes during a partition ({@link Unavailable}) so replicas never diverge —
 *       consistent but unavailable.</li>
 *   <li>{@link Mode#AP}: accept writes on each side during a partition — available but divergent, then
 *       reconciled on heal via <strong>last-write-wins</strong> (eventual consistency).</li>
 * </ul>
 *
 * <p><strong>PACELC</strong> adds: <em>else</em> (when there's no partition) you still trade
 * <strong>L</strong>atency vs <strong>C</strong>onsistency. We model that with {@code syncReplication}:
 * synchronous replication waits for the peer (consistent, slower); asynchronous acks locally first (fast, but
 * a peer read can be stale until {@link #sync()}).
 *
 * <p>Deterministic by design: callers supply the logical timestamp for each write, so last-write-wins
 * reconciliation is reproducible (ties broken by replica id).
 */
public final class ReplicatedRegister {

    public enum Mode { CP, AP }

    /** Thrown by a CP register that refuses to act during a partition (chooses Consistency over Availability). */
    public static final class Unavailable extends RuntimeException {
        public Unavailable(String message) {
            super(message);
        }
    }

    /** A value tagged with the logical time it was written and the replica that wrote it (for LWW). */
    public record Stamped(long timestamp, String writer, String value) {
        static final Stamped EMPTY = new Stamped(0, "", null);

        boolean newerThan(Stamped other) {
            if (this.timestamp != other.timestamp) {
                return this.timestamp > other.timestamp;
            }
            return this.writer.compareTo(other.writer) > 0;   // deterministic tie-break
        }
    }

    private final Mode mode;
    private final boolean syncReplication;
    private final Map<String, Stamped> replicas = new HashMap<>();
    private boolean partitioned;

    public ReplicatedRegister(Mode mode, boolean syncReplication, String... replicaIds) {
        this.mode = mode;
        this.syncReplication = syncReplication;
        for (String id : replicaIds) {
            replicas.put(id, Stamped.EMPTY);
        }
    }

    /** Convenience: an AP/CP register with synchronous replication. */
    public ReplicatedRegister(Mode mode, String... replicaIds) {
        this(mode, true, replicaIds);
    }

    public void partition() {
        this.partitioned = true;
    }

    /** Write {@code value} at {@code replica}, stamped with the caller's logical {@code timestamp}. */
    public void write(String replica, String value, long timestamp) {
        Stamped stamped = new Stamped(timestamp, replica, value);
        if (mode == Mode.CP) {
            if (partitioned) {
                throw new Unavailable("CP: refusing write during partition to preserve consistency");
            }
            replicas.replaceAll((id, old) -> stamped);   // synchronous replication to all (connected)
            return;
        }
        // AP: always accept locally (stay available)...
        replicas.put(replica, stamped);
        // ...and propagate immediately only if connected AND synchronous.
        if (!partitioned && syncReplication) {
            replicas.replaceAll((id, old) -> stamped.newerThan(old) ? stamped : old);
        }
    }

    /** Read the value currently held by {@code replica} (may be stale under AP partition / async replication). */
    public String read(String replica) {
        return replicas.get(replica).value();
    }

    /** Reconcile all replicas to the last-write-wins value. Used to flush async replication. */
    public void sync() {
        Stamped winner = Stamped.EMPTY;
        for (Stamped s : replicas.values()) {
            if (s.newerThan(winner)) {
                winner = s;
            }
        }
        final Stamped chosen = winner;
        replicas.replaceAll((id, old) -> chosen);
    }

    /** Heal the partition and reconcile (eventual consistency: replicas converge via LWW). */
    public void heal() {
        this.partitioned = false;
        sync();
    }
}
