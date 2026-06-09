// services/demand-account/src/main/java/com/buildabank/account/web/TransferRequest.java
package com.buildabank.account.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Request body for a money transfer. The amount must be strictly positive. */
public record TransferRequest(
        @NotBlank String from,
        @NotBlank String to,
        @NotNull @Positive BigDecimal amount,
        String description) {
}
