// services/cif/src/main/java/com/buildabank/cif/domain/Address.java
package com.buildabank.cif.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A customer's postal address — the "many" side of Customer (1) → Address (*).
 *
 * <p>{@code @ManyToOne(fetch = LAZY)}: the parent {@link Customer} is loaded on demand, not eagerly with
 * every address. The matching {@code @OneToMany} on Customer is also lazy — which is what creates the
 * N+1 trap this step is all about.
 */
@Entity
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private String line1;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false, length = 2)
    private String country; // ISO-3166 alpha-2

    protected Address() {
    }

    public Address(String line1, String city, String country) {
        this.line1 = line1;
        this.city = city;
        this.country = country;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    void setCustomer(Customer customer) { // package-private: set via Customer.addAddress(...)
        this.customer = customer;
    }

    public String getLine1() {
        return line1;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }
}
