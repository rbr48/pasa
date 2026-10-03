#Requires -Version 5.1
<#
.SYNOPSIS
    PASA Sentinel — Windows Device Owner Setup Wizard
    Guides a non-technical user step-by-step through installing PASA
    and granting Device Owner permission via ADB.
.NOTES
    Version     : 1.0
    Compatible  : Windows 10 / 11
    Requirements: USB cable, Android phone with USB Debugging enabled
#>

param(
    [string]$KitDir = $PSScriptRoot
)

# ─────────────────────────────────────────────────────────────────────────────
#  CONSTANTS & PATHS
# ─────────────────────────────────────────────────────────────────────────────
$PASA_PKG         = "com.izhaanintellect.pasa"
$PASA_ADMIN       = "$PASA_PKG/.admin.PasaDeviceAdmin"
$ADB_DIR          = Join-Path $KitDir "adb"
$ADB_EXE          = Join-Path $ADB_DIR "adb.exe"
$APK_PATH         = Join-Path $env:TEMP "pasa-latest.apk"
$APK_MANIFEST_URL = "https://pasa.izhaanintellect.fun/api/app/latest?current_version_code=0"
$ADB_ZIP_URL      = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
$ADB_ZIP_TEMP     = Join-Path $env:TEMP "platform-tools.zip"
$ADB_TEMP_DIR     = Join-Path $env:TEMP "platform-tools-pasa"

# ─────────────────────────────────────────────────────────────────────────────
#  CONSOLE HELPERS
# ─────────────────────────────────────────────────────────────────────────────
function Clear-Kit { Clear-Host }

function Write-Banner {
    Clear-Kit
    $banner = @"

  ██████╗  █████╗ ███████╗ █████╗     ███████╗███████╗████████╗██╗   ██╗██████╗
  ██╔══██╗██╔══██╗██╔════╝██╔══██╗    ██╔════╝██╔════╝╚══██╔══╝██║   ██║██╔══██╗
  ██████╔╝███████║███████╗███████║    ███████╗█████╗     ██║   ██║   ██║██████╔╝
  ██╔═══╝ ██╔══██║╚════██║██╔══██║    ╚════██║██╔══╝     ██║   ██║   ██║██╔═══╝
  ██║     ██║  ██║███████║██║  ██║    ███████║███████╗   ██║   ╚██████╔╝██║
  ╚═╝     ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝   ╚══════╝╚══════╝   ╚═╝    ╚═════╝ ╚═╝

"@
    Write-Host $banner -ForegroundColor Cyan
    Write-Host "  🛡️  PASA SENTINEL — DEVICE OWNER SETUP KIT  🛡️" -ForegroundColor White
    Write-Host "  Sovereign Mobile Anti-Theft Configuration Wizard" -ForegroundColor DarkCyan
    Write-Host ("  " + "─" * 65) -ForegroundColor DarkGray
    Write-Host ""
}

function Write-Step([int]$num, [int]$total, [string]$title) {
    Write-Host ""
    Write-Host "  ┌─ STEP $num of $total " -NoNewline -ForegroundColor Yellow
    Write-Host "─────────────────────────────────────" -ForegroundColor DarkGray
    Write-Host "  │  $title" -ForegroundColor White
    Write-Host "  └" + ("─" * 50) -ForegroundColor DarkGray
    Write-Host ""
}

function Write-OK([string]$msg)   { Write-Host "  ✅  $msg" -ForegroundColor Green }
function Write-Warn([string]$msg) { Write-Host "  ⚠️   $msg" -ForegroundColor Yellow }
function Write-Err([string]$msg)  { Write-Host "  ❌  $msg" -ForegroundColor Red }
function Write-Info([string]$msg) { Write-Host "  ℹ️   $msg" -ForegroundColor Cyan }
function Write-Bullet([string]$msg) { Write-Host "       • $msg" -ForegroundColor Gray }

function Pause-User([string]$prompt = "Press ENTER to continue...") {
    Write-Host ""
    Write-Host "  $prompt" -ForegroundColor DarkYellow -NoNewline
    Read-Host
}

