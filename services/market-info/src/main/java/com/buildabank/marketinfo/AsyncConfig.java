// services/market-info/src/main/java/com/buildabank/marketinfo/AsyncConfig.java
package com.buildabank.marketinfo;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Step 22 · runs {@code @Async} work on <strong>virtual threads</strong> (Project Loom) via a dedicated
 * executor, rather than flipping the whole app to virtual threads — keeping the rest of the stack (Tomcat,
 * the Lettuce/Redis client) on its normal threading. A {@link SimpleAsyncTaskExecutor} with
 * {@code setVirtualThreads(true)} starts each task on a fresh virtual thread — cheap by the thousands and
 * ideal for the mostly-waiting cache-warm calls (Step 11).
 */
@Configuration
public class AsyncConfig {

    /** The executor Spring uses for {@code @Async} methods (bean name is the Spring default). */
    @Bean
    Executor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("market-async-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
