# 🛡️ PASA Sentinel (Physical Anti-Theft Security Agent)

> **Next-Generation Sovereign Mobile Defense & Covert Anti-Theft Agent for Android (8.0 – 16).**  
> *Zero Google Play dependencies. Air-gapped cellular SMS fallback. Knox-grade Device Owner permanence. Dual-channel Telegram C2.*

[![Android](https://img.shields.io/badge/Android-8.0%20to%2016%20(API%2036)-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)](https://kotlinlang.org)
[![Version](https://img.shields.io/badge/Release-v3.1.6%20(Build%2022)-blue.svg)](https://pasa.izhaanintellect.fun/api/app/download/pasa-latest.apk)
[![Zero Telemetry](https://img.shields.io/badge/Privacy-Zero%20Telemetry%20Policy-brightgreen.svg)](PRIVACY.md)
[![Terms of Service](https://img.shields.io/badge/Legal-Terms%20of%20Service%20%26%20EULA-blueviolet.svg)](TERMS.md)
[![7-Day Guarantee](https://img.shields.io/badge/Guarantee-7--Day%20Money%20Back-30D158.svg)](PRIVACY.md#-4-7-day-money-back-guarantee-binance-pay)
[![Binance Pay](https://img.shields.io/badge/Payments-Binance%20Pay%20Verified-F0B90B.svg)](#commercial-licensing--binance-pay)

---

## 📑 Table of Contents
1. [The Problem: Why Google "Find My Device" Fails](#the-problem-why-google-find-my-device-fails)
2. [Core Defense Architecture](#core-defense-architecture)
3. [Complete Telegram Bot Setup Guide](#complete-telegram-bot-setup-guide)
   - [Method 1: Private Dedicated Bot via @BotFather (Recommended)](#method-1-private-dedicated-bot-via-botfather-recommended)
   - [Method 2: Instant Pairing via @Pas_agent_bot](#method-2-instant-pairing-via-pas_agent_bot)
4. [Enterprise Device Owner & Persistence Setup](#enterprise-device-owner--persistence-setup)
5. [Telegram C2 Command Glossary](#telegram-c2-command-glossary)
6. [Offline Air-Gapped SMS Defense](#offline-air-gapped-sms-defense)
7. [Commercial Licensing & Binance Pay](#commercial-licensing--binance-pay)
   - [7-Day Money-Back Guarantee](#-7-day-no-questions-asked-money-back-guarantee)
8. [Building from Source](#building-from-source)
9. [Release Verification & Integrity](#release-verification--integrity)
10. [Zero-Telemetry Privacy Policy](PRIVACY.md)
11. [Terms of Service & EULA (Mandatory)](TERMS.md)

---

## ⚠️ The Problem: Why Google "Find My Device" Fails

When a smartphone is stolen, street thieves act within the first **5 to 10 seconds**:

1. **They cut the Internet immediately:** Swiping down Quick Settings to enable **Airplane Mode** or ripping out the SIM card. The second Wi-Fi and mobile data drop, Google "Find My Device", Samsung SmartThings, and standard tracker apps go completely blind.
2. **They power down the phone:** Holding the power button shuts down standard Android devices completely.
3. **They boot into Safe Mode or Factory Reset:** In Safe Mode, all third-party apps are disabled, allowing the thief to wipe or uninstall standard security apps.
4. **Subscription creep & Privacy violations:** Most commercial trackers (Life360, Prey, Cerberus) charge **$50–$100/year** on recurrent subscriptions while continuously uploading user location and contacts to corporate clouds.

### 🛡️ How PASA Sentinel Solves This:
* **Air-Gapped SMS Fallback:** When internet is dead, GSM cellular signal remains connected. A single encrypted SMS command powers up the hardware GPS receiver chip and texts back exact Google Maps coordinates directly from satellites.
* **Fake Shutdown Deception:** When the thief tries to turn off the phone, PASA presents a spoofed power-down sequence and enters an AMOLED 0-nit black screen mode. The thief thinks the phone is dead, while PASA covertly records ambient audio and streams background GPS beacons.
* **Autonomous Anti-Snatch Kinetic Lock:** If snatched from your hands (acceleration delta > 2.85G), PASA instantly locks the screen via Device Owner, captures a silent front-camera mugshot of the thief, and blasts a maximum-volume alarm.
* **Knox-Grade Device Owner:** The Android "Uninstall" button is permanently grayed out. Safe Mode and developer options can be cryptographically disabled.
* **Zero Big-Tech Telemetry:** All communication is routed strictly between your device, your private Telegram Bot, and your self-hosted control plane.

---

## 🏛️ Core Defense Architecture

```
                                +-----------------------------------+
                                |     Owner C2 Terminal (Telegram)  |
                                +-----------------+-----------------+
                                                  |
                         Dual-Channel C2          | HTTPS Poll / Webhook
                         Failover Protocol        v
+------------------+          Encrypted   +-------+-------+          Encrypted   +--------------------+
|  GSM Radio Tower |<====== Cellular PDU =| PASA Sentinel |<====== HTTPS Payload =| PASA Control Plane |
+--------+---------+         (Offline)    | Android Agent |          (Online)    |  (VPS / Node.js)   |
         |                                +-------+-------+                      +--------------------+
         | Emergency SMS                          |
         v                                        +---> Covert Camera2 (No Viewfinder / Screen Off)
+--------+---------+                              +---> AudioRecord Buffer (16kHz PCM / AAC)
|  Emergency Phone |                              +---> Hardware GNSS Direct Satellite Lock
+------------------+                              +---> G-Force Sensor (Anti-Snatch Delta Detector)
                                                  +---> AMOLED 0-Nit Fake Shutdown Canvas
```

---

## 🤖 Complete Telegram Bot Setup Guide

PASA Sentinel uses Telegram as its primary Command and Control (C2) console. You can connect using a **private dedicated bot** (maximum sovereignty) or the **official pre-configured bot**.

---

### Method 1: Private Dedicated Bot via @BotFather (Recommended)

Running your own private bot means only you have the credentials, with zero shared traffic.

#### Step 1: Create your bot in Telegram
1. Open Telegram and search for the official **`@BotFather`** ([https://t.me/BotFather](https://t.me/BotFather)).
2. Tap **Start** and send the command:
   ```text
   /newbot
   ```
3. Enter a friendly name for your bot (e.g., `My Phone Sentinel`).
4. Enter a unique username ending in `bot` (e.g., `my_device_pasa_bot`).
5. **@BotFather will give you an HTTP API Token:**
   ```text
   Use this token to access the HTTP API:
   7894561230:AAGabcdef1234567890abcdef1234567890
   ```
   *Keep this token secret!*

#### Step 2: Obtain your personal Telegram Chat ID
PASA only accepts commands from **your specific Telegram account** so no stranger can command your phone.
1. Open Telegram and search for **`@userinfobot`** ([https://t.me/userinfobot](https://t.me/userinfobot)).
2. Tap **Start**.
3. It will reply with your profile info. Copy your **`Id`** (a number such as `123456789`).

#### Step 3: Connect your Phone in the PASA App
1. Download and open **PASA Sentinel** on your Android phone.
2. In the setup wizard:
   * **Telegram Bot Token:** Paste the token from `@BotFather`.
   * **Owner Chat ID:** Paste your numeric ID from `@userinfobot`.
   * **Master PIN:** Choose a 4- to 8-digit emergency PIN (used for SMS authentication & unlocking).
   * **Server URL:** Default is `https://izhaanintellect.fun/pasa/` (or your private VPS URL).
3. Tap **Connect & Initialize Security Agent**.
4. Grant the required Android permissions (Camera, Microphone, Location: Always Allow, Battery: Unrestricted).

#### Step 4: Verify Remote C2
1. Open your newly created bot in Telegram and send:
   ```text
   /start
   ```
2. The bot will welcome you and display the **Interactive Tactical Dashboard** with status buttons.
3. Send `/ping` or `/locate` to confirm instant bidirectional telemetry!

---

### Method 2: Instant 6-Digit Pairing via @Pas_agent_bot

If you prefer instant automated onboarding without creating your own bot via @BotFather:

> 🛡️ **Sovereign Security Advisory:**  
> Method 1 (Private Bot via @BotFather) is **recommended for zero-trust, maximum privacy**. With Method 1, only you possess the bot token.  
> Method 2 routes commands through the central PASA gateway. While protected by device-binding OTPs and anti-brute-force rate limits (max 3 failed attempts before a 1-hour ban), Method 1 provides true sovereign autonomy.

1. In the PASA Android app setup screen, accept the **Mandatory Terms & Conditions**.
2. Tap **"⚡ Instant Pair via @Pas_agent_bot"**.
3. Review the Sovereign Security Advisory and tap **"Proceed with Instant Pair"**.
4. The app generates a 3-minute ephemeral 6-digit pairing code (e.g. `839 201`).
5. Open Telegram, start **[@Pas_agent_bot](https://t.me/Pas_agent_bot)**, and send the 6-digit code (or tap the direct link button).
6. `@Pas_agent_bot` confirms the link, and your Android device auto-configures and activates instantly!

---

### 🔒 Pure Remote Unlock (Impenetrable Lost Mode)

* **Telegram-Only Unlock:** To eliminate vulnerabilities and keypad brute-forcing by thieves, the lock screen has **zero on-screen PIN keypads**.
* **Impenetrable Lockdown:** When locked via `/lock` or sensor triggers, the screen is locked in Knox Kiosk mode. It can **only be unlocked remotely** by sending `/unlock` from your verified Telegram C2 bot.
* **Covert Touch Trap:** Any physical touch or swipe on the locked screen silently triggers front-camera mugshots and satellite GPS telemetry dispatched directly to your Telegram chat.

---

## 🔒 Enterprise Device Owner & Persistence Setup

To make PASA Sentinel **impossible to uninstall** and allow it to lock Airplane Mode, run this one-time ADB command:

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
* 📱 **Status Bar Lockdown:** Completely disables pulling down the notification shade and quick settings.
* 🚫 **Safe Mode Lockout:** Prevents booting into Safe Mode to disable security services.
* ✈️ **Network Protection:** Blocks unauthorized toggling of Airplane Mode, USB debugging, or file transfers while locked.
* 👁️ **100% Invisible Stealth:** Zero persistent notification icons via `IMPORTANCE_MIN` channel and hidden launcher icon.

> **Crucial Android 14–16 Setting:**  
> Go to **App Info → PASA Sentinel → Battery** and set to **"Unrestricted"**. This prevents Android's aggressive background sleep killers from suspending the telemetry daemon.

---

## 🕹️ Complete Telegram C2 Command Glossary (44 Commands)

Send these commands directly to your Telegram bot (or use the interactive menu autocomplete):

| Category | Command | Syntax | Action & Behavior |
|---|---|---|---|
| **Core & Control** | `/menu` | `/menu` | 📱 Opens interactive touchscreen dashboard & quick controls |
| | `/help` | `/help` | 📖 Complete documentation manual & command guide |
| | `/status` | `/status` | 📊 Real-time battery, storage, RAM & sensor diagnostics |
| | `/selftest` | `/selftest` | 🩺 Comprehensive 9-point security & sensor diagnostic audit |
| | `/info` | `/info` | ℹ️ Hardware specs, SIM card details, and OS patch level |
| **Location & Geofencing** | `/locate` | `/locate` or `/gps` | 📍 Acquires high-accuracy GNSS fix and sends Google Maps pin |
| | `/track` | `/track [minutes]` | 🛰️ Starts continuous periodic GPS tracking |
| | `/track_stop` | `/track_stop` | 🛑 Stops ongoing continuous GPS tracking |
| | `/geofence` | `/geofence here 200` | 🌐 Sets safe zone radius & breach/return alerts |
| **Covert Forensics** | `/snap` | `/snap front\|back\|both` | 📸 Silent covert photo with zero preview or shutter sound |
| | `/screenshot` | `/screenshot` | 📱 Silent full-screen capture via native Accessibility |
| | `/screen_burst` | `/screen_burst [5-10]` | 🎞️ Rapid 5–10 frame storyboard montage of intruder activity |
| | `/screenrecord` | `/screenrecord [seconds]` | 🎥 Covert HD MP4 screen recording (5–60s) via Device Owner |
| | `/video` | `/video front\|back [sec]` | 📹 Silent camera video clip (1–60s) without screen wake |
| | `/record` | `/record [seconds]` | 🎙️ Silent ambient microphone wiretap (default 30s) |
| | `/clipboard` | `/clipboard` | 📋 Reads current device clipboard text |
| **Lockdown & Emergency** | `/lock` | `/lock [pin] [msg]` | 🔒 Locks device with emergency PIN & Lost Mode banner |
| | `/lock_message` | `/lock_message <text>` | 💬 Updates lockscreen banner message |
| | `/lock_pin` | `/lock_pin <pin>` | 🔑 Locks phone with explicit 4–8 digit emergency PIN |
| | `/set_os_pin` | `/set_os_pin <pin>` | 🔐 Overwrites physical Android OS lockscreen PIN (Device Owner) |
| | `/unlock` | `/unlock` | 🔓 Dismisses Lost Mode & restores normal device UI |
| | `/fakeshutdown` | `/fakeshutdown` | 🕶️ Fake shutdown: blackout screen & silent touch traps |
| | `/wake` | `/wake` | ☀️ Restores device from Fake Shutdown blackout |
| | `/ring` | `/ring [seconds]` | 🚨 Blasts maximum volume 100% siren, overriding silent/DND |
| | `/ring_stop` | `/ring_stop` | 🔇 Silences active emergency siren immediately |
| | `/message` | `/message <text>` | 📢 Displays urgent fullscreen alert banner on display |
| **Defense & Deception** | `/duress_pin` | `/duress_pin <pin>` | 🆘 Sets decoy coercion PIN: simulates unlock while sending SOS |
| | `/trap` | `/trap on\|off\|status` | 🛡️ Arms autonomous sensor traps (snatch, charger, pocket) |
| | `/shred` | `/shred <path>` | 🗑️ Cryptographically sanitizes sensitive files with zero-fill |
| | `/device_owner` | `/device_owner` | 👑 Checks Device Owner & Kiosk hardware lock status |
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

## 💳 Commercial Licensing & Binance Pay

PASA operates on a sovereign, one-time payment model — **no recurrent monthly subscriptions**.

### Exclusive Payment Provider: Binance Pay
* **Binance Pay ID / UID:** `756303714`
* **Verified Payee Nickname:** `RBR48`
* **Accepted Currency:** `USDT / BUSD`

### License Tiers
| Tier | Pricing | Devices Armed | Capabilities |
|---|:---:|:---:|---|
| **Community Trial** | **$0** (7 Days) | 1 Device | Basic Telegram C2, Camera snap, Siren test |
| **Pro Lifetime** | **$29.99 USDT** (One-time) | 3 Devices | Full Airgap SMS, Fake Shutdown, Anti-Snatch, Lifetime OTA Updates |
| **Fleet / Enterprise**| **$79.99 USDT** (One-time) | 10 Devices | Dedicated deployment engineering, ADB automation scripts |

### Anti-Fraud Two-Step Approval Workflow:
1. Buyer selects plan on [https://pasa.izhaanintellect.fun](https://pasa.izhaanintellect.fun) and sends USDT via Binance Pay.
2. Buyer enters email and submits transaction ID.
3. Order is held in `PENDING_APPROVAL`.
4. A real-time Telegram alert arrives on the operator's phone with interactive buttons:
   `[ ✅ Approve & Issue Key ]`  `[ ❌ Reject Fake Payment ]`
5. Upon confirmation, the cryptographically signed `PASA-LIFE-XXXX-XXXX` key appears automatically on the buyer's screen and is activated.

### 🛡️ 7-Day No-Questions-Asked Money-Back Guarantee
Every paid license comes with an unconditional **7-day money-back guarantee**:
* **Full Refund in USDT:** If PASA Sentinel doesn't meet your defense requirements or your device has OEM battery constraints, simply request a refund within 7 days.
* **Rapid Payout:** 100% of your payment is sent straight back to your Binance Pay ID / UID within 24 hours.
* **How to Claim:** Message Customer Support on WhatsApp at [**+880 1762-033445**](https://wa.me/8801762033445) or email [support@izhaanintellect.fun](mailto:support@izhaanintellect.fun) with your Order ID or License Key.
* Full policy documented in [PRIVACY.md](PRIVACY.md#-4-7-day-money-back-guarantee-binance-pay).

---

## 🛠️ Building from Source

### Prerequisites
* JDK 17 (Recommended: Microsoft OpenJDK 17 or Eclipse Temurin 17)
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

* **Direct GitHub Download:** [📦 PASA-Sentinel-v3.0.4.apk](https://github.com/rbr48/pasa/raw/main/releases/PASA-Sentinel-v3.0.4.apk)
* **Direct Rolling Latest:** [📦 pasa-latest.apk](https://github.com/rbr48/pasa/raw/main/releases/pasa-latest.apk)
* **High-Speed CDN Mirror:** [https://pasa.izhaanintellect.fun/api/app/download/pasa-latest.apk](https://pasa.izhaanintellect.fun/api/app/download/pasa-latest.apk)
* **Official Version:** `v3.0.4` (Build Code `10`)
* **File Size:** `18,967,500 bytes` (18.09 MB)
* **SHA-256 Checksum:**
  ```text
  2bb570ed8b1e82da9e2bcab602cca9ee03f0c4f1eacca493933f94238d955003
  ```

### Verify on Windows PowerShell:
```powershell
Get-FileHash pasa-latest.apk -Algorithm SHA256
```

### Verify on Linux / macOS:
```bash
sha256sum pasa-latest.apk
```

---

## ⚖️ License & Disclaimer

* **Disclaimer:** PASA Sentinel is a sovereign defensive security tool intended strictly for personal asset recovery, anti-theft defense, and enterprise device tracking on devices you legally own. Unauthorized surveillance of third parties without consent is strictly prohibited.
* **Copyright:** &copy; 2026 PASA Sentinel / Izhaan Intellect. All rights reserved.
