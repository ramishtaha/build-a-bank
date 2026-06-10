# 📚 Lesson Enrichment Tracker

A documentation-only pass that brings each step's `lesson.md` up to the full **MASTER_PROMPT §8.1** Hands-On Build Playbook bar (the DEPTH GATE in §8): every build sub-step carries the complete micro-anatomy — 🎯 Goal → 📁 Path → ⌨️ complete verbatim code → 🔍 line-by-line → 💭 under-the-hood → 🔮 predict → ▶️ Run & See (fenced ✅ expected output) → ✋ checkpoint → 💾 commit → ⚠️ pitfall.

**Code is FROZEN.** This pass never modifies code, tests, poms, configs, scripts, or git tags — only `steps/step-NN/*` lesson aids and files under `docs/`. Verified facts (Verification Logs, recorded run numbers) are preserved.

**Enrichment range: steps 11 → 30 only.** One step per session.

| Step | Status | Notes |
|---|---|---|
| 11 | ✅ enriched | Concurrency lab. Build expanded to 10 full-micro-anatomy sub-steps with verbatim `step-11-end` code (pom, `Balance` + 4 impls, both test classes, smoke.sh). No `requests.http`/seed — the module is pure-JVM JUnit with **no endpoints/services**. Not a multiple of 5 → no Cumulative Review. Flashcards already present in `docs/flashcards.md`. Interview Q&As + Key Terms appended to `docs/interview-bank.md` / `docs/glossary.md`. Verification Log preserved intact. |
| 12 | ✅ enriched | Phase-B finale: the `demand-account` service. Build rewritten to **12 full-micro-anatomy sub-steps** (module wiring + root-pom diff, config + compose, Flyway V1, `Account`/`EntryDirection`/`InsufficientFundsException`, `LedgerEntry`/`AuditEntry`, 3 repositories incl. `FOR UPDATE` + unsafe bulk update, `TransferService` safe+unsafe, `AuditService`/`PropagationDemoService` REQUIRES_NEW, web layer + main class, `ContainersConfig`+`TransferServiceTest`, the capstone `ConcurrentTransferTest`, then optimistic/propagation/web-slice/live-HTTP tests) — **all code pasted verbatim from `step-12-end`** (later steps may refactor; the tag is the truth). 2,622 lines / 148 fenced blocks. `requests.http` already present (endpoints exposed) — not re-created. `docs/flashcards.md` already had Step 12 — untouched. Interview Q&As (7) + Key Terms appended to `interview-bank.md` / `glossary.md`. Verification Log preserved intact (11 tests, capstone both ways, §12.3 mutation 17/20, clean-room). Not a multiple of 5 → existing Phase-B Cumulative Review (Steps 8–12) retained and lightly extended (8 questions). Nothing unrecoverable. |
| 13 | ⬜ pending | next |
| 14 | ⬜ pending | |
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

**Next step to enrich: 13**

When every step 11–30 is marked ✅, create an empty `STOP` file in the repo root and halt.
