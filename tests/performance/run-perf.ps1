#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Runs CircleGuard performance tests with Locust.
.PARAMETER Profile
  steady (default) | spike | soak
.EXAMPLE
  .\run-perf.ps1 -Profile spike
#>
param(
    [ValidateSet("steady","spike","soak")]
    [string]$Profile   = "steady",
    [int]   $Users     = 100,
    [int]   $SpawnRate = 10,
    [string]$Duration  = "5m",
    [string]$ReportDir = "$PSScriptRoot\reports"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Get-Command locust -ErrorAction SilentlyContinue)) {
    Write-Host "Locust not found — installing..." -ForegroundColor Yellow
    pip install locust
}

New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

$locustFile = if ($Profile -eq "spike") {
    "$PSScriptRoot\locustfile_spike.py"
} else {
    "$PSScriptRoot\locustfile.py"
}

Write-Host "Starting '$Profile' performance test ($Users users, ${SpawnRate}/s spawn, $Duration)..." -ForegroundColor Cyan

locust `
    --headless `
    -f $locustFile `
    --host http://localhost:8180 `
    -u $Users `
    -r $SpawnRate `
    -t $Duration `
    --html "$ReportDir\locust-${Profile}-${timestamp}.html" `
    --csv  "$ReportDir\locust-${Profile}-${timestamp}" `
    --logfile "$ReportDir\locust-${Profile}-${timestamp}.log"

$exitCode = $LASTEXITCODE

# Parse CSV for quick summary
$statsFile = "$ReportDir\locust-${Profile}-${timestamp}_stats.csv"
if (Test-Path $statsFile) {
    Write-Host "`n─── PERFORMANCE SUMMARY ───────────────────────────────" -ForegroundColor Cyan
    $stats = Import-Csv $statsFile
    $stats | Where-Object { $_.'Name' -ne '' } | ForEach-Object {
        Write-Host ("  {0,-55} | RPS: {1,6} | p50: {2,6}ms | p95: {3,6}ms | Fails: {4}" -f
            $_.'Name', $_.'Requests/s', $_.'50%', $_.'95%', $_.'Failure Count')
    }
    Write-Host "─────────────────────────────────────────────────────────`n"
}

Write-Host "Reports saved to: $ReportDir" -ForegroundColor Green
exit $exitCode
