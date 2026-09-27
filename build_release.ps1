<#
.SYNOPSIS
    PASA Sentinel -- Master Build & Deploy Script
    Bumps version, builds release APK, and deploys to VPS in one command.

.PARAMETER VersionName
    New semantic version string, e.g. "3.7.6"

.PARAMETER VersionCode
    New integer build number, e.g. 71

.PARAMETER SkipDeploy
    If set, builds APK only without deploying to VPS.

.EXAMPLE
    .\build_release.ps1 -VersionName "3.7.6" -VersionCode 71
    .\build_release.ps1 -VersionName "3.7.6" -VersionCode 71 -SkipDeploy
#>

param(
    [Parameter(Mandatory=$true)]  [string]$VersionName,
    [Parameter(Mandatory=$true)]  [int]$VersionCode,
    [switch]$SkipDeploy
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------
$ProjectRoot     = $PSScriptRoot
$BuildGradle     = Join-Path $ProjectRoot "app\build.gradle.kts"
$AppBuildDir     = Join-Path $ProjectRoot "app\build"
$ReleasesDir     = Join-Path $ProjectRoot "releases"
$VpsUser         = "root"
$VpsHost         = "148.135.137.245"
$VpsPort         = 2222
$SshKey          = "C:\Users\USER\.ssh\id_rsa_dbm"
$VpsWebRoot      = "/var/www/pasa-commercial-web/public/releases"
$VpsServerRoot   = "/var/www/pasa-server/releases"
$VpsInjectScript = "/var/www/pasa-server/inject_release.js"
$ApkName         = "pasa-v${VersionName}-${VersionCode}.apk"
$ApkSrc          = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"
$ApkDest         = Join-Path $ReleasesDir $ApkName
$ApkLatest       = Join-Path $ReleasesDir "pasa-latest.apk"
$LocalJdk        = "C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"

# Ensure local build uses JDK 17 without polluting repository gradle.properties
if (Test-Path $LocalJdk) {
    $env:JAVA_HOME = $LocalJdk
    $env:PATH = "$LocalJdk\bin;" + $env:PATH
}

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
function Write-Step([string]$msg) {
    Write-Host ""
    Write-Host "--------------------------------------------------" -ForegroundColor Cyan
    Write-Host "  $msg" -ForegroundColor Cyan
    Write-Host "--------------------------------------------------" -ForegroundColor Cyan
}
function Write-OK([string]$msg)   { Write-Host "  OK  $msg" -ForegroundColor Green }
function Write-Warn([string]$msg) { Write-Host "  WARN  $msg" -ForegroundColor Yellow }
function Fail([string]$msg)       { Write-Host "  FAIL  $msg" -ForegroundColor Red; exit 1 }

function Invoke-Scp([string]$src, [string]$dst) {
    & scp -O -P $VpsPort -i $SshKey $src "${VpsUser}@${VpsHost}:${dst}"
    if ($LASTEXITCODE -ne 0) { Fail "SCP failed uploading $src" }
}

function Invoke-Ssh([string]$cmd) {
    $out = & ssh -p $VpsPort -i $SshKey "${VpsUser}@${VpsHost}" $cmd 2>&1
    if ($LASTEXITCODE -ne 0) { Fail "SSH command failed.`nCmd: $cmd`nOutput: $out" }
    return $out
}

# ---------------------------------------------------------------------------
# STEP 1 -- Validate inputs
# ---------------------------------------------------------------------------
Write-Step "STEP 1 -- Validate inputs"

if ($VersionCode -le 0)                        { Fail "VersionCode must be a positive integer." }
if ($VersionName -notmatch '^\d+\.\d+\.\d+$') { Fail "VersionName must be semver, e.g. 3.7.6" }
if (-not (Test-Path $BuildGradle))             { Fail "build.gradle.kts not found." }
if (-not (Test-Path (Join-Path $ProjectRoot "gradlew.bat"))) { Fail "gradlew.bat not found. Run from project root." }

Write-OK "v${VersionName} (Build ${VersionCode})"

# ---------------------------------------------------------------------------
# STEP 2 -- Bump version in build.gradle.kts
# ---------------------------------------------------------------------------
Write-Step "STEP 2 -- Bump version in build.gradle.kts"

$gradleLines = Get-Content $BuildGradle
$newLines = @()
$updatedCode = $false
$updatedName = $false

foreach ($line in $gradleLines) {
    if ($line -match '^\s*versionCode\s*=\s*\d+') {
        $newLines += "        versionCode = $VersionCode"
        $updatedCode = $true
    } elseif ($line -match '^\s*versionName\s*=\s*"') {
        $newLines += '        versionName = "' + $VersionName + '"'
        $updatedName = $true
    } else {
        $newLines += $line
    }
}

if (-not $updatedCode) { Fail "Could not find versionCode line in build.gradle.kts" }
if (-not $updatedName) { Fail "Could not find versionName line in build.gradle.kts" }

Set-Content -Path $BuildGradle -Value $newLines -Encoding UTF8
Write-OK "build.gradle.kts: versionCode=$VersionCode versionName=$VersionName"

# Update AGENTS.md version reference (simple string replace, no regex needed)
$agentsMd = Join-Path $ProjectRoot "AGENTS.md"
if (Test-Path $agentsMd) {
    $agentsContent = Get-Content $agentsMd -Raw
    # Replace any "vX.Y.Z (Build N)" pattern
    $agentsContent = $agentsContent -replace 'v\d+\.\d+\.\d+ \(Build \d+\)', "v${VersionName} (Build ${VersionCode})"
    Set-Content -Path $agentsMd -Value $agentsContent -Encoding UTF8
    Write-OK "AGENTS.md version reference updated"
}

# ---------------------------------------------------------------------------
# STEP 3 -- Kill Java processes + wipe app/build
# ---------------------------------------------------------------------------
Write-Step "STEP 3 -- Kill stale Java processes and clear build cache"

# Gracefully stop Gradle daemons
try {
    & "$ProjectRoot\gradlew.bat" --stop 2>&1 | Out-Null
    Write-OK "Gradle daemons stopped"
} catch {
    Write-Warn "gradlew --stop skipped (no daemons running)"
}

# Kill any remaining Java processes to release file handles
$javaProcs = @(Get-Process -Name "java" -ErrorAction SilentlyContinue)
if ($javaProcs.Count -gt 0) {
    $count = $javaProcs.Count
    $javaProcs | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Write-OK "Killed $count Java process(es)"
} else {
    Write-OK "No lingering Java processes"
}

# Nuke entire app/build directory -- the root cause of all Windows file-lock failures
if (Test-Path $AppBuildDir) {
    Remove-Item -Recurse -Force $AppBuildDir -ErrorAction SilentlyContinue
    if (Test-Path $AppBuildDir) {
        # Last resort: use robocopy mirror with empty dir to force-delete
        $tmp = New-Item -ItemType Directory -Path "$env:TEMP\pasa_empty_build" -Force
        & robocopy "$tmp" "$AppBuildDir" /MIR /NFL /NDL /NJH /NJS 2>&1 | Out-Null
        Remove-Item -Recurse -Force $AppBuildDir -ErrorAction SilentlyContinue
        Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
    }
    Write-OK "app\build directory wiped"
} else {
    Write-OK "app\build was already clean"
}

# ---------------------------------------------------------------------------
# STEP 4 -- Build release APK
# ---------------------------------------------------------------------------
Write-Step "STEP 4 -- Build release APK (takes ~2-3 minutes)"

$buildStart   = Get-Date
$buildLogErr  = Join-Path $env:TEMP "pasa_build_stderr.log"
$buildLogOut  = Join-Path $env:TEMP "pasa_build_stdout.log"

$gradleArgs = @("assembleRelease", "--no-daemon")
$localJdk = "C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"
if (Test-Path $localJdk) {
    $gradleArgs += "-Dorg.gradle.java.home=`"$localJdk`""
}

Push-Location $ProjectRoot
# Run Gradle with stdout and stderr to separate temp files so JVM warnings on
# stderr never trigger PowerShell's ErrorActionPreference = Stop mechanism.
$proc = Start-Process -FilePath ".\gradlew.bat" `
    -ArgumentList $gradleArgs `
    -RedirectStandardOutput $buildLogOut `
    -RedirectStandardError  $buildLogErr `
    -NoNewWindow -Wait -PassThru
Pop-Location

$buildSecs = [math]::Round(((Get-Date) - $buildStart).TotalSeconds)

# Stream filtered build output for visibility
if (Test-Path $buildLogOut) {
    Get-Content $buildLogOut | ForEach-Object {
        $line = "$_"
        if ($line -match '> Task|BUILD |FAILURE|error:|Exception') {
            $color = if ($line -match 'FAILED|FAILURE|error|Error|Exception') { 'Red' } else { 'DarkGray' }
            Write-Host "  $line" -ForegroundColor $color
        }
    }
}

if ($proc.ExitCode -ne 0 -or -not (Test-Path $ApkSrc)) {
    # Show last 30 lines of stderr for diagnosis
    Write-Host ""
    Write-Host "  -- BUILD ERRORS --" -ForegroundColor Red
    if (Test-Path $buildLogErr) {
        Get-Content $buildLogErr | Select-Object -Last 30 | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    }
    Fail "Build FAILED (exit code $($proc.ExitCode)) -- APK not produced."
}
Write-OK "Build succeeded in ${buildSecs}s"

# ---------------------------------------------------------------------------
# STEP 5 -- Copy APK to releases/ and compute SHA-256
# ---------------------------------------------------------------------------
Write-Step "STEP 5 -- Copy APK and compute SHA-256"

if (-not (Test-Path $ReleasesDir)) { New-Item -ItemType Directory -Path $ReleasesDir -Force | Out-Null }

Copy-Item $ApkSrc $ApkDest  -Force
Copy-Item $ApkSrc $ApkLatest -Force

$sha256    = (Get-FileHash $ApkDest -Algorithm SHA256).Hash.ToLower()
$sizeMB    = [math]::Round((Get-Item $ApkDest).Length / 1MB, 2)
$sizeBytes = (Get-Item $ApkDest).Length

Write-OK "releases\$ApkName"
Write-Host "  SHA-256 : $sha256" -ForegroundColor White
Write-Host "  Size    : $sizeMB MB" -ForegroundColor White

# Update local release catalog & independent audit report
$updateScript = Join-Path $ProjectRoot "scripts\update_release.js"
if (Test-Path $updateScript) {
    & node $updateScript $ApkName
    Write-OK "Local release catalog updated ($ApkName)"
}
$auditScript = Join-Path $ProjectRoot "scripts\verify_independent_audit.js"
if (Test-Path $auditScript) {
    & node $auditScript
    Write-OK "Independent audit report updated"
}

if ($SkipDeploy) {
    Write-Host ""
    Write-Host "  SkipDeploy set -- stopping after local build." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "DONE: v${VersionName} (Build ${VersionCode})" -ForegroundColor Green
    exit 0
}

# ---------------------------------------------------------------------------
# STEP 6 -- Upload APK to VPS (both locations)
# ---------------------------------------------------------------------------
Write-Step "STEP 6 -- Upload APK to VPS"

Invoke-Scp $ApkDest "${VpsWebRoot}/${ApkName}"
Write-OK "Uploaded to commercial web"

Invoke-Scp $ApkDest "${VpsServerRoot}/${ApkName}"
Write-OK "Uploaded to pasa-server"

# ---------------------------------------------------------------------------
# STEP 7 -- Update pasa-latest.apk symlinks
# ---------------------------------------------------------------------------
Write-Step "STEP 7 -- Update pasa-latest.apk symlinks"

Invoke-Ssh "ln -sf ${ApkName} ${VpsWebRoot}/pasa-latest.apk" | Out-Null
Write-OK "Commercial web symlink updated"

Invoke-Ssh "ln -sf ${ApkName} ${VpsServerRoot}/pasa-latest.apk" | Out-Null
Write-OK "Server symlink updated"

# ---------------------------------------------------------------------------
# STEP 8 -- Inject release into VPS SQLite via inject_release.js
# ---------------------------------------------------------------------------
Write-Step "STEP 8 -- Inject release into VPS database"

$injectOut = Invoke-Ssh "node ${VpsInjectScript} ${VersionCode} ${VersionName} ${sha256} ${sizeBytes}"
Write-Host ("  " + ($injectOut -join "`n  ")) -ForegroundColor White

# ---------------------------------------------------------------------------
# STEP 9 -- Restart PM2 and verify API response
# ---------------------------------------------------------------------------
Write-Step "STEP 9 -- Restart server and verify API"

Invoke-Ssh "pm2 restart pasa-server" | Out-Null
Write-OK "PM2 pasa-server restarted"

Start-Sleep -Seconds 3

$apiOut = Invoke-Ssh "curl -s http://localhost:8160/api/app/latest"
if ($apiOut -match "versionCode.*$VersionCode") {
    Write-OK "API confirmed: v${VersionName} (Build ${VersionCode})"
} else {
    Write-Warn "API response looks unexpected -- verify manually:"
    Write-Host ("  $apiOut") -ForegroundColor Yellow
}

# ---------------------------------------------------------------------------
# Done
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "  DEPLOYED: PASA v${VersionName} (Build ${VersionCode})" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  APK     : releases\$ApkName" -ForegroundColor White
Write-Host "  SHA-256 : $sha256" -ForegroundColor White
Write-Host "  Size    : $sizeMB MB" -ForegroundColor White
Write-Host "  API     : https://pasa.izhaanintellect.fun/api/app/latest" -ForegroundColor White
Write-Host ""
Write-Host "  Next: Install APK on device and test /update check" -ForegroundColor Cyan
Write-Host ""
