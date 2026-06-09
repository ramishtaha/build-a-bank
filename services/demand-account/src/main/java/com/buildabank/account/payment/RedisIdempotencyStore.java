// services/demand-account/src/main/java/com/buildabank/account/payment/RedisIdempotencyStore.java
package com.buildabank.account.payment;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Step 21 · a durable <strong>Idempotency-Key</strong> store backed by <strong>Redis</strong>. A client that
 * retries a payment (after a timeout, say) sends the same {@code Idempotency-Key}; we record the resulting
 * {@code paymentId} against that key so a retry returns the original result instead of paying twice.
 *
 * <p>Why Redis (vs the DB store in Step 14, or the in-memory set in Step 20)? It's fast, shared across
 * instances, and entries auto-expire via TTL (idempotency keys only matter for a retry window). We use
 * {@code SET key value NX EX ttl} ({@code setIfAbsent} with a TTL) — an atomic "claim if not present".
 */
@Component
public class RedisIdempotencyStore {

    private static final String PREFIX = "idem:payment:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public RedisIdempotencyStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** The paymentId previously recorded for this key, if any (a retry hit). */
    public Optional<String> completedPaymentId(String idempotencyKey) {
        return Optional.ofNullable(redis.opsForValue().get(PREFIX + idempotencyKey));
    }

    /** Record the result for this key (atomic claim + TTL). No-op if a value is already present. */
    public void recordCompleted(String idempotencyKey, String paymentId) {
        redis.opsForValue().setIfAbsent(PREFIX + idempotencyKey, paymentId, TTL);
    }
}
