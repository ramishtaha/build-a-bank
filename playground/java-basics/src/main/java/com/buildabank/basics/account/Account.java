// playground/java-basics/src/main/java/com/buildabank/basics/account/Account.java
package com.buildabank.basics.account;

import com.buildabank.basics.money.Money;

/**
 * A <strong>sealed</strong> interface: the set of account kinds is closed and known at compile time.
 *
 * <p>Sealing (Java 17+) lets the compiler verify a {@code switch} over accounts is <em>exhaustive</em>
 * (no {@code default} needed) — if we add a new permitted type, every switch that forgot it fails to
 * compile. That is type-safety the bank relies on. See {@code AccountInfo} for the pattern-match switch.
 */
public sealed interface Account permits CheckingAccount, SavingsAccount {

    String id();

    String owner();

    Money balance();
}
