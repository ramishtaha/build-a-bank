# 📚 Lesson Enrichment Tracker

A documentation-only pass that brings each step's `lesson.md` up to the full **MASTER_PROMPT §8.1** Hands-On Build Playbook bar (the DEPTH GATE in §8): every build sub-step carries the complete micro-anatomy — 🎯 Goal → 📁 Path → ⌨️ complete verbatim code → 🔍 line-by-line → 💭 under-the-hood → 🔮 predict → ▶️ Run & See (fenced ✅ expected output) → ✋ checkpoint → 💾 commit → ⚠️ pitfall.

**Code is FROZEN.** This pass never modifies code, tests, poms, configs, scripts, or git tags — only `steps/step-NN/*` lesson aids and files under `docs/`. Verified facts (Verification Logs, recorded run numbers) are preserved.

**Enrichment range: steps 11 → 30 only.** One step per session.

| Step | Status | Notes |
|---|---|---|
| 11 | ✅ enriched | Concurrency lab. Build expanded to 10 full-micro-anatomy sub-steps with verbatim `step-11-end` code (pom, `Balance` + 4 impls, both test classes, smoke.sh). No `requests.http`/seed — the module is pure-JVM JUnit with **no endpoints/services**. Not a multiple of 5 → no Cumulative Review. Flashcards already present in `docs/flashcards.md`. Interview Q&As + Key Terms appended to `docs/interview-bank.md` / `docs/glossary.md`. Verification Log preserved intact. |
| 12 | ✅ enriched | Phase-B finale: the `demand-account` service. Build rewritten to **12 full-micro-anatomy sub-steps** (module wiring + root-pom diff, config + compose, Flyway V1, `Account`/`EntryDirection`/`InsufficientFundsException`, `LedgerEntry`/`AuditEntry`, 3 repositories incl. `FOR UPDATE` + unsafe bulk update, `TransferService` safe+unsafe, `AuditService`/`PropagationDemoService` REQUIRES_NEW, web layer + main class, `ContainersConfig`+`TransferServiceTest`, the capstone `ConcurrentTransferTest`, then optimistic/propagation/web-slice/live-HTTP tests) — **all code pasted verbatim from `step-12-end`** (later steps may refactor; the tag is the truth). 2,622 lines / 148 fenced blocks. `requests.http` already present (endpoints exposed) — not re-created. `docs/flashcards.md` already had Step 12 — untouched. Interview Q&As (7) + Key Terms appended to `interview-bank.md` / `glossary.md`. Verification Log preserved intact (11 tests, capstone both ways, §12.3 mutation 17/20, clean-room). Not a multiple of 5 → existing Phase-B Cumulative Review (Steps 8–12) retained and lightly extended (8 questions). Nothing unrecoverable. |
| 13 | ✅ enriched | Phase-C opener: the demand-account **web layer** (Spring MVC deep). Build rewritten to **8 full-micro-anatomy sub-steps** (0 springdoc pom diff · 1 `GlobalExceptionHandler` domain handlers, replacing the deleted `ApiExceptionHandler` placeholder shown verbatim · 2 validation override + **whole file** · 3 `OpenApiConfig`/Swagger UI · 4 `RequestIdFilter` · 5 `TimingInterceptor`+`WebConfig` · 6 `TransferControllerTest` slice diff+whole-file · 7 `DemandAccountIntegrationTest` live-HTTP diff+whole-file + `verify`) — **all code pasted verbatim from `step-13-end`** (HEAD has since refactored the web pkg heavily — payments/webhooks/security; the tag is the truth). Added the existing **unchanged web layer** (`TransferController` + 4 DTOs) verbatim to Your Starting Point for context. **1,636 lines / 108 fenced-delimiter lines** (was 812/58). `requests.http` already present (endpoints exposed; seeds via `POST /api/accounts`) — not re-created; no separate seed file needed. `docs/flashcards.md` already had Step 13 (also 14/15) — untouched. Interview Q&As (6 + STAR seed) + Key Terms (24) appended to `interview-bank.md` / `glossary.md`. Verification Log preserved intact (13 tests, live OpenAPI/Swagger/Problem+JSON, §12.3 mutation 422→200, clean-room). Not a multiple of 5 → no Cumulative Review. Nothing unrecoverable. |
| 14 | ✅ enriched | Phase-C: API design — URI versioning, idempotency, pagination & signed webhooks. Build rewritten to **9 full-micro-anatomy sub-steps** (0 V2 migration + `IdempotencyRecord` entity + repository · 1 `IdempotentTransferService` lookup-or-execute · 2 pagination plumbing — `LedgerEntryRepository` paged-finder diff + `TransferService.entriesOf` diff · 3 `PageResponse` + `LedgerEntryResponse` envelopes · 4 `WebhookSigner` HMAC+replay-window + `WebhookSignerTest` · 5 `WebhookSender` bounded retries · 6 `WebhookPublisher` config-gated · 7 `TransferController` /api/v1 + deprecation — diff **and** whole file · 8 tests `IdempotencyTest` whole-file + `TransferControllerTest`/`DemandAccountIntegrationTest` diffs + ADR-0006) — **all code pasted verbatim from `step-14-end`** (HEAD has since refactored the web/webhook pkg — payments/security; the tag is the truth). Added the unchanged `TransferRequest`/`TransferResponse` DTOs to Your Starting Point for context. **1,771 lines / 108 fenced-delimiter lines** (was 741/46). Ran the **pure-JUnit** `WebhookSignerTest`+`WebhookDeliveryTest` live (no Docker) → real fresh output (6 tests, random ports 58129/58131, genuine 500→retry→200) folded into Run-and-See §4-5 and Verification Log §3. `requests.http` already present (endpoints exposed; seeds via `POST /api/accounts`) — not re-created. `docs/flashcards.md` already had Step 14 — untouched. Interview Q&As (7 + STAR seed) + Key Terms (23) appended to `interview-bank.md` / `glossary.md`. Verification Log preserved intact (25 tests, idempotent retry, signed delivery+replay, §12.3 mutation, clean-room). Not a multiple of 5 → no Cumulative Review. Nothing unrecoverable. |
| 15 | ⬜ pending | multiple of 5 → ensure Cumulative Review (steps 1-15) |
| 16 | ⬜ pending | |
| 17 | ⬜ pending | |
| 18 | ⬜ pending | |
| 19 | ⬜ pending | |
| 20 | ⬜ pending | multiple of 5 → ensure Cumulative Review (steps 1-20) |
| 21 | ⬜ pending | |
| 22 | ⬜ pending | |
| 23 | ⬜ pending | |
| 24 | ⬜ pending | |
| 25 | ⬜ pending | multiple of 5 → ensure Cumulative Review (steps 1-25) |
| 26 | ⬜ pending | |
| 27 | ⬜ pending | |
| 28 | ⬜ pending | |
| 29 | ⬜ pending | |
| 30 | ⬜ pending | multiple of 5 → ensure Cumulative Review (steps 1-30) |

**Next step to enrich: 15**

When every step 11–30 is marked ✅, create an empty `STOP` file in the repo root and halt.
