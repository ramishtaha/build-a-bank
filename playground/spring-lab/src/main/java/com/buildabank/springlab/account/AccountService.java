// playground/spring-lab/src/main/java/com/buildabank/springlab/account/AccountService.java
package com.buildabank.springlab.account;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.buildabank.springlab.aop.Audited;

/**
 * The "service" half of the vertical slice. Its {@code @Audited} methods are advised by {@code AuditAspect}.
 *
 * <p>{@link #summaryFor(String)} deliberately demonstrates the <strong>self-invocation pitfall</strong>:
 * it calls {@code findById(id)} on {@code this}, which goes straight to the real object and BYPASSES the
 * AOP proxy — so the inner {@code findById} is NOT audited (proven in {@code AuditAspectSelfInvocationTest}).
 */
@Service
public class AccountService {

    private final InMemoryAccountStore store;

    public AccountService(InMemoryAccountStore store) {
        this.store = store;
    }

    @Audited
    public Optional<Account> findById(String id) {
        return store.findById(id);
    }

    @Audited
    public List<Account> findAll() {
        return store.findAll();
    }

    @Audited
    public String summaryFor(String id) {
        // ⚠️ self-invocation: this.findById(...) is NOT intercepted by the aspect.
        return findById(id)
                .map(a -> a.owner() + " has " + a.balance())
                .orElse("account " + id + " not found");
    }
}
