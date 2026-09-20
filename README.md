# 🛡️ PASA Sentinel (Physical Anti-Theft Security Agent)

> **Next-Generation Sovereign Mobile Defense & Covert Anti-Theft Agent for Android (8.0 – 16).**  
> *Zero Google Play dependencies. Air-gapped cellular SMS fallback. Knox-grade Device Owner permanence. Dual-channel Telegram C2.*

[![Android](https://img.shields.io/badge/Android-8.0%20to%2016%20(API%2036)-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)](https://kotlinlang.org)
[![Version](https://img.shields.io/badge/Release-v3.0.3%20(Build%209)-blue.svg)](https://pasa.izhaanintellect.fun/api/app/download/pasa-latest.apk)
[![Zero Telemetry](https://img.shields.io/badge/Privacy-Zero%20Telemetry%20Policy-brightgreen.svg)](PRIVACY.md)
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

### Method 2: Instant Pairing via @Pas_agent_bot

If you prefer not to create a bot via @BotFather:
1. Open the official PASA Bot: **[@Pas_agent_bot](https://t.me/Pas_agent_bot)**.
2. Tap **Start**.
3. In the PASA Android app, tap **"Quick Pair via @Pas_agent_bot"**.
4. The app displays a 6-digit one-time pairing code (e.g., `839 201`).
5. Send that code to `@Pas_agent_bot` in Telegram. Your device is now paired!

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
* 🚫 **Safe Mode Lockout:** Prevents booting into Safe Mode to disable security services.
* ✈️ **Network Protection:** Blocks unauthorized toggling of Airplane Mode while locked.
* 📱 **Status Bar Lockdown:** Disables pulling down the notification shade on the lockscreen.

> **Crucial Android 14–16 Setting:**  
> Go to **App Info → PASA Sentinel → Battery** and set to **"Unrestricted"**. This prevents Android's aggressive background sleep killers from suspending the telemetry daemon.

---

## 🕹️ Telegram C2 Command Glossary

Send these commands directly to your Telegram bot:

| Command | Syntax | Action & Behavior |
|---|---|---|
| **Live Location** | `/locate` or `/gps` | Wakes GNSS hardware, acquires direct satellite lock, returns precision Google Maps pin, altitude, speed, and accuracy. |
| **Forensic Camera** | `/snap` or `/photo [front\|rear]` | Captures high-res photo with **zero screen wake, zero preview, and zero shutter sound**. Delivered directly to Telegram. |
| **Ambient Wiretap** | `/record [seconds]` | Covertly activates microphone and streams audio file to Telegram (default 30s, up to 300s). |
| **Stealth Video** | `/video [front\|rear] [sec]` | Records stealth video without viewfinder preview and streams MP4 to chat. |
| **Emergency Siren** | `/ring` or `/siren [on\|off]` | Blasts maximum volume 100% SPL alarm, overriding silent switch and Do Not Disturb (DND). |
| **Fake Shutdown** | `/fakeshutdown` or `/blackout` | Spoofs Android power-down animation and enters 0-nit black screen trap mode while keeping all sensors armed. |
| **Wake from Trap** | `/wake` | Restores normal display controller from Fake Shutdown mode. |
| **Remote Lock** | `/lock` | Instantly locks device screen via Device Owner and enters Lost Mode. |
| **Telemetry Health** | `/status` | Returns battery level, charging state, cellular/Wi-Fi status, uptime, and sensor diagnostics. |
| **Offline TOTP** | `/smssetup` | Enrolls or rotates the time-based OTP secret for offline air-gapped SMS commands. |
| **License Check** | `/license` | Checks active Pro license status or activates key (`/license activate KEY`). |
| **Remote Wipe** | `/wipe` & `/wipe_confirm` | Two-step cryptographically verified factory reset for extreme compromise situations. |

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
* **How to Claim:** Message [@Pas_agent_bot](https://t.me/Pas_agent_bot) in Telegram or email [support@izhaanintellect.fun](mailto:support@izhaanintellect.fun) with your Order ID or License Key.
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

* **Direct GitHub Download:** [📦 PASA-Sentinel-v3.0.3.apk](https://github.com/rbr48/pasa/raw/main/releases/PASA-Sentinel-v3.0.3.apk)
* **Direct Rolling Latest:** [📦 pasa-latest.apk](https://github.com/rbr48/pasa/raw/main/releases/pasa-latest.apk)
* **High-Speed CDN Mirror:** [https://pasa.izhaanintellect.fun/api/app/download/pasa-latest.apk](https://pasa.izhaanintellect.fun/api/app/download/pasa-latest.apk)
* **Official Version:** `v3.0.3` (Build Code `9`)
* **File Size:** `18,967,424 bytes` (18.09 MB)
* **SHA-256 Checksum:**
  ```text
  b74f77b867ca498e1de5ac6e0f8fd9d528548111ddaf85202bc8217ebe519d39
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
