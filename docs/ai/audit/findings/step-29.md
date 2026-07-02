# Step 29 audit - swe:5 pedagogy:3 adhd:3 structure:5 - thinBuild:true

## Strengths

- Verification Log is genuinely evidenced for its core items: real pasted output for `npm run build`, `npm test`, lint, and the gateway routing/CORS tests, plus an honest §12.3 mutation with the actual failing-test trace and an explicit §12.8 sandbox-limits disclosure (no browser).
- Orient/Understand framing is accurate and repo-verified: the Vite ESM-dev/Rollup-prod explanation, the no-StripPrefix rationale for the auth route, deny-by-default CORS, and the localStorage-vs-httpOnly-vs-in-memory JWT trade-off (with a forward pointer to Step 32 hardening) all match the committed gateway code.
- Strong retrieval and interview layer: test-yourself items align with the stated outcomes, 5 solid interview Q&A, and misconception-targeting content (lockfile-vs-package.json predict, CORS preflight) that hits real beginner confusions.

## Missing spine

- Per-sub-step build micro-anatomy: complete code blocks, line-by-line explanation, under-the-hood, predict-then-run (only sub-step 1 has one), run-and-see with expected output, checkpoint, and per-sub-step commit are absent for all six sub-steps (only the Goal line exists).
- Sequence diagram of the built flow at build close.
- What-we-will-build Mermaid diagram at build open (the flowchart lives back in Understand; Build opens with only the starting point; the files tree is present in the B→C bridge).
- Flashcard content in the recap — only a pointer to `docs/flashcards.md` (which holds 6 cards for this step, itself above the 3-5 range).
- Explicit analogy in the Big Idea (only a one-line Spring-DI aside inside Pattern Spotlight).
- `requests.http` in `steps/step-29/` (smoke.sh exists; requests.http does not), despite the step adding new gateway HTTP surface.

## Findings

### F1: Build sub-steps are stubs — zero code for ~16 files
**Severity:** high
**Lens:** structure
**Location:** C · Build, Sub-steps 1-6 (lines 194-224)
**needsRun:** false
**Issue:** Every sub-step is a single Goal paragraph. Not one line of the SPA or gateway code being taught appears in the lesson: no `package.json`, `tsconfig.json`, `vite.config.ts`, `client.ts`, `AuthContext.tsx`, `ProtectedRoute.tsx`, pages, `main.tsx`/`App.tsx`, test setup, test files, gateway `application.yml` diff, or `GatewayCorsConfig.java`. A nervous beginner making the JVM→TS stack shift cannot write these files from one-paragraph descriptions — the sacred-build contract is violated for every sub-step.
**Fix:** Expand each sub-step to full micro-anatomy by pulling the complete, already-committed step-29 file contents from the repo (`frontend/src/**`, `frontend/*.{json,ts,js,html}`, `gateway/src/main/resources/application.yml` as a diff, `gateway/src/main/java/com/buildabank/gateway/GatewayCorsConfig.java`, `GatewayRoutingTest` additions as a diff) into code blocks with file-path header comments and all imports, each followed by a line-by-line explanation and an under-the-hood note. No new code needs writing — it exists at tag step-29-end.

### F2: No run-and-see, checkpoint, or commit inside any sub-step
**Severity:** high
**Lens:** structure
**Location:** C · Build, Sub-steps 1-6 (lines 194-224)
**needsRun:** true
**Issue:** The build contains no commands to run and no expected output anywhere between "Your Starting Point" and "Play With It". There are no checkpoints and only one commit (after sub-step 6). The learner types (or would type) for ~10+ hours with nothing to execute and no way to confirm progress.
**Fix:** Add to every sub-step: an exact command (`npm install` + `npm run dev` for 1; `npm run build` for 2; `npx vitest run` for 3, 4, 6; `./mvnw -pl gateway test` for 5), the real pasted expected output, one common-wrong-output with its cause, a one-line checkpoint ("you now have ..."), and a per-sub-step commit message. Run each command against the step-29 tree to capture truthful output — do not synthesize it.

