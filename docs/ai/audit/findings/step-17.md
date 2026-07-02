# Step 17 audit - swe:6 pedagogy:6 adhd:4 structure:7 - thinBuild:true

## Strengths

- Honest, sophisticated security teaching: the testing-without-committing-keys strategy, the deliberate blast-radius decision to keep `@PreAuthorize` off transfer service methods, the algorithm-confusion attack, and the §12.8 honesty note about what the automated suite does vs. the live run are senior-level and rare in course material.
- Full spine present and well-ordered: all six movements, resolving TOC anchors, stated 🔴 Full tier, §12.3 mutation with real failure output, 6 interview Q&As, 5 flashcards, you-are-here markers on every sub-step.
- Strong prior-knowledge threading: every concept is anchored to Steps 11/12/14/15/16 and forward-linked to Steps 18/32/41/43, with a clear "Depends on" line.

## Missing spine

- common-wrong-output: absent from every run-and-see block in the build (all 5 sub-steps).
- Sub-step 2: no predict-then-run, no under-the-hood, run-and-see has no command and no real output.
- Sub-step 3: no predict-then-run; no run-and-see beyond a bare compile checkpoint.
- Sub-step 4: no under-the-hood; no run-and-see (checkpoint is just "compiles" with no command).
- Sub-step 5: no predict-then-run; no under-the-hood.
- No type-it-yourself or knowledge-check anywhere in the build (interactivity toolkit reduced to 2 predicts + 1 break-it).
- No session plan / named sittings for a ~22-hour step; no per-movement or per-sub-step time-boxes; no re-entry lines at checkpoints.

## Findings

### F1: Build code is fragments; `rolesConverter()` used but never defined
**Severity:** high
**Lens:** swe
**Location:** C · Build, all sub-steps 1-5 (worst: Sub-step 4, SecurityConfig snippet ~lines 355-377)
**needsRun:** false
**Issue:** No code block in the build has a file-path header comment, package line, or imports; sub-step 2 elides the class body with `{ ... }`; sub-step 1 describes the `JwtService` edit in prose instead of a diff; sub-step 5 replaces the token-minting code with a prose stub (`// mint: SignedJWT signed with TEST_KEY's private half`). Fatal instance: sub-step 4's filter chain calls `rolesConverter()`, which is never defined anywhere in the lesson — yet it exists in the repo at `services/demand-account/src/main/java/com/buildabank/account/web/SecurityConfig.java` lines 85-89. A beginner cannot compile any sub-step as shown. This violates "complete code, compiles as shown" for all five sub-steps.
**Fix:** For each sub-step, replace the fragment with the complete file (or a true diff view for edited files) copied from the repo at step-17-end: sub-step 1 = full `SecurityConfig.java` + `JwksController.java` + the `JwtService` diff; sub-step 4 = full demand-account `SecurityConfig.java` including the `rolesConverter()` method and the static imports for `STATELESS`/`withDefaults`; sub-step 5 = the real minting helper from `DemandAccountIntegrationTest`. Add a `// services/<svc>/src/main/java/...` path header and all imports to every block.

### F2: Checkpoints demand test counts (11/31) but the test code is never shown
**Severity:** high
**Lens:** swe
**Location:** Sub-step 2 checkpoint (line ~308), Sub-step 5 (lines ~391-430), Verification Log §1
**needsRun:** false
**Issue:** The learner is told to reach "auth 11 tests" and "demand-account 31 tests" green, and run-and-see for sub-step 2 says "proven in `AuthSecurityTest`" — but the new tests (JWKS no-`d` assertion, method-security 403/200, `unauthenticatedRequestIs401`, admin-ping 403/200, the integration-test `@TestConfiguration` beyond a 4-line stub) are never given as typeable code. The learner cannot produce the green build the checkpoints require without outside help.
**Fix:** In sub-step 2, paste the new `AuthSecurityTest` methods (JWKS public-only assertion + admin-method 403/200) as complete code; in sub-step 5, paste the full new/changed test methods of `TransferControllerTest` and `DemandAccountIntegrationTest` (including the TEST_KEY setup and mint helper) from the repo, as diffs against Step 16's versions.

