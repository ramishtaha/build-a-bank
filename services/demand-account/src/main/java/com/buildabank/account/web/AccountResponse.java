// services/demand-account/src/main/java/com/buildabank/account/web/AccountResponse.java
package com.buildabank.account.web;

import java.math.BigDecimal;

import com.buildabank.account.domain.Account;

/** API view of an account — a DTO, so we never leak the JPA entity (or its version) to clients. */
public record AccountResponse(String accountNumber, String currency, BigDecimal balance) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getAccountNumber(), account.getCurrency(), account.getBalance());
    }
}
