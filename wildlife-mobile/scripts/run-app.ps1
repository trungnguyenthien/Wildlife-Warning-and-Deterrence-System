# run-app.ps1
# Script bien dich, cai dat va chay Android App tren Windows 11 qua PowerShell

# Lay duong dan thu muc chua script de di chuyen ve thu muc gốc cua project mobile
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location (Split-Path $scriptDir -Parent)

# 1. Do tim duong dan SDK Android
$sdkDir = ""
if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) {
    $sdkDir = $env:ANDROID_HOME
} elseif (Test-Path "$env:LOCALAPPDATA\Google\AndroidSDK") {
    $sdkDir = "$env:LOCALAPPDATA\Google\AndroidSDK"
} elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk") {
    $sdkDir = "$env:LOCALAPPDATA\Android\Sdk"
}

if ([string]::IsNullOrEmpty($sdkDir) -and (Test-Path "local.properties")) {
    $prop = Get-Content "local.properties" | Select-String "sdk.dir="
    if ($prop) {
        $sdkDir = ($prop -split "=")[1].Replace('\\', '\').Trim()
    }
}

if ([string]::IsNullOrEmpty($sdkDir) -or -not (Test-Path $sdkDir)) {
    Write-Host "-----------------------------------------" -ForegroundColor Yellow
    Write-Host "LOI: Khong tim thay Android SDK hop le. Vui long kiem tra Android Studio." -ForegroundColor Red
    Write-Host "-----------------------------------------" -ForegroundColor Yellow
    Pop-Location
    exit 1
}

$adbPath = "$sdkDir\platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) {
    Write-Error "LOI: Khong tim thay adb.exe tai $adbPath"
    Pop-Location
    exit 1
}

# Kiem tra thiet bi online, neu khong co thi tu dong khoi dong may ao
$devices = & $adbPath devices | Where-Object { $_ -match "\bdevice\b" }
if (-not $devices) {
    Write-Host "Khong tim thay thiet bi Android online. Dang kiem tra may ao (AVD)..." -ForegroundColor Yellow
    $emulatorPath = "$sdkDir\emulator\emulator.exe"
    if (Test-Path $emulatorPath) {
        $avds = & $emulatorPath -list-avds
        if ($avds) {
            $avdName = $avds[0]
            Write-Host "Dang tu dong khoi dong may ao: $avdName ..." -ForegroundColor Cyan
            Start-Process $emulatorPath -ArgumentList "-avd $avdName"
            Write-Host "Dang cho thiet bi ket noi adb (co the mat 30-60 giay)..." -ForegroundColor Yellow
            & $adbPath wait-for-device
            Write-Host "Da ket noi may ao thanh cong!" -ForegroundColor Green
            Start-Sleep -Seconds 3
        } else {
            Write-Warning "Khong tim thay bat ky may ao AVD nao trong SDK. Vui long bat thiet bi hoac tao may ao trong Android Studio."
        }
    } else {
        Write-Warning "Khong tim thay emulator.exe de tu dong bat may ao."
    }
}

# Tu dong do tim va thiet lap JAVA_HOME neu chua co cho phien chay nay
if ([string]::IsNullOrEmpty($env:JAVA_HOME) -or -not (Test-Path $env:JAVA_HOME)) {
    $portableJdk = "$env:USERPROFILE\.openjdk\jdk-17.0.19+10"
    if (Test-Path $portableJdk) {
        $env:JAVA_HOME = $portableJdk
        $env:PATH = "$portableJdk\bin;$env:PATH"
    } else {
        $msJdk = Get-ChildItem -Path "C:\Program Files\Microsoft" -Filter "jdk-17*" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($msJdk) {
            $env:JAVA_HOME = $msJdk.FullName
            $env:PATH = "$($msJdk.FullName)\bin;$env:PATH"
        }
    }
}

Write-Host "=== DANG BIEN DICH & CAI DAT UNG DUNG ===" -ForegroundColor Cyan
Write-Host "Dang chay gradlew installDebug (co the mat 1-2 phut)..."
& .\gradlew.bat installDebug

if ($LASTEXITCODE -ne 0) {
    Write-Error "LOI: Bien dich Gradle that bai."
    Pop-Location
    exit 1
}

Write-Host "Dang mo ung dung..." -ForegroundColor Cyan
& $adbPath shell am start -n com.wildlife.deterrence/.MainActivity

if ($LASTEXITCODE -ne 0) {
    Write-Error "LOI: Khong the khoi chay MainActivity. Dam bao thiet bi ao da bat va ket noi adb."
    Pop-Location
    exit 1
}

Write-Host "=== UNG DUNG DA KHOI CHAY THANH CONG! ===" -ForegroundColor Green
Write-Host "Dang loc logcat cua ung dung (Nhan Ctrl+C de dung)..." -ForegroundColor Yellow

Pop-Location
& $adbPath logcat | Select-String "com.wildlife.deterrence"
