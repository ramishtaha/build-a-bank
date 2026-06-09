// services/demand-account/src/test/java/com/buildabank/account/outbox/OutboxWriteTest.java
package com.buildabank.account.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.buildabank.account.ContainersConfig;
import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.InsufficientFundsException;
import com.buildabank.account.domain.LedgerEntryRepository;
import com.buildabank.account.event.TransferEventListener;
import com.buildabank.account.service.TransferService;

/**
 * Step 20 · proves the <strong>Outbox</strong> is written atomically with the transfer, and that the
 * {@code @TransactionalEventListener(AFTER_COMMIT)} fires only on commit. Real Postgres (Testcontainers); no
 * Kafka needed here — this is purely about the in-transaction guarantees. The test methods are deliberately
 * NOT {@code @Transactional}, so each transfer commits its own transaction (otherwise AFTER_COMMIT wouldn't fire).
 */
@SpringBootTest
@Import(ContainersConfig.class)
class OutboxWriteTest {

    @Autowired
    TransferService transfers;

    @Autowired
    OutboxEventRepository outbox;

    @Autowired
    TransferEventListener listener;

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerEntryRepository ledger;

    @BeforeEach
    void clean() {
        outbox.deleteAll();
        ledger.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void committedTransferWritesExactlyOneOutboxRow_andFiresAfterCommitListener() {
        transfers.openAccount("ACC-A", "USD", new BigDecimal("100.00"));
        transfers.openAccount("ACC-B", "USD", BigDecimal.ZERO);
        int committedBefore = listener.committedCount();

        UUID txId = transfers.transfer("ACC-A", "ACC-B", new BigDecimal("30.00"), "rent");

        // The outbox row committed in the SAME transaction as the ledger change.
        assertThat(outbox.count()).isEqualTo(1);
        OutboxEvent row = outbox.findAll().getFirst();
        assertThat(row.getType()).isEqualTo("transfer.completed");
        assertThat(row.isPublished()).isFalse();                 // relay hasn't run yet
        assertThat(row.getPayload())
                .contains("ACC-A").contains("ACC-B").contains(txId.toString());

        // AFTER_COMMIT listener fired exactly once — only because the transfer committed.
        assertThat(listener.committedCount()).isEqualTo(committedBefore + 1);
    }

    @Test
    void rolledBackTransferWritesNoOutboxRow_andDoesNotFireAfterCommit() {
        transfers.openAccount("ACC-A", "USD", new BigDecimal("10.00"));
        transfers.openAccount("ACC-B", "USD", BigDecimal.ZERO);
        int committedBefore = listener.committedCount();

        // Overdraft → InsufficientFundsException → the whole transaction rolls back.
        assertThatThrownBy(() -> transfers.transfer("ACC-A", "ACC-B", new BigDecimal("999.00"), "overdraft"))
                .isInstanceOf(InsufficientFundsException.class);

        // Atomicity: no money moved ⇒ NO outbox event (the dual-write can't leave a dangling "intent").
        assertThat(outbox.count()).isZero();
        // AFTER_COMMIT did NOT fire — we never react to a transfer that didn't happen.
        assertThat(listener.committedCount()).isEqualTo(committedBefore);
    }
}
