// services/onboarding/src/main/java/com/buildabank/onboarding/web/OnboardingController.java
package com.buildabank.onboarding.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buildabank.onboarding.service.OnboardingService;

/**
 * Step 23 · {@code POST /api/onboarding} kicks off the orchestration (create customer → open account, with
 * compensation on failure). The caller's {@code Authorization} header is forwarded to the secured
 * demand-account service. Returns 201 with the onboarding result, or 502 if a downstream step failed (after
 * compensation) — see {@code OnboardingFailedException}.
 */
@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingService onboarding;

    public OnboardingController(OnboardingService onboarding) {
        this.onboarding = onboarding;
    }

    @PostMapping
    public ResponseEntity<OnboardingResult> onboard(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody OnboardingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onboarding.onboard(request, authorization));
    }
}
