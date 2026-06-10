// services/onboarding/src/test/java/com/buildabank/onboarding/OnboardingOrchestrationTest.java
package com.buildabank.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.buildabank.onboarding.client.AccountClient;
import com.buildabank.onboarding.client.CifClient;
import com.buildabank.onboarding.client.HttpInterfaceClients;
import com.buildabank.onboarding.service.OnboardingFailedException;
import com.buildabank.onboarding.service.OnboardingService;
import com.buildabank.onboarding.web.OnboardingRequest;
import com.buildabank.onboarding.web.OnboardingResult;

/**
 * Step 23 · proves the onboarding <strong>orchestration</strong> over REAL HTTP, with the downstream CIF +
 * demand-account services replaced by an in-process {@link StubDownstream} (the Step-15 in-test-stub pattern —
 * no Docker, deterministic). The happy path runs both steps; a forced account-open failure makes the
 * orchestrator run its <strong>compensation</strong> (deactivate the just-created customer) and surface the
 * failure. Also confirms the caller's bearer token is forwarded downstream (identity propagation).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(OnboardingOrchestrationTest.StubDownstream.class)
class OnboardingOrchestrationTest {

    @LocalServerPort
    int port;

    @Autowired
    StubDownstream stub;

    @BeforeEach
    void reset() {
        stub.reset();
    }

    private OnboardingService orchestrator() {
        String base = "http://localhost:" + port;   // the stubs are served by THIS test app
        CifClient cif = HttpInterfaceClients.create(
                CifClient.class, base, Duration.ofSeconds(2), Duration.ofSeconds(5));
        AccountClient accounts = HttpInterfaceClients.create(
                AccountClient.class, base, Duration.ofSeconds(2), Duration.ofSeconds(5));
        return new OnboardingService(cif, accounts);
    }

    private static OnboardingRequest sampleRequest() {
        return new OnboardingRequest("Ada", "Lovelace", "ada@bank.example", "1990-05-17", "USD");
    }

    @Test
    void happyPath_createsCustomerThenOpensAccount_andForwardsTheToken() {
        OnboardingResult result = orchestrator().onboard(sampleRequest(), "Bearer test-token");

        assertThat(result.customerNumber()).isEqualTo("CIF-STUB");
        assertThat(result.customerId()).isEqualTo(42L);
        assertThat(result.accountNumber()).isEqualTo("DDA-CIF-STUB");   // derived from the customer number
        assertThat(result.status()).isEqualTo("ONBOARDED");
        assertThat(stub.deactivateCalls()).isEmpty();                    // no compensation needed
        assertThat(stub.forwardedAuthorization()).isEqualTo("Bearer test-token");   // identity propagation
    }

    @Test
    void accountOpenFails_orchestratorCompensatesByDeactivatingTheCustomer() {
        stub.failAccountOpen(true);

        assertThatThrownBy(() -> orchestrator().onboard(sampleRequest(), "Bearer test-token"))
                .isInstanceOf(OnboardingFailedException.class);

        // The customer was created (step 1 committed in CIF), the account failed (step 2), so the orchestrator
        // compensated by deactivating that exact customer — no half-onboarded customer left active.
        assertThat(stub.deactivateCalls()).containsExactly(42L);
    }

    /** In-process stand-in for CIF + demand-account, served on the test app's random port. */
    @RestController
    static class StubDownstream {

        private volatile boolean failAccountOpen = false;
        private volatile String forwardedAuthorization;
        private final List<Long> deactivateCalls = new CopyOnWriteArrayList<>();

        void reset() {
            failAccountOpen = false;
            forwardedAuthorization = null;
            deactivateCalls.clear();
        }

        void failAccountOpen(boolean fail) {
            this.failAccountOpen = fail;
        }

        List<Long> deactivateCalls() {
            return deactivateCalls;
        }

        String forwardedAuthorization() {
            return forwardedAuthorization;
        }

        @PostMapping("/api/customers")
        Map<String, Object> createCustomer(@RequestBody Map<String, Object> body) {
            return Map.of("id", 42, "customerNumber", "CIF-STUB");
        }

        @PostMapping("/api/customers/{id}/deactivate")
        ResponseEntity<Void> deactivate(@PathVariable Long id) {
            deactivateCalls.add(id);
            return ResponseEntity.noContent().build();
        }

        @PostMapping("/api/accounts")
        ResponseEntity<Map<String, Object>> openAccount(
                @RequestHeader(value = "Authorization", required = false) String authorization,
                @RequestBody Map<String, Object> body) {
            this.forwardedAuthorization = authorization;
            if (failAccountOpen) {
                return ResponseEntity.status(500).body(Map.of("error", "downstream boom"));
            }
            return ResponseEntity.status(201).body(Map.of("accountNumber", body.get("accountNumber"), "balance", 0));
        }
    }
}
