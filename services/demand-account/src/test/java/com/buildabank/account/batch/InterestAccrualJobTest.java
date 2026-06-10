// services/demand-account/src/test/java/com/buildabank/account/batch/InterestAccrualJobTest.java
package com.buildabank.account.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.step.StepExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.buildabank.account.ContainersConfig;
import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.LedgerEntryRepository;
import com.buildabank.account.service.TransferService;

/**
 * Step 24 · runs the EOD interest-accrual <strong>batch job</strong> on a REAL Postgres (Testcontainers) and
 * asserts the chunk processing: eligible accounts are credited (+ ledger entry), a zero-balance account is
 * <strong>filtered</strong>, a sentinel account is <strong>skipped</strong> (fault tolerance), and the step's
 * read/write/skip/filter counts match. Jobs don't auto-run at startup ({@code spring.batch.job.enabled=false});
 * this test launches the job explicitly with a unique parameter (a fresh JobInstance).
 */
@SpringBootTest
@Import(ContainersConfig.class)
class InterestAccrualJobTest {

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    Job interestAccrualJob;

    @Autowired
    TransferService transfers;

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerEntryRepository ledger;

    @BeforeEach
    void clean() {
        ledger.deleteAll();
        accounts.deleteAll();
    }

    private BigDecimal balanceOf(String accountNumber) {
        return accounts.findByAccountNumber(accountNumber).orElseThrow().getBalance();
    }

    @Test
    void accrualCreditsInterest_filtersZeroBalance_skipsSentinel_andRecordsCounts() throws Exception {
        // dailyRate = 0.0001 (0.01%/day): 1000 → 0.10, 500 → 0.05.
        transfers.openAccount("ACC-1", "USD", new BigDecimal("1000.00"));
        transfers.openAccount("ACC-2", "USD", new BigDecimal("500.00"));
        transfers.openAccount("ACC-3", "USD", new BigDecimal("0.00"));      // filtered — no interest
        transfers.openAccount("ACC-SKIP", "USD", new BigDecimal("999.00")); // skipped by the processor

        JobExecution execution = jobLauncher.run(interestAccrualJob, new JobParametersBuilder()
                .addLong("runId", System.currentTimeMillis())   // unique → a fresh JobInstance
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getReadCount()).isEqualTo(4);            // all four accounts read
        assertThat(step.getWriteCount()).isEqualTo(2);           // ACC-1 + ACC-2 credited
        assertThat(step.getFilterCount()).isEqualTo(1);          // ACC-3 filtered (zero balance)
        assertThat(step.getProcessSkipCount()).isEqualTo(1);     // ACC-SKIP skipped (fault-tolerant)

        // The real effects on the ledger:
        assertThat(balanceOf("ACC-1")).isEqualByComparingTo("1000.10");
        assertThat(balanceOf("ACC-2")).isEqualByComparingTo("500.05");
        assertThat(balanceOf("ACC-3")).isEqualByComparingTo("0.00");      // untouched
        assertThat(balanceOf("ACC-SKIP")).isEqualByComparingTo("999.00"); // untouched (skipped)
        assertThat(ledger.count()).isEqualTo(2);                          // one interest entry per credited account
    }
}
