// services/demand-account/src/main/java/com/buildabank/account/domain/LedgerEntryRepository.java
package com.buildabank.account.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByAccountIdOrderByCreatedAtAsc(Long accountId);

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    /**
     * Net of all ledger entries (credits minus debits) across the whole book. Double-entry guarantees this
     * is always <strong>zero</strong> — every debit has an equal credit. A non-zero result means the books
     * don't balance (a bug we assert can never happen).
     */
    @Query("""
            select coalesce(sum(case when e.direction = com.buildabank.account.domain.EntryDirection.CREDIT
                                     then e.amount else e.amount * -1 end), 0)
            from LedgerEntry e""")
    BigDecimal netOfAllEntries();
}
