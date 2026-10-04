# 🛡️ PASA Sentinel — Complete Production Release & Deployment Playbook

This document is the authoritative, step-by-step operational standard operating procedure (SOP) for engineering, building, auditing, publishing, and deploying updates for **PASA Sentinel** (`com.izhaanintellect.pasa` / `rbr48/pasa`).

Every release must follow this exact playbook in sequence to ensure cryptographic continuity, seamless Over-The-Air (OTA) updates for existing installs, zero downtime, and complete alignment across documentation, security audits, and production web portals.

---

## 📑 Table of Contents

1. [Architectural Invariants & Non-Negotiable Rules](#1-architectural-invariants--non-negotiable-rules)
2. [Release Environment & Toolchain Prerequisites](#2-release-environment--toolchain-prerequisites)
3. [Phase 1: Code Hardening & Version Bumping](#3-phase-1-code-hardening--version-bumping)
4. [Phase 2: Cryptographic Production Build & Hash Calculation](#4-phase-2-cryptographic-production-build--hash-calculation)
5. [Phase 3: Local Release Packaging & Audit Script Execution](#5-phase-3-local-release-packaging--audit-script-execution)
6. [Phase 4: Full Repository Documentation & Manifest Synchronization](#6-phase-4-full-repository-documentation--manifest-synchronization)
7. [Phase 5: Git Commit, Tagging & GitHub Release Publishing](#7-phase-5-git-commit-tagging--github-release-publishing)
8. [Phase 6: Remote VPS Control Plane & OTA Database Deployment](#8-phase-6-remote-vps-control-plane--ota-database-deployment)
9. [Phase 7: Commercial Web Portal & Docker Container Deployment](#9-phase-7-commercial-web-portal--docker-container-deployment)
10. [Phase 8: End-to-End 10-Point Verification Checklist](#10-phase-8-end-to-end-10-point-verification-checklist)
11. [Emergency Rollback Runbook](#11-emergency-rollback-runbook)

---

## 1. Architectural Invariants & Non-Negotiable Rules

Before touching any code or initiating a build, verify adherence to these invariants:

1. **CRITICAL PROJECT BOUNDARY:**  
   `pasa super` is a **completely separate project** from `pasa sentinel`. **NEVER** mix, copy, or confuse code, dependencies, tokens, or assets between the two.
2. **CANONICAL SIGNING KEY INVARIANT:**  
   All production APKs **MUST** be signed with the **Original Master Release Key**:
   - Location: `e:/Projects/pasa-release-key.jks` (backed up at `C:\Users\USER\Downloads\pasa-release-key.jks`)
   - Alias: `pasa_sentinel`
   - Key Password: `PasaSentinel@2026#Secure`
   - Serial Number: `6b1df0d16e909b6d`
   - SHA-256 Fingerprint: `0C:8F:62:DD:89:34:D3:B7:3E:12:D9:65:74:2D:A2:9E:64:3B:DC:15:7B:C8:59:E5:B6:AA:74:54:40:9A:D5:7A`
   > ⚠️ **Warning:** Building with any other keystore will cause Android `INSTALL_FAILED_UPDATE_INCOMPATIBLE` signature mismatch errors for existing users and permanently break automated OTA updates!
3. **SOVEREIGN ZERO-STORAGE ARCHITECTURE:**  
   - Private `botToken` and `ownerChatId` must never leave the device.
   - Surveillance media (photos, audio recordings, video, GPS coordinates) must transmit directly to Telegram and immediately execute `file.delete()` cryptographic shredding.
   - VPS server memory storage (`multer.memoryStorage()`) must never write customer media to disk.
4. **OPT-IN DEFENSE SAFETY:**  
   High-impact autonomous hardware traps (such as kinetic snatch detection) must default to `false` (opt-in) and avoid engaging fullscreen Kiosk lock without explicit owner activation.
5. **GITHUB RELEASE MARKER:**  
   GitHub does **not** automatically mark newly created semantic tags as "Latest". The release creation command must **always** include the `--latest` flag.
6. **SSH / SCP PROTOCOL CONSTRAINT:**  
   The VPS OpenSSH server on port 2222 does not support the SFTP subsystem. All file transfers via SCP must include the legacy flag: `scp -O -P 2222 ...`.

---

## 2. Release Environment & Toolchain Prerequisites

Ensure the following tools and environment variables are active on your Windows workstation:

| Component | Path / Configuration | Verification Command |
| :--- | :--- | :--- |
| **Java JDK 17** | `C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot` | `& "C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot\bin\java.exe" -version` |
| **Android SDK** | API 36 (targetSdk 36, compileSdk 36, minSdk 26) | Inspected in `app/build.gradle.kts` |
| **Gradle Wrapper** | Gradle 8.7 / AGP 8.5.1 | `.\gradlew.bat -v` |
| **Git CLI** | Local repo on `main` branch | `git status` |
| **GitHub CLI (`gh`)** | Authenticated as `rbr48` | `gh auth status` |
| **SSH Client & Key** | Port `2222`, Key `~/.ssh/id_rsa_dbm` | `ssh -p 2222 -i ~/.ssh/id_rsa_dbm root@148.135.137.245 "uname -a"` |
| **Node.js** | Node.js v20+ for audit and build scripts | `node -v` |

---

## 3. Phase 1: Code Hardening & Version Bumping

### Step 1.1: Update Version Codes
Open [`app/build.gradle.kts`](file:///e:/Projects/PrivateApp/app/build.gradle.kts) and increment `versionCode` (integer) and `versionName` (semantic string):

```kotlin
defaultConfig {
    applicationId = "com.izhaanintellect.pasa"
    minSdk = 26
    targetSdk = 36
    versionCode = 83          // e.g. previous was 82
    versionName = "3.7.18"     // e.g. previous was 3.7.17
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}
```

### Step 1.2: Check Code Scanning & Security Rules
Ensure any new features or fixes adhere to CI security standards:
- **No sensitive data in logs:** Never print phone numbers, tokens, or command arguments to `Log.*` (prevents CWE-532).
- **Task Hijacking defense:** Ensure newly declared activities have `android:launchMode="singleTask"` and `taskAffinity=""`.
- **Arbitrary APK Install prevention:** Always resolve the Package Manager installer before launching install intents.
- **Biometric authentication:** Bind biometrics to `AES/GCM/NoPadding` cipher with `setUserAuthenticationRequired(true)`.

---

## 4. Phase 2: Cryptographic Production Build & Hash Calculation

### Step 2.1: Verify Keystore File
Ensure `e:/Projects/pasa-release-key.jks` is present and matches the canonical fingerprint:
```powershell
keytool -list -v -keystore "e:/Projects/pasa-release-key.jks" -storepass "PasaSentinel@2026#Secure"
```
Verify the output contains:
`Serial number: 6b1df0d16e909b6d`  
`SHA256: 0C:8F:62:DD:89:34:D3:B7:3E:12:D9:65:74:2D:A2:9E:64:3B:DC:15:7B:C8:59:E5:B6:AA:74:54:40:9A:D5:7A`

### Step 2.2: Compile Signed Release APK
Run the clean release assemble command using JDK 17:
```powershell
cmd.exe /c "set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot&& gradlew.bat clean assembleRelease"
```
The compiled signed APK will be output to:
`app/build/outputs/apk/release/app-release.apk`

### Step 2.3: Calculate Cryptographic Hashes
Run PowerShell commands to extract exact metrics:
```powershell
$apk = "app/build/outputs/apk/release/app-release.apk"
$sha256 = (Get-FileHash $apk -Algorithm SHA256).Hash.ToLower()
$sha1   = (Get-FileHash $apk -Algorithm SHA1).Hash.ToLower()
$md5    = (Get-FileHash $apk -Algorithm MD5).Hash.ToLower()
$bytes  = (Get-Item $apk).Length
$mb     = [math]::Round($bytes / 1MB, 2)

Write-Host "Version:    v<NEW_VERSION> (Build <NEW_CODE>)"
Write-Host "File Size:  $bytes bytes ($mb MB)"
Write-Host "SHA-256:    $sha256"
Write-Host "SHA-1:      $sha1"
Write-Host "MD5:        $md5"
```
*Record these values carefully. They are required across all documentation and manifest files.*

---

## 5. Phase 3: Local Release Packaging & Audit Script Execution

### Step 3.1: Copy APKs to Local Releases Folder
```powershell
# Create versioned APK and rolling latest symlink/copy
Copy-Item "app/build/outputs/apk/release/app-release.apk" "releases/pasa-v<NEW_VERSION>-<NEW_CODE>.apk" -Force
Copy-Item "app/build/outputs/apk/release/app-release.apk" "releases/pasa-latest.apk" -Force
```

### Step 3.2: Update `releases/app_releases.json`
Prepend the new release entry to the array in [`releases/app_releases.json`](file:///e:/Projects/PrivateApp/releases/app_releases.json):
```json
[
  {
    "versionCode": <NEW_CODE>,
    "versionName": "<NEW_VERSION>",
    "releaseDate": "YYYY-MM-DD",
    "fileName": "pasa-v<NEW_VERSION>-<NEW_CODE>.apk",
    "downloadUrl": "https://pasa.izhaanintellect.fun/releases/pasa-v<NEW_VERSION>-<NEW_CODE>.apk",
    "sha256": "<NEW_SHA256>",
    "size": <NEW_BYTE_SIZE>,
    "mandatory": false,
    "releaseNotes": "v<NEW_VERSION>: <Clear description of new features and fixes>."
  },
  ...
]
```

### Step 3.3: Update `releases/README.md`
Update table row 1, file size, SHA-256 block, and VirusTotal consensus report link in [`releases/README.md`](file:///e:/Projects/PrivateApp/releases/README.md).

### Step 3.4: Run Automated Independent Audit Script
Run the automated verification script:
```powershell
node scripts/verify_independent_audit.js
```
This regenerates [`INDEPENDENT_AUDIT_REPORT.json`](file:///e:/Projects/PrivateApp/INDEPENDENT_AUDIT_REPORT.json) with exact byte counts, hashes, and VirusTotal URLs.

---

## 6. Phase 4: Full Repository Documentation & Manifest Synchronization

Every release requires updating the following 9 files in the Git repository:

| # | File | Sections / Items to Update |
| :---: | :--- | :--- |
| 1 | [**`README.md`**](file:///e:/Projects/PrivateApp/README.md) | Badge line 8 (`v<NEW_VERSION> (Build <NEW_CODE>)`), Badge line 10 (VirusTotal URL), Release Verification section lines ~367–387 (binary name, version, build code, byte size, SHA-256 hash, VirusTotal link, SARIF zip name). |
| 2 | [**`SECURITY.md`**](file:///e:/Projects/PrivateApp/SECURITY.md) | Section 1 supported versions table line 15 (`v3.7.x (Latest: v<NEW_VERSION> / Build <NEW_CODE>)`), Section 6 VirusTotal URL line 86. |
| 3 | [**`LICENSE.md`**](file:///e:/Projects/PrivateApp/LICENSE.md) | Section 4 line 28 binary reference (`pasa-v<NEW_VERSION>-<NEW_CODE>.apk`). |
| 4 | [**`USER_MANUAL_EN.md`**](file:///e:/Projects/PrivateApp/USER_MANUAL_EN.md) | Header line 4 (`Production Version: v<NEW_VERSION> (Build <NEW_CODE>)`). |
| 5 | [**`USER_MANUAL_BN.md`**](file:///e:/Projects/PrivateApp/USER_MANUAL_BN.md) | Header line 4 (`প্রোডাকশন সংস্করণ: v<NEW_VERSION> (বিল্ড <NEW_BENGALI_NUM>)`). |
| 6 | [**`FAQ.md`**](file:///e:/Projects/PrivateApp/FAQ.md) | Header line 3 (`সংস্করণ: v<NEW_VERSION> (Build <NEW_CODE>)`). |
| 7 | [**`FAQ_EN.md`**](file:///e:/Projects/PrivateApp/FAQ_EN.md) | Header line 3 (`Version: v<NEW_VERSION> (Build <NEW_CODE>)`). |
| 8 | [**`AUDIT_TRAIL.md`**](file:///e:/Projects/PrivateApp/AUDIT_TRAIL.md) | Header lines 3–4 (Document ID, date, release target), Section 1 line 14 target app, Section 8.1 binary, byte size, SHA-256, OTA response JSON, Section 12.1 target file, byte size, SHA-256, VirusTotal URL, Section 12.5 SARIF zip name, Section 13 conclusion. |
| 9 | [**`AGENTS.md`**](file:///e:/Projects/PrivateApp/AGENTS.md) | Section 2 active release, build code, binary names, SHA-256, and byte size. |

### Step 4.1: Audit Verification Check
Run `git grep` to confirm no stale version references remain:
```powershell
git grep -n "<OLD_VERSION>"
```
*(The only acceptable match is historical release records in `releases/app_releases.json`.)*

---

## 7. Phase 5: Git Commit, Tagging & GitHub Release Publishing

### Step 5.1: Stage and Commit Changes
Carefully stage only the intended code, release, and documentation files:
```powershell
git add app/build.gradle.kts
git add releases/pasa-v<NEW_VERSION>-<NEW_CODE>.apk releases/pasa-latest.apk releases/app_releases.json releases/README.md
git add README.md SECURITY.md LICENSE.md USER_MANUAL_EN.md USER_MANUAL_BN.md FAQ.md FAQ_EN.md AUDIT_TRAIL.md INDEPENDENT_AUDIT_REPORT.json AGENTS.md

git commit -m "release: v<NEW_VERSION> (Build <NEW_CODE>) - <Brief Highlights>"
```

### Step 5.2: Create Annotated Git Tag & Push
```powershell
git tag -a "v<NEW_VERSION>" -m "Release v<NEW_VERSION> (Build <NEW_CODE>)"
git push origin main --tags
```

### Step 5.3: Publish GitHub Release via CLI
Publish the GitHub release with binary attachments and explicitly mark it as `--latest`:
```powershell
gh release create "v<NEW_VERSION>" `
  "releases/pasa-v<NEW_VERSION>-<NEW_CODE>.apk" `
  "releases/pasa-latest.apk" `
  --title "PASA Sentinel v<NEW_VERSION> (Build <NEW_CODE>)" `
  --notes "### 🚀 What's New in v<NEW_VERSION>`n* <Feature 1>`n* <Feature 2>`n`n### 🔐 Cryptographic Checksum`n* **SHA-256:** \`<NEW_SHA256>\``n* **File Size:** <NEW_BYTE_SIZE> bytes (<NEW_MB> MB)" `
  --latest
```

---

## 8. Phase 6: Remote VPS Control Plane & OTA Database Deployment

Deploy to the production VPS (`148.135.137.245`, port `2222`):

### Step 8.1: Upload APK Binaries via SCP (Legacy `-O` Flag)
```powershell
scp -O -P 2222 -i ~/.ssh/id_rsa_dbm "releases/pasa-v<NEW_VERSION>-<NEW_CODE>.apk" root@148.135.137.245:/var/www/pasa-server/releases/
scp -O -P 2222 -i ~/.ssh/id_rsa_dbm "releases/pasa-v<NEW_VERSION>-<NEW_CODE>.apk" root@148.135.137.245:/opt/pasa-commercial-web/public/releases/
scp -O -P 2222 -i ~/.ssh/id_rsa_dbm "releases/pasa-v<NEW_VERSION>-<NEW_CODE>.apk" root@148.135.137.245:/var/www/pasa-commercial-web/public/releases/
```

### Step 8.2: Update Server SQLite Database (`pasa.db`) & Releases JSON
Run a base64 Python script via SSH to update SQLite WAL database and filesystem symlinks:
```powershell
$pyScript = @'
import sqlite3
import os
import shutil

db_path = "/var/www/pasa-server/data/pasa.db"
conn = sqlite3.connect(db_path)
cur = conn.cursor()

# Ensure table exists
cur.execute("""
CREATE TABLE IF NOT EXISTS app_releases (
    version_code INTEGER PRIMARY KEY,
    version_name TEXT NOT NULL,
    file_name TEXT NOT NULL,
    download_url TEXT NOT NULL,
    file_size INTEGER NOT NULL,
    sha256 TEXT NOT NULL,
    changelog TEXT,
    is_mandatory INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
)
""")

# Insert or replace new release
cur.execute("""
INSERT OR REPLACE INTO app_releases (version_code, version_name, file_name, download_url, file_size, sha256, changelog, is_mandatory)
VALUES (?, ?, ?, ?, ?, ?, ?, 0)
""", (
    <NEW_CODE>,
    "<NEW_VERSION>",
    "pasa-v<NEW_VERSION>-<NEW_CODE>.apk",
    "https://pasa.izhaanintellect.fun/releases/pasa-v<NEW_VERSION>-<NEW_CODE>.apk",
    <NEW_BYTE_SIZE>,
    "<NEW_SHA256>",
    "v<NEW_VERSION>: <Changelog description>"
))
conn.commit()
conn.close()
print("SQLite database updated successfully.")

# Update symlinks across all directories
for d in ["/var/www/pasa-server/releases", "/opt/pasa-commercial-web/public/releases", "/var/www/pasa-commercial-web/public/releases"]:
    link = os.path.join(d, "pasa-latest.apk")
    target = "pasa-v<NEW_VERSION>-<NEW_CODE>.apk"
    if os.path.islink(link) or os.path.exists(link):
        os.remove(link)
    os.symlink(target, link)
    print(f"Symlink updated in {d} -> {target}")
'@

$b64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($pyScript))
ssh -p 2222 -i ~/.ssh/id_rsa_dbm root@148.135.137.245 "echo $b64 | base64 -d | python3"
```

### Step 8.3: Restart Server PM2 Process & Verify Live OTA API
```powershell
ssh -p 2222 -i ~/.ssh/id_rsa_dbm root@148.135.137.245 "pm2 restart 27"
```
Verify the live OTA endpoint immediately:
```powershell
curl.exe -s "https://pasa.izhaanintellect.fun/api/app/latest?current_version_code=0"
```
Confirm the JSON response returns `"versionCode": <NEW_CODE>`, `"versionName": "<NEW_VERSION>"`, and the exact matching SHA-256 hash.

---

## 9. Phase 7: Commercial Web Portal & Docker Container Deployment

### Step 9.1: Sync Documentation & Audit Manifests to VPS
```powershell
scp -O -P 2222 -i ~/.ssh/id_rsa_dbm AUDIT_TRAIL.md FAQ.md FAQ_EN.md INDEPENDENT_AUDIT_REPORT.json LICENSE.md README.md SECURITY.md USER_MANUAL_BN.md USER_MANUAL_EN.md releases/app_releases.json root@148.135.137.245:/tmp/pasa_web_update/
```

### Step 9.2: Execute Web Assets Hot-Update Script
Execute the update Python script on the VPS to patch HTML pages, sync folders, copy into Docker, and reload Nginx:
```powershell
$pyWeb = @'
import os
import shutil
import subprocess

opt_public = "/opt/pasa-commercial-web/public"
www_public = "/var/www/pasa-commercial-web/public"
tmp_dir = "/tmp/pasa_web_update"

# 1. Sync markdown & json
for f in ["AUDIT_TRAIL.md", "FAQ.md", "FAQ_EN.md", "INDEPENDENT_AUDIT_REPORT.json", "LICENSE.md", "README.md", "SECURITY.md", "USER_MANUAL_BN.md", "USER_MANUAL_EN.md"]:
    src = os.path.join(tmp_dir, f)
    if os.path.exists(src):
        shutil.copy2(src, os.path.join(opt_public, f))
        shutil.copy2(src, os.path.join(www_public, f))

# 2. Sync app_releases.json
rel_json = os.path.join(tmp_dir, "app_releases.json")
if os.path.exists(rel_json):
    shutil.copy2(rel_json, os.path.join(opt_public, "releases", "app_releases.json"))
    shutil.copy2(rel_json, os.path.join(www_public, "releases", "app_releases.json"))

# 3. Update HTML files
def patch(filename, old_str, new_str):
    p = os.path.join(opt_public, filename)
    if os.path.exists(p):
        with open(p, "r", encoding="utf-8") as f:
            c = f.read()
        c = c.replace(old_str, new_str)
        with open(p, "w", encoding="utf-8") as f:
            f.write(c)

patch("index.html", "v<OLD_VERSION>", "v<NEW_VERSION>")
patch("index.html", "Build <OLD_CODE>", "Build <NEW_CODE>")
patch("index.html", "pasa-v<OLD_VERSION>-<OLD_CODE>.apk", "pasa-v<NEW_VERSION>-<NEW_CODE>.apk")

patch("docs.html", "v<OLD_VERSION>", "v<NEW_VERSION>")
patch("docs.html", "Build <OLD_CODE>", "Build <NEW_CODE>")
patch("docs.html", "pasa-v<OLD_VERSION>-<OLD_CODE>.apk", "pasa-v<NEW_VERSION>-<NEW_CODE>.apk")
patch("docs.html", "<OLD_BYTE_SIZE> bytes", "<NEW_BYTE_SIZE> bytes")
patch("docs.html", "<OLD_SHA256>", "<NEW_SHA256>")

patch("manual.html", "v<OLD_VERSION>", "v<NEW_VERSION>")
patch("manual.html", "Build <OLD_CODE>", "Build <NEW_CODE>")
patch("terms.html", "<OLD_VERSION>", "<NEW_VERSION>")
patch("privacy.html", "v<OLD_VERSION>+", "v<NEW_VERSION>+")
patch("promo.html", "v<OLD_VERSION>", "v<NEW_VERSION>")
patch("llms.txt", "v<OLD_VERSION>", "v<NEW_VERSION>")

# 4. Sync opt to var/www
subprocess.run(["cp", "-r", opt_public + "/.", www_public + "/"], check=True)

# 5. Hot copy into Docker container & reload
subprocess.run(["docker", "cp", opt_public + "/.", "pasa-commercial-app:/usr/share/nginx/html/"], check=True)
subprocess.run(["docker", "exec", "pasa-commercial-app", "nginx", "-s", "reload"], check=True)
print("Web assets deployed and Nginx reloaded!")
'@

$b64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($pyWeb))
ssh -p 2222 -i ~/.ssh/id_rsa_dbm root@148.135.137.245 "echo $b64 | base64 -d | python3"
```

---

## 10. Phase 8: End-to-End 10-Point Verification Checklist

Execute these 10 commands sequentially to certify the production deployment:

```powershell
# 1. Verify live OTA endpoint version and hash
curl.exe -s "https://pasa.izhaanintellect.fun/api/app/latest?current_version_code=0"

# 2. Verify rolling latest APK download header and exact Content-Length
curl.exe -sI "https://pasa.izhaanintellect.fun/releases/pasa-latest.apk" | Select-String "Content-Length"

# 3. Verify versioned APK download header
curl.exe -sI "https://pasa.izhaanintellect.fun/releases/pasa-v<NEW_VERSION>-<NEW_CODE>.apk" | Select-String "HTTP/1.1 200 OK"

# 4. Verify landing page HTML badges
curl.exe -s "https://pasa.izhaanintellect.fun/" | Select-String -Pattern "v<NEW_VERSION>"

# 5. Verify documentation page badges and file name
curl.exe -s "https://pasa.izhaanintellect.fun/docs" | Select-String -Pattern "pasa-v<NEW_VERSION>-<NEW_CODE>.apk"

# 6. Verify manual page header
curl.exe -s "https://pasa.izhaanintellect.fun/manual" | Select-String -Pattern "v<NEW_VERSION>"

# 7. Verify live independent audit report JSON
curl.exe -s "https://pasa.izhaanintellect.fun/INDEPENDENT_AUDIT_REPORT.json"

# 8. Verify audit trail document header
curl.exe -s "https://pasa.izhaanintellect.fun/AUDIT_TRAIL.md" | Select-Object -First 6

# 9. Verify GitHub release status and tag
gh release view "v<NEW_VERSION>"

# 10. Verify GitHub Actions CI/CD security audit runs
gh run list --limit 3
```

**Sign-Off Criteria:**
- All 10 commands exit `0` with matching hashes.
- GitHub Actions CI/CD security audit jobs are 100% green.
- 0 open code scanning alerts on GitHub.

---

## 11. Emergency Rollback Runbook

If a critical bug or regression is discovered immediately post-launch:

1. **Revert Rolling Latest Symlink on VPS:**
   ```bash
   ssh -p 2222 -i ~/.ssh/id_rsa_dbm root@148.135.137.245 "
     ln -sf pasa-v<PREVIOUS_VERSION>-<PREVIOUS_CODE>.apk /var/www/pasa-server/releases/pasa-latest.apk
     ln -sf pasa-v<PREVIOUS_VERSION>-<PREVIOUS_CODE>.apk /opt/pasa-commercial-web/public/releases/pasa-latest.apk
     docker cp /opt/pasa-commercial-web/public/releases/. pasa-commercial-app:/usr/share/nginx/html/releases/
   "
   ```
2. **Revert OTA Database Record:**
   Update `pasa.db` to point `latest` back to the previous stable release code.
3. **Notify Users via Telegram:**
   Broadcast a notice to the official community channel (`https://t.me/pasa_sentinel_official`) advising users to hold on updating while the hotfix is compiled.
4. **Prepare Hotfix:**
   Follow Phases 1–8 with an incremented build code (e.g. `v3.7.19` Build `84`). Never overwrite an existing version code or tag!
