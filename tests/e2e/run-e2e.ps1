#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Runs the CircleGuard E2E Newman test suite.
.PARAMETER Environment
  Target environment: dev (default) or stage
.EXAMPLE
  .\run-e2e.ps1 -Environment stage
#>
param(
    [string]$Environment = "dev",
    [string]$ReportDir   = "$PSScriptRoot\reports"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$collection = "$PSScriptRoot\circleguard-e2e.postman_collection.json"
$envFile    = "$PSScriptRoot\$Environment.env.json"
$timestamp  = Get-Date -Format "yyyyMMdd-HHmmss"

# Ensure newman is installed
if (-not (Get-Command newman -ErrorAction SilentlyContinue)) {
    Write-Host "Installing Newman..." -ForegroundColor Yellow
    npm install -g newman newman-reporter-htmlextra
}

New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null

Write-Host "Running E2E tests against '$Environment' environment..." -ForegroundColor Cyan

newman run $collection `
    --environment $envFile `
    --reporters cli,junit,htmlextra `
    --reporter-junit-export "$ReportDir\e2e-results-$timestamp.xml" `
    --reporter-htmlextra-export "$ReportDir\e2e-report-$timestamp.html" `
    --timeout-request 15000 `
    --delay-request 500

$exitCode = $LASTEXITCODE
if ($exitCode -eq 0) {
    Write-Host "`nAll E2E tests PASSED" -ForegroundColor Green
} else {
    Write-Host "`nE2E tests FAILED (exit code $exitCode)" -ForegroundColor Red
}

Write-Host "Reports saved to: $ReportDir"
exit $exitCode
