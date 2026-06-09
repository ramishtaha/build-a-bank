// services/demand-account/src/main/java/com/buildabank/account/payment/PaymentService.java
package com.buildabank.account.payment;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Step 21 · the payment <strong>Saga orchestrator</strong>. A payment is a sequence of independently-committed
 * steps coordinated here; if a later step fails, the orchestrator runs the matching <strong>compensation</strong>
 * to undo the committed ones. (This is the <em>orchestration</em> flavour — one coordinator drives the steps.
 * The <em>choreography</em> flavour has each service react to events with no central coordinator; the Step-20
 * Kafka pipeline is the substrate for that, and the lesson contrasts the two.)
 *
 * <p>Deliberately NOT {@code @Transactional}: the orchestrator must not wrap the steps in one transaction —
 * each {@link PaymentStepService} method is {@code REQUIRES_NEW} and commits on its own, which is exactly why
 * compensation (not rollback) is the recovery mechanism. Idempotency is handled up front via a Redis-backed
 * {@link RedisIdempotencyStore} so a retried payment returns the original {@code paymentId}.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentStepService steps;
    private final RedisIdempotencyStore idempotency;

    public PaymentService(PaymentStepService steps, RedisIdempotencyStore idempotency) {
        this.steps = steps;
        this.idempotency = idempotency;
    }

    /**
     * Execute (or replay) a payment from {@code from} to {@code to}. With an {@code idempotencyKey}, a retry
     * returns the original payment instead of moving money again.
     *
     * @throws PaymentFailedException if a step fails after an earlier step committed (the earlier step is
     *                                compensated first, leaving balances consistent)
     */
    public UUID pay(String from, String to, BigDecimal amount, String idempotencyKey) {
        boolean idempotent = idempotencyKey != null && !idempotencyKey.isBlank();
        if (idempotent) {
            Optional<String> prior = idempotency.completedPaymentId(idempotencyKey);
            if (prior.isPresent()) {
                return UUID.fromString(prior.get());   // retry hit — do NOT pay again
            }
        }

        UUID paymentId = UUID.randomUUID();

        // ── Saga ──
        steps.debit(from, amount, paymentId);          // step 1 — commits independently
        try {
            steps.credit(to, amount, paymentId);       // step 2 — commits independently
        } catch (RuntimeException stepFailure) {
            // step 2 failed AFTER step 1 committed → COMPENSATE step 1, then surface the failure.
            log.warn("payment {} failed at credit ({}); compensating with a refund to {}",
                    paymentId, stepFailure.toString(), from);
            steps.refund(from, amount, paymentId);
            throw new PaymentFailedException(
                    "payment " + paymentId + " failed; source " + from + " was refunded", stepFailure);
        }

        if (idempotent) {
            idempotency.recordCompleted(idempotencyKey, paymentId.toString());
        }
        return paymentId;
    }
}
