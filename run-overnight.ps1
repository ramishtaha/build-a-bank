cd $PSScriptRoot

$model = "opus"
$kick = @"
Read MASTER_PROMPT.md as your full operating brief, then read PROGRESS.md and resume autonomously from the last verified tag.
RUN MODE: phase-by-phase - build step to step continuously; only stop when you genuinely approach a context limit, then finish the current sub-step cleanly, commit, update PROGRESS.md, and exit. Hold the full bar: complete the section 8 contract, the exhaustive section 8.1 hands-on build with real pasted output, tiered section 12 verification, and step-NN-end == step-(NN+1)-start. Make every decision yourself (record architectural ones as ADRs); never wait for input. kind and minikube are installed - record this in CAPABILITIES.md and treat local k8s as available for Phase G.
"@

for ($i = 1; $i -le 50; $i++) {
  Write-Host "=== Pass $i  $(Get-Date) ===" -ForegroundColor Cyan

  if ($i -eq 1) {
    claude -p $kick --effort xhigh --permission-mode bypassPermissions --model $model --max-budget-usd 100000 --output-format stream-json --verbose 2>&1 | Tee-Object -Append run.log
  } else {
    claude -p "Re-read MASTER_PROMPT.md sections 8, 8.1, and 12, then continue from PROGRESS.md." --continue --effort xhigh --permission-mode bypassPermissions --model $model --max-budget-usd 100000 --output-format stream-json --verbose 2>&1 | Tee-Object -Append run.log
  }

  if (Test-Path STOP) { Write-Host "STOP file found - halting." -ForegroundColor Yellow; break }
  Start-Sleep -Seconds 10
}
