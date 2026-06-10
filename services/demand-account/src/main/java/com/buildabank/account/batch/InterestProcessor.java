// services/demand-account/src/main/java/com/buildabank/account/batch/InterestProcessor.java
package com.buildabank.account.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.buildabank.account.domain.Account;

/**
 * Step 24 · the chunk <strong>processor</strong>: turn an {@link Account} into the interest to post, or
 * <strong>filter</strong> it out (return {@code null} → not written). Accounts with a non-positive balance, or
 * whose interest rounds to zero, are filtered. A "SKIP" sentinel account throws {@link InterestSkipException}
 * to demonstrate fault-tolerant <strong>skipping</strong> — one bad record doesn't abort the EOD run.
 */
@Component
public class InterestProcessor implements ItemProcessor<Account, InterestPosting> {

    private final BigDecimal dailyRate;

    public InterestProcessor(@Value("${bank.interest.daily-rate:0.0001}") BigDecimal dailyRate) {
        this.dailyRate = dailyRate;   // 0.0001 = 0.01% per day
    }

    @Override
    public InterestPosting process(Account account) {
        if (account.getAccountNumber().contains("SKIP")) {
            throw new InterestSkipException("interest skipped for " + account.getAccountNumber());
        }
        if (account.getBalance().signum() <= 0) {
            return null;   // no interest on a zero/negative balance → filtered
        }
        BigDecimal interest = account.getBalance().multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);
        if (interest.signum() <= 0) {
            return null;   // rounds to nothing → filtered
        }
        return new InterestPosting(account.getAccountNumber(), interest, UUID.randomUUID());
    }
}
