$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$adbCandidates = @((Join-Path $root 'platform-tools\adb.exe'),(Join-Path $root '..\platform-tools\adb.exe'),'adb.exe')
$adb = $adbCandidates | Where-Object { ($_ -eq 'adb.exe') -or (Test-Path $_) } | Select-Object -First 1
if (-not $adb) { throw 'No se encontró adb.exe.' }
Write-Host '=== TABLET ESCOLAR · VERIFICACIÓN ===' -ForegroundColor Cyan
Write-Host "`nDispositivo ADB:"; & $adb devices
Write-Host "`nPaquete:"; & $adb shell pm list packages | Select-String 'cl.antumapu.aulacontrol'
Write-Host "`nPolítica / administrador:"; & $adb shell dumpsys device_policy | Select-String -Pattern 'aulacontrol|Device Owner|Profile Owner'
Write-Host "`nUsuarios Android:"; & $adb shell pm list users
Write-Host "`nSi ves el paquete y Tablet Escolar como Device Owner, la base de administración quedó activa." -ForegroundColor Green
