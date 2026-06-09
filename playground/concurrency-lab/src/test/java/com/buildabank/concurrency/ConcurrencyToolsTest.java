// playground/concurrency-lab/src/test/java/com/buildabank/concurrency/ConcurrencyToolsTest.java
package com.buildabank.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Test;

/**
 * The modern {@code java.util.concurrent} toolkit — used the way the bank will: an {@link ExecutorService}
 * (here backed by <strong>virtual threads</strong>, JDK 21+/stable in 25), {@link CompletableFuture}
 * composition, and a {@link Semaphore} bounding concurrency (e.g. limiting calls to a downstream service).
 */
class ConcurrencyToolsTest {

    @Test
    void virtualThreads_runManyTasksConcurrently() throws Exception {
        int tasks = 10_000;
        LongAdder completed = new LongAdder();

        // newVirtualThreadPerTaskExecutor: one lightweight virtual thread per task — 10k is trivial.
        // ExecutorService is AutoCloseable (Java 19+): close() waits for all tasks to finish.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                executor.submit(completed::increment);
            }
        }

        System.out.println("[tools] virtual threads completed " + completed.sum() + " / " + tasks + " tasks");
        assertThat(completed.sum()).isEqualTo(tasks);
    }

    @Test
    void completableFuture_composesAsyncWork() throws Exception {
        CompletableFuture<Long> fees = CompletableFuture.supplyAsync(() -> 100L);
        CompletableFuture<Long> interest = CompletableFuture.supplyAsync(() -> 50L);

        long total = fees.thenCombine(interest, Long::sum).get();   // run both, then combine

        assertThat(total).isEqualTo(150L);
    }

    @Test
    void semaphore_capsConcurrentAccess() throws Exception {
        int permits = 3;
        Semaphore limiter = new Semaphore(permits);
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxObserved = new AtomicInteger();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 100; i++) {
                executor.submit(() -> {
                    try {
                        limiter.acquire();
                        int now = inFlight.incrementAndGet();
                        maxObserved.accumulateAndGet(now, Math::max);
                        Thread.sleep(1);                 // hold the permit briefly
                        inFlight.decrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        limiter.release();
                    }
                });
            }
        }

        System.out.println("[tools] semaphore(" + permits + ") capped concurrency at " + maxObserved.get());
        assertThat(maxObserved.get()).isLessThanOrEqualTo(permits);   // never more than `permits` at once
    }
}
