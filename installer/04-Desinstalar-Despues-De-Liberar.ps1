$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$adbCandidates = @((Join-Path $root 'platform-tools\adb.exe'),(Join-Path $root '..\platform-tools\adb.exe'),'adb.exe')
$adb = $adbCandidates | Where-Object { ($_ -eq 'adb.exe') -or (Test-Path $_) } | Select-Object -First 1
if (-not $adb) { throw 'No se encontró adb.exe.' }
Write-Host 'Este script NO libera Device Owner.' -ForegroundColor Yellow
Write-Host 'Primero debes usar la opción Liberar Device Owner dentro de Administración de Tablet Escolar.' -ForegroundColor Yellow
$policy = (& $adb shell dumpsys device_policy | Select-String -Pattern 'cl.antumapu.aulacontrol')
if ($policy) {
  Write-Host "`nAndroid todavía muestra referencias administrativas a Tablet Escolar:" -ForegroundColor Red
  $policy
  throw 'No se intentará desinstalar hasta liberar completamente la administración.'
}
& $adb uninstall cl.antumapu.aulacontrol
if ($LASTEXITCODE -ne 0) { throw 'Android no pudo desinstalar el paquete.' }
Write-Host 'Tablet Escolar fue desinstalada.' -ForegroundColor Green
