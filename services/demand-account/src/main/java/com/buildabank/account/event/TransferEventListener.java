// services/demand-account/src/main/java/com/buildabank/account/event/TransferEventListener.java
package com.buildabank.account.event;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Step 20 · demonstrates {@code @TransactionalEventListener}. Unlike a plain {@code @EventListener} (which
 * fires immediately, even if the surrounding transaction later rolls back), this fires only at a chosen
 * <strong>transaction phase</strong>. We use {@link TransactionPhase#AFTER_COMMIT}: the listener runs only
 * once the transfer has actually committed — so we never react to money that didn't move.
 *
 * <p><strong>Why this is NOT where we publish to Kafka:</strong> publishing to Kafka here would be a
 * <em>dual-write</em> — if the app crashes between commit and this listener running, the event is lost
 * forever (the DB committed but nothing was published). That gap is exactly what the {@code outbox} package
 * fixes by writing the event <em>inside</em> the transaction. So this listener does only safe, in-process work
 * (here: a metric/log); the durable hand-off to Kafka is the Outbox relay's job.
 */
@Component
public class TransferEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);
    private final AtomicInteger committedCount = new AtomicInteger();

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferCommitted(TransferCompletedEvent event) {
        committedCount.incrementAndGet();
        log.info("transfer committed: txn={} {}->{} amount={} (eventId={})",
                event.transactionId(), event.fromAccount(), event.toAccount(), event.amount(), event.eventId());
    }

    /** Test/observability hook: how many transfers have committed since startup. */
    public int committedCount() {
        return committedCount.get();
    }
}
