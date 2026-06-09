// services/demand-account/src/main/java/com/buildabank/account/web/TransferResponse.java
package com.buildabank.account.web;

import java.util.UUID;

/** Returned after a successful transfer — the shared transaction id of the two ledger legs. */
public record TransferResponse(UUID transactionId) {
}
