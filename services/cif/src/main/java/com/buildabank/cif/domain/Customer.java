// services/cif/src/main/java/com/buildabank/cif/domain/Customer.java
package com.buildabank.cif.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A bank customer (JPA entity). The table is owned by Flyway (see {@code db/migration}); Hibernate is set
 * to {@code ddl-auto=validate}, so this mapping must match the migration exactly or startup fails fast.
 *
 * <p>Design notes that recur across the bank:
 * <ul>
 *   <li>a JPA entity is a plain class (not a record) — Hibernate needs a no-arg constructor and mutable fields
 *       it can proxy;</li>
 *   <li>the enum is persisted as a STRING (readable + stable), never its ordinal;</li>
 *   <li>time is an {@link Instant} (UTC) → {@code timestamptz}.</li>
 * </ul>
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_number", nullable = false, unique = true, updatable = false)
    private String customerNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false)
    private KycStatus kycStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor (may be package-private). */
    protected Customer() {
    }

    public Customer(String customerNumber, String firstName, String lastName, String email,
                    LocalDate dateOfBirth, KycStatus kycStatus, Instant createdAt) {
        this.customerNumber = customerNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.kycStatus = kycStatus;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
