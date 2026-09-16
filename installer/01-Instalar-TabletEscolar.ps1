$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$adbCandidates = @(
  (Join-Path $root 'platform-tools\adb.exe'),
  (Join-Path $root '..\platform-tools\adb.exe'),
  'adb.exe'
)
$adb = $adbCandidates | Where-Object { ($_ -eq 'adb.exe') -or (Test-Path $_) } | Select-Object -First 1
if (-not $adb) { throw 'No se encontró adb.exe. Copia la carpeta platform-tools junto a este instalador.' }
$apk = Get-ChildItem -Path $root -Filter 'TabletEscolar-*.apk' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) { $apk = Get-ChildItem -Path (Join-Path $root '..\Android') -Filter 'TabletEscolar-*.apk' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1 }
if (-not $apk) { throw 'No se encontró la APK de Tablet Escolar.' }
Write-Host '1) Conecta la tablet por USB y acepta la depuración USB.' -ForegroundColor Cyan
& $adb devices
Write-Host "`n2) Instalando $($apk.Name)..." -ForegroundColor Cyan
& $adb install -r $apk.FullName
if ($LASTEXITCODE -ne 0) { throw 'La instalación ADB no finalizó correctamente.' }
Write-Host '`nAPK instalada. Ejecuta ahora 02-Activar-DeviceOwner.ps1.' -ForegroundColor Green
