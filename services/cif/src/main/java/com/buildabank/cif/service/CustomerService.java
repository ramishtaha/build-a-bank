// services/cif/src/main/java/com/buildabank/cif/service/CustomerService.java
package com.buildabank.cif.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buildabank.cif.domain.Customer;
import com.buildabank.cif.domain.CustomerRepository;
import com.buildabank.cif.domain.KycStatus;

/**
 * Application service for customers. Methods are transactional — writes in a read-write transaction,
 * reads marked {@code readOnly} (a hint that lets the DB/JDBC driver optimize and prevents accidental
 * flushes). Transaction propagation/isolation gets the deep treatment in Step 12.
 */
@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Customer create(String firstName, String lastName, String email, LocalDate dateOfBirth) {
        String customerNumber = "CIF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Customer customer = new Customer(
                customerNumber, firstName, lastName, email, dateOfBirth, KycStatus.PENDING, Instant.now());
        return repository.save(customer);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findByCustomerNumber(String customerNumber) {
        return repository.findByCustomerNumber(customerNumber);
    }

    /**
     * Deactivate a customer (KYC → {@link KycStatus#REJECTED}). Added for the Step-23 onboarding orchestration:
     * if opening the account fails after the customer was created, the orchestrator calls this as a
     * <strong>compensating</strong> action so a half-onboarded customer isn't left active. Dirty checking
     * flushes the status change at commit.
     */
    @Transactional
    public void deactivate(Long id) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("no such customer: " + id));
        customer.setKycStatus(KycStatus.REJECTED);
    }
}
