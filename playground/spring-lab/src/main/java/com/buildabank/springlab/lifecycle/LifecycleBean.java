// playground/spring-lab/src/main/java/com/buildabank/springlab/lifecycle/LifecycleBean.java
package com.buildabank.springlab.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Makes the bean lifecycle visible. Watch the log order on startup:
 * <pre>
 *   1) constructor        ← instance created
 *   2) BPP before-init    ← BeanPostProcessor.postProcessBeforeInitialization
 *   3) @PostConstruct     ← initialization callback
 *   4) BPP after-init     ← BeanPostProcessor.postProcessAfterInitialization
 * </pre>
 * and {@code @PreDestroy} on shutdown. (Steps 2 &amp; 4 are logged by {@code TimingBeanPostProcessor}.)
 */
@Component
public class LifecycleBean {

    private static final Logger log = LoggerFactory.getLogger(LifecycleBean.class);

    public LifecycleBean() {
        log.info("1) constructor");
    }

    @PostConstruct
    void init() {
        log.info("3) @PostConstruct");
    }

    @PreDestroy
    void destroy() {
        log.info("@PreDestroy (context closing)");
    }
}
