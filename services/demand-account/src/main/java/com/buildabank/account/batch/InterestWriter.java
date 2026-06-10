// services/demand-account/src/main/java/com/buildabank/account/batch/InterestWriter.java
package com.buildabank.account.batch;

import java.time.Instant;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.buildabank.account.domain.Account;
import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.EntryDirection;
import com.buildabank.account.domain.LedgerEntry;
import com.buildabank.account.domain.LedgerEntryRepository;

/**
 * Step 24 · the chunk <strong>writer</strong>: credit each account its interest and append a ledger entry,
 * within the chunk's transaction (Batch manages the per-chunk commit). Re-reads each account with a pessimistic
 * lock (Step 12) so a concurrent transfer during the EOD run can't lose the update.
 *
 * <p>Simplification: this posts interest income to the customer only; the bank-side contra-entry (interest
 * expense to a GL account) is out of scope for this batch-focused step — double-entry was Step 12.
 */
@Component
public class InterestWriter implements ItemWriter<InterestPosting> {

    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;

    public InterestWriter(AccountRepository accounts, LedgerEntryRepository ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    @Override
    public void write(Chunk<? extends InterestPosting> chunk) {
        Instant now = Instant.now();
        for (InterestPosting posting : chunk) {
            Account account = accounts.findByAccountNumberForUpdate(posting.accountNumber())
                    .orElseThrow(() -> new IllegalStateException("account vanished: " + posting.accountNumber()));
            account.credit(posting.interest());
            ledger.save(new LedgerEntry(account.getId(), posting.transactionId(),
                    EntryDirection.CREDIT, posting.interest(), "interest accrual", now));
        }
    }
}
