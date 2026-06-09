// playground/java-basics/src/main/java/com/buildabank/basics/customer/Customer.java
package com.buildabank.basics.customer;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * A bank customer. {@link LocalDate} (a date with no time/zone) is the right type for a birth date —
 * contrast with {@code Instant} for transaction timestamps. Choosing the correct {@code java.time}
 * type for each concept is a recurring banking-correctness theme.
 */
public record Customer(Long id, String firstName, String lastName, LocalDate dateOfBirth) {

    public Customer {
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    /** Age in whole years as of {@code today}. */
    public int ageOn(LocalDate today) {
        return Period.between(dateOfBirth, today).getYears();
    }
}
