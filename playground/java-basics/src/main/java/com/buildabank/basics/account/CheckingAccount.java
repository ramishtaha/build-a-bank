// playground/java-basics/src/main/java/com/buildabank/basics/account/CheckingAccount.java
package com.buildabank.basics.account;

import com.buildabank.basics.money.Money;

/** A checking account with an overdraft limit. A record that implements the sealed {@link Account}. */
public record CheckingAccount(String id, String owner, Money balance, Money overdraftLimit) implements Account {
}
