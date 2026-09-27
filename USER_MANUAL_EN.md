# 🛡️ PASA Sentinel (Private Android Security Agent)
## Sovereign Mobile Defense & Covert Anti-Theft System
### Comprehensive User Manual & Operational Field Guide (English Edition)
**Production Version:** v3.7.14 (Build 79) | **OS Target:** Android 8.0 – 16 (API 26 – 36) | **Document Revision:** 2026.2

---

## Table of Contents
1. [System Overview & Zero-Storage Philosophy](#1-system-overview--zero-storage-philosophy)
2. [Operating System Compatibility & Permissions Matrix](#2-operating-system-compatibility--permissions-matrix)
3. [Step-by-Step Installation & Device Owner Provisioning](#3-step-by-step-installation--device-owner-provisioning)
   - [Method A: Guided Windows Setup Kit (Recommended)](#method-a-guided-windows-setup-kit-recommended)
   - [Method B: Manual ADB CLI Method](#method-b-manual-adb-cli-method)
   - [Troubleshooting Device Owner Provisioning](#troubleshooting-device-owner-provisioning)
4. [Initial Setup, Credential Arming & Hardware Escrow](#4-initial-setup-credential-arming--hardware-escrow)
   - [Telegram Bot Creation & Pairing](#telegram-bot-creation--pairing)
   - [Master Password & Cryptographic Identity](#master-password--cryptographic-identity)
   - [Arming Hardware Escrow Token (Android 8.0 – 16 PIN Reset)](#arming-hardware-escrow-token-android-80--16-pin-reset)
   - [Air-Gapped TOTP SMS Fallback Enrollment](#air-gapped-totp-sms-fallback-enrollment)
   - [Commercial License Activation (Ed25519 Offline Verification)](#commercial-license-activation-ed25519-offline-verification)
5. [Telegram Bot C2 Operations & Interactive Console](#5-telegram-bot-c2-operations--interactive-console)
   - [The 4x3 Persistent Quick-Access Keyboard](#the-4x3-persistent-quick-access-keyboard)
   - [The 6-Hub Interactive Command Console (`/menu`)](#the-6-hub-interactive-command-console-menu)
   - [Interactive Conversational Wizards](#interactive-conversational-wizards)
6. [Complete Command Reference Matrix (All 94 Commands)](#6-complete-command-reference-matrix-all-94-commands)
   - [Category 1: Core & Diagnostics (8 Commands)](#category-1-core--diagnostics)
   - [Category 2: Enterprise Knox Device Owner Suite (22 Commands)](#category-2-enterprise-knox-device-owner-suite)
   - [Category 3: Location & Cellular RF Telemetry (8 Commands)](#category-3-location--cellular-rf-telemetry)
   - [Category 4: Covert Forensics & Surveillance (13 Commands)](#category-4-covert-forensics--surveillance)
   - [Category 5: Lockdown, Alert & Deception (13 Commands)](#category-5-lockdown-alert--deception)
   - [Category 6: Traps & Mobile Cyber Defense Suite (15 Commands)](#category-6-traps--mobile-cyber-defense-suite)
   - [Category 7: Extraction & Telephony (6 Commands)](#category-7-extraction--telephony)
   - [Category 8: System, Maintenance & Updates (9 Commands)](#category-8-system-maintenance--updates)
7. [Autonomous Sensor Traps & Edge Defenses](#7-autonomous-sensor-traps--edge-defenses)
   - [Snatch-and-Run Trap (`/trap snatch`)](#snatch-and-run-trap-trap-snatch)
   - [Charger Disconnect & USB Insertion Trap (`/trap charger`)](#charger-disconnect--usb-insertion-trap-trap-charger)
   - [Pocket & Bag Extraction Trap (`/trap pocket`)](#pocket--bag-extraction-trap-trap-pocket)
   - [SIM Ejection & Foreign SIM Auto-SMS Trap (`/sim_lock`)](#sim-ejection--foreign-sim-auto-sms-trap-sim_lock)
   - [Pattern Guard / Failed Lockscreen Trap (`/pattern_guard`)](#pattern-guard--failed-lockscreen-trap-pattern_guard)
   - [Thermal Anomaly Heat-Gun Trap (`/thermal`)](#thermal-anomaly-heat-gun-trap-thermal)
   - [Dead Man's Switch / Faraday Isolation Auto-Destruct (`/deadman`)](#dead-mans-switch--faraday-isolation-auto-destruct-deadman)
8. [High-Security Deception & Anti-Coercion Features](#8-high-security-deception--anti-coercion-features)
   - [Duress PIN & Sterile Sandbox Decoy OS (`/duress_pin`)](#duress-pin--sterile-sandbox-decoy-os-duress_pin)
   - [Fake Shutdown & Tactile Touch Sensor Canvas (`/fakeshutdown`, `/wake`)](#fake-shutdown--tactile-touch-sensor-canvas-fakeshutdown-wake)
   - [Stealth Mode & App Drawer Concealment (`/stealth`, `/hide`, `/show`)](#stealth-mode--app-drawer-concealment-stealth-hide-show)
   - [Shadow App Vault Freezing (`/freeze`, `/unfreeze`, `/frozen`)](#shadow-app-vault-freezing-freeze-unfreeze-frozen)
   - [App Network Isolation Firewall (`/app_firewall`)](#app-network-isolation-firewall-app_firewall)
   - [System-Wide Encrypted DNS-over-TLS (`/dns`)](#system-wide-encrypted-dns-over-tls-dns)
   - [Biometric Coercion Killswitch (`/biometrics`)](#biometric-coercion-killswitch-biometrics)
9. [Air-Gapped Cellular SMS Fallback Protocol](#9-air-gapped-cellular-sms-fallback-protocol)
   - [Command Protocol Syntax](#command-protocol-syntax)
   - [Dual-SIM Dynamic Return Routing](#dual-sim-dynamic-return-routing)
   - [Ready-to-Use 1-Tap Monospace SMS Templates](#ready-to-use-1-tap-monospace-sms-templates)
10. [Licensing, OTA Updates, Warranty & Emergency Contacts](#10-licensing-ota-updates-warranty--emergency-contacts)
    - [Licensing Tiers & Hard Lockout Policy](#licensing-tiers--hard-lockout-policy)
    - [Automated Silent OTA Updates (`/check_update`, `/update_confirm`)](#automated-silent-ota-updates-check_update-update_confirm)
    - [Payment Methods & 24-Hour Refund Guarantee](#payment-methods--24-hour-refund-guarantee)
    - [Emergency Operational Cheatsheet](#emergency-operational-cheatsheet)

---

## 1. System Overview & Zero-Storage Philosophy

**PASA Sentinel** is a sovereign mobile defense and covert anti-theft operating system for Android devices. Unlike conventional tracking apps that rely on Google Play Services, third-party cloud databases, or periodic location beacons, PASA Sentinel operates with an uncompromising architectural guarantee:

### 🛡️ Core Architectural Principles
* **Strategy 1: Direct-to-Telegram Zero-Storage Architecture:**  
  Zero surveillance photos, zero audio recordings, zero video clips, and zero GPS tracking histories are ever written to physical VPS disk or centralized databases. All user surveillance evidence is transmitted directly from device RAM to your private encrypted Telegram bot via HTTPS and immediately memory-shredded locally on the phone.
* **Knox-Grade Device Owner Permanence:**  
  Provisioned via Android Enterprise Device Owner (`dpm`), granting unrevokable anti-uninstall protection, kernel-level anti-tamper locks, hardware USB data pin severance, and cryptographic lockscreen reset capabilities.
* **Dual-Channel Out-of-Band C2:**  
  Primary command and control via encrypted Telegram Bot API (or self-hosted VPS relay); secondary air-gapped cellular SMS fallback authenticated via RFC 6238 TOTP or Master Cryptographic PIN.
* **Zero Google Play Dependencies:**  
  100% operational on de-Googled ROMs (GrapheneOS, CalyxOS, LineageOS) and standard OEM distributions without Google Play Services or Firebase.
* **ASTRA Cryptographic Security Layer:**  
  Hardware-backed Android Keystore / StrongBox keys (P-256 ECDSA), server-side Ed25519 command verification, and offline Ed25519 license validation in <0.2ms.

---

## 2. Operating System Compatibility & Permissions Matrix

### 2.1 OS Compatibility
* **Minimum Android Version:** Android 8.0 Oreo (API level 26).
* **Target Android Version:** Android 16 (API level 36, `compileSdk = 36`, `targetSdk = 36`).
* **Supported Architectures:** ARM64-v8a, ARMeabi-v7a, x86, x86_64.

### 2.2 Permissions Matrix
PASA Sentinel requires advanced system permissions to enforce security and forensics. When provisioned as Device Owner, these permissions are locked permanently via `/self_heal` ("Managed by your organization") so neither a thief nor system cleaners can revoke them:

| Permission Name | Category | Operational Purpose |
|---|---|---|
| `INTERNET` | Connectivity | Direct communication with Telegram Bot API and C2 server. |
| `ACCESS_FINE_LOCATION` | Forensics | High-precision GNSS/GPS satellite positioning for `/locate` and traps. |
| `ACCESS_BACKGROUND_LOCATION` | Forensics | Continuous background tracking while screen is turned off. |
| `ACCESS_COARSE_LOCATION` | Telemetry | Indoor cell tower and Wi-Fi triangulation (`/tower`). |
| `CAMERA` | Forensics | Headless covert photography (`/snap`), video (`/video`), and theft mugshots. |
| `RECORD_AUDIO` | Forensics | Ambient covert microphone recordings (`/record`) and audio livestreaming. |
| `READ_PHONE_STATE` | Telephony | SIM ICCID, carrier telemetry, and radio diagnostics (`/sim`). |
| `READ_PHONE_NUMBERS` | Telephony | Resolves telephone line numbers for multi-SIM outbound routing. |
| `SEND_SMS` & `RECEIVE_SMS` | C2 Fallback | High-priority air-gapped SMS fallback receiver (`priority=999`) and SIM swap alert SMS. |
| `READ_CALL_LOG` & `READ_CONTACTS` | Extraction | Emergency forensic extraction of call logs and phonebook during recovery (`/call_log`, `/contacts`). |
| `READ_MEDIA_IMAGES/VIDEO/AUDIO` | Storage | Forensic camera roll recovery (`/gallery_latest`, `/getfile`). |
| `MANAGE_EXTERNAL_STORAGE` | Forensic / Wipe | Storage indexing (`/list_files`) and multi-pass cryptographic file shredding (`/shred`). |
| `FOREGROUND_SERVICE_*` | Persistence | Android 14+ dynamic service elevation for headless camera, audio, location, and data sync. |
| `SYSTEM_ALERT_WINDOW` | UI & Deception | Renders Lost Mode Kiosk screen, lockscreen broadcast banners, and blackout overlay. |
| `BIND_ACCESSIBILITY_SERVICE` | Forensics / Duress | Non-intrusive screenshot capture (`/screenshot`) and lockscreen Duress PIN detection. |
| `BIND_DEVICE_ADMIN` | Administration | Low-level Device Policy Manager enforcement (`PasaDeviceAdmin`). |

---

## 3. Step-by-Step Installation & Device Owner Provisioning

Device Owner privilege elevates PASA Sentinel above ordinary applications, conferring complete immunity to uninstallation and unlocking hardware killswitches.

### Critical Provisioning Prerequisite (Zero-Account Rule)
Android security architecture forbids setting a Device Owner if any user account is registered on the device.
1. Open phone **Settings → Passwords & Accounts** (or **Accounts & Sync**).
2. Temporarily remove your Google Account, Samsung Account, Xiaomi/Mi Account, or corporate email accounts.
> **Note:** Removing your Google account signs you out temporarily without deleting your cloud emails, photos, or contacts. You will log right back in immediately after completing Step 5!
3. Enable Developer Options: Go to **Settings → About Phone**, tap **Build Number** 7 times, then go to **Settings → System → Developer Options** and enable **USB Debugging**.

---

### Method A: Guided Windows Setup Kit (Recommended)
The official Windows Setup Kit automates the entire process in under 3 minutes without requiring manual command typing.

1. **Download the Kit:**  
   Download `PASA-Device-Owner-Setup-Kit.zip` from `https://pasa.izhaanintellect.fun/releases/PASA-Device-Owner-Setup-Kit.zip` and extract it to your PC.
2. **Launch Setup:**  
   Right-click `PASA Device Owner Setup.bat` and select **Run as Administrator** (or double-click). It automatically launches the 6-step guided wizard `setup.ps1` with bypass execution policy.
3. **Step 1 — ADB Check:**  
   The wizard checks for Android Debug Bridge (`adb.exe`). If missing, it automatically downloads official Google platform-tools (~8 MB) directly to the kit.
4. **Step 2 — Connect Phone:**  
   Connect your Android phone to the PC via USB cable. On the phone screen, check **Always allow from this computer** and tap **OK**. The wizard displays your device model and Android version.
5. **Step 3 — Auto-Assisted Account Detection:**  
   The wizard scans for active accounts. If any account remains, it automatically launches the account removal screen directly on your phone display! Tap **Remove Account** on the phone and press Enter on the PC.
6. **Step 4 — Smart Version Verification & Install:**  
   The wizard queries `https://pasa.izhaanintellect.fun/api/app/latest?current_version_code=0`. If PASA Sentinel is already installed and up-to-date, it skips download. Otherwise, it downloads the verified production APK, checks its SHA-256 hash, and installs it via `adb install -r`.
7. **Step 5 — Provision Device Owner:**  
   The wizard issues the provisioning command. Upon seeing `Success: Device owner set`, Knox-grade permanence is active!
8. **Step 6 — Finalize:**  
   Re-add your Google/Samsung accounts in phone settings. Open PASA Sentinel on your phone to complete initial setup.

---

### Method B: Manual ADB CLI Method
For Linux, macOS, or advanced Windows developers:

```bash
# 1. Connect phone and verify authorization
adb devices -l

# 2. Check for active accounts (must output 0 user accounts)
adb shell dumpsys account | grep -E "Account \{"

# 3. Install production APK
adb install -r pasa-latest.apk

# 4. Provision PASA Sentinel as Device Owner
adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin

# Expected Success Output:
# Success: Device owner set to package ComponentInfo{com.izhaanintellect.pasa/com.izhaanintellect.pasa.admin.PasaDeviceAdmin}
# Active admin set to component {com.izhaanintellect.pasa/com.izhaanintellect.pasa.admin.PasaDeviceAdmin}

# 5. Verify Device Owner registration
adb shell dpm list-owners
```

---

### Troubleshooting Device Owner Provisioning

* **Error: `java.lang.IllegalStateException: Not allowed to set device owner because there are already some accounts on the device`**  
  *Fix:* An account is still logged in. Open Settings → Accounts, check Google, Samsung, Telegram, WhatsApp, or work profiles, remove them, and rerun the command.
* **Error: `java.lang.IllegalStateException: Active admin already set for another package`**  
  *Fix:* Another app has Device Admin rights. Go to Settings → Security → Device Admin Apps and disable them. If the device was previously enrolled in corporate MDM, a factory reset is required.
* **Error: `Device unauthorized`**  
  *Fix:* Check your phone screen. Unlock it, unplug and replug the USB cable, and ensure you check **Always allow from this computer** before pressing OK.
* **Error: `Failed to set device owner: Unknown component`**  
  *Fix:* The APK is not installed yet. Run `adb install -r pasa-latest.apk` first.

---

## 4. Initial Setup, Credential Arming & Hardware Escrow

When you first open PASA Sentinel on your device, you are greeted by the Setup Wizard (`SetupActivity`).

```
┌────────────────────────────────────────────────────────┐
│            🛡️ PASA SENTINEL DEFENSE SETUP             │
├────────────────────────────────────────────────────────┤
│  [ Shield Logo ] (Tap 5x for Custom VPS Gateway)       │
│                                                        │
│  Telegram Bot Token:   [ 123456789:ABCdefGhIJK... ]    │
│  Telegram Owner Chat ID:[ 987654321               ]    │
│  Master Password:      [ ********************     ]    │
│  Backup Email:         [ owner@protonmail.com     ]    │
│  [X] Hide launcher icon immediately (Stealth Mode)     │
│                                                        │
│  [ Grant Permissions ]  [ Activate Device Admin ]      │
│  [ Whitelist Battery ]  [ Enable Accessibility ]       │
│  [X] I accept Sovereign Terms of Service               │
│                                                        │
│             [ ACTIVATE SYSTEM DEFENSE ]                │
└────────────────────────────────────────────────────────┘
```

### Telegram Bot Creation & Pairing
1. Open Telegram and search for `@BotFather`.
2. Send `/newbot`, provide a display name (e.g. `Sentinel Core`), and choose a username ending in `bot` (e.g. `my_alpha_sentinel_bot`).
3. Copy the HTTP API token provided by BotFather.
4. Message `@userinfobot` in Telegram to discover your personal numeric **User ID** (e.g. `987654321`).
5. Paste the Bot Token and Chat ID into the setup wizard. Tap **Verify Bot Connection** to confirm instant pairing with `@<bot_username>`.

### Master Password & Cryptographic Identity
* Choose a robust Master Password (minimum 8 alphanumeric characters).
* **Storage:** Master credentials are salted with PBKDF2 and secured inside Android Keystore / EncryptedSharedPreferences.
* **Usage:** Required to enter Setup Dashboard, authorized file shredding (`/shred`), video capture (`/video`), remote wipe (`/wipe_confirm`), or manual Lost Mode exit.

### Arming Hardware Escrow Token (Android 8.0 – 16 PIN Reset)
Android 14+ removed the legacy `resetPassword()` API. PASA Sentinel implements cryptographic hardware escrow tokens:
1. During setup, PASA generates a secure 32-byte hardware random token and registers it with the OS: `dpm.setResetPasswordToken()`.
2. On the Guardian Dashboard, you will see:  
   `👑 Device Owner: Active | ⚠️ Remote OS PIN Reset: Pending (Tap to Arm)`
3. **Arming Action:** Tap the pending banner (or lock your phone with the power button and unlock once with your current screen PIN/pattern/fingerprint).
4. The system Keyguard verifies your credentials and cryptographically seals the escrow token into the synthetic password HAL.
5. The banner transitions to: `✅ Remote OS PIN Reset: Armed & Ready`. You can now remotely change or clear your phone's lockscreen PIN anytime via Telegram using `/set_os_pin <new_pin>`.

### Air-Gapped TOTP SMS Fallback Enrollment
To enable emergency control when the device has no mobile data or Wi-Fi:
1. In your Telegram bot, send `/smssetup`.
2. The bot generates a 16-character Base32 secret key, an `otpauth://` URI, and a setup QR code.
3. Scan the QR code into your authenticator app (**Google Authenticator**, **Aegis**, **Bitwarden**, or **1Password**).
4. You can now execute offline emergency commands via SMS:  
   `PASA <6-digit-TOTP-code> <command>` (e.g. `PASA 481029 /locate`).

### Commercial License Activation (Ed25519 Offline Verification)
Every installation starts with a **7-Day Full-Featured Free Trial** granting access to all 94 commands.
1. Check status anytime: Send `/license` in Telegram.
2. To purchase a lifetime key, visit `https://pasa.izhaanintellect.fun/#pricing`.
3. Activate: Send in Telegram:  
   `/license activate PASA-PRO-XXXX-XXXX`
4. The VPS signs a cryptographic device certificate with Ed25519. The Android app verifies this certificate offline in **<0.2ms**, permanently unlocking lifetime defense with zero recurring network check-ins.

---

## 5. Telegram Bot C2 Operations & Interactive Console

### The 4x3 Persistent Quick-Access Keyboard
For split-second emergency reaction, PASA Sentinel pins a 4x3 ergonomic keyboard beneath the Telegram chat input:

```
┌─────────────────────┬─────────────────────┬─────────────────────┐
│      📊 Status      │      📍 Locate      │      🚨 Siren       │
├─────────────────────┼─────────────────────┼─────────────────────┤
│      📸 Photo       │      📱 Screen      │      🎥 Video       │
├─────────────────────┼─────────────────────┼─────────────────────┤
│       🔒 Lock       │     🎛️ Hub Menu     │   👑 Device Owner   │
├─────────────────────┼─────────────────────┼─────────────────────┤
│     💬 Message      │      🛡️ Traps       │      🔑 License     │
└─────────────────────┴─────────────────────┴─────────────────────┘
```

* **Status:** Instant live telemetry readout (battery, GPS coordinates, network, storage, uptime).
* **Locate:** Forcibly powers on GNSS receiver and sends Google Maps link.
* **Siren:** Blasts high-decibel alarm siren overriding silent mode.
* **Photo:** Headless front selfie snapshot.
* **Screen:** Covert accessibility screenshot.
* **Video:** Headless 15-second video recording.
* **Lock:** Engages Knox Kiosk Lost Mode.
* **Hub Menu:** Opens the main 6-Hub interactive console.
* **Device Owner:** Audits enterprise Knox policies and hardware locks.
* **Message:** Launches screen broadcast prompt.
* **Traps:** Opens autonomous sensor traps status and arming panel.
* **License:** Displays licensing tier, days remaining, or activation prompt.

---

### The 6-Hub Interactive Command Console (`/menu`)
Sending `/menu` displays live system telemetry badges (Connection latency, Battery level, StrongBox status, License tier) and gives access to the 6 operational command hubs:

```
╔═════════════════════════════════════════════════════════════╗
║               🛡️ PASA SENTINEL C2 CONSOLE                   ║
║  🟢 Online (<45s) | 🔋 88% Charging | 🛡️ StrongBox TEE      ║
║  💎 Pro Lifetime Shield | 👑 Knox Device Owner: ACTIVE      ║
╚═════════════════════════════════════════════════════════════╝
 📍 [1. Location & Cellular RF]     📸 [2. Covert Forensics]
 🚨 [3. Lockdown & Siren]          👑 [4. Enterprise Device Owner]
 🛡️ [5. Autonomous Traps]          📇 [6. Extraction & Logs]
```

1. **📍 Location & Cellular RF (`menu:location_hub`):** Instant GPS fix, Cell tower triangulation (`/tower`), live route tracking (`/track`), radial geofencing (`/geofence`), SIM radio telemetry (`/sim`), and SIM Swap Guard (`/sim_lock`).
2. **📸 Covert Forensics & Surveillance (`menu:forensics_hub`):** Headless front/rear/dual photos (`/snap`), silent screenshots (`/screenshot`), storyboard frame burst (`/screen_burst`), video capture (`/video`), ambient audio wiretap (`/record`), progressive video livestreaming (`/livestream`), clipboard extraction, and camera roll recovery.
3. **🚨 Emergency Lockdown & Siren (`menu:lockdown_hub`):** Instant Knox Kiosk lock (`/lock`), remote unlock (`/unlock`), custom PIN lock, emergency siren (`/ring`), tactile silent vibration pulse (`/vibrate_pulse`), and fake shutdown deception (`/fakeshutdown`).
4. **👑 Enterprise Knox Device Owner Suite (`menu:device_owner_hub`):** Hardware USB data killswitch (`/usb_lock`), camera killswitch (`/camera_lock`), Bluetooth disable (`/bluetooth_lock`), HAL mic mute (`/mic_mute`), lockscreen recovery banner (`/lockscreen_info`), inactivity autolock policy (`/autolock`), encrypted DNS (`/dns`), and shadow app vault (`/freeze`).
5. **🛡️ Autonomous Sensor Traps & Anti-Theft (`menu:traps_hub`):** Snatch acceleration trap, charger disconnect trap, pocket extraction trap, pattern guard failed PIN trap, thermal anomaly trap, anti-EDL dead man's switch, app isolation firewall, and launcher stealth mode.
6. **📇 Extraction, Telemetry & System Logs (`menu:data_hub`):** Address book contacts (`/contacts`), call log extraction (`/call_log`), SMS inbox recovery (`/sms_log`), outbound SMS dispatcher (`/sendsms`), remote outbound phone call (`/call`), storage file browser (`/list_files`, `/getfile`), and multi-pass cryptographic file shredder (`/shred`).

---

### Interactive Conversational Wizards
PASA Sentinel includes conversational state machine wizards (5-minute session lifetime). If you send a command without arguments or tap a wizard button, the bot guides you step-by-step:

* **Lockscreen Alert Wizard (`wizard:msg:custom`):** Prompts for custom text to broadcast across the display.
* **Custom Lock PIN Wizard (`wizard:lock:custom`):** Prompts for 4–8 digits to lock the device into Kiosk mode.
* **Decoy Duress PIN Wizard (`wizard:duress:set`):** Prompts for a 4–8 digit decoy PIN for armed robbery / checkpoint coercion defense.
* **Cryptographic File Shredder (`wizard:shred`):** Prompts for Master Password to authorize irreversible multi-pass sanitization of confidential folders.
* **Lockscreen Recovery Banner (`wizard:lockscreen_info`):** Prompts for owner recovery information to pin permanently to the native lockscreen.
* **Direct Outbound Cellular SMS (`wizard:sendsms`):** Prompts for `<number> <message>` to transmit an SMS from the stolen device.
* **Remote Outbound Phone Call (`wizard:call`):** Prompts for target phone number and dials on speakerphone.
* **Smart App Lockout (`wizard:lock_app` / `wizard:unlock_app`):** Prompts for target app (`gallery`, `phone`, `files`, or package name) to hide or restore.
* **Remote Storage File Downloader (`wizard:getfile`):** Prompts for index number (e.g. `1` from `/list_files`) or absolute path to download files up to 50MB.
* **Emergency SIM Alert Recipient (`wizard:sim_phone`):** Prompts for your secondary phone number to receive silent SMS alerts when a thief inserts an unauthorized SIM.
* **Dead Man's Switch Timeout (`wizard:deadman`):** Prompts for countdown hours (1–72) for offline auto-destruct.
* **Thermal Trap Anomaly Threshold (`wizard:thermal`):** Prompts for temperature threshold (40–65°C) to counter heat-gun disassembly.
* **Pro License Activation (`wizard:license:activate`):** Prompts for your purchased license key (`PASA-PRO-XXXX-XXXX`).

---

## 6. Complete Command Reference Matrix (All 94 Commands)

### Category 1: Core & Diagnostics

| Command | Syntax & Arguments | Description | Practical Example | Prerequisites |
|---|---|---|---|---|
| `/menu` | `/menu` | Opens the 6-hub interactive command console with live telemetry badges. | `/menu` | None |
| `/help` | `/help` | Returns the complete categorized command cheat sheet. | `/help` | None (Always exempt from lockout) |
| `/status` | `/status` | Comprehensive live telemetry: battery %, charging status, Wi-Fi/Cellular network, GPS link, storage/RAM usage, and crypto identity. | `/status` | None |
| `/selftest` | `/selftest` | 9-point hardware and software integrity audit: Camera, Mic, GNSS, KeyStore, Knox DO, Root/Tamper, Accessibility, Storage, Network. | `/selftest` | None |
| `/info` | `/info` | Hardware & OS details: manufacturer, model, SoC board, Android version, patch level, display resolution, IMEI/IDs, installed apps count. | `/info` | None |
| `/reboot` | `/reboot` | Remotely reboots the Android device hardware via DevicePolicyManager. | `/reboot` | Knox Device Owner |
| `/battery_alert` | `/battery_alert [enable\|disable\|threshold <5-50>\|status]` | Monitors battery health and alerts on abnormal background rapid drain while screen is off. | `/battery_alert threshold 15` | None |
| `/network` | `/network` | Inspects Wi-Fi SSID, BSSID, frequency band, link speed, cellular carrier, network type (LTE/5G), internal IP, and gateway. | `/network` | None |

---

### Category 2: Enterprise Knox Device Owner Suite

| Command | Syntax & Arguments | Description | Practical Example | Prerequisites |
|---|---|---|---|---|
| `/device_owner` | `/device_owner` | Audits Knox security policies, lock task state, restrictions, and escrow token enrollment. | `/device_owner` | Device Admin / Device Owner |
| `/antitamper` | `/antitamper [on\|off\|status]` | Enforces kernel restrictions: blocks Safe Boot, Airplane Mode, Factory Reset, Network Reset, USB OTG mounts, and MTP transfers. | `/antitamper on` | Knox Device Owner |
| `/usb_lock` | `/usb_lock [on\|off\|status]` | Hardware USB data pin killswitch (Android 12+). Physically cuts data pins to stop GrayKey/Cellebrite extraction while keeping AC charging active. | `/usb_lock on` | Knox Device Owner, Android 12+ |
| `/camera_lock` | `/camera_lock [on\|off\|status]` | Hardware camera killswitch globally disabling all front and rear cameras across the entire OS. | `/camera_lock on` | Device Admin / Device Owner |
| `/bluetooth_lock` | `/bluetooth_lock [on\|off\|status]` | Shuts down Bluetooth radio, disconnects accessories, and blocks file transfer/beacons. | `/bluetooth_lock on` | Knox Device Owner |
| `/mic_mute` | `/mic_mute [on\|off\|status]` | Mutes all system audio input and output at the hardware HAL level. | `/mic_mute on` | Knox Device Owner |
| `/lockscreen_info` | `/lockscreen_info [<text>\|clear\|status]` | Pins owner recovery contact information permanently onto the native lockscreen display. | `/lockscreen_info Call +1-555-0199 for reward!` | Knox Device Owner |
| `/autolock` | `/autolock [<seconds>\|default\|status]` | Enforces maximum screen inactivity timeout policy (5 to 3600 seconds), overriding user settings. | `/autolock 30` | Device Admin / Device Owner |
| `/wifi_connect` | `/wifi_connect <ssid> [password]` | Remotely connects device to Wi-Fi while locked, bypassing lockscreen UI. | `/wifi_connect OfficeSecure Pass123` | Location & Wi-Fi permissions |
| `/security_audit` | `/security_audit` | Dumps Linux kernel security logs: interactive ADB shell invocations, KeyStore tampering, media mounts. | `/security_audit` | Knox Device Owner, Android 7.0+ |
| `/notification` | `/notification [hide\|show\|toggle\|status]` | Revokes `POST_NOTIFICATIONS` to remove the persistent notification from drawer/lockscreen while daemon runs invisibly. | `/notification hide` | Knox Device Owner, Android 13+ |
| `/self_heal` | `/self_heal` | Re-asserts unrevokable permission sovereignty for Camera, Mic, GPS, SMS, Call Log, Contacts as "Managed by organization". | `/self_heal` | Knox Device Owner |
| `/freeze` | `/freeze <package\|alias>` | Conceals target app into shadow vault via `setApplicationHidden(true)`. Vanishes from launcher/search without data loss. | `/freeze binance` | Knox Device Owner |
| `/unfreeze` | `/unfreeze <package\|alias>` | Restores a hidden shadow vault application back to the launcher immediately. | `/unfreeze binance` | Knox Device Owner |
| `/frozen` | `/frozen` | Lists all installed applications currently hidden in the shadow vault. | `/frozen` | Knox Device Owner |
| `/lock_app` | `/lock_app <gallery\|phone\|files\|pkg>` | Smart app freeze targeting system utilities: automatically resolves aliases to OEM package IDs. | `/lock_app gallery` | Knox Device Owner |
| `/unlock_app` | `/unlock_app <gallery\|phone\|files\|pkg>` | Unfreezes and restores applications hidden via `/lock_app`. | `/unlock_app gallery` | Knox Device Owner |
| `/biometrics` | `/biometrics [on\|off\|status]` | Deactivates fingerprint and 3D face recognition on lockscreen to counter physical biometric coercion at checkpoints/robberies. | `/biometrics off` | Knox Device Owner |
| `/dns` | `/dns [quad9\|cloudflare\|adguard\|off\|<host>]` | Enforces tamper-proof DNS-over-TLS (DoT) system-wide, defeating ISP query logging and rogue Wi-Fi redirects. | `/dns quad9` | Knox Device Owner, Android 10+ |
| `/app_firewall` | `/app_firewall [enable\|disable\|block <pkg>\|blacklist\|whitelist]` | App network isolation firewall. Isolates unauthorized apps, RATs, or spyware from sending telemetry. | `/app_firewall block com.anydesk.anydeskandroid` | Knox Device Owner |
| `/usb_autolock` | `/usb_autolock [status\|enable\|disable\|delay <sec>]` | Locked-state USB killswitch: physically cuts data pins whenever screen is locked, neutralizing forensic cables (Cellebrite/GrayKey). | `/usb_autolock enable` | Knox Device Owner, Android 12+ |
| `/anti_2g` | `/anti_2g [status\|enable\|disable\|enforce_5g on\|off]` | Anti-2G / IMSI-Catcher Shield: disables insecure legacy 2G cellular connections, blocking stingray eavesdropping and fake base stations. | `/anti_2g enable` | Knox Device Owner, Android 12+ |

---

### Category 3: Location & Cellular RF Telemetry (8 Commands)

| Command | Syntax & Arguments | Description | Practical Example | Prerequisites |
|---|---|---|---|---|
| `/locate` | `/locate` | Forcibly turns on GNSS hardware receiver (Device Owner) and returns high-precision coordinates with Google Maps link. | `/locate` | Location permission |
| `/gps` | `/gps [on\|off\|force\|status]` | Direct remote hardware GPS chip power and GNSS satellite fix controller. | `/gps force` | Location permission, Device Owner |
| `/tower` | `/tower` | Scans cellular radios for LTE, 5G NR, and GSM base station identities (MCC, MNC, LAC/TAC, CID, dBm) for indoor triangulation. | `/tower` | Telephony permission |
| `/sim` | `/sim [slot]` | Dumps active SIM slots, carrier names, subscription IDs, signal strength levels, network types (2G/3G/4G/5G), and ICCID. | `/sim` | Read Phone State |
| `/sim_lock` | `/sim_lock [enable\|disable\|whitelist\|phone\|status]` | Armed SIM tray ejection guard. Locks device into Kiosk mode on SIM removal. Sends silent outbound SMS on foreign SIM insertion to expose thief's caller ID. | `/sim_lock phone +1234567890` | Device Owner, Send SMS |
| `/track` | `/track <minutes>` | Starts autonomous background GPS tracking beacon, sending updates to Telegram at specified minute intervals (e.g. every 2m). | `/track 2` | Background Location |
| `/track_stop` | `/track_stop` | Terminates active background GPS tracking beacon immediately. | `/track_stop` | None |
| `/geofence` | `/geofence [here <radius_m>\|on\|off\|status]` | Arms a radial geofence boundary (50m to 50km). Triggers emergency Telegram SOS if the device exits the perimeter. | `/geofence here 200` | Pro License, Location |

---

### Category 4: Covert Forensics & Surveillance

| Command | Syntax & Arguments | Description | Practical Example | Prerequisites |
|---|---|---|---|---|
| `/snap` | `/snap [front\|back\|both]` | Headless camera capture via CameraX bound to background service. Never flashes screen or UI. Directly streams photo to Telegram and shreds cache. | `/snap front` | Camera permission |
| `/screenshot` | `/screenshot` | Silent screenshot capture via Accessibility Service (`takeScreenshot`) without system sounds or permission popups. | `/screenshot` | Accessibility Service |
| `/screen_burst` | `/screen_burst [frames=5]` | Captures rapid sequence of screenshot frames over 10 seconds and compiles them into a single storyboard composite PNG grid. | `/screen_burst 6` | Accessibility Service |
| `/screenrecord` | `/screenrecord [seconds=15]` | Covert MP4 screen video recording (5 to 60s) via Device Owner shell or accessibility frame encoder. | `/screenrecord 30` | Pro License, Accessibility / DO |
| `/video` | `/video <password> [front\|back] [seconds=15]` | Headless covert video clip recording (1 to 60s) without screen activation. Requires Master Password authorization. | `/video MyPass123 front 15` | Master Password, Camera & Mic |
| `/record` | `/record <password> [seconds=30]` | Covert ambient microphone audio recording (PCM/AAC wiretap, 1 to 300s). Dispatches audio note directly to Telegram. | `/record MyPass123 60` | Master Password, Audio Record |
| `/livestream` | `/livestream <password> [front\|back] [min=5]` | Progressive surveillance video stream: captures sequential 5-second MP4 video segments sent continuously to Telegram. | `/livestream MyPass123 front 3` | Pro License, Camera & Mic |
| `/stopstream` | `/stopstream` | Terminates active progressive surveillance video stream immediately. | `/stopstream` | None |
| `/livestream_diag`| `/livestream_diag` | Diagnostic check of camera hardware, audio encoders, network uplink, and background execution limits. | `/livestream_diag` | None |
| `/clipboard` | `/clipboard` | Reads and extracts text currently stored in device clipboard RAM. | `/clipboard` | None |
| `/gallery_latest`| `/gallery_latest [count=3]` | Extracts the most recent media files taken on the device from Android MediaStore. | `/gallery_latest 5` | Storage / Media permission |
| `/getfile` | `/getfile <index\|filename\|path>` | Downloads any file from internal storage (up to 50MB) by directory index number (from `/list_files`) or absolute path. | `/getfile 1` | Storage permission |
| `/list_files` | `/list_files [path\|shortcut] [--all]` | Browses internal storage with numbered items for 1-tap download. Shortcuts: `camera`, `downloads`, `dcim`, `pictures`, `whatsapp`. | `/list_files downloads` | Storage permission |

---

### Category 5: Lockdown, Alert & Deception

| Command | Syntax & Arguments | Description | Practical Example | Prerequisites |
|---|---|---|---|---|
| `/lock` | `/lock [pin] [message]` | Locks screen into Knox Kiosk Lost Mode (`LOCK_TASK_FEATURE_NONE`), suppresses status bar, and pins recovery message. | `/lock 4819 Lost phone! Return.` | Device Admin / Device Owner |
| `/lock_message` | `/lock_message <text>` | Updates the on-screen alert banner text displayed over active Lost Mode screen. | `/lock_message Reward: $500! Call owner.` | Device Admin |
| `/set_os_pin` | `/set_os_pin <new_pin>` | Remotely resets physical Android OS lockscreen PIN/password via hardware cryptographic escrow tokens without data loss. | `/set_os_pin 5892` | Device Owner + Armed Escrow Token |
| `/set_master_pin`| `/set_master_pin <pin>` | Remotely rotates PASA Master Emergency PIN used for Kiosk bypass, SMS backdoor, and wipe authorization. | `/set_master_pin Alpha9182Pass` | None |
| `/unlock` | `/unlock` | Releases Lost Mode Kiosk overlay, restores status bar, re-enables biometrics, and clears lockout policies. | `/unlock` | Device Admin / Device Owner |
| `/fakeshutdown` | `/fakeshutdown` | Plays authentic power-off animation, then drops brightness to 0-nit black canvas with all hardware buttons suppressed. Screen taps snap mugshots. | `/fakeshutdown` | Pro License |
| `/wake` | `/wake` | Restores screen brightness and exits fake shutdown blackout mode. | `/wake` | None |
| `/ring` | `/ring [seconds=60]` | Sounds high-decibel emergency siren (5 to 300s) overriding silent and vibrate modes. | `/ring 30` | None |
| `/ring_stop` | `/ring_stop` | Immediately silences active emergency alarm siren. | `/ring_stop` | None |
| `/vibrate_pulse`| `/vibrate_pulse [pulse\|sos\|continuous\|stop]`| Tactile device locator for locating device covertly in hostile environments without triggering loud audio sirens. | `/vibrate_pulse sos` | None |
| `/message` | `/message <text>` | Displays fullscreen high-priority alert dialog over lockscreen with 1-tap call-owner button. | `/message Police tracking active!` | Draw Over Apps permission |
| `/escrow` | `/escrow [status\|test]` | Audits hardware cryptographic escrow token enrollment and keyguard readiness for remote PIN resets. | `/escrow status` | Knox Device Owner |
| `/sim_tray_lock` | `/sim_tray_lock [arm\|disarm\|release\|status]` | Armed SIM tray ejection defense: rotates lockscreen PIN to random 8-digit secret and suspends all user apps upon SIM tampering. | `/sim_tray_lock arm` | Knox Device Owner, Escrow Token |

---

### Category 6: Traps & Mobile Cyber Defense Suite (15 Commands)

| Command | Syntax & Arguments | Description | Practical Example | Prerequisites |
|---|---|---|---|---|
| `/duress_pin` | `/duress_pin <pin\|clear\|status>` | Sets 4–8 digit decoy coercion PIN. Unlocks to sterile home screen, hides banking/crypto into shadow vault, snaps mugshot, and sends SOS. | `/duress_pin 2580` | Pro License, Accessibility |
| `/a11y_shield` | `/a11y_shield [status\|enable\|disable\|scan\|strict on\|off]` | Real-time Accessibility Trojan Shield. Blocks unauthorized a11y hijacking and banking overlay malware. | `/a11y_shield enable` | Knox Device Owner / Accessibility |
| `/clipper_guard` | `/clipper_guard [status\|enable\|disable\|action alert\|poison\|sanitize]` | Crypto Clipper Trap: intercepts malicious clipboard replacements of cryptocurrency addresses (BTC, ETH, SOL, TON). | `/clipper_guard enable` | None |
| `/canary_guard` | `/canary_guard [status\|arm\|disarm\|action wipe\|lock\|alert]` | Ransomware Canary Tripwire Guard: plants covert decoy files and instantly reacts if modified or encrypted. | `/canary_guard arm` | Storage permission |
| `/app_install_lock` | `/app_install_lock [status\|enable\|disable\|allowed <pkgs>]` | Sideload & App Install Lockdown: blocks unauthorized APK package installations and sideloading. | `/app_install_lock enable` | Knox Device Owner |
| `/otp_guard` | `/otp_guard [status\|enable\|disable\|suppress on\|off\|vault on\|off]` | 2FA / OTP Interception Guard: protects 2FA SMS tokens from notification snooping, keyloggers, and spyware. | `/otp_guard enable` | SMS permission |
| `/pattern_guard`| `/pattern_guard [enable\|disable\|threshold <1-10>\|status]`| Monitors failed unlock attempts on OS keyguard. Once threshold (default 3) is exceeded, captures intruder photo and GPS fix. | `/pattern_guard threshold 3` | Device Admin |
| `/trap` | `/trap [on\|off\|status\|snatch\|charger\|pocket]` | Master controller for autonomous sensor traps: Snatch (>26 m/s²), Charger disconnect, Pocket extraction (5s grace). | `/trap snatch on` | Pro License |
| `/thermal` | `/thermal [on\|off\|threshold <40-65>\|status]` | Anti-EDL heat-gun anomaly trap. Detects battery heating (>48°C) from technician ungluing back glass. Severs USB data pins and snaps photo. | `/thermal threshold 48` | Knox Device Owner |
| `/deadman` | `/deadman [enable\|disable\|hours <1-72>\|heartbeat\|status]`| Anti-EDL auto-destruct timer. If phone is isolated in Faraday box/offline without heartbeat for configured hours, triggers irreversible wipe. | `/deadman hours 12` | Knox Device Owner |
| `/shred` | `/shred <password> <target>` | Irreversible multi-pass cryptographic file shredder. Overwrites files with 2-pass PRNG noise and zero-fill. Targets: `downloads`, `documents`, `camera`, `cache`. | `/shred MyPass123 downloads` | Pro License, Master Password |
| `/stealth` | `/stealth [hide\|show\|toggle\|status]` | Completely conceals or restores PASA application icon from Android home screen and app drawer. Background protection stays 100% active. | `/stealth hide` | None |
| `/tamper_detect`| `/tamper_detect [scan\|status]` | Scans for root binaries (`su`), Magisk, active debuggers, Xposed/Frida hooks, and APK signature tampering. | `/tamper_detect scan` | None |
| `/harden_boot` | `/harden_boot [lock\|unlock\|status]` | Enforces restrictions blocking OEM bootloader unlocking and USB debugging, neutralizing Fastboot attacks. | `/harden_boot lock` | Knox Device Owner |
| `/factory_reset_defense` | `/factory_reset_defense [on\|off\|status]` | Deep Knox hardware policy preventing master reset, recovery wipe, and fastboot flashing. | `/factory_reset_defense on` | Knox Device Owner |

---

### Category 7: Extraction & Telephony (6 Commands)

| Command | Syntax & Arguments | Description | Practical Example | Prerequisites |
|---|---|---|---|---|
| `/call` | `/call <phone_number> [speaker\|earpiece]` | Remotely places outbound phone call via cellular radio on speakerphone for room listening. | `/call +1234567890 speaker` | Call Phone permission |
| `/contacts` | `/contacts [search_query]` | Extracts device address book contacts matching query with phone numbers. | `/contacts John` | Read Contacts |
| `/call_log` | `/call_log [count=15]` | Extracts recent incoming, outgoing, and missed cellular call logs with caller IDs and timestamps. | `/call_log 25` | Read Call Log |
| `/sms_log` | `/sms_log [count=15]` | Extracts recent incoming and outgoing SMS text messages and 2FA authentication codes from device inbox. | `/sms_log 10` | Read SMS |
| `/sendsms` | `/sendsms [sim1\|sim2] <number> <message>` | Transmits an outbound SMS message directly via cellular radio (reveals unknown SIM phone number via caller ID). | `/sendsms +1234567890 Test SMS` | Send SMS permission |
| `/history` | `/history [count=20]` | Returns cryptographic audit trail of recent C2 command executions from local encrypted database. | `/history 30` | None |

---

### Category 8: System, Maintenance & Updates

| Command | Syntax & Arguments | Description | Practical Example | Prerequisites |
|---|---|---|---|---|
| `/apps` | `/apps [search_query]` | Dumps installed applications, package names, version codes, and install dates. | `/apps banking` | None |
| `/app_uninstall`| `/app_uninstall <package_name>` | Silently uninstalls specified app via Device Owner PackageInstaller without confirmation prompts. | `/app_uninstall com.spyware.tracker` | Knox Device Owner |
| `/smssetup` | `/smssetup` | Generates rotating 6-digit TOTP secret (RFC 6238) and QR code for air-gapped cellular SMS authentication. | `/smssetup` | None |
| `/sms_help` | `/sms_help` | Returns air-gapped cellular SMS command cheatsheet with 1-tap copyable monospace templates. | `/sms_help` | None |
| `/license` | `/license [activate <key>\|buy\|status]` | Audits current licensing status or activates Ed25519-signed offline license certificate key (`PASA-PRO-XXXX-XXXX`). | `/license activate PASA-PRO-89F2-K102` | None (Exempt from lockout) |
| `/check_update` | `/check_update` | Checks VPS repository for signed OTA APK releases. Shows changelog, file size, and SHA-256 hash. | `/check_update` | None |
| `/update_confirm`| `/update_confirm` | Downloads and performs unattended silent background installation of latest signed OTA APK update. | `/update_confirm` | Knox Device Owner |
| `/wipe` | `/wipe` | Initiates 2-step authenticated remote factory reset challenge with master password authorization. | `/wipe` | Device Admin / Device Owner |
| `/wipe_confirm` | `/wipe_confirm <master_password>` | Validates master password and executes irreversible cryptographic device erasure (`dpm.wipeData(0)`). | `/wipe_confirm MyMasterPass123` | Device Owner, Master Password |

---

## 7. Autonomous Sensor Traps & Edge Defenses

Autonomous traps operate entirely on-device, reacting within milliseconds without needing an active internet connection.

```
┌────────────────────────────────────────────────────────────────────────┐
│                   AUTONOMOUS SENSOR TRAP PIPELINE                      │
├────────────────────┬───────────────────────────────────────────────────┤
│ Trap Trigger       │ Automated System Countermeasures                  │
├────────────────────┼───────────────────────────────────────────────────┤
│ 🏃 Kinetic Snatch  │ Immediate Kiosk Lock + Mugshot + Sat GPS fix      │
│ 🔌 Unplug Charger  │ Front Mugshot + GPS fix + Telegram SOS            │
│ 👜 Pocket Extract  │ 5s Grace Period -> If locked: Kiosk Lock + Photo  │
│ 💳 SIM Tray Eject  │ Knox Lockdown + GPS On + Biometrics Disabled      │
│ 📱 Foreign SIM In  │ Silent Outbound SMS -> Exposes Thief's Caller ID  │
│ 🔢 Failed PIN x3   │ Front Mugshot + GPS Telemetry to Telegram         │
│ 🔥 Heat Gun >48°C  │ Sever USB Data Pins + Kiosk Lock + Alert          │
│ ⏳ Faraday Box     │ Dead Man's Countdown Reaches 0 -> Crypto Auto-Wipe│
└────────────────────┴───────────────────────────────────────────────────┘
```

### Snatch-and-Run Trap (`/trap snatch`)
* **Detection:** Accelerometer vector magnitude $\sqrt{x^2 + y^2 + z^2} > 26.0\text{ m/s}^2\ (\approx 2.65\text{G})$.
* **Reaction:** Instantly locks keyguard, pins `AlertMessageActivity` in Kiosk Lost Mode (`LOCK_TASK_FEATURE_NONE`), snaps front-camera mugshot of runner, grabs GNSS location, transmits Telegram alert, and shreds local photo.
* **Control:** Enable with `/trap snatch on`, disable with `/trap snatch off`.

### Charger Disconnect & USB Insertion Trap (`/trap charger`)
* **Detection:** Unplugging AC/USB power **while the screen is locked**. (Normal unplugging while unlocked triggers no alert).
* **Reaction:** Captures front mugshot + GPS fix and sends Telegram alert with quick action buttons.
* **Locked USB Insertion Countermeasure:** If a USB cable is plugged in while locked (potential forensic extraction / juice-jacking), PASA automatically **severs the hardware USB data pins** (`setUsbDataSignaling(false)`), snaps an intruder mugshot, and alerts Telegram.
* **Control:** `/trap charger on` / `/trap charger off`.

### Pocket & Bag Extraction Trap (`/trap pocket`)
* **Detection:** Proximity sensor transitions from covered to uncovered while the phone is locked.
* **5-Second Grace Period:** If the legitimate owner unlocks the phone within 5 seconds, the trap cancels silently. If the device remains locked after 5 seconds, theft is confirmed.
* **Reaction:** Engages Kiosk lock, snaps perpetrator mugshot, and sends GPS pin to Telegram.
* **Control:** `/trap pocket on` / `/trap pocket off`.

### SIM Ejection & Foreign SIM Auto-SMS Trap (`/sim_lock`)
* **SIM Tray Ejected:** Immediately triggers Knox Kiosk Lost Mode, disables status bar, forcibly powers on GNSS hardware receiver, disables biometric sensors, captures 3 mugshots, and broadcasts Telegram SOS.
* **Foreign SIM Inserted:** Silently transmits an outbound emergency SMS via `SmsManager` to your configured emergency phone number (`/sim_lock phone <number>`). The SMS contains device IMEI, carrier name, and GPS coordinates—**instantly exposing the thief's cellular phone number on your caller ID!**
* **Control:** `/sim_lock enable`, `/sim_lock phone +1234567890`, `/sim_lock alert_action lock|alert|wipe`.

### Pattern Guard / Failed Lockscreen Trap (`/pattern_guard`)
* **Detection:** Monitors consecutive failed PIN/pattern attempts on the native lockscreen via `onPasswordFailed`.
* **Reaction:** Once the threshold (default 3 failed attempts) is reached, silently snaps a front-camera intruder photo and sends coordinates to Telegram. Entering the correct PIN resets the counter to zero.
* **Control:** `/pattern_guard enable`, `/pattern_guard threshold 3`, `/pattern_guard action photo|video|lock`.

### Thermal Anomaly Heat-Gun Trap (`/thermal`)
* **Detection:** Monitors battery temperature while locked. Technicians and forensic labs use hot-air heat guns (45–70°C) to soften adhesive and remove the back cover to access Qualcomm 9008 EDL or MediaTek BROM test points.
* **Reaction:** When temperature exceeds threshold (default 48°C), PASA **physically severs USB data pins**, reinforces Kiosk lock, snaps a mugshot, and sends an urgent Telegram heat anomaly SOS.
* **Control:** `/thermal on`, `/thermal threshold 48`, `/thermal status`.

### Dead Man's Switch / Faraday Isolation Auto-Destruct (`/deadman`)
* **Detection:** Thieves frequently place stolen phones inside Faraday RF-shielded bags or metal tins where Telegram/SMS C2 cannot reach.
* **Countdown:** Evaluates state every 5 minutes while locked. Countdown resets automatically on legitimate owner unlock, valid Telegram command, or valid SMS command.
* **Reaction:** If the timer reaches 0 hours (configurable 1–72 hours) without a heartbeat, PASA autonomously initiates irreversible cryptographic hardware erasure: **`dpm.wipeData(0)`**, destroying all encryption keys and user data before attackers can execute chip-off forensics.
* **Control:** `/deadman enable`, `/deadman hours 12`, `/deadman heartbeat`.

---

## 8. High-Security Deception & Anti-Coercion Features

### Duress PIN & Sterile Sandbox Decoy OS (`/duress_pin`)
Designed for armed robberies, violent coercion, or border checkpoint inspections where an attacker forces you to unlock your phone at gunpoint.
1. **Setup:** Set a 4–8 digit decoy PIN: `/duress_pin 2580`.
2. **Coercion Execution:** When forced, enter `2580` on the physical lockscreen keypad.
3. **Smooth Decoy Unlock:** The phone dismisses the lockscreen with a simulated swipe gesture and opens the Android Home screen—appearing completely normal to the attacker.
4. **Sterile Sandbox Decoy OS:** In that same split-second, Device Owner hides all sensitive applications from the system:
   - Crypto wallets (Binance, Coinbase, Trust Wallet, MetaMask).
   - Private messengers (Signal, WhatsApp, Telegram).
   - Mobile banking (bKash, Nagad, bank apps).
   - Custom frozen vault packages.
   The phone looks like a mundane device with zero financial or private data!
5. **Silent Emergency SOS:** Covertly captures a front mugshot of the coercer, grabs GPS fix, broadcasts a high-priority duress beacon to Telegram, and activates live tracking (`/track 2`).

### Fake Shutdown & Tactile Touch Sensor Canvas (`/fakeshutdown`, `/wake`)
* **Authentic Power-Down Simulation:** Displays an authentic 2.2-second OEM spinning power-off dialog.
* **Total Blackout Canvas:** Drops screen brightness to 0.001-nit (pitch black), suppresses status/navigation bars, and completely consumes all hardware keys (Power, Volume Up, Volume Down). The phone appears 100% dead.
* **Tactile Sensor Trap:** The dark screen remains active as an invisible touch sensor. Any physical tap, swipe, or press on the black screen silently snaps a front-camera photo of the handler and dispatches coordinates to Telegram.
* **Wake:** Send `/wake` via Telegram, send `PASA <PIN> /wake` via SMS, or tap 4 times rapidly in the top-right corner of the black screen within 3 seconds.

### Stealth Mode & App Drawer Concealment (`/stealth`, `/hide`, `/show`)
* **Concealment:** `/hide` completely strips the PASA Sentinel launcher icon from the home screen, app drawer, and system search.
* **Persistence:** All background services, traps, Device Owner rules, and Telegram polling remain 100% active.
* **Restoration:** Send `/show` via Telegram or `PASA <PIN> /show` via SMS to restore the icon instantly.

### Shadow App Vault Freezing (`/freeze`, `/unfreeze`, `/frozen`)
* Hides banking, crypto, or messaging apps on demand via `setApplicationHidden(true)`.
* Apps completely vanish from the launcher, application settings, and process table without deleting accounts or local data.
* Restore anytime with `/unfreeze <app_name>`.

### App Network Isolation Firewall (`/app_firewall`)
* Granularly isolates applications from network access.
* Preloads known remote control tools (TeamViewer, AnyDesk, Chrome Remote Desktop) to prevent malicious hackers from accessing your phone remotely.
* Toggle between Blacklist mode and Whitelist-only mode.

### System-Wide Encrypted DNS-over-TLS (`/dns`)
* Enforces tamper-proof DNS-over-TLS across all cellular and Wi-Fi networks (`dns.quad9.net`, `one.one.one.one`, or `dns.adguard-dns.com`).
* Completely blocks cellular carrier tracking, ISP query logging, and captive portal MITM attacks.

### Biometric Coercion Killswitch (`/biometrics`)
* Instantly disables fingerprint and 3D face recognition on the lockscreen (`/biometrics off`).
* Attackers cannot force your finger onto the sensor or hold the phone to your face while sleeping or under duress.

---

## 9. Air-Gapped Cellular SMS Fallback Protocol

When your phone has no internet connection, mobile data is toggled off, or you are roaming abroad, the cellular SMS fallback provides complete out-of-band control.

### Command Protocol Syntax
Send an SMS from any mobile phone to the target device's SIM number:
```
PASA <credential> <command> [arguments...]
```
* `<credential>` can be either:
  1. Your current **6-digit TOTP code** from Google Authenticator / Bitwarden (tolerance: ±90s), OR
  2. Your **Master Password / Master PIN** (ensuring you are never locked out).

### Dual-SIM Dynamic Return Routing
`SmsCommandReceiver` inspects the receiving `subscriptionId`. All response SMS messages are dispatched via `SmsManager.getSmsManagerForSubscriptionId(subId)` to return replies from the exact SIM card that received the command.

### Ready-to-Use 1-Tap Monospace SMS Templates
Copy and paste these templates directly into your SMS messaging app (replace `123456` with your TOTP code or Master PIN):

```text
PASA 123456 /locate
PASA 123456 /status
PASA 123456 /ring 30
PASA 123456 /lock 5892
PASA 123456 /unlock
PASA 123456 /usb_lock on
PASA 123456 /camera_lock on
PASA 123456 /fakeshutdown
PASA 123456 /wake
PASA 123456 /deadman enable
PASA 123456 /wifi_connect HomeNetwork Pass123
PASA 123456 /lockscreen_info Call +1-555-0199
PASA 123456 /reboot
PASA 123456 /app_uninstall com.spyware.app
PASA 123456 /help
```

---

## 10. Licensing, OTA Updates, Warranty & Emergency Contacts

### Licensing Tiers & Hard Lockout Policy
PASA Sentinel operates on an offline sovereign ownership model with zero recurring monthly subscription traps:

| Feature / Tier | Tactical Evaluation (Free Trial) | Pro Lifetime Shield ($25 / ৳3,000) | Enterprise Fleet ($99 / ৳12,000) |
|---|---|---|---|
| **Validity** | 7 Days | 100 Years (Lifetime) | 100 Years (Lifetime) |
| **Device Allowance** | 1 Device | 1 Device (up to 3 supported) | 5 Devices (up to 10 supported) |
| **All 94 C2 Commands** | ✅ Full Access | ✅ Full Access | ✅ Full Access |
| **Knox Device Owner Suite** | ✅ Full Access | ✅ Full Access | ✅ Full Access |
| **Hardware Escrow Token PIN Reset** | ✅ Full Access | ✅ Full Access | ✅ Full Access |
| **Offline Ed25519 Certificate** | ❌ None | ✅ Permanent Offline Cryptography | ✅ Permanent Offline Cryptography |
| **Dedicated Server Relay Node** | ❌ Public Gateway | ❌ Public Gateway | ✅ Private Isolated Server Node |
| **Personal Remote Setup Concierge** | ❌ Self-Service | ✅ 1-on-1 Remote Setup Guidance | ✅ VIP Dedicated WhatsApp Hotline |

> **⚠️ Strict 7-Day Trial Hard Lockout Policy:**  
> During the 7-day trial, a persistent red countdown banner is shown in Telegram. After 7 days, **all security commands are strictly locked**. Only `/license`, `/help`, and `/info` remain operational until a lifetime key is activated.

### Automated Silent OTA Updates (`/check_update`, `/update_confirm`)
* Check for updates: Send `/check_update` in Telegram.
* When an update is published, send `/update_confirm`.
* **Silent Background Installation:** On Device Owner devices, PASA Sentinel downloads the signed APK, verifies its SHA-256 hash, and commits the package install session silently in the background without user prompts. An exact alarm watchdog restarts the background service immediately after update!

### Payment Methods & 7-Day Money-Back Guarantee
* **Binance Pay (Crypto):** UID `756303714` (Nickname: `RBR48`). Pay in USDT and submit TxID via web checkout or bot.
* **bKash (Bangladesh Local):** Personal account `01737-910040` or official account requested via WhatsApp.
* **7-Day 100% Money-Back Guarantee:** If you are not completely satisfied, request a refund within 7 days of purchase via WhatsApp or Telegram for an unconditional 100% refund.

### Emergency Operational Cheatsheet
* **Official Website:** `https://pasa.izhaanintellect.fun`
* **Direct APK Download:** `https://pasa.izhaanintellect.fun/releases/pasa-latest.apk`
* **Windows Setup Kit:** `https://pasa.izhaanintellect.fun/releases/PASA-Device-Owner-Setup-Kit.zip`
* **WhatsApp VIP Concierge:** `+880 1762 033445`
* **Telegram Support Bot:** `@pasa_sentinel_bot`

---
*PASA Sentinel — Sovereign Mobile Defense Architecture. Built by Izhaan Intellect.*


