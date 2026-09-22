param([string]$Adb="adb")
Write-Host "=== PANGI V20.5 · Verificación previa ===" -ForegroundColor Cyan
& $Adb devices
Write-Host ""
Write-Host "Usuarios:"
& $Adb shell pm list users
Write-Host ""
Write-Host "Cuentas:"
& $Adb shell dumpsys account | Select-String "Accounts:|Account \{"
Write-Host ""
Write-Host "Device Owner / Profile Owner:"
& $Adb shell dumpsys device_policy | Select-String "Device Owner|Profile Owner|cl.antumapu"
Write-Host ""
Write-Host "PANGI instalado:"
& $Adb shell dumpsys package cl.antumapu.pangi.v205 | Select-String "versionName|versionCode"
