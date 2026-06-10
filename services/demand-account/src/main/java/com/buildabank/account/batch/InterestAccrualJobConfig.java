// services/demand-account/src/main/java/com/buildabank/account/batch/InterestAccrualJobConfig.java
package com.buildabank.account.batch;

import java.util.Map;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import com.buildabank.account.domain.Account;
import com.buildabank.account.domain.AccountRepository;

/**
 * Step 24 · the EOD <strong>interest-accrual</strong> batch job — a classic chunk-oriented step
 * (read → process → write, committed in chunks). Spring Boot auto-configures the {@link JobRepository},
 * {@code JobLauncher}, and transaction manager (no {@code @EnableBatchProcessing} needed); we just declare the
 * {@link Job} and {@link Step}.
 *
 * <ul>
 *   <li><strong>Reader</strong> — pages over all accounts (oldest first) via the JPA repository.</li>
 *   <li><strong>Processor</strong> — computes the interest (or filters the account out).</li>
 *   <li><strong>Writer</strong> — credits the account + appends a ledger entry, per chunk.</li>
 *   <li><strong>Fault tolerance</strong> — {@code skip} a bad record ({@link InterestSkipException}) so the run
 *       continues; {@code retry} a transient optimistic-lock conflict (an EOD run can race live transfers).</li>
 * </ul>
 */
@Configuration
public class InterestAccrualJobConfig {

    static final String JOB_NAME = "interestAccrualJob";
    private static final int CHUNK = 10;

    @Bean
    Job interestAccrualJob(JobRepository jobRepository, Step interestAccrualStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(interestAccrualStep)
                .build();
    }

    @Bean
    Step interestAccrualStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                             RepositoryItemReader<Account> accountReader,
                             InterestProcessor processor, InterestWriter writer) {
        return new StepBuilder("interestAccrualStep", jobRepository)
                .<Account, InterestPosting>chunk(CHUNK, transactionManager)
                .reader(accountReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(InterestSkipException.class).skipLimit(100)               // one bad record ≠ failed night
                .retry(OptimisticLockingFailureException.class).retryLimit(3)    // ride a transient lock conflict
                .build();
    }

    @Bean
    RepositoryItemReader<Account> accountReader(AccountRepository accounts) {
        return new RepositoryItemReaderBuilder<Account>()
                .name("accountReader")
                .repository(accounts)
                .methodName("findAll")
                .sorts(Map.of("id", Sort.Direction.ASC))   // a deterministic, restartable read order
                .pageSize(CHUNK)
                .build();
    }
}
