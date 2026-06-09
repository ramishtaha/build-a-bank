// services/demand-account/src/main/java/com/buildabank/account/service/TransferService.java
package com.buildabank.account.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buildabank.account.domain.Account;
import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.EntryDirection;
import com.buildabank.account.domain.LedgerEntry;
import com.buildabank.account.domain.LedgerEntryRepository;

/**
 * Moves money between accounts and records it in the double-entry ledger. The interesting part is
 * <em>concurrency correctness</em>: two transfer strategies show the spectrum (plus optimistic locking,
 * which lives on the {@link Account} {@code @Version} column and is proven by {@code OptimisticLockTest}).
 *
 * <ul>
 *   <li>{@link #transfer} — <strong>pessimistic</strong> lock ({@code SELECT ... FOR UPDATE}); the safe,
 *       production path. Concurrent transfers on the same account serialize.</li>
 *   <li>{@link #transferUnsafe} — <strong>no guard at all</strong> (a bulk absolute write that bypasses both
 *       lock and version). Demonstration only: it loses updates under contention.</li>
 * </ul>
 *
 * Every transfer writes exactly two ledger entries (a DEBIT and a CREDIT sharing one {@code transactionId})
 * inside one transaction, so the books always balance and a failure rolls back <em>both</em> legs.
 */
@Service
public class TransferService {

    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;

    public TransferService(AccountRepository accounts, LedgerEntryRepository ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    @Transactional
    public Account openAccount(String accountNumber, String currency, BigDecimal openingBalance) {
        return accounts.save(new Account(accountNumber, currency, openingBalance, Instant.now()));
    }

    /**
     * SAFE transfer using pessimistic row locks. We lock the two accounts in a deterministic order (by
     * account number) so two transfers touching the same pair can never deadlock by grabbing them in the
     * opposite order (the lock-ordering rule from Step 11).
     */
    @Transactional
    public UUID transfer(String fromNumber, String toNumber, BigDecimal amount, String description) {
        if (fromNumber.equals(toNumber)) {
            throw new IllegalArgumentException("cannot transfer to the same account");
        }
        // Lock in a stable global order to avoid deadlock, then map back to from/to.
        boolean fromIsLower = fromNumber.compareTo(toNumber) < 0;
        String firstNumber = fromIsLower ? fromNumber : toNumber;
        String secondNumber = fromIsLower ? toNumber : fromNumber;
        Account firstLocked = lockOrThrow(firstNumber);
        Account secondLocked = lockOrThrow(secondNumber);
        Account from = fromIsLower ? firstLocked : secondLocked;
        Account to = fromIsLower ? secondLocked : firstLocked;
        return post(from, to, amount, description);
    }

    /**
     * DEMONSTRATION ONLY — no lock, no version. Reads the balances, runs {@code afterRead} (a test seam that
     * lets the Step-12 capstone force two transfers to interleave), then writes <em>absolute</em> balances
     * via a bulk update that bypasses {@code @Version}. Under contention this loses updates. Never use for
     * real money.
     */
    @Transactional
    public UUID transferUnsafe(String fromNumber, String toNumber, BigDecimal amount,
                               String description, Runnable afterRead) {
        Account from = accounts.findByAccountNumber(fromNumber).orElseThrow();
        Account to = accounts.findByAccountNumber(toNumber).orElseThrow();
        if (from.getBalance().compareTo(amount) < 0) {
            throw new com.buildabank.account.domain.InsufficientFundsException("insufficient funds");
        }
        afterRead.run();   // the race window: another transfer can read the same balances here
        accounts.applyBalanceUnsafe(from.getId(), from.getBalance().subtract(amount));
        accounts.applyBalanceUnsafe(to.getId(), to.getBalance().add(amount));
        UUID transactionId = UUID.randomUUID();
        Instant now = Instant.now();
        ledger.save(new LedgerEntry(from.getId(), transactionId, EntryDirection.DEBIT, amount, description, now));
        ledger.save(new LedgerEntry(to.getId(), transactionId, EntryDirection.CREDIT, amount, description, now));
        return transactionId;
    }

    /** A page of an account's ledger entries (Step 14 — pagination/sorting via the {@link Pageable}). */
    @Transactional(readOnly = true)
    public Page<LedgerEntry> entriesOf(String accountNumber, Pageable pageable) {
        Account account = accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("no such account: " + accountNumber));
        return ledger.findByAccountId(account.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal balanceOf(String accountNumber) {
        return accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("no such account: " + accountNumber))
                .getBalance();
    }

    @Transactional(readOnly = true)
    public BigDecimal totalSystemBalance() {
        return accounts.totalBalance();
    }

    /** The net of every ledger entry across the book — double-entry guarantees this is always zero. */
    @Transactional(readOnly = true)
    public BigDecimal ledgerNet() {
        return ledger.netOfAllEntries();
    }

    private Account lockOrThrow(String accountNumber) {
        return accounts.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("no such account: " + accountNumber));
    }

    /** Apply a debit + credit and record both ledger legs. Shared by the safe and optimistic paths. */
    private UUID post(Account from, Account to, BigDecimal amount, String description) {
        from.debit(amount);            // throws InsufficientFundsException → whole transfer rolls back
        to.credit(amount);             // dirty checking flushes both balance UPDATEs at commit
        UUID transactionId = UUID.randomUUID();
        Instant now = Instant.now();
        ledger.save(new LedgerEntry(from.getId(), transactionId, EntryDirection.DEBIT, amount, description, now));
        ledger.save(new LedgerEntry(to.getId(), transactionId, EntryDirection.CREDIT, amount, description, now));
        return transactionId;
    }
}
