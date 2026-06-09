// services/cif/src/test/java/com/buildabank/cif/domain/CustomerRepositoryTest.java
package com.buildabank.cif.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.buildabank.cif.ContainersConfig;

/**
 * Repository slice test against a REAL Postgres (Testcontainers), not H2 — so Flyway runs, the
 * Postgres-specific schema is exercised, and derived queries hit the actual engine.
 * {@code replace = NONE} stops {@code @DataJpaTest} from swapping in an embedded database.
 *
 * <p>{@code @ImportAutoConfiguration(FlywayAutoConfiguration.class)} is required because the
 * {@code @DataJpaTest} slice does NOT include Flyway by default — without it, the schema is never
 * created and Hibernate's {@code ddl-auto=validate} fails with "missing table [customer]".
 */
@DataJpaTest
@Import(ContainersConfig.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryTest {

    @Autowired
    CustomerRepository repository;

    @Test
    void savesAndFindsByCustomerNumber() {
        repository.save(new Customer("CIF-TEST01", "Ada", "Lovelace", "ada@bank.example",
                LocalDate.of(1990, 5, 17), KycStatus.PENDING, Instant.now()));

        assertThat(repository.findByCustomerNumber("CIF-TEST01"))
                .get()
                .satisfies(c -> {
                    assertThat(c.getId()).isNotNull();                 // DB-generated identity
                    assertThat(c.getEmail()).isEqualTo("ada@bank.example");
                    assertThat(c.getKycStatus()).isEqualTo(KycStatus.PENDING);
                });
    }

    @Test
    void existsByEmailDerivedQuery() {
        repository.save(new Customer("CIF-TEST02", "Alan", "Turing", "alan@bank.example",
                LocalDate.of(1992, 6, 23), KycStatus.PENDING, Instant.now()));

        assertThat(repository.existsByEmail("alan@bank.example")).isTrue();
        assertThat(repository.existsByEmail("nobody@bank.example")).isFalse();
    }
}
