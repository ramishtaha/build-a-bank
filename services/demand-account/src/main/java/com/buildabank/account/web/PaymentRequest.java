// services/demand-account/src/main/java/com/buildabank/account/web/PaymentRequest.java
package com.buildabank.account.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Request body for a payment (a Saga-orchestrated cross-account money movement). Amount must be positive. */
public record PaymentRequest(
        @NotBlank String from,
        @NotBlank String to,
        @NotNull @Positive BigDecimal amount) {
}
