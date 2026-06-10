// services/onboarding/src/main/java/com/buildabank/onboarding/OnboardingApplication.java
package com.buildabank.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Step 23 · the Retail onboarding service — a thin <strong>orchestrator</strong> (no database) that drives a
 * multi-service workflow over declarative HTTP clients: create a customer in CIF, open a demand account, and
 * <strong>compensate</strong> (deactivate the customer) if the account step fails.
 */
@SpringBootApplication
public class OnboardingApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnboardingApplication.class, args);
    }
}
