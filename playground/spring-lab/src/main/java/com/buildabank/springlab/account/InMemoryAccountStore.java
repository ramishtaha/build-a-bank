// playground/spring-lab/src/main/java/com/buildabank/springlab/account/InMemoryAccountStore.java
package com.buildabank.springlab.account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

/** A tiny in-memory store seeded with two demo accounts (the "store" half of the vertical slice). */
@Repository
public class InMemoryAccountStore {

    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();

    public InMemoryAccountStore() {
        save(new Account("1", "Ada Lovelace", new BigDecimal("1250.00")));
        save(new Account("2", "Alan Turing", new BigDecimal("8000.00")));
    }

    public Optional<Account> findById(String id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public List<Account> findAll() {
        return List.copyOf(accounts.values());
    }

    public Account save(Account account) {
        accounts.put(account.id(), account);
        return account;
    }
}
