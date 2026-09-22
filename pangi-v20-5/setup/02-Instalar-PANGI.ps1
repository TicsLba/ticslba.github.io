param(
 [Parameter(Mandatory=$true)][string]$Apk,
 [string]$Adb="adb"
)
if(!(Test-Path $Apk)){ throw "No existe el APK: $Apk" }
Write-Host "Instalando PANGI V20.5..." -ForegroundColor Cyan
& $Adb install -r $Apk
if($LASTEXITCODE -ne 0){ throw "ADB devolvió error durante la instalación." }
Write-Host "Instalación completada." -ForegroundColor Green
