// services/cif/src/test/java/com/buildabank/cif/domain/SqlInjectionSafetyTest.java
package com.buildabank.cif.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.buildabank.cif.ContainersConfig;

/**
 * Step 18 (secure coding) — proves the CIF query layer is <strong>injection-safe</strong>, against a REAL
 * Postgres (Testcontainers), and proves the proof has teeth with a side-by-side <em>vulnerable</em> contrast.
 *
 * <p>OWASP A03:2021 Injection. Spring Data derived queries ({@code findByCustomerNumber}, {@code existsByEmail})
 * compile to <strong>parameterized</strong> SQL ({@code WHERE customer_number = ?}); the user input is sent as
 * a bound value, never spliced into the SQL text, so a classic {@code ' OR '1'='1} payload is just a string
 * that equals no real key. The contrast test builds the same query by <strong>string concatenation</strong>
 * (the anti-pattern) and shows the identical payload now matches every row — that delta is the whole lesson.
 */
@DataJpaTest
@Import(ContainersConfig.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SqlInjectionSafetyTest {

    /** The textbook authentication-bypass payload: closes the string and OR-trues the predicate. */
    private static final String CLASSIC_PAYLOAD = "' OR '1'='1";
    /** A "stacked query" payload that would try to run a second, destructive statement. */
    private static final String DESTRUCTIVE_PAYLOAD = "x'; DROP TABLE customer; --";

    @Autowired
    CustomerRepository repository;

    @Autowired
    EntityManager em;

    private void seedOneRealCustomer() {
        repository.saveAndFlush(new Customer("CIF-REAL01", "Grace", "Hopper", "grace@bank.example",
                LocalDate.of(1985, 1, 2), KycStatus.PENDING, Instant.now()));
    }

    @Test
    void derivedQueriesBindInputAsData_soInjectionMatchesNothing() {
        seedOneRealCustomer();

        // Parameterized: the entire payload is one bound value compared to customer_number → no match.
        assertThat(repository.findByCustomerNumber(CLASSIC_PAYLOAD)).isEmpty();
        // Same for the boolean existence check on email.
        assertThat(repository.existsByEmail(CLASSIC_PAYLOAD)).isFalse();
    }

    @Test
    void stackedQueryPayloadIsTreatedAsData_theTableSurvives() {
        seedOneRealCustomer();

        // The "'; DROP TABLE customer; --" is bound as a value, never parsed as SQL — so it matches nothing...
        assertThat(repository.findByCustomerNumber(DESTRUCTIVE_PAYLOAD)).isEmpty();

        // ...and, crucially, the table is intact and still serves the real row afterward.
        assertThat(repository.findByCustomerNumber("CIF-REAL01")).isPresent();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void contrast_handConcatenatedSqlWOULDhaveBeenInjectable() {
        seedOneRealCustomer();

        // ❗ ANTI-PATTERN — never do this in real code. Building SQL by string concatenation. We do it ONLY
        // here, in a test, to prove the assertions above are meaningful: the SAME payload that matched
        // NOTHING through the parameterized repository matches EVERY row when concatenated, because
        // "'' OR '1'='1'" is now SQL syntax (an always-true predicate), not a value being compared.
        long viaConcat = ((Number) em.createNativeQuery(
                "SELECT count(*) FROM customer WHERE customer_number = '" + CLASSIC_PAYLOAD + "'")
                .getSingleResult()).longValue();
        assertThat(viaConcat).isEqualTo(1);   // injection SUCCEEDED on the vulnerable query: matched the real row

        long viaParameterized = repository.findByCustomerNumber(CLASSIC_PAYLOAD).map(c -> 1L).orElse(0L);
        assertThat(viaParameterized).isZero(); // parameterized query is immune: payload is just data
    }
}
