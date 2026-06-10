// services/onboarding/src/main/java/com/buildabank/onboarding/client/AccountClient.java
package com.buildabank.onboarding.client;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Declarative HTTP client for the demand-account service. demand-account is a secured OAuth2 resource server
 * (Step 17), so we <strong>forward the caller's bearer token</strong> ({@code Authorization} header) —
 * identity propagation through the orchestration. A 5xx from this call surfaces as an exception, which the
 * orchestrator catches to trigger compensation.
 */
@HttpExchange
public interface AccountClient {

    /** POST /api/accounts (with the forwarded Authorization header) → the opened account. */
    @PostExchange("/api/accounts")
    OpenedAccount open(@RequestHeader(name = "Authorization", required = false) String authorization,
                       @RequestBody OpenAccount request);

    /** What we send demand-account. */
    record OpenAccount(String accountNumber, String currency, BigDecimal openingBalance) {
    }

    /** What demand-account returns (we tolerate extra fields). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenedAccount(String accountNumber, BigDecimal balance) {
    }
}
