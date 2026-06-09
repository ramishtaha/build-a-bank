// playground/spring-lab/src/main/java/com/buildabank/springlab/lifecycle/TimingBeanPostProcessor.java
package com.buildabank.springlab.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * A {@link BeanPostProcessor} — a container extension point invoked for EVERY bean, around its
 * initialization. This is exactly how Spring itself implements much of its "magic" (e.g. wiring AOP
 * proxies, resolving {@code @Value}). Here we just log around {@link LifecycleBean} to show the ordering.
 */
@Component
public class TimingBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(TimingBeanPostProcessor.class);

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof LifecycleBean) {
            log.info("2) BPP before-init for bean '{}'", beanName);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof LifecycleBean) {
            log.info("4) BPP after-init for bean '{}'", beanName);
        }
        return bean;
    }
}
