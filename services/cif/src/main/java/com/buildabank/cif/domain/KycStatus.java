// services/cif/src/main/java/com/buildabank/cif/domain/KycStatus.java
package com.buildabank.cif.domain;

/** Mock KYC (Know Your Customer) status. New customers start PENDING. */
public enum KycStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
