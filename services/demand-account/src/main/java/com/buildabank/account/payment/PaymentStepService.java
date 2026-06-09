// services/demand-account/src/main/java/com/buildabank/account/payment/PaymentStepService.java
package com.buildabank.account.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.buildabank.account.domain.Account;
import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.EntryDirection;
import com.buildabank.account.domain.LedgerEntry;
import com.buildabank.account.domain.LedgerEntryRepository;

/**
 * Step 21 · the individual <strong>Saga steps</strong>, each in its OWN transaction
 * ({@code Propagation.REQUIRES_NEW}) so it commits independently. That independence is the whole point of a
 * Saga: once {@link #debit} commits you can't just roll it back if a later step fails — you must run a
 * <strong>compensating</strong> step ({@link #refund}). In a real distributed system these legs would live in
 * different services and travel as events; here they're separate local transactions, which models the same
 * "no shared transaction across steps" reality (and reuses the pessimistic row lock from Step 12).
 *
 * <p>Lives in its OWN bean because {@code REQUIRES_NEW} only takes effect through the Spring proxy — a
 * {@code this.}-call inside the orchestrator would bypass it (the self-invocation pitfall, Step 7 / AuditService).
 */
@Service
public class PaymentStepService {

    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;

    public PaymentStepService(AccountRepository accounts, LedgerEntryRepository ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    /** Step 1 — take money from the source (commits independently). Throws if it would overdraw. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void debit(String accountNumber, BigDecimal amount, UUID paymentId) {
        Account account = lock(accountNumber);
        account.debit(amount);   // throws InsufficientFundsException → step fails before anything else commits
        ledger.save(new LedgerEntry(account.getId(), paymentId, EntryDirection.DEBIT, amount,
                "payment debit", Instant.now()));
    }

    /** Step 2 — give money to the destination (commits independently). Throws if the account doesn't exist. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void credit(String accountNumber, BigDecimal amount, UUID paymentId) {
        Account account = lock(accountNumber);
        account.credit(amount);
        ledger.save(new LedgerEntry(account.getId(), paymentId, EntryDirection.CREDIT, amount,
                "payment credit", Instant.now()));
    }

    /** COMPENSATION for {@link #debit} — give the money back to the source (commits independently). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refund(String accountNumber, BigDecimal amount, UUID paymentId) {
        Account account = lock(accountNumber);
        account.credit(amount);
        ledger.save(new LedgerEntry(account.getId(), paymentId, EntryDirection.CREDIT, amount,
                "payment refund (compensation)", Instant.now()));
    }

    private Account lock(String accountNumber) {
        return accounts.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("no such account: " + accountNumber));
    }
}
