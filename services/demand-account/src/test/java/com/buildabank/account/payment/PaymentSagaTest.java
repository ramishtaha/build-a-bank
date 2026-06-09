// services/demand-account/src/test/java/com/buildabank/account/payment/PaymentSagaTest.java
package com.buildabank.account.payment;

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
import com.buildabank.account.RedisContainers;
import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.LedgerEntryRepository;
import com.buildabank.account.service.TransferService;

/**
 * Step 21 · proves the payment <strong>Saga</strong> end-to-end on real Postgres + Redis (Testcontainers):
 * the happy path moves money; a failure after the debit committed triggers a <strong>compensating refund</strong>
 * (no money lost or created); and a Redis-backed <strong>Idempotency-Key</strong> makes a retry pay only once.
 */
@SpringBootTest
@Import({ContainersConfig.class, RedisContainers.class})
class PaymentSagaTest {

    @Autowired
    PaymentService payments;

    @Autowired
    TransferService transfers;   // reused only to open accounts

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerEntryRepository ledger;

    @BeforeEach
    void clean() {
        ledger.deleteAll();
        accounts.deleteAll();
    }

    private BigDecimal balanceOf(String accountNumber) {
        return accounts.findByAccountNumber(accountNumber).orElseThrow().getBalance();
    }

    @Test
    void happyPath_movesMoneyAndWritesBothLedgerLegs() {
        transfers.openAccount("ACC-A", "USD", new BigDecimal("100.00"));
        transfers.openAccount("ACC-B", "USD", new BigDecimal("0.00"));

        UUID paymentId = payments.pay("ACC-A", "ACC-B", new BigDecimal("40.00"), null);

        assertThat(paymentId).isNotNull();
        assertThat(balanceOf("ACC-A")).isEqualByComparingTo("60.00");
        assertThat(balanceOf("ACC-B")).isEqualByComparingTo("40.00");
        assertThat(ledger.count()).isEqualTo(2);          // a DEBIT leg + a CREDIT leg
    }

    @Test
    void creditStepFails_sagaCompensatesWithARefund_leavingBalancesConsistent() {
        transfers.openAccount("ACC-A", "USD", new BigDecimal("100.00"));
        // ACC-B is NEVER created → the credit step throws after the debit has already committed.

        assertThatThrownBy(() -> payments.pay("ACC-A", "ACC-MISSING", new BigDecimal("40.00"), null))
                .isInstanceOf(PaymentFailedException.class);

        // Compensation ran: A was debited 40 then refunded 40 → back to its starting balance. No money lost.
        assertThat(balanceOf("ACC-A")).isEqualByComparingTo("100.00");
        assertThat(ledger.count()).isEqualTo(2);          // the DEBIT + the compensating refund CREDIT
        assertThat(ledger.netOfAllEntries()).isEqualByComparingTo("0.00");   // debit and refund cancel out
    }

    @Test
    void redisIdempotencyKey_makesARetryPayOnlyOnce() {
        transfers.openAccount("ACC-A", "USD", new BigDecimal("100.00"));
        transfers.openAccount("ACC-B", "USD", new BigDecimal("0.00"));
        String key = "PAY-" + UUID.randomUUID();

        UUID first = payments.pay("ACC-A", "ACC-B", new BigDecimal("40.00"), key);
        UUID retry = payments.pay("ACC-A", "ACC-B", new BigDecimal("40.00"), key);   // same key — a retry

        assertThat(retry).isEqualTo(first);                       // original payment id returned, not a new one
        assertThat(balanceOf("ACC-A")).isEqualByComparingTo("60.00");   // money moved exactly ONCE
        assertThat(balanceOf("ACC-B")).isEqualByComparingTo("40.00");
    }
}
