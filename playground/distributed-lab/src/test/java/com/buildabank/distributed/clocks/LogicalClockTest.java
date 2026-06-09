// playground/distributed-lab/src/test/java/com/buildabank/distributed/clocks/LogicalClockTest.java
package com.buildabank.distributed.clocks;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Step 19 · causality. Proves the <strong>clock condition</strong> (causally-ordered events get increasing
 * timestamps), then shows the key difference: a {@link VectorClock} can tell <em>concurrent</em> events apart,
 * while a {@link LamportClock} cannot.
 */
class LogicalClockTest {

    @Test
    @DisplayName("Lamport: if a → b (a message links them), then L(a) < L(b)")
    void lamportRespectsCausality() {
        LamportClock alice = new LamportClock("alice");
        LamportClock bob = new LamportClock("bob");

        long aLocal = alice.tick();          // 1 — Alice does a local event
        long aSend = alice.tick();           // 2 — Alice sends a message (a local event too)
        long bLocal = bob.tick();            // 1 — Bob, independently, does a local event
        long bRecv = bob.onReceive(aSend);   // max(1,2)+1 = 3 — Bob receives Alice's message

        // The causal edges (send → receive, and everything before the send → the receive) are ordered:
        assertThat(aSend).isLessThan(bRecv);     // send(2) → recv(3)
        assertThat(aLocal).isLessThan(bRecv);    // aLocal(1) → ... → recv(3)
        assertThat(bLocal).isLessThan(bRecv);    // bob's own prior event precedes his receive
    }

    @Test
    @DisplayName("Lamport's limitation: L(a) < L(b) does NOT imply a → b (concurrency is invisible)")
    void lamportCannotDetectConcurrency() {
        LamportClock alice = new LamportClock("alice");
        LamportClock bob = new LamportClock("bob");

        long aLocal = alice.tick();   // 1 — never communicated to Bob
        bob.tick();                   // 1
        long bSecond = bob.tick();    // 2 — also never communicated to Alice

        // Numerically aLocal(1) < bSecond(2), which *looks* like aLocal happened first...
        assertThat(aLocal).isLessThan(bSecond);
        // ...but they are actually CONCURRENT (no message ever linked Alice and Bob). The Lamport numbers
        // give a false sense of ordering — which is exactly why we reach for vector clocks below.
    }

    @Test
    @DisplayName("Vector clock: detects happens-before for a message, and concurrency for independent events")
    void vectorClockDistinguishesCausalFromConcurrent() {
        VectorClock alice = new VectorClock("alice");
        VectorClock bob = new VectorClock("bob");

        VectorClock a1 = alice.tick();        // {alice:1}
        VectorClock b1 = bob.tick();          // {bob:1}  — independent of a1
        VectorClock b2 = b1.onReceive(a1);    // {alice:1, bob:2} — Bob receives Alice's a1

        // Causal edges are detected:
        assertThat(a1.happensBefore(b2)).isTrue();    // a1 → b2 (via the message)
        assertThat(b1.happensBefore(b2)).isTrue();    // b1 → b2 (Bob's own prior event)
        assertThat(b2.happensBefore(a1)).isFalse();   // not the other way round

        // Concurrency is detected — the thing Lamport could not do:
        assertThat(a1.isConcurrentWith(b1)).isTrue();
        assertThat(b1.isConcurrentWith(a1)).isTrue();
        assertThat(a1.isConcurrentWith(b2)).isFalse(); // a1 is causally before b2, not concurrent
    }
}
