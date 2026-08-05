# run-emulator.ps1
# Script khoi chay Android Emulator tren Windows 11 qua PowerShell

Write-Host "=== DANG KIEM TRA THIET BI AO (AVD) ==="

# 1. Do tim duong dan SDK Android
$sdkDir = ""
if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) {
    $sdkDir = $env:ANDROID_HOME
} elseif (Test-Path "$env:LOCALAPPDATA\Google\AndroidSDK") {
    $sdkDir = "$env:LOCALAPPDATA\Google\AndroidSDK"
} elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk") {
    $sdkDir = "$env:LOCALAPPDATA\Android\Sdk"
}

if ([string]::IsNullOrEmpty($sdkDir)) {
    Write-Host "-----------------------------------------" -ForegroundColor Yellow
    Write-Host "LOI: Khong tim thay Android SDK tren he thong!" -ForegroundColor Red
    Write-Host "Android Studio da duoc cai dat tai: C:\Program Files\Android\Android Studio"
    Write-Host "Huong dan khac phuc:"
    Write-Host "1. Vui long mo Android Studio va hoan tat 'Setup Wizard' (chon Standard) de tu dong tai SDK ve may."
    Write-Host "2. Sau khi Android Studio tai xong SDK, hay chay lai script nay."
    Write-Host "-----------------------------------------" -ForegroundColor Yellow
    exit 1
}

# 2. Xac dinh duong dan emulator va adb
$emulatorPath = "$sdkDir\emulator\emulator.exe"
$adbPath = "$sdkDir\platform-tools\adb.exe"

if (-not (Test-Path $emulatorPath)) {
    Write-Error "LOI: Khong tim thay emulator.exe tai $emulatorPath"
    exit 1
}

# Them thu muc SDK vao PATH cua phien hien tai
$env:PATH = "$sdkDir\emulator;$sdkDir\platform-tools;$env:PATH"

# 3. Lay danh sach AVD
$avdList = & $emulatorPath -list-avds 2>$null
if ([string]::IsNullOrEmpty($avdList)) {
    Write-Host "-----------------------------------------" -ForegroundColor Yellow
    Write-Host "LOI: Khong tim thay thiet bi ao (AVD) nao!" -ForegroundColor Red
    Write-Host "Huong dan khac phuc:"
    Write-Host "1. Mo Android Studio."
    Write-Host "2. Vao More Actions -> Virtual Device Manager."
    Write-Host "3. Tao mot thiet bi ao moi (vi du: Pixel 6, API 34)."
    Write-Host "4. Chay lai script nay."
    Write-Host "-----------------------------------------" -ForegroundColor Yellow
    exit 1
}

$avdArray = $avdList -split "`r`n" | Where-Object { $_ -ne "" }
Write-Host "Danh sach Emulator (AVD) kha dung:"
foreach ($avd in $avdArray) {
    Write-Host " - $avd"
}

$firstAvd = $avdArray[0]
Write-Host "Dang khoi chay Emulator: $firstAvd..." -ForegroundColor Cyan

# Su dung Start-Process de chay emulator ngam ma khong khoa terminal
Start-Process -FilePath $emulatorPath -ArgumentList "-avd `"$firstAvd`" -dns-server 8.8.8.8" -WindowStyle Hidden

Write-Host "Dang cho thiet bi ao boot hoan tat..."
$bootCompleted = $false
while (-not $bootCompleted) {
    Start-Sleep -Seconds 2
    $status = & $adbPath shell getprop sys.boot_completed 2>$null
    $cleanStatus = if ($status) { $status.Trim() } else { "" }
    if ($cleanStatus -eq "1") {
        $bootCompleted = $true
    } else {
        Write-Host -NoNewline "."
    }
}
Write-Host ""
Write-Host "=== THIET BI AO DA SAN SANG! ===" -ForegroundColor Green
