// services/demand-account/src/main/java/com/buildabank/account/payment/PaymentFailedException.java
package com.buildabank.account.payment;

/**
 * Thrown when a payment Saga fails after a step had already committed, so a <strong>compensating</strong>
 * action was run to undo it (e.g. the credit leg failed, so the debited source was refunded). The system is
 * left consistent; the caller is told the payment did not complete.
 */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
