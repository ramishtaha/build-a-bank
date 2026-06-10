// services/cif/src/main/java/com/buildabank/cif/web/CustomerController.java
package com.buildabank.cif.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buildabank.cif.domain.Customer;
import com.buildabank.cif.service.CustomerService;

/** REST API for the customer master. */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    /** Create a customer → 201 Created with a Location header pointing at the new resource. */
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        Customer created = service.create(
                request.firstName(), request.lastName(), request.email(), request.dateOfBirth());
        CustomerResponse body = CustomerResponse.from(created);
        return ResponseEntity.created(URI.create("/api/customers/" + created.getId())).body(body);
    }

    /** Fetch by database id → 200, or 404 if absent. */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> byId(@PathVariable Long id) {
        return service.findById(id)
                .map(CustomerResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Fetch by the public customer number (e.g. CIF-AB12CD34). */
    @GetMapping("/by-number/{customerNumber}")
    public ResponseEntity<CustomerResponse> byNumber(@PathVariable String customerNumber) {
        return service.findByCustomerNumber(customerNumber)
                .map(CustomerResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deactivate a customer (KYC → REJECTED) → 204. The Step-23 onboarding orchestrator calls this as a
     * <strong>compensating</strong> action when account-opening fails after the customer was created.
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
