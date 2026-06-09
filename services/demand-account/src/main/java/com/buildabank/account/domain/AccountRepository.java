// services/demand-account/src/main/java/com/buildabank/account/domain/AccountRepository.java
package com.buildabank.account.domain;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /** Plain read — NO lock. Used by the deliberately-unsafe transfer to demonstrate the race. */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Read <strong>and take a pessimistic write lock</strong> on the row — Hibernate emits
     * {@code SELECT ... FOR UPDATE}, so any other transaction trying to lock the same row <em>blocks</em>
     * until we commit. This serializes concurrent transfers touching the account and is how we make the
     * read-check-write of a balance atomic at the database level (the safe transfer uses this).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    /** Sum of all account balances — used to assert money is conserved across concurrent transfers. */
    @Query("select coalesce(sum(a.balance), 0) from Account a")
    BigDecimal totalBalance();

    /**
     * <strong>DEMONSTRATION ONLY — never use for real money.</strong> A bulk JPQL update that writes an
     * <em>absolute</em> balance computed in Java. It takes NO row lock and bypasses the {@code @Version}
     * optimistic check (bulk updates don't load/version the entity), so two threads that both read the old
     * balance and both write back will lose an update. The Step-12 capstone uses this to show the race
     * <em>failing</em>, then contrasts it with the pessimistic-lock path that's correct.
     */
    @Modifying
    @Query("update Account a set a.balance = :balance where a.id = :id")
    void applyBalanceUnsafe(@Param("id") Long id, @Param("balance") BigDecimal balance);
}