### F3: 16-hour step with no session plan, time-boxes, or re-entry support
**Severity:** high
**Lens:** adhd
**Location:** A · Orient "This Step in 30 Seconds" (line 36) and C · Build throughout
**needsRun:** false
**Issue:** Effort is stated as ≈16 hours but there is no sittings plan, no per-movement or per-sub-step time-box, no named save points, and no re-entry lines. An ADHD learner has no way to plan stops or resume without re-reading.
**Fix:** Add a "Plan your sittings" table after Before You Start: e.g., S1 Orient+Understand (2h), S2 sub-steps 1-2 (2.5h), S3 sub-step 3 (2h), S4 sub-step 4 (2.5h), S5 sub-step 5 (2.5h), S6 sub-step 6 + break-it (2.5h), S7 Prove+Apply+Review (2h), each with a named save point (the sub-step commit from F2). Add a time-box to each sub-step heading ("~2h") and a re-entry line at each checkpoint: "Stopping here? You have X working. Next session: sub-step N, first action: open <file> / run <command>."

### F4: Stack-shift jargon never explained — new tokens undefined on first appearance
**Severity:** high
**Lens:** pedagogy
**Location:** C · Build, Sub-steps 1-4 (lines 194-210)
**needsRun:** false
**Issue:** This is the course's first non-JVM step, yet TSX syntax, hooks (`useState`/`useContext`), `import.meta.env` and the `VITE_` env-prefix convention, `tsconfig` strict flags, ESLint flat config, jsdom, and controlled forms are used in goal text without ever being defined — the contract requires every new token explained at first appearance, and there is no code for the explanations to attach to.
**Fix:** As part of F1's expansion, annotate every first-appearance token in the line-by-line sections, and open sub-step 1 with a short "Your first .tsx file, for Java developers" primer (component = function returning JSX; hooks = stateful functions with call-order rules; `import.meta.env.VITE_*` = build-time env vars, only `VITE_`-prefixed keys reach the browser).

### F5: Play With It sets the CORS env var on the wrong process
**Severity:** medium
**Lens:** swe
**Location:** C · "Play With It" code block (lines 228-234)
**needsRun:** false
**Issue:** `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173` prefixes the **auth** service command, but repo config shows only the gateway and demand-account consume `app.security.cors.allowed-origins`; the auth service ignores the variable. The gateway (which actually answers the preflight) is started without it — which works only because `gateway/application.yml` defaults to `http://localhost:5173`. The lesson's own troubleshooting entry correctly attributes the variable to the gateway, contradicting this block.
**Fix:** Delete the env-var prefix from the auth command and add a comment on the gateway line: "# CORS: the gateway's dev default already allows http://localhost:5173; set APP_CORS_ALLOWED_ORIGINS on the *gateway* to allow other origins."

