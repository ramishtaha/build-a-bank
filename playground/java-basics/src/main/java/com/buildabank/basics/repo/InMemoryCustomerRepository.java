// playground/java-basics/src/main/java/com/buildabank/basics/repo/InMemoryCustomerRepository.java
package com.buildabank.basics.repo;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.buildabank.basics.customer.Customer;

/**
 * An in-memory {@link Repository} of {@link Customer}, keyed by id.
 *
 * <p>Uses {@link ConcurrentHashMap} — a thread-safe map — because even a toy store can be touched by
 * multiple threads. (We go deep on concurrency in Step 11; here it is just "the safe default map".)
 * {@code findById} returns {@link Optional}, never {@code null}.
 */
public class InMemoryCustomerRepository implements Repository<Customer, Long> {

    private final ConcurrentMap<Long, Customer> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Customer> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Customer> findAll() {
        // Defensive copy: callers cannot mutate our internal store.
        return List.copyOf(store.values());
    }

    @Override
    public Customer save(Customer entity) {
        store.put(entity.id(), entity);
        return entity;
    }
}
