// services/demand-account/src/test/java/com/buildabank/account/service/ConcurrentTransferTest.java
package com.buildabank.account.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.buildabank.account.ContainersConfig;
import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.LedgerEntryRepository;

/**
 * 🎓 <strong>The Phase B Capstone.</strong> A concurrency stress test against the ledger that <em>fails
 * without locking and passes with it</em> — on a real Postgres.
 *
 * <ul>
 *   <li>{@link #withoutLocking_concurrentTransfersLoseAnUpdate()} forces two transfers to interleave with no
 *       lock and no version check: one update is silently lost and the materialized balance no longer
 *       matches the ledger.</li>
 *   <li>{@link #withPessimisticLock_concurrentTransfersAreCorrect()} runs 20 transfers concurrently through
 *       the real {@code SELECT ... FOR UPDATE} path: every transfer applies, money is conserved exactly, the
 *       books balance, and no account overdraws.</li>
 * </ul>
 */
@SpringBootTest
@Import(ContainersConfig.class)
class ConcurrentTransferTest {

    @Autowired
    TransferService transfers;

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerEntryRepository ledger;

    @BeforeEach
    void clean() {
        ledger.deleteAll();
        accounts.deleteAll();
    }

    /** ❌ No locking: two concurrent transfers of 100 both read 200, both write 100 → one transfer is LOST. */
    @Test
    void withoutLocking_concurrentTransfersLoseAnUpdate() throws Exception {
        transfers.openAccount("ACC-A", "USD", new BigDecimal("200.00"));
        transfers.openAccount("ACC-B", "USD", new BigDecimal("0.00"));

        CyclicBarrier bothHaveRead = new CyclicBarrier(2);   // force both to read BEFORE either writes
        Runnable afterRead = () -> {
            try {
                bothHaveRead.await();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<?> t1 = pool.submit(() ->
                    transfers.transferUnsafe("ACC-A", "ACC-B", new BigDecimal("100.00"), "race-1", afterRead));
            Future<?> t2 = pool.submit(() ->
                    transfers.transferUnsafe("ACC-A", "ACC-B", new BigDecimal("100.00"), "race-2", afterRead));
            t1.get();
            t2.get();
        }

        BigDecimal a = transfers.balanceOf("ACC-A");
        BigDecimal b = transfers.balanceOf("ACC-B");
        System.out.println("[capstone:no-lock] A=" + a + " B=" + b
                + "  (correct would be A=0, B=200 — but one transfer was lost)");

        // The lost update: only ONE transfer's effect survived, even though BOTH "succeeded".
        assertThat(a).isEqualByComparingTo("100.00");   // should be 0 if both applied
        assertThat(b).isEqualByComparingTo("100.00");   // should be 200 if both applied
        // Corruption made visible: the ledger recorded TWO credits to B (200 total) but B only holds 100.
        assertThat(ledger.count()).isEqualTo(4);        // 2 entries per "successful" transfer
        assertThat(transfers.ledgerNet()).isEqualByComparingTo("0");   // the ledger itself still balances
    }

    /** ✅ Pessimistic lock: 20 concurrent transfers all apply correctly — conserved, balanced, no overdraft. */
    @Test
    void withPessimisticLock_concurrentTransfersAreCorrect() throws Exception {
        int transferCount = 20;
        BigDecimal each = new BigDecimal("50.00");      // 20 × 50 = 1000 = the whole of A
        transfers.openAccount("ACC-A", "USD", new BigDecimal("1000.00"));
        transfers.openAccount("ACC-B", "USD", new BigDecimal("0.00"));

        AtomicInteger failures = new AtomicInteger();
        try (ExecutorService pool = Executors.newFixedThreadPool(transferCount)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < transferCount; i++) {
                futures.add(pool.submit(() -> {
                    try {
                        transfers.transfer("ACC-A", "ACC-B", each, "concurrent");
                    } catch (RuntimeException e) {
                        failures.incrementAndGet();
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        }

        BigDecimal a = transfers.balanceOf("ACC-A");
        BigDecimal b = transfers.balanceOf("ACC-B");
        System.out.println("[capstone:pessimistic] failures=" + failures.get() + " A=" + a + " B=" + b
                + " total=" + transfers.totalSystemBalance() + " ledgerNet=" + transfers.ledgerNet());

        assertThat(failures.get()).isZero();                              // every transfer succeeded
        assertThat(a).isEqualByComparingTo("0.00");                       // A fully drained
        assertThat(b).isEqualByComparingTo("1000.00");                    // B received it all
        assertThat(transfers.totalSystemBalance()).isEqualByComparingTo("1000.00");   // money conserved
        assertThat(transfers.ledgerNet()).isEqualByComparingTo("0");      // books balance
        assertThat(ledger.count()).isEqualTo(transferCount * 2L);         // 2 entries per transfer
    }
}
