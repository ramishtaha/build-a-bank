// services/demand-account/src/main/java/com/buildabank/account/domain/IdempotencyRecord.java
package com.buildabank.account.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Remembers that a given {@code Idempotency-Key} already produced a transfer, and which one. A retried
 * request with the same key returns the stored {@code transactionId} instead of moving money again. The key
 * is the natural {@code @Id} (a client-supplied string), and its PRIMARY KEY uniqueness is the concurrency
 * guard — two racing requests with the same key can't both insert, so only one transfer commits.
 */
@Entity
@Table(name = "idempotency_key")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", updatable = false)
    private String key;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String key, UUID transactionId, Instant createdAt) {
        this.key = key;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
    }

    public String getKey() {
        return key;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