function Ask-YesNo([string]$question) {
    Write-Host ""
    Write-Host "  $question" -ForegroundColor White
    Write-Host "  [Y] Yes     [N] No" -ForegroundColor DarkGray
    while ($true) {
        $ans = (Read-Host "  Your choice").Trim().ToUpper()
        if ($ans -eq "Y") { return $true }
        if ($ans -eq "N") { return $false }
        Write-Host "  Please type Y or N." -ForegroundColor Red
    }
}

function Show-Progress([string]$activity, [string]$status, [int]$pct) {
    Write-Progress -Activity $activity -Status $status -PercentComplete $pct
}

# ─────────────────────────────────────────────────────────────────────────────
#  ADB HELPERS
# ─────────────────────────────────────────────────────────────────────────────
function Get-AdbExe {
    # 1. Kit's own adb/ folder
    if (Test-Path $ADB_EXE) { return $ADB_EXE }

    # 2. ADB already on PATH
    $adbOnPath = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbOnPath) { return $adbOnPath.Source }

    # 3. Common Android SDK locations
    $candidates = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:ProgramFiles\Android\platform-tools\adb.exe",
        "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }

    return $null
}

function Invoke-Adb([string[]]$args, [bool]$capture = $true) {
    if ($capture) {
        return & $script:ADB $args 2>&1
    } else {
        & $script:ADB $args
    }
}

function Get-ConnectedDevices {
    $raw = Invoke-Adb @("devices")
    $lines = $raw -split "`n" | Select-Object -Skip 1 | Where-Object { $_.Trim() -ne "" }
    return $lines
}

function Get-AuthorizedDevice {
    $devs = Get-ConnectedDevices
    foreach ($line in $devs) {
        if ($line -match "(\S+)\s+device$") {
            return $Matches[1]
        }
    }
    return $null
}

function Get-UnauthorizedDevice {
    $devs = Get-ConnectedDevices
    foreach ($line in $devs) {
        if ($line -match "unauthorized") { return $true }
    }
    return $false
}

function Test-PasaInstalled {
    $result = Invoke-Adb @("shell", "pm", "list", "packages", $PASA_PKG)
    return ($result -match "package:$PASA_PKG")
}

function Test-DeviceOwnerSet {
    $result = Invoke-Adb @("shell", "dpm", "list-owners")
    return ($result -match $PASA_PKG)
}

function Get-DeviceAccounts {
    $result = Invoke-Adb @("shell", "dumpsys", "account") 2>&1
    $accounts = @()
    foreach ($line in ($result -split "`n")) {
        if ($line -match "Account \{name=(.*?), type=(.*?)\}") {
            $accounts += "$($Matches[2]): $($Matches[1])"
        }
    }
    return $accounts
}

function Get-AndroidVersion {
    return (Invoke-Adb @("shell", "getprop", "ro.build.version.release")).Trim()
}

function Get-DeviceModel {
    $brand = (Invoke-Adb @("shell", "getprop", "ro.product.brand")).Trim()
    $model = (Invoke-Adb @("shell", "getprop", "ro.product.model")).Trim()
    return "$brand $model"
}

# ─────────────────────────────────────────────────────────────────────────────
#  STEP 0  —  WELCOME
# ─────────────────────────────────────────────────────────────────────────────
function Show-Welcome {
    Write-Banner
    Write-Host "  Welcome! This wizard will guide you through 6 simple steps:" -ForegroundColor White
    Write-Host ""
    Write-Bullet "Check & download ADB (Android Debug Bridge)"
    Write-Bullet "Connect your Android phone via USB"
    Write-Bullet "Enable USB Debugging on your phone"
    Write-Bullet "Remove phone accounts temporarily"
    Write-Bullet "Install PASA Sentinel on your phone"
    Write-Bullet "Grant Device Owner permission"
    Write-Host ""
    Write-Warn "Total time required: approximately 3–5 minutes"
    Write-Host ""
    Write-Info "Make sure you have your USB cable ready."
    Pause-User "Press ENTER to begin setup..."
}

