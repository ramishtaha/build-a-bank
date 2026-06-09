// services/cif/src/main/java/com/buildabank/cif/domain/CustomerRepository.java
package com.buildabank.cif.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository. Extending {@link JpaRepository} gives CRUD + paging for free; the
 * {@code findByCustomerNumber} method is a <strong>derived query</strong> — Spring Data parses the method
 * NAME into a JPQL query at startup (no implementation needed).
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerNumber(String customerNumber);

    boolean existsByEmail(String email);

    /**
     * Loads customers WITH their addresses in a single query (an {@code @EntityGraph} turns the lazy
     * association into a join just for this call) — the N+1 fix. Contrast with calling {@code findAll()}
     * and then touching {@code getAddresses()} on each, which fires one extra query per customer.
     */
    @EntityGraph(attributePaths = "addresses")
    @Query("select c from Customer c")
    List<Customer> findAllWithAddresses();

    /** Returns a lightweight {@link CustomerSummary} projection (SELECTs only the projected columns). */
    List<CustomerSummary> findByKycStatus(KycStatus kycStatus);
}
