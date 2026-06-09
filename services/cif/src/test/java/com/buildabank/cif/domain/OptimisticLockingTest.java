// services/cif/src/test/java/com/buildabank/cif/domain/OptimisticLockingTest.java
package com.buildabank.cif.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.buildabank.cif.ContainersConfig;

/**
 * Proves {@code @Version} optimistic locking on a REAL Postgres: two "users" read the same row, the first
 * commits an update (version 0→1), and the second's stale update is REJECTED instead of silently lost.
 * Each step runs in its own transaction (via {@link TransactionTemplate}) to simulate concurrent users.
 */
@SpringBootTest
@Import(ContainersConfig.class)
class OptimisticLockingTest {

    @Autowired
    CustomerRepository repository;

    @Autowired
    PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    @BeforeEach
    void init() {
        tx = new TransactionTemplate(transactionManager);
    }

    @Test
    void concurrentUpdateIsRejected() {
        Long id = tx.execute(s -> repository.save(new Customer("CIF-V1", "Vera", "Version",
                "vera@bank.example", LocalDate.of(1990, 1, 1), KycStatus.PENDING, Instant.now())).getId());

        // Two users independently read the same row (each gets a detached copy at version 0).
        Customer userA = tx.execute(s -> repository.findById(id).orElseThrow());
        Customer userB = tx.execute(s -> repository.findById(id).orElseThrow());
        assertThat(userA.getVersion()).isZero();

        // User A updates and commits → version goes 0 → 1.
        tx.executeWithoutResult(s -> {
            userA.setKycStatus(KycStatus.VERIFIED);
            repository.save(userA);
        });

        // User B updates the now-stale copy (still version 0) → optimistic lock conflict, no lost update.
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            userB.setKycStatus(KycStatus.REJECTED);
            repository.save(userB);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // The winner's change stands.
        Customer current = tx.execute(s -> repository.findById(id).orElseThrow());
        assertThat(current.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(current.getVersion()).isEqualTo(1L);
    }
}