# ─────────────────────────────────────────────────────────────────────────────
#  STEP 1  —  ADB SETUP
# ─────────────────────────────────────────────────────────────────────────────
function Step-AdbSetup {
    Write-Banner
    Write-Step 1 6 "Setting Up ADB (Android Debug Bridge)"

    $adb = Get-AdbExe
    if ($adb) {
        $script:ADB = $adb
        $version = Invoke-Adb @("version")
        Write-OK "ADB found: $adb"
        Write-OK ($version | Select-Object -First 1)
        Start-Sleep -Seconds 1
        return
    }

    Write-Warn "ADB not found on this computer."
    Write-Info "ADB is a small official Google tool (~8 MB) needed to communicate with your Android phone."
    Write-Info "It will be downloaded now automatically."
    Write-Host ""

    $download = Ask-YesNo "Download ADB automatically from Google's official servers?"
    if (-not $download) {
        Write-Err "ADB is required to continue. Please install Android Platform Tools manually."
        Write-Info "Download from: https://developer.android.com/tools/releases/platform-tools"
        Pause-User
        exit 1
    }

    Write-Host ""
    Write-Info "Downloading ADB from Google servers..."

    try {
        Show-Progress "Downloading ADB" "Connecting to dl.google.com..." 10
        $wc = New-Object System.Net.WebClient
        $wc.DownloadFile($ADB_ZIP_URL, $ADB_ZIP_TEMP)
        Show-Progress "Downloading ADB" "Extracting files..." 70

        if (Test-Path $ADB_TEMP_DIR) { Remove-Item $ADB_TEMP_DIR -Recurse -Force }
        Expand-Archive -Path $ADB_ZIP_TEMP -DestinationPath $ADB_TEMP_DIR -Force

        $extractedTools = Join-Path $ADB_TEMP_DIR "platform-tools"
        if (-not (Test-Path $ADB_DIR)) { New-Item -ItemType Directory -Path $ADB_DIR | Out-Null }

        Copy-Item "$extractedTools\adb.exe"         $ADB_DIR -Force
        Copy-Item "$extractedTools\AdbWinApi.dll"   $ADB_DIR -Force -ErrorAction SilentlyContinue
        Copy-Item "$extractedTools\AdbWinUsbApi.dll" $ADB_DIR -Force -ErrorAction SilentlyContinue

        Show-Progress "Downloading ADB" "Done!" 100
        Start-Sleep -Milliseconds 500
        Write-Progress -Activity "Downloading ADB" -Completed

        $script:ADB = $ADB_EXE
        Write-OK "ADB downloaded and ready!"
    } catch {
        Write-Err "Download failed: $_"
        Write-Info "Please check your internet connection and try again."
        Pause-User
        exit 1
    }
}

# ─────────────────────────────────────────────────────────────────────────────
#  STEP 2  —  CONNECT PHONE
# ─────────────────────────────────────────────────────────────────────────────
function Step-ConnectPhone {
    Write-Banner
    Write-Step 2 6 "Connect Your Android Phone via USB"

    Write-Info "Please follow these steps on your phone now:"
    Write-Host ""
    Write-Host "  1️⃣  Open SETTINGS on your phone" -ForegroundColor White
    Write-Host "  2️⃣  Go to  ABOUT PHONE" -ForegroundColor White
    Write-Host "  3️⃣  Tap  BUILD NUMBER  seven (7) times quickly" -ForegroundColor White
    Write-Host "       → You will see: 'You are now a developer!'" -ForegroundColor DarkGray
    Write-Host "  4️⃣  Go back to SETTINGS → DEVELOPER OPTIONS" -ForegroundColor White
    Write-Host "  5️⃣  Enable  USB DEBUGGING  → Tap OK to confirm" -ForegroundColor White
    Write-Host "  6️⃣  Connect your phone to this computer with the USB cable" -ForegroundColor White
    Write-Host ""
    Write-Warn "When your phone asks 'Allow USB Debugging?', tap ALLOW."
    Write-Host ""

    Pause-User "Once USB Debugging is ON and phone is connected, press ENTER..."

    Write-Host ""
    Write-Info "Restarting ADB server and scanning for devices..."

    Invoke-Adb @("kill-server") | Out-Null
    Start-Sleep -Seconds 1
    Invoke-Adb @("start-server") | Out-Null
    Start-Sleep -Seconds 2

    # Wait for authorized device (up to 60 seconds)
    $waited = 0
    while ($waited -lt 60) {
        $serial = Get-AuthorizedDevice
        if ($serial) {
            $model   = Get-DeviceModel
            $android = Get-AndroidVersion
            Write-Host ""
            Write-OK "Phone connected successfully!"
            Write-Bullet "Model:   $model"
            Write-Bullet "Android: $android"
            Write-Bullet "Serial:  $serial"
            $script:DEVICE_SERIAL = $serial
            Start-Sleep -Seconds 1
            return
        }

        if (Get-UnauthorizedDevice) {
            Write-Warn "Phone detected but waiting for your authorization..."
            Write-Info "Check your phone screen → Tap ALLOW on the 'Allow USB Debugging?' popup."
        } else {
            Write-Info "Waiting for phone... ($waited s)"
        }

        Start-Sleep -Seconds 3
        $waited += 3
    }

    Write-Err "No authorized phone detected after 60 seconds."
    Write-Info "Please make sure:"
    Write-Bullet "USB cable is properly connected"
    Write-Bullet "USB Debugging is enabled in Developer Options"
    Write-Bullet "You tapped ALLOW on your phone"
    Pause-User
    exit 1
}

