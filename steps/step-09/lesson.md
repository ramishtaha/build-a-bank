# Step 9 · Hibernate Performance & Correctness

> **Step 9 of 67 · Phase B — Data, Databases, Concurrency & Transactions 🔵** · Level badge: 🔵 Core · Effort ≈ 20h (experienced JPA devs: skip-test below, then skim) · **the step where the bank's persistence layer becomes both *fast* and *correct under concurrency*.**

`🟢` Foundations &nbsp;·&nbsp; `🔵` Core &nbsp;·&nbsp; `🟣` Advanced &nbsp;·&nbsp; `🔴` Frontier

> [!CAUTION]
> **Educational, non-production project.** Build-a-Bank is for learning only. It never handles real money, real customers, or real personal data, and it is **not** security-audited for production banking. Every credential and customer you ever see here is fake/synthetic. (Full disclaimer + guardrails in the [README](../../README.md).)

> [!WARNING]
> **🐳 Docker is REQUIRED for this step.** Every proof in Step 9 runs against a **real PostgreSQL** spun up by **Testcontainers** — the N+1 statistics, the projection, and the `@Version` optimistic-lock conflict. If `docker info` errors, start Docker Desktop (or your engine) before you begin. We do **not** trust H2 for any of this: the locking and SQL-statement counts must be observed on the real engine.

---

## 🧭 The Six Movements of This Step

A one-line map of where we're going. Click to jump.

