// playground/spring-lab/src/main/java/com/buildabank/springlab/aop/AuditAspect.java
package com.buildabank.springlab.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The bank's cross-cutting <strong>audit/logging aspect</strong>. Instead of copying logging into every
 * service method, we declare it once here and apply it declaratively.
 *
 * <ul>
 *   <li><b>Aspect</b> — this class (the cross-cutting concern).</li>
 *   <li><b>Pointcut</b> — {@code @annotation(...Audited)}: "any method annotated {@code @Audited}".</li>
 *   <li><b>Advice</b> — {@code @Around}: wraps the call (before + after + on exception).</li>
 *   <li><b>Join point</b> — the actual method execution, captured as {@link ProceedingJoinPoint}.</li>
 * </ul>
 *
 * <p>How it works: Spring creates a PROXY around each audited bean (CGLIB by default in Boot). Calls from
 * OUTSIDE go through the proxy → advised. A {@code this.method()} call from inside the bean bypasses the
 * proxy → NOT advised (the famous self-invocation pitfall, proven in the tests).
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditCounter counter;

    public AuditAspect(AuditCounter counter) {
        this.counter = counter;
    }

    @Around("@annotation(com.buildabank.springlab.aop.Audited)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        counter.record(method);
        long start = System.nanoTime();
        log.info("AUDIT ▶ {} called", method);
        try {
            Object result = joinPoint.proceed();           // run the real method
            long micros = (System.nanoTime() - start) / 1_000;
            log.info("AUDIT ✔ {} returned in {} µs", method, micros);
            return result;
        } catch (Throwable t) {
            log.warn("AUDIT ✗ {} threw {}", method, t.toString());
            throw t;                                        // never swallow — re-throw
        }
    }
}