# ─────────────────────────────────────────────────────────────────────────────
#  STEP 3  —  REMOVE ACCOUNTS
# ─────────────────────────────────────────────────────────────────────────────
function Get-DeviceAccountsRaw {
    # Returns structured list of [name, type] pairs
    $raw = Invoke-Adb @("shell", "dumpsys", "account") 2>&1
    $accounts = @()
    foreach ($line in ($raw -split "`n")) {
        if ($line -match "Account \{name=(.*?), type=(.*?)\}") {
            $accounts += [PSCustomObject]@{ Name = $Matches[1].Trim(); Type = $Matches[2].Trim() }
        }
    }
    return $accounts
}

function Open-AccountRemovalScreen([string]$accountName, [string]$accountType) {
    # Try the deepest direct intent first (Android 8+)
    $result = Invoke-Adb @(
        "shell", "am", "start",
        "-a", "android.settings.ACCOUNT_SYNC_SETTINGS",
        "--es", "account_name", $accountName,
        "--es", "account_type", $accountType
    )
    # Fallback: open generic accounts list
    if (($result -join "") -match "Error|does not exist") {
        Invoke-Adb @("shell", "am", "start", "-a", "android.settings.SYNC_SETTINGS") | Out-Null
    }
}

function Step-RemoveAccounts {
    Write-Banner
    Write-Step 3 6 "Remove All Accounts from Phone (Temporary)"

    Write-Info "Android requires ZERO accounts before granting Device Owner."
    Write-Info "You can add your accounts back after setup is complete."
    Write-Host ""

    $accounts = Get-DeviceAccountsRaw
    if ($accounts.Count -eq 0) {
        Write-OK "No accounts detected on this phone. You're all set!"
        Start-Sleep -Seconds 1
        return
    }

    Write-Warn "$($accounts.Count) account(s) found — this wizard will open each one for you."
    Write-Info "For each account: just tap  REMOVE ACCOUNT  on your phone."
    Write-Host ""

    $i = 0
    foreach ($acc in $accounts) {
        $i++
        Write-Host ("  " + "─" * 60) -ForegroundColor DarkGray
        Write-Host "  Account $i of $($accounts.Count)" -ForegroundColor Yellow
        Write-Host "  Type  : $($acc.Type)" -ForegroundColor White
        Write-Host "  Name  : $($acc.Name)" -ForegroundColor White
        Write-Host ""

        Write-Info "Opening account removal screen on your phone..."
        Open-AccountRemovalScreen $acc.Name $acc.Type
        Start-Sleep -Seconds 2

        Write-Warn "Your phone screen should now show this account's settings."
        Write-Host ""
        Write-Host "  👉  On your phone: tap  REMOVE ACCOUNT  (or 'Delete Account')" -ForegroundColor Cyan
        Write-Host "  👉  If asked to confirm — tap  OK  or  REMOVE" -ForegroundColor Cyan
        Write-Host ""

        Pause-User "Once '$($acc.Name)' is removed, press ENTER to continue..."
    }

    Write-Host ""
    Write-Info "Verifying all accounts have been removed..."
    Start-Sleep -Seconds 2

    $remaining = Get-DeviceAccountsRaw

    # Filter out system-internal accounts that cannot be removed by user
    $systemTypes = @("com.android.localTransport", "com.android.exchange",
                     ".backgroundData", "sprd.com.android", "com.sec.android.provider.badge")
    $blockingAccounts = $remaining | Where-Object {
        $t = $_.Type
        -not ($systemTypes | Where-Object { $t -like "*$_*" })
    }

    if ($blockingAccounts.Count -eq 0) {
        Write-OK "All user accounts successfully removed!"
        if ($remaining.Count -gt 0) {
            Write-Info "($($remaining.Count) internal system account(s) remain — these are safe to ignore)"
        }
        Start-Sleep -Seconds 1
    } else {
        Write-Err "$($blockingAccounts.Count) account(s) still present:"
        foreach ($acc in $blockingAccounts) {
            Write-Host "       🔴  $($acc.Type): $($acc.Name)" -ForegroundColor Red
        }
        Write-Host ""
        Write-Info "Tip: Some OEM accounts (Samsung, Xiaomi) may need removal from their own app."
        Write-Bullet "Samsung: Settings → Accounts → Samsung Account → Sign Out"
        Write-Bullet "Xiaomi:  Settings → Mi Account → Sign Out"
        Write-Host ""
        $force = Ask-YesNo "Try to continue anyway? (Device Owner step may fail if accounts remain)"
        if (-not $force) {
            Write-Info "Please remove remaining accounts and run this setup again."
            Pause-User; exit 1
        }
        Write-Warn "Continuing with accounts present. Device Owner step may fail."
    }
}


