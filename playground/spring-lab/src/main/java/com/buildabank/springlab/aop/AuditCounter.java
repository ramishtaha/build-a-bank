// playground/spring-lab/src/main/java/com/buildabank/springlab/aop/AuditCounter.java
package com.buildabank.springlab.aop;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

/**
 * Records how many audited calls the aspect actually intercepted. We use it to PROVE the
 * self-invocation pitfall in a test: an internal {@code this.method()} call is NOT advised, so it does
 * not increment this counter. (Thread-safe list because the bean is a shared singleton — see Step 11.)
 */
@Component
public class AuditCounter {

    private final List<String> auditedCalls = new CopyOnWriteArrayList<>();

    public void record(String method) {
        auditedCalls.add(method);
    }

    public int total() {
        return auditedCalls.size();
    }

    public List<String> calls() {
        return List.copyOf(auditedCalls);
    }

    public void reset() {
        auditedCalls.clear();
    }
}
