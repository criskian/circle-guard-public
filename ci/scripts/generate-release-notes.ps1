#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Generates Release Notes from git commit history following Conventional Commits.
  Creates RELEASE_NOTES_<version>.md and prepends entry to CHANGELOG.md.
  Also creates a git tag vN.N.N.
  Semantic versioning bump is derived automatically from commit types:
    - BREAKING CHANGE / feat! / fix!  → MAJOR bump
    - feat                            → MINOR bump
    - fix / refactor / perf           → PATCH bump
    - (others)                        → PATCH bump

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

# ── Determine last version tag ─────────────────────────────────────────────────
$lastTag = git describe --tags --abbrev=0 2>$null
if (-not $lastTag) {
    $lastTag    = $null
    $rangeSpec  = "HEAD~50"
    $baseVersion = @{ Major = 1; Minor = 0; Patch = 0 }
} elseif ($lastTag -match 'v(\d+)\.(\d+)\.(\d+)') {
    $rangeSpec  = "${lastTag}..HEAD"
    $baseVersion = @{ Major = [int]$Matches[1]; Minor = [int]$Matches[2]; Patch = [int]$Matches[3] }
} else {
    $rangeSpec  = "HEAD~50"
    $baseVersion = @{ Major = 1; Minor = 0; Patch = 0 }
}

$date = Get-Date -Format "yyyy-MM-dd"

Write-Host "Collecting commits: $rangeSpec" -ForegroundColor Cyan

# ── Collect commits ────────────────────────────────────────────────────────────
$rawCommits = git log $rangeSpec --pretty=format:"%s (%h) by %an" 2>$null
if (-not $rawCommits) {
    $rawCommits = "chore: no changes since last release (n/a) by CI"
}
$commitLines = ($rawCommits -split "`n") | Where-Object { $_.Trim() -ne "" }

# ── Categorize by Conventional Commits ────────────────────────────────────────
$features  = @()
$fixes     = @()
$tests     = @()
$refactors = @()
$docs      = @()
$chores    = @()
$breaking  = @()
$others    = @()

$hasMajorBump = $false
$hasMinorBump = $false
$hasPatchBump = $false

foreach ($line in $commitLines) {
    if ($line -match "^BREAKING CHANGE:|^.*!:") {
        $breaking    += "- $line"
        $hasMajorBump = $true
    } elseif ($line -match "^feat(\(.+\))?:") {
        $features    += "- $line"
        $hasMinorBump = $true
    } elseif ($line -match "^fix(\(.+\))?:") {
        $fixes       += "- $line"
        $hasPatchBump = $true
    } elseif ($line -match "^test(\(.+\))?:") {
        $tests       += "- $line"
        $hasPatchBump = $true
    } elseif ($line -match "^refactor(\(.+\))?:|^perf(\(.+\))?:") {
        $refactors   += "- $line"
        $hasPatchBump = $true
    } elseif ($line -match "^docs(\(.+\))?:") {
        $docs        += "- $line"
        $hasPatchBump = $true
    } elseif ($line -match "^chore(\(.+\))?:") {
        $chores      += "- $line"
    } else {
        $others      += "- $line"
        $hasPatchBump = $true
    }
}

# ── Compute semantic version bump ──────────────────────────────────────────────
if ($hasMajorBump) {
    $newVersion = "v$($baseVersion.Major + 1).0.0"
    $bumpType   = "MAJOR"
} elseif ($hasMinorBump) {
    $newVersion = "v$($baseVersion.Major).$($baseVersion.Minor + 1).0"
    $bumpType   = "MINOR"
} elseif ($hasPatchBump -or $chores.Count -gt 0 -or $others.Count -gt 0) {
    $newVersion = "v$($baseVersion.Major).$($baseVersion.Minor).$($baseVersion.Patch + 1)"
    $bumpType   = "PATCH"
} else {
    $newVersion = "v$($baseVersion.Major).$($baseVersion.Minor).$($baseVersion.Patch + 1)"
    $bumpType   = "PATCH"
}

$range = if ($lastTag) { "${lastTag}..HEAD" } else { "(initial)" }
Write-Host "Bump: $bumpType  |  $($lastTag ?? 'none') → $newVersion" -ForegroundColor Green

# ── Build markdown ─────────────────────────────────────────────────────────────
$md = @"
# Release Notes — $Service $newVersion

**Date:** $date
**Build:** #$BuildNumber
**Range:** ``$range``
**Bump type:** $bumpType

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

$md += "`n---`n*Generated automatically by CircleGuard CI/CD — Build #$BuildNumber*`n"

# ── Write release notes file ───────────────────────────────────────────────────
$releaseFile = "RELEASE_NOTES_${newVersion}.md"
$md | Out-File -FilePath $releaseFile -Encoding utf8
Write-Host "Written: $releaseFile" -ForegroundColor Green

# ── Prepend to CHANGELOG.md ────────────────────────────────────────────────────
$changelogFile = "CHANGELOG.md"
$header = "# CircleGuard Changelog`n`n"
if (Test-Path $changelogFile) {
    $existing = Get-Content $changelogFile -Raw
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
    $existingTag = git tag -l $newVersion 2>$null
    if (-not $existingTag) {
        git tag -a "$newVersion" -m "Release $newVersion ($bumpType) — Build #$BuildNumber" 2>$null
        Write-Host "Tagged: $newVersion" -ForegroundColor Green
    } else {
        Write-Host "Tag $newVersion already exists — skipping." -ForegroundColor Yellow
    }
} catch {
    Write-Warning "Could not create git tag: $_"
}

Write-Host "Release Notes generation complete: $newVersion ($bumpType)" -ForegroundColor Cyan
