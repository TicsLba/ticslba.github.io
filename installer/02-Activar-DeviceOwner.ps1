$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$adbCandidates = @((Join-Path $root 'platform-tools\adb.exe'),(Join-Path $root '..\platform-tools\adb.exe'),'adb.exe')
$adb = $adbCandidates | Where-Object { ($_ -eq 'adb.exe') -or (Test-Path $_) } | Select-Object -First 1
if (-not $adb) { throw 'No se encontró adb.exe.' }
Write-Host 'Comprobando conexión...' -ForegroundColor Cyan
& $adb devices
Write-Host "`nIMPORTANTE: Device Owner sólo puede activarse cuando Android permite el aprovisionamiento y no existen cuentas que lo bloqueen." -ForegroundColor Yellow
$ok = Read-Host 'Escribe SI para continuar'
if ($ok -ne 'SI') { Write-Host 'Cancelado.'; exit }
& $adb shell dpm set-device-owner cl.antumapu.aulacontrol/.AdminReceiver
if ($LASTEXITCODE -ne 0) { throw 'Android rechazó la activación de Device Owner. Revisa el mensaje anterior.' }
Write-Host '`nTablet Escolar quedó configurada como Device Owner.' -ForegroundColor Green
