// services/onboarding/src/main/java/com/buildabank/onboarding/client/CifClient.java
package com.buildabank.onboarding.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Declarative HTTP client for the CIF service (the Step-15 {@code @HttpExchange} pattern). The orchestrator
 * uses {@link #create} to register a customer and {@link #deactivate} as the <strong>compensating</strong>
 * action when a later step fails.
 */
@HttpExchange
public interface CifClient {

    /** POST /api/customers → the created customer. */
    @PostExchange("/api/customers")
    CreatedCustomer create(@RequestBody CreateCustomer request);

    /** POST /api/customers/{id}/deactivate → compensation: mark the customer rejected/inactive. */
    @PostExchange("/api/customers/{id}/deactivate")
    void deactivate(@PathVariable Long id);

    /** What we send CIF. {@code dateOfBirth} is an ISO string ("yyyy-MM-dd") — CIF parses it to a LocalDate. */
    record CreateCustomer(String firstName, String lastName, String email, String dateOfBirth) {
    }

    /** What CIF returns — we only need the id + customer number ({@code ignoreUnknown} tolerates the rest). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record CreatedCustomer(Long id, String customerNumber) {
    }
}
