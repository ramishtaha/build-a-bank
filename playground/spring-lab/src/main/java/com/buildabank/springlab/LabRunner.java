// playground/spring-lab/src/main/java/com/buildabank/springlab/LabRunner.java
package com.buildabank.springlab;

import java.math.BigDecimal;
import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.buildabank.springlab.audit.AuditEntry;
import com.buildabank.springlab.autoconfig.GreetingService;
import com.buildabank.springlab.config.BankProperties;
import com.buildabank.springlab.interest.InterestService;

/**
 * A {@link CommandLineRunner} — Spring runs its {@code run} method once the context is fully started.
 * It prints the IoC + config concepts so you can SEE them: which {@code RateProvider} got wired, the
 * type-safe {@link BankProperties} values, the auto-configured {@link GreetingService}, an interest
 * calculation, and the singleton-vs-prototype scope difference.
 *
 * <p>Step 6 change: the bank name + rate now come from typed {@link BankProperties} (constructor binding),
 * not scattered {@code @Value}/SpEL strings; and {@code GreetingService} arrives via auto-configuration.
 */
@Component
public class LabRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LabRunner.class);

    private final InterestService interest;
    private final ApplicationContext context;
    private final Clock clock;
    private final BankProperties properties;
    private final GreetingService greeting;

    public LabRunner(InterestService interest,
                     ApplicationContext context,
                     Clock clock,
                     BankProperties properties,
                     GreetingService greeting) {
        this.interest = interest;
        this.context = context;
        this.clock = clock;
        this.properties = properties;
        this.greeting = greeting;
    }

    @Override
    public void run(String... args) {
        log.info("================ Spring Lab :: {} ================", properties.name());
        log.info("greeting (auto-config) : {}", greeting.greet("intern"));
        log.info("wired RateProvider     : {}", interest.rateSource());
        log.info("annual rate (props)    : {}%", properties.rates().fixed().movePointRight(2).toPlainString());
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
