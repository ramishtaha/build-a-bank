// services/demand-account/src/main/java/com/buildabank/account/service/AuditService.java
package com.buildabank.account.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.buildabank.account.domain.AuditEntry;
import com.buildabank.account.domain.AuditEntryRepository;

/**
 * Writes audit records in their OWN transaction. {@code Propagation.REQUIRES_NEW} suspends any caller's
 * transaction and starts a fresh one that commits independently — so an audit row <strong>survives even if
 * the business transaction that called it rolls back</strong>. (Contrast with the default {@code REQUIRED},
 * which joins the caller's transaction and would roll back with it.)
 *
 * <p>Note the seam: REQUIRES_NEW only takes effect when called through the Spring proxy — i.e. from a
 * <em>different</em> bean. A {@code this.}-call inside the same bean bypasses the proxy (the self-invocation
 * pitfall from Step 7), so this lives in its own service.
 */
@Service
public class AuditService {

    private final AuditEntryRepository repository;

    public AuditService(AuditEntryRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String event, String detail) {
        repository.save(new AuditEntry(event, detail, Instant.now()));
    }
}
