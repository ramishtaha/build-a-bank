// services/demand-account/src/main/java/com/buildabank/account/client/CifClient.java
package com.buildabank.account.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * A <strong>declarative HTTP interface</strong> for calling the CIF service. You declare the calls as
 * annotated methods; Spring generates the implementation (an {@code HttpServiceProxyFactory} proxy backed by
 * a {@code RestClient}) — no hand-written HTTP plumbing. This is the modern, type-safe successor to writing
 * {@code RestTemplate} calls by hand (and to Spring Cloud OpenFeign for in-Spring use).
 */
@HttpExchange("/api/customers")
public interface CifClient {

    /** GET /api/customers/by-number/{customerNumber} → the customer, or an error status mapped to an exception. */
    @GetExchange("/by-number/{customerNumber}")
    CifCustomer getByNumber(@PathVariable String customerNumber);
}