### F6: No sequence diagram closing the build
**Severity:** medium
**Lens:** structure
**Location:** C · "The Finished Result" (lines 241-243)
**needsRun:** false
**Issue:** The contract requires the build to close with a sequence diagram of the flow just built; none exists anywhere in the lesson (the cheat card's ASCII sketch is not it, and the only Mermaid diagram is the Understand flowchart).
**Fix:** Insert a `mermaid sequenceDiagram` before the Definition of Done: participants Browser(SPA), Gateway, Auth; LoginPage→AuthContext.login → POST /api/auth/login → {token} → localStorage.setItem('bab.token') → GET /api/auth/me (Authorization: Bearer) → {username, roles} → navigate('/') → DashboardPage; plus an `alt no token` branch showing ProtectedRoute → Navigate to /login.

### F7: First win comes after ~10 hours — nothing runs during the build
**Severity:** medium
**Lens:** adhd
**Location:** C · Build, Sub-step 1 (lines 194-198) through "Play With It" (line 226)
**needsRun:** true
**Issue:** The first executed command with visible result is Play With It, after all six sub-steps. Reward-loop density inside the build is zero; the contract expects something visibly running within ~10 minutes.
**Fix:** End sub-step 1 with `npm run dev` and the real Vite ready banner plus what the browser shows at http://localhost:5173, so the learner sees a page within the first sitting; from sub-step 3 onward suggest `npx vitest --watch` so every saved file gives immediate red/green feedback. Capture the actual outputs by running them (overlaps with F2).

### F8: Verification Log items 7-8 are narrated claims, not pasted output
**Severity:** medium
**Lens:** swe
**Location:** D · Prove, items 7 and 8 (lines 307-309)
**needsRun:** true
**Issue:** The 🔴 Full tier promises real pasted output, but smoke.sh ("→ ✅ Step 29 smoke test PASSED"), full `./mvnw verify` ("BUILD SUCCESS (14 modules ...)"), and the clean-room `npm ci` are asserted in prose with no evidence blocks.
**Fix:** Run `bash steps/step-29/smoke.sh`, `./mvnw verify`, and the clean-room `npm ci && npm run build && npm test`, and paste trimmed real output (last lines with the reactor summary / PASSED banner) into fenced blocks for items 7 and 8.

### F9: Interactivity toolkit nearly absent from the build
**Severity:** medium
**Lens:** pedagogy
**Location:** C · Build, Sub-steps 2-6 (lines 200-224)
**needsRun:** false
**Issue:** One predict (sub-step 1) and one break-it (sub-step 6) are the only interactive elements; there are no knowledge-checks, no type-it-yourself, and no you-are-here markers, so there is also no scaffold fading — every sub-step is (would be) equally fully specified.
**Fix:** Add one predict-then-run per sub-step (e.g., sub-step 3: "what renders if localStorage has a token but /me fails?"; sub-step 5: "what status does a disallowed origin's preflight get, and from which component?"), a 2-question knowledge-check after sub-steps 3 and 5, a "You are here: sub-step X of 6" line at each heading, and make sub-step 4 (DashboardPage) or the second test file in sub-step 6 type-it-yourself from a spec + failing test, with the full solution in a collapsed details block.

### F10: Flashcards not in the lesson
**Severity:** low
**Lens:** structure
**Location:** F · Recap item (g) (line 380)
**needsRun:** false
**Issue:** The recap only points at `docs/flashcards.md`; the contract requires 3-5 flashcards in the lesson itself. The external file holds 6 cards for this step (also above the 3-5 range).
**Fix:** Inline 4-5 of the step-29 cards (Vite speed, route guard, JWT storage trade-off, CORS/preflight, lockfile) as Q/A bullets under recap (g), keeping the pointer to `docs/flashcards.md`; fold the jsdom-localStorage card into troubleshooting rather than keeping 6.

### F11: requests.http missing from the step folder
**Severity:** low
**Lens:** structure
**Location:** steps/step-29/ (folder contains lesson.md + smoke.sh only)
**needsRun:** false
**Issue:** The step adds new HTTP surface at the gateway (the `/api/auth/**` route and the CORS preflight) but ships no `requests.http`, unlike backend steps, so the learner has no click-to-run way to poke the new route without the SPA.
**Fix:** Add `steps/step-29/requests.http` with: POST http://localhost:8080/api/auth/login (alice/password JSON body), GET http://localhost:8080/api/auth/me with `Authorization: Bearer {{token}}`, and an OPTIONS preflight to /api/auth/login carrying `Origin: http://localhost:5173` + `Access-Control-Request-Method: POST` (and a second with a disallowed origin, expecting 403).

### F12: Big Idea has no analogy
**Severity:** low
**Lens:** pedagogy
**Location:** B · "The Big Idea" (lines 106-128)
**needsRun:** false
**Issue:** The contract's Understand movement pairs the big idea + diagram with an analogy; the only analogy in the lesson is the one-line Spring-DI aside inside Pattern Spotlight, and the SPA/gateway/CORS mental model gets none.
**Fix:** Add a two-sentence analogy after the Big Idea paragraph: the gateway is the bank's single reception desk — every request from the lobby kiosk (the SPA) goes through it; CORS is the desk checking the visitor's badge (the Origin header) before answering, and a badge not on the list is turned away (403).
