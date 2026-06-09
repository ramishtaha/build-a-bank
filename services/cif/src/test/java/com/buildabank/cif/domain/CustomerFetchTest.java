// services/cif/src/test/java/com/buildabank/cif/domain/CustomerFetchTest.java
package com.buildabank.cif.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.buildabank.cif.ContainersConfig;

/**
 * Demonstrates — with Hibernate's own statistics as the witness — the <strong>N+1 problem</strong> and its
 * fix. Two customers (3 addresses total) are seeded, then we count the SQL statements for a lazy traversal
 * vs. an {@code @EntityGraph} fetch.
 */
@DataJpaTest
@Import(ContainersConfig.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class CustomerFetchTest {

    @Autowired
    CustomerRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @BeforeEach
    void seed() {
        Customer ada = new Customer("CIF-F1", "Ada", "Lovelace", "ada.f@bank.example",
                LocalDate.of(1990, 5, 17), KycStatus.PENDING, Instant.now());
        ada.addAddress(new Address("1 Analytical Ave", "London", "GB"));
        ada.addAddress(new Address("2 Engine Way", "London", "GB"));

        Customer alan = new Customer("CIF-F2", "Alan", "Turing", "alan.f@bank.example",
                LocalDate.of(1992, 6, 23), KycStatus.PENDING, Instant.now());
        alan.addAddress(new Address("3 Bletchley Rd", "Milton Keynes", "GB"));

        repository.save(ada);
        repository.save(alan);
        entityManager.flush();
        entityManager.clear(); // detach everything so the reads below actually hit the DB
    }

    @Test
    void lazyTraversalCausesNPlusOneQueries() {
        Statistics stats = statistics();
        stats.clear();

        List<Customer> all = repository.findAll();                 // 1 query for the customers
        int addresses = all.stream().mapToInt(c -> c.getAddresses().size()).sum(); // +1 query PER customer

        assertThat(addresses).isEqualTo(3);
        // 1 (customers) + 2 (one lazy address load per customer) = 3  →  the N+1 signature
        assertThat(stats.getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    void entityGraphFetchesEverythingInOneQuery() {
        Statistics stats = statistics();
        stats.clear();

        List<Customer> all = repository.findAllWithAddresses();    // single query (addresses joined in)
        int addresses = all.stream().mapToInt(c -> c.getAddresses().size()).sum(); // no extra queries

        assertThat(addresses).isEqualTo(3);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void projectionReturnsOnlyTheSummary() {
        List<CustomerSummary> summaries = repository.findByKycStatus(KycStatus.PENDING);
        assertThat(summaries).hasSize(2);
        assertThat(summaries).allSatisfy(s -> assertThat(s.getCustomerNumber()).startsWith("CIF-F"));
    }
}
