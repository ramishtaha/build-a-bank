// services/demand-account/src/main/java/com/buildabank/account/domain/LedgerEntry.java
package com.buildabank.account.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One leg of a money movement — the append-only heart of double-entry bookkeeping. A transfer writes two
 * entries that share a {@code transactionId}: a {@link EntryDirection#DEBIT} on the payer and a
 * {@link EntryDirection#CREDIT} on the payee, with equal {@code amount}. Entries are never updated or
 * deleted — the ledger is an immutable audit trail (we revisit immutability + event sourcing in Phase J).
 *
 * <p>We store {@code accountId} as a plain id (not a {@code @ManyToOne}) on purpose: the ledger is a
 * high-volume fact table, and a bare foreign key avoids accidental lazy-loading / N+1 (Step 9) when we
 * append to it.
 */
@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EntryDirection direction;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(updatable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(Long accountId, UUID transactionId, EntryDirection direction,
                       BigDecimal amount, String description, Instant createdAt) {
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.direction = direction;
        this.amount = amount;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public EntryDirection getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
