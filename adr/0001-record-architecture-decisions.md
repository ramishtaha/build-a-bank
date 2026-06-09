# ADR-0001: Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-06-09
- **Deciders:** Build-a-Bank (autonomous senior-engineer default)

## Context
This project makes many forks (versions, patterns, trade-offs) and runs autonomously — it must *not* pause to ask.
The Operating Contract requires that at every fork we "choose what a senior engineer would and record it (an ADR if
architectural)." We need a lightweight, consistent place to capture the *why* so future sessions (and learners) can
follow the reasoning and so decisions are reviewable.

## Decision
We use **Architecture Decision Records** (lightweight MADR style) in `adr/NNNN-title.md`, one file per significant
decision, append-only, numbered sequentially. Each records Context → Decision → Consequences. ADRs are referenced from
the lesson/step that makes the decision and (where relevant) from `VERSIONS.md` / `PROGRESS.md`.

## Consequences
- ✅ Decisions are discoverable and teachable (Domain 15: technical writing / design docs).
- ✅ A resuming session reconstructs intent without re-litigating settled choices.
- ⚠️ Superseded ADRs are kept (marked `Superseded by ADR-XXXX`) rather than deleted — history matters.