### F3: 22-hour step with no session plan, save points, or re-entry support
**Severity:** high
**Lens:** adhd
**Location:** A · Orient (30-seconds table, line ~35) and all build checkpoints
**needsRun:** false
**Issue:** Effort is stated as ≈22 hours but there is no sitting plan, no named save points, and no re-entry lines. An ADHD learner has no way to plan or resume: checkpoints say what works but never "stopping here? next session starts at sub-step N, first action: ...".
**Fix:** Add a "🗓️ Session plan" block to Before You Start splitting the step into 7-8 sittings of ~2-3h (e.g. S1 Orient+Understand; S2 sub-step 1; S3 sub-step 2 + auth green; S4 sub-step 3; S5 sub-step 4; S6 sub-step 5 + verify; S7 live cross-service run + Play With It; S8 Prove+Apply+Review), each ending at a named commit. Append a one-line re-entry note to each sub-step checkpoint: "Stopping here? You have <X> working and committed; next session starts at sub-step <N>, first action: <command/file>."

### F4: "SS7" used for Spring Security 7 — collides with the SS7 telecom protocol
**Severity:** medium
**Lens:** swe
**Location:** Then-vs-Now table (line ~195), Interview Q6 (line ~561), Recap key points (line ~605)
**needsRun:** false
**Issue:** Three occurrences abbreviate Spring Security 7 as "SS7". In an authentication lesson this is actively confusing: SS7 is the telephony signaling protocol infamous for SMS-OTP interception attacks — the exact MFA context being discussed.
**Fix:** Replace all three occurrences of "SS7" with "Spring Security 7".

### F5: Sub-step 1 key-generation snippet cannot compile (checked exception in field initializer)
**Severity:** medium
**Lens:** swe
**Location:** Sub-step 1 code block (lines ~248-254)
**needsRun:** false
**Issue:** `private final RSAKey rsaKey = new RSAKeyGenerator(2048).keyID(...).generate();` — Nimbus `generate()` throws the checked `JOSEException`, which a bare field initializer in a `@Configuration` class with a default constructor cannot throw. As shown, this line is a compile error, contradicting "compiles as shown".
**Fix:** Replace the snippet with the real repo implementation (static factory or constructor wrapping `generate()` in try/catch rethrowing `IllegalStateException`), matching `services/auth/.../security/SecurityConfig.java` at step-17-end.

### F6: Sub-steps 2-4 have no runnable run-and-see; no common-wrong-output anywhere
**Severity:** medium
**Lens:** structure
**Location:** Sub-step 2 (line ~306), Sub-step 3 (checkpoint line ~341), Sub-step 4 (checkpoint line ~383)
**needsRun:** true
**Issue:** Sub-step 2's run-and-see is one sentence with no command and no output; sub-steps 3 and 4 have only compile checkpoints with no expected output. No sub-step in the lesson has a common-wrong-output block. The reward loop goes dark from the sub-step 1 JWKS curl until the sub-step 5 verify — three sub-steps of typing across two services with nothing visibly running.
**Fix:** Run and capture real output for: sub-step 2 — curl `/api/auth/admin-method` with a USER token (403 body) and ADMIN token (200 body); sub-step 4 — `curl -i localhost:8082/api/accounts` with no token showing the real 401 + `WWW-Authenticate: Bearer` header; sub-step 3 — the actual `compile` success tail. Add one common-wrong-output per sub-step (e.g. 403-instead-of-401 when the converter misses `roles`; `@PreAuthorize` returning 200 for USER when `@EnableMethodSecurity` is missing).

### F7: No time-boxes below the whole-step estimate; optional content unlabeled
**Severity:** medium
**Lens:** adhd
**Location:** Movement headings A-F; sub-step headings 1-5; Go Deeper (lines ~529-547)
**needsRun:** false
**Issue:** The only time signal is "≈22 hours" for the whole step. No movement or sub-step carries a time-box, and the three Go Deeper `<details>` have no time cost, so a learner cannot budget a sitting or decide whether to expand an optional section.
**Fix:** Append estimates to each movement heading (e.g. "B · Understand (~2h)") and each sub-step heading (e.g. "Sub-step 1 of 5 (~3h)"), and label each Go Deeper summary with "(optional, ~10 min read)".