# ─────────────────────────────────────────────────────────────────────────────
#  STEP 4  —  DOWNLOAD & INSTALL APK (Smart Version Check)
# ─────────────────────────────────────────────────────────────────────────────
function Get-InstalledVersionCode {
    # Reads the versionCode of the installed PASA package via ADB dumpsys
    $raw = Invoke-Adb @("shell", "dumpsys", "package", $PASA_PKG) 2>&1
    foreach ($line in ($raw -split "`n")) {
        if ($line -match "versionCode=(\d+)") {
            return [int]$Matches[1]
        }
    }
    return 0
}

function Step-InstallApk {
    Write-Banner
    Write-Step 4 6 "Checking & Installing PASA Sentinel"

    # ── Always fetch latest metadata from server first ─────────────────────
    Write-Info "Checking latest PASA version from server..."
    try {
        $manifest = Invoke-RestMethod -Uri $APK_MANIFEST_URL -UseBasicParsing -TimeoutSec 15
        $apkUrl   = $manifest.latest.downloadUrl
        $apkVer   = $manifest.latest.versionName
        $apkBuild = [int]$manifest.latest.versionCode
        $apkSize  = [math]::Round($manifest.latest.fileSize / 1MB, 1)
        $apkHash  = $manifest.latest.sha256
        Write-OK "Server latest: v$apkVer (Build $apkBuild)"
    } catch {
        Write-Err "Could not reach PASA server: $_"
        Write-Info "Please check your internet connection and try again."
        Pause-User; exit 1
    }

    # ── Compare with installed version on phone ────────────────────────────
    $alreadyInstalled = Test-PasaInstalled
    if ($alreadyInstalled) {
        $installedBuild = Get-InstalledVersionCode
        Write-Info "Phone has: Build $installedBuild"
        Write-Host ""

        if ($installedBuild -ge $apkBuild) {
            # ✅ Already up to date — skip download entirely
            Write-OK "PASA Sentinel is already up to date on this phone!"
            Write-Bullet "Installed: v$apkVer (Build $installedBuild)"
            Write-Bullet "Server:    v$apkVer (Build $apkBuild)"
            Write-Host ""
            Write-Info "No download needed. Continuing to next step..."
            Start-Sleep -Seconds 2
            return
        } else {
            # ⬆️ Newer version available
            Write-Warn "A newer version is available!"
            Write-Bullet "Installed: Build $installedBuild"
            Write-Bullet "Available: Build $apkBuild (v$apkVer) — $apkSize MB"
            Write-Host ""
            $doUpdate = Ask-YesNo "Update PASA Sentinel to v$apkVer now?"
            if (-not $doUpdate) {
                Write-Info "Skipping update. Keeping installed Build $installedBuild."
                return
            }
        }
    } else {
        Write-Info "PASA Sentinel not found on this phone."
        Write-Bullet "Will download: v$apkVer (Build $apkBuild) — $apkSize MB"
    }

    # ── Download APK ──────────────────────────────────────────────────────
    Write-Host ""
    Write-Info "Downloading PASA Sentinel v$apkVer..."

    try {
        $wc = New-Object System.Net.WebClient
        $wc.DownloadProgressChanged += {
            param($s, $e)
            Show-Progress "Downloading PASA Sentinel v$apkVer" "$($e.ProgressPercentage)%  ($apkSize MB total)" $e.ProgressPercentage
        }
        $wc.DownloadFile($apkUrl, $APK_PATH)
        Write-Progress -Activity "Downloading PASA Sentinel" -Completed
    } catch {
        Write-Info "Trying alternate download method..."
        try {
            Invoke-WebRequest -Uri $apkUrl -OutFile $APK_PATH -UseBasicParsing
        } catch {
            Write-Err "Download failed: $_"
            Pause-User; exit 1
        }
    }

    # ── Verify SHA-256 integrity ──────────────────────────────────────────
    if ($apkHash -and $apkHash.Length -eq 64) {
        Write-Info "Verifying download integrity (SHA-256)..."
        $localHash = (Get-FileHash -Path $APK_PATH -Algorithm SHA256).Hash.ToLower()
        if ($localHash -eq $apkHash.ToLower()) {
            Write-OK "File integrity verified."
        } else {
            Write-Err "Integrity check failed — file may be corrupted or tampered."
            Write-Bullet "Expected: $apkHash"
            Write-Bullet "Got:      $localHash"
            Remove-Item $APK_PATH -Force -ErrorAction SilentlyContinue
            Pause-User; exit 1
        }
    }

    $dlSizeMB = [math]::Round((Get-Item $APK_PATH).Length / 1MB, 1)
    Write-OK "Downloaded: pasa-v$apkVer-$apkBuild.apk ($dlSizeMB MB)"

    # ── Install on device ─────────────────────────────────────────────────
    Write-Host ""
    Write-Info "Installing on your phone... (may take 15–30 seconds)"
    Write-Warn "If your phone shows 'Install Blocked', tap SETTINGS → enable 'Unknown Sources'."
    Write-Host ""

    $result    = Invoke-Adb @("install", "-r", $APK_PATH)
    $resultStr = ($result -join " ")

    if ($resultStr -match "Success") {
        Write-OK "PASA Sentinel installed successfully!"
    } elseif ($resultStr -match "INSTALL_FAILED_UPDATE_INCOMPATIBLE") {
        Write-Warn "Incompatible version. Removing old install and re-trying..."
        Invoke-Adb @("uninstall", $PASA_PKG) | Out-Null
        $result2 = Invoke-Adb @("install", $APK_PATH)
        if (($result2 -join " ") -match "Success") {
            Write-OK "PASA Sentinel installed successfully!"
        } else {
            Write-Err "Installation failed: $result2"
            Pause-User; exit 1
        }
    } else {
        Write-Err "Installation failed!"
        Write-Host "  Output: $resultStr" -ForegroundColor Red
        Pause-User; exit 1
    }
    Start-Sleep -Seconds 1
}

