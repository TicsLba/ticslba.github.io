param([string]$Adb="adb")
Write-Host "Antes de continuar, la tablet debe tener un solo usuario y 0 cuentas." -ForegroundColor Yellow
& $Adb shell dumpsys account | Select-String "Accounts:|Account \{"
& $Adb shell pm list users
Write-Host ""
Write-Host "Activando PANGI como Device Owner..." -ForegroundColor Cyan
& $Adb shell dpm set-device-owner cl.antumapu.pangi.v205/.AdminReceiver
