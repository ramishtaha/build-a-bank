// services/demand-account/src/main/java/com/buildabank/account/web/PaymentController.java
package com.buildabank.account.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.buildabank.account.payment.PaymentService;

/**
 * Step 21 · the payments API. {@code POST /api/v1/payments} runs the payment {@link PaymentService Saga} and
 * is <strong>idempotent</strong> via an optional {@code Idempotency-Key} header (backed by Redis): retrying
 * with the same key returns the original payment instead of paying twice. Secured like every money endpoint
 * (resource server, Step 17). A Saga that fails after a step committed compensates and returns 422
 * (see {@link GlobalExceptionHandler}).
 */
@RestController
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/api/v1/payments")
    public ResponseEntity<PaymentResponse> pay(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        UUID paymentId = payments.pay(request.from(), request.to(), request.amount(), idempotencyKey);
        return ResponseEntity.ok(new PaymentResponse(paymentId));
    }
}
