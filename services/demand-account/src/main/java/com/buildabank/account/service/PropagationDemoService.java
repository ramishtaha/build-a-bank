// services/demand-account/src/main/java/com/buildabank/account/service/PropagationDemoService.java
package com.buildabank.account.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demonstrates transaction <strong>propagation</strong>. This (outer) method runs in its own transaction,
 * writes an audit record through {@link AuditService} (which is {@code REQUIRES_NEW}, so it commits in a
 * <em>separate</em> transaction), then throws. The outer transaction rolls back — but the audit row, having
 * committed independently, <strong>survives</strong>. That's the whole point of REQUIRES_NEW.
 */
@Service
public class PropagationDemoService {

    private final AuditService auditService;

    public PropagationDemoService(AuditService auditService) {
        this.auditService = auditService;
    }

    @Transactional
    public void auditThenFail(String event) {
        auditService.record(event, "written before the failure");   // REQUIRES_NEW → commits independently
        throw new IllegalStateException("business failure after auditing");   // rolls the OUTER txn back
    }
}
