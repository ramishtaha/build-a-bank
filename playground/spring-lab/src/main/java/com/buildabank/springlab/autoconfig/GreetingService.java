// playground/spring-lab/src/main/java/com/buildabank/springlab/autoconfig/GreetingService.java
package com.buildabank.springlab.autoconfig;

/**
 * A plain service (NOT a {@code @Component}). It is contributed by {@code GreetingAutoConfiguration}
 * as a {@code @Bean}, the same way Spring Boot's own starters auto-configure beans for you. This is the
 * miniature preview of the real auto-configured starter you build in Step 28.
 */
public class GreetingService {

    private final String bankName;

    public GreetingService(String bankName) {
        this.bankName = bankName;
    }

    public String greet(String who) {
        return "Welcome to %s, %s!".formatted(bankName, who);
    }
}
