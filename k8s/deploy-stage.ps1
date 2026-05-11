#!/usr/bin/env pwsh
<#
.SYNOPSIS Deploys CircleGuard to the stage namespace in Docker Desktop Kubernetes.
#>
param(
    [string]$ImageTag = "stage-latest"
)
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Split-Path $PSScriptRoot -Parent

Write-Host "Tagging Docker images as $ImageTag..." -ForegroundColor Cyan
$services = @("auth","identity","form","promotion","notification","dashboard")
foreach ($svc in $services) {
    docker tag "circleguard-$svc-service:latest" "circleguard-$svc-service:$ImageTag"
}

Write-Host "Applying manifests to circleguard-stage..." -ForegroundColor Cyan
kubectl apply -k "$PSScriptRoot\overlays\stage"

Write-Host "Waiting for rollouts..." -ForegroundColor Yellow
foreach ($svc in $services) {
    kubectl rollout status deployment/$svc-service -n circleguard-stage --timeout=120s
}

Write-Host "`nStage deployment complete." -ForegroundColor Green
kubectl get pods -n circleguard-stage -o wide
