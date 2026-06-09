// services/demand-account/src/test/java/com/buildabank/account/service/TransferServiceTest.java
package com.buildabank.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.buildabank.account.ContainersConfig;
import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.EntryDirection;
import com.buildabank.account.domain.InsufficientFundsException;
import com.buildabank.account.domain.LedgerEntry;
import com.buildabank.account.domain.LedgerEntryRepository;

/**
 * The ledger's core behaviour on a real Postgres: a transfer atomically debits one account, credits the
 * other, and writes a balanced pair of ledger entries — and a failed transfer (overdraw) rolls back
 * <em>everything</em>, leaving no trace.
 */
@SpringBootTest
@Import(ContainersConfig.class)
class TransferServiceTest {

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

    @Test
    void transfer_movesMoney_andWritesABalancedLedgerPair() {
        var from = transfers.openAccount("ACC-A", "USD", new BigDecimal("200.00"));
        var to = transfers.openAccount("ACC-B", "USD", new BigDecimal("0.00"));

        UUID txId = transfers.transfer("ACC-A", "ACC-B", new BigDecimal("50.00"), "rent");

        // Balances moved exactly.
        assertThat(transfers.balanceOf("ACC-A")).isEqualByComparingTo("150.00");
        assertThat(transfers.balanceOf("ACC-B")).isEqualByComparingTo("50.00");

        // Money is conserved across the system, and the books balance (debits == credits).
        assertThat(transfers.totalSystemBalance()).isEqualByComparingTo("200.00");
        assertThat(transfers.ledgerNet()).isEqualByComparingTo("0");

        // Exactly two ledger entries, sharing the transaction id: a DEBIT on the payer, a CREDIT on the payee.
        List<LedgerEntry> entries = ledger.findByTransactionId(txId);
        assertThat(entries).hasSize(2);
        assertThat(entries).anySatisfy(e -> {
            assertThat(e.getAccountId()).isEqualTo(from.getId());
            assertThat(e.getDirection()).isEqualTo(EntryDirection.DEBIT);
            assertThat(e.getAmount()).isEqualByComparingTo("50.00");
        });
        assertThat(entries).anySatisfy(e -> {
            assertThat(e.getAccountId()).isEqualTo(to.getId());
            assertThat(e.getDirection()).isEqualTo(EntryDirection.CREDIT);
            assertThat(e.getAmount()).isEqualByComparingTo("50.00");
        });
    }

    @Test
    void overdraw_isRejected_andRollsBackEverything() {
        transfers.openAccount("ACC-A", "USD", new BigDecimal("10.00"));
        transfers.openAccount("ACC-B", "USD", new BigDecimal("0.00"));

        assertThatThrownBy(() -> transfers.transfer("ACC-A", "ACC-B", new BigDecimal("50.00"), "too much"))
                .isInstanceOf(InsufficientFundsException.class);

        // Nothing changed: the debit that ran before the exception was rolled back, and NO ledger rows exist.
        assertThat(transfers.balanceOf("ACC-A")).isEqualByComparingTo("10.00");
        assertThat(transfers.balanceOf("ACC-B")).isEqualByComparingTo("0.00");
        assertThat(ledger.count()).isZero();
    }
}