# ─────────────────────────────────────────────────────────────────────────────
#  STEP 5  —  GRANT DEVICE OWNER
# ─────────────────────────────────────────────────────────────────────────────
function Step-SetDeviceOwner {
    Write-Banner
    Write-Step 5 6 "Granting Device Owner Permission"

    # Check already set
    if (Test-DeviceOwnerSet) {
        Write-OK "Device Owner is ALREADY active on this phone for PASA Sentinel!"
        Write-Info "No further action needed for this step."
        Start-Sleep -Seconds 2
        return
    }

    Write-Info "Running the Device Owner command on your phone..."
    Write-Host ""

    $result = Invoke-Adb @("shell", "dpm", "set-device-owner", $PASA_ADMIN)
    $resultStr = ($result -join " ").Trim()

    if ($resultStr -match "Success") {
        Write-OK "Device Owner granted successfully!"
        Write-OK "PASA Sentinel now has full Knox-grade anti-theft privileges."
    } elseif ($resultStr -match "already some accounts") {
        Write-Err "FAILED: Accounts still present on the phone."
        Write-Warn "Please go back and remove ALL accounts from Settings → Accounts."
        Write-Host ""
        Write-Info "After removing accounts, run this setup again."
        Pause-User
        exit 1
    } elseif ($resultStr -match "already set") {
        Write-Warn "A different app is already Device Owner on this phone."
        Write-Info "You need to Factory Reset the phone and run this setup again."
        Pause-User
        exit 1
    } else {
        Write-Err "Unexpected error: $resultStr"
        Write-Info "If you see 'accounts' in the error, please remove all phone accounts and retry."
        Pause-User
        exit 1
    }
    Start-Sleep -Seconds 1
}

