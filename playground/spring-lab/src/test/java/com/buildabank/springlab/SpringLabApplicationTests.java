// playground/spring-lab/src/test/java/com/buildabank/springlab/SpringLabApplicationTests.java
package com.buildabank.springlab;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.buildabank.springlab.audit.AuditEntry;
import com.buildabank.springlab.interest.InterestService;

/** Full-context test (default profile: fixed rate). Proves wiring, the interest calc, and bean scopes. */
@SpringBootTest(properties = {"bank.rates.source=fixed", "bank.rates.fixed=0.0325", "bank.name=Build-a-Bank"})
class SpringLabApplicationTests {

    @Autowired
    InterestService interest;

    @Autowired
    ApplicationContext context;

    @Test
    void contextLoadsAndWiresTheFixedProvider() {
        assertThat(interest.rateSource()).isEqualTo("fixed");
        assertThat(interest.annualInterest(new BigDecimal("10000.00"))).isEqualByComparingTo("325.00");
    }

    @Test
    void servicesAreSingletons() {
        assertThat(context.getBean(InterestService.class)).isSameAs(context.getBean(InterestService.class));
    }

    @Test
    void auditEntryIsPrototype() {
        assertThat(context.getBean(AuditEntry.class)).isNotSameAs(context.getBean(AuditEntry.class));
    }
}