1. **[A · 🧭 Orient](#orient)** — what "performance & correctness" means for Hibernate, the cheat card, and whether you can skip.
2. **[B · 🧠 Understand](#understand)** — the persistence context / 1st-level cache, dirty checking & flush, LAZY vs EAGER and `LazyInitializationException` (and why OSIV is off), the **N+1 problem**, **projections**, and **optimistic `@Version` locking** — no magic; plus the security lens, the OSIV version story, the Optimistic-Locking pattern, and a thread-safety note.
3. **[C · 🛠️ Build](#build)** — the heart: the `Address` entity (`@ManyToOne` lazy) → wire `@OneToMany`/`addAddress` + `@Version` into `Customer` → the Flyway `V2` migration → the `@EntityGraph` repository method + the projection → the **N+1 statistics test** (see 3 vs 1) → the **optimistic-locking test** (see the conflict). Break-it experiments throughout. Then 🎮 Play With It and the 🏁 finished result.
4. **[D · 🔬 Prove](#prove)** — the Verification Log (🔴 Full tier): the real, pasted `verify` (CIF now **10** tests), the N+1 statistics proof, the optimistic-lock conflict, the **§12.3 mutation sanity-check**, and `smoke.sh`.
5. **[E · 🎓 Apply](#apply)** — go-deeper asides, interview prep (a version-evolution question + a concurrency question), and your-turn exercises.
6. **[F · 🏆 Review](#review)** — troubleshooting (the three real failures), resources & glossary, and the recap/study notes.

---

<a id="orient"></a>

# A · 🧭 Orient

## 📋 This Step in 30 Seconds

| | |
|---|---|
| **Title** | Hibernate Performance & Correctness — make the CIF persistence layer *fast* (no N+1) and *correct under concurrency* (no lost updates) |
| **Step** | 9 of 67 · **Phase B — Data, Databases, Concurrency & Transactions** 🔵 |
| **Effort** | ≈ 20 hours focused. The payoff: you can *prove* an N+1 problem with Hibernate's own statistics and *prove* a lost-update race is rejected — both interview-defining skills. Experienced JPA devs can skip-test and skim to ~4h. |
| **What you'll run this step** | **JVM + Maven** for the build & tests; **🐳 Docker** for the tests only (Testcontainers Postgres). One command: `./mvnw -pl services/cif -am verify`. **No HTTP endpoints change this step** — the proofs are TESTS — so there's no service to start and no new `requests.http`. |
| **Buildable artifact** | The **existing** [cif](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif) module, extended: a new [Address](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/main/java/com/buildabank/cif/domain/Address.java) entity (`@ManyToOne` LAZY); [Customer](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/main/java/com/buildabank/cif/domain/Customer.java) gains a `@OneToMany addresses` (LAZY) + an `addAddress(...)` helper + a `@Version version`; a Flyway [V2__add_address_and_version.sql](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/main/resources/db/migration/V2__add_address_and_version.sql) (address table + version column); a repository `findAllWithAddresses()` (`@EntityGraph`) + a `findByKycStatus(...)` interface projection; a [CustomerSummary](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/main/java/com/buildabank/cif/domain/CustomerSummary.java) projection; and two new tests — [CustomerFetchTest](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/test/java/com/buildabank/cif/domain/CustomerFetchTest.java) (3 tests: N+1, fix, projection) and [OptimisticLockingTest](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/test/java/com/buildabank/cif/domain/OptimisticLockingTest.java). CIF goes from 6 → **10** tests. `step-09-start == step-08-end`. |
| **Verification tier** | 🔴 **Full** — this step changes a service *and* the concurrency/correctness path. `./mvnw verify` green + all **10** tests + the **N+1 proven by statistics** (3 vs 1) + the **optimistic-lock conflict proven** + the **§12.3 mutation sanity-check** (remove `@Version`, watch the lost update slip through, revert) + [smoke.sh](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/steps/step-09/smoke.sh). |
| **Depends on** | **[Step 8](../step-08/lesson.md)** (the CIF service: JPA entity, `CustomerRepository`, Flyway, `ddl-auto=validate`, `@DataJpaTest` + Testcontainers `@ServiceConnection`, and crucially **`open-in-view: false`** which we set "for reasons explained in Step 9" — this is that step). **+ Docker.** |

By the end you will be able to explain the **persistence context** (Hibernate's 1st-level cache) and **dirty checking**; say exactly *when* SQL flushes; reproduce a `LazyInitializationException` and explain why it happens *now* that OSIV is off (and why that's good); **demonstrate the N+1 SELECT problem with Hibernate statistics** and **fix it with an `@EntityGraph`**; trim over-fetching with a **DTO interface projection**; and prevent the **lost-update race** with **optimistic `@Version` locking** — proven by a test where two transactions collide and the stale one is rejected.

### ⏭️ Can You Skip This Step? (5-minute self-check)

If you can confidently do **all** of this, skim the 🕰️ OSIV history and the 🧩 Optimistic-Locking spotlight, then jump to **[Step 10 — Relational Databases Up Close](../step-10/lesson.md)**.

- [ ] I can explain the **persistence context / 1st-level cache**, **dirty checking**, and *when* Hibernate **flushes** (and that flush ≠ commit).
- [ ] I can explain **LAZY vs EAGER** fetching, what a Hibernate **proxy** is, and exactly why `LazyInitializationException` happens — and why **OSIV off** makes it surface *early* instead of hiding.
- [ ] I can describe the **N+1 SELECT problem**, *detect* it (Hibernate statistics or `show-sql`), and *fix* it with a **fetch join / `@EntityGraph`** — and say when a **projection** is better still.
- [ ] I can explain **optimistic locking with `@Version`** — the SQL it generates, what a **lost update** is, why the conflict is thrown, and how it differs from **pessimistic** (`SELECT … FOR UPDATE`) locking.

> [!TIP]
> Not 100%? Stay. "What's the N+1 problem and how do you fix it?", "optimistic vs pessimistic locking?", and "why is `LazyInitializationException` thrown?" are three of the most common JPA interview questions in existence — and the strongest answers come from someone who has *watched the statistics count go from 3 to 1* and *watched a lost update get rejected*. That's exactly what you'll do here.

## 📇 Cheat Card

> **What this step delivers (one sentence):** the CIF persistence layer becomes **fast** — the N+1 query explosion is demonstrated with Hibernate's statistics and fixed with an `@EntityGraph` (3 statements → 1) — and **correct under concurrency** — a `@Version` column makes a lost-update race throw a conflict instead of silently overwriting, all proven on a real Postgres.

**Key commands** (Windows uses `.\mvnw.cmd`; macOS/Linux/Git-Bash use `./mvnw`):

```bash
# Build CIF (and deps, -am) and run all 10 tests on a real Testcontainers Postgres:
./mvnw -pl services/cif -am verify

# Run ONLY this step's two new tests, to watch the proofs in isolation:
./mvnw -pl services/cif test -Dtest=CustomerFetchTest,OptimisticLockingTest

# One-shot proof your build matches the lesson (needs only Docker):
bash steps/step-09/smoke.sh
```

**The one headline idea — *lazy iteration fires one query per parent (N+1); an `@EntityGraph` collapses it to one join; and `@Version` turns a lost-update race into a clean conflict*:**

```mermaid
flowchart LR
    subgraph nplus1["❌ Lazy traversal — N+1"]
        q0["SELECT customers"] --> q1["SELECT addresses WHERE customer_id=1"]
        q0 --> q2["SELECT addresses WHERE customer_id=2"]
        q0 --> qd["… one per customer (×N)"]
    end
    subgraph fixed["✅ @EntityGraph — ONE query"]
        j["SELECT customers LEFT JOIN address"]
    end
```

*Alt-text: two boxes side by side. The left, labelled "Lazy traversal — N+1", shows one SELECT for customers fanning out into one SELECT-addresses-per-customer (one, two, and "one per customer ×N"). The right, labelled "@EntityGraph — ONE query", shows a single SELECT joining customers to address.*

## 🎯 Why This Matters

The two failures this step kills are the two that most reliably take real systems down. **N+1** is the silent performance killer: code that looks innocent ("just loop the customers and read their addresses") quietly fires hundreds of round-trips to the database — one request can become a thousand queries, which is both a latency disaster *and* a denial-of-service amplifier. **Lost updates** are the silent *correctness* killer: two users read the same record, both save, and the second overwrites the first with no error and no trace — exactly the bug you cannot afford near a balance or a KYC status. Interviewers probe both relentlessly ("what's N+1 and how do you fix it?", "optimistic vs pessimistic locking?"). After this step you don't just *describe* them — you've *measured* one and *defeated* the other, on a real database.

## ✅ What You'll Be Able to Do

- **Explain** the persistence context (1st-level cache), dirty checking, and the flush-vs-commit distinction.
- **Reason about** LAZY vs EAGER, Hibernate proxies, and `LazyInitializationException` — and why **OSIV off** (set in Step 8) makes lazy-outside-a-transaction fail *fast*.
- **Demonstrate** the N+1 SELECT problem with Hibernate statistics, then **fix** it with a fetch join / `@EntityGraph`.
- **Trim over-fetching** with a Spring Data **interface projection** that selects only the columns you need.
- **Prevent the lost-update race** with optimistic **`@Version`** locking, and articulate when **pessimistic** locking (Step 12) is the right tool instead.
- **Prove all of it** with tests on a real Postgres — including a mutation sanity-check that shows the locking test actually detects a lost update.

## 🧰 Before You Start

**Prerequisites**

- ✅ You finished **[Step 8](../step-08/lesson.md)** (CIF service). You have a `Customer` entity, a `CustomerRepository`, Flyway `V1`, `ddl-auto=validate`, and `@DataJpaTest` + Testcontainers working. **You also set `open-in-view: false` in `application.yml` "for reasons explained in Step 9"** — this is that step; we now cash that cheque.
- ✅ **Docker is running.** Quick check: `docker info` prints engine details (not an error).
- ✅ You have the repo at `step-09-start` (== `step-08-end`), which builds clean: `./mvnw -pl services/cif -am verify` is green with **6** tests.

**What you already learned that connects here**

- The **persistence context** was named in Step 8; here you *use* it — dirty checking, flush, the 1st-level cache, and lazy proxies all live inside it.
- **`open-in-view: false`** (Step 8) is the setting that makes `LazyInitializationException` surface early. We explain exactly why.
- **Derived queries** and `@DataJpaTest` (Step 8) extend naturally to `@EntityGraph` methods and projections.
- This is the **first concurrency-correctness tool** in the bank. It forward-references **Step 11** (the Java Memory Model) and **Step 12** (the ledger under concurrent transfers, where pessimistic locking joins the toolkit).

> **Depends on: Steps 8** (and conceptually 5–7 for the Spring/JPA fundamentals).

---

<a id="understand"></a>

# B · 🧠 Understand

## 🧠 The Big Idea

Hibernate is not a thin SQL wrapper — it maintains, for the lifetime of a transaction, a **persistence context**: an in-memory map of every entity it's managing, keyed by primary key. That single data structure explains almost everything that surprises beginners.

> **Analogy — the librarian's desk.** Imagine a librarian (the persistence context) working at a desk inside a session that lasts exactly one transaction. When you ask for book #5, she fetches it once and **keeps a copy on her desk** (the 1st-level cache); ask again and she hands you the same copy — no second trip to the stacks. While a book is on her desk she **watches it for pencil marks** (dirty checking); at closing time (**flush**) she copies every change back into the stacks as `UPDATE`s. Some books arrive as **slip-cover stand-ins** (lazy proxies) — the real contents are fetched only the moment you open them, *and only while the desk is still staffed*. The instant the librarian goes home (the transaction ends), opening a stand-in fails: there's no one to fetch the contents → **`LazyInitializationException`**. The whole step is about steering that librarian: don't make her run to the stacks once per book (N+1), tell her up front which books you'll need together (`@EntityGraph`), and stamp each book with an **edition number** so two people can't overwrite each other's edits unnoticed (`@Version`).

```mermaid
flowchart TB
    subgraph tx["A transaction (Hibernate Session)"]
        direction TB
        pc["🗂️ Persistence Context · 1st-level cache<br/>id → managed entity"]
        c1["Customer#1 (managed)"]
        c2["Customer#2 (managed)"]
        proxy["addresses → 💤 LAZY proxy<br/>(not loaded yet)"]
        pc --- c1
        pc --- c2
        c1 -. "holds" .-> proxy
    end
    find["repository.findById(1)"] -->|"miss → SELECT, then cache"| pc
    find2["findById(1) again"] -->|"HIT → no SQL"| pc
    setter["customer.setKycStatus(VERIFIED)"] -->|"marks dirty"| c1
    flush["flush (commit / query / explicit)"] -->|"dirty check → UPDATE customer SET … WHERE id=1"| db[("PostgreSQL")]
```

*Alt-text: a box labelled "A transaction (Hibernate Session)" contains the Persistence Context / 1st-level cache mapping ids to managed entities Customer#1 and Customer#2; Customer#1 holds a lazy proxy for its addresses that is not yet loaded. Arrows from outside: findById(1) misses the cache so issues a SELECT then caches; a second findById(1) hits the cache with no SQL; calling a setter marks the entity dirty; at flush time (on commit, before a query, or explicitly) Hibernate dirty-checks and emits an UPDATE to PostgreSQL.*

The five forces in that picture, named:

1. **Persistence context / 1st-level cache** — within one transaction, each entity is loaded *once* and reused. Two `findById(1)` calls = one SQL `SELECT`.
2. **Dirty checking** — Hibernate snapshots each managed entity on load; at flush it compares and auto-generates `UPDATE`s for what changed. You never write `UPDATE` for a managed entity — you just call a setter.
3. **Flush** — the moment Hibernate pushes pending changes to the DB. It happens on commit, before a query that might be affected (`AUTO` flush mode), or when you call `flush()` explicitly. **Flush is not commit** — flushed-but-uncommitted changes still roll back if the transaction aborts.
4. **Lazy fetching & proxies** — associations marked LAZY are stand-in proxies; the real query fires the first time you touch them, *if a session is still open*. If not → `LazyInitializationException`.
5. **Versioning** — a `@Version` column lets Hibernate detect that the row changed under you between read and write, turning a silent lost update into a thrown conflict.

## 🧩 Pattern Spotlight — Optimistic Locking (`@Version`)

> **Problem (lost update).** Two users load the same customer (both at `version = 0`). User A sets KYC to `VERIFIED` and saves. User B — who still holds the *stale* copy — sets KYC to `REJECTED` and saves. With no protection, B's `UPDATE` overwrites A's work and nobody is told. A's verification is **lost**. Near a balance, that's money silently created or destroyed.

> **Why optimistic locking fits.** In a bank-customer/admin workload, *conflicts are rare* — two people editing the same customer at the same millisecond is the exception, not the rule. Optimistic locking takes no database locks during the read or the user's "think time"; it only checks for a conflict at write time. That's cheap and scales well precisely *because* it assumes the optimistic case. (When conflicts are *common* or you must serialize — e.g., debiting a hot account — you switch to pessimistic locking; see the alternative.)

> **How it works (the mechanism).** Add a `@Version` column. Hibernate (a) reads it with the row, (b) on `UPDATE` adds `WHERE id = ? AND version = ?` and `SET version = version + 1`, and (c) checks the **affected-row count**. If the row's version already moved (someone committed first), the `WHERE` matches **0 rows**, Hibernate sees `rowCount == 0`, and throws — Spring translates it to `ObjectOptimisticLockingFailureException`. No lost update, no row lock held during think time.

> **Alternative — pessimistic locking (`SELECT … FOR UPDATE`, Step 12).** Take a *real* database lock on the row at read time so no one else can even read-to-write it until you commit. Correct under heavy contention and required for some ledger operations, but it holds locks (reducing concurrency) and can deadlock. **Rule of thumb:** optimistic for low-contention edits (this step); pessimistic for high-contention, must-serialize money movement (Step 12). Same goal — no lost update — different bet on how often you collide.

> **Implementation (here).** A single `@Version private long version;` on `Customer`, a `version bigint not null default 0` column in the Flyway `V2` migration, and Hibernate does the rest. The proof is `OptimisticLockingTest`.

## 🌱 Under the Hood: How It Really Works

**The N+1 problem — why a harmless loop explodes.** When you call `findAll()`, Hibernate runs **one** `SELECT` for the parents. Each `Customer.addresses` is a **lazy** collection — a proxy that holds *nothing* until touched. The instant your loop reads `customer.getAddresses()`, the proxy fires *its own* `SELECT … WHERE customer_id = ?`. So for **N** customers you pay **1 + N** queries: one for the list, then one per customer. With 2 customers that's 3; with 500 customers it's 501. The query *count* scales with your data — that's the "N+1" name. The shape Hibernate generates here:

```sql
-- lazy traversal → N+1:
select c1_0.id, c1_0.customer_number, ... from customer c1_0;                                  -- 1
select a1_0.customer_id, a1_0.id, a1_0.city, ... from address a1_0 where a1_0.customer_id=?;    -- per customer (×N)
```

**The `@EntityGraph` fix — one join.** An `@EntityGraph(attributePaths = "addresses")` tells Hibernate "for *this* query, eagerly fetch `addresses`" — it rewrites the query as a single `LEFT JOIN` and hydrates everything in one round-trip:

```sql
-- @EntityGraph → ONE query:
select c1_0.id, ..., a1_0.id, a1_0.city, ...
from customer c1_0 left join address a1_0 on c1_0.id=a1_0.customer_id;
```

Same data, **one** statement instead of `1 + N`. A JPQL `JOIN FETCH` does the same thing; `@EntityGraph` is the declarative, per-query form Spring Data gives us. Crucially, we keep the association **LAZY by default** and opt into eager loading *only on the method that needs it* — eager-everywhere would create the opposite problem (always over-fetching).

**How we *prove* the count — Hibernate statistics.** Hibernate can expose a `Statistics` object (enabled with `hibernate.generate_statistics=true`). `statistics.getPrepareStatementCount()` is the number of JDBC prepared statements executed since the last `clear()`. The test clears it, runs the traversal, and asserts the count. This is the hard, machine-checkable proof — no eyeballing logs. (`spring.jpa.show-sql=true` or `format_sql=true` *also* prints the SQL so you can *see* the explosion; the count is what we *assert*.)

**Why `entityManager.clear()` matters in the test.** After seeding, the customers and addresses sit in the persistence context (1st-level cache). If we read them back without clearing, Hibernate would serve them from cache and fire *zero* SQL — hiding the very problem we're demonstrating. `clear()` detaches everything so the reads genuinely hit the database. (This is also why the 1st-level cache, while great in production, must be controlled in tests.)

**Projections — selecting fewer columns.** A Spring Data **interface projection** (`CustomerSummary` with just three getters) makes Spring Data generate SQL that selects *only those columns* — not the whole row, and not the lazy associations. It returns proxy instances backed by that narrow result. Use it when a screen needs three fields: less data over the wire, no full-entity hydration, no accidental lazy loads.

**Optimistic locking — the affected-row trick.** With `@Version`, every `UPDATE` becomes `UPDATE customer SET …, version = version + 1 WHERE id = ? AND version = ?`. Postgres returns how many rows it changed. If another transaction already bumped the version, the `WHERE` matches nothing → `0 rows` → Hibernate throws `StaleObjectStateException`, which Spring's exception translation surfaces as `ObjectOptimisticLockingFailureException`. The genius is that it needs **no locks** during the read — it detects the conflict purely from the version number diverging.

## 🛡️ Security Lens: What Could Go Wrong

- **N+1 is a DoS amplifier.** A single endpoint that lazily loads a child per row turns *one* HTTP request into *hundreds* of database queries. An attacker (or just a popular page) can exhaust your connection pool and database CPU with very little effort — the work amplification is the weapon. Fixing N+1 isn't only about latency; it's about not handing attackers a force multiplier.
- **Projections avoid over-fetching sensitive columns.** Selecting `SELECT *` and mapping to a full entity drags every column — including ones a given screen has no business seeing (think a future `ssn`, `tax_id`, or internal risk flag) — into memory and often into serialization. A narrow projection that selects only `customer_number, first_name, last_name` is a *data-minimization* control: you cannot leak what you never loaded.
- **Lost updates are an integrity failure, not just a bug.** Imagine two concurrent updates to a customer's KYC status — or, in Step 12, to a balance. Without `@Version`, the later writer silently clobbers the earlier one and the audit trail shows a consistent-looking but *wrong* final state. Optimistic locking turns that into a detectable, rejectable event — a **correctness *and* integrity** property. (We forward-reference this to the ledger in Step 12.)

## 🕰️ Then vs. Now (How This Changed Across Versions)

| Topic | Then (the old default / way) | Now (our config & modern practice) | Why it changed |
|---|---|---|---|
| **Open-Session-in-View (OSIV)** | In Spring Boot, OSIV was historically **ON by default** — a request-scoped Hibernate session stayed open through the view/serialization layer, so lazy associations could be loaded *outside* the service transaction without error. | We set **`spring.jpa.open-in-view: false`** (Step 8). Lazy access outside a transaction now **fails fast** with `LazyInitializationException`. | OSIV *hid* N+1 and lazy-loading bugs behind an always-open session, firing surprise queries during JSON serialization. Turning it off forces you to fetch deliberately (`@EntityGraph`/projection) at the service layer. Boot still defaults OSIV on but **logs a warning**; explicitly setting it off is the senior default. |
| **Optimistic `@Version` locking** | Has existed since **JPA 1.0** — not new. The `@Version` annotation and the affected-row mechanism are stable and unchanged. | Same annotation, same mechanism. We use `jakarta.persistence.Version`. | Stable API; the only "evolution" is the package move `javax.persistence` → `jakarta.persistence` (Boot 3+). |
| **Hibernate 6 → 7** | Hibernate 6 introduced the modern SQM (Semantic Query Model) engine. | Spring Boot 4 ships a Hibernate 7 line; the APIs we use (`@EntityGraph`, `@Version`, `Statistics`, `SessionFactory.unwrap`) are **compatible and unchanged** across the 6→7 boundary. | Mostly an internal/version bump; nothing in this step's code differs. |

> [!NOTE]
> *Verify, don't guess.* The OSIV-on default + warning, and `@Version` since JPA 1.0, are long-standing facts; the `javax`→`jakarta` move landed in Spring Boot 3. The exact Hibernate line is whatever the transient dependency resolved — see `pom.xml` pins.

## 🧵 Thread-safety note

This step introduces the **bank's first concurrency-correctness tool**. `@Version` optimistic locking guards against the *lost-update* race — two transactions reading the same row and racing to write it. Note carefully what it does and doesn't cover: it protects a single logical record across *separate database transactions* (the classic web "two tabs, two saves" race); it is **not** a replacement for in-JVM thread-safety of shared mutable objects. The Java Memory Model — visibility, happens-before, `volatile`, atomics — is **Step 11**, and the harder case (a ledger debited by concurrent transfers, where you'll weigh optimistic vs **pessimistic** locking) is **Step 12**. Today's lesson is the gentle on-ramp: see one race, see it rejected, on a real database.

---

<a id="build"></a>

# C · 🛠️ Build

## 📦 Your Starting Point

You're at **`step-09-start`** (== `step-08-end`). What's green:

- The `services/cif` module builds and its **6** Step-8 tests pass on a real Testcontainers Postgres.
- `Customer` (entity), `KycStatus` (enum), `CustomerRepository` (derived queries), Flyway **`V1__create_customer.sql`**, the `@Transactional` `CustomerService`, the DTOs, and `CustomerController` all exist and work.
- `application.yml` already has `ddl-auto: validate`, **`open-in-view: false`**, and `format_sql: true`. `ContainersConfig` (the `@ServiceConnection` Postgres) already exists.

> [!NOTE]
> **No new HTTP endpoints this step — and that's intentional.** The REST API is *unchanged* from Step 8. Everything we build (an entity, a relationship, a migration, two repository methods, two test classes) is proven by **tests**, not by hitting an endpoint. So there is **no new `requests.http`** for Step 9 — the Step-8 collection still describes the live API. We'll say so again in 🎮 Play With It.

Confirm the starting point builds:

```bash
./mvnw -pl services/cif -am verify
```

✅ You should see `BUILD SUCCESS` with `Tests run: 6` for CIF. If not, fix Step 8 first.

## 🛠️ Let's Build It — Step by Step

Here's the whole map. We build **inside-out**: Address entity → Customer wiring → Flyway V2 → Repository methods → Fetch test → Locking test. We run compile between edits and verify at the end.

```mermaid
flowchart TB
    a["1 · Address entity<br/>@ManyToOne LAZY"] --> b["2 · Customer: @OneToMany addresses<br/>+ addAddress() + @Version"]
    b --> c["3 · Flyway V2<br/>address table + version column"]
    c --> d["4 · Repository: findAllWithAddresses (@EntityGraph)<br/>+ findByKycStatus (projection)"]
    d --> e["5 · CustomerFetchTest<br/>N+1 = 3 vs @EntityGraph = 1 (statistics)"]
    e --> f["6 · OptimisticLockingTest<br/>two transactions → conflict"]
```

*Alt-text: a top-to-bottom flow of the six build sub-steps: (1) the Address entity with a lazy @ManyToOne; (2) wiring a lazy @OneToMany addresses, an addAddress helper, and a @Version into Customer; (3) the Flyway V2 migration adding the address table and version column; (4) the repository's @EntityGraph findAllWithAddresses plus the findByKycStatus projection; (5) the CustomerFetchTest proving N+1 (3 statements) vs the @EntityGraph fix (1); (6) the OptimisticLockingTest proving a conflict between two transactions.*

🌳 **Files we'll touch:**

```text
services/cif/
├── src/main/java/com/buildabank/cif/domain/
│   ├── Address.java              # NEW — the "many" side, @ManyToOne LAZY
│   ├── Customer.java             # EDIT — add @OneToMany addresses, addAddress(), @Version
│   ├── CustomerRepository.java   # EDIT — add findAllWithAddresses (@EntityGraph) + findByKycStatus (projection)
│   └── CustomerSummary.java      # NEW — interface projection (3 getters)
├── src/main/resources/db/migration/
│   └── V2__add_address_and_version.sql   # NEW — address table + version column
└── src/test/java/com/buildabank/cif/domain/
    ├── CustomerFetchTest.java    # NEW — N+1 statistics proof (3 vs 1) + projection
    └── OptimisticLockingTest.java # NEW — two transactions → optimistic-lock conflict
```

🧭 *You are here: → **sub-step 1** of 6.*

---

### Sub-step 1 of 6 — The `Address` entity (`@ManyToOne` LAZY) 🧭 *(→ Address → Customer wiring → V2 → repo → fetch test → lock test)*

🎯 **Goal:** Create a new [Address](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/main/java/com/buildabank/cif/domain/Address.java) entity to represent the child side of the Customer (1) ──► Address (*) relationship. Configure the `@ManyToOne` association as `LAZY` (override default eager fetch).

📁 **Exact location:** new file → `services/cif/src/main/java/com/buildabank/cif/domain/Address.java`

⌨️ **Code:**

```java
// services/cif/src/main/java/com/buildabank/cif/domain/Address.java
package com.buildabank.cif.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A customer's postal address — the "many" side of Customer (1) → Address (*).
 *
 * <p>{@code @ManyToOne(fetch = LAZY)}: the parent {@link Customer} is loaded on demand, not eagerly with
 * every address. The matching {@code @OneToMany} on Customer is also lazy — which is what creates the
 * N+1 trap this step is all about.
 */
@Entity
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private String line1;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false, length = 2)
    private String country; // ISO-3166 alpha-2

    protected Address() {
    }

    public Address(String line1, String city, String country) {
        this.line1 = line1;
        this.city = city;
        this.country = country;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    void setCustomer(Customer customer) { // package-private: set via Customer.addAddress(...)
        this.customer = customer;
    }

    public String getLine1() {
        return line1;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }
}
```

🔍 **Line-by-line:**
- `@Entity` / `@Table(name = "address")` — Maps this class to the `address` database table (created in `V2`).
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` — Let Postgres auto-assign the primary key using its identity column strategy.
- `@ManyToOne(fetch = FetchType.LAZY)` — Links multiple address instances back to a single customer. Overrides the default EAGER fetch strategy to prevent automatic, unwanted joining whenever an address is read.
- `@JoinColumn(name = "customer_id", nullable = false)` — Configures the database foreign key column to be `customer_id` and non-nullable.
- `protected Address()` — The no-arg constructor required by Hibernate for proxying and instantiation via reflection.
- `void setCustomer(Customer customer)` — Package-private setter. Callers do not call this directly; they use `Customer.addAddress(...)` to keep both sides of the relationship synchronized.

💭 **Under the hood:** By setting `FetchType.LAZY` on `@ManyToOne`, Hibernate creates a dynamic runtime **proxy subclass** of `Customer` when loading an `Address` instance. It will not query the `customer` table unless you call `address.getCustomer().getFirstName()` or similar methods within an active session.

🔮 **Predict:** If you run the build right now, what will happen? (Hint: mappings exist but database table is missing).
<details><summary>answer</summary>Compilation will succeed. However, if we were to run the application, Hibernate's `ddl-auto=validate` routine would fail fast because the `address` table does not exist in the database yet.</details>

▶️ **Run & See:**
Let's verify the new class compiles cleanly:
```bash
./mvnw -pl services/cif compile
```
✅ **Expected output:**
```
[INFO] Scanning for projects...
[INFO] -------------------------< com.buildabank:cif >-------------------------
[INFO] Building Build-a-Bank :: Services :: CIF 0.1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
...
[INFO] --- compiler:3.14.1:compile (default-compile) @ cif ---
[INFO] Recompiling the module because of added or removed source files.
[INFO] Compiling 2 source files
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

✋ **Checkpoint:** The `Address` class compiles cleanly without errors.

💾 **Commit:**
```bash
git add services/cif/src/main/java/com/buildabank/cif/domain/Address.java
git commit -m "feat(cif): add Address entity (lazy @ManyToOne to Customer)"
```

⚠️ **Pitfall:** Default `@ManyToOne` is EAGER. Forgetting the `fetch = FetchType.LAZY` flag would silently generate joining queries for the parent `Customer` on every address retrieval, destroying read efficiency when working with lists of addresses.

---

### Sub-step 2 of 6 — Wire `@OneToMany`, `addAddress()`, and `@Version` into `Customer` 🧭 *(Address ✅ → **Customer wiring** → V2 → repo → fetch test → lock test)*

🎯 **Goal:** Edit the existing [Customer](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/main/java/com/buildabank/cif/domain/Customer.java) entity. Wire the inverse collection (`@OneToMany addresses`) with proper cascade options, create a bi-directional sync helper `addAddress()`, and add the `@Version` field to enable optimistic locking.

📁 **Exact location:** edit → `services/cif/src/main/java/com/buildabank/cif/domain/Customer.java`

⌨️ **Code (before vs after diff):**

```diff
--- services/cif/src/main/java/com/buildabank/cif/domain/Customer.java (step-08-end)
+++ services/cif/src/main/java/com/buildabank/cif/domain/Customer.java (step-09-end)
@@ -6,11 +6,14 @@
+import java.util.ArrayList;
+import java.util.List;
+
+import jakarta.persistence.CascadeType;
 import jakarta.persistence.Column;
 import jakarta.persistence.Entity;
 import jakarta.persistence.EnumType;
 import jakarta.persistence.Enumerated;
 import jakarta.persistence.GeneratedValue;
 import jakarta.persistence.GenerationType;
 import jakarta.persistence.Id;
+import jakarta.persistence.OneToMany;
 import jakarta.persistence.Table;
+import jakarta.persistence.Version;
 
@@ -62,4 +65,19 @@
+    /**
+     * Optimistic-locking version. Hibernate increments it on every update and adds
+     * {@code WHERE version = ?} to UPDATEs; a mismatch (someone else updated first) throws — no row is
+     * silently overwritten. This is how the bank prevents lost updates without locking rows.
+     */
+    @Version
+    private long version;
+
+    /**
+     * The "one" side of Customer → Address. {@code LAZY} by default: addresses are NOT loaded until touched
+     * — which is exactly what triggers the N+1 problem when you iterate many customers and read each one's
+     * addresses. Fix it with a fetch join / {@code @EntityGraph} (see the repository).
+     */
+    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
+    private List<Address> addresses = new ArrayList<>();
+
     /** JPA requires a no-arg constructor (may be package-private). */
```

⌨️ **Code (Complete updated file):**

```java
// services/cif/src/main/java/com/buildabank/cif/domain/Customer.java
package com.buildabank.cif.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A bank customer (JPA entity). The table is owned by Flyway (see {@code db/migration}); Hibernate is set
 * to {@code ddl-auto=validate}, so this mapping must match the migration exactly or startup fails fast.
 *
 * <p>Design notes that recur across the bank:
 * <ul>
 *   <li>a JPA entity is a plain class (not a record) — Hibernate needs a no-arg constructor and mutable fields
 *       it can proxy;</li>
 *   <li>the enum is persisted as a STRING (readable + stable), never its ordinal;</li>
 *   <li>time is an {@link Instant} (UTC) → {@code timestamptz}.</li>
 * </ul>
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_number", nullable = false, unique = true, updatable = false)
    private String customerNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false)
    private KycStatus kycStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Optimistic-locking version. Hibernate increments it on every update and adds
     * {@code WHERE version = ?} to UPDATEs; a mismatch (someone else updated first) throws — no row is
     * silently overwritten. This is how the bank prevents lost updates without locking rows.
     */
    @Version
    private long version;

    /**
     * The "one" side of Customer → Address. {@code LAZY} by default: addresses are NOT loaded until touched
     * — which is exactly what triggers the N+1 problem when you iterate many customers and read each one's
     * addresses. Fix it with a fetch join / {@code @EntityGraph} (see the repository).
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    /** JPA requires a no-arg constructor (may be package-private). */
    protected Customer() {
    }

    public Customer(String customerNumber, String firstName, String lastName, String email,
                    LocalDate dateOfBirth, KycStatus kycStatus, Instant createdAt) {
        this.customerNumber = customerNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.kycStatus = kycStatus;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    /** Adds an address and keeps BOTH sides of the relationship consistent (the JPA way). */
    public void addAddress(Address address) {
        addresses.add(address);
        address.setCustomer(this);
    }
}
```

🔍 **Line-by-line:**
- `import jakarta.persistence.Version;` and `OneToMany;` — Imports key annotations.
- `@Version` — Declares the field used for optimistic concurrency tracking. Placed on `version` of type `long`.
- `@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)` — Maps the 1-to-N relationship. `mappedBy` designates the child's `customer` field as the owner. `CascadeType.ALL` handles cascading operations (insert/delete). `orphanRemoval` deletes rows when removed from the collection.
- `public void addAddress(Address address)` — Synchronizes both sides of the bi-directional association. It appends the child and sets the parent on the child.

💭 **Under the hood:** When dirty checking happens, Hibernate looks for modifications on the entity. When versioning is active, it increments the version counter by 1 in memory and appends `AND version = <old_value>` to the SQL `UPDATE` statement.

🔮 **Predict:** If we run the compiler now, will it fail?
<details><summary>answer</summary>No. Java compilations are successful because both types exist and resolve. However, database validations will still fail until migration `V2` is added.</details>

▶️ **Run & See:**
Verify the changes compile:
```bash
./mvnw -pl services/cif compile
```
✅ **Expected output:**
```
[INFO] Scanning for projects...
[INFO] -------------------------< com.buildabank:cif >-------------------------
[INFO] Building Build-a-Bank :: Services :: CIF 0.1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
...
[INFO] --- compiler:3.14.1:compile (default-compile) @ cif ---
[INFO] Compiling 1 source file
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** `Customer.java` compiles successfully.

💾 **Commit:**
```bash
git add services/cif/src/main/java/com/buildabank/cif/domain/Customer.java
git commit -m "feat(cif): wire @Version and @OneToMany relationship into Customer"
```

⚠️ **Pitfall:** Adding a child directly to the collection `addresses.add(address)` without setting `address.setCustomer(this)` is a classic bug. Since the collection is a read-only mapping (`mappedBy`), Hibernate will not write the foreign key to the database, resulting in a database insert failing with a null constraint error.

---

### Sub-step 3 of 6 — The Flyway `V2` migration (address table + version column) 🧭 *(Address ✅ → Customer wiring ✅ → **V2** → repo → fetch test → lock test)*

🎯 **Goal:** Create Flyway database migration file [V2__add_address_and_version.sql](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/main/resources/db/migration/V2__add_address_and_version.sql) to add the `version` column to the `customer` table and create the `address` table with a foreign key index.

📁 **Exact location:** new file → `services/cif/src/main/resources/db/migration/V2__add_address_and_version.sql`

⌨️ **Code:**

```sql
-- services/cif/src/main/resources/db/migration/V2__add_address_and_version.sql
-- Adds optimistic-locking version to customer, and the address table (Customer 1 -> * Address).

alter table customer add column version bigint not null default 0;

create table address (
    id          bigint generated by default as identity primary key,
    customer_id bigint       not null references customer (id),
    line1       varchar(200) not null,
    city        varchar(100) not null,
    country     varchar(2)   not null
);

create index idx_address_customer on address (customer_id);
```

🔍 **Line-by-line:**
- `alter table customer add column version bigint not null default 0;` — Adds the column corresponding to the `@Version` property in our entity. Existing records are initialized to `0`.
- `customer_id bigint not null references customer (id)` — Defines the foreign key reference matching the `@JoinColumn` on the `Address` class.
- `create index idx_address_customer on address (customer_id);` — Indexes the foreign key column. Crucial for sub-step joins and avoiding sequential database table scans during lazy-loading iterations.

💭 **Under the hood:** Flyway tracks schema versioning using a metadata history table (`flyway_schema_history`). Since the app starts up, Flyway executes pending files before Hibernate's bootstrap sequence performs validation checking.

🔮 **Predict:** When you execute the maven verification command, what will happen? (Hint: The migrations will run and the schema validator will confirm the mapping is correct).
<details><summary>answer</summary>The migrations will apply and validation will pass. The 6 existing tests from Step 8 will run and succeed because the schema changes are fully backwards compatible.</details>

▶️ **Run & See:**
Let's run verify to verify the migration matches our mappings:
```bash
./mvnw -pl services/cif -am verify
```
✅ **Expected output:**
```
[INFO] Scanning for projects...
...
2026-06-12T06:25:13.163+05:30  INFO 21508 --- [cif] [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "public" to version "2 - add address and version"
2026-06-12T06:25:13.242+05:30  INFO 21508 --- [cif] [           main] o.f.core.internal.command.DbMigrate      : Successfully applied 2 migrations to schema "public", now at version v2 (execution time 00:00.041s)
...
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** The migrations run and the Step 8 tests pass, validating our schema.

💾 **Commit:**
```bash
git add services/cif/src/main/resources/db/migration/V2__add_address_and_version.sql
git commit -m "feat(cif): create Flyway V2 migration for address table and version column"
```

⚠️ **Pitfall:** Never modify a migration file that has already been pushed and applied. Flyway tracks checksum hashes for applied scripts and throws a validation error if any discrepancies are found upon startup.

---

### Sub-step 4 of 6 — Repository: `findAllWithAddresses` (`@EntityGraph`) + `findByKycStatus` (projection) 🧭 *(Address ✅ → Customer wiring ✅ → V2 ✅ → **repo** → fetch test → lock test)*

🎯 **Goal:** Update the [CustomerRepository](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/main/java/com/buildabank/cif/domain/CustomerRepository.java) class to define the query graph optimization `findAllWithAddresses` and the closed interface projection query `findByKycStatus`. Create the closed interface projection [CustomerSummary](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/main/java/com/buildabank/cif/domain/CustomerSummary.java).

📁 **Exact location:**
- Edit → `services/cif/src/main/java/com/buildabank/cif/domain/CustomerRepository.java`
- New file → `services/cif/src/main/java/com/buildabank/cif/domain/CustomerSummary.java`

⌨️ **Code (Repository diff):**

```diff
--- services/cif/src/main/java/com/buildabank/cif/domain/CustomerRepository.java (step-08-end)
+++ services/cif/src/main/java/com/buildabank/cif/domain/CustomerRepository.java (step-09-end)
@@ -3,4 +3,8 @@
+import java.util.List;
 import java.util.Optional;
 
+import org.springframework.data.jpa.repository.EntityGraph;
 import org.springframework.data.jpa.repository.JpaRepository;
+import org.springframework.data.jpa.repository.Query;
 
@@ -10,4 +14,15 @@
 public interface CustomerRepository extends JpaRepository<Customer, Long> {
 
     Optional<Customer> findByCustomerNumber(String customerNumber);
 
     boolean existsByEmail(String email);
+
+    /**
+     * Loads customers WITH their addresses in a single query (an {@code @EntityGraph} turns the lazy
+     * association into a join just for this call) — the N+1 fix.
+     */
+    @EntityGraph(attributePaths = "addresses")
+    @Query("select c from Customer c")
+    List<Customer> findAllWithAddresses();
+
+    /** Returns a lightweight {@link CustomerSummary} projection (SELECTs only the projected columns). */
+    List<CustomerSummary> findByKycStatus(KycStatus kycStatus);
 }
```

⌨️ **Code (Complete CustomerRepository.java):**

```java
// services/cif/src/main/java/com/buildabank/cif/domain/CustomerRepository.java
package com.buildabank.cif.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository. Extending {@link JpaRepository} gives CRUD + paging for free; the
 * {@code findByCustomerNumber} method is a <strong>derived query</strong> — Spring Data parses the method
 * NAME into a JPQL query at startup (no implementation needed).
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerNumber(String customerNumber);

    boolean existsByEmail(String email);

    /**
     * Loads customers WITH their addresses in a single query (an {@code @EntityGraph} turns the lazy
     * association into a join just for this call) — the N+1 fix. Contrast with calling {@code findAll()}
     * and then touching {@code getAddresses()} on each, which fires one extra query per customer.
     */
    @EntityGraph(attributePaths = "addresses")
    @Query("select c from Customer c")
    List<Customer> findAllWithAddresses();

    /** Returns a lightweight {@link CustomerSummary} projection (SELECTs only the projected columns). */
    List<CustomerSummary> findByKycStatus(KycStatus kycStatus);
}
```

⌨️ **Code (Complete CustomerSummary.java):**

```java
// services/cif/src/main/java/com/buildabank/cif/domain/CustomerSummary.java
package com.buildabank.cif.domain;

/**
 * A Spring Data <strong>interface projection</strong>: Spring Data implements it with a proxy and, crucially,
 * issues SQL that selects ONLY these columns — not the whole row. Use projections when a screen/endpoint
 * needs a few fields, to avoid hydrating full entities (and their lazy associations).
 */
public interface CustomerSummary {

    String getCustomerNumber();

    String getFirstName();

    String getLastName();
}
```

🔍 **Line-by-line:**
- `@EntityGraph(attributePaths = "addresses")` — Signals Spring Data JPA to generate a left join fetching the `addresses` relation dynamically within a single SQL statement.
- `List<CustomerSummary> findByKycStatus(...)` — Spring Data recognizes the return type is an interface projection containing only getters.
- `CustomerSummary` getters — Getter names mapping exactly to fields inside the target `Customer` entity class.

💭 **Under the hood:** By requesting `CustomerSummary` projection instances, Spring Data skips entity instantiation/caching altogether. It generates select syntax querying only the matching properties (`customer_number`, `first_name`, `last_name`), bypassing the lazy fields.

🔮 **Predict:** If we execute compile, will it compile?
<details><summary>answer</summary>Yes, everything will compile since the interfaces are properly defined. We will test the functionality next.</details>

▶️ **Run & See:**
Verify both compilation targets:
```bash
./mvnw -pl services/cif compile
```
✅ **Expected output:**
```
[INFO] Scanning for projects...
[INFO] -------------------------< com.buildabank:cif >-------------------------
[INFO] Building Build-a-Bank :: Services :: CIF 0.1.0-SNAPSHOT
...
[INFO] --- compiler:3.14.1:compile (default-compile) @ cif ---
[INFO] Compiling 2 source files
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** Both files compile without issues.

💾 **Commit:**
```bash
git add services/cif/src/main/java/com/buildabank/cif/domain/CustomerRepository.java services/cif/src/main/java/com/buildabank/cif/domain/CustomerSummary.java
git commit -m "feat(cif): declare EntityGraph fetch and interface projection in CustomerRepository"
```

⚠️ **Pitfall:** When using `@EntityGraph` or `JOIN FETCH` over multiple collections (e.g. addresses and phoneNumbers), Hibernate will throw a `MultipleBagFetchException` due to Cartesian product expansion. Handle this by using `Set` mappings or executing separate queries with batching.

---

### Sub-step 5 of 6 — `CustomerFetchTest`: prove N+1 (3) vs the fix (1) 🧭 *(Address ✅ → Customer wiring ✅ → V2 ✅ → repo ✅ → **fetch test** → lock test)*

🎯 **Goal:** Write an integration test [CustomerFetchTest](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/test/java/com/buildabank/cif/domain/CustomerFetchTest.java) to assert the statement execution counts under both default lazy traversal (which causes the N+1 problem) and our optimized `@EntityGraph` query.

📁 **Exact location:** new file → `services/cif/src/test/java/com/buildabank/cif/domain/CustomerFetchTest.java`

⌨️ **Code:**

```java
// services/cif/src/test/java/com/buildabank/cif/domain/CustomerFetchTest.java
package com.buildabank.cif.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.buildabank.cif.ContainersConfig;

/**
 * Demonstrates — with Hibernate's own statistics as the witness — the <strong>N+1 problem</strong> and its
 * fix. Two customers (3 addresses total) are seeded, then we count the SQL statements for a lazy traversal
 * vs. an {@code @EntityGraph} fetch.
 */
@DataJpaTest
@Import(ContainersConfig.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class CustomerFetchTest {

    @Autowired
    CustomerRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @BeforeEach
    void seed() {
        Customer ada = new Customer("CIF-F1", "Ada", "Lovelace", "ada.f@bank.example",
                LocalDate.of(1990, 5, 17), KycStatus.PENDING, Instant.now());
        ada.addAddress(new Address("1 Analytical Ave", "London", "GB"));
        ada.addAddress(new Address("2 Engine Way", "London", "GB"));

        Customer alan = new Customer("CIF-F2", "Alan", "Turing", "alan.f@bank.example",
                LocalDate.of(1992, 6, 23), KycStatus.PENDING, Instant.now());
        alan.addAddress(new Address("3 Bletchley Rd", "Milton Keynes", "GB"));

        repository.save(ada);
        repository.save(alan);
        entityManager.flush();
        entityManager.clear(); // detach everything so the reads below actually hit the DB
    }

    @Test
    void lazyTraversalCausesNPlusOneQueries() {
        Statistics stats = statistics();
        stats.clear();

        List<Customer> all = repository.findAll();                 // 1 query for the customers
        int addresses = all.stream().mapToInt(c -> c.getAddresses().size()).sum(); // +1 query PER customer

        assertThat(addresses).isEqualTo(3);
        // 1 (customers) + 2 (one lazy address load per customer) = 3  →  the N+1 signature
        assertThat(stats.getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    void entityGraphFetchesEverythingInOneQuery() {
        Statistics stats = statistics();
        stats.clear();

        List<Customer> all = repository.findAllWithAddresses();    // single query (addresses joined in)
        int addresses = all.stream().mapToInt(c -> c.getAddresses().size()).sum(); // no extra queries

        assertThat(addresses).isEqualTo(3);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void projectionReturnsOnlyTheSummary() {
        List<CustomerSummary> summaries = repository.findByKycStatus(KycStatus.PENDING);
        assertThat(summaries).hasSize(2);
        assertThat(summaries).allSatisfy(s -> assertThat(s.getCustomerNumber()).startsWith("CIF-F"));
    }
}
```

🔍 **Line-by-line:**
- `@DataJpaTest` — Boots the slice context containing the repository beans.
- `@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")` — Enables statement auditing.
- `entityManagerFactory.unwrap(SessionFactory.class).getStatistics()` — Accesses Hibernate statistics to record DB interaction counts.
- `entityManager.clear()` — Clears the cache so entity data must be read from the DB.
- `lazyTraversalCausesNPlusOneQueries` — Traverses lazy addresses of 2 customers, asserting that exactly 3 statements were prepared.
- `entityGraphFetchesEverythingInOneQuery` — Executes `@EntityGraph` join, asserting that exactly 1 statement was prepared.

💭 **Under the hood:** Hibernate keeps loaded entities inside its first-level cache. Clearing the cache via `entityManager.clear()` forces Hibernate to issue SQL statements to the database, enabling precise profiling of the query behavior.

🔮 **Predict:** If you run this test class, what will be the result?
<details><summary>answer</summary>The tests will pass. The lazy test will record exactly 3 statements (1 select customer + 2 select addresses). The graph test will record exactly 1 statement (left outer join).</details>

▶️ **Run & See:**
Execute the fetch test:
```bash
./mvnw -pl services/cif test -Dtest=CustomerFetchTest
```
✅ **Expected output:**
```
[INFO] Running com.buildabank.cif.domain.CustomerFetchTest
...
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** `CustomerFetchTest` passes with all 3 test cases succeeding.

💾 **Commit:**
```bash
git add services/cif/src/test/java/com/buildabank/cif/domain/CustomerFetchTest.java
git commit -m "test(cif): verify N+1 problem and entity graph fix using Hibernate statistics"
```

⚠️ **Pitfall:** If you forget `entityManager.clear()`, the test assertions will fail because Hibernate will read the entities from its cache, yielding 0 queries and causing the assertions to fail.

---

### Sub-step 6 of 6 — `OptimisticLockingTest`: two transactions → a conflict 🧭 *(Address ✅ → Customer wiring ✅ → V2 ✅ → repo ✅ → fetch test ✅ → **lock test**)*

🎯 **Goal:** Implement the integration test [OptimisticLockingTest](file:///C:/Users/ramishtaha/Desktop/Claude/build-a-bank%20-%20Antigravity/services/cif/src/test/java/com/buildabank/cif/domain/OptimisticLockingTest.java). Simulate concurrent operations by running distinct transactions using `TransactionTemplate` to prove that concurrent updates throw an `ObjectOptimisticLockingFailureException`.

📁 **Exact location:** new file → `services/cif/src/test/java/com/buildabank/cif/domain/OptimisticLockingTest.java`

⌨️ **Code:**

```java
// services/cif/src/test/java/com/buildabank/cif/domain/OptimisticLockingTest.java
package com.buildabank.cif.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.buildabank.cif.ContainersConfig;

/**
 * Proves {@code @Version} optimistic locking on a REAL Postgres: two "users" read the same row, the first
 * commits an update (version 0→1), and the second's stale update is REJECTED instead of silently lost.
 * Each step runs in its own transaction (via {@link TransactionTemplate}) to simulate concurrent users.
 */
@SpringBootTest
@Import(ContainersConfig.class)
class OptimisticLockingTest {

    @Autowired
    CustomerRepository repository;

    @Autowired
    PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    @BeforeEach
    void init() {
        tx = new TransactionTemplate(transactionManager);
    }

    @Test
    void concurrentUpdateIsRejected() {
        Long id = tx.execute(s -> repository.save(new Customer("CIF-V1", "Vera", "Version",
                "vera@bank.example", LocalDate.of(1990, 1, 1), KycStatus.PENDING, Instant.now())).getId());

        // Two users independently read the same row (each gets a detached copy at version 0).
        Customer userA = tx.execute(s -> repository.findById(id).orElseThrow());
        Customer userB = tx.execute(s -> repository.findById(id).orElseThrow());
        assertThat(userA.getVersion()).isZero();

        // User A updates and commits → version goes 0 → 1.
        tx.executeWithoutResult(s -> {
            userA.setKycStatus(KycStatus.VERIFIED);
            repository.save(userA);
        });

        // User B updates the now-stale copy (still version 0) → optimistic lock conflict, no lost update.
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            userB.setKycStatus(KycStatus.REJECTED);
            repository.save(userB);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // The winner's change stands.
        Customer current = tx.execute(s -> repository.findById(id).orElseThrow());
        assertThat(current.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(current.getVersion()).isEqualTo(1L);
    }
}
```

🔍 **Line-by-line:**
- `@SpringBootTest` — Loads the full application context.
- `TransactionTemplate tx` — Manages transaction boundaries programmatically.
- `tx.execute(...)` — Opens, runs, and commits a new transaction, ensuring entities are detached between calls.
- `assertThatThrownBy(...)` — Asserts that B's update throws `ObjectOptimisticLockingFailureException` due to its stale version.

💭 **Under the hood:** Since transaction A commits first, it increments the database row's version to `1`. When transaction B attempts to commit its update containing `WHERE version = 0`, Postgres updates `0` rows. Hibernate detects this and throws `StaleObjectStateException`, which Spring translates into the target exception.

🔮 **Predict:** If you run this test, what will happen?
<details><summary>answer</summary>The test passes. B's update throws the exception and rolls back, leaving A's update in the database at version 1.</details>

▶️ **Run & See:**
Run the locking test:
```bash
./mvnw -pl services/cif test -Dtest=OptimisticLockingTest
```
✅ **Expected output:**
```
[INFO] Running com.buildabank.cif.domain.OptimisticLockingTest
...
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

✋ **Checkpoint:** `OptimisticLockingTest` passes successfully.

💾 **Commit:**
```bash
git add services/cif/src/test/java/com/buildabank/cif/domain/OptimisticLockingTest.java
git commit -m "test(cif): verify @Version optimistic locking rejects stale updates"
```

⚠️ **Pitfall:** Executing both reads and updates within a single transaction would fail to test this scenario. Hibernate's cache would serve the same entity instance, preventing version mismatch. You must use separate transaction boundaries (`TransactionTemplate`) to simulate concurrent users.

---

## 🎮 Play With It

Step 9 changes no HTTP endpoints; the REST API matches Step 8. You can verify the behavior by running:

1. **Verify the N+1 sql profile logs:**
   Add `spring.jpa.show-sql=true` to the properties in `CustomerFetchTest.java` and run the tests to see the SQL output:
   ```bash
   ./mvnw -pl services/cif test -Dtest=CustomerFetchTest
   ```

2. **Trigger a `LazyInitializationException`:**
   Write a quick test where you load a customer entity in one transaction, close the transaction, and attempt to read its addresses. You will see:
   ```
   org.hibernate.LazyInitializationException: could not initialize proxy [com.buildabank.cif.domain.Address#1] - no Session
   ```

Verify the build matches the lesson by running:
```bash
bash steps/step-09/smoke.sh
```

## 🏁 The Finished Result

You're at `step-09-end` (== `step-10-start`). The `services/cif` module contains:
- `Address` entity mapped as LAZY.
- `Customer` entity mapped with `@Version` and `@OneToMany` addresses.
- Flyway `V2` migration adding the table and version column.
- Repository optimizations (`@EntityGraph` and interface projection).
- Integrated tests on a real Postgres (Testcontainers), taking CIF from 6 to 10 tests.

### ✅ Definition of Done (your self-check)

- [ ] You can explain the persistence context, dirty checking, flush-vs-commit, LAZY vs EAGER, and why OSIV-off makes lazy-outside-tx fail fast.
- [ ] You can *demonstrate* N+1 with statistics and fix it with `@EntityGraph` — and explain why a projection avoids the problem entirely.
- [ ] You can explain optimistic `@Version` locking, the SQL it generates, and how it differs from pessimistic locking.
- [ ] `./mvnw -pl services/cif -am verify` is **green with 10 CIF tests**, and `bash steps/step-09/smoke.sh` passes.
- [ ] You've committed and tagged `step-09-end`.

---

<a id="prove"></a>

# D · 🔬 Prove It Works — the Verification Log

> **Verification tier: 🔴 Full** — this step changes a service *and* the concurrency/correctness path. The log below is the real, pasted evidence from this machine: the full `verify` (10 CIF tests), the N+1 statistics proof, the optimistic-lock conflict, the **§12.3 mutation sanity-check**, and `smoke.sh`.

### 1 · `./mvnw -pl services/cif -am verify` — CIF now 10 tests, green

```
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.buildabank.cif.domain.CustomerFetchTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.buildabank.cif.domain.OptimisticLockingTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 2 · The N+1 proof — Hibernate statistics (the hard, machine-checked evidence)

`CustomerFetchTest` seeds **2 customers (3 addresses total)** and asserts the prepared-statement count:

- **Lazy traversal** (`findAll()` then touch each `getAddresses()`): `getPrepareStatementCount() == 3` — i.e. **1** query for the customers **+ 1 per customer** (×2) — the N+1 signature. ✅ assertion passed.
- **`@EntityGraph`** (`findAllWithAddresses()`): `getPrepareStatementCount() == 1` — a single join. ✅ assertion passed.

The Hibernate-generated SQL (confirmed in logs; with `format_sql` on it prints as `Hibernate:` blocks) — *illustrative; the count above is the hard proof:*

```sql
-- lazy traversal → N+1:
select c1_0.id, c1_0.customer_number, ... from customer c1_0;                                  -- 1
select a1_0.customer_id, a1_0.id, a1_0.city, ... from address a1_0 where a1_0.customer_id=?;    -- per customer (×N)
-- @EntityGraph → ONE query:
select c1_0.id, ..., a1_0.id, a1_0.city, ... from customer c1_0 left join address a1_0 on c1_0.id=a1_0.customer_id;
```

### 3 · The optimistic-lock conflict — proven

`OptimisticLockingTest.concurrentUpdateIsRejected` passes: two transactions read `version 0`; A commits (→ `version 1`); B's stale `UPDATE … WHERE version = 0` matches **0 rows** → `ObjectOptimisticLockingFailureException`; final state is `VERIFIED` at `version 1` (A won, no lost update). ✅

### 4 · §12.3 Mutation sanity-check — the locking test really detects lost updates

Commenting out `@Version` on `Customer` made the locking test **FAIL** — the lost update went through undetected (no exception thrown):

```
Expecting code to raise a throwable.
[ERROR] OptimisticLockingTest.concurrentUpdateIsRejected:60
[ERROR] Tests run: 1, Failures: 1
```

Then **reverted** (`@Version` restored) → green again. This proves the test isn't vacuously passing — it genuinely catches a lost update.

### 5 · `smoke.sh`

```bash
bash steps/step-09/smoke.sh
```

Builds + tests CIF on a real Postgres (Testcontainers) and prints `✅ Step 9 smoke test PASSED`.

> [!NOTE]
> **What was actually executed:** Maven + all 10 CIF tests on a real Testcontainers Postgres, the N+1 statistics assertions, the optimistic-lock conflict, the mutation sanity-check (break → fail → revert → green), and `smoke.sh`. Nothing here is claimed without a run.

---

<a id="apply"></a>

# E · 🎓 Apply

## 🚀 Go Deeper (Optional)

<details>
<summary><strong>The <code>MultipleBagFetchException</code> / cartesian-product trap</strong></summary>

`@EntityGraph` (or `JOIN FETCH`) one collection: fine. Fetch **two** `List` collections in one query and Hibernate throws `MultipleBagFetchException` — because joining two one-to-many collections produces a *cartesian product* (every address × every phone), which a `List` ("bag") can't deduplicate correctly. Fixes: use a `Set` instead of `List` for one side, fetch the second collection in a separate query (`@EntityGraph` on a second method, or `hibernate.default_batch_fetch_size`), or use `@BatchSize`. The lesson: fetch *one* collection eagerly per query; batch the rest.

</details>

<details>
<summary><strong>Batch fetching — the middle ground between N+1 and a giant join</strong></summary>

Set `hibernate.default_batch_fetch_size = 16` (or `@BatchSize(size = 16)` on the association). Now, instead of one query per parent, Hibernate loads lazy associations for *up to 16 parents at a time* with `WHERE customer_id IN (?, ?, …)`. For N=500 that's ~32 queries instead of 501 — without the cartesian-product risk of fetching multiple collections. A pragmatic default for read paths where a single join would be too wide.

</details>

<details>
<summary><strong>Flush modes & the read-then-write ordering surprise</strong></summary>

Hibernate's default `FlushMode.AUTO` flushes pending changes *before* a query that might be affected, so your query sees your own un-committed writes. `FlushMode.COMMIT` flushes only at commit — faster, but a query might not see earlier writes in the same transaction. Almost always leave it `AUTO`. Knowing this explains "why did my `UPDATE` run *before* my `SELECT` even though I wrote the `SELECT` first?"

</details>

<details>
<summary><strong>Optimistic locking on writes that don't change the entity (<code>@OptimisticLock</code> / <code>OPTIMISTIC_FORCE_INCREMENT</code>)</strong></summary>

Sometimes you must bump the version even though you only changed a *child* (e.g., adding an address must invalidate a cached parent). Hibernate's `LockModeType.OPTIMISTIC_FORCE_INCREMENT` forces a version increment on the parent. Conversely, `@OptimisticLock(excluded = true)` on a field excludes it from triggering a version bump. Niche, but it comes up when aggregate consistency spans a parent and its children.

</details>

## 💼 Interview Prep: Questions You'll Be Asked

<details>
<summary><strong>1. What is the N+1 SELECT problem, and how do you detect and fix it? ⭐ (most commonly asked)</strong></summary>

**N+1** is when loading a list of N parents fires **1** query for the parents and then **1 query per parent** to lazily load a child association — `1 + N` queries where you wanted `1`. It's silent because the code looks innocent (a loop reading a lazy collection). **Detect** it with Hibernate statistics (`hibernate.generate_statistics=true` → `getPrepareStatementCount()`), `spring.jpa.show-sql=true`, or a tool like datasource-proxy. **Fix** it with a fetch join (`JOIN FETCH`) or `@EntityGraph(attributePaths=…)` to load the children in one join — or, if you only need a few fields, a **projection** that avoids loading the association at all. Keep associations LAZY by default and opt into eager fetching per-query. For wide reads, **batch fetching** (`@BatchSize` / `default_batch_fetch_size`) is the middle ground.

</details>

<details>
<summary><strong>2. Optimistic vs pessimistic locking — when do you use each? ⭐ (concurrency)</strong></summary>

**Optimistic** (`@Version`): no DB locks during read/think-time; detect a conflict at write via `WHERE version=?` matching 0 rows → throw. Best when **conflicts are rare** (most edit screens) — cheap, scales well, but the loser must retry. **Pessimistic** (`SELECT … FOR UPDATE`, `LockModeType.PESSIMISTIC_WRITE`, Step 12): take a real row lock at read time so no one else can write until you commit. Best when **contention is high** or you must serialize (debiting a hot account) — correct under load but holds locks (less concurrency) and can deadlock. Both prevent **lost updates**; they differ in the bet on conflict frequency. In a bank: optimistic for customer/profile edits; pessimistic (or a serializable transaction / SELECT … FOR UPDATE) for ledger entries.

</details>

<details>
<summary><strong>3. Why does <code>LazyInitializationException</code> happen, and why did turning OSIV *off* make it appear? (version-evolution)</strong></summary>

A LAZY association is a proxy that loads only when touched **within an open Hibernate session**. If you touch it after the session/transaction closed, there's no session to run the query → `LazyInitializationException: could not initialize proxy - no Session`. **OSIV (Open-Session-in-View)** was historically **on by default** in Spring Boot: it kept a session open for the whole HTTP request (through view/serialization), so lazy loads "worked" even outside the service transaction — but that *hid* N+1s (queries firing during JSON serialization) and tied DB connections to request duration. We set `open-in-view: false`, so lazy-outside-transaction now **fails fast**, forcing you to fetch deliberately (`@EntityGraph`/projection) at the service layer. Modern senior default: OSIV off, fetch explicitly. (Boot still defaults it on and logs a warning.)

</details>

<details>
<summary><strong>4. What exactly is the persistence context, and what's dirty checking? (gotcha)</strong></summary>

The **persistence context** is Hibernate's per-transaction **1st-level cache**: a map of id → managed entity. Load the same id twice → one SQL `SELECT`, same instance returned. **Dirty checking**: Hibernate snapshots each managed entity on load and, at **flush**, compares current state to the snapshot and auto-generates `UPDATE`s for what changed — you never write `UPDATE` for a managed entity, just call a setter. **Flush** (push pending SQL to the DB) happens on commit, before an affecting query (AUTO mode), or on explicit `flush()` — and **flush ≠ commit** (flushed changes still roll back if the tx aborts). In tests, `clear()` detaches everything so subsequent reads hit the DB instead of the cache.

</details>

<details>
<summary><strong>5. How does <code>@Version</code> prevent a lost update at the SQL level? (applied)</strong></summary>

Hibernate adds the version to every `UPDATE`: `UPDATE customer SET ?, version = version + 1 WHERE id = ? AND version = ?` (the old version). The DB reports the affected-row count. If another transaction already incremented the version, the `WHERE` matches **0 rows** → Hibernate throws `StaleObjectStateException` → Spring's `ObjectOptimisticLockingFailureException`. No row was locked; the conflict is detected purely from the version number diverging. The losing transaction can then re-read and retry. That single extra `AND version = ?` predicate is the entire lost-update defence.

</details>

<details>
<summary><strong>6. Interface vs class (DTO) projection — what's the difference and why use one? (applied)</strong></summary>

A **projection** returns a subset of fields instead of full entities. An **interface projection** is a Java interface with getters; Spring Data backs it with a proxy and (for *closed* projections — all accessors map to properties) restricts the `SELECT` to just those columns. A **class/DTO projection** is a concrete class whose constructor parameters Spring Data binds. Use projections to (a) cut data over the wire, (b) avoid hydrating full entities and their lazy associations, and (c) practice **data minimization** — you can't leak a sensitive column you never selected. Use the full entity only when you need to *modify* it.

</details>

## 🏋️ Your Turn: Practice & Challenges

**Quick (answers hidden):**

1. With **500** customers each having addresses, how many queries does the lazy traversal fire, and the `@EntityGraph` version? <details><summary>answer</summary>Lazy: **501** (1 + 500). `@EntityGraph`: **1**. That gap is the N+1 problem, and why it's a DoS amplifier.</details>
2. Why must the fetch test call `entityManager.clear()` after seeding? <details><summary>answer</summary>So the subsequent reads hit the database, not the 1st-level cache. Without `clear()`, the entities are already managed and reads fire 0 SQL — hiding the N+1.</details>
3. You add `@Version` to an entity whose table has existing rows but **no** `version` column. What happens at startup? <details><summary>answer</summary>`ddl-auto=validate` fails fast — schema doesn't match the mapping. You must add the column via a migration first (with `default 0` so existing rows are valid), which is exactly what `V2` does.</details>

**Stretch (reference solutions in `solutions/step-09/`):**

- **A.** Add a `PhoneNumber` entity (`@ManyToOne` LAZY) and a second `@OneToMany` on `Customer`. Then write a test that tries to `@EntityGraph`-fetch *both* collections in one query — observe `MultipleBagFetchException`, then fix it (switch one to a `Set`, or batch-fetch). Prove the fix with statistics.
- **B.** Add a **class (DTO) projection** `record CustomerCard(String customerNumber, String fullName)` and a repository method that returns it via a constructor expression (`select new …`). Compare the generated SQL to the interface projection.
- **C.** Add a retry: catch `ObjectOptimisticLockingFailureException` in a small service method, re-read the entity, re-apply the change, and retry up to 3 times. Write a test proving the retry eventually succeeds when one writer keeps losing. (This is the optimistic-locking *completion* — detection plus recovery.)

---

<a id="review"></a>

# F · 🏆 Review

## 🩺 Stuck? Troubleshooting & Fixes

<details>
<summary><strong>(a) <code>LazyInitializationException: could not initialize proxy - no Session</code></strong></summary>

**Cause:** you touched a LAZY association (e.g., `customer.getAddresses()`) *after* the transaction/session that loaded the entity closed. Now that **OSIV is off** (`open-in-view: false`, Step 8), there's no request-scoped session to lazily run the query — so it fails fast (which is the point: it surfaces the bug instead of hiding a surprise query in the serialization layer).

**Fix:** fetch what you need *inside* the transaction — use `findAllWithAddresses()` (`@EntityGraph`) or a `JOIN FETCH`, or map to a **DTO/projection** within the transaction so nothing lazy escapes. Never "fix" it by turning OSIV back on.

</details>

<details>
<summary><strong>(b) Silent N+1 — the page is slow but no error</strong></summary>

**Cause:** code iterates parents and touches a lazy child per row; each touch fires a query. No exception — just `1 + N` round-trips and creeping latency.

**Detect:** enable `spring.jpa.properties.hibernate.generate_statistics=true` and assert `getPrepareStatementCount()` in a test (as `CustomerFetchTest` does), or add `spring.jpa.show-sql=true` and count the `select` blocks. **Fix:** `@EntityGraph(attributePaths=…)` / `JOIN FETCH` for the join, or a projection if you only need a few fields, or batch fetching for wide reads.

</details>

<details>
<summary><strong>(c) Windows: <code>repackage</code> fails — "Unable to rename … .jar"</strong></summary>

**Cause:** a stale `java -jar` (or `spring-boot:run`) from a previous run is still **holding the jar file open**, so Maven's `repackage` can't rename it. Classic Windows file-lock.

**Fix:** kill the lingering process, then rebuild:

```powershell
# find and stop a stray Java process holding the jar (PowerShell):
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
.\mvnw.cmd -pl services/cif -am verify
```

(On macOS/Linux: `pkill -f cif` or find the PID with `jps`.) Then re-run the build.

</details>

<details>
<summary><strong>Schema validation fails: <code>missing table [address]</code> or <code>missing column [customer.version]</code></strong></summary>

**Cause:** the `V2` migration didn't run (or its names don't match the entities), so `ddl-auto=validate` finds the schema doesn't match the mapping.

**Fix:** confirm the file is exactly `V2__add_address_and_version.sql` (two underscores) under `src/main/resources/db/migration/`, that `spring.flyway.enabled=true`, and that in `@DataJpaTest` you have `@ImportAutoConfiguration(FlywayAutoConfiguration.class)` + `@AutoConfigureTestDatabase(replace = NONE)` (otherwise the slice skips Flyway and/or swaps in H2). Column/table names must match the entity mappings.

</details>

**Reset to a known-good state:** `git checkout step-09-end -- services/cif` (or re-`git checkout step-09-end`). Run `make doctor` if anything about your toolchain feels off.

## 📚 Learn More: Resources & Glossary

**Resources** (verify versions against `VERSIONS.md`):

- Spring Data JPA reference — *EntityGraphs*, *Projections* (interface & class), and *Query Methods*.
- Hibernate User Guide — *Fetching* (lazy/eager, fetch strategies, batch fetching) and *Optimistic and pessimistic locking*.
- Vlad Mihalcea's blog — the canonical deep dives on N+1, fetch strategies, and `@Version` (search the specific topic).
- Spring Boot reference — *Open EntityManager in View* (the `spring.jpa.open-in-view` property and its default/warning).

**Glossary:**

- **Persistence context** — Hibernate's per-transaction 1st-level cache (id → managed entity).
- **Dirty checking** — auto-detecting changes to managed entities and emitting `UPDATE`s at flush.
- **Flush** — pushing pending SQL to the DB (≠ commit).
- **LAZY / EAGER** — load an association on first touch vs. immediately.
- **Proxy** — a stand-in object Hibernate loads lazily on access.
- **`LazyInitializationException`** — touching a lazy proxy after the session closed.
- **OSIV (Open-Session-in-View)** — keeping a session open for the whole request; **off** here.
- **N+1 problem** — `1 + N` queries from lazily loading a child per parent in a loop.
- **`@EntityGraph` / `JOIN FETCH`** — fetch an association in one join (the N+1 fix).
- **Projection** — returning only selected fields (interface or DTO) instead of full entities.
- **Optimistic locking / `@Version`** — detect a concurrent change via a version column at write time.
- **Lost update** — a concurrent write silently overwriting another's, with no error.
- **Pessimistic locking** — taking a real DB row lock (`SELECT … FOR UPDATE`); Step 12.

## 🏆 Recap & Study Notes

**(a) Key points**

- Hibernate maintains a **persistence context** (1st-level cache) per transaction; **dirty checking** turns setter calls into `UPDATE`s at **flush** (flush ≠ commit).
- **LAZY** associations are proxies; touching one outside an open session throws `LazyInitializationException`. **OSIV off** (Step 8) makes that fail *fast* instead of hiding surprise queries in the view layer.
- The **N+1 problem** = `1 + N` queries. Detect with Hibernate **statistics** (`getPrepareStatementCount`) or `show-sql`; fix with **`@EntityGraph`/`JOIN FETCH`** (here: 3 → 1), or sidestep with a **projection**.
- **Projections** select only the columns you need — faster *and* a data-minimization control.
- **Optimistic `@Version` locking** adds `WHERE version=?` + increment; a stale write matches **0 rows** → conflict → no **lost update**. **Pessimistic** locking (Step 12) is the high-contention alternative.

**(b) Key Terms:** persistence context · dirty checking · flush · LAZY/EAGER · proxy · `LazyInitializationException` · OSIV · N+1 · `@EntityGraph` · projection · `@Version` · lost update · pessimistic locking.

**(c) 🧠 Test Yourself**

1. Why does a lazy traversal of 2 customers fire 3 queries, and what makes it scale to 501 for 500 customers? <details><summary>answer</summary>1 query for the customers + 1 per customer (the lazy address load). It's `1 + N`, so 2 → 3 and 500 → 501. That linear growth is the N+1 problem.</details>
2. At the SQL level, how does `@Version` reject a stale update? <details><summary>answer</summary>`UPDATE … SET version = version + 1 WHERE id = ? AND version = ?`. If the row's version moved, 0 rows match → Hibernate throws `ObjectOptimisticLockingFailureException`.</details>
3. Why did setting `open-in-view: false` make `LazyInitializationException` start appearing? <details><summary>answer</summary>OSIV had kept a session open for the whole request, so lazy loads worked (and hid N+1) even outside the service transaction. Off, there's no such session, so lazy-outside-tx fails fast.</details>
4. Why must the N+1 test `clear()` the persistence context after seeding? <details><summary>answer</summary>Otherwise the seeded entities are already cached/managed and reads fire no SQL, hiding the N+1.</details>

**(d) 🔗 How This Connects**

- **Back to Step 8:** you now know *why* `open-in-view: false` was set there — this step is the payoff. The `@DataJpaTest` + `@ServiceConnection` Testcontainers setup carried straight over.
- **Forward to Step 10 (Relational DBs Up Close):** you indexed `address.customer_id` here; next you'll read `EXPLAIN`/query plans, understand isolation anomalies, and see *why* that index matters.
- **Forward to Step 11 (Concurrency & the JMM):** `@Version` guards cross-transaction races; in-JVM thread-safety (visibility, happens-before) is its own topic next.
- **Forward to Step 12 (the ledger):** optimistic vs **pessimistic** locking under concurrent transfers — where you'll choose locks per the trade-off you learned here, and see a race *fail without locking, pass with it*.

**(e) 🏆 Résumé line / interview talking point**

> *"Diagnosed and fixed the N+1 query problem using Hibernate statistics and `@EntityGraph` (proven 3 → 1 statements), added interface projections to minimize over-fetching, and prevented lost-update races with optimistic `@Version` locking — all verified against a real PostgreSQL via Testcontainers."*

**(f) ✅ You can now…**

- [ ] Explain the persistence context, dirty checking, flush-vs-commit, and lazy proxies.
- [ ] Reproduce, detect (statistics/`show-sql`), and fix an N+1 with `@EntityGraph`.
- [ ] Use an interface projection to select only the columns you need.
- [ ] Prevent a lost update with `@Version` and explain optimistic vs pessimistic locking.
- [ ] Prove all of it on a real Postgres, including a mutation sanity-check.

**(g) 🃏 Flashcards** — append these to `docs/flashcards.md` (cumulative; Anki-importable CSV optional):

```text
Q: What is the N+1 SELECT problem? | A: Loading N parents fires 1 query for them + 1 per parent to lazily load a child = 1+N queries. Fix with JOIN FETCH / @EntityGraph (or a projection).
Q: How do you detect N+1 in Hibernate? | A: hibernate.generate_statistics=true → getPrepareStatementCount(), or spring.jpa.show-sql=true and count the selects.
Q: How does @Version prevent a lost update? | A: UPDATE … SET version=version+1 WHERE id=? AND version=?; a stale version matches 0 rows → ObjectOptimisticLockingFailureException.
Q: Optimistic vs pessimistic locking? | A: Optimistic (@Version): no locks, detect conflict at write — best for rare conflicts. Pessimistic (SELECT … FOR UPDATE): real row lock at read — best for high contention. Both stop lost updates.
Q: Why does LazyInitializationException happen (OSIV off)? | A: A lazy proxy is touched after the session closed; with open-in-view:false there's no request-scoped session to load it, so it fails fast.
```

> 🔁 **Revisit in ~3 steps** (Step 12): re-derive optimistic vs pessimistic locking when the ledger is hammered by concurrent transfers.

**(h) ✍️ One-line reflection**

> What clicked: seeing the statement count go from **3 to 1**, and watching the lost update get **rejected**. What's still fuzzy for you? (Great build-in-public material: post the 3-vs-1 number and the conflict exception.)

**(i) Sign-off**

You just made the bank's persistence layer both *fast* and *correct under concurrency* — and, more importantly, you *proved* both with numbers instead of hand-waving. "Diagnosed an N+1 with statistics" and "prevented a lost update with optimistic locking" are sentences that land in interviews. Next, in **[Step 10](../step-10/lesson.md)**, you go one layer deeper: into the relational engine itself — `EXPLAIN`, indexes, isolation anomalies, and MVCC. Onward. 🏦🚀
