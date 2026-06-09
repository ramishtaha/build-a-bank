// services/demand-account/src/main/java/com/buildabank/account/web/PaymentResponse.java
package com.buildabank.account.web;

import java.util.UUID;

/** Result of a payment: the (idempotent) payment id. */
public record PaymentResponse(UUID paymentId) {
}
