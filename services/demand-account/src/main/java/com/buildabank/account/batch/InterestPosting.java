// services/demand-account/src/main/java/com/buildabank/account/batch/InterestPosting.java
package com.buildabank.account.batch;

import java.math.BigDecimal;
import java.util.UUID;

/** The interest to post to one account — the output of the processor, consumed by the writer. */
public record InterestPosting(String accountNumber, BigDecimal interest, UUID transactionId) {
}
