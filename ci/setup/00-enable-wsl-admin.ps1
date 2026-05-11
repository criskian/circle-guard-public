# Script de configuracion - ejecutar como Administrador en PowerShell
# Habilita WSL2 + Virtual Machine Platform y luego lanza Docker Desktop
Write-Host "[1/3] Habilitando Windows Subsystem for Linux..." -ForegroundColor Cyan
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart

Write-Host "[2/3] Habilitando Virtual Machine Platform..." -ForegroundColor Cyan
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart

Write-Host "[3/3] Descargando e instalando kernel WSL2..." -ForegroundColor Cyan
$wslUrl = "https://wslstorestorage.blob.core.windows.net/wslblob/wsl_update_x64.msi"
$wslMsi = "$env:TEMP\wsl_update_x64.msi"
Invoke-WebRequest -Uri $wslUrl -OutFile $wslMsi -UseBasicParsing
Start-Process msiexec.exe -ArgumentList "/i $wslMsi /quiet /norestart" -Wait

Write-Host "`n[LISTO] Caracteristicas habilitadas." -ForegroundColor Green
Write-Host "Ahora ejecuta Docker Desktop Installer con doble clic en:" -ForegroundColor Yellow
$dockerInstaller = Get-ChildItem "$env:TEMP\Docker*Installer.exe" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
if ($dockerInstaller) {
    Write-Host "  $dockerInstaller" -ForegroundColor Yellow
} else {
    Write-Host "  Busca Docker Desktop Installer.exe en Descargas o vuelve a correr:" -ForegroundColor Yellow
    Write-Host '  winget install --id Docker.DockerDesktop -e --accept-source-agreements --accept-package-agreements' -ForegroundColor White
}
Write-Host "`nREINICIAR el equipo despues de que Docker Desktop se instale correctamente." -ForegroundColor Red
