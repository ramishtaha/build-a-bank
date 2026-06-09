// services/cif/src/main/java/com/buildabank/cif/web/CustomerResponse.java
package com.buildabank.cif.web;

import java.time.Instant;
import java.time.LocalDate;

import com.buildabank.cif.domain.Customer;

/**
 * The API representation of a customer — a DTO, deliberately separate from the {@code Customer} entity so
 * we never leak the DB shape (or lazy associations) to the API. Mapping happens in {@link #from(Customer)}.
 */
public record CustomerResponse(
        Long id,
        String customerNumber,
        String firstName,
        String lastName,
        String email,
        LocalDate dateOfBirth,
        String kycStatus,
        Instant createdAt) {

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getCustomerNumber(), c.getFirstName(), c.getLastName(),
                c.getEmail(), c.getDateOfBirth(), c.getKycStatus().name(), c.getCreatedAt());
    }
}
