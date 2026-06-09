// services/demand-account/src/main/java/com/buildabank/account/domain/EntryDirection.java
package com.buildabank.account.domain;

/** The two legs of double-entry bookkeeping. Persisted as a STRING (stable, readable), never the ordinal. */
public enum EntryDirection {
    /** Money leaving this account. */
    DEBIT,
    /** Money arriving in this account. */
    CREDIT
}
