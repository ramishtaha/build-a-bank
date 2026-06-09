// services/cif/src/main/java/com/buildabank/cif/domain/CustomerSummary.java
package com.buildabank.cif.domain;

/**
 * A Spring Data <strong>interface projection</strong>: Spring Data implements it with a proxy and, crucially,
 * issues SQL that selects ONLY these columns — not the whole row. Use projections when a screen/endpoint
 * needs a few fields, to avoid hydrating full entities (and their lazy associations).
 */
public interface CustomerSummary {

    String getCustomerNumber();

    String getFirstName();

    String getLastName();
}
