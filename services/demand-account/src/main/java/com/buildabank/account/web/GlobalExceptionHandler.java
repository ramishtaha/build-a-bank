// services/demand-account/src/main/java/com/buildabank/account/web/GlobalExceptionHandler.java
package com.buildabank.account.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.buildabank.account.domain.InsufficientFundsException;

/**
 * Centralized error handling that returns <strong>RFC 9457 Problem Details</strong> (the standard
 * {@code application/problem+json} shape: {@code type}, {@code title}, {@code status}, {@code detail}, plus
 * custom members). Extending {@link ResponseEntityExceptionHandler} means Spring's built-in MVC exceptions
 * (e.g. validation, unreadable body) are already turned into {@code ProblemDetail}; we override
 * {@link #handleMethodArgumentNotValid} to attach the per-field errors, and add handlers for our domain
 * exceptions. Returning a {@link ProblemDetail} from an {@code @ExceptionHandler} makes Spring set the HTTP
 * status from it and serialize it as {@code application/problem+json}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String PROBLEM_BASE = "https://buildabank.example/problems/";

    /** Overdraw attempt → 422 Unprocessable Entity. */
    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFunds(InsufficientFundsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Insufficient funds");
        problem.setType(URI.create(PROBLEM_BASE + "insufficient-funds"));
        return problem;
    }

    /** Unknown account / same-account transfer → 400 Bad Request. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid request");
        problem.setType(URI.create(PROBLEM_BASE + "invalid-request"));
        return problem;
    }

    /** Bean Validation failures → 400 with a per-field {@code errors} map added to the Problem Detail. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Validation failed");
        problem.setType(URI.create(PROBLEM_BASE + "validation"));
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String message = fieldError.getDefaultMessage();
            errors.putIfAbsent(fieldError.getField(), message == null ? "invalid" : message);
        }
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }
}
