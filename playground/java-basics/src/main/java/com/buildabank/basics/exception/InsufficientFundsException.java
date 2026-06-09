// playground/java-basics/src/main/java/com/buildabank/basics/exception/InsufficientFundsException.java
package com.buildabank.basics.exception;

import com.buildabank.basics.money.Money;

/**
 * Thrown when a debit would overdraw an account beyond its limit.
 *
 * <p>Extends {@link RuntimeException} (unchecked): a programming/flow condition callers may choose to handle,
 * not something we force every caller to declare. We will map this to a clean HTTP 4xx via
 * {@code @ControllerAdvice} + {@code ProblemDetail} in Step 13.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Money requested, Money available) {
        super("Insufficient funds: requested %s but only %s available".formatted(requested, available));
    }
}
