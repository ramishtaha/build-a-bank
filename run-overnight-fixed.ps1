cd $PSScriptRoot

$model = "opus"

# FIXED LOOP — the anti-drift version.
# Key change vs the old script: NO --continue. Every pass is a FRESH session that
# re-reads MASTER_PROMPT.md in full. Resume state lives in PROGRESS.md + git tags
# (that is what they were designed for), NOT in conversation memory. This prevents
# the "anchoring ratchet" where each new lesson pattern-matches the previous thin
# lesson instead of the spec.

$kick = @"
Read MASTER_PROMPT.md IN FULL as your operating brief — especially section 8 (the per-step contract, INCLUDING the new DEPTH GATE in the structural self-check), section 8.1 (the Hands-On Build Playbook, INCLUDING the complete worked example), and section 12 (verification). Then read PROGRESS.md, git checkout the last verified tag, re-run make doctor, and continue from the recorded next action.

DEPTH MANDATE (this is the priority of this run): recent lessons (roughly steps 19-30) drifted far below the bar — skeleton sections with no code blocks, no line-by-line, no expected output. Do NOT match them. Before writing any new lesson, open steps/step-08/lesson.md as your live exemplar. Every new lesson must meet the full section-8.1 micro-anatomy in every build sub-step (Goal, Path, complete copy-pasteable code, line-by-line, under-the-hood, predict, Run-and-See with a fenced expected-output block, checkpoint, commit, pitfall) and the depth gate's sanity floor (~1,000+ lines, ~80+ fenced blocks for a typical step). Ship the per-step requests.http and seed data where the step's services warrant them. A thin lesson counts as a FAILED step even if its code verifies. Fewer full-depth steps per session always beats more thin ones.

RUN MODE: phase-by-phase - build step to step continuously; only stop when you genuinely approach a context limit, then finish the current sub-step cleanly, commit, update PROGRESS.md, and exit. Hold the full bar: complete section-8 contract, exhaustive section-8.1 hands-on build with real pasted output, tiered section-12 verification, step-NN-end == step-(NN+1)-start. Make every decision yourself (record architectural ones as ADRs); never wait for input.
"@

for ($i = 1; $i -le 50; $i++) {
  Write-Host "=== Pass $i  $(Get-Date) ===" -ForegroundColor Cyan

  # Every pass is identical and FRESH (no --continue): the spec is always the
  # dominant context, never the previous passes' output.
  claude -p $kick --effort xhigh --permission-mode bypassPermissions --model $model --max-budget-usd 100 --output-format stream-json --verbose 2>&1 | Tee-Object -Append run.log

  if (Test-Path STOP) { Write-Host "STOP file found - halting." -ForegroundColor Yellow; break }
  Start-Sleep -Seconds 10
}
