// playground/distributed-lab/src/test/java/com/buildabank/distributed/quorum/QuorumTest.java
package com.buildabank.distributed.quorum;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buildabank.distributed.quorum.QuorumSystem.Versioned;

/**
 * Step 19 · quorums & consistency. Proves the {@code W + R > N} intersection guarantee empirically (over all
 * quorum combinations) and shows the two consequences concretely: a strict quorum always reads the latest
 * write; a sloppy one can read stale.
 */
class QuorumTest {

    @Test
    @DisplayName("W + R > N ⇒ every read quorum intersects every write quorum (checked over ALL combinations)")
    void intersectionGuaranteeHoldsExactlyWhenWPlusRExceedsN() {
        int n = 5;
        // Brute-force every (W,R) pair and confirm the empirical result matches the W+R>N rule.
        for (int w = 1; w <= n; w++) {
            for (int r = 1; r <= n; r++) {
                boolean alwaysIntersect = QuorumSystem.everyWriteAndReadQuorumIntersect(n, w, r);
                assertThat(alwaysIntersect)
                        .as("N=%d W=%d R=%d → W+R>N is %b", n, w, r, (w + r > n))
                        .isEqualTo(w + r > n);
            }
        }
    }

    @Test
    @DisplayName("Strict quorum (N=3, W=2, R=2): a read ALWAYS sees the latest write, even a disjoint-looking one")
    void strictQuorumReadsLatestWrite() {
        QuorumSystem store = new QuorumSystem(3);     // replicas 0,1,2
        store.write(Set.of(0, 1), "v1");              // first write to {0,1}
        store.write(Set.of(1, 2), "v2");              // newer write to {1,2}

        // Any 2-of-3 read quorum (W+R=4>3) must overlap the latest write {1,2}:
        assertThat(store.read(Set.of(0, 1)).value()).isEqualTo("v2");   // overlaps at 1
        assertThat(store.read(Set.of(0, 2)).value()).isEqualTo("v2");   // overlaps at 2
        assertThat(store.read(Set.of(1, 2)).value()).isEqualTo("v2");   // overlaps at 1,2
    }

    @Test
    @DisplayName("Sloppy quorum (N=3, W=1, R=1): a read can be chosen disjoint from the write → STALE")
    void sloppyQuorumCanReadStale() {
        QuorumSystem store = new QuorumSystem(3);
        store.write(Set.of(0), "fresh");              // W=1, only replica 0 has it (W+R=2 ≤ 3)

        Versioned staleRead = store.read(Set.of(1));  // R=1, disjoint from the write
        assertThat(staleRead.value()).isNull();        // never saw "fresh" — eventual consistency window

        assertThat(store.read(Set.of(0)).value()).isEqualTo("fresh");   // a read that happens to hit 0 sees it
    }
}
