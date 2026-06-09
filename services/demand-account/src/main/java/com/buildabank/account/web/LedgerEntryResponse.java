// services/demand-account/src/main/java/com/buildabank/account/web/LedgerEntryResponse.java
package com.buildabank.account.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.buildabank.account.domain.EntryDirection;
import com.buildabank.account.domain.LedgerEntry;

/** API view of a ledger entry — a DTO, so we never serialize the JPA entity directly. */
public record LedgerEntryResponse(
        UUID transactionId, EntryDirection direction, BigDecimal amount, String description, Instant createdAt) {

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(entry.getTransactionId(), entry.getDirection(),
                entry.getAmount(), entry.getDescription(), entry.getCreatedAt());
    }
}
