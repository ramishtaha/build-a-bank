# run-overnight-v3.ps1 — generate the remaining course steps (32 → 67), one verified step per pass.
#
# v3 vs the old loops:
#   - Uses the docs/ai operational layer (CLAUDE.md routes → CONTEXT-PLAYBOOK read-set) instead of
#     re-reading the 35k-token master prompt every pass. Fresh session per pass (anti-anchoring —
#     resume state lives in PROGRESS.md + git tags, never in conversation memory).
#   - ONE step per pass, gated: a pass that makes no commit twice in a row halts the loop
#     (stuck ≠ keep burning tokens). A pass may legitimately end mid-step (budget stop) —
#     that still commits, so it counts as progress and the next pass resumes the sub-step.
#   - Fable 5 + xhigh effort + ultracode (multi-agent orchestration allowed inside each pass).
#
# Controls:
#   - Stop gracefully: create a file named STOP in the repo root (checked between passes).
#   - Watch live:      Get-Content overnight-v3.log -Wait -Tail 20
#   - Each pass also writes logs\pass-NN-<timestamp>.log
#
# Expectations: a full-depth verified step takes real time (typically 1–3 h+ at this bar).
# An overnight run lands a handful of steps, not all 36 — that is by design; the chain resumes.

cd $PSScriptRoot
$ErrorActionPreference = 'Continue'
New-Item -ItemType Directory -Force logs | Out-Null

# ── Preflight ────────────────────────────────────────────────────────────────
# A STOP file is the mid-run halt signal; one lying around at startup is stale — clear it.
if (Test-Path STOP) { Remove-Item STOP -Force; Write-Host "Removed stale STOP file from a previous run." -ForegroundColor Yellow }

$branch = git branch --show-current
if ($branch -ne 'main') { Write-Host "ABORT: on '$branch', not main. Run: git switch main" -ForegroundColor Red; exit 1 }

docker ps *> $null
if ($LASTEXITCODE -ne 0) { Write-Host "ABORT: Docker is not running — Testcontainers-backed verification needs it all night." -ForegroundColor Red; exit 1 }

$dirty = git status --porcelain
if ($dirty) { Write-Host "ABORT: working tree not clean — commit or stash first:`n$dirty" -ForegroundColor Red; exit 1 }

# ── The per-pass kickoff (identical + fresh every pass; ultracode opts in multi-agent workflows) ──
$kick = @'
ultracode

Continue the Build-a-Bank course by completing EXACTLY ONE step, end to end. Read CLAUDE.md, then follow docs/ai/CONTEXT-PLAYBOOK.md exactly — its read-set (PROGRESS.md → previous step's capsule.md → the COURSE.md row → CAPABILITIES.md/VERSIONS.md → LESSON-SPEC.md/PROJECT-MAP.md), its build order (build + verify the code FIRST, capture real output, then write the lesson build-section-first), and its output-budget protocol.

Hold the full bar, no exceptions:
- docs/ai/LESSON-SPEC.md contract: six movements, Session Plan, full micro-anatomy in EVERY build sub-step, interactivity minimums, re-entry rituals, time-boxes.
- Real pasted output only (master prompt section 12) — never invent an expected-output block; verify-adjacent items say so prominently per CAPABILITIES.md.
- The Lesson DoD (docs/ai/LESSON-CHECKLIST.md) must PASS and be recorded in the Verification Log before tagging.
- Anti-anchoring: before writing the build, open steps/step-08 or steps/step-12 as the live exemplar — never pattern-match the most recent lesson.
- Bookkeeping, all of it: capsule.md, PROGRESS.md resume block + lesson-metrics row, VERIFICATION-LEDGER row, flashcards append, COURSE.md tick, CONTRACT-DEBT for anything promised-but-skipped, ADR for architectural decisions.
- Commit on main; tag step-NN-end (== step-(NN+1)-start) ONLY when the step's full tiered Definition of Done passes.

If you approach a context/output limit: finish the current sub-step cleanly (full micro-anatomy), commit, update PROGRESS.md with exactly where you stopped, and exit — the next pass resumes. Stopping early always beats compressing; a thin lesson is a failed step even if its code verifies. Make every decision yourself; never wait for input.
'@

# ── The loop ─────────────────────────────────────────────────────────────────
$noProgressStrikes = 0
for ($pass = 1; $pass -le 40; $pass++) {
    if (Test-Path STOP) { Write-Host "STOP file found — halting." -ForegroundColor Yellow; break }
    if (git tag --list 'step-67-end') { Write-Host "step-67-end exists — course complete. 🏁" -ForegroundColor Green; break }

    $headBefore = git rev-parse HEAD
    $tagsBefore = (git tag --list 'step-*-end' | Measure-Object).Count
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    Write-Host "=== Pass $pass · $(Get-Date) · HEAD $($headBefore.Substring(0,7)) · $tagsBefore end-tags ===" -ForegroundColor Cyan

    claude -p $kick `
        --model claude-fable-5 `
        --effort xhigh `
        --permission-mode bypassPermissions `
        --max-budget-usd 120 `
        --output-format stream-json --verbose 2>&1 |
        Tee-Object "logs\pass-$('{0:d2}' -f $pass)-$stamp.log" |
        Tee-Object -Append overnight-v3.log | Out-Null

    $headAfter = git rev-parse HEAD
    $tagsAfter = (git tag --list 'step-*-end' | Measure-Object).Count

    if ($tagsAfter -gt $tagsBefore) {
        $newTag = git tag --list 'step-*-end' --sort=-creatordate | Select-Object -First 1
        Write-Host "Pass $pass ✅ completed a step: $newTag" -ForegroundColor Green
        $noProgressStrikes = 0
        git push origin main --tags 2>&1 | Out-Null   # sync after every completed step
    }
    elseif ($headAfter -ne $headBefore) {
        Write-Host "Pass $pass 🟡 partial progress (commits, no new tag) — next pass resumes the step." -ForegroundColor Yellow
        $noProgressStrikes = 0
    }
    else {
        $noProgressStrikes++
        Write-Host "Pass $pass 🔴 NO progress (no commits) — strike $noProgressStrikes/2." -ForegroundColor Red
        if ($noProgressStrikes -ge 2) {
            Write-Host "Two consecutive no-progress passes — something is stuck; halting instead of burning tokens. Check the last logs\pass-*.log." -ForegroundColor Red
            break
        }
    }
    Start-Sleep -Seconds 15
}
Write-Host "Overnight run finished · $(Get-Date) · latest: $(git tag --list 'step-*-end' --sort=-creatordate | Select-Object -First 1)" -ForegroundColor Cyan
