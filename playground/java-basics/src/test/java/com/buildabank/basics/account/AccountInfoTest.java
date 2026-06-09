// playground/java-basics/src/test/java/com/buildabank/basics/account/AccountInfoTest.java
package com.buildabank.basics.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.buildabank.basics.money.Money;

class AccountInfoTest {

    @Test
    void describesACheckingAccountViaPatternSwitch() {
        Account a = new CheckingAccount("CHK-1", "Ada Lovelace", Money.of("1250.00", "USD"), Money.of("500.00", "USD"));
        assertThat(AccountInfo.describe(a)).startsWith("Checking CHK-1").contains("overdraft 500.00 USD");
    }

    @Test
    void describesASavingsAccountViaPatternSwitch() {
        Account a = new SavingsAccount("SAV-1", "Ada Lovelace", Money.of("8000.00", "USD"), new BigDecimal("0.0325"));
        assertThat(AccountInfo.describe(a)).startsWith("Savings SAV-1").contains("3.25% APR");
    }
}
