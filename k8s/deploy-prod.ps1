#!/usr/bin/env pwsh
<#
.SYNOPSIS Promotes stage images to prod and deploys to circleguard-prod namespace.
#>
param(
    [string]$StageTag = "stage-latest",
    [string]$ProdTag  = "prod-latest"
)
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "Promoting $StageTag -> $ProdTag..." -ForegroundColor Cyan
$services = @("auth","identity","form","promotion","notification","dashboard")
foreach ($svc in $services) {
    docker tag "circleguard-$svc-service:$StageTag" "circleguard-$svc-service:$ProdTag"
}

Write-Host "Applying manifests to circleguard-prod..." -ForegroundColor Cyan
kubectl apply -k "$PSScriptRoot\overlays\prod"

Write-Host "Waiting for rollouts..." -ForegroundColor Yellow
foreach ($svc in $services) {
    kubectl rollout status deployment/$svc-service -n circleguard-prod --timeout=180s
}

Write-Host "`nProd deployment complete." -ForegroundColor Green
kubectl get pods -n circleguard-prod -o wide
