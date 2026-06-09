// playground/java-basics/src/main/java/com/buildabank/basics/account/SavingsAccount.java
package com.buildabank.basics.account;

import java.math.BigDecimal;

import com.buildabank.basics.money.Money;

/** A savings account that earns interest. A record that implements the sealed {@link Account}. */
public record SavingsAccount(String id, String owner, Money balance, BigDecimal annualInterestRate) implements Account {
}
