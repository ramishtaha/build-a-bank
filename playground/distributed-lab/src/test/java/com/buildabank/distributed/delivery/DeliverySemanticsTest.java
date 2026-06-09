// playground/distributed-lab/src/test/java/com/buildabank/distributed/delivery/DeliverySemanticsTest.java
package com.buildabank.distributed.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buildabank.distributed.delivery.DeliverySim.BalanceProjection;
import com.buildabank.distributed.delivery.DeliverySim.Transfer;
import com.buildabank.distributed.delivery.DeliverySim.UnreliableChannel;

/**
 * Step 19 · proves the delivery-semantics trade-offs: at-least-once duplicates corrupt a naive consumer;
 * an idempotent consumer turns the same duplicates into <strong>exactly-once effect</strong>; at-most-once
 * can silently lose the message.
 */
class DeliverySemanticsTest {

    private final UnreliableChannel channel = new UnreliableChannel();
    private static final Transfer DEPOSIT = new Transfer("txn-1", 100);

    @Test
    @DisplayName("At-least-once + NAIVE consumer: 3 deliveries overcount the balance (the bug)")
    void atLeastOnceWithoutDedupeOvercounts() {
        BalanceProjection naive = new BalanceProjection(false);

        channel.deliver(DEPOSIT, 3, naive::apply);   // retried/duplicated 3×

        assertThat(naive.balance()).isEqualTo(300);  // 3 × 100 — money invented out of duplicates 😱
    }

    @Test
    @DisplayName("At-least-once + IDEMPOTENT consumer: 3 deliveries → exactly-once effect")
    void atLeastOnceWithDedupeGivesExactlyOnceEffect() {
        BalanceProjection idempotent = new BalanceProjection(true);

        channel.deliver(DEPOSIT, 3, idempotent::apply);   // same 3 duplicates

        assertThat(idempotent.balance()).isEqualTo(100);      // applied once
        assertThat(idempotent.distinctApplied()).isEqualTo(1);
    }

    @Test
    @DisplayName("At-most-once: a lost message (0 deliveries) leaves the balance untouched")
    void atMostOnceCanLoseTheMessage() {
        BalanceProjection consumer = new BalanceProjection(true);

        channel.deliver(DEPOSIT, 0, consumer::apply);   // dropped

        assertThat(consumer.balance()).isZero();        // the other failure mode: loss, not duplication
    }

    @Test
    @DisplayName("Exactly-once effect holds under interleaved duplicates of multiple messages")
    void exactlyOnceEffectAcrossInterleavedDuplicates() {
        BalanceProjection idempotent = new BalanceProjection(true);
        Transfer t1 = new Transfer("txn-A", 100);
        Transfer t2 = new Transfer("txn-B", 25);

        channel.deliver(t1, 2, idempotent::apply);   // t1 ×2
        channel.deliver(t2, 1, idempotent::apply);   // t2 ×1
        channel.deliver(t1, 3, idempotent::apply);   // t1 again ×3 (late retries)

        assertThat(idempotent.balance()).isEqualTo(125);      // 100 + 25, each once
        assertThat(idempotent.distinctApplied()).isEqualTo(2);
    }
}
