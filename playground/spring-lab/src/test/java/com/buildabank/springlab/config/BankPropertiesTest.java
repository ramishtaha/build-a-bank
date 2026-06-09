// playground/spring-lab/src/test/java/com/buildabank/springlab/config/BankPropertiesTest.java
package com.buildabank.springlab.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Proves {@code bank.*} properties bind into the typed {@link BankProperties} record. */
@SpringBootTest(properties = {"bank.name=Build-a-Bank", "bank.rates.source=fixed", "bank.rates.fixed=0.0325"})
class BankPropertiesTest {

    @Autowired
    BankProperties properties;

    @Test
    void bindsTypedConfiguration() {
        assertThat(properties.name()).isEqualTo("Build-a-Bank");
        assertThat(properties.rates().source()).isEqualTo("fixed");
        assertThat(properties.rates().fixed()).isEqualByComparingTo("0.0325");
    }
}
