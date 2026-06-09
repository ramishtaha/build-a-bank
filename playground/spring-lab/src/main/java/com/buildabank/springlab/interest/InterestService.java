// playground/spring-lab/src/main/java/com/buildabank/springlab/interest/InterestService.java
package com.buildabank.springlab.interest;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.buildabank.springlab.rates.RateProvider;

/**
 * A singleton {@code @Service} that computes interest. It receives its {@link RateProvider} via
 * <strong>constructor injection</strong> — Spring sees the single constructor and supplies the wired bean
 * automatically (no {@code @Autowired} needed). The field is {@code final}: set once, safe to share.
 */
@Service
public class InterestService {

    private final RateProvider rateProvider;

    public InterestService(RateProvider rateProvider) {
        this.rateProvider = rateProvider;
    }

    /** Annual interest on a principal, rounded to 2 dp (banker's rounding). */
    public BigDecimal annualInterest(BigDecimal principal) {
        return principal.multiply(rateProvider.annualRate()).setScale(2, RoundingMode.HALF_EVEN);
    }

    public String rateSource() {
        return rateProvider.name();
    }
}
