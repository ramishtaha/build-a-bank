// playground/java-basics/src/main/java/com/buildabank/basics/account/AccountInfo.java
package com.buildabank.basics.account;

/**
 * Demonstrates <strong>pattern matching for switch</strong> over a sealed type.
 *
 * <p>No {@code default} branch: because {@link Account} is sealed and we cover both permitted types,
 * the compiler proves the switch is exhaustive. Each {@code case} binds a typed pattern variable
 * ({@code c}, {@code s}) so we can call type-specific accessors with no cast.
 */
public final class AccountInfo {

    private AccountInfo() { }

    public static String describe(Account account) {
        return switch (account) {
            case CheckingAccount c ->
                    "Checking %s (%s): balance %s, overdraft %s".formatted(c.id(), c.owner(), c.balance(), c.overdraftLimit());
            case SavingsAccount s ->
                    "Savings %s (%s): balance %s @ %s%% APR".formatted(
                            s.id(), s.owner(), s.balance(), s.annualInterestRate().movePointRight(2).toPlainString());
        };
    }
}
