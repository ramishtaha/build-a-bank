// services/demand-account/src/main/java/com/buildabank/account/client/CifCustomer.java
package com.buildabank.account.client;

/** The slice of a CIF customer this service cares about (a client-side DTO, decoupled from CIF's entity). */
public record CifCustomer(String customerNumber, String firstName, String lastName, String kycStatus) {
}
