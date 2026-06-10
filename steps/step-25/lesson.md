# Step 25 · SOLID & Clean Code — Refactoring a Smelly Service (with Tests as the Safety Net)
### Phase E — Design, Architecture & Testing Mastery 🟣 · Step 25 of 67 · **Phase E opener**

> *Phase E is about design. We start with the discipline you'll use forever: take real code with **smells** and
> refactor it toward **SOLID** — without changing what it does. Our target is the notification consumer, whose
> one `@KafkaListener` method counted, parsed JSON, deduped, built a message, published, and logged — a
> textbook **SRP** violation tightly coupled to concrete tech (**DIP** violation). We extract single-purpose
> collaborators and invert the idempotency dependency behind a **port** — and the unchanged tests prove
> behaviour is preserved.*

---

<a id="toc"></a>
## 🧭 The Six Movements of This Step

| | Movement | What happens |
|---|---|---|
| **A** | [🧭 Orient](#orient) | 30-second overview · skip-test · cheat card · why it matters · before you start |
| **B** | [🧠 Understand](#understand) | SOLID (esp. SRP & DIP) · code smells · refactoring discipline · ports-and-adapters |
| **C** | [🛠️ Build](#build) | spot the smells · extract collaborators · introduce a DIP port + adapter · keep tests green |
| **D** | [🔬 Prove](#prove) | the Verification Log — unchanged tests still green + new unit tests; §12.3 mutation |
| **E** | [🎓 Apply](#apply) | go deeper · interview prep · your-turn challenges |
| **F** | [🏆 Review](#review) | troubleshooting · resources · recap, flashcards & what's next |

---

<a id="orient"></a>

# A · 🧭 Orient

## 📋 This Step in 30 Seconds

| | |
|---|---|
| **Title** | SOLID & clean code — refactor a smelly service (SRP, DIP → ports-and-adapters, code smells), behaviour-preserving |
| **Step** | 25 of 67 · **Phase E — Design, Architecture & Testing Mastery** 🟣 · **Phase E opener** |
| **Effort** | ≈ 10 hours focused. A **refactor** — no new feature; the win is cleaner, more testable code. |
| **What you'll run this step** | **JVM + Maven**; **🐳 Docker** for the notification integration tests (Testcontainers Redpanda). The new unit tests need neither. |
| **Buildable artifact** | A refactored **notification** service: `TransferEventConsumer` becomes a thin orchestration depending on abstractions — a `TransferEventParser` (SRP), a `ProcessedEventStore` **port** + `InMemoryProcessedEventStore` **adapter** (DIP), a `Notification.from(event)` factory. New unit tests for the extracted pieces. The **existing integration tests are unchanged** and still pass. `step-25-start == step-24-end`. |
| **Verification tier** | 🟠 **Standard** — a refactor that keeps tests green (no money/security path touched). `./mvnw verify` green + the **unchanged** integration tests pass (behaviour preserved) + new unit tests + `smoke.sh`; a §12.3 mutation confirms the new port is exercised. |
| **Depends on** | **[Step 20](../step-20/lesson.md)** (the notification consumer we refactor), **[Step 7](../step-07/lesson.md)** (proxies/DI), and it sets up **[Step 26](../step-26/lesson.md)** (hexagonal). |

By the end you will be able to **name code smells**, apply **SOLID** (especially **SRP** and **DIP**), do a **behaviour-preserving refactor** with tests as the safety net, and explain **ports-and-adapters** (DIP in the large).

### ⏭️ Can You Skip This Step? (5-minute self-check)

If you can confidently do **all** of this, skim the 🛠️ Build and jump to **[Step 26 — Hexagonal architecture + DDD](../step-26/lesson.md)**.

- [ ] I can state the **SOLID** principles and give a real example of an SRP and a DIP violation.
- [ ] I can name common **code smells** (god method, feature envy, tight coupling) and the refactorings that fix them.
- [ ] I can do a **behaviour-preserving** refactor and explain why the tests mustn't change.
- [ ] I can apply **DIP** with a **port + adapter** and say what it buys (swappable, testable).
- [ ] I can explain why SOLID improves **testability**, not just aesthetics.

> [!TIP]
> Not 100%? Stay. "Tell me about SOLID with examples," "what's a code smell and how would you fix it," and "what does dependency inversion buy you" are perennial design interview questions.

## 📇 Cheat Card

> **What this step delivers (one sentence):** the notification consumer is refactored from a do-everything method into a thin orchestration of single-responsibility collaborators (parser, a dedup **port** + adapter, a notification factory) — proven safe because the unchanged integration tests still pass.

**Key commands** (Windows uses `.\mvnw.cmd`):

```bash
./mvnw -pl services/notification test     # unchanged integration tests + new unit tests
bash steps/step-25/smoke.sh
```

**The headline — before → after:**

```
BEFORE: onTransferCompleted(payload) { count; parse JSON; ConcurrentHashMap dedupe; build msg; publish; log }
                                        └────────── one method, many reasons to change (SRP✗), concrete deps (DIP✗)
AFTER:  parser.parse(payload) → processedEvents.markIfNew(id) → Notification.from(event) → hub.publish
        (TransferEventParser)   (ProcessedEventStore PORT + adapter)  (factory)            (SseHub)
```

**The one sentence to remember:** *Refactoring changes structure, not behaviour — so the tests don't change; SOLID's payoff is small, single-purpose pieces you can swap (DIP) and test in isolation.*

## 🎯 Why This Matters

Code is read and changed far more than written. A god-method coupled to concrete tech is hard to test, hard to change safely, and where bugs hide. SOLID — especially **SRP** (one reason to change) and **DIP** (depend on abstractions) — is the everyday toolkit for keeping a growing codebase soft, and "refactor this smelly class / explain SOLID" is a staple of senior interviews and real PRs.

## ✅ What You'll Be Able to Do

- Spot code smells and name the refactoring that fixes each.
- Apply **SRP** (extract collaborators) and **DIP** (port + adapter).
- Refactor **behaviour-preservingly**, with tests as the safety net.
- Explain how SOLID improves **testability** and **changeability**.

## 🧰 Before You Start

- **Prereqs:** bank builds green (`git describe` → `step-24-end`); Docker for the notification integration tests.
- **Connects to what you know:** the **notification consumer** (Step 20) is the target; **proxies/DI** (Step 7) underpin the port wiring; this is the SOLID groundwork for **hexagonal architecture** (Step 26) and the boundaries **ArchUnit** will enforce (Step 27).
- **Depends on:** Steps **20, 7**.

---

<a id="understand"></a>

# B · 🧠 Understand

## 🧠 The Big Idea — SOLID, and refactoring as a safe transformation

**SOLID** is five principles for changeable OO design:
- **S**ingle Responsibility — a class has one reason to change.
- **O**pen/Closed — open to extension, closed to modification (add behaviour without editing callers).
- **L**iskov Substitution — a subtype works wherever its base type is expected.
- **I**nterface Segregation — small, focused interfaces beat fat ones.
- **D**ependency Inversion — depend on **abstractions**, not concretions; high-level policy shouldn't depend on low-level detail.

**Refactoring** is changing *structure* without changing *behaviour*. The safety net is your **test suite**: if
the tests are green before and (unchanged) green after, the behaviour is preserved. That's why this step does
**not** edit the integration tests — they're the proof.

## 🧩 Pattern Spotlight — the smells in our consumer, and the fixes

`TransferEventConsumer.onTransferCompleted` had several smells:

| Smell | What it looked like | Refactoring |
|---|---|---|
| **God method / SRP✗** | one method: count + parse + dedupe + build + publish + log | **Extract** parser, dedup store, notification factory |
| **Feature envy / Law of Demeter** | `buildMessage(JsonNode)` reaching into the JSON tree | build the message **from a domain event** (`Notification.from`) |
| **Tight coupling / DIP✗** | direct `ObjectMapper` + inline `ConcurrentHashMap` | depend on a `TransferEventParser` and a `ProcessedEventStore` **port** |
| **Mixed levels** | wire-format parsing next to domain logic | separate the transport concern (parse) from the policy (dedupe → notify) |

## 🌱 Under the Hood: DIP with a port + adapter

A **port** is an interface owned by the code that *uses* it (here `ProcessedEventStore.markIfNew`). An
**adapter** implements it for a specific technology (`InMemoryProcessedEventStore`; tomorrow a Redis one). The
consumer now depends on the **abstraction**, so the idempotency mechanism is **swappable without changing the
consumer** (DIP) and **mockable in tests** — and adding a Redis adapter is *extension, not modification* (OCP).
This is "dependency inversion in the small"; Step 26 grows it into a full hexagon.

## 🛡️ Security Lens & 🧵 Thread-safety note

Behaviour is preserved, including the **Dead-Letter** path: the parser **throws** on a poison payload and the
consumer doesn't swallow it (Step 21), so it still routes to the DLT. The dedup adapter stays thread-safe
(`ConcurrentHashMap.newKeySet()`), now behind the port.

## 🕰️ Then vs. Now

The old advice "make it work, then make it right" still holds — but the modern discipline is **continuous,
test-backed refactoring**: small structural moves under a green suite, rather than big rewrites. And DIP is no
longer just "program to an interface" — it's the backbone of **ports-and-adapters / hexagonal** (Step 26),
which the team then **enforces** mechanically (ArchUnit, Step 27).

---

# B→C bridge: 🌳 files we'll touch (all in `services/notification`)

```
TransferEventConsumer.java        (refactor) → thin orchestration: parse → dedupe → notify
TransferEvent.java                (new)        a plain domain record (no JSON/Kafka)
TransferEventParser.java          (new)        the parsing concern (isolates Jackson; throws on poison)
ProcessedEventStore.java          (new)        the idempotency PORT (DIP)
InMemoryProcessedEventStore.java  (new)        the in-memory ADAPTER (Redis-ready)
Notification.java                 (edit)       + from(TransferEvent) factory (message in one place)
src/test/.../TransferEventParserTest.java          (new) unit test — no Kafka
src/test/.../InMemoryProcessedEventStoreTest.java  (new) unit test — no Kafka
# UNCHANGED: TransferEventConsumerKafkaTest, DeadLetterTest, NotificationControllerTest  ← the safety net
steps/step-25/{lesson.md, smoke.sh}
```

<a id="build"></a>

# C · 🛠️ Let's Build It — Step by Step

## 📦 Your Starting Point

`step-25-start == step-24-end`: 13 modules green, including the notification consumer with its smells.

## Sub-step 1 — extract the parsing concern (SRP)

🎯 `TransferEvent` (domain record) + `TransferEventParser.parse(payload)` (Jackson here, throws on poison). The consumer stops touching `JsonNode`.

## Sub-step 2 — invert the idempotency dependency (DIP)

🎯 `ProcessedEventStore.markIfNew(eventId)` (port) + `InMemoryProcessedEventStore` (adapter, the old `ConcurrentHashMap`). The consumer depends on the port.

## Sub-step 3 — a notification factory, and a thin consumer

🎯 `Notification.from(TransferEvent)` (message wording in one place). `TransferEventConsumer` becomes: `parse → markIfNew → from → publish` (one line each), depending on `TransferEventParser`, `ProcessedEventStore`, `SseHub`.

🔬 **Break-it-on-purpose:** run the **unchanged** `TransferEventConsumerKafkaTest`/`DeadLetterTest` — they pass, proving you changed structure, not behaviour. (If they'd failed, the refactor wasn't safe.)

## Sub-step 4 — unit-test the extracted pieces (the payoff)

🎯 `TransferEventParserTest` and `InMemoryProcessedEventStoreTest` — fast, no Kafka, no Spring. The refactor made these independently testable.

💾 **Commit:** `refactor(notification): Step 25 SOLID — extract parser/dedup port, thin consumer`

## 🎮 Play With It

```bash
./mvnw -pl services/notification test     # watch the unchanged integration tests + the new unit tests pass
```

🧪 **Little experiments:** sketch a `RedisProcessedEventStore implements ProcessedEventStore` — note that the consumer needs **zero** changes (DIP/OCP). Mock the `ProcessedEventStore` in a consumer test to simulate a duplicate without Kafka.

## 🏁 The Finished Result

`step-25-end`: a cleaner notification service — a thin consumer over single-purpose, abstraction-backed collaborators — with identical behaviour. **✅ Definition of Done:** the unchanged integration tests pass, the new unit tests pass, `./mvnw verify` is green, `bash steps/step-25/smoke.sh` passes, and you've committed/tagged `step-25-end`.

---

<a id="prove"></a>

# D · 🔬 Prove It Works — Verification Log

> **Tier: 🟠 Standard** (behaviour-preserving refactor, no money/security path). The decisive proof is the
> **unchanged** integration tests still passing. Real output below; Docker used (Testcontainers Redpanda).

**1 · The notification suite — unchanged integration tests + new unit tests, all green:**

```
[INFO] Tests run: 1, … -- in com.buildabank.notification.DeadLetterTest                 (UNCHANGED — DLT still works)
[INFO] Tests run: 1, … -- in com.buildabank.notification.TransferEventConsumerKafkaTest  (UNCHANGED — exactly-once effect preserved)
[INFO] Tests run: 2, … -- in com.buildabank.notification.NotificationControllerTest      (UNCHANGED)
[INFO] Tests run: 2, … -- in com.buildabank.notification.TransferEventParserTest          (NEW — parsing in isolation)
[INFO] Tests run: 1, … -- in com.buildabank.notification.InMemoryProcessedEventStoreTest  (NEW — dedup port adapter)
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
The four pre-existing integration tests passing **without modification** is the refactor's proof: behaviour preserved.

**2 · §12.3 Mutation sanity-check (prove the new port is exercised).** Made `InMemoryProcessedEventStore.markIfNew` always return `true` (dedup broken) and re-ran:

```
[ERROR] InMemoryProcessedEventStoreTest.firstSightingIsNew_repeatIsADuplicate:19
Expecting value to be false but was true
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
```
→ The unit test catches a broken dedup adapter (and the exactly-once integration test would too). **Reverted**; green again.

**3 · `smoke.sh`** — `bash steps/step-25/smoke.sh` re-ran the whole notification suite (unchanged integration tests + new unit tests) on real Redpanda → `✅ Step 25 smoke test PASSED — refactor preserved behaviour (SRP + DIP applied)`.

**4 · Build** — full-repo `./mvnw verify` → BUILD SUCCESS (13 modules). *(Clean-room/extended checks skipped per §12 at Standard tier — no critical path touched; the refactor's behaviour is pinned by the unchanged integration suite.)*

**§12.8 honesty:** this is a partial ports-and-adapters move (one port) — the full hexagonal restructure is Step 26. Only `notification` was refactored; other services' smells are addressed as they're touched, not in a blanket rewrite.

---

<a id="apply"></a>

# E · 🎓 Apply

## 🚀 Go Deeper (Optional)

<details><summary>Is a port over-engineering for one adapter?</summary>For a single, stable implementation it can be (YAGNI). Here it's justified: idempotency is a known future swap (in-memory → Redis, Step 21's pattern), the port makes the consumer testable without Kafka, and it's the seed of the Step-26 hexagon. Introduce abstractions when you have a concrete reason (a second implementation, a test seam), not reflexively.</details>

<details><summary>SRP vs anemic over-splitting</summary>SRP isn't "one method per class." It's "one reason to change." We split parsing (changes when the wire format changes) from idempotency (changes when the dedup store changes) from notification-building (changes when the message changes) — genuinely different axes of change. Don't shred cohesive logic into noise.</details>

## 💼 Interview Prep

1. **Explain SOLID with a real example.** *e.g. SRP: a Kafka listener that parsed + deduped + built + published had many reasons to change; we extracted a parser, a dedup port, and a factory. DIP: the consumer now depends on a `ProcessedEventStore` interface, not a `ConcurrentHashMap`, so the store is swappable/testable.* **(Common.)**
2. **What's a code smell — name a few and their fixes?** *God method → extract method/class; feature envy → move behaviour to the data's owner; tight coupling → depend on an interface (DIP); primitive obsession → introduce a type. Smells are hints, not rules.*
3. **What does Dependency Inversion buy you?** *Swappable implementations without touching callers, isolated unit tests (mock the port), and decoupling policy from detail — the basis of ports-and-adapters.*
4. **How do you refactor safely?** *Behaviour-preserving steps under a green test suite; don't change tests and code together. If there are no tests, characterize first (write tests for current behaviour), then refactor.*
5. **(Gotcha) When is an abstraction NOT worth it?** *When there's one stable implementation and no test/seam need — YAGNI. Add it when a real second case or a testability pain appears.*

## 🏋️ Your Turn: Practice & Challenges

- **Quick:** add a consumer unit test that mocks `ProcessedEventStore` to return `false` (duplicate) and asserts `hub.publish` is never called — now possible *without* Kafka, thanks to DIP.
- **Quick:** find one more smell elsewhere (e.g. demand-account's `TransferService` breadth) and write down the refactoring you'd apply (don't do it yet — Step 26/27).
- 🎯 **Stretch (reference solution in `solutions/step-25/`):** implement a `RedisProcessedEventStore implements ProcessedEventStore` (reusing Step 21's Redis) and wire it via a profile/`@ConditionalOnProperty` — proving the consumer needs no change (DIP/OCP). Test it on Testcontainers Redis.

---

<a id="review"></a>

# F · 🏆 Review

## 🩺 Stuck? Troubleshooting & Fixes

- **An integration test changed/broke during the refactor.** Then it wasn't behaviour-preserving — your structural move altered behaviour. Revert and move in smaller steps; the unchanged test is the contract.
- **The DLT test fails after the refactor.** You swallowed the parse exception (e.g. caught it in the parser). The parser must **throw** on poison so the consumer propagates it → DLT (Step 21).
- **Two beans of the port type.** Only one adapter (`InMemoryProcessedEventStore`) should be a `@Component`; if you add another (Redis), gate it with a profile/condition so exactly one is active.
- **Reset:** `git checkout step-25-end`.

## 📚 Learn More & Glossary

- Martin, *Clean Code* & *Clean Architecture* (SOLID, smells); Fowler, *Refactoring* (the catalog + "refactor under tests"); Cockburn, *Hexagonal Architecture* (ports-and-adapters).
- **Glossary:** *SOLID (SRP/OCP/LSP/ISP/DIP)*, *code smell*, *refactoring*, *port*, *adapter*, *feature envy*, *Law of Demeter*, *DRY/KISS/YAGNI*.

## 🏆 Recap & Study Notes

**(a) Key points:** Refactoring changes **structure, not behaviour** — so the tests don't change; they're the safety net. We fixed an **SRP** god-method by extracting a parser, a dedup **port** (+adapter), and a factory, and inverted a concrete dependency (**DIP**). The payoff is small, swappable, independently-testable pieces — and the seed of hexagonal architecture.

**(b) Key terms:** SOLID, SRP, DIP, OCP, code smell, refactoring, port, adapter, feature envy, YAGNI.

**(c) 🧠 Test Yourself:** ① What's the difference between refactoring and rewriting? ② Give an SRP and a DIP fix from this step. ③ Why didn't we change the integration tests? ④ What does a port + adapter buy? ⑤ When is an abstraction over-engineering? <details><summary>Answers</summary>① Refactor preserves behaviour (tests unchanged & green); rewrite may change it. ② SRP: extract parser/factory/dedup; DIP: consumer depends on `ProcessedEventStore`, not a map. ③ They're the proof behaviour is preserved. ④ Swappable implementations + testability without changing callers. ⑤ One stable impl, no test/seam need (YAGNI).</details>

**(d) 🔗 How this connects:** cleans the Step-20 consumer; the port is dependency inversion in the small. **Next: Step 26** — **hexagonal architecture + DDD** (grow this into ports-and-adapters layering), then Step 27 (**Spring Modulith + ArchUnit** to *enforce* the boundaries), Step 28 (code-quality gates), and the Phase-E capstone (hexagonal + ArchUnit + mutation testing).

**(e) 🏆 Résumé line:** *"Refactored a service to SOLID — single-responsibility collaborators and a dependency-inverted port/adapter — behaviour-preservingly, with the test suite as the safety net."*

**(f) ✅ You can now:** name and fix code smells · apply SRP and DIP · refactor safely under tests · explain ports-and-adapters.

**(g) 🃏 Flashcards** appended to `docs/flashcards.md` · 🔁 revisit DIP/ports at Step 26 (hexagonal) and Step 27 (ArchUnit enforcement).

**(h) ✍️ One-line reflection:** *Which smell in the bank bothers you most — and what's the smallest safe refactor that would fix it?*

**(i)** 🎉 Phase E is open — design discipline begins. Next: hexagonal architecture.
