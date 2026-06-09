// services/demand-account/src/main/java/com/buildabank/account/web/ApiExceptionHandler.java
package com.buildabank.account.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.buildabank.account.domain.InsufficientFundsException;

/**
 * Minimal error mapping so business failures return sensible HTTP codes (not 500). The full
 * {@code ProblemDetail}/RFC 9457 treatment arrives in Step 13 — this is just enough to make the API usable.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Overdraw attempt → 422 Unprocessable Entity (the request was well-formed but can't be fulfilled). */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, String>> insufficientFunds(InsufficientFundsException e) {
        return ResponseEntity.unprocessableEntity().body(Map.of("error", "insufficient_funds", "detail", e.getMessage()));
    }

    /** Unknown account / same-account transfer → 400 Bad Request. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "bad_request", "detail", e.getMessage()));
    }
}
