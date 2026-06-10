// services/demand-account/src/main/java/com/buildabank/account/batch/InterestSkipException.java
package com.buildabank.account.batch;

/**
 * Thrown by the interest processor for a record that must be <strong>skipped</strong> rather than abort the
 * whole EOD run (Step 24). The step is configured fault-tolerant for this exception, so one bad account
 * doesn't fail the night's job — Spring Batch records the skip and moves on.
 */
public class InterestSkipException extends RuntimeException {

    public InterestSkipException(String message) {
        super(message);
    }
}
