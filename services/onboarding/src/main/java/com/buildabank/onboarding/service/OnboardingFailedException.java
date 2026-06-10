// services/onboarding/src/main/java/com/buildabank/onboarding/service/OnboardingFailedException.java
package com.buildabank.onboarding.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Onboarding could not be completed: a downstream step failed and the orchestrator ran its compensation
 * (so the system is left consistent). Mapped to 502 Bad Gateway — the failure originated downstream.
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class OnboardingFailedException extends RuntimeException {

    public OnboardingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
