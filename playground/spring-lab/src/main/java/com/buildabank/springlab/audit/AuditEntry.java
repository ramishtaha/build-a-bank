// playground/spring-lab/src/main/java/com/buildabank/springlab/audit/AuditEntry.java
package com.buildabank.springlab.audit;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A <strong>prototype</strong>-scoped bean: Spring creates a NEW instance every time it is requested
 * (contrast the default <strong>singleton</strong> scope — one shared instance per context). Each
 * instance gets a unique sequence number, so two lookups differ. Web scopes (request/session) arrive later.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AuditEntry {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final long instanceId = SEQUENCE.incrementAndGet();

    public long instanceId() {
        return instanceId;
    }
}
