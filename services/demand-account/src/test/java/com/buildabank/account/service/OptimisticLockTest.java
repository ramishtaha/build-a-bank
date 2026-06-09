// services/demand-account/src/test/java/com/buildabank/account/service/OptimisticLockTest.java
package com.buildabank.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.buildabank.account.ContainersConfig;
import com.buildabank.account.domain.Account;
import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.LedgerEntryRepository;

/**
 * The <strong>optimistic</strong> alternative to pessimistic locking (the third strategy from the lesson),
 * proven on the {@code Account} {@code @Version} column exactly as Step 9 did for the customer: two
 * "sessions" read the same account at version 0; the first credit commits (version → 1); the second, holding
 * a now-stale copy, is REJECTED instead of silently overwriting. No lock is held during the read — the
 * conflict is detected only at write time. (Contrast: {@code transfer} blocks at read time with FOR UPDATE.)
 */
@SpringBootTest
@Import(ContainersConfig.class)
class OptimisticLockTest {

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerEntryRepository ledger;

    @Autowired
    PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    @BeforeEach
    void init() {
        tx = new TransactionTemplate(transactionManager);
        // Delete ledger rows first — they have a FK to account (other test classes leave data; the DB is shared).
        tx.executeWithoutResult(s -> ledger.deleteAll());
        tx.executeWithoutResult(s -> accounts.deleteAll());
        tx.executeWithoutResult(s -> accounts.save(
                new Account("ACC-O", "USD", new BigDecimal("200.00"), java.time.Instant.now())));
    }

    @Test
    void concurrentUpdateIsRejected() {
        Account sessionA = tx.execute(s -> accounts.findByAccountNumber("ACC-O").orElseThrow());
        Account sessionB = tx.execute(s -> accounts.findByAccountNumber("ACC-O").orElseThrow());
        assertThat(sessionA.getVersion()).isZero();

        // Session A credits and commits → version 0 → 1.
        tx.executeWithoutResult(s -> {
            sessionA.credit(new BigDecimal("10.00"));
            accounts.save(sessionA);
        });

        // Session B updates its now-stale copy (still version 0) → optimistic-lock conflict, no lost update.
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            sessionB.credit(new BigDecimal("20.00"));
            accounts.save(sessionB);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // Only the winner's credit stands.
        Account current = tx.execute(s -> accounts.findByAccountNumber("ACC-O").orElseThrow());
        assertThat(current.getBalance()).isEqualByComparingTo("210.00");
        assertThat(current.getVersion()).isEqualTo(1L);
    }
}
