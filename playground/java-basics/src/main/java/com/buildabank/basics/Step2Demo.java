// playground/java-basics/src/main/java/com/buildabank/basics/Step2Demo.java
package com.buildabank.basics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.buildabank.basics.account.Account;
import com.buildabank.basics.account.AccountInfo;
import com.buildabank.basics.account.CheckingAccount;
import com.buildabank.basics.account.SavingsAccount;
import com.buildabank.basics.analytics.TransactionAnalytics;
import com.buildabank.basics.customer.Customer;
import com.buildabank.basics.money.Money;
import com.buildabank.basics.repo.InMemoryCustomerRepository;
import com.buildabank.basics.txn.Transaction;
import com.buildabank.basics.txn.TransactionType;

/**
 * A runnable tour of the Step 2 concepts — run it to SEE the language features produce a tiny bank report.
 * Build then run (from the repo root):
 *   ./mvnw -pl playground/java-basics -am -q -DskipTests package
 *   java -cp playground/java-basics/target/classes com.buildabank.basics.Step2Demo
 * (or simply run main() from your IDE).
 */
public final class Step2Demo {

    public static void main(String[] args) {
        // --- records + an in-memory generic repository ---
        var customers = new InMemoryCustomerRepository();
        var ada = customers.save(new Customer(1L, "Ada", "Lovelace", java.time.LocalDate.of(1990, 5, 17)));

        // --- a sealed Account hierarchy (records) + pattern-matching switch ---
        List<Account> accounts = List.of(
                new CheckingAccount("CHK-1", ada.fullName(), Money.of("1250.00", "USD"), Money.of("500.00", "USD")),
                new SavingsAccount("SAV-1", ada.fullName(), Money.of("8000.00", "USD"), new BigDecimal("0.0325")));

        // --- transactions with Money (BigDecimal) + Instant (UTC) ---
        Instant t0 = Instant.parse("2026-06-01T09:00:00Z");
        List<Transaction> txns = List.of(
                new Transaction("T1", Money.of("2000.00", "USD"), TransactionType.CREDIT, t0),
                new Transaction("T2", Money.of("750.00", "USD"), TransactionType.DEBIT, t0.plus(1, ChronoUnit.DAYS)),
                new Transaction("T3", Money.of("125.50", "USD"), TransactionType.DEBIT, t0.plus(2, ChronoUnit.DAYS)));

        // --- text block for a header ---
        String header = """
                ============================================
                  Build-a-Bank · Step 2 · Java Primer Demo
                ============================================""";
        System.out.println(header);

        System.out.println("\nCustomer: " + ada.fullName() + " (age in 2026: " + ada.ageOn(java.time.LocalDate.of(2026, 6, 9)) + ")");

        System.out.println("\nAccounts:");
        accounts.forEach(a -> System.out.println("  - " + AccountInfo.describe(a)));

        // --- streams: counts, totals, net, largest (Optional) ---
        System.out.println("\nActivity:");
        System.out.println("  counts by type : " + TransactionAnalytics.countByType(txns));
        System.out.println("  total credits  : " + TransactionAnalytics.totalAmount(txns, TransactionType.CREDIT) + " USD");
        System.out.println("  total debits   : " + TransactionAnalytics.totalAmount(txns, TransactionType.DEBIT) + " USD");
        System.out.println("  net movement   : " + TransactionAnalytics.net(txns) + " USD");
        TransactionAnalytics.largest(txns)
                .ifPresent(t -> System.out.println("  largest txn    : " + t.id() + " = " + t.amount()));

        System.out.println("\nDone. ✅");
    }
}
