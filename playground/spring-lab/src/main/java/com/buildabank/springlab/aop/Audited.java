// playground/spring-lab/src/main/java/com/buildabank/springlab/aop/Audited.java
package com.buildabank.springlab.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as auditable. {@code AuditAspect} advises every call to a method carrying this annotation.
 * {@code RUNTIME} retention is required so the AOP pointcut can see it via reflection at runtime.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
}
