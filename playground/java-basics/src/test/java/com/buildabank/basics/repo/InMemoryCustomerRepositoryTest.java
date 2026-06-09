// playground/java-basics/src/test/java/com/buildabank/basics/repo/InMemoryCustomerRepositoryTest.java
package com.buildabank.basics.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.buildabank.basics.customer.Customer;

class InMemoryCustomerRepositoryTest {

    private final InMemoryCustomerRepository repo = new InMemoryCustomerRepository();

    @Test
    void savesAndFindsById() {
        var c = new Customer(1L, "Ada", "Lovelace", LocalDate.of(1990, 5, 17));
        repo.save(c);
        assertThat(repo.findById(1L)).contains(c);
    }

    @Test
    void findByIdReturnsEmptyWhenAbsent() {
        assertThat(repo.findById(999L)).isEmpty();
    }

    @Test
    void findAllReturnsAnImmutableSnapshot() {
        repo.save(new Customer(1L, "Ada", "Lovelace", LocalDate.of(1990, 5, 17)));
        repo.save(new Customer(2L, "Alan", "Turing", LocalDate.of(1992, 6, 23)));
        assertThat(repo.findAll()).hasSize(2);
    }
}
