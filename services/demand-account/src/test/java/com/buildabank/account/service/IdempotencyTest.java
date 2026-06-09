// services/demand-account/src/test/java/com/buildabank/account/service/IdempotencyTest.java
package com.buildabank.account.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.buildabank.account.ContainersConfig;
import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.IdempotencyRecordRepository;
import com.buildabank.account.domain.LedgerEntryRepository;

/**
 * Public-API idempotency: a retried transfer (same {@code Idempotency-Key}) returns the original result and
 * moves money <strong>once</strong> — the property that makes a money API safe to retry after a timeout.
 */
@SpringBootTest
@Import(ContainersConfig.class)
class IdempotencyTest {

    @Autowired
    IdempotentTransferService idempotentTransfers;

    @Autowired
    TransferService transfers;

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerEntryRepository ledger;

    @Autowired
    IdempotencyRecordRepository keys;

    @BeforeEach
    void clean() {
        keys.deleteAll();
        ledger.deleteAll();
        accounts.deleteAll();
        transfers.openAccount("ACC-A", "USD", new BigDecimal("200.00"));
        transfers.openAccount("ACC-B", "USD", new BigDecimal("0.00"));
    }

    @Test
    void sameKeyReturnsTheSameResult_andMovesMoneyOnce() {
        UUID first = idempotentTransfers.transfer("KEY-1", "ACC-A", "ACC-B", new BigDecimal("50.00"), "rent");
        UUID retry = idempotentTransfers.transfer("KEY-1", "ACC-A", "ACC-B", new BigDecimal("50.00"), "rent");

        assertThat(retry).isEqualTo(first);                              // same transaction id returned
        assertThat(transfers.balanceOf("ACC-A")).isEqualByComparingTo("150.00");   // moved ONCE, not twice
        assertThat(transfers.balanceOf("ACC-B")).isEqualByComparingTo("50.00");
    }

    @Test
    void aDifferentKeyMovesMoneyAgain() {
        idempotentTransfers.transfer("KEY-1", "ACC-A", "ACC-B", new BigDecimal("50.00"), "first");
        idempotentTransfers.transfer("KEY-2", "ACC-A", "ACC-B", new BigDecimal("50.00"), "second");

        assertThat(transfers.balanceOf("ACC-A")).isEqualByComparingTo("100.00");   // two distinct transfers
    }

    @Test
    void noKeyMeansNoDeduplication() {
        idempotentTransfers.transfer(null, "ACC-A", "ACC-B", new BigDecimal("10.00"), "a");
        idempotentTransfers.transfer(null, "ACC-A", "ACC-B", new BigDecimal("10.00"), "b");

        assertThat(transfers.balanceOf("ACC-A")).isEqualByComparingTo("180.00");   // both applied
    }
}
