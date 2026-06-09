// services/demand-account/src/main/java/com/buildabank/account/domain/InsufficientFundsException.java
package com.buildabank.account.domain;

/**
 * Thrown when a debit would overdraw an account. It's a {@link RuntimeException}, so Spring's
 * {@code @Transactional} rolls the transfer back by default (no half-completed money movement) — see the
 * Step-12 lesson on rollback rules.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
