// playground/spring-lab/src/test/java/com/buildabank/springlab/MarketRateContextTest.java
package com.buildabank.springlab;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.buildabank.springlab.interest.InterestService;

/** Same app, different configuration: flipping bank.rates.source=market wires a different bean — no code change. */
@SpringBootTest(properties = {"bank.rates.source=market", "bank.name=Build-a-Bank"})
class MarketRateContextTest {

    @Autowired
    InterestService interest;

    @Test
    void wiresTheMarketProviderByConfigurationAlone() {
        assertThat(interest.rateSource()).isEqualTo("market");
        assertThat(interest.annualInterest(new BigDecimal("10000.00"))).isEqualByComparingTo("475.00");
    }
}
