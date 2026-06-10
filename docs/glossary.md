# 📖 Glossary — Build-a-Bank

A cumulative glossary: each step contributes its **Key Terms**, defined in plain words. Grouped by the step that introduces the term (a term is defined where it first matters; later steps deepen it).

> Pairs with `docs/interview-bank.md` (Q&A) and `docs/flashcards.md` (spaced repetition).

---

## Step 11 — Concurrency & Thread Safety

- **JMM (Java Memory Model)** — the JLS contract defining what one thread is guaranteed to see of another's actions. It governs three things: atomicity, visibility, and ordering.
- **Atomicity** — whether an operation happens "all at once" or can be interrupted partway by another thread. `balance += amount` is *not* atomic (it's read-modify-write).
- **Visibility** — whether a write by one thread is guaranteed to be *seen* by another. Without coordination, a write may sit in a CPU cache/register and never (or only late) become visible.
- **Ordering** — the compiler/JIT/CPU may reorder instructions for speed; another thread can observe a surprising order unless a happens-before edge forbids it.
- **happens-before** — the JMM relation that, when it holds between actions A and B, guarantees A's writes are visible to B and A is ordered before B. Created by lock unlock→lock, `volatile` write→read, `start()`, `join()`, and final-field publication.
- **race condition** — a bug whose outcome depends on the unpredictable timing/interleaving of threads.
- **lost update** — a specific race where two read-modify-write operations interleave so one update overwrites (and discards) the other.
- **`synchronized` / monitor** — Java's intrinsic lock. Entering a `synchronized` method/block acquires the object's monitor (mutual exclusion); the release→acquire pair creates a happens-before edge (visibility + ordering).
- **`volatile`** — a field modifier giving visibility + ordering (a memory barrier) and atomic single read/write, but **not** atomicity of compound actions. For flags, not counters.
- **CAS (compare-and-swap)** — a single atomic CPU instruction: "set this location to *new* only if it currently equals *expected*." The basis of lock-free algorithms; retries on conflict instead of blocking.
- **`AtomicLong` / `AtomicInteger`** — a single `volatile` value updated via a CAS loop (`addAndGet`, `incrementAndGet`). Lock-free, ideal at low/medium contention.
- **`LongAdder`** — a contention-friendly counter that stripes its value across multiple `Cell`s (threads usually hit different cells), summed on read via `sum()`. Beats `AtomicLong` under heavy write contention.
- **virtual thread** — a lightweight JVM-scheduled thread (stable Java 21) that runs many-to-few on platform carrier threads; cheap to create by the million.
- **carrier thread** — a platform (OS) thread that actually runs virtual threads; a virtual thread **mounts** a carrier to run and **unmounts** it when it blocks.
- **mount / unmount** — a virtual thread mounts onto a carrier while running and unmounts (freeing the carrier) when it blocks, which is why blocking is cheap.
- **`ExecutorService`** — a task-submission abstraction over a thread pool; `AutoCloseable` since Java 19 (its `close()` performs an orderly shutdown that *waits* for submitted tasks).
- **`CompletableFuture`** — composable asynchronous results (`thenCombine`, `thenCompose`, `allOf`) without manual thread juggling.
- **`Semaphore`** — a counting permit holder; `Semaphore(n)` lets at most `n` threads into a section at once — a clean way to bound concurrency.
- **`CyclicBarrier`** — a sync point where N threads `await()` until all arrive, then all proceed; used here to *force* a deterministic lost-update interleaving.
- **deadlock** — two+ threads each holding a lock the other needs, so none can proceed. Fixed by a global lock ordering or `tryLock` with timeout.
- **double-checked locking** — the lazy-singleton trick that is broken unless the field is `volatile` (reordering can publish a half-constructed object).
- **false sharing** — two unrelated fields on the same 64-byte cache line, causing cores to fight over the line; mitigated by `LongAdder` / `@Contended`.
- **TOCTOU (time-of-check-to-time-of-use)** — a race where state changes between checking it and acting on it (e.g. check balance, then debit); a security bug, not just a correctness one. Fix by making check-and-act atomic.

---

## Step 12 — Demand Account, the Double-Entry Ledger & Transactions

- **ACID** — the four guarantees of a database transaction: **A**tomicity (all-or-nothing), **C**onsistency (invariants hold), **I**solation (concurrent transactions don't corrupt each other), **D**urability (committed survives a crash).
- **double-entry bookkeeping** — every money movement is recorded as two entries of equal amount: a **DEBIT** on the payer and a **CREDIT** on the payee, sharing one `transactionId`. The entry log is append-only and always nets to zero, so the books always balance.
- **ledger / `LedgerEntry`** — the append-only table of entries (one leg per row). Never updated or deleted — an immutable audit trail.
- **materialized balance** — a `balance` column kept on the account and updated inside the transfer transaction, so reads are O(1); contrasted with a **derived balance** (summing the ledger on each read, O(entries)).
- **`@Transactional`** — Spring's declarative transaction boundary. A proxy opens a transaction before the method, commits on normal return, rolls back on a (runtime) exception.
- **propagation** — what happens when a transactional method calls another: `REQUIRED` (join or start), `REQUIRES_NEW` (suspend caller, run independently), `NESTED` (savepoint), plus `SUPPORTS`/`MANDATORY`/`NEVER`/`NOT_SUPPORTED`.
- **`REQUIRES_NEW`** — suspends the caller's transaction and runs in a brand-new one that commits independently; used for audit rows that must survive an outer rollback. Only takes effect across a bean boundary.
- **rollback rules** — by default `@Transactional` rolls back on `RuntimeException`/`Error` only; checked exceptions **commit** unless `rollbackFor` is set.
- **`readOnly = true`** — a transaction hint that no writes happen; Hibernate sets the flush mode to manual (skips dirty-checking) and the DB/driver can optimize.
- **self-invocation pitfall** — calling a `@Transactional` (or `@Async`/`@PreAuthorize`) method via `this.` inside the same bean bypasses the Spring proxy, so the advice never runs (Step 7).
- **optimistic locking (`@Version`)** — no lock during the read; an `@Version` column makes the `UPDATE` carry `WHERE version = ?` and `SET version = version + 1`. A stale write matches 0 rows → `ObjectOptimisticLockingFailureException`. Detect-and-retry; best for rare conflicts.
- **`ObjectOptimisticLockingFailureException`** — Spring's exception thrown when an optimistic-lock version check fails (a concurrent write bumped the version first).
- **pessimistic locking (`SELECT … FOR UPDATE`)** — take a row write-lock at read time so other transactions **block** until commit. Expressed in Spring Data as `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Lock-and-wait; best for high contention.
- **`@Lock(LockModeType.PESSIMISTIC_WRITE)`** — the Spring Data annotation that makes Hibernate emit `SELECT … FOR UPDATE` for a query method.
- **lock ordering** — acquiring multiple locks in a consistent global order (here, by account number) so concurrent operations can't form a hold-and-wait deadlock cycle (Step 11).
- **`40P01` / deadlock detection** — Postgres's error code when it detects a deadlock and aborts one transaction to break it; lock ordering avoids it entirely.
- **lost update** — two read-modify-writes interleave so one overwrites and discards the other; the failure mode the transfer lock prevents (also Step 11).
- **double-spend / TOCTOU** — spending the same balance twice via the check-then-debit race; the security framing of the lost-update bug (Step 11).
- **`BigDecimal` / minor units** — exact decimal money (`numeric(19,4)`), compared with `compareTo` (scale-insensitive), never `double`/`float`. "Minor units" = the smallest currency unit (cents) as a concept.
- **bulk `@Modifying` update** — a JPQL `UPDATE`/`DELETE` that runs directly against the table, bypassing the persistence context and `@Version` (no dirty-checking, no optimistic check) — which is why `applyBalanceUnsafe` can demonstrate a true lost update.
- **`@ServiceConnection`** — a Spring Boot 3.1+ test annotation that wires the app's `DataSource` to a running Testcontainers container automatically (no JDBC URL/credentials in test config).
- **`@Enumerated(EnumType.STRING)`** — persists an enum as its stable text name (e.g. `"DEBIT"`) rather than its ordinal, so reordering the enum can't corrupt stored rows.
