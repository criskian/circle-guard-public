#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Generates Release Notes from git commit history following Conventional Commits.
  Creates RELEASE_NOTES_<version>.md and prepends entry to CHANGELOG.md.
  Also creates a git tag vN.N.N.

.PARAMETER Service
  Name of the microservice being released (e.g. 'circleguard-auth-service')
.PARAMETER BuildNumber
  CI build number to embed in the version tag
#>
param(
    [string]$Service     = "circleguard",
    [string]$BuildNumber = "0"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ── Determine version ──────────────────────────────────────────────────────────
$lastTag = git describe --tags --abbrev=0 2>$null
if (-not $lastTag) {
    $lastTag = "HEAD~20"
    $newVersion = "v1.0.0"
} else {
    # Increment patch
    if ($lastTag -match 'v(\d+)\.(\d+)\.(\d+)') {
        $newVersion = "v$($Matches[1]).$($Matches[2]).$([int]$Matches[3] + 1)"
    } else {
        $newVersion = "v1.0.${BuildNumber}"
    }
}

$date = Get-Date -Format "yyyy-MM-dd"
$range = "${lastTag}..HEAD"

Write-Host "Generating release notes: $lastTag -> $newVersion" -ForegroundColor Cyan

# ── Collect commits ────────────────────────────────────────────────────────────
$commits = git log $range --pretty=format:"%s (%h) by %an" 2>$null
if (-not $commits) {
    $commits = @("chore: no changes since last release")
}
$commitLines = $commits -split "`n"

# ── Categorize by Conventional Commits ────────────────────────────────────────
$features     = @()
$fixes        = @()
$tests        = @()
$refactors    = @()
$docs         = @()
$chores       = @()
$breaking     = @()
$others       = @()

foreach ($line in $commitLines) {
    if ($line -match "^BREAKING CHANGE:|^.*!:") { $breaking += "- $line" }
    elseif ($line -match "^feat(\(.+\))?:") { $features  += "- $line" }
    elseif ($line -match "^fix(\(.+\))?:")  { $fixes      += "- $line" }
    elseif ($line -match "^test(\(.+\))?:") { $tests      += "- $line" }
    elseif ($line -match "^refactor(\(.+\))?:") { $refactors += "- $line" }
    elseif ($line -match "^docs(\(.+\))?:") { $docs       += "- $line" }
    elseif ($line -match "^chore(\(.+\))?:") { $chores    += "- $line" }
    else { $others += "- $line" }
}

# ── Build markdown ─────────────────────────────────────────────────────────────
$md = @"
# Release Notes — $Service $newVersion

**Date:** $date
**Build:** #$BuildNumber
**Range:** ``$range``

"@

if ($breaking) {
    $md += "`n## ⚠ BREAKING CHANGES`n`n" + ($breaking -join "`n") + "`n"
}
if ($features) {
    $md += "`n## ✨ New Features`n`n" + ($features -join "`n") + "`n"
}
if ($fixes) {
    $md += "`n## 🐛 Bug Fixes`n`n" + ($fixes -join "`n") + "`n"
}
if ($tests) {
    $md += "`n## 🧪 Tests`n`n" + ($tests -join "`n") + "`n"
}
if ($refactors) {
    $md += "`n## ♻ Refactors`n`n" + ($refactors -join "`n") + "`n"
}
if ($docs) {
    $md += "`n## 📚 Documentation`n`n" + ($docs -join "`n") + "`n"
}
if ($chores -or $others) {
    $allChores = $chores + $others
    $md += "`n## 🔧 Chores`n`n" + ($allChores -join "`n") + "`n"
}

$md += "`n---`n*Generated automatically by CircleGuard CI/CD*`n"

# ── Write release notes file ───────────────────────────────────────────────────
$releaseFile = "RELEASE_NOTES_${newVersion}.md"
$md | Out-File -FilePath $releaseFile -Encoding utf8
Write-Host "Written: $releaseFile" -ForegroundColor Green

# ── Prepend to CHANGELOG.md ────────────────────────────────────────────────────
$changelogFile = "CHANGELOG.md"
$header = "# CircleGuard Changelog`n`n"
if (Test-Path $changelogFile) {
    $existing = Get-Content $changelogFile -Raw
    # Remove the header line if already there to avoid duplication
    $existing = $existing -replace "^# CircleGuard Changelog\s*`n`n?", ""
    $header + $md + "`n---`n`n" + $existing | Out-File -FilePath $changelogFile -Encoding utf8
} else {
    $header + $md | Out-File -FilePath $changelogFile -Encoding utf8
}
Write-Host "Updated: $changelogFile" -ForegroundColor Green

# ── Git tag ────────────────────────────────────────────────────────────────────
try {
    git config user.email "ci@circleguard.local"
    git config user.name  "CircleGuard CI"
    git tag -a "$newVersion" -m "Release $newVersion — Build #$BuildNumber" 2>$null
    Write-Host "Tagged: $newVersion" -ForegroundColor Green
} catch {
    Write-Warning "Could not create git tag: $_"
}

Write-Host "Release Notes generation complete." -ForegroundColor Cyan