# ─────────────────────────────────────────────────────────────────────────────
#  STEP 6  —  DONE
# ─────────────────────────────────────────────────────────────────────────────
function Step-Finish {
    Write-Banner
    Write-Step 6 6 "Setup Complete!"

    Write-Host ""
    Write-Host "  ╔══════════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "  ║                                                          ║" -ForegroundColor Green
    Write-Host "  ║   🎉  PASA Sentinel is fully configured and active!  🎉  ║" -ForegroundColor Green
    Write-Host "  ║                                                          ║" -ForegroundColor Green
    Write-Host "  ╚══════════════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    Write-Host "  What to do next:" -ForegroundColor White
    Write-Host ""
    Write-Bullet "Open PASA Sentinel app on your phone to complete initial setup."
    Write-Bullet "Connect to your Telegram bot by entering your Bot Token."
    Write-Bullet "You can now add your Google Account back to the phone."
    Write-Bullet "Type /status in Telegram to verify the device is online."
    Write-Host ""
    Write-Host ("  " + "─" * 65) -ForegroundColor DarkGray
    Write-Host ""
    Write-Info "For support, visit: https://pasa.izhaanintellect.fun"
    Write-Info "Telegram bot: @Pas_agent_bot"
    Write-Host ""

    # Verify final state
    $installed = Test-PasaInstalled
    $isOwner   = Test-DeviceOwnerSet
    $model     = Get-DeviceModel
    $android   = Get-AndroidVersion

    Write-Host "  📋 Setup Summary:" -ForegroundColor White
    Write-Host ""
    if ($installed) { Write-OK "PASA Sentinel Installed" } else { Write-Err "PASA Sentinel NOT Installed" }
    if ($isOwner)   { Write-OK "Device Owner Active"       } else { Write-Err "Device Owner NOT Active"   }
    Write-Info "Device: $model (Android $android)"
    Write-Host ""

    Pause-User "Press ENTER to close this window..."
}

# ─────────────────────────────────────────────────────────────────────────────
#  MAIN EXECUTION
# ─────────────────────────────────────────────────────────────────────────────
try {
    Show-Welcome
    Step-AdbSetup
    Step-ConnectPhone
    Step-RemoveAccounts
    Step-InstallApk
    Step-SetDeviceOwner
    Step-Finish
} catch {
    Write-Host ""
    Write-Host "  ❌ An unexpected error occurred:" -ForegroundColor Red
    Write-Host "  $_" -ForegroundColor Red
    Write-Host ""
    Pause-User "Press ENTER to close..."
    exit 1
}
