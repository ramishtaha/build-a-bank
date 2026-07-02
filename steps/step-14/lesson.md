# Step 14 · API Design, Versioning, Idempotency & Webhooks
### Phase C — Web, APIs & Application Security 🔵 · Step 14 of 67

> *A toy API returns JSON. A **professional** API is a durable contract: it versions without breaking
> clients, is safe to retry (idempotency), pages large results predictably, and notifies partners with
> **signed** webhooks they can trust. This step turns demand-account's endpoints into exactly that — and
> every piece is the kind of thing payment APIs (Stripe, Adyen) get judged on.*

---

<a id="toc"></a>
## 🧭 The Six Movements of This Step

| | Movement | What happens | ~time |
|---|---|---|---|
| **A** | [🧭 Orient](#orient) | 30-second overview · skip-test · cheat card · why it matters · before you start | ~0.5h |
| **B** | [🧠 Understand](#understand) | versioning · idempotency · pagination · webhook signing/delivery | ~2.5h |
| **C** | [🛠️ Build](#build) | versioned + idempotent transfers, paginated entries, signed outbound webhooks | ~13h |
| **D** | [🔬 Prove](#prove) | the Verification Log — 25 tests, idempotent retry, signed delivery + replay, §12.3 mutation | ~1h |
| **E** | [🎓 Apply](#apply) | go deeper · interview prep · your-turn challenges | ~2h |
| **F** | [🏆 Review](#review) | troubleshooting · resources · recap, flashcards & what's next | ~1h |

---

<a id="orient"></a>

# A · 🧭 Orient

## 📋 This Step in 30 Seconds

| | |
|---|---|
| **Title** | API design — URI versioning & deprecation, public-API idempotency, pagination, and signed outbound webhooks |
| **Step** | 14 of 67 · **Phase C — Web, APIs & Application Security** 🔵 |
| **Effort** | ≈ 20 hours focused. Idempotency and webhook signing are the parts of "design a payments API" interviews that separate seniors from juniors — and you'll have built and tested both. Experienced API designers can skim to ~4h. |
| **What you'll run this step** | **JVM + Maven** for build & tests; **🐳 Docker** for the Testcontainers Postgres. One command: `./mvnw -pl services/demand-account -am verify`. (Webhook delivery is tested with an in-process HTTP receiver — no external service needed.) |
| **Buildable artifact** | `services/demand-account` gains a **`/api/v1`** namespace: an **idempotent** `POST /api/v1/transfers` (`Idempotency-Key` header + a key store) that emits a **signed webhook**, a **paginated** `GET /api/v1/accounts/{n}/entries`, and `Deprecation`/`Sunset`/`Link` headers on the old transfer. New: `IdempotencyRecord`, `IdempotentTransferService`, `WebhookSigner`/`WebhookSender`/`WebhookPublisher`, `PageResponse`. 13 → **25** tests. `step-14-start == step-13-end`. |
| **Verification tier** | 🔴 **Full** — changes a service *and* the idempotency/security path. `./mvnw verify` green + all **25** tests + idempotent retry proven (money moves once) + signed webhook delivered & verified + replay rejected + the **§12.3 mutation** (remove replay protection → test fails → revert) + clean-room + `smoke.sh`. |
| **Depends on** | **[Step 13](../step-13/lesson.md)** (the MVC layer + ProblemDetail), **[Step 12](../step-12/lesson.md)** (transfers), **[Step 10](../step-10/lesson.md)** (unique constraints for the idempotency guard). **+ Docker.** |

By the end you will be able to choose and justify an **API versioning** strategy and deprecate gracefully; make a money endpoint **idempotent** so clients can safely retry; **paginate** with a stable contract; and **sign & deliver outbound webhooks** with replay protection and retries — and explain why receivers must be idempotent.

### ⏭️ Can You Skip This Step? (5-minute self-check)

If you can confidently do **all** of this, skim the 🧩 Pattern Spotlight and jump to **[Step 15 — API Gateway / BFF](../step-15/lesson.md)**.

- [ ] I can compare **URI vs header vs media-type** versioning and deprecate an endpoint with `Deprecation`/`Sunset` headers.
- [ ] I can implement **idempotency** with an `Idempotency-Key` + a store, and explain how the unique key guards concurrent duplicates.
- [ ] I can design **pagination** (`page`/`size`/`sort`) with a stable response envelope (and say why not to leak `Page`).
- [ ] I can **sign a webhook** (HMAC-SHA256 over timestamp+body), verify it, and add **replay protection**.
- [ ] I can explain webhook **at-least-once delivery** + retries → why receivers must be **idempotent**, and the **dual-write** problem (→ Outbox, Step 20).

> [!TIP]
> Not 100%? Stay. "How do you make a payment API safe to retry?", "how would you secure a webhook?", and "how do you version an API without breaking clients?" are exactly the questions a fintech interview asks — and you'll answer them having built and *tested* all three.

## 📇 Cheat Card

> **What this step delivers (one sentence):** demand-account's API becomes partner-grade — versioned (`/api/v1`) with graceful deprecation, **idempotent** transfers (safe to retry), **paginated** listings with a stable envelope, and **HMAC-signed** outbound webhooks with replay protection and retries — all test-proven.

**Key commands** (Windows uses `.\mvnw.cmd`):

```bash
# Build + test (25 tests) on a real Testcontainers Postgres:
./mvnw -pl services/demand-account -am verify

# Just the idempotency / webhook proofs:
./mvnw -pl services/demand-account test -Dtest=IdempotencyTest,WebhookSignerTest,WebhookDeliveryTest

# One-shot proof your build matches the lesson (needs Docker):
bash steps/step-14/smoke.sh
```

**The one headline idea — *an `Idempotency-Key` makes a retried transfer move money once; a webhook signature lets a partner trust the event*:**

```mermaid
flowchart TB
    c["client POST /api/v1/transfers<br/>Idempotency-Key: K"] --> chk{"key K seen?"}
    chk -- "yes" --> ret["return stored transactionId<br/>(no money moves)"]
    chk -- "no" --> tx["transfer + store K→txId<br/>(one transaction)"]
    tx --> wh["sign event (HMAC over ts.body) → POST to partner"]
    wh --> retry["retry w/ backoff if it fails<br/>(at-least-once → receiver idempotent)"]
```

*Alt-text: a client POSTs a transfer with an Idempotency-Key; if the key was seen, the stored transactionId is returned and no money moves; otherwise the transfer runs and the key→txId is stored in one transaction, then a signed webhook (HMAC over timestamp.body) is POSTed to the partner with retries (at-least-once, so the receiver must be idempotent).*

## 🎯 Why This Matters

Money APIs live or die on three properties this step builds. **Idempotency** is non-negotiable: networks time out, clients retry, and without an idempotency key a retry charges twice — the canonical fintech disaster. **Webhook signing** is how a partner knows an event truly came from you and wasn't forged or replayed — get it wrong and you've built an injection point. **Versioning** is how an API survives years of change without a flag-day that breaks every client. Interviewers probe all three ("make this safe to retry", "secure this webhook", "version this without breaking anyone"), and after this step you answer from having built and tested them on a real ledger.

## ✅ What You'll Be Able to Do

- **Version & deprecate** — `/api/v1` namespace; `Deprecation`/`Sunset`/`Link` headers to retire old paths gracefully.
- **Make it idempotent** — `Idempotency-Key` + a key store; a retry returns the original result and moves money once.
- **Paginate** — `page`/`size`/`sort` with a stable `PageResponse` envelope.
- **Sign & deliver webhooks** — HMAC-SHA256 + timestamp, replay protection, bounded retries; explain at-least-once + idempotent receivers.

## 🧰 Before You Start

**Prerequisites**

- ✅ You finished **Step 13**; the repo is at `step-14-start` (== `step-13-end`) and `./mvnw verify` is green.
- ✅ **Docker is running.** No new dependencies this step (HMAC via `javax.crypto`, delivery via the JDK `HttpClient`, pagination via Spring Data).

**What you already learned that connects here**

- **Step 13**: the MVC layer, ProblemDetail, headers — we add versioned endpoints and more response headers.
- **Step 12**: the transfer + ledger we now wrap with idempotency and emit events for.
- **Step 10**: unique constraints — the idempotency key's PRIMARY KEY is the concurrency guard.
- **Step 11**: thread-safety — the unique key resolves the concurrent-duplicate race at the database.

> **Depends on: Steps 13, 12, 10.**

## 🗓️ Session Plan

≈20 hours won't fit one sitting. Eight sittings of ~2–3h, each ending at a real commit or movement boundary — stop at the end of any sitting and you resume clean:

| Sitting | Covers | ~time | Ends at |
|---|---|---|---|
| **S1 · The contract** | A · Orient + B · Understand (Big Idea → Thread-safety note) | ~3h | the B/C boundary — nothing typed yet |
| **S2 · Key store + service** | Sub-steps 0–1 (V2 migration, `IdempotencyRecord`, `IdempotentTransferService`) | ~2.5h | sub-step 1 commit |
| **S3 · Pagination** | Sub-step 2 (paged finder, `PageResponse`, `LedgerEntryResponse`) | ~2h | sub-step 2 commit |
| **S4 · The signer** | Sub-step 3 (`WebhookSigner` + `WebhookSignerTest`) | ~2.5h | sub-step 3 commit (first green webhook tests) |
| **S5 · Sender + publisher** | Sub-step 4 (`WebhookSender` retry, `WebhookPublisher`, `WebhookDeliveryTest`) | ~2.5h | sub-step 4 commit |
| **S6 · The v1 controller** | Sub-step 5 (`/api/v1` endpoints + deprecation) + 🎮 Play With It | ~2.5h | sub-step 5 commit |
| **S7 · Tests + proof** | Sub-step 6 (`IdempotencyTest` + test updates) + D · Prove | ~2.5h | 25 green tests · Definition of Done |
| **S8 · Apply + Review** | E · Apply (go deeper, interview prep, challenges) + F · Review | ~3h | flashcards + sign-off |

*Optional routes:* the ⏭️ skip-test above costs ~5 min (experienced API designers who pass it can compress S1–S7 to ~4h); each 🚀 Go Deeper aside is +~10 min; 🎮 Play With It's live webhook demo is +~10 min.

---

<a id="understand"></a>

# B · 🧠 Understand

## 🧠 The Big Idea

Four design concerns, each a contract between you and your clients:

**1 — Versioning.** APIs change. A **version** lets a client pin to a shape that won't break under them. Three strategies: **URI** (`/api/v1/...` — visible, cacheable, curl-able), **header** (`Accept: …v1+json` or `X-API-Version` — clean URLs, invisible), **media-type** (most RESTful, heaviest). When you retire an endpoint, you don't just delete it — you mark it **deprecated** (`Deprecation: true`), announce a removal date (`Sunset`), and point at the successor (`Link`), giving clients time to migrate.

**2 — Idempotency.** An operation is idempotent if doing it twice has the same effect as once. `GET`/`PUT`/`DELETE` are naturally idempotent; `POST` (create a transfer) is **not** — a retry creates a second transfer. The fix is an **`Idempotency-Key`**: the client sends a unique key per logical operation; the server remembers `key → result` and, on a retry with the same key, returns the original result without re-executing. This is what makes a payment API safe to retry after a timeout.

**3 — Pagination.** You never return "all rows" — that's an unbounded response and a DoS waiting to happen. Clients ask for a **page** (`page`, `size`) and an order (`sort`); you return that slice plus metadata (total count, total pages). And you return it in a **stable envelope you own** — not a framework's internal object whose JSON shape can change under you.

**4 — Webhooks.** Instead of partners polling you, you **push** events to their URL (an outbound HTTP `POST`). But the receiver must trust it: you **sign** each delivery with a shared secret (HMAC) so they can verify authenticity and integrity, include a **timestamp** so they can reject **replays**, and **retry** on failure — which means delivery is **at-least-once**, so receivers must be **idempotent** (they may see the same event twice — there's that word again).

> **Analogy — a bank's correspondence.** **Versioning** is like keeping the old account-form (v1) valid while introducing v2, stamping the old one "discontinued after October — use the new form." **Idempotency** is the reference number on a wire instruction: send the same instruction twice with the same reference and the bank executes it once. **Pagination** is getting your statement 20 transactions to a page, not the whole history in one envelope. A **signed webhook** is a letter with a **wax seal** (HMAC) and a **date** — the recipient checks the seal is yours and the date is recent (not a letter someone copied and re-sent months later).

```mermaid
flowchart LR
    subgraph v1["/api/v1 (versioned)"]
        t["POST /transfers (Idempotency-Key)"]
        e["GET /accounts/{n}/entries (page,size,sort)"]
    end
    old["POST /api/transfers (deprecated)"] -.->|"Deprecation / Sunset / Link"| t
    t --> store[("idempotency_key: key→txId")]
    t --> hook["WebhookPublisher → sign(HMAC) → POST partner (retry)"]
```

*Alt-text: the /api/v1 namespace holds an idempotent transfers endpoint and a paginated entries endpoint. The old /api/transfers is deprecated and points (via Deprecation/Sunset/Link headers) at the v1 successor. The idempotent transfer consults an idempotency_key store (key→transactionId) and, on success, publishes a signed webhook to the partner with retries.*

## 🧩 Pattern Spotlight — The Idempotency Key

> **Problem.** A client `POST`s a transfer, the network times out before the response arrives, and the client retries. Without protection, that's **two** transfers — a double-charge. The client can't tell "the request failed" from "the response was lost".

> **Why an idempotency key fits.** The client generates a unique key per logical operation (a UUID) and sends it as `Idempotency-Key` on every attempt. The server records `key → result` the first time and, on any retry with that key, returns the **stored** result without re-executing. The client can now retry freely — exactly once is guaranteed by the server, not by hope.

> **How it works (the mechanism).** A table keyed by the idempotency key (PRIMARY KEY). On a request: look up the key → if present, return its stored `transactionId`; else execute the transfer, store `key → transactionId`, return. The **PK uniqueness is the concurrency guard**: if two retries race and both miss the lookup, both transfer, but only one can commit the key row — the other's commit fails the unique constraint and its whole transaction (including its transfer) **rolls back**. So even under concurrency, exactly one transfer commits.

> **Alternatives / trade-offs.** A natural idempotency key (e.g. a client-supplied `transferId` as the resource id with `PUT`) makes the operation idempotent by REST design — cleaner, but requires the client to own the id. A request **hash** (dedupe identical bodies) avoids a client key but mis-fires on legitimately-repeated identical operations. The explicit `Idempotency-Key` header (Stripe's model) is the industry standard for `POST`-create money operations — *chosen here*. Keys need a **TTL** in production (we note it; a cleanup job comes later).

> **Implementation (here).** `IdempotencyRecord` (key PK), `IdempotentTransferService` (lookup-or-execute-and-store in one transaction), and `POST /api/v1/transfers` reading the `Idempotency-Key` header. `IdempotencyTest` proves a retry moves money once.

❓ **Quick check:** the service does a lookup before executing — so why is the PRIMARY KEY (not the lookup) the real concurrency guard? <details><summary>answer</summary>Two racing duplicates can *both* miss the lookup (neither has committed yet). Only the PK's uniqueness stops the second insert — one commits, the other's whole transaction rolls back.</details>

## 🌱 Under the Hood: How It Really Works

#### Versioning & deprecation headers

`/api/v1/...` is just a path prefix mapped by `@RequestMapping`. Deprecation uses standard headers: `Sunset: <HTTP-date>` (**RFC 8594** — when it'll be removed), `Deprecation` (**RFC 9745**, 2025 — it's deprecated; the standardized value is a structured-field date like `Deprecation: @1761955199`, while `Deprecation: true` is the older draft form still widely seen in the wild — and what our code sends, since most tooling recognizes it), and a `Link: </api/v1/transfers>; rel="successor-version"` pointing at the replacement. Clients (and gateways, Step 15) can detect these and warn/migrate. You keep the old endpoint working until the sunset date — **never** a flag-day break. The three headers on our old endpoint:

```
Deprecation: true
Sunset: Sat, 31 Oct 2026 23:59:59 GMT
Link: </api/v1/transfers>; rel="successor-version"
```

#### Idempotency under concurrency

Our `IdempotentTransferService.transfer(...)` runs `@Transactional`: it joins (REQUIRED) the transfer's transaction, so the key-insert and the transfer commit **atomically**. Sequential retry (the common case — client retries after a timeout): the second call finds the stored record and returns its `transactionId` with no re-execution. Concurrent duplicates (both miss the lookup): both attempt the transfer + key-insert in **separate** transactions; the PRIMARY KEY lets only one commit — the other gets a unique-constraint violation and its transaction rolls back entirely (no double transfer). The DB is the coordination point (as in Step 12's pessimistic lock). The racing-duplicates outcome in two lines:

```
request 1: lookup miss → transfer → insert key → COMMIT ✅ (returns txId)
request 2: lookup miss → transfer → insert key ✗ unique violation → ROLLBACK (no money moved)
```

**What does the losing client see?** In this implementation the unique violation surfaces as an **error** (HTTP 500) — not the stored `transactionId`. Its *next* retry then hits the stored key and gets the original result, so no money is ever doubled — but "a retry returns the stored result" only holds sequentially. Production hardening: catch the violation, re-read the key in a fresh transaction, and return the stored `transactionId` (or a 409) — see 🚀 Go Deeper ②.

#### Pagination with Spring Data

A controller parameter of type `Pageable` is bound from `?page=&size=&sort=field,dir` by Spring Data's `PageableHandlerMethodArgumentResolver` (auto-configured). The repository method `Page<LedgerEntry> findByAccountId(Long, Pageable)` runs a `LIMIT/OFFSET` query **plus** a `COUNT` for the total. We map the `Page` into our own **`PageResponse`** record — *not* serialize Spring Data's `Page` directly, whose JSON shape is an internal detail Spring explicitly warns against exposing (it even logs a warning). Owning the envelope means the API contract is ours.

#### Webhook signing (HMAC-SHA256)

The whole scheme is one line:

```
signature = HMAC_SHA256(secret, timestamp + "." + body)   → hex-encoded
```

Only someone with the shared `secret` can produce a signature that matches a given `(timestamp, body)`. The receiver recomputes it and compares — in **constant time** (`MessageDigest.isEqual`) so an attacker can't learn, from response timing, how many leading bytes of a guessed signature were right. Including the **timestamp** in the signed material and rejecting timestamps outside a tolerance window (e.g. ±300s) gives **replay protection**: a captured-but-still-valid request can't be re-sent hours later. This is precisely Stripe's/GitHub's webhook scheme.

#### Delivery is at-least-once

Networks and receivers fail, so `WebhookSender` **retries** (bounded, with backoff). That means a receiver might get the **same event twice** → receivers must be **idempotent** (dedupe by event id). We send *after* the DB transaction commits — which exposes the **dual-write problem**: if the commit succeeds but the send permanently fails, the partner never hears; if we sent inside the transaction and the transaction rolled back, we'd have lied. The correct fix is the **Outbox pattern** (persist the event in the same transaction as the transfer; a separate process delivers it) — that's **Step 20**. We flag this honestly rather than pretend a direct send is complete.

❓ **Quick check:** your retry logic works perfectly — so why must the *partner* still dedupe by event id? <details><summary>answer</summary>A retry can fire after the partner processed the event but before their 200 reached you (lost response). At-least-once delivery means duplicates are possible even when nothing is buggy.</details>

#### Spring Boot 4 & Jackson (a real gotcha)

Boot 4's web stack defaults to **Jackson 3** (package `tools.jackson`), so a Jackson-2 `com.fasterxml.jackson.databind.ObjectMapper` **bean** isn't auto-created — injecting one fails (the class is still on the classpath, so code compiles, then the context fails to start). Our `WebhookPublisher` therefore **owns** a `new ObjectMapper()` instead of injecting one. (See 🩺.)

## 🛡️ Security Lens: What Could Go Wrong

- **No idempotency = double-spend.** A retried `POST` without a key moves money twice — both a correctness *and* a fraud/financial-loss issue. The key turns "I'm not sure if it went through" into a safe retry.
- **Unsigned/replayable webhooks = forgery & replay.** Without a signature, anyone who learns the URL can POST fake events; without a timestamp+window, an attacker can capture a real signed request and replay it later. HMAC + replay window closes both. **Never** trust a webhook's contents without verifying the signature.
- **Constant-time comparison.** Comparing signatures with `==`/`String.equals` leaks, via timing, how much matched — enabling a byte-by-byte forgery. Use a constant-time compare (`MessageDigest.isEqual`).
- **Leaking the secret / using a weak one.** The webhook secret is a credential — config/Vault (Phase H), never committed, rotated. A per-partner secret limits blast radius.
- **Unbounded pagination.** Allowing `size=1000000` is a DoS; cap the page size (and we default it). Sorting on arbitrary columns can also be abused — whitelist sortable fields in a hardened API.

## 🕰️ Then vs. Now (How This Changed Across Versions)

| Topic | Then | Now | Why it changed |
|---|---|---|---|
| **Idempotency** | Ad-hoc dedupe, or none (double-charges). | Standard **`Idempotency-Key`** header + key store (Stripe-style). | A documented, client-driven contract for safe retries. |
| **Webhook auth** | Shared secret in the URL / no verification. | **HMAC signature + timestamp** (replay window), constant-time verify. | URL secrets leak in logs; signatures prove authenticity + integrity + freshness. |
| **Pagination** | Return everything, or leak the ORM `Page` object. | `Pageable` + a **stable DTO envelope**; Spring warns against exposing `Page`. | Bounded responses + a contract the API owns. |
| **API docs/versioning tooling** | springfox + URI versioning by hand. | springdoc (Step 13) + explicit `/v1` + RFC 8594 `Sunset` / RFC 9745 `Deprecation` headers. | Maintained tooling; standardized deprecation signaling. |

> [!NOTE]
> *Verify, don't guess.* `Sunset` is RFC 8594; `Deprecation` is RFC 9745 (2025 — its standardized value is an `@<unix-timestamp>` date; our code sends the widely-deployed legacy draft `true`). The `Idempotency-Key` + HMAC-timestamp webhook scheme is the de-facto industry standard (Stripe). HMAC-SHA256 is `javax.crypto.Mac` (JDK). The Boot-4 Jackson-3 default (no Jackson-2 `ObjectMapper` bean) is a real change we hit and worked around (🩺). No new dependencies were added this step.

## 🧵 Thread-safety note

Two shared-state hazards here, both already in your toolkit. (1) **Concurrent duplicate idempotency keys** — resolved at the database by the key's **PRIMARY KEY** (Step 10's unique constraint + Step 12's "the DB is the coordination point"): only one of two racing duplicates can commit. (2) The webhook components (`WebhookSigner`/`WebhookSender`/`WebhookPublisher`) are **singletons** with **no mutable state** — the signer is pure, the sender holds only an immutable `HttpClient` (itself thread-safe), so they're safe to share across request threads (Step 11's "stateless singletons" rule). Per-request data (timestamp, body) is passed as arguments, never stored in fields.

---

<a id="build"></a>

# C · 🛠️ Build

## 📦 Your Starting Point

You're at **`step-14-start`** (== `step-13-end`). demand-account has the transfer + ledger (Step 12) and ProblemDetail/OpenAPI (Step 13). We add a `/api/v1` namespace with idempotency, pagination, deprecation, and signed webhooks — **no new dependencies**.

Confirm the start builds:
```bash
./mvnw -q -pl services/demand-account -am verify   # green, 13 tests, from Step 13
```

## 🛠️ Let's Build It — Step by Step

```mermaid
flowchart TB
    a["0 · V2 migration + IdempotencyRecord (key store)"] --> b["1 · IdempotentTransferService (lookup-or-execute)"]
    b --> c["2 · pagination: Pageable repo + PageResponse envelope"]
    c --> d["3 · WebhookSigner (HMAC + replay window) + WebhookSignerTest"]
    d --> e["4 · WebhookSender (retry) + WebhookPublisher + WebhookDeliveryTest"]
    e --> f["5 · controller: /api/v1 (idempotent + paginated) + deprecate old"]
    f --> g["6 · IdempotencyTest + controller/integration test updates"]
```

🌳 **Files we'll touch** (under `services/demand-account/`):
```
src/main/resources/db/migration/V2__idempotency_keys.sql
src/main/java/com/buildabank/account/
├── domain/{IdempotencyRecord, IdempotencyRecordRepository}.java   + LedgerEntryRepository (paged finder)
├── service/IdempotentTransferService.java                         + TransferService.entriesOf(...)
├── webhook/{WebhookSigner, WebhookSender, WebhookPublisher}.java
└── web/{TransferController (v1 + deprecation), PageResponse, LedgerEntryResponse}.java
src/test/java/com/buildabank/account/  (WebhookSignerTest → sub-step 3, WebhookDeliveryTest → sub-step 4,
                                        IdempotencyTest + controller/integration updates → sub-step 6)
steps/step-14/{requests.http, smoke.sh} · adr/0006-api-versioning-and-idempotency.md
```

---

### Sub-step 0 of 6 — Idempotency key store · ~1h 🧭 *(you are here: **key store** → idempotent service → pagination → signer → sender → controller → tests)*

🎯 **Goal:** a table + entity to remember `Idempotency-Key → transactionId`.

📁 **Location:** `V2__idempotency_keys.sql` + `domain/IdempotencyRecord.java` + its repository.

⌨️ **Code** (the migration — a new file):
```sql
-- services/demand-account/src/main/resources/db/migration/V2__idempotency_keys.sql
-- Public-API idempotency (Step 14): a store of Idempotency-Key -> the result it produced, so a retried
-- request returns the original result instead of moving money twice. The PRIMARY KEY on the key gives us
-- the concurrency guard: two racing requests with the same key can't both insert, so only one transfer commits.

create table idempotency_key (
    idempotency_key varchar(200) primary key,
    transaction_id  uuid        not null,
    created_at      timestamp(6) with time zone not null
);
```
the entity (key as the natural `@Id` — the whole file):
```java
// services/demand-account/src/main/java/com/buildabank/account/domain/IdempotencyRecord.java
package com.buildabank.account.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Remembers that a given {@code Idempotency-Key} already produced a transfer, and which one. A retried
 * request with the same key returns the stored {@code transactionId} instead of moving money again. The key
 * is the natural {@code @Id} (a client-supplied string), and its PRIMARY KEY uniqueness is the concurrency
 * guard — two racing requests with the same key can't both insert, so only one transfer commits.
 */
@Entity
@Table(name = "idempotency_key")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", updatable = false)
    private String key;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String key, UUID transactionId, Instant createdAt) {
        this.key = key;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
    }

    public String getKey() {
        return key;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
```
and the repository — the shortest file in the course, but it's the lookup half of the pattern:
```java
// services/demand-account/src/main/java/com/buildabank/account/domain/IdempotencyRecordRepository.java
package com.buildabank.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
}
```

🔍 **Line-by-line:** the **`primary key`** on `idempotency_key` is doing double duty — it's the lookup key *and* the uniqueness constraint that lets only one of two racing duplicates commit. The entity uses the client-supplied string as its `@Id` (a natural key, not generated), so `JpaRepository<IdempotencyRecord, String>` gives us `findById(key)` for free. The `protected` no-arg constructor is for JPA only; all columns are `updatable = false` — a key mapping is written once, never edited.

💭 **Under the hood:** `ddl-auto=validate` means Flyway owns the table; Hibernate just checks the mapping matches. Flyway runs `V1` then `V2` on startup.

🔮 **Predict:** you restart the app after adding this file — what does Flyway do with `V1`, which already ran? <details><summary>answer</summary>Skips it (its checksum is recorded in `flyway_schema_history`) and applies only `V2` — the startup log reports 2 applied migrations total.</details>

✋ **Checkpoint:** `./mvnw -q -pl services/demand-account compile` succeeds; `flyway` will report 2 migrations.

💾 **Commit:** `git add services/demand-account/src/main/resources/db/migration/V2__idempotency_keys.sql services/demand-account/src/main/java/com/buildabank/account/domain/Idempotency* && git commit -m "feat(demand-account): idempotency key store (V2 + entity)"`

🔁 *Stopping here? You have the V2 migration + `IdempotencyRecord` entity/repository committed, compiling green. Next: sub-step 1 (`IdempotentTransferService`); first action: create `services/demand-account/src/main/java/com/buildabank/account/service/IdempotentTransferService.java`.*

⚠️ **Pitfall:** keys grow forever — production needs a TTL/cleanup. Noted in ADR-0006.

---

### Sub-step 1 of 6 — `IdempotentTransferService` · ~1.5h 🧭 *(key store ✅ → **idempotent service** → …)*

🎯 **Goal:** lookup-or-execute-and-store, in one transaction.

📁 **Location:** `service/IdempotentTransferService.java`

⌨️ **Code** (the whole file):
```java
// services/demand-account/src/main/java/com/buildabank/account/service/IdempotentTransferService.java
package com.buildabank.account.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buildabank.account.domain.IdempotencyRecord;
import com.buildabank.account.domain.IdempotencyRecordRepository;

/**
 * Public-API <strong>idempotency</strong> for transfers. A client retrying a transfer (e.g. after a network
 * timeout) sends the same {@code Idempotency-Key}; this service returns the original result instead of
 * moving money a second time — the property that makes money-moving APIs safe to retry.
 *
 * <p>The whole thing runs in one transaction with {@link TransferService#transfer} (REQUIRED propagation),
 * so the key row and the transfer commit atomically. The key's PRIMARY-KEY uniqueness is the concurrency
 * guard: if two racing requests with the same key both miss the lookup and both transfer, only one can
 * commit the key row — the other's commit fails the unique constraint and the whole transaction (including
 * its transfer) rolls back. For the common case — a <em>sequential</em> retry — the second request finds the
 * stored record and returns its {@code transactionId} without re-executing.
 */
@Service
public class IdempotentTransferService {

    private final TransferService transfers;
    private final IdempotencyRecordRepository keys;

    public IdempotentTransferService(TransferService transfers, IdempotencyRecordRepository keys) {
        this.transfers = transfers;
        this.keys = keys;
    }

    @Transactional
    public UUID transfer(String idempotencyKey, String from, String to, BigDecimal amount, String description) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return transfers.transfer(from, to, amount, description);   // no idempotency requested
        }
        Optional<IdempotencyRecord> existing = keys.findById(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get().getTransactionId();                  // idempotent hit — do NOT re-execute
        }
        UUID transactionId = transfers.transfer(from, to, amount, description);
        keys.save(new IdempotencyRecord(idempotencyKey, transactionId, Instant.now()));
        return transactionId;
    }
}
```

🔍 **Line-by-line:** `@Transactional` joins the transfer's transaction (REQUIRED), so the key row + the transfer commit together. A present key → return the stored `transactionId` (no second transfer). A new key → transfer, then store the mapping. A blank/absent key → plain transfer (idempotency is opt-in per request).

💭 **Under the hood:** for a **sequential** retry the lookup hits and short-circuits. For a **concurrent** duplicate, both miss and transfer, but the PK lets only one commit the key — the other rolls back (no double-spend). The DB is the arbiter (Step 12).

🔮 **Predict:** two POSTs of $50 with the same key on a $200 account — final balance? <details><summary>answer</summary>$150 — the money moves once; the retry returns the stored transactionId. (`IdempotencyTest` proves it.)</details>

✋ **Checkpoint:** compiles.

💾 **Commit:** `git add services/demand-account/src/main/java/com/buildabank/account/service/IdempotentTransferService.java && git commit -m "feat(demand-account): idempotent transfer (Idempotency-Key)"`

🔁 *Stopping here? You have the lookup-or-execute-and-store service committed (end of Session Plan S2). Next: sub-step 2 (pagination); first action: open `services/demand-account/src/main/java/com/buildabank/account/domain/LedgerEntryRepository.java` and add the paged finder.*

⚠️ **Pitfall:** re-emitting the webhook on an idempotent *hit* is fine (at-least-once → receivers dedupe), but executing the *transfer* twice is not — the lookup must short-circuit before `transfers.transfer`.

---

### Sub-step 2 of 6 — Pagination · ~2h 🧭 *(… → **pagination** → …)*

🎯 **Goal:** page an account's ledger entries with a stable envelope.

📁 **Location:** `LedgerEntryRepository` (paged finder), `TransferService.entriesOf(...)`, `web/PageResponse.java`, `web/LedgerEntryResponse.java`.

⌨️ **Code.** Two files change and two are new. The repository gains a paged finder (a diff — this file exists since Step 12):
```diff
--- a/services/demand-account/src/main/java/com/buildabank/account/domain/LedgerEntryRepository.java
+++ b/services/demand-account/src/main/java/com/buildabank/account/domain/LedgerEntryRepository.java
@@
 import java.math.BigDecimal;
 import java.util.List;
 import java.util.UUID;
 
+import org.springframework.data.domain.Page;
+import org.springframework.data.domain.Pageable;
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.data.jpa.repository.Query;
@@
     List<LedgerEntry> findByAccountIdOrderByCreatedAtAsc(Long accountId);
 
+    /** A page of an account's entries — Spring Data applies the {@link Pageable}'s page/size/sort to the SQL. */
+    Page<LedgerEntry> findByAccountId(Long accountId, Pageable pageable);
+
     List<LedgerEntry> findByTransactionId(UUID transactionId);
```
`TransferService` gains `entriesOf(...)` (a diff — insert after `transfer(...)`, before `balanceOf`):
```diff
--- a/services/demand-account/src/main/java/com/buildabank/account/service/TransferService.java
+++ b/services/demand-account/src/main/java/com/buildabank/account/service/TransferService.java
@@
+import org.springframework.data.domain.Page;
+import org.springframework.data.domain.Pageable;
 import org.springframework.stereotype.Service;
 import org.springframework.transaction.annotation.Transactional;
@@
+    /** A page of an account's ledger entries (Step 14 — pagination/sorting via the {@link Pageable}). */
+    @Transactional(readOnly = true)
+    public Page<LedgerEntry> entriesOf(String accountNumber, Pageable pageable) {
+        Account account = accounts.findByAccountNumber(accountNumber)
+                .orElseThrow(() -> new IllegalArgumentException("no such account: " + accountNumber));
+        return ledger.findByAccountId(account.getId(), pageable);
+    }
```
The envelope we own (a new file, whole thing):
```java
// services/demand-account/src/main/java/com/buildabank/account/web/PageResponse.java
package com.buildabank.account.web;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * A stable, explicit pagination envelope. We do NOT serialize Spring Data's {@code Page} directly: its JSON
 * shape is an internal implementation detail (Spring even warns against exposing it), and a public API
 * should own its contract. This record is that contract: the items plus the page metadata clients need.
 */
public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Map a Spring Data {@link Page} of entities into a DTO page via {@code mapper}. */
    public static <E, T> PageResponse<T> of(Page<E> page, java.util.function.Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
```
and the per-entry DTO (a new file, whole thing):
```java
// services/demand-account/src/main/java/com/buildabank/account/web/LedgerEntryResponse.java
package com.buildabank.account.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.buildabank.account.domain.EntryDirection;
import com.buildabank.account.domain.LedgerEntry;

/** API view of a ledger entry — a DTO, so we never serialize the JPA entity directly. */
public record LedgerEntryResponse(
        UUID transactionId, EntryDirection direction, BigDecimal amount, String description, Instant createdAt) {

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(entry.getTransactionId(), entry.getDirection(),
                entry.getAmount(), entry.getDescription(), entry.getCreatedAt());
    }
}
```

🔍 **Line-by-line:** `Page<LedgerEntry> findByAccountId(Long, Pageable)` — Spring Data runs a `LIMIT/OFFSET` + `COUNT`. `entriesOf` is `@Transactional(readOnly = true)` — a read path, resolved account-number → id first so a bad account fails fast. `PageResponse.of(page, mapper)` maps entities → DTOs and copies the page metadata. We **never** return the raw `Page` (its JSON shape is an unstable internal detail), and `LedgerEntryResponse` keeps JPA entities out of the JSON.

💭 **Under the hood:** the controller takes a `Pageable` parameter bound from `?page=&size=&sort=field,dir` by Spring Data's resolver (auto-configured in the full app context).

🔮 **Predict:** what SQL does `findByAccountId(accountId, PageRequest.of(0, 2, Sort.by("createdAt").descending()))` produce? <details><summary>answer</summary>Two statements: a `SELECT … WHERE account_id = ? ORDER BY created_at DESC LIMIT 2 OFFSET 0`, plus a `SELECT count(*) …` for `totalElements`.</details>

✋ **Checkpoint:** compiles; the entries endpoint (sub-step 5) will return a `PageResponse`.

💾 **Commit:** `git add services/demand-account/src/main/java/com/buildabank/account/domain/LedgerEntryRepository.java services/demand-account/src/main/java/com/buildabank/account/service/TransferService.java services/demand-account/src/main/java/com/buildabank/account/web/PageResponse.java services/demand-account/src/main/java/com/buildabank/account/web/LedgerEntryResponse.java && git commit -m "feat(demand-account): pagination envelope + paged ledger finder"`

🔁 *Stopping here? You have the paged finder, `entriesOf(...)`, and the `PageResponse`/`LedgerEntryResponse` envelope committed (end of Session Plan S3). Next: sub-step 3 (`WebhookSigner`); first action: create `services/demand-account/src/main/java/com/buildabank/account/webhook/WebhookSigner.java`.*

⚠️ **Pitfall:** exposing `Page` directly works but its JSON can change between Spring versions (Spring even logs a warning). Own your envelope.

---

### Sub-step 3 of 6 — `WebhookSigner` (HMAC + replay window) · ~2.5h 🧭 *(… → **signer** → …)*

🎯 **Goal:** sign and verify webhooks; reject tampering and replays — and prove it with `WebhookSignerTest` in the same sitting.

📁 **Location:** `webhook/WebhookSigner.java` + `test/…/webhook/WebhookSignerTest.java`

⌨️ **Code** (the whole file — note the checked crypto exceptions are wrapped, and `toHex` is ours):
```java
// services/demand-account/src/main/java/com/buildabank/account/webhook/WebhookSigner.java
package com.buildabank.account.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * Signs and verifies outbound webhooks with <strong>HMAC-SHA256</strong>. The signature is computed over
 * {@code "<timestamp>.<body>"} with a shared secret, so a receiver can prove (a) the payload wasn't tampered
 * with and (b) it really came from us. Including the timestamp in the signed material — and rejecting old
 * timestamps on verify — gives <strong>replay protection</strong>: an attacker can't re-send a captured,
 * still-valid request hours later.
 *
 * <p>This is the same scheme Stripe/GitHub-style webhooks use. We compare signatures in
 * <strong>constant time</strong> to avoid leaking, via timing, how much of a guessed signature was correct.
 */
@Component
public class WebhookSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /** Hex HMAC-SHA256 of {@code timestamp + "." + body} keyed by {@code secret}. */
    public String sign(String secret, long timestampEpochSeconds, String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] raw = mac.doFinal((timestampEpochSeconds + "." + body).getBytes(StandardCharsets.UTF_8));
            return toHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("failed to sign webhook", e);
        }
    }

    /**
     * Verify a received signature: recompute and compare in constant time, AND reject timestamps outside the
     * tolerance window (replay protection). {@code nowEpochSeconds} is passed in so it's testable.
     */
    public boolean verify(String secret, long timestampEpochSeconds, String body, String providedSignature,
                          long nowEpochSeconds, long toleranceSeconds) {
        if (Math.abs(nowEpochSeconds - timestampEpochSeconds) > toleranceSeconds) {
            return false;   // too old (or too far in the future) → likely a replay
        }
        String expected = sign(secret, timestampEpochSeconds, body);
        return constantTimeEquals(expected, providedSignature);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
```
Now write the test — pure unit, no Spring, no Docker — so this sub-step's checkpoint can run it:
```java
// services/demand-account/src/test/java/com/buildabank/account/webhook/WebhookSignerTest.java
package com.buildabank.account.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Pure unit tests for the HMAC webhook signing — authenticity, tamper-detection, and replay protection. */
class WebhookSignerTest {

    private final WebhookSigner signer = new WebhookSigner();
    private static final String SECRET = "whsec_test_0123456789";
    private static final long TS = 1_700_000_000L;
    private static final String BODY = "{\"event\":\"transfer.completed\",\"amount\":50.00}";

    @Test
    void aFreshValidSignatureVerifies() {
        String signature = signer.sign(SECRET, TS, BODY);
        // verify "now" is the same second → within tolerance
        assertThat(signer.verify(SECRET, TS, BODY, signature, TS, 300)).isTrue();
    }

    @Test
    void aTamperedBodyIsRejected() {
        String signature = signer.sign(SECRET, TS, BODY);
        String tampered = BODY.replace("50.00", "5000.00");   // attacker bumps the amount
        assertThat(signer.verify(SECRET, TS, tampered, signature, TS, 300)).isFalse();
    }

    @Test
    void theWrongSecretIsRejected() {
        String signature = signer.sign(SECRET, TS, BODY);
        assertThat(signer.verify("whsec_attacker", TS, BODY, signature, TS, 300)).isFalse();
    }

    @Test
    void aStaleTimestampIsRejected_replayProtection() {
        String signature = signer.sign(SECRET, TS, BODY);
        long muchLater = TS + 3_600;   // one hour later, tolerance is 300s
        assertThat(signer.verify(SECRET, TS, BODY, signature, muchLater, 300)).isFalse();
    }
}
```

🔍 **Line-by-line:** `Mac.getInstance("HmacSHA256")` + a `SecretKeySpec` keyed by the secret → an HMAC over `"<ts>.<body>"`. Both throw *checked* crypto exceptions (`NoSuchAlgorithmException`/`InvalidKeyException`) that can't realistically happen for a JDK-standard algorithm, so `sign` wraps them in an `IllegalStateException` rather than polluting every caller's signature. `verify` first checks the timestamp is within tolerance (**replay protection**), then recomputes and compares in **constant time** (`MessageDigest.isEqual`) — never `String.equals`, which short-circuits and leaks timing. `toHex` hex-encodes the raw MAC bytes (two hex digits per byte) — the wire format partners see. In the test, `nowEpochSeconds` is a *parameter*, which is why replay can be tested without sleeping for an hour.

💭 **Under the hood:** HMAC is a keyed hash — without the secret you can't produce a matching signature for a chosen `(ts, body)`. Binding the timestamp into the signed material means an attacker can't reuse an old signature with a new time, and can't change the time without breaking the signature.

🔮 **Predict:** a captured valid request replayed an hour later (tolerance 300s) — verify result? <details><summary>answer</summary>false — the timestamp is outside the window. (`WebhookSignerTest.aStaleTimestampIsRejected` proves it; the §12.3 mutation removes this check and the test fails.)</details>

✋ **Checkpoint:** `./mvnw -pl services/demand-account test -Dtest=WebhookSignerTest` is green (sign/verify, tamper, wrong-secret, replay all covered).

💾 **Commit:** `git add services/demand-account/src/main/java/com/buildabank/account/webhook/WebhookSigner.java services/demand-account/src/test/java/com/buildabank/account/webhook/WebhookSignerTest.java && git commit -m "feat(demand-account): HMAC webhook signing + replay protection"`

🔁 *Stopping here? You have a signing/verifying `WebhookSigner` with 4 green `WebhookSignerTest` tests committed (end of Session Plan S4). Next: sub-step 4 (sender + publisher); first action: create `services/demand-account/src/main/java/com/buildabank/account/webhook/WebhookSender.java`.*

⚠️ **Pitfall:** comparing signatures with `equals`/`==` is a timing-attack vector — use `MessageDigest.isEqual`.

---

### Sub-step 4 of 6 — `WebhookSender` (retry) + `WebhookPublisher` (config-gated) · ~2.5h 🧭 *(… → **sender/publisher** → …)*

🎯 **Goal:** deliver the signed event over HTTP with bounded retries; build the event JSON — and prove delivery + retry with `WebhookDeliveryTest` in the same sitting.

📁 **Location:** `webhook/WebhookSender.java`, `webhook/WebhookPublisher.java` + `test/…/webhook/WebhookDeliveryTest.java`

⌨️ **Code** (the sender — whole file; note the `HttpClient` field, `MAX_ATTEMPTS`, and `sleepBackoff`):
```java
// services/demand-account/src/main/java/com/buildabank/account/webhook/WebhookSender.java
package com.buildabank.account.webhook;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Delivers a signed webhook over HTTP with <strong>bounded retries</strong>. Webhook delivery is
 * <em>at-least-once</em>: networks fail, receivers hiccup, so we retry a few times with backoff — which is
 * exactly why receivers must be <strong>idempotent</strong> (they may see the same event twice). Each attempt
 * carries the HMAC signature and timestamp ({@link WebhookSigner}) so the receiver can verify authenticity
 * and reject replays.
 *
 * <p>(This sends directly for teaching clarity. In production the <em>dual-write problem</em> — the DB
 * transaction commits but the webhook send fails, or vice-versa — is solved by the <strong>Outbox
 * pattern</strong> in Step 20; we flag that explicitly rather than pretend this is complete.)
 */
@Component
public class WebhookSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookSender.class);
    private static final int MAX_ATTEMPTS = 3;

    private final WebhookSigner signer;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public WebhookSender(WebhookSigner signer) {
        this.signer = signer;
    }

    /** POST the signed body to {@code url}; retry up to 3 times on failure. Returns true if a 2xx was received. */
    public boolean send(String url, String secret, String body) {
        long timestamp = Instant.now().getEpochSecond();
        String signature = signer.sign(secret, timestamp, body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .header("X-Webhook-Timestamp", Long.toString(timestamp))
                .header("X-Webhook-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() / 100 == 2) {
                    log.info("webhook delivered to {} on attempt {} ({})", url, attempt, response.statusCode());
                    return true;
                }
                log.warn("webhook to {} got {} on attempt {}", url, response.statusCode(), attempt);
            } catch (Exception e) {
                log.warn("webhook to {} failed on attempt {}: {}", url, attempt, e.toString());
            }
            sleepBackoff(attempt);
        }
        log.error("webhook to {} FAILED after {} attempts", url, MAX_ATTEMPTS);
        return false;
    }

    private static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(50L * attempt);   // simple linear backoff (small, so tests stay fast)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```
The publisher (whole file) owns a Jackson mapper (Boot-4 gotcha — see 🩺) and is **config-gated** via `@Value`. There is **no `application.yml` change**: `bank.webhook.url` defaults to empty (→ no-op) and `bank.webhook.secret` to `demo-secret`; you override them per-environment (e.g. the `BANK_WEBHOOK_URL`/`BANK_WEBHOOK_SECRET` env vars in 🎮 Play With It):
```java
// services/demand-account/src/main/java/com/buildabank/account/webhook/WebhookPublisher.java
package com.buildabank.account.webhook;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Builds the {@code transfer.completed} event JSON and hands it to the {@link WebhookSender}. Gated by
 * config: if {@code bank.webhook.url} is unset (the default), publishing is a no-op — so local runs and
 * tests that don't care about webhooks aren't affected. The secret comes from config too (never hard-coded
 * in real life — Vault/secrets in Phase H).
 */
@Component
public class WebhookPublisher {

    private final WebhookSender sender;
    // Own a Jackson mapper rather than inject one: Spring Boot 4 defaults the web stack to Jackson 3, so a
    // Jackson-2 com.fasterxml ObjectMapper bean isn't auto-created. A self-owned mapper keeps this independent.
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String url;
    private final String secret;

    public WebhookPublisher(WebhookSender sender,
                            @Value("${bank.webhook.url:}") String url,
                            @Value("${bank.webhook.secret:demo-secret}") String secret) {
        this.sender = sender;
        this.url = url;
        this.secret = secret;
    }

    /** Emit a signed {@code transfer.completed} webhook (no-op if no URL is configured). */
    public void transferCompleted(UUID transactionId, String from, String to, BigDecimal amount) {
        if (url == null || url.isBlank()) {
            return;   // webhooks not configured → skip
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", "transfer.completed");
        event.put("transactionId", transactionId.toString());
        event.put("from", from);
        event.put("to", to);
        event.put("amount", amount);
        try {
            sender.send(url, secret, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new IllegalStateException("failed to publish webhook", e);
        }
    }
}
```
And the proof — a real (in-test) HTTP receiver, no Spring, no Docker (whole file):
```java
// services/demand-account/src/test/java/com/buildabank/account/webhook/WebhookDeliveryTest.java
package com.buildabank.account.webhook;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;

/**
 * End-to-end webhook delivery over a real (in-test) HTTP receiver — no Spring, no Docker. Proves the
 * {@link WebhookSender} delivers a signed payload that the receiver can <strong>verify</strong> with the
 * shared secret, and that delivery <strong>retries</strong> on a transient failure (at-least-once).
 */
class WebhookDeliveryTest {

    private static final String SECRET = "whsec_delivery_test";
    private final WebhookSigner signer = new WebhookSigner();
    private final WebhookSender sender = new WebhookSender(signer);

    @Test
    void deliversASignedWebhookTheReceiverCanVerify() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        AtomicBoolean signatureValid = new AtomicBoolean(false);
        CountDownLatch received = new CountDownLatch(1);

        server.createContext("/webhooks", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            long timestamp = Long.parseLong(exchange.getRequestHeaders().getFirst("X-Webhook-Timestamp"));
            String signature = exchange.getRequestHeaders().getFirst("X-Webhook-Signature");
            // The receiver verifies authenticity + freshness exactly as a partner would.
            boolean ok = signer.verify(SECRET, timestamp, body, signature, Instant.now().getEpochSecond(), 300);
            signatureValid.set(ok);
            exchange.sendResponseHeaders(ok ? 200 : 400, -1);
            exchange.close();
            received.countDown();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/webhooks";
            boolean delivered = sender.send(url, SECRET, "{\"event\":\"transfer.completed\",\"amount\":50.00}");

            assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(delivered).isTrue();
            assertThat(signatureValid).isTrue();   // the receiver validated our HMAC signature + timestamp
        } finally {
            server.stop(0);
        }
    }

    @Test
    void retriesOnTransientFailure() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        AtomicInteger calls = new AtomicInteger();

        server.createContext("/webhooks", exchange -> {
            int attempt = calls.incrementAndGet();
            exchange.sendResponseHeaders(attempt == 1 ? 500 : 200, -1);   // fail first, then accept
            exchange.close();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/webhooks";
            boolean delivered = sender.send(url, SECRET, "{\"event\":\"x\"}");

            assertThat(delivered).isTrue();
            assertThat(calls.get()).isGreaterThanOrEqualTo(2);   // it retried after the 500
        } finally {
            server.stop(0);
        }
    }
}
```

🔍 **Line-by-line:** the sender attaches `X-Webhook-Timestamp` + `X-Webhook-Signature` and POSTs; on a non-2xx or exception it **retries** with backoff (at-least-once). `MAX_ATTEMPTS = 3` bounds it; `sleepBackoff` sleeps `50ms × attempt` (linear, deliberately tiny so tests stay fast — production would use exponential + jitter) and restores the interrupt flag if interrupted. The `HttpClient` is one immutable field — thread-safe, shared across requests (Step 11). The publisher is a **no-op unless `bank.webhook.url` is set**, so local runs/tests that don't care aren't affected; it owns its own `ObjectMapper` (Boot 4's web stack is Jackson 3, so there's no Jackson-2 mapper bean to inject) and uses a `LinkedHashMap` so the event JSON's field order is stable. In the delivery test, `HttpServer.create(…, 0)` binds a random free port — no clashes.

💭 **Under the hood:** because delivery retries, the partner may receive the event twice → **receivers must be idempotent**. We send *after* the transfer commits (the controller isn't `@Transactional`), which is the **dual-write** seam the Outbox pattern (Step 20) closes.

🔮 **Predict:** the receiver 500s the first attempt and 200s the second — what does `send(...)` return, and how many HTTP calls did the receiver count? <details><summary>answer</summary>`true`, and ≥2 calls — attempt 1 gets the 500, `sleepBackoff(1)` waits 50ms, attempt 2 gets the 200. `WebhookDeliveryTest.retriesOnTransientFailure` asserts exactly this.</details>

✋ **Checkpoint:** `./mvnw -pl services/demand-account test -Dtest=WebhookDeliveryTest` green (an in-test receiver verifies our signature; a transient 500 triggers a retry).

💾 **Commit:** `git add services/demand-account/src/main/java/com/buildabank/account/webhook services/demand-account/src/test/java/com/buildabank/account/webhook/WebhookDeliveryTest.java && git commit -m "feat(demand-account): webhook sender (retry) + config-gated publisher"`

🔁 *Stopping here? You have signed webhook delivery with bounded retries, proven by 2 green `WebhookDeliveryTest` tests, committed (end of Session Plan S5). Next: sub-step 5 (the `/api/v1` controller); first action: open `services/demand-account/src/main/java/com/buildabank/account/web/TransferController.java`.*

⚠️ **Pitfall:** injecting `com.fasterxml…ObjectMapper` fails on Boot 4 (no such bean — Jackson 3 default). Own the mapper, or use Jackson 3. (🩺)

---

### Sub-step 5 of 6 — Controller: `/api/v1` + deprecate the old · ~2h 🧭 *(… → **controller** → tests)*

🎯 **Goal:** wire the versioned, idempotent transfer (with webhook) and the paginated entries; deprecate the old transfer. This is an **edit to an existing file** — and by now you've built every piece it wires together, so this one is *type-it-yourself first*.

📁 **Location:** `web/TransferController.java`

✍️ **Type it yourself.** Before opening the solution, try writing the three changes from what you already know. The controller gains two collaborators (`IdempotentTransferService`, `WebhookPublisher` — constructor injection) and:

1. `transfer` (existing `POST /api/transfers`) — keep the behavior, add three response headers: `Deprecation: true`, `Sunset: Sat, 31 Oct 2026 23:59:59 GMT`, `Link: </api/v1/transfers>; rel="successor-version"`.
2. `transferV1(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, @Valid @RequestBody TransferRequest request)` on `POST /api/v1/transfers` — delegate to `idempotentTransfers.transfer(...)`, then `webhookPublisher.transferCompleted(...)`, return the `TransferResponse`.
3. `entries(@PathVariable String accountNumber, @PageableDefault(size = 20, sort = "createdAt") Pageable pageable)` on `GET /api/v1/accounts/{accountNumber}/entries` — return `PageResponse.of(transfers.entriesOf(accountNumber, pageable), LedgerEntryResponse::from)`.

<details>
<summary>✅ Solution — the full diff to <code>TransferController.java</code></summary>

```diff
--- a/services/demand-account/src/main/java/com/buildabank/account/web/TransferController.java
+++ b/services/demand-account/src/main/java/com/buildabank/account/web/TransferController.java
@@
 import jakarta.validation.Valid;
 
+import org.springframework.data.domain.Pageable;
+import org.springframework.data.web.PageableDefault;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.GetMapping;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.PostMapping;
 import org.springframework.web.bind.annotation.RequestBody;
+import org.springframework.web.bind.annotation.RequestHeader;
 import org.springframework.web.bind.annotation.RestController;
 
 import com.buildabank.account.domain.Account;
+import com.buildabank.account.service.IdempotentTransferService;
 import com.buildabank.account.service.TransferService;
+import com.buildabank.account.webhook.WebhookPublisher;
 
-/** REST API for accounts and transfers. Money movement always uses the safe (pessimistic-lock) path. */
+/**
+ * REST API for accounts and transfers. Step 14 adds <strong>versioned</strong> endpoints under
+ * {@code /api/v1}: an <strong>idempotent</strong> transfer ({@code Idempotency-Key} header) that also emits a
+ * signed <strong>webhook</strong>, and a <strong>paginated</strong> ledger-entries listing. The original
+ * {@code POST /api/transfers} stays for compatibility but is marked <strong>deprecated</strong> (it returns
+ * {@code Deprecation}/{@code Sunset}/{@code Link} headers pointing at its successor).
+ */
 @RestController
 public class TransferController {
 
     private final TransferService transfers;
+    private final IdempotentTransferService idempotentTransfers;
+    private final WebhookPublisher webhookPublisher;
 
-    public TransferController(TransferService transfers) {
+    public TransferController(TransferService transfers, IdempotentTransferService idempotentTransfers,
+                              WebhookPublisher webhookPublisher) {
         this.transfers = transfers;
+        this.idempotentTransfers = idempotentTransfers;
+        this.webhookPublisher = webhookPublisher;
     }
@@
-    /** Move money → 200 with the transaction id (safe, pessimistic-lock transfer). */
+    /**
+     * DEPRECATED transfer (the Step-12 endpoint). Still works, but advertises its replacement via standard
+     * deprecation headers so clients can migrate. New integrations should use {@code POST /api/v1/transfers}.
+     */
     @PostMapping("/api/transfers")
     public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
         UUID transactionId = transfers.transfer(
                 request.from(), request.to(), request.amount(), request.description());
+        return ResponseEntity.ok()
+                .header("Deprecation", "true")                                       // RFC 8594
+                .header("Sunset", "Sat, 31 Oct 2026 23:59:59 GMT")                   // when it will be removed
+                .header("Link", "</api/v1/transfers>; rel=\"successor-version\"")     // where to go instead
+                .body(new TransferResponse(transactionId));
+    }
+
+    /**
+     * v1 transfer — <strong>idempotent</strong> (optional {@code Idempotency-Key} header) and emits a signed
+     * {@code transfer.completed} webhook after the money moves.
+     */
+    @PostMapping("/api/v1/transfers")
+    public ResponseEntity<TransferResponse> transferV1(
+            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
+            @Valid @RequestBody TransferRequest request) {
+        UUID transactionId = idempotentTransfers.transfer(
+                idempotencyKey, request.from(), request.to(), request.amount(), request.description());
+        // After the transfer's transaction has committed (this controller is not @Transactional).
+        // Webhooks are at-least-once, so a retried request may re-emit — receivers must be idempotent.
+        webhookPublisher.transferCompleted(transactionId, request.from(), request.to(), request.amount());
         return ResponseEntity.ok(new TransferResponse(transactionId));
     }
+
+    /** v1 paginated ledger entries for an account → 200 with a {@link PageResponse} envelope. */
+    @GetMapping("/api/v1/accounts/{accountNumber}/entries")
+    public PageResponse<LedgerEntryResponse> entries(
+            @PathVariable String accountNumber,
+            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
+        return PageResponse.of(transfers.entriesOf(accountNumber, pageable), LedgerEntryResponse::from);
+    }
 }
```
</details>

🔍 **Line-by-line:** the old `/api/transfers` still works but advertises its successor via headers. (The code comment says RFC 8594 — precisely, that RFC defines `Sunset`; `Deprecation` is RFC 9745, and `true` is the widely-deployed legacy draft value — see 🌱.) `/api/v1/transfers` reads the optional `Idempotency-Key`, runs the idempotent transfer, and (after the transaction commits) emits the webhook. `/api/v1/accounts/{n}/entries` binds a `Pageable` (with sane defaults) and returns a `PageResponse`.

💭 **Under the hood:** the webhook fires **after** `idempotentTransfers.transfer` returns — the controller has no `@Transactional`, so the transfer's transaction has committed. (Dual-write caveat → Outbox, Step 20.)

❓ **Quick check:** why is the webhook published from the *controller* rather than from inside the `@Transactional` service? <details><summary>answer</summary>So it fires only after the transfer's transaction has committed — publishing inside the transaction could announce a transfer that later rolls back. (The remaining gap — commit succeeds but the send fails — is the dual-write problem the Outbox pattern closes in Step 20.)</details>

🔮 **Predict:** you `POST /api/transfers` (the old endpoint) — which response headers come back beyond the usual ones? <details><summary>answer</summary>`Deprecation: true`, `Sunset: Sat, 31 Oct 2026 23:59:59 GMT`, and `Link: </api/v1/transfers>; rel="successor-version"` — the graceful-migration trio.</details>

▶️ **Run & See** (live, optional, +~10 min):
```bash
docker compose -f services/demand-account/compose.yaml up -d
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/demand_account ./mvnw -pl services/demand-account spring-boot:run
# POST the same Idempotency-Key twice → money moves once; GET .../entries → a PageResponse; old endpoint → Deprecation header
```
(All proven over real HTTP in `DemandAccountIntegrationTest` — see 🔬.)

✋ **Checkpoint:** the service exposes `/api/v1/transfers`, `/api/v1/accounts/{n}/entries`, and a deprecated `/api/transfers`.

💾 **Commit:** `git add services/demand-account/src/main/java/com/buildabank/account/web/TransferController.java && git commit -m "feat(demand-account): /api/v1 idempotent transfer + paginated entries + deprecate old"`

🔁 *Stopping here? You have all three v1 behaviors wired — idempotent transfer + webhook, paginated entries, deprecation headers — committed (end of Session Plan S6). Next: sub-step 6 (the remaining tests); first action: create `services/demand-account/src/test/java/com/buildabank/account/service/IdempotencyTest.java`.*

⚠️ **Pitfall:** the slice `@WebMvcTest` now needs the new collaborators mocked (`IdempotentTransferService`, `WebhookPublisher`) or the context won't load.

---

### Sub-step 6 of 6 — Tests · ~1.5h 🧭 *(… → **tests**)*

🎯 **Goal:** prove idempotency end-to-end, and update the existing web-slice + integration tests for the new endpoints. (Signing and delivery+retry were already proven in sub-steps 3–4.)

📁 **Location:** `IdempotencyTest` (new), plus updates to `TransferControllerTest` + `DemandAccountIntegrationTest`.

⌨️ **Code** (the idempotency proof — whole file, on the real Testcontainers Postgres):
```java
// services/demand-account/src/test/java/com/buildabank/account/service/IdempotencyTest.java
package com.buildabank.account.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.buildabank.account.ContainersConfig;
import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.IdempotencyRecordRepository;
import com.buildabank.account.domain.LedgerEntryRepository;

/**
 * Public-API idempotency: a retried transfer (same {@code Idempotency-Key}) returns the original result and
 * moves money <strong>once</strong> — the property that makes a money API safe to retry after a timeout.
 */
@SpringBootTest
@Import(ContainersConfig.class)
class IdempotencyTest {

    @Autowired
    IdempotentTransferService idempotentTransfers;

    @Autowired
    TransferService transfers;

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerEntryRepository ledger;

    @Autowired
    IdempotencyRecordRepository keys;

    @BeforeEach
    void clean() {
        keys.deleteAll();
        ledger.deleteAll();
        accounts.deleteAll();
        transfers.openAccount("ACC-A", "USD", new BigDecimal("200.00"));
        transfers.openAccount("ACC-B", "USD", new BigDecimal("0.00"));
    }

    @Test
    void sameKeyReturnsTheSameResult_andMovesMoneyOnce() {
        UUID first = idempotentTransfers.transfer("KEY-1", "ACC-A", "ACC-B", new BigDecimal("50.00"), "rent");
        UUID retry = idempotentTransfers.transfer("KEY-1", "ACC-A", "ACC-B", new BigDecimal("50.00"), "rent");

        assertThat(retry).isEqualTo(first);                              // same transaction id returned
        assertThat(transfers.balanceOf("ACC-A")).isEqualByComparingTo("150.00");   // moved ONCE, not twice
        assertThat(transfers.balanceOf("ACC-B")).isEqualByComparingTo("50.00");
    }

    @Test
    void aDifferentKeyMovesMoneyAgain() {
        idempotentTransfers.transfer("KEY-1", "ACC-A", "ACC-B", new BigDecimal("50.00"), "first");
        idempotentTransfers.transfer("KEY-2", "ACC-A", "ACC-B", new BigDecimal("50.00"), "second");

        assertThat(transfers.balanceOf("ACC-A")).isEqualByComparingTo("100.00");   // two distinct transfers
    }

    @Test
    void noKeyMeansNoDeduplication() {
        idempotentTransfers.transfer(null, "ACC-A", "ACC-B", new BigDecimal("10.00"), "a");
        idempotentTransfers.transfer(null, "ACC-A", "ACC-B", new BigDecimal("10.00"), "b");

        assertThat(transfers.balanceOf("ACC-A")).isEqualByComparingTo("180.00");   // both applied
    }
}
```
`TransferControllerTest` (existing slice) needs the new collaborators mocked and gains two tests (a diff):
```diff
--- a/services/demand-account/src/test/java/com/buildabank/account/web/TransferControllerTest.java
+++ b/services/demand-account/src/test/java/com/buildabank/account/web/TransferControllerTest.java
@@
 import com.buildabank.account.domain.Account;
 import com.buildabank.account.domain.InsufficientFundsException;
+import com.buildabank.account.service.IdempotentTransferService;
 import com.buildabank.account.service.TransferService;
+import com.buildabank.account.webhook.WebhookPublisher;
@@
     @MockitoBean
     TransferService transfers;
 
+    @MockitoBean
+    IdempotentTransferService idempotentTransfers;
+
+    @MockitoBean
+    WebhookPublisher webhookPublisher;
+
@@
+    @Test
+    void deprecatedTransferAdvertisesSuccessor() throws Exception {
+        given(transfers.transfer(any(), any(), any(), any())).willReturn(UUID.randomUUID());
+
+        mvc.perform(post("/api/transfers")
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("""
+                                {"from":"ACC-A","to":"ACC-B","amount":25.00}
+                                """))
+                .andExpect(status().isOk())
+                .andExpect(header().string("Deprecation", "true"))
+                .andExpect(header().exists("Sunset"))
+                .andExpect(header().string("Link", "</api/v1/transfers>; rel=\"successor-version\""));
+    }
+
+    @Test
+    void v1TransferPassesTheIdempotencyKey() throws Exception {
+        UUID txId = UUID.fromString("00000000-0000-0000-0000-0000000000cc");
+        given(idempotentTransfers.transfer(eq("KEY-1"), eq("ACC-A"), eq("ACC-B"), any(), any()))
+                .willReturn(txId);
+
+        mvc.perform(post("/api/v1/transfers")
+                        .header("Idempotency-Key", "KEY-1")
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("""
+                                {"from":"ACC-A","to":"ACC-B","amount":25.00}
+                                """))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$.transactionId").value(txId.toString()));
+    }
```
`DemandAccountIntegrationTest` (existing, real HTTP on `RANDOM_PORT`) cleans the new table and gains the end-to-end proof (a diff):
```diff
--- a/services/demand-account/src/test/java/com/buildabank/account/DemandAccountIntegrationTest.java
+++ b/services/demand-account/src/test/java/com/buildabank/account/DemandAccountIntegrationTest.java
@@
+    @Autowired
+    com.buildabank.account.domain.IdempotencyRecordRepository idempotencyKeys;
+
     private final HttpClient http = HttpClient.newHttpClient();
     private String base;
 
     @BeforeEach
     void setup() {
+        idempotencyKeys.deleteAll();
         ledger.deleteAll();
         accounts.deleteAll();
         base = "http://localhost:" + port;
     }
@@
+    @Test
+    void v1Idempotency_pagination_andDeprecation_overHttp() throws Exception {
+        post("/api/accounts", "{\"accountNumber\":\"ACC-A\",\"currency\":\"USD\",\"openingBalance\":200.00}");
+        post("/api/accounts", "{\"accountNumber\":\"ACC-B\",\"currency\":\"USD\",\"openingBalance\":0.00}");
+
+        // Idempotency: two POSTs with the same key move money ONCE.
+        String body = "{\"from\":\"ACC-A\",\"to\":\"ACC-B\",\"amount\":50.00,\"description\":\"rent\"}";
+        assertThat(postWithHeader("/api/v1/transfers", body, "Idempotency-Key", "K1").statusCode()).isEqualTo(200);
+        assertThat(postWithHeader("/api/v1/transfers", body, "Idempotency-Key", "K1").statusCode()).isEqualTo(200);
+        assertThat(get("/api/accounts/ACC-A").body()).contains("150");   // moved once (200 − 50), not 100
+
+        // A couple more (distinct) transfers to build up ledger entries for ACC-A.
+        postWithHeader("/api/v1/transfers", body, "Idempotency-Key", "K2");
+        postWithHeader("/api/v1/transfers", body, "Idempotency-Key", "K3");
+
+        // Pagination: page of ACC-A's entries → a PageResponse envelope.
+        HttpResponse<String> page = get("/api/v1/accounts/ACC-A/entries?page=0&size=2&sort=createdAt,desc");
+        assertThat(page.statusCode()).isEqualTo(200);
+        assertThat(page.body())
+                .contains("\"content\":").contains("\"totalElements\":").contains("\"size\":2");
+
+        // Deprecation: the old transfer endpoint advertises its successor.
+        HttpResponse<String> deprecated = post("/api/transfers",
+                "{\"from\":\"ACC-A\",\"to\":\"ACC-B\",\"amount\":1.00}");
+        assertThat(deprecated.statusCode()).isEqualTo(200);
+        assertThat(deprecated.headers().firstValue("Deprecation")).hasValue("true");
+    }
+
@@
+    private HttpResponse<String> postWithHeader(String path, String json, String name, String value)
+            throws Exception {
+        return http.send(HttpRequest.newBuilder(URI.create(base + path))
+                        .header("Content-Type", "application/json")
+                        .header(name, value)
+                        .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
+                HttpResponse.BodyHandlers.ofString());
+    }
```

▶️ **Run & See:**
```bash
./mvnw -pl services/demand-account -am verify
```
✅ **Expected output:**
```
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

🔬 **Break-it (the §12.3 mutation):** delete the replay-protection line in `WebhookSigner.verify` and rerun `WebhookSignerTest` — `aStaleTimestampIsRejected` fails (`Expecting value to be false but was true`). Put it back. (See 🔬 §5.)

✋ **Checkpoint:** 25 green tests.

💾 **Commit:** `git add services/demand-account/src/test && git commit -m "test(demand-account): idempotency, webhook signing/delivery, pagination, deprecation"`

🔁 *Stopping here? You have all 25 tests green and committed — the build is done (end of Session Plan S7's build half). Next: D · Prove (compare your run to the Verification Log); first action: `bash steps/step-14/smoke.sh`.*

⚠️ **Pitfall:** `@SpringBootTest` shares one DB — clean `idempotency_key` (and ledger before account, FK) in `@BeforeEach`, or a stale key turns the first request into an idempotent hit and your balance assertions drift.

---

### 🔁 The full flow you just built

```mermaid
sequenceDiagram
    participant C as Client
    participant V1 as POST /api/v1/transfers
    participant K as idempotency_key
    participant T as TransferService
    participant W as WebhookPublisher
    participant P as Partner receiver
    C->>V1: transfer (Idempotency-Key: K)
    V1->>K: seen K?
    alt first time
        K-->>V1: no
        V1->>T: transfer (one txn: debit/credit + store K→txId)
        V1->>W: transfer.completed (after commit)
        W->>P: POST + X-Webhook-Signature/Timestamp (retry if needed)
        P-->>W: 200 (after verifying HMAC + freshness)
    else retry (same K)
        K-->>V1: yes → stored txId
        V1-->>C: same transactionId (no money moves)
    end
```

*Alt-text: a client POSTs a transfer with an Idempotency-Key. First time: the key is unseen, so the transfer runs (debit/credit + store key→txId in one transaction), and after commit a signed webhook is POSTed to the partner with retries; the partner verifies the HMAC and freshness and returns 200. On a retry with the same key, the stored transactionId is returned and no money moves.*

## 🎮 Play With It

1. **Idempotency:** start the service (`make run-demand-account`), open `steps/step-14/requests.http`, and send the `POST /api/v1/transfers` with `Idempotency-Key: demo-key-001` **twice**. Then `GET /api/accounts/ACC-A` — it only dropped by 50 once.
2. **Pagination:** `GET /api/v1/accounts/ACC-A/entries?page=0&size=2&sort=createdAt,desc` → a `PageResponse` with `content`, `totalElements`, `size`.
3. **Deprecation:** `POST /api/transfers` and inspect the response headers — `Deprecation`, `Sunset`, `Link`.
4. **Webhooks (optional, live, +~10 min):** set `BANK_WEBHOOK_URL` to a [webhook.site](https://webhook.site) URL + `BANK_WEBHOOK_SECRET`, do a v1 transfer, and watch the signed `transfer.completed` arrive (with `X-Webhook-Signature`/`X-Webhook-Timestamp`). Verify it with `hmac_sha256(secret, ts + "." + body)`.
5. 🧪 **Little experiments:** change the amount on a same-key retry → still returns the original result (the key, not the body, decides); send `Idempotency-Key: a` then `b` → two transfers.

## 🏁 The Finished Result

You're at **`step-14-end`** (== `step-15-start`). demand-account's API is now versioned, idempotent, paginated, and emits signed webhooks — **25** green tests.

### ✅ Definition of Done (your self-check)
- [ ] `./mvnw -pl services/demand-account -am verify` is green with **Tests run: 25**.
- [ ] A retried transfer with the same `Idempotency-Key` moves money once.
- [ ] Webhooks are HMAC-signed, replay-protected, and retried; the old transfer advertises its successor.
- [ ] `bash steps/step-14/smoke.sh` prints `✅ Step 14 smoke test PASSED`.
- [ ] You've committed and tagged `step-14-end`.

---

<a id="prove"></a>

# D · 🔬 Prove It Works — the Verification Log

> **Tier: 🔴 Full** (changes a service + the idempotency/security path). Real pasted output below — idempotent retry, signed delivery + replay rejection, the §12.3 mutation, and a clean-room verify.

### 1 · `./mvnw -pl services/demand-account -am verify` — 25 tests green
```
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
Per class: `DemandAccountIntegrationTest` 3 · `ConcurrentTransferTest` 2 · `IdempotencyTest` 3 · `OptimisticLockTest` 1 · `TransactionPropagationTest` 1 · `TransferServiceTest` 2 · `TransferControllerTest` 7 · `WebhookDeliveryTest` 2 · `WebhookSignerTest` 4. Real Postgres 17 via Testcontainers (random high port); Flyway applies V1 + V2.

### 2 · Idempotency — a retry moves money once (IdempotencyTest)
- Same key twice → the **same `transactionId`** returned; `ACC-A` balance `150.00` (moved once, not 100).
- Different key → money moves again (`100.00`).
- No key → no dedup (both apply → `180.00`).

### 3 · Webhook signing + delivery (WebhookSignerTest + WebhookDeliveryTest)
- `sign`/`verify` round-trips; a **tampered body**, **wrong secret**, and **stale timestamp** are all rejected.
- An in-test HTTP receiver **verified our HMAC signature** on a real delivery; a transient `500` triggered a **retry** and the second attempt succeeded (`calls ≥ 2`).

### 4 · Pagination, versioning & deprecation over real HTTP (DemandAccountIntegrationTest)
- `POST /api/v1/transfers` twice with the same `Idempotency-Key` → `ACC-A` shows `150` (moved once).
- `GET /api/v1/accounts/ACC-A/entries?page=0&size=2&sort=createdAt,desc` → `200`, body has `"content"`, `"totalElements"`, `"size":2`.
- `POST /api/transfers` (old) → `200` with `Deprecation: true` header.

### 5 · §12.3 Mutation sanity-check — replay protection is load-bearing
Deleted the timestamp-tolerance check in `WebhookSigner.verify`, then ran `WebhookSignerTest`:
```
[ERROR] WebhookSignerTest.aStaleTimestampIsRejected_replayProtection:40
Expecting value to be false but was true
[INFO] BUILD FAILURE
```
A stale (replayed) timestamp is now accepted — proving the test guards replay protection. Reverted; suite green again.

### 6 · `smoke.sh`
```
==> Build + test demand-account (versioning, idempotency, pagination, signed webhooks) on real Postgres
✅ Step 14 smoke test PASSED
```

### 7 · Clean-room (§12.4) & chain
Fresh `git clone` at `step-14-end` → `make doctor` + `./mvnw verify` → **BUILD SUCCESS** (all 7 modules). Confirmed `step-14-end` == `step-15-start`.

---

<a id="apply"></a>

# E · 🎓 Apply

## 🚀 Go Deeper (Optional)

<details>
<summary>① The dual-write problem and the Outbox pattern (Step 20) · +~10 min</summary>

We send the webhook *after* the DB commit. If the process dies between commit and send, the partner never hears — the DB and the outside world disagree (a **dual write**). The **Outbox pattern** fixes it: in the *same* transaction as the transfer, insert an `outbox` row describing the event; a separate poller (or CDC, Step 54) reads the outbox and delivers, marking it sent. Now the event is durable iff the transfer committed, and delivery is retried independently. That's Step 20 — here we deliberately keep it simple and flag the gap.
</details>

<details>
<summary>② Idempotency edge cases · +~10 min</summary>

What if the *same key* arrives with a *different body* (a client bug)? Stripe returns a 422 "key reused with different parameters". A robust store also records a request fingerprint and a status (`in-progress`/`done`) so a concurrent duplicate can 409 instead of double-running. And keys need a TTL (e.g. 24h) — you don't remember them forever. We kept the core; these are the hardening steps.
</details>

<details>
<summary>③ Cursor vs offset pagination · +~10 min</summary>

`page`/`size` (offset) is simple but degrades on deep pages (`OFFSET 1000000` scans and discards a million rows) and can skip/duplicate rows if data changes between pages. **Cursor** (keyset) pagination — "give me 20 after id X / createdAt T" — is stable and fast at any depth, at the cost of no random page access. For high-volume ledgers, prefer cursors.
</details>

## 💼 Interview Prep: Questions You'll Be Asked

1. **"How do you make a payment API safe to retry?"** *(the fintech classic)* → An `Idempotency-Key` header + a server-side store of `key → result`. A retry with the same key returns the stored result without re-executing; the key's unique constraint guards concurrent duplicates (only one commits). Keys get a TTL.

2. **"How would you secure an outbound webhook?"** *(security)* → Sign each delivery with HMAC-SHA256 over `timestamp + "." + body` using a per-partner secret; the receiver verifies in constant time and rejects timestamps outside a window (replay protection). Deliver over HTTPS; rotate secrets.

3. **"Webhook delivery semantics?"** *(gotcha)* → At-least-once (you retry on failure), so receivers **must be idempotent** (dedupe by event id). Exactly-once delivery is impossible in general; you get exactly-once *effect* via idempotent receivers (+ Outbox to avoid lost/dual writes).

4. **"How do you version an API without breaking clients?"** → Pick a strategy (URI `/v1` — visible/cacheable; or header/media-type). Add new versions additively; deprecate old endpoints with `Deprecation`/`Sunset`/`Link` headers and a migration window — never a flag-day removal.

5. **"Offset vs cursor pagination?"** → Offset (`page`/`size`) is simple but slow at depth and unstable under concurrent writes; cursor (keyset) is stable and fast at any depth but no random access. Return a stable envelope you own, not the ORM's `Page`.

6. **"Concurrency: two retries of the same idempotent transfer race — what happens?"** *(concurrency)* → Both may miss the lookup and transfer, but the idempotency key's PRIMARY KEY lets only one commit; the other's transaction rolls back on the unique violation — so exactly one transfer commits. The DB is the coordination point (as with the pessimistic lock in Step 12). (In our implementation the loser surfaces an error, and its *next* retry gets the stored result; production catches the violation and re-reads the key — see 🌱.)

> **Behavioral/STAR seed:** *"Tell me about preventing a costly bug."* → Added idempotency keys to the transfer API (S/T) after noticing retries could double-charge (A); proved with a test that a same-key retry moves money once, and signed the webhooks so partners couldn't be spoofed (R).

## 🏋️ Your Turn: Practice & Challenges

1. **Reject key reuse with different parameters.** Store a request fingerprint; if the same key arrives with a different body, return a 422 ProblemDetail. <details><summary>hint</summary>Hash the normalized body; compare on lookup.</details>
2. **Add a `transfer.failed` webhook** for overdraws, signed the same way.
3. **Cursor pagination.** Add `GET /api/v1/accounts/{n}/entries?after=<id>&size=` using keyset pagination; compare query plans (Step 10) with offset on a big table. *(Reference: `solutions/step-14/`.)*
4. **Stretch — Outbox preview.** Persist the webhook event in the transfer transaction (an `outbox` table) and deliver from a `@Scheduled` poller; delete on success. (You'll formalize this in Step 20.)
5. **Stretch — key TTL.** Add `created_at`-based expiry and a cleanup that deletes keys older than 24h.

---

<a id="review"></a>

# F · 🏆 Review

## 🩺 Stuck? Troubleshooting & Fixes

| Symptom | Cause | Fix |
|---|---|---|
| Context fails: `No qualifying bean of type ...ObjectMapper` | Boot 4 web defaults to **Jackson 3** → no Jackson-2 `ObjectMapper` bean | own one: `new com.fasterxml.jackson.databind.ObjectMapper()` (or use the Jackson 3 mapper). |
| Same-key retry still moves money twice | lookup not short-circuiting before the transfer | return the stored `transactionId` *before* calling `transfers.transfer`. |
| Idempotency test flaky / wrong balance | stale keys from another test (shared DB) | clean `idempotency_key` in `@BeforeEach` (and ledger before account, FK). |
| Webhook signature never verifies | signing different bytes than the receiver hashes | both sides must hash exactly `timestamp + "." + body` (same encoding); compare in constant time. |
| `Pageable` not bound from query params | testing in a slice without Spring Data web config | bind it in the full `@SpringBootTest` context (the resolver is auto-configured there). |
| Reset to known-good | — | `git checkout step-14-end && ./mvnw -pl services/demand-account -am verify`. |

## 📚 Learn More: Resources & Glossary

- **RFC 8594** (`Sunset`), **RFC 9745** (`Deprecation` — standardized 2025; its value is an `@<unix-timestamp>` date, `true` is the legacy draft form), **RFC 9457** (ProblemDetail, Step 13).
- Stripe's API docs — the reference for idempotency keys and webhook signatures (the schemes we built).
- Spring Data — `Pageable`/`Page`; the "don't expose `Page`" guidance.

**Glossary:** **URI/header/media-type versioning** · **`Deprecation`/`Sunset`/`Link`** (RFC 9745 / RFC 8594) · **idempotency / `Idempotency-Key`** · **HMAC-SHA256** · **replay protection** · **constant-time compare** · **at-least-once / idempotent receiver** · **dual-write / Outbox** · **`Pageable` / `PageResponse`** · **offset vs cursor pagination**.

## 🏆 Recap & Study Notes

**(a) Key points**
- **Versioning**: `/api/v1` (URI) + graceful **deprecation** headers — never break clients.
- **Idempotency**: `Idempotency-Key` + a key store → retries move money once; the unique PK guards concurrency.
- **Pagination**: `page`/`size`/`sort` + a **stable envelope you own** (not Spring's `Page`).
- **Webhooks**: **HMAC-SHA256** over `timestamp.body` + a replay window + constant-time verify; **at-least-once** delivery → idempotent receivers; **dual-write** → Outbox (Step 20).
- Boot 4 web is **Jackson 3** — own your Jackson-2 `ObjectMapper` if you need one.

**(b) Key terms:** versioning (URI/header/media-type), Deprecation/Sunset/Link, Idempotency-Key, HMAC-SHA256, replay protection, constant-time compare, at-least-once, idempotent receiver, dual-write/Outbox, Pageable/PageResponse, offset vs cursor.

**(c) 🧠 Test Yourself**
1. Why is `POST /transfers` not idempotent, and how do you fix it? <details><summary>answer</summary>A retry creates a second transfer; fix with an `Idempotency-Key` + a store that returns the original result.</details>
2. What two things does a webhook timestamp + signature together protect against? <details><summary>answer</summary>Forgery/tampering (signature) and replay (timestamp within a window).</details>
3. Why must webhook receivers be idempotent? <details><summary>answer</summary>Delivery is at-least-once (retries) — they may see the same event twice.</details>
4. Why not serialize Spring Data's `Page`? <details><summary>answer</summary>Its JSON shape is an unstable internal detail; own a `PageResponse` envelope.</details>
5. Two concurrent retries with the same key — how is a double-transfer prevented? <details><summary>answer</summary>The idempotency key's PRIMARY KEY lets only one commit; the other rolls back on the unique violation.</details>

**(d) 🔗 How this connects**
- **Back to Step 13** (the MVC/ProblemDetail layer), **Step 12** (the transfer), **Step 10** (unique constraint), **Step 11** (concurrency).
- **Forward:** Step 15 (API Gateway/BFF can enforce versioning/deprecation centrally); Step 20 (**Outbox** + Kafka fixes the dual-write and makes events first-class); Step 21 (Saga + idempotent consumers for cross-service money).

**(e) 🏆 Résumé line / interview talking point earned**
> *"Designed a partner-grade banking API — URI versioning with standard `Deprecation`/`Sunset` headers, Stripe-style idempotency keys for safe retries, stable pagination, and HMAC-signed outbound webhooks with replay protection and retries — all test-proven, including a concurrency-safe idempotency guard."*

**(f) ✅ You can now…**
- [ ] Version and deprecate an API gracefully.
- [ ] Make a money endpoint idempotent and explain the concurrency guard.
- [ ] Paginate with a stable envelope.
- [ ] Sign, deliver, and verify webhooks with replay protection.

**(g) 🃏 Flashcards** *(appended to `docs/flashcards.md`)*
- Q: Make a payment API retry-safe? · A: Idempotency-Key + key store; retry returns the stored result; unique PK guards concurrency.
- Q: Secure a webhook? · A: HMAC-SHA256 over timestamp.body + constant-time verify + reject stale timestamps (replay).
- Q: Webhook delivery semantics? · A: at-least-once → receivers must be idempotent.
- Q: Why not expose Spring's Page? · A: unstable internal JSON; own a PageResponse envelope.
- Q: Deprecate an endpoint? · A: Deprecation/Sunset/Link headers (RFC 8594) + a migration window.
> 🔁 **Revisit in ~6 steps** (Step 20 Outbox makes webhook/event delivery durable).

**(h) ✍️ One-line reflection:** *Which felt more "senior" to build — the idempotency guard or the webhook signature — and why?*

**(i) Sign-off** 🎉 Your API is now a contract a partner could integrate against with confidence: versioned, retry-safe, paginated, and cryptographically verifiable. Next: **Step 15 — API Gateway / BFF**, where a single front door fronts all the services. Onward! 🚀
