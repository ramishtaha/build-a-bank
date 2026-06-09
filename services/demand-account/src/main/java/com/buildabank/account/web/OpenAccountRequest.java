// services/demand-account/src/main/java/com/buildabank/account/web/OpenAccountRequest.java
package com.buildabank.account.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Request body to open an account. Bean Validation rejects bad input before the controller runs. */
public record OpenAccountRequest(
        @NotBlank String accountNumber,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull @PositiveOrZero BigDecimal openingBalance) {
}
