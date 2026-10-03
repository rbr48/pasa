# 🛡️ PASA Sentinel (Private Android Security Agent)

> **Next-Generation Sovereign Mobile Defense & Covert Anti-Theft Agent for Android (8.0 – 16).**  
> *Zero Google Play dependencies. Air-gapped cellular SMS fallback. Knox-grade Device Owner permanence. Dual-channel Telegram C2. Direct-to-Telegram Zero-Storage Architecture.*

[![Android](https://img.shields.io/badge/Android-8.0%20to%2016%20(API%2036)-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)](https://kotlinlang.org)
[![Version](https://img.shields.io/badge/Release-v3.7.16%20(Build%2081)-blue.svg)](https://pasa.izhaanintellect.fun/releases/pasa-latest.apk)
[![License](https://img.shields.io/badge/License-BSL%201.1-orange.svg)](LICENSE.md)
[![VirusTotal](https://img.shields.io/badge/VirusTotal-70%2B%20AV%20Clean-brightgreen.svg)](https://www.virustotal.com/gui/file/6ba3c5df252591f7c090359d27c99f27defe63e6f64bef72256e544ff9dff55e)
[![CodeQL](https://img.shields.io/badge/CodeQL-Passed-brightgreen.svg)](https://github.com/rbr48/pasa/security/code-scanning)
[![Zero Storage](https://img.shields.io/badge/Zero%20Storage-Direct--to--Telegram-brightgreen.svg)](PRIVACY.md)
[![Zero Telemetry](https://img.shields.io/badge/Privacy-Zero%20Cloud%20Telemetry-brightgreen.svg)](PRIVACY.md)
[![Ed25519 Security](https://img.shields.io/badge/Cryptography-Ed25519%20Offline%20Certificates-blueviolet.svg)](#commercial-licensing--payment-methods)
[![Responsible Use](https://img.shields.io/badge/Ethical%20Charter-Anti--Stalkerware-red.svg)](RESPONSIBLE_USE.md)
[![Security Policy](https://img.shields.io/badge/Security-RFC%209116-blue.svg)](SECURITY.md)
[![Terms of Service](https://img.shields.io/badge/Legal-Terms%20of%20Service%20%26%20EULA-blueviolet.svg)](TERMS.md)

---

## 📑 Table of Contents
1. [The Problem: Why Google "Find My Device" Fails](#the-problem-why-google-find-my-device-fails)
2. [Zero-Storage Architecture (Strategy 1 Sovereign Privacy)](#-zero-storage-architecture-strategy-1-sovereign-privacy)
3. [Core Defense Capabilities](#-core-defense-capabilities)
4. [Complete Telegram Bot Setup Guide](#-complete-telegram-bot-setup-guide)
5. [Enterprise Device Owner & Persistence Setup](#-enterprise-device-owner--persistence-setup)
6. [Executive Telegram C2 Command Glossary (86 Commands & 6-Hub Console)](#-complete-telegram-c2-command-glossary-86-commands)
7. [Hardware Escrow Token & Physical Lockscreen Reset](#-hardware-escrow-token--physical-lockscreen-reset)
8. [Lockscreen Duress PIN & Anti-Coercion Mode](#-lockscreen-duress-pin--anti-coercion-mode)
9. [Offline Air-Gapped SMS Defense](#-offline-air-gapped-sms-defense)
10. [Commercial Licensing & Payment Methods](#-commercial-licensing--payment-methods)
    - [7-Day Money-Back Guarantee](#-7-day-no-questions-asked-money-back-guarantee)
11. [Source-Available Architecture & Responsible Use](#-source-available-architecture--responsible-use)
12. [Security & Vulnerability Disclosure](#-security--vulnerability-disclosure)
13. [Building from Source](#-building-from-source)
14. [Release Verification & Integrity](#-release-verification--integrity)
15. [Zero-Telemetry Privacy Policy](PRIVACY.md)
16. [Terms of Service & EULA (Mandatory)](TERMS.md)
17. [Frequently Asked Questions (FAQ) — English](FAQ_EN.md) / [বাংলা প্রশ্নোত্তর](FAQ.md)

---

## ⚠️ The Problem: Why Google "Find My Device" Fails

When a smartphone is stolen, street thieves act within the first **5 to 10 seconds**:

1. **They cut the Internet immediately:** Swiping down Quick Settings to enable **Airplane Mode** or ripping out the SIM card. The second Wi-Fi and mobile data drop, Google "Find My Device", Samsung SmartThings, and standard tracker apps go completely blind.
2. **They power down the phone:** Holding the power button shuts down standard Android devices completely.
3. **They boot into Safe Mode or Factory Reset:** In Safe Mode, all third-party apps are disabled, allowing the thief to wipe or uninstall standard security apps.
4. **Subscription creep & Privacy violations:** Most commercial trackers (Life360, Prey, Cerberus) charge **$50–$100/year** on recurrent subscriptions while continuously hoarding user location, contacts, and personal photos on corporate servers.

### 🛡️ How PASA Sentinel Solves This:
* **Direct-to-Telegram Zero-Storage Architecture:** Zero photos, zero GPS tracks, and zero audio recordings are stored on any VPS disk or cloud database. Everything is dispatched directly and exclusively to your private Telegram Bot and immediately memory-shredded on the phone.
* **Air-Gapped SMS Fallback:** When internet is dead, GSM cellular signal remains connected. A single encrypted SMS command powers up the hardware GPS receiver chip and texts back exact Google Maps coordinates directly from satellites.
* **Fake Shutdown Deception:** When the thief tries to turn off the phone, PASA presents a spoofed power-down animation and enters an AMOLED 0-nit black screen mode. The thief thinks the phone is dead, while PASA covertly records ambient audio, captures intruder mugshots, and streams background GPS beacons.
* **Autonomous Anti-Snatch Kinetic Lock:** If snatched from your hands (acceleration delta > 2.65G), PASA instantly locks the screen via Device Owner, captures a silent front-camera mugshot of the thief, and blasts a maximum-volume alarm.
* **Knox-Grade Device Owner:** The Android "Uninstall" button is permanently grayed out. Notification pull-down, Safe Mode, and USB debugging can be cryptographically disabled.
* **Hardware Escrow Token (Android 14–16):** Remotely reset or overwrite the physical lockscreen PIN anytime via `/set_os_pin <new_pin>`.
* **Zero Big-Tech Telemetry:** All communication is routed strictly between your device and your private Telegram Bot.

---

## 🔒 Zero-Storage Architecture (Strategy 1 Sovereign Privacy)

Unlike commercial spy apps that store user media on central servers, **PASA Sentinel operates on a strict Zero-Storage Architecture**:

```
[ Android Device (PASA Agent) ]
        │
        ├── 1. Covert Mugshot / Surveillance Video Captured
        ├── 2. Direct Encrypted Multipart POST via Telegram Bot API
        │       (https://api.telegram.org/bot<TOKEN>/sendPhoto)
        │
        ├── 3. Instant Local Cryptographic Shredding (photoFile.delete())
        ▼
[ Private Telegram Chat (Owner Only) ]  <─── Only YOU receive and hold the media!
```

* **No Evidence on Server Disk:** Surveillance photos, audio wiretaps, video clips, and live streams are transmitted directly to your private Telegram chat.
* **Transient RAM Buffers Only:** Any C2 command telemetry relay uses ephemeral in-memory buffers (Multer `memoryStorage`), never touching a physical hard drive.
* **Zero Location Logs:** The VPS database stores no GPS breadcrumbs or location history. GPS coordinates are sent straight to your Telegram message stream.

---

## 🏛️ Core Defense Capabilities

```
                                +-----------------------------------+
                                |     Owner C2 Terminal (Telegram)  |
                                +-----------------+-----------------+
                                                  |
                         Dual-Channel C2          | Direct Telegram HTTPS / Webhook
                         Failover Protocol        v
+------------------+          Encrypted   +-------+-------+          Encrypted   +--------------------+
|  GSM Radio Tower |<====== Cellular PDU =| PASA Sentinel |<====== HTTPS Payload =| PASA Control Plane |
+--------+---------+         (Offline)    | Android Agent |          (Ephemeral) |  (VPS / Node.js)   |
         |                                +-------+-------+                      +--------------------+
         | Emergency SMS                          |
         v                                        +---> Covert Camera2 (No Viewfinder / Screen Off)
+--------+---------+                              +---> AudioRecord Buffer (16kHz PCM / AAC)
|  Emergency Phone |                              +---> Hardware GNSS Direct Satellite Lock
+------------------+                              +---> G-Force Sensor (Anti-Snatch Delta Detector)
                                                  +---> AMOLED 0-Nit Fake Shutdown Canvas
                                                  +---> Hardware Escrow Token Keyguard Arming
```

---

## 🤖 Complete Telegram Bot Setup Guide

PASA Sentinel uses Telegram as its sovereign Command and Control (C2) console. You connect using your own **private dedicated bot** created via @BotFather for zero-trust, end-to-end security.

---

### Private Dedicated Bot via @BotFather (Zero-Trust Setup)
1. In Telegram, search for **[@BotFather](https://t.me/BotFather)** and send `/newbot`.
2. Name your bot (e.g. `MyPhoneSentinelBot`) and choose a username ending in `bot`.
3. Copy the HTTP API token provided by BotFather.
4. Search for **[@userinfobot](https://t.me/userinfobot)** in Telegram, click Start, and copy your numeric **User ID** (e.g. `123456789`).
5. Open the PASA Android app, enter your Bot Token and Owner Chat ID, and tap **"Initialize Sovereign Sentinel"**.
6. Send `/menu` to your new bot to verify immediate C2 communication!

---

## 🔒 Enterprise Device Owner & Persistence Setup

To make PASA Sentinel **impossible to uninstall** and grant hardware-level administrative control, run this one-time ADB command:

```bash
# 1. Connect phone via USB with USB Debugging enabled
adb devices

# 2. Grant Enterprise Device Owner privilege to PASA:
adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin
```

### What Device Owner unlocks:
* 🛡️ **Permanent Protection:** The "Uninstall" button in Android Settings is permanently grayed out.
* 🔐 **Remote Hardware OS Lockscreen PIN Reset:** Overwrite the physical device lock screen PIN/password via `/set_os_pin <new_pin>`.
* 🔒 **Knox Kiosk Mode (`LOCK_TASK_FEATURE_NONE`):** Physically disables the Home button, Recents button, Power menu, and notification pull-down on lock.
* 📱 **Status Bar & Quick Settings Lockdown:** Completely disables pulling down the notification shade while locked, preventing thieves from toggling Airplane Mode or Wi-Fi.
* 🛡️ **System-Wide Encrypted DNS:** Force DNS-over-TLS via `/dns [quad9|cloudflare|adguard|host]` (Android 10+).
* 🔌 **Hardware USB Data Pin Killswitch:** Physically sever USB data communication pins via `/usb_lock on` (Android 12+).
* 👑 **Self-Healing Permissions:** Permanently lock Camera, Mic, SMS, and GPS permissions as unrevokable via `/self_heal`.
* 🧊 **Shadow App Vault:** Vanish banking, crypto, and chat apps completely via `/freeze <pkg>` and `/unfreeze <pkg>`.
* 🧬 **Duress Biometrics:** Disable fingerprint and face unlock to force Master PIN during checkpoints via `/biometrics on`.
* 🔄 **Remote Hardware Reboot:** Trigger a clean hardware reboot via `/reboot`.
* 🚫 **Safe Mode Lockout:** Prevents booting into Safe Mode to bypass security services.
* ✈️ **Network Protection:** Blocks unauthorized toggling of Airplane Mode, USB debugging, or file transfers while locked.
* 👁️ **100% Invisible Stealth:** Zero persistent notification icons via `IMPORTANCE_MIN` channel and hidden launcher icon.

> **Crucial Android 14–16 Setting:**  
> Go to **App Info → PASA Sentinel → Battery** and set to **"Unrestricted"**. This prevents Android's aggressive background sleep killers from suspending the telemetry daemon.

---

## 🕹️ Complete Telegram C2 Command Glossary (86 Commands & 6-Hub Console)

Send these commands directly to your Telegram bot (or use the interactive menu autocomplete):

| Category | Command | Syntax | Action & Behavior |
|---|---|---|---|
| **Core & Diagnostics** | `/menu` | `/menu` | 📱 Opens interactive touchscreen dashboard & quick controls |
| | `/help` | `/help` | 📖 Complete documentation manual & command guide |
| | `/status` | `/status` | 📊 Real-time battery, storage, RAM & sensor diagnostics |
| | `/selftest` | `/selftest` | 🩺 Comprehensive 9-point security & sensor diagnostic audit |
| | `/info` | `/info` | ℹ️ Hardware specs, SIM card details, and OS patch level |
| | `/reboot` | `/reboot` | 🔄 Hardware reboot initiated remotely (Device Owner) |
| **Enterprise Device Owner** | `/device_owner` | `/device_owner` | 👑 Checks Device Owner & Kiosk hardware lock status |
| | `/antitamper` | `/antitamper on\|off\|status` | 🛡️ Safe boot, airplane mode, and factory reset lockout |
| | `/usb_lock` | `/usb_lock on\|off\|status` | 🔌 Kills USB data pins (AC charge only) [Android 12+] |
| | `/self_heal` | `/self_heal` | 👑 Permanently locks & grants all runtime permissions |
| | `/freeze` | `/freeze <pkg>` | 🧊 Conceals banking/crypto/messaging app into shadow vault |
| | `/unfreeze` | `/unfreeze <pkg>` | ☀️ Restores hidden application from shadow vault |
| | `/frozen` | `/frozen` | 📦 Lists all hidden/quarantined applications |
| | `/biometrics` | `/biometrics on\|off` | 🧬 Duress biometric killswitch (forces Master PIN) |
| | `/dns` | `/dns [quad9\|cloudflare\|adguard\|off\|status]` | 🛡️ Enforces system-wide encrypted DNS-over-TLS |
| **Location & Geofencing** | `/locate` | `/locate` or `/gps` | 📍 Acquires high-accuracy GNSS fix and sends Google Maps pin |
| | `/tower` | `/tower` | 📡 Dual-SIM cell tower triangulation & signal RF telemetry |
| | `/track` | `/track [minutes]` | 🛰️ Starts continuous periodic GPS tracking |
| | `/track_stop` | `/track_stop` | 🛑 Stops ongoing continuous GPS tracking |
| | `/geofence` | `/geofence here 200` | 🌐 Sets safe zone radius & breach/return alerts |
| **Covert Forensics** | `/snap` | `/snap front\|back\|both` | 📸 Silent covert photo with zero preview or shutter sound |
| | `/screenshot` | `/screenshot` | 📱 Silent full-screen capture via native Accessibility |
| | `/screen_burst` | `/screen_burst [5-10]` | 🎞️ Rapid 5–10 frame storyboard montage of intruder activity |
| | `/screenrecord` | `/screenrecord [seconds]` | 🎥 Covert HD MP4 screen recording (5–60s) via Device Owner |
| | `/video` | `/video front\|back [sec]` | 📹 Silent camera video clip (1–60s) without screen wake |
| | `/record` | `/record [seconds]` | 🎙️ Silent ambient microphone wiretap (default 30s) |
| | `/livestream` | `/livestream` | 🔴 Starts encrypted real-time covert camera/audio stream |
| | `/stopstream` | `/stopstream` | ⏹️ Stops active live video/audio stream |
| | `/clipboard` | `/clipboard` | 📋 Reads current device clipboard text |
| **Lockdown & Emergency** | `/lock` | `/lock [pin] [msg]` | 🔒 Locks device with emergency PIN & Lost Mode banner |
| | `/lock_message` | `/lock_message <text>` | 💬 Updates lockscreen banner message |
| | `/set_os_pin` | `/set_os_pin <pin>` | 🔐 Overwrites physical Android OS lockscreen PIN (Device Owner) |
| | `/set_master_pin` | `/set_master_pin <pin>` | 🔑 Remotely updates emergency Master PIN/Password |
| | `/unlock` | `/unlock` | 🔓 Dismisses Lost Mode & restores normal device UI |
| | `/fakeshutdown` | `/fakeshutdown` | 🕶️ Fake shutdown: blackout screen & silent touch traps |
| | `/wake` | `/wake` | ☀️ Restores device from Fake Shutdown blackout |
| | `/ring` | `/ring [seconds]` | 🚨 Blasts maximum volume 100% siren, overriding silent/DND |
| | `/ring_stop` | `/ring_stop` | 🔇 Silences active emergency siren immediately |
| | `/message` | `/message <text>` | 📢 Displays urgent fullscreen alert banner on display |
| **Defense & Deception** | `/duress_pin` | `/duress_pin <pin>` | 🆘 Sets decoy coercion PIN: unlocks device while sending SOS |
| | `/trap` | `/trap on\|off\|snatch\|charger\|pocket` | 🛡️ Arms autonomous sensor traps (snatch, charger, pocket) |
| | `/shred` | `/shred <path>` | 🗑️ Cryptographically sanitizes sensitive files with zero-fill |
| | `/stealth` | `/stealth` (or `/hide`) | 👁️ Hides/reveals PASA app icon in launcher |
| **Extraction & Logs** | `/contacts` | `/contacts [search]` | 👥 Searches or reads device address book contacts |
| | `/call_log` | `/call_log [count]` | 📞 Views incoming and outgoing call history |
| | `/sms_log` | `/sms_log [count]` | 💬 Views recent SMS inbox messages |
| | `/history` | `/history [count]` | 📜 Views recent command execution audit trail |
| | `/network` | `/network` | 🌐 Current IP, Wi-Fi SSID, and cellular carrier signal |
| **System & Maintenance** | `/apps` | `/apps` | 📦 Lists installed third-party applications |
| | `/app_uninstall` | `/app_uninstall <pkg>` | ❌ Silently uninstalls package (Device Owner) |
| | `/smssetup` | `/smssetup` | 📲 Enrolls TOTP secret for offline air-gapped SMS commands |
| | `/license` | `/license` | 🔑 Checks Pro license status or activates purchased key |
| | `/check_update` | `/check_update` | 🔄 Checks for OTA application updates |
| | `/update_confirm` | `/update_confirm` | ⚡ Downloads and installs pending OTA update directly |
| | `/wipe` | `/wipe` | ⚠️ Initiates remote emergency factory reset (requires auth) |
| | `/wipe_confirm` | `/wipe_confirm <pass>` | 💥 Confirms remote factory reset with master password |

---

## 🔐 Hardware Escrow Token & Physical Lockscreen Reset

On Android 14, 15, and 16, Google deprecated legacy password reset APIs in favor of **Cryptographic Hardware Escrow Tokens**:

1. **Enrolling the Token:** PASA automatically enrolls an AES-256 escrow token with the Android Keystore.
2. **One-Time Keyguard Arming:**
   * After installing PASA, lock your phone with the power button.
   * Unlock it once using your physical PIN, password, or biometric.
   * Android Keyguard automatically arms the escrow token in hardware.
3. **Remote Password Reset:**
   * Send `/set_os_pin <new_pin>` from Telegram.
   * PASA calls `resetPasswordWithToken()` to overwrite your physical lockscreen credential instantly without wiping device data!

---

## 🆘 Lockscreen Duress PIN & Anti-Coercion Mode

If an attacker coerces you to unlock your phone under threat:

1. **Set your Duress PIN:** In Telegram, send:
   ```text
   /duress_pin 9999
   ```
2. **Under Coercion:** Enter `9999` on your lockscreen instead of your real PIN.
3. **What happens automatically:**
   * The device unlocks smoothly so the attacker believes you complied.
   * **Sterile Sandbox Decoy OS:** All banking, crypto, and private messenger apps (Binance, Signal, Telegram, WhatsApp, bKash, etc.) are vanished instantly from the launcher, app drawer, and process list via Device Owner privileges, leaving only harmless stock apps visible.
   * PASA immediately captures silent front-camera mugshots of the attacker.
   * A high-priority **🚨 EMERGENCY SOS COERCION ALERT** with live satellite coordinates and intruder photo is dispatched directly to your Telegram chat.
   * High-frequency live GPS tracking automatically engages every 2 minutes.

---

## 📡 Offline Air-Gapped SMS Defense

When mobile data and Wi-Fi are disconnected, PASA listens for encrypted incoming SMS PDUs.

### Step 1: Enroll TOTP Secret
In your Telegram bot, send:
```text
/smssetup
```
The bot returns an `otpauth://` URI and secret key (e.g., `JCZBGTTY43FBF26AF6IPCQUT6JMJT4A2`). Add this to **Google Authenticator** or **Aegis**.

### Step 2: Sending Emergency SMS
From any phone, send an SMS to your phone's SIM number:
```text
PASA <6-digit-totp-code> /locate
```
*(Example: `PASA 419582 /locate`)*

The target phone intercepts the SMS silently, verifies the TOTP against its internal hardware clock, wakes the GPS receiver, and texts you back:
```text
PASA GPS: https://maps.google.com/?q=23.7771,90.3994 (Acc: 4m, Bat: 78%)
```

---

## 💳 Commercial Licensing & Payment Methods

PASA operates on a sovereign, one-time payment model — **no recurrent monthly subscriptions**. Own your mobile defense forever.

### Supported Payment Channels:
1. **Lemon Squeezy (Cards, Apple Pay, Google Pay) — Instant Digital Delivery:**
   * **Instant Checkout:** [Buy Pro Lifetime on Lemon Squeezy](https://pasa-sentinel.lemonsqueezy.com/checkout/buy/dee32362-b68f-4372-805a-c8ff4e310962)
   * Automated instant license key issuance, Zero-Touch QR code generation, and receipt.
2. **Binance Pay (Crypto USDT / BUSD):**
   * **Binance Pay ID / UID:** `756303714`
   * **Verified Payee Nickname:** `RBR48`
3. **bKash Personal (Bangladesh BDT):**
   * **Local Checkout & Manual Concierge:** Available at [https://pasa.izhaanintellect.fun/#pricing](https://pasa.izhaanintellect.fun/#pricing)

> 🔑 **Customer License Portal:** Already purchased? Access your license keys, active devices, and Zero-Touch provisioning QR codes at [**https://pasa.izhaanintellect.fun/portal**](https://pasa.izhaanintellect.fun/portal).

### License Tiers
| Tier | Price | Devices | Features |
|---|---|---|---|
| **Community Trial** | Free (7 Days) | 1 Device | Core Telegram C2, Camera & Siren triggers, Full Pro Evaluation |
| **Pro Lifetime** | **$25 USD / ৳3,000 BDT** | 1 Device | All 98+ C2 Commands Unlocked, Knox Device Owner & USB Data Pin Killswitch, Air-Gapped Cellular SMS Fallback (RFC 6238), Zero-Touch QR Provisioning, Lifetime Silent Background OTA Updates, 100-Year Ed25519 License |
| **Enterprise Fleet** | **$99 USD / ৳12,000 BDT** | 5 Devices | Everything in Pro for 5 Devices, Dedicated Isolated Server Relay Node, Priority Setup & Bulk Provisioning Profiles, VIP WhatsApp Concierge (+880 1762 033445) |

### Offline Ed25519 Cryptographic Verification:
* Licenses are cryptographically signed with military-grade **Ed25519** elliptic curves.
* Once activated, the app verifies the certificate **offline in <0.2ms**, requiring zero continuous internet connection.

### 🛡️ 7-Day No-Questions-Asked Money-Back Guarantee
Every paid license comes with an unconditional **7-day money-back guarantee**:
* **Full Refund:** If PASA Sentinel doesn't meet your defense requirements or your device has OEM constraints, simply request a refund within 7 days.
* **Rapid Payout:** 100% of your payment is sent back within 24 hours.
* **How to Claim:** Message Customer Support on WhatsApp at [**+880 1762-033445**](https://wa.me/8801762033445) or email [support@izhaanintellect.fun](mailto:support@izhaanintellect.fun).

---

## ⚖️ Source-Available Architecture & Responsible Use

PASA Sentinel is developed under a **Source-Available Security Model** licensed under the [Business Source License 1.1 (BSL 1.1)](LICENSE.md).

### Sovereign Non-Commercial Rights
* **100% Code Auditability:** The complete Android client, Telegram bot C2 server, and deployment scripts are openly auditable by anyone to verify the strict Zero-Storage and Zero-Telemetry guarantees.
* **Personal Defense Grant:** Individuals are free to inspect, compile, test, and run PASA Sentinel for their personal asset defense on devices they physically own.
* **Commercial Protection:** Unlicensed commercial resale, white-label distribution, or managed cloud/SaaS surveillance hosting is strictly prohibited. On September 25, 2030, this code transitions automatically to GPL-3.0-or-later.

### Strict Anti-Stalkerware Ethical Charter
PASA Sentinel is engineered exclusively as a **defensive countermeasure** against phone theft, street robbery, extortion, and forensic exploitation. Deploying PASA Sentinel to track, monitor, or intercept spouses, partners, or third parties without their documented, explicit consent is strictly prohibited and constitutes a criminal offense. See [RESPONSIBLE_USE.md](RESPONSIBLE_USE.md) for full legal and ethical guidelines.

---

## 🛡️ Security & Vulnerability Disclosure

Security is fundamental to our mission. PASA Sentinel adheres to **RFC 9116** for coordinated vulnerability disclosure:

* **Security Policy:** [SECURITY.md](SECURITY.md)
* **Canonical Security Contact:** [security@izhaanintellect.fun](mailto:security@izhaanintellect.fun)
* **RFC 9116 Metadata:** [https://pasa.izhaanintellect.fun/.well-known/security.txt](https://pasa.izhaanintellect.fun/.well-known/security.txt)
* **Audit Verification Log:** [AUDIT_TRAIL.md](https://pasa.izhaanintellect.fun/AUDIT_TRAIL.md) | [INDEPENDENT_AUDIT_REPORT.json](https://pasa.izhaanintellect.fun/INDEPENDENT_AUDIT_REPORT.json)

---

## 🛠️ Building from Source

### Prerequisites
* JDK 17 (Microsoft OpenJDK 17 or Eclipse Temurin 17)
* Android SDK Platform 36 (Android 16)
* Gradle 8.7

```bash
# 1. Clone repository
git clone https://github.com/rbr48/pasa.git
cd pasa

# 2. Set Java 17 Home (Windows PowerShell)
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"

# 3. Assemble Release APK
.\gradlew.bat assembleRelease
```
The signed APK will be output to:  
`app/build/outputs/apk/release/app-release.apk`

---

## 🔐 Release Verification & Integrity

Download the official signed release binary directly from GitHub or our high-speed CDN:

* **Direct Release Binary:** [📦 pasa-v3.7.16-81.apk](https://github.com/rbr48/pasa/raw/main/releases/pasa-v3.7.16-81.apk)
* **Direct Rolling Latest:** [📦 pasa-latest.apk](https://github.com/rbr48/pasa/raw/main/releases/pasa-latest.apk)
* **High-Speed CDN Mirror:** [https://pasa.izhaanintellect.fun/releases/pasa-latest.apk](https://pasa.izhaanintellect.fun/releases/pasa-latest.apk)
* **Windows Setup Kit:** [📦 PASA-Device-Owner-Setup-Kit.zip](https://pasa.izhaanintellect.fun/releases/PASA-Device-Owner-Setup-Kit.zip)
* **Complete SARIF Audit Bundle:** [📦 PASA-Security-Audit-SARIF-v3.7.16.zip](https://pasa.izhaanintellect.fun/audit/PASA-Security-Audit-SARIF-v3.7.16.zip)
* **Official Version:** `v3.7.16` (Build Code `81`)
* **File Size:** `19,331,755 bytes` (18.44 MB)
* **APK SHA-256 Checksum:**
  ```text
  6ba3c5df252591f7c090359d27c99f27defe63e6f64bef72256e544ff9dff55e
  ```
* **Setup Kit SHA-256 Checksum:**
  ```text
  7d1f99b6bfaf9975e570414bd1907b30e036952d3601601ee4cebde668080766
  ```
* **SARIF Bundle SHA-256 Checksum:**
  ```text
  3271918168d539e90c3b8b0c49efa987ee27d1136a60f13109553a55b4a805fb
  ```
* **Independent Audit Portal:** [https://pasa.izhaanintellect.fun/audit/](https://pasa.izhaanintellect.fun/audit/)
* **VirusTotal Multi-AV Consensus:** [Inspect VirusTotal Audit Report](https://www.virustotal.com/gui/file/6ba3c5df252591f7c090359d27c99f27defe63e6f64bef72256e544ff9dff55e)

### Verify on Windows PowerShell:
```powershell
Get-FileHash pasa-latest.apk -Algorithm SHA256
```

### Verify on Linux / macOS:
```bash
sha256sum pasa-latest.apk
```

---

## ⚖️ License & Legal Attribution

* **Software License:** Licensed under the [Business Source License 1.1 (BSL 1.1)](LICENSE.md). Free for personal defense, academic research, and cryptographic inspection. Transitions to GPL-3.0-or-later on September 25, 2030.
* **Ethical Use Charter:** Use is governed by the mandatory [Responsible Use Policy (Anti-Stalkerware)](RESPONSIBLE_USE.md).
* **Commercial Rights:** Production binaries, automated VPS control plane infrastructure, and enterprise deployment services are proprietary to Izhaan Intellect.
* **Disclaimer:** PASA Sentinel is a sovereign defensive security tool intended strictly for personal asset recovery, anti-theft defense, and enterprise device tracking on devices you legally own. Unauthorized surveillance of third parties without consent is strictly prohibited.
* **Copyright:** &copy; 2026 PASA Sentinel / Izhaan Intellect. All rights reserved.
