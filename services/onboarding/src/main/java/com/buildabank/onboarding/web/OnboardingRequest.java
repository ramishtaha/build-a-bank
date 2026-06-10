// services/onboarding/src/main/java/com/buildabank/onboarding/web/OnboardingRequest.java
package com.buildabank.onboarding.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request to onboard a retail customer. {@code dateOfBirth} is an ISO date string ("yyyy-MM-dd"). */
public record OnboardingRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank String dateOfBirth,
        @NotBlank String currency) {
}
