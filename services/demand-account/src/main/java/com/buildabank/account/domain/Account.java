// services/demand-account/src/main/java/com/buildabank/account/domain/Account.java
package com.buildabank.account.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A demand (current) account with a materialized {@code balance}. Money is a {@link BigDecimal} — exact
 * decimal arithmetic, <strong>never</strong> {@code double}/{@code float} (which can't represent 0.10
 * exactly). The {@code @Version} column gives optimistic locking (Step 9); under heavy contention we instead
 * take a pessimistic row lock at read time (see {@code AccountRepository.findByAccountNumberForUpdate}).
 *
 * <p>The balance is kept correct by doing the read-check-write of {@link #debit}/{@link #credit} inside one
 * transaction while holding the row lock — the database analogue of Step 11's {@code synchronized}.
 */
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, updatable = false)
    private String accountNumber;

    @Column(nullable = false, updatable = false)
    private String currency;

    @Column(nullable = false)
    private BigDecimal balance;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor. */
    protected Account() {
    }

    public Account(String accountNumber, String currency, BigDecimal openingBalance, Instant createdAt) {
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.balance = openingBalance;
        this.createdAt = createdAt;
    }

    /** Take money out — refuses to overdraw (the invariant this service must never break). */
    public void debit(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "account " + accountNumber + " balance " + balance + " < debit " + amount);
        }
        balance = balance.subtract(amount);
    }

    /** Put money in. */
    public void credit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, was " + amount);
        }
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
