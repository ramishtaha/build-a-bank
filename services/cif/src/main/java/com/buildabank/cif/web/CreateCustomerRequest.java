// services/cif/src/main/java/com/buildabank/cif/web/CreateCustomerRequest.java
package com.buildabank.cif.web;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * The request body for creating a customer, with <strong>Bean Validation</strong> constraints. When the
 * controller marks the parameter {@code @Valid}, Spring runs these checks BEFORE your code; a violation
 * yields a 400 automatically (we make the error body prettier with ProblemDetail in Step 13).
 */
public record CreateCustomerRequest(

        @NotBlank @Size(max = 100) String firstName,

        @NotBlank @Size(max = 100) String lastName,

        @NotBlank @Email @Size(max = 255) String email,

        @Past LocalDate dateOfBirth) {
}
