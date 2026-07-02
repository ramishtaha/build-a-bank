# Step 30 audit - swe:6 pedagogy:4 adhd:3 structure:5 - thinBuild:true

## Strengths

- Technical accuracy is grounded in real repo code: the API paths (`/bank/api/accounts/...` vs `/bank/api/v1/transfers` mix), the `enabled: token !== null` query guard, the gateway `StripPrefix=1` notification route, and the invalidation keys all match what actually exists at tag `step-30-end` (verified against `frontend/src/accounts/queries.ts`, `frontend/src/api/client.ts`, `gateway/src/main/resources/application.yml`).
- The Verification Log is honest and specific: tier stated (Standard), real pasted outputs, a genuine mutation test (drop the `to` rule, show the failing test output, revert), and an explicit §12.8 honesty paragraph naming exactly what could NOT be verified (live browser SSE, servlet-gateway buffering, CORS) instead of pretending it was.
- Strong orientation and retrieval scaffolding: 30-seconds table with effort/tier/depends-on, skip-test, cheat card with a one-line flow diagram, prior-step callbacks (14/20/21/29), good misconception coverage (`z.coerce` for string inputs, jsdom's missing `EventSource`), and a test-yourself aligned to the step's actual core ideas.

## Missing spine

- Build sub-step micro-anatomy for ALL 5 sub-steps: complete code with file-path headers, line-by-line explanation, under-the-hood, run-and-see (exact command + expected output + common-wrong-output), checkpoint, per-sub-step commit. Only Goal lines, 2 pitfalls, 1 predict, and 1 break-it exist.
- What-we-will-build Mermaid diagram at the open of C (the files tree sits in a "B→C bridge" before the build anchor; no build-opening diagram).
- Closing sequence diagram of the flow built (no sequence diagram anywhere in the lesson).
- Analogy in the Understand big idea (diagram present, analogy absent).
- Inline flashcards (3-5) in the recap — line 358 only points to `docs/flashcards.md` (the cards do exist there under "Step 30").
- Session plan / named sittings, time-boxes per movement and sub-step, re-entry lines (ADHD contract items — absent entirely for a ~16-hour step).
- `requests.http` absent from `steps/step-30/` (only `lesson.md` and `smoke.sh` present).

## Findings

### F1: Build is a stub outline — micro-anatomy missing for every sub-step

**Severity:** high
**Lens:** structure
**Location:** C · Build, Sub-steps 1–5 (lines 182–210)
**needsRun:** true
**Issue:** Each sub-step is 1–4 lines of Goal-only prose. No code appears anywhere in the build: no `queries.ts`, no `AccountPanel.tsx`, no `TransferForm.tsx`, no SSE hook, no gateway YAML, no test wrapper. There is no line-by-line explanation, no under-the-hood, no run-and-see, and no checkpoint for any sub-step. A nervous beginner at 11pm cannot complete this from the lesson — they must reverse-engineer the repo. The sacred-build contract is violated for 5 of 5 sub-steps.
**Fix:** Expand each sub-step to full micro-anatomy using the real sources at tag `step-30-end`: Sub-step 1 = `main.tsx` diff adding `QueryClientProvider` (diff view); Sub-step 2 = full `frontend/src/accounts/queries.ts` + the `api/client.ts` additions (diff view) with line-by-line notes on `queryKey`, `enabled`, `invalidateQueries`; Sub-step 3 = `AccountPanel.tsx`; Sub-step 4 = `TransferForm.tsx` with the Zod schema explained token-by-token; Sub-step 5 = `useNotificationStream.ts`, `LiveNotifications.tsx`, and the gateway `application.yml` route (diff view). Add a run-and-see after each sub-step (run the relevant `npm test -- <file>` slice or `npm run build`) with real captured output.

### F2: DoD demands 15 passing tests the build never writes

**Severity:** high
**Lens:** swe
**Location:** Sub-steps 1–5 vs Verification Log item 2 (lines 249–261) and Definition of Done (line 228)
**needsRun:** true
**Issue:** The Verification Log and DoD require 15 tests across 6 files (`client.test.ts`, `useNotificationStream.test.ts`, `AccountPanel.test.tsx`, `TransferForm.test.tsx`, etc.) plus `test/renderWithProviders.tsx` and the `setup.ts` EventSource stub — but no sub-step instructs the learner to write any of them. Following the build as written produces zero of the tests the step is graded on; the §12.3 break-it in Sub-step 5 references a test the learner never created.
**Fix:** Add test authoring into the micro-anatomy: `renderWithProviders.tsx` + the `setup.ts` EventSource stub as their own sub-step (or fold into Sub-step 2), then the client tests with Sub-step 2, `AccountPanel.test.tsx` with Sub-step 3, `TransferForm.test.tsx` with Sub-step 4, and the controllable-EventSource hook test with Sub-step 5 — each with full code from `step-30-end` and a run-and-see showing the growing test count.

### F3: No session plan for a 16-hour step

**Severity:** high
**Lens:** adhd
**Location:** A · Orient (30-seconds table, line 35; no session section anywhere)
**needsRun:** false
**Issue:** Effort is stated as "≈ 16 hours focused" with no decomposition into sittings. The ADHD contract requires 6–10 planned sittings of ~2–3h with named save points; a learner has no way to know where a safe stopping point is.
**Fix:** Add a "🗓️ Session plan" block at the end of Orient, e.g.: Sitting 1 (~2h) Orient + Understand, save point: knowledge-check done; Sitting 2 (~2.5h) Sub-steps 1–2 (hooks + client), save point: commit `wip: query hooks`; Sitting 3 (~2.5h) Sub-step 3 + its test; Sitting 4 (~3h) Sub-step 4 (form + tests); Sitting 5 (~2.5h) Sub-step 5 (SSE + gateway route); Sitting 6 (~2h) Play-with-it live run; Sitting 7 (~1.5h) Prove + Apply + Review. Name the file/test that must be green at each save point.

### F4: No first win and near-zero reward loop inside the build

**Severity:** high
**Lens:** adhd
**Location:** Sub-steps 1–5 (lines 182–210); first runnable moment is Play With It (line 214)
**needsRun:** true
**Issue:** Nothing visibly runs until after all five sub-steps are done. Sub-step 1 (`npm install`) has no verification command; sub-steps 2–5 have no run-and-see. The learner types for hours (once code exists per F1) with no feedback — the highest-risk pattern for ADHD abandonment.
**Fix:** Give Sub-step 1 an immediate win within ~10 minutes: after installing and wrapping `QueryClientProvider`, run `npm run dev` and `npm test` to show everything still green, with real output pasted. Then end every sub-step with a run-and-see (a targeted `npx vitest run src/accounts/queries...`, a dev-server screenshot description, or `npm run build`), each with real captured output.

### F5: No checkpoints, re-entry lines, or commit cadence

**Severity:** medium
**Lens:** adhd
**Location:** Sub-steps 1–5; single commit at line 210
**needsRun:** false
**Issue:** The only commit in a multi-day build is at the very end of Sub-step 5, and there are no "stopping here?" re-entry lines. A learner who stops mid-step returns with no anchor for what works and what's next.
**Fix:** After each sub-step add a checkpoint box: "🛑 Stopping here? You have <X> working (hooks compile / panel renders / form validates). Next session starts at Sub-step <N+1>; first action: open `<file>`." Add a per-sub-step commit line (e.g., `feat(frontend): step-30 wip — query hooks`, `... — transfer form`) so the final tag commit is the fifth, not the first.

### F6: Missing build-opening diagram and closing sequence diagram

**Severity:** medium
**Lens:** structure
**Location:** "B→C bridge" (lines 159–172) and end of C (after Play With It, line 226)
**needsRun:** false
**Issue:** The contract says the build opens with a what-we-will-build Mermaid diagram plus the files tree, and closes with a sequence diagram of the flow built. The files tree exists but sits before the `#build` anchor in a bridge section; there is no build-opening diagram and no sequence diagram anywhere.
**Fix:** Move the files tree just after "Your Starting Point" and add above it a small Mermaid flowchart (DashboardPage → AccountPanel/TransferForm/LiveNotifications → hooks → gateway → services). After Play With It, add a Mermaid `sequenceDiagram`: User→TransferForm (Zod validate)→useTransfer (POST /bank/api/v1/transfers + Idempotency-Key)→gateway→demand-account→Kafka→notification→SSE `transfer` event→LiveNotifications, plus `onSuccess → invalidate → refetch` back into AccountPanel.

### F7: No time-boxes per movement or sub-step

**Severity:** medium
**Lens:** adhd
**Location:** TOC table (lines 14–21) and every sub-step heading
**needsRun:** false
**Issue:** Only the whole-step effort (≈16h) is stated. There is no per-movement or per-sub-step estimate, so a learner cannot plan a sitting or notice they are stuck.
**Fix:** Add a time column to the six-movements table (A ~30min, B ~1.5h, C ~10h, D ~1h, E ~1.5h, F ~45min) and a "· ~Xh" suffix on each sub-step heading (e.g., "Sub-step 4 — the transfer form (RHF + Zod) · ~3h"). Label Go Deeper items with "~10 min each".

### F8: Interactivity toolkit nearly absent from the build

**Severity:** medium
**Lens:** pedagogy
**Location:** C · Build (one predict at line 190, one break-it at line 208; no knowledge-checks, type-it-yourself, or you-are-here markers)
**needsRun:** false
**Issue:** One predict-then-run and one break-it across a 5-sub-step build; zero knowledge-checks and no visible-progress markers. There is also no scaffold fading — every sub-step is at the same (zero-code) altitude, so nothing shifts toward type-it-yourself.
**Fix:** Add "📍 You are here: Sub-step X of 5" under each heading. Add a knowledge-check after Understand ("Which of these is server state: the ledger page? the form's amount field? the modal-open flag?"). Add a predict before Sub-step 5 ("if the notification service restarts, what does EventSource do?" — auto-reconnect). Once F1's code exists, make Sub-step 5 partially type-it-yourself (give the hook skeleton, have the learner fill the `addEventListener`/cleanup lines) so scaffolding fades across the build.

### F9: Big Idea has a diagram but no analogy

**Severity:** low
**Lens:** pedagogy
**Location:** B · Understand, "The Big Idea" (lines 104–123)
**needsRun:** false
**Issue:** The contract's big-idea trio is idea + diagram + analogy; the analogy is missing, and it's the concept (cache/invalidate) that benefits most from one.
**Fix:** Add one sentence after the first paragraph, e.g.: "Think of the query cache as a hotel concierge: components ask the concierge (never the back office directly); the concierge answers instantly from its notes, and when told 'room 4 changed' (invalidation) it re-checks with the back office before answering again."

### F10: Flashcards not inline in the recap

**Severity:** low
**Lens:** structure
**Location:** F · Review, recap item (g) (line 358)
**needsRun:** false
**Issue:** The recap only points to `docs/flashcards.md`. The contract requires 3–5 flashcards in the lesson itself. (The cards do exist in `docs/flashcards.md` under "Step 30" — this is a copy, not an authoring task.)
**Fix:** Inline 4 of the existing Step-30 Q/A pairs from `docs/flashcards.md` (server vs UI state; mutation → invalidate; why `z.coerce.number()`; SSE vs WebSocket) as a bullet list under (g), keeping the pointer to the full deck.

### F11: requests.http missing from the step folder

**Severity:** low
**Lens:** swe
**Location:** `steps/step-30/` (contains only `lesson.md` and `smoke.sh`)
**needsRun:** false
**Issue:** The step adds a new gateway route (`/notifications/**`) and exercises transfer + accounts through the gateway, but ships no `requests.http` for manually poking those endpoints as other steps do.
**Fix:** Add `steps/step-30/requests.http` with: auth login (capture token), `GET http://localhost:8080/bank/api/accounts/ACC-A` with `Authorization: Bearer`, `POST http://localhost:8080/bank/api/v1/transfers` with `Idempotency-Key: {{$guid}}`, and a commented `GET http://localhost:8080/notifications/api/notifications/stream` noting `Accept: text/event-stream` and that an HTTP client shows the raw event frames.
