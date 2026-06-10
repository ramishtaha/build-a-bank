# 💼 Interview Bank — Build-a-Bank

A cumulative, job-prep payoff file: every step's **💼 Interview Prep** questions land here so you can revise the whole course's interview surface in one place. Concise model answers; the 🌟 marks the most commonly asked. Grouped by step; skim the headers to find a topic.

> Pairs with `docs/glossary.md` (the term definitions) and `docs/flashcards.md` (spaced-repetition Q/A).

---

## Step 11 — Concurrency & Thread Safety in Java

1. 🌟 **"Is `i++` atomic? Why not?"** *(the classic)* — No. `i++` (and `balance += amount`) is a **read-modify-write**: read the field, add, write it back — three operations, not one. Two threads can both read the same old value, both add, and both write back the same result, so one update is silently lost. Make it atomic with `AtomicInteger.incrementAndGet`, a `synchronized` block / `Lock`, or `LongAdder` for hot counters.

2. 🌟 **"`volatile` vs `synchronized`?"** *(gotcha)* — `volatile` gives **visibility + ordering** (a write is immediately visible to later reads; it's a memory barrier so reads/writes aren't reordered across it) and makes single reads/writes of the field atomic *even for `long`/`double`* — but it does **not** make a compound action atomic, so `volatileCounter++` still loses updates. `synchronized` gives **mutual exclusion + visibility + ordering** across a whole block. Rule: `volatile` for a one-writer flag (e.g. a `stop` signal); `synchronized` when you need atomicity across multiple steps or fields.

3. **"What is happens-before, and what creates it?"** — A guarantee that one action's writes are **visible to** and **ordered before** another action. You never get it for free; specific constructs create the edges: monitor **unlock → lock** of the *same* monitor, **`volatile` write → read** of the same field, **`Thread.start()`** → the started thread's first action, a thread's actions → another thread's successful **`join()`**, and **`final`-field** publication of a correctly constructed object.

4. **"`AtomicLong` vs `synchronized` vs `LongAdder` — when each?"** *(applied)* — `AtomicLong` (lock-free CAS) for a single counter at low/medium contention — no blocking, just occasional retries. `LongAdder` for a *hot* counter at high contention — it stripes the count across cells so threads usually hit different memory (no CAS contention / false sharing), and `sum()` aggregates. `synchronized`/`Lock` when you must update **multiple fields** atomically (an invariant a single CAS can't express).

5. 🌟 **"What are virtual threads, and what do they change?"** *(version-evolution)* — Lightweight threads (stable Java 21, JEP 444) scheduled by the JVM onto a small pool of **carrier** platform threads. When a virtual thread **blocks** (I/O, `sleep`, most `j.u.c` waits) it **unmounts** the carrier, freeing it to run another — so blocking is cheap and you can run millions, writing simple blocking code instead of reactive callbacks. They do **not** change the memory model and do **not** make racy code safe; you no longer pool them (one thread per task).

6. **"How would you prevent two simultaneous withdrawals from overdrawing an account?"** *(concurrency + money — the Step 12 setup)* — Make check-and-debit **atomic**. Inside one JVM: a lock (`synchronized`/`ReentrantLock`) or an atomic compound op. Across instances (the real bank): push the atomicity to the database — a pessimistic row lock (`SELECT … FOR UPDATE`, Step 10), an optimistic `@Version` with retry (Step 9), or `SERIALIZABLE` + retry — or a distributed lock. In-JVM `synchronized` alone only works with a single instance.

> **Behavioral / STAR seed:** *"Tell me about a hard bug you debugged."* — A race that only failed under load. **S/T:** intermittent balance corruption in prod, green on the laptop. **A:** reproduce it *deterministically* (a `CyclicBarrier` that forces the interleaving) so it fails every run, fix with the right primitive, add a stress test. **R:** regression-proof, and a reusable pattern for the team.

---

## Step 12 — Demand Account, the Double-Entry Ledger & Transactions Deep

1. 🌟 **"Two withdrawals hit the same account at the same instant — what happens, and how do you make it correct?"** *(the money question)* — Without protection it's a lost-update / overdraft race (the balance is a read-check-write). Fix by making check-and-debit **atomic on the contended row**: a pessimistic row lock (`SELECT … FOR UPDATE`) to serialize, optimistic `@Version` + a retry loop, or `SERIALIZABLE` + retry. For a **hot** account, pessimistic; for **rare** conflicts, optimistic.

2. 🌟 **"Optimistic vs pessimistic locking — trade-offs?"** *(the most common)* — Optimistic (`@Version`): no lock during the read, detect the conflict at write time (the `UPDATE … WHERE version = ?` matches 0 rows → `ObjectOptimisticLockingFailureException`) and **retry**; best when conflicts are rare. Pessimistic (`SELECT … FOR UPDATE`): take a row lock at read time, others **block**; best under high contention when you must serialize. Measured here: 20/20 transfers succeed with `FOR UPDATE`; **17/20 fail** with only `@Version`.

3. **"What does `@Transactional(propagation = REQUIRES_NEW)` do, and when would you use it?"** — Suspends the caller's transaction and runs in a **brand-new** one that commits independently — so e.g. an **audit/log** row survives even when the business transaction rolls back. (The default `REQUIRED` joins the caller's transaction and rolls back with it.) It only works across a **bean boundary**: a `this.`-call is self-invocation and bypasses the proxy.

4. **"Does `@Transactional` roll back on a checked exception?"** *(gotcha)* — No. By default it rolls back only on `RuntimeException`/`Error`; **checked exceptions commit** unless you set `rollbackFor`. (That's why `InsufficientFundsException extends RuntimeException` — a failed transfer rolls back automatically.) Bonus gotcha: `@Transactional` is proxy-based, so a self-invocation (`this.method()`) starts no transaction at all.

5. 🌟 **"Why `BigDecimal` for money, not `double`?"** — `double`/`float` are binary floating point and can't represent decimals like `0.10` exactly, so cents drift over many operations. `BigDecimal` is exact decimal: store as `numeric(19,4)`, **compare with `compareTo`** (not `equals`, which is scale-sensitive: `100.00` ≠ `100.0000`), and round explicitly with a `RoundingMode`.

6. **"How do you avoid deadlocks when a transfer locks two rows?"** *(concurrency)* — Acquire the locks in a **consistent global order** (here, by account number) regardless of transfer direction, so A→B and B→A both lock the lower-numbered account first and can't form a hold-and-wait cycle. (Postgres also detects deadlocks and aborts one with `40P01`, but ordering avoids them entirely.)

7. **"Why a double-entry ledger, and how do you know the books balance?"** *(domain + design)* — Every movement is two equal entries — a DEBIT on the payer, a CREDIT on the payee — sharing one `transactionId`, on an **append-only** table. Because every debit has a matching credit, the sum of all entries (credits − debits) is always **zero**; assert `ledgerNet == 0` plus `totalSystemBalance` constant to prove correctness. The materialized `balance` is a fast-read projection kept in sync inside the same transaction (event sourcing in Step 52 makes the ledger the only source of truth).

> **Behavioral / STAR seed:** *"Tell me about a time you prevented a serious bug before production."* — The overdraft race. **S/T:** concurrent transfers could double-spend (a TOCTOU bug), green on a single thread. **A:** reproduced it *deterministically* with a `CyclicBarrier` stress test that lost an update every run, added pessimistic `SELECT … FOR UPDATE` with deadlock-safe lock ordering, and a money-conservation assertion. **R:** the test now fails-closed and the books always balance under 20-thread load.