### F8: Cognitive overload in sub-step 1 (~8 new API types in one screen)
**Severity:** medium
**Lens:** pedagogy
**Location:** Sub-step 1 (lines ~241-283)
**needsRun:** false
**Issue:** One sub-step introduces `RSAKey`, `RSAKeyGenerator`, `JWKSource`, `ImmutableJWKSet`, `JWKSet`, `NimbusJwtEncoder`, `NimbusJwtDecoder`, plus `kid`/`kty`/`n`/`e` — far beyond the ~3-new-terms guideline — with a five-line explanation covering five beans.
**Fix:** Split into Sub-step 1a (generate the RSA key; swap encoder/decoder beans; run-and-see: auth tests still green with RS256) and 1b (publish `/oauth2/jwks` + permit it; run-and-see: the curl already present), each with its own line-by-line and pitfall; renumber to "of 6".

### F9: No scaffold fading — zero type-it-yourself or mid-build knowledge-checks
**Severity:** medium
**Lens:** pedagogy
**Location:** C · Build, sub-steps 3-5
**needsRun:** false
**Issue:** All five sub-steps use the same show-the-snippet style; interactivity is 2 predicts + 1 break-it. Later sub-steps never shift toward retrieval-based construction, so the learner passively copies (or, given F1, guesses) to the end.
**Fix:** In sub-step 4, add a type-it-yourself prompt before revealing the filter chain ("write the three `requestMatchers` rules yourself — you wrote the same shape in Step 16's SecurityConfig — then compare"); after sub-step 3, add a knowledge-check ("Boot now finds `jwk-set-uri` — at what moment is the JWKS actually fetched, and what does that mean if auth is down at startup?" with a details answer).

### F10: Verification Log §2 is paraphrased and contradicts the cheat card
**Severity:** medium
**Lens:** swe
**Location:** D · Prove §2 (lines ~490-498) vs. Cheat Card (line ~72)
**needsRun:** true
**Issue:** §2's "real pasted output" is a hand-formatted summary, not a transcript, and its data disagrees with the lesson's own command: the cheat card creates `ACC-A` with `openingBalance: 200.00`, but §2 shows `201 {"accountNumber":"XACC","currency":"USD","balance":100.00}`. Learners following the cheat card will get output that doesn't match the log, undermining trust in the tier-Full claim.
**Fix:** Re-run the live cross-service flow with the exact cheat-card commands and paste the raw curl transcripts (status lines + bodies) into §2; make the account number/opening balance identical in both places.

### F11: Under-the-Hood wall of text (six dense paragraphs, ~1,000 words)
**Severity:** low
**Lens:** adhd
**Location:** B · Understand, "🌱 Under the Hood" (lines ~166-178)
**needsRun:** false
**Issue:** Six consecutive dense paragraphs with no code, diagram, or interactive break; worst offenders are the "URL rules vs `@PreAuthorize` (blast-radius)" and "Testing security without committing keys" paragraphs (~150-170 words each of heavy jargon).
**Fix:** Break with structure: convert "URL rules vs `@PreAuthorize`" into a 3-row comparison table (where enforced / granularity / test impact), put "Testing security without committing keys" under its own sub-heading with the two techniques as bullets, and insert one knowledge-check between paragraphs 3 and 4.

### F12: "Explain MFA/passkeys/step-up" objective has no retrieval item
**Severity:** low
**Lens:** pedagogy
**Location:** ✅ What You'll Be Able to Do (line ~98) vs. 🧠 Test Yourself (lines ~609-614)
**needsRun:** false
**Issue:** The fourth outcome (MFA, passkeys/WebAuthn, step-up) is covered by prose and one flashcard, but none of the five Test-Yourself questions exercises it — the only objective without an aligned retrieval item.
**Fix:** Add Test-Yourself Q6: "Why are passkeys phishing-resistant where passwords and SMS OTP are not, and what is step-up auth?" with a details answer (challenge signed by a device-bound private key that never leaves the device / requiring a stronger or fresh factor for sensitive actions).
