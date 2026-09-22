param([string]$Adb="adb")
Write-Host "=== PANGI V20.5 ===" -ForegroundColor Cyan
& $Adb shell dumpsys package cl.antumapu.pangi.v205 | Select-String "versionName|versionCode"
& $Adb shell dumpsys device_policy | Select-String "Device Owner|cl.antumapu.pangi.v205"
& $Adb shell pm list users
