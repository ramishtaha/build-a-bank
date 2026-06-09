// playground/spring-lab/src/main/java/com/buildabank/springlab/LabRunner.java
package com.buildabank.springlab;

import java.math.BigDecimal;
import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.buildabank.springlab.audit.AuditEntry;
import com.buildabank.springlab.interest.InterestService;

/**
 * A {@link CommandLineRunner} — Spring runs its {@code run} method once the context is fully started.
 * It prints the IoC concepts of this step so you can SEE them: which {@code RateProvider} got wired,
 * a {@code SpEL}-computed value, an interest calculation, and the singleton-vs-prototype scope difference.
 */
@Component
public class LabRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LabRunner.class);

    private final InterestService interest;
    private final ApplicationContext context;
    private final Clock clock;
    private final String bankName;
    private final double ratePercent;

    public LabRunner(InterestService interest,
                     ApplicationContext context,
                     Clock clock,
                     @Value("${bank.name}") String bankName,
                     // SpEL: resolve the placeholder, then do arithmetic — 0.0325 * 100 = 3.25
                     @Value("#{ ${bank.rates.fixed:0.0325} * 100 }") double ratePercent) {
        this.interest = interest;
        this.context = context;
        this.clock = clock;
        this.bankName = bankName;
        this.ratePercent = ratePercent;
    }

    @Override
    public void run(String... args) {
        log.info("================ Spring Lab :: {} ================", bankName);
        log.info("wired RateProvider     : {}", interest.rateSource());
        log.info("annual rate (via SpEL) : {}%", ratePercent);
        log.info("interest on 10000.00   : {}", interest.annualInterest(new BigDecimal("10000.00")));
        log.info("clock.instant() (UTC)  : {}", clock.instant());

        boolean singletonSame = context.getBean(InterestService.class) == context.getBean(InterestService.class);
        AuditEntry first = context.getBean(AuditEntry.class);
        AuditEntry second = context.getBean(AuditEntry.class);
        log.info("singleton same instance? {}", singletonSame);
        log.info("prototype instances     : #{} vs #{}  (same? {})",
                first.instanceId(), second.instanceId(), first == second);
        log.info("==================================================");
    }
}
