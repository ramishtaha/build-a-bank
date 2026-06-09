// playground/distributed-lab/src/test/java/com/buildabank/distributed/cap/CapPacelcTest.java
package com.buildabank.distributed.cap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buildabank.distributed.cap.ReplicatedRegister.Mode;

/**
 * Step 19 · CAP & PACELC made concrete. CP sacrifices availability under partition to stay consistent; AP
 * stays available but diverges, then reconciles (eventual consistency). PACELC's "else" branch: even with no
 * partition, async replication trades latency for consistency.
 */
class CapPacelcTest {

    @Test
    @DisplayName("CP: under a partition the register refuses writes (unavailable) but stays consistent")
    void cpChoosesConsistencyOverAvailability() {
        ReplicatedRegister reg = new ReplicatedRegister(Mode.CP, "n1", "n2");
        reg.write("n1", "balance=100", 1);                 // connected: replicated to both
        assertThat(reg.read("n1")).isEqualTo("balance=100");
        assertThat(reg.read("n2")).isEqualTo("balance=100");

        reg.partition();
        // A write during the partition is rejected — we sacrifice Availability to avoid divergence.
        assertThatThrownBy(() -> reg.write("n1", "balance=200", 2))
                .isInstanceOf(ReplicatedRegister.Unavailable.class);

        // Both replicas still agree on the last consistent value:
        assertThat(reg.read("n1")).isEqualTo("balance=100");
        assertThat(reg.read("n2")).isEqualTo("balance=100");
    }

    @Test
    @DisplayName("AP: under a partition both sides accept writes (available) → divergence, then heal reconciles (LWW)")
    void apChoosesAvailabilityThenConvergesOnHeal() {
        ReplicatedRegister reg = new ReplicatedRegister(Mode.AP, "n1", "n2");
        reg.write("n1", "balance=100", 1);                 // connected: both see it
        assertThat(reg.read("n2")).isEqualTo("balance=100");

        reg.partition();
        reg.write("n1", "balance=200", 5);                 // accepted on n1 (available)
        reg.write("n2", "balance=300", 9);                 // accepted on n2 (available) — conflicting!

        // During the partition the replicas DIVERGE (this is the price of availability):
        assertThat(reg.read("n1")).isEqualTo("balance=200");
        assertThat(reg.read("n2")).isEqualTo("balance=300");

        reg.heal();   // partition repaired → reconcile by last-write-wins (timestamp 9 > 5)
        assertThat(reg.read("n1")).isEqualTo("balance=300");
        assertThat(reg.read("n2")).isEqualTo("balance=300");   // converged (eventual consistency)
    }

    @Test
    @DisplayName("PACELC 'else': with NO partition, async replication trades latency for a stale read until sync")
    void pacelcElseLatencyVersusConsistency() {
        // AP register, asynchronous replication, fully connected (no partition).
        ReplicatedRegister reg = new ReplicatedRegister(Mode.AP, false, "n1", "n2");

        reg.write("n1", "balance=100", 1);     // acked locally fast; NOT yet propagated (low latency)
        assertThat(reg.read("n1")).isEqualTo("balance=100");
        assertThat(reg.read("n2")).isNull();   // peer is stale — the latency/consistency trade-off (E-L)

        reg.sync();                            // pay the replication cost → consistency (E-C)
        assertThat(reg.read("n2")).isEqualTo("balance=100");
    }
}
