#!/usr/bin/env pwsh
<#
.SYNOPSIS Builds and starts Jenkins for CircleGuard CI/CD.
.NOTES Run from the repository root AFTER Docker Desktop is running.
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = "C:\Users\crist\OneDrive\Desktop\circle-guard-public"
$jenkinDir = "$repoRoot\ci\jenkins"

Write-Host "[1/3] Building Jenkins image..." -ForegroundColor Cyan
docker compose -f "$jenkinDir\docker-compose.yml" build

Write-Host "[2/3] Starting Jenkins..." -ForegroundColor Cyan
$env:CIRCLE_GUARD_REPO = $repoRoot
docker compose -f "$jenkinDir\docker-compose.yml" up -d

Write-Host "[3/3] Waiting for Jenkins to start (60s)..." -ForegroundColor Yellow
Start-Sleep -Seconds 60

# Get initial admin password (may not be needed if CasC is configured)
$password = docker exec circleguard-jenkins cat /var/jenkins_home/secrets/initialAdminPassword 2>$null
if ($password) {
    Write-Host "`nInitial Admin Password: $password" -ForegroundColor Yellow
} else {
    Write-Host "`nCasC configured — admin/admin123 should work." -ForegroundColor Green
}

Write-Host "`nJenkins is available at: http://localhost:8080" -ForegroundColor Green
Write-Host "Login: admin / admin123"
