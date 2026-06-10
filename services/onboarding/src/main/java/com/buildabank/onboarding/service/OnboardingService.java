// services/onboarding/src/main/java/com/buildabank/onboarding/service/OnboardingService.java
package com.buildabank.onboarding.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.buildabank.onboarding.client.AccountClient;
import com.buildabank.onboarding.client.AccountClient.OpenAccount;
import com.buildabank.onboarding.client.AccountClient.OpenedAccount;
import com.buildabank.onboarding.client.CifClient;
import com.buildabank.onboarding.client.CifClient.CreateCustomer;
import com.buildabank.onboarding.client.CifClient.CreatedCustomer;
import com.buildabank.onboarding.web.OnboardingRequest;
import com.buildabank.onboarding.web.OnboardingResult;

/**
 * Step 23 · the onboarding <strong>orchestrator</strong>. It runs a multi-service workflow as a sequence of
 * calls and decides what to do on failure — the <em>orchestration</em> flavour of a distributed workflow (one
 * coordinator that knows the whole flow), in contrast to the event-driven <em>choreography</em> of the Step-21
 * Saga. Each step is a remote call (no shared transaction), so recovery is <strong>compensation</strong>, not
 * rollback: if the account step fails after the customer was created, we deactivate the customer.
 *
 * <p>Like a Saga, this is NOT isolated — between steps a customer exists with no account yet. We design for
 * that (the compensation) and surface a clear failure to the caller.
 */
@Service
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private final CifClient cif;
    private final AccountClient accounts;

    public OnboardingService(CifClient cif, AccountClient accounts) {
        this.cif = cif;
        this.accounts = accounts;
    }

    /**
     * Onboard a retail customer: create them in CIF, then open their demand account. The caller's
     * {@code authorization} is forwarded to the (secured) demand-account service.
     *
     * @throws OnboardingFailedException if the account step fails (the just-created customer is compensated)
     */
    public OnboardingResult onboard(OnboardingRequest request, String authorization) {
        // Step 1 — create the customer (its own service + DB).
        CreatedCustomer customer = cif.create(new CreateCustomer(
                request.firstName(), request.lastName(), request.email(), request.dateOfBirth()));
        log.info("onboarding: created customer {}", customer.customerNumber());

        String accountNumber = "DDA-" + customer.customerNumber();
        try {
            // Step 2 — open the account (a different service + DB; no shared transaction with step 1).
            OpenedAccount account = accounts.open(authorization,
                    new OpenAccount(accountNumber, request.currency(), BigDecimal.ZERO));
            log.info("onboarding: opened account {}", account.accountNumber());
        } catch (RuntimeException accountFailure) {
            // COMPENSATE — the customer exists but has no account; deactivate them so they aren't left active.
            log.warn("onboarding: account-open failed for {} — compensating (deactivate)",
                    customer.customerNumber(), accountFailure);
            cif.deactivate(customer.id());
            throw new OnboardingFailedException(
                    "onboarding failed opening the account for " + customer.customerNumber()
                            + "; the customer was deactivated", accountFailure);
        }

        return new OnboardingResult(customer.customerNumber(), customer.id(), accountNumber, "ONBOARDED");
    }
}
