// services/cif/src/main/java/com/buildabank/cif/domain/CustomerRepository.java
package com.buildabank.cif.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository. Extending {@link JpaRepository} gives CRUD + paging for free; the
 * {@code findByCustomerNumber} method is a <strong>derived query</strong> — Spring Data parses the method
 * NAME into a JPQL query at startup (no implementation needed). We dissect how that works in Step 9.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerNumber(String customerNumber);

    boolean existsByEmail(String email);
}
