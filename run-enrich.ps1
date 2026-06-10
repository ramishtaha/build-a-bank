cd $PSScriptRoot

$model = "opus"

# RESCUE LOOP — enrich thin lessons (steps 11..30) back to full section-8.1 depth.
# CODE-FROZEN: lesson/docs files only; the verified code, tests, and tags are never touched.
# One step per pass, FRESH session each pass (no --continue), tracked in docs/ENRICHMENT.md.
# Run this AFTER (or instead of) the build loop — never both at once (same repo).

$kick = @"
You are doing a DOCUMENTATION-ONLY enrichment pass on this repo. The code is verified and FROZEN — you must not modify any code, test, pom, config, script, or git tag. You only rewrite lesson files and per-step learner aids.

1. Read MASTER_PROMPT.md section 8 (per-step contract incl. the DEPTH GATE) and section 8.1 IN FULL (the Hands-On Build Playbook, including the complete worked example). Open steps/step-08/lesson.md — it is your live exemplar for depth and shape.

2. Read docs/ENRICHMENT.md to find the next step to enrich (if the file does not exist, create it and start at step 11). Enrich exactly ONE step this session: call it step NN.

3. Reconstruct what step NN actually built: run 'git diff step-(NN-1)-end..step-NN-end --name-only' to list the files it touched, and read each file's content AS OF THAT STEP with 'git show step-NN-end:<path>'. CRITICAL: later steps refactored some of this code — HEAD is the WRONG source for old steps; the tag is the only truth. Also read the existing steps/step-NN/lesson.md (its Verification Log contains REAL recorded run output) and steps/step-NN/smoke.sh.

4. Rewrite steps/step-NN/lesson.md to the full section-8.1 bar, keeping its verified facts: every build sub-step gets the complete micro-anatomy — Goal, exact Path, the COMPLETE code block verbatim from 'git show step-NN-end:<path>' (with file-path header; before/after diff for edits), line-by-line on every new token, under-the-hood, predict-then-run, Run-and-See with a fenced expected-output block, checkpoint, commit message, pitfall. Expected outputs must come from the step's existing Verification Log / smoke.sh evidence or from commands you actually run now (cheap, read-only ones like ./mvnw -pl <module> test are allowed) — NEVER invent output; if an output is not recorded and not cheaply re-runnable, present the command with its expected shape and mark it verify-adjacent per section 12.8. Keep the existing Verification Log intact at the end. Restore a per-step requests.http (and seed data) where the step's services expose endpoints and one is missing. Append the step's interview Q&As to docs/interview-bank.md and its Key Terms to docs/glossary.md (create these files with a short header if missing). If NN is a multiple of 5, ensure the lesson ends with a '## Cumulative Review (steps 1-NN)' mixed quiz spanning recent + older steps (answers in details blocks) — add it if absent. Apply the DEPTH GATE before finishing: full micro-anatomy in every sub-step, sanity floor ~1,000+ lines / ~80+ fenced blocks for a typical step.

5. GUARD before committing: run 'git status --porcelain' — the ONLY changed/added paths may be steps/step-NN/* and files under docs/ (ENRICHMENT.md, interview-bank.md, glossary.md, flashcards.md). If anything else changed, revert it. Then update docs/ENRICHMENT.md (mark step NN enriched, next = NN+1, note anything unrecoverable), and commit: 'docs(step-NN): enrich lesson to full section-8.1 depth (code-frozen)'. Do NOT move any tags. Then exit — one step per session.
"@

for ($i = 1; $i -le 30; $i++) {
  Write-Host "=== Enrich pass $i  $(Get-Date) ===" -ForegroundColor Cyan

  claude -p $kick --effort xhigh --permission-mode bypassPermissions --model $model --max-budget-usd 100 --output-format stream-json --verbose 2>&1 | Tee-Object -Append enrich.log

  if (Test-Path STOP) { Write-Host "STOP file found - halting." -ForegroundColor Yellow; break }
  Start-Sleep -Seconds 10
}
