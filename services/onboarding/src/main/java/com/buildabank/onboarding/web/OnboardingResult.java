// services/onboarding/src/main/java/com/buildabank/onboarding/web/OnboardingResult.java
package com.buildabank.onboarding.web;

/** The outcome of a successful onboarding: the new customer + their freshly-opened account. */
public record OnboardingResult(String customerNumber, Long customerId, String accountNumber, String status) {
}
