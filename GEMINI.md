# 🛡️ PASA Sentinel (Private Android Security Agent) — System Knowledge & Architecture

This document serves as the permanent memory, architectural specification, and operational guideline for the PASA Sentinel codebase. Every agent working in this workspace must adhere to the principles, patterns, and constraints documented herein.

---

## 1. Project Overview & Core Philosophy

**PASA Sentinel** is a sovereign mobile defense and covert anti-theft system for Android devices (Android 8.0 through Android 16 / API 26–36).
* **Zero Google Play Dependencies:** Operates without Google Play Services, Firebase, or Google Find My Device.
* **Direct-to-Telegram Zero-Storage Architecture (Strategy 1):** Zero photos, zero GPS tracks, and zero audio recordings are stored on VPS disk or cloud databases. Evidence is transmitted directly to the user's private Telegram bot and immediately memory-shredded on the phone.
* **Knox-Grade Device Owner Permanence:** Configured via `adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin`. Grants irreversible anti-uninstall protection, hardware lockscreen reset via escrow tokens, status bar/notification shade lockout, airplane mode prevention, and kiosk lockdown.
* **Dual-Channel C2:** Primary channel via Telegram Bot API (direct polling or VPS gateway); secondary air-gapped cellular SMS fallback authenticated via TOTP (RFC 6238).
* **Cryptographic Security Layer (ASTRA):** Hardware-backed Android Keystore / StrongBox keys (P-256 ECDSA), server-side Ed25519 command signing, and offline Ed25519 license verification.

---

## 2. Directory Structure & Component Map

```
e:/Projects/PrivateApp/
├── app/                                 # Native Android Application (Kotlin, Gradle 8.7, AGP 8.5.1)
│   ├── build.gradle.kts                 # compileSdk=36, minSdk=26, targetSdk=36, Hilt, Room, CameraX, Tink
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml      # Permissions, receivers, foreground service declarations, file providers
│       │   ├── java/com/izhaanintellect/pasa/
│       │   │   ├── PasaApp.kt           # App entrypoint, Hilt init, notification channels, watchdog restart
│       │   │   ├── accessibility/       # A11y screenshot capture & Duress PIN lockscreen keypad interceptor
│       │   │   ├── admin/               # DeviceAdminReceiver, escrow password tokens, kiosk mode, reboot
│       │   │   ├── audio/               # AudioRecorderManager (PCM/AAC ambient wiretaps)
│       │   │   ├── bot/                 # TelegramApi (Retrofit), CommandParser, CommandExecutor
│       │   │   ├── camera/              # CameraX headless manager, stealth video, screenshot & burst capture
│       │   │   ├── commands/            # 46 modular command implementations extending Command base class
│       │   │   ├── crypto/              # DeviceIdentity (StrongBox/TEE), CommandVerifier (Ed25519), ReplayStore
│       │   │   ├── data/                # EncryptedSharedPreferences, Room database (SQLCipher)
│       │   │   ├── detection/           # Autonomous traps: Snatch (accelerometer), Charger, SIM change, SMS
│       │   │   ├── di/                  # Hilt modules (AppModule, DatabaseModule, NetworkModule)
│       │   │   ├── location/            # FusedLocation + GNSS hardware tracker, geofencing
│       │   │   ├── network/             # PasaBackendApi (Retrofit), OkHttp clients
│       │   │   ├── security/            # Anti-tamper, DuressManager, Escrow Token activation
│       │   │   ├── service/             # PasaService persistent foreground daemon with dynamic FGS elevation
│       │   │   ├── ui/                  # SetupActivity, FakeShutdownActivity, AlertMessageActivity, Escrow
│       │   │   ├── update/              # OtaUpdateManager (self-downloading signed APK installer)
│       │   │   └── util/                # System utilities, SecurityActivityLauncher, permissions
│       │   └── res/                     # Layouts, themes, drawables, accessibility_config, device_admin.xml
├── pasa-server/                         # VPS Control Plane & Telegram Gateway (Node.js 22, Express)
│   ├── server.js                        # Master server: HTTP C2 long-polling, Telegram Webhook, Web Admin Console
│   ├── lib/
│   │   ├── db.js                        # SQLite WAL control plane (node:sqlite DatabaseSync)
│   │   ├── licensing.js                 # Ed25519 license signer, tiers (Trial, Pro, Enterprise), Binance/bKash
│   │   ├── rateLimit.js                 # Sliding-window anti-brute-force rate limiter
│   │   └── storage.js                   # JSON fallback persistence helper
│   ├── landingPage.js                   # Serves commercial landing page
│   ├── update_releases_v*.js            # Release publishing scripts
│   └── releases/                        # Hosted signed APKs for OTA distribution
├── pasa-commercial-web/                 # Commercial Marketing & Pricing Frontend
│   └── public/assets/                   # CSS, JS, Branding assets
├── releases/                            # Git-tracked release binaries (e.g. pasa-latest.apk)
├── PRIVACY.md                           # Sovereign Zero-Telemetry & Zero-Storage Guarantee
├── TERMS.md                             # Legal Terms of Service & EULA
├── keystore.properties                  # Keystore signing credentials
└── pasa-release-key.jks                 # Production release signing key
```

---

## 3. Key Subsystems & Technical Details

### 3.1 Direct-to-Telegram Zero-Storage Architecture (Strategy 1)
* **Surveillance Media:** Photos taken by `/snap`, videos by `/video`, screenshots by `/screenshot`, and recordings by `/record` are streamed directly to Telegram API (`sendPhoto`, `sendVideo`, `sendVoice`, `sendDocument`).
* **Instant Shredding:** As soon as transmission completes (or fails), local temporary files on the device are cryptographically overwritten and deleted (`photoFile.delete()`).
* **Server Ephemeral Memory:** In `server.js`, `multer.memoryStorage()` is strictly enforced. Media buffers live exclusively in RAM during relay and never touch physical VPS disk.
* **Zero Location Logs:** The VPS database never records GPS tracks. Location pins are forwarded straight to Telegram.

### 3.2 Enterprise Device Owner & Hardware Defense Suite (Android 8.0 – 16)
* Android 14+ removed `resetPassword()`. PASA uses cryptographic hardware escrow tokens:
  1. Generates 32-byte secure random token stored in `EncryptedSharedPreferences`.
  2. Enrolls token via `dpm.setResetPasswordToken(adminComponent, tokenBytes)`.
  3. Keyguard arms token on first physical device unlock.
  4. Remote reset executed via `dpm.resetPasswordWithToken(adminComponent, newPin, tokenBytes, 0)`.
* **Hardware Camera Killswitch (`/camera_lock`):** `dpm.setCameraDisabled(component, true)` completely disables all front and rear cameras system-wide, neutralizing malware or unauthorized camera access.
* **Peripheral Lockout (`/bluetooth_lock`, `/mic_mute`):** `dpm.addUserRestriction(UserManager.DISALLOW_BLUETOOTH)` blocks Bluetooth pairings and file sharing. `dpm.setMasterVolumeMuted(component, true)` mutes all audio at the hardware HAL level.
* **Lockscreen Emergency Banner (`/lockscreen_info`):** Android 7.0+ `dpm.setDeviceOwnerLockScreenInfo(component, text)` pins owner recovery contact information permanently onto the OS lockscreen display.
* **Inactivity Autolock Policy (`/autolock`):** `dpm.setMaximumTimeToLock(component, ms)` enforces custom maximum screen inactivity timeouts before locking.
* **Emergency Wi-Fi Auto-Provisioning (`/wifi_connect`):** Android 10+ `WifiNetworkSpecifier` and legacy `WifiConfiguration` provision and connect the device to Wi-Fi while locked.
* **Kernel OS Security Auditing (`/security_audit`):** Low-level Linux kernel `SecurityLog` events (`dpm.retrieveSecurityLogs`) report interactive ADB shell openings, KeyStore tampering/destruction, and physical media mounts.
* **Silent Application Uninstallation (`/app_uninstall`):** Device Owner `PackageInstaller.uninstall(pkg, pendingIntent)` silently removes unauthorized apps or spyware without showing confirmation prompts.
* **Hardware USB Data Pin Killswitch (`/usb_lock`):** Android 12+ (API 31+) `dpm.setUsbDataSignalingEnabled(false)` physically disables USB data pins. Forensic extraction boxes (Cellebrite, GrayKey), BadUSB, and juice-jacking attacks are neutralized while preserving AC power charging.
* **Enterprise Anti-Tamper Suite (`/antitamper`):** Enforces kernel/OS restrictions: `DISALLOW_SAFE_BOOT` (blocks safe mode), `DISALLOW_AIRPLANE_MODE`, `DISALLOW_FACTORY_RESET`, `DISALLOW_NETWORK_RESET`, `DISALLOW_MOUNT_PHYSICAL_MEDIA`, `DISALLOW_USB_FILE_TRANSFER`, `DISALLOW_CONFIG_LOCATION`.
* **Self-Healing Permission Sovereignty (`/self_heal`):** Uses `dpm.setPermissionGrantState(..., PERMISSION_GRANT_STATE_GRANTED)` to permanently lock Camera, Microphone, GPS, SMS, Call Log, and Contacts permissions as "Managed by your organization" (unrevokable by user or thief). Auto-enforced on app startup.
* **Shadow App Vault (`/freeze`, `/unfreeze`, `/frozen`):** `dpm.setApplicationHidden` completely conceals banking, crypto, and private messenger apps from launcher, app drawer, and system process table without data loss.
* **Biometric Coercion Killswitch (`/biometrics`):** `dpm.setKeyguardDisabledFeatures` deactivates fingerprint and 3D face recognition on the lockscreen, forcing complex Master Passphrase authentication during robberies or checkpoint duress.
* **System-Wide Encrypted DNS Enforcement (`/dns`):** Android 10+ (API 29+) `dpm.setGlobalPrivateDnsModeSpecifiedHost` enforces tamper-proof DNS-over-TLS (DoT) across all Wi-Fi and cellular networks (Quad9, Cloudflare, AdGuard, or custom host), defeating ISP snooping and rogue captive portals.
* **Remote Hardware GPS Enforcement:** `dpm.setLocationEnabled(adminComponent, true)` forcibly turns on the GNSS chip whenever location is requested.
* **OS Security Event Auditing (`SecurityLog`):** Hooks kernel security logs (`onSecurityLogsAvailable`) to alert on unauthorized ADB shell connections, KeyStore tampering, and media mount operations.
* **Kiosk Lockdown:** `dpm.setLockTaskPackages(adminComponent, [packageName])` and `dpm.setLockTaskFeatures(adminComponent, LOCK_TASK_FEATURE_NONE)`.
* **SystemUI Lockout:** `dpm.setStatusBarDisabled(adminComponent, true)` blocks pulling down Quick Settings to prevent toggling Airplane Mode or Wi-Fi.
* **Permanent Notification Tray Suppression (`/notification`):** On Android 13+ (API 33+), Device Owner enforces `dpm.setPermissionGrantState(..., Manifest.permission.POST_NOTIFICATIONS, PERMISSION_GRANT_STATE_DENIED)` by default. Completely removes the ongoing "System Security Core" notification from the drawer and lockscreen forever while keeping `PasaService` fully active. Remotely toggleable via `/notification hide|show|toggle|status`.
* **App Network Isolation Firewall (`/app_firewall`):** Granular package network isolation (blacklist / whitelist mode) preventing RATs, malware, or compromised apps from outbound telemetry or command reception. Remotely toggleable via `/app_firewall [status|enable|disable|blacklist|whitelist|block|unblock]`.

### 3.3 Covert Forensics & Headless Sensor Architecture
* **Zero-Blackout Headless Camera & Video (`/snap`, `/video`):** Operates purely headlessly via `StealthCameraManager` and `StealthVideoManager` using CameraX bound to a `ServiceLifecycleOwner`. Dynamically elevates `PasaService` to `FOREGROUND_SERVICE_TYPE_CAMERA` and `FOREGROUND_SERVICE_TYPE_MICROPHONE`. Bypasses `StealthCaptureActivity` overlay during standard capture, completely eliminating display dimming, UI flicker, or black screens.
* **AccessibilityScreenCaptureService (`AccessibilityService`):**
  - Captures non-intrusive screenshots via `takeScreenshot(Display.DEFAULT_DISPLAY, ...)` on Android 11+ (API 30+) without permission popups.
  - Monitors `TYPE_VIEW_CLICKED` on lockscreen/SystemUI keyboards to detect **Decoy Duress PIN**.
  - When Duress PIN is matched:
    1. Unlocks device via `duressMgr.executeDuressUnlock()`.
    2. **Sterile Sandbox Decoy OS:** Instantly vanishes banking, crypto, and private messengers via `dpm.setApplicationHidden` to leave a sterile decoy environment for the coercer.
    3. Performs simulated swipe-up gesture (`dispatchGesture`) to dismiss lockscreen.
    4. Issues `GLOBAL_ACTION_HOME`.
    5. Silently captures front-camera mugshot, sat GPS fix, and broadcasts emergency Telegram SOS.
  - In Lost Mode, automatically dismisses unauthorized Notification Shade access.

### 3.4 Autonomous Sensor Traps & Physical Anti-Theft
* **Kinetic Snatch Detection (`TrapManager`):** Continuous accelerometer vector magnitude check `sqrt(x² + y² + z²) > 26.0 m/s² (~2.65G)`. Triggers immediate device lock, Kiosk Lost Mode guard, perpetrator selfie, and Telegram alert.
* **Pocket & Bag Extraction Trap (`/trap pocket on`):** Monitors proximity sensor transitions from covered (in pocket) to uncovered while locked. If device is not unlocked within 5 seconds grace period, automatically engages Kiosk lock, snaps front camera mugshot, and alerts owner.
* **Physical SIM Ejection Knox Kiosk Lockdown & Auto Outbound SMS (`/sim_lock`):** `SIMChangeReceiver` detects SIM tray eject (`ABSENT`) or unauthorized foreign SIM insertion (`LOADED`). Upon SIM removal, immediately engages Knox Kiosk Lost Mode (`configureLockTask`), sets comprehensive device lockdown, disables status bar/quick settings, locks keyguard, powers on GNSS hardware, captures perpetrator mugshot, and alerts Telegram. Upon unauthorized foreign SIM insertion, silently transmits an outbound emergency SMS via `SmsManager` to the owner's emergency contact phone (`/sim_lock phone <number>`) containing device IMEI, carrier, and Google Maps GPS fix—**instantly exposing the thief's phone number via caller ID**. Configurable alert actions include `/sim_lock [enable|disable|whitelist|alert_action|phone|status]`.
* **Battery Health & Rapid Drain Alert (`/battery_alert`):** Proactive battery telemetry alerting owner on critical low levels, abnormal rapid drain (detecting background surveillance/tethers), or unauthorized charger disconnection (`/battery_alert [status|enable|disable|threshold]`).
* **Dual-SIM Cell Tower Triangulation (`/tower`):** Scans LTE/5G NR/GSM cell identities (MCC, MNC, LAC/TAC, CID, dBm) across all active subscriptions for resilient indoor localization without satellite reception.

### 3.5 Deception & Covert Tactile Feedback
* **Fake Shutdown Deception (`FakeShutdownActivity`):**
  - Simulates authentic OEM power-down animation, then drops brightness to 0-nit black canvas with `WindowManager.LayoutParams.FLAG_FULLSCREEN` and `SYSTEM_UI_FLAG_IMMERSIVE_STICKY`.
  - Phone appears completely dead. Screen taps silently trigger front camera mugshots and GPS beacons.
  - Dismissed remotely via `/wake` command or secret multi-tap sequence.
* **Covert Tactile Device Locator (`/vibrate_pulse`):** Custom vibration sequences (intermittent pulse, SOS Morse code `...---...`, continuous) for locating device covertly without loud audio sirens alerting thieves in hostile environments (`/vibrate_pulse [pulse|sos|continuous|stop]`).

### 3.6 Air-Gapped Cellular SMS Fallback (`SmsCommandReceiver`)
* Listens on `SMS_RECEIVED` (priority 999) with direct boot awareness.
* Syntax: `PASA <6-digit-TOTP-or-MasterPIN> <command>` (e.g. `PASA 419582 /locate` or `PASA 419582 /status`).
* Validates TOTP code against hardware clock using enrolled secret key (RFC 6238, window tolerance ±3 steps) or master cryptographic passphrase.
* **Dual-SIM Routing:** Dynamically extracts `subscriptionId` from incoming SMS and dispatches responses via the receiving SIM's `SmsManager`.
* **Direct Outbound Cellular SMS (`/sendsms`):** Transmits SMS messages directly via cellular radio (`/sendsms [sim1|sim2] <number> <msg>`). Used to verify unknown device phone numbers via caller ID when carriers do not store the MSISDN on the SIM card chip.
* **SIM & Cellular Carrier Telemetry (`/sim`):** Displays active SIM slots, carrier names, subscription IDs, signal strength levels, network types (2G/3G/4G/5G), and MCC/MNC codes.
* **Air-Gapped SMS Fallback Guide (`/sms_help`):** Comprehensive on-device and Telegram offline cheatsheet with 1-tap copyable monospace templates (`<code>PASA <PIN> /locate</code>`, etc.) and step-by-step TOTP enrollment guidance (`/smssetup`).
* **Complete Air-Gapped Command Coverage:** Every single command responds via SMS: `/locate`, `/status`, `/usb_lock`, `/camera_lock`, `/bluetooth_lock`, `/mic_mute`, `/wifi_connect`, `/lockscreen_info`, `/autolock`, `/app_uninstall`, `/reboot`, `/security_audit`, `/antitamper`, `/biometrics`, `/ring`, `/ring_stop`, `/lock`, `/unlock`, `/fakeshutdown`, `/wake`, `/set_master_pin <pin>`, `/wipe`, `/wipe_confirm`, `/sms_help`. All SMS responses are stripped of HTML tags for clean SMS delivery.

### 3.7 Commercial Licensing & Cryptography
* **Ed25519 Offline Verification:** License keys (`PASA-PRO-XXXX-XXXX`, `PASA-LIFE-XXXX-XXXX`) issue an Ed25519-signed certificate payload. The Android client verifies the signature offline using the embedded public key in <0.2ms.
* **Payment Gateways:** Binance Pay (UID `756303714`, Nickname `RBR48`) and bKash personal integration.
* **7-Day Guarantee:** Unconditional 24-hour refund policy built into customer support operations.

### 3.8 Executive Telegram Bot C2 & 6-Hub Modular Architecture
* **Live Telemetry & Status Badges:** Executive header showing device name, live connection latency (`🟢 Online <90s` / `🟡 Idle <10m` / `🔴 Offline`), cryptographic identity (`🛡️ StrongBox TEE` vs software), and license status (`💎 Pro Lifetime`, `⭐ Pro Annual`, `🏢 Enterprise`, `⏳ Trial`).
* **Modular 6-Category Navigation:** Restructured `/menu` dashboard into 6 core operational command hubs:
  1. `📍 Location & Cellular RF` (`menu:location_hub`)
  2. `📸 Covert Forensics` (`menu:forensics_hub`)
  3. `🚨 Lockdown & Siren` (`menu:lockdown_hub`)
  4. `👑 Enterprise Knox Device Owner Suite` (`menu:device_owner_hub`)
  5. `🛡️ Autonomous Traps & Anti-Theft` (`menu:traps_hub`)
  6. `📇 Extraction, Telemetry & System Logs` (`menu:data_hub`)
* **Ergonomic Persistent Keyboard:** Pinned 4x3 bottom keyboard providing instant 1-tap access to primary emergency actions (`Status`, `Locate`, `Siren`, `Photo`, `Screen`, `Video`, `Lock`, `Hub Menu`, `Device Owner`, `Message`, `Traps`, `License`).
* **Interactive Guided Wizards:** Conversational state machine with 5-minute session lifetimes supporting guided inputs:
  - Custom lock PIN (`wizard:lock:custom`)
  - Screen broadcast message (`wizard:msg:custom`)
  - Lockscreen emergency banner (`wizard:lockscreen_info`)
  - Direct outbound SMS dispatcher (`wizard:sendsms`)
  - Emergency SIM alert phone (`wizard:sim_phone`)
  - Remote outbound phone call (`wizard:call`)
  - Smart app lockout (`wizard:lock_app`, `wizard:unlock_app`)
  - Remote storage file downloader (`wizard:getfile`)
  - Coercion decoy Duress PIN (`wizard:duress:set`)
  - Multi-pass cryptographic file shredder (`wizard:shred`)
  - Pro license activation (`wizard:license:activate`)
* **1-Tap Copyable SMS Templates:** Interactive template generator (`sms_template:*`) outputting ready-to-send monospace SMS strings for air-gapped emergency control.

---

## 4. Complete Command Matrix (84 Telegram C2 Commands)

| Category | Commands |
|---|---|
| **Core & Diagnostics** | `/menu`, `/help`, `/status`, `/selftest`, `/info`, `/reboot`, `/battery_alert`, `/network` |
| **Enterprise Device Owner** | `/device_owner`, `/antitamper`, `/usb_lock`, `/camera_lock`, `/bluetooth_lock`, `/mic_mute`, `/lockscreen_info`, `/autolock`, `/wifi_connect`, `/security_audit`, `/notification`, `/self_heal`, `/freeze`, `/unfreeze`, `/frozen`, `/lock_app`, `/unlock_app`, `/biometrics`, `/dns`, `/app_firewall` |
| **Location & Cellular RF**| `/locate` (`/gps`, `/location`), `/tower`, `/sim`, `/sim_lock`, `/track`, `/track_stop`, `/geofence` |
| **Covert Forensics**   | `/snap`, `/screenshot`, `/screen_burst`, `/screenrecord`, `/video`, `/record`, `/livestream`, `/stopstream`, `/livestream_diag`, `/clipboard`, `/gallery_latest`, `/getfile`, `/list_files` |
| **Lockdown & Alert**   | `/lock`, `/lock_message`, `/lock_pin`, `/set_os_pin`, `/set_master_pin`, `/unlock`, `/fakeshutdown`, `/wake`, `/ring`, `/ring_stop`, `/vibrate_pulse`, `/message` |
| **Defense & Deception**| `/duress_pin`, `/pattern_guard`, `/trap`, `/shred`, `/stealth` (`/hide`, `/show`), `/tamper_detect`, `/dead_drop`, `/harden_boot`, `/factory_reset_defense` |
| **Extraction & Telephony**| `/call`, `/contacts`, `/call_log`, `/sms_log`, `/sendsms`, `/history` |
| **System & Maintenance**| `/apps`, `/app_uninstall`, `/smssetup`, `/sms_help`, `/license`, `/check_update`, `/update_confirm`, `/wipe`, `/wipe_confirm` |

---

## 5. Development & Contribution Rules

1. **Never Persist Sensitive Media to Server Disk:** Strategy 1 Zero-Storage is absolute. No PR or code change may save camera captures, audio clips, or GPS history to VPS hard drives.
2. **Foreground Service Types (Android 14+):** Dynamic elevation is required when accessing Camera or Microphone from background (`PasaService.elevateToCamera()` / `elevateToMicrophone()` / `demoteFromCamera()`). Catch `ForegroundServiceStartNotAllowedException` gracefully.
3. **Thread Safety & Dispatchers:**
   - Network & heavy crypto: `Dispatchers.IO`.
   - UI and Accessibility gestures: `Dispatchers.Main` / `Handler(Looper.getMainLooper())`.
   - Background tasks: AndroidX WorkManager with Hilt worker factories.
4. **Direct Boot Awareness:** Receivers handling emergency restarts (`BootReceiver`, `SIMChangeReceiver`, `PowerAlertReceiver`, `SmsCommandReceiver`) must declare `android:directBootAware="true"` and use Device Encrypted Storage before first user unlock.
5. **Node.js Server Conventions:**
   - Use Node 22 built-in `node:sqlite` in WAL mode (`DatabaseSync`).
   - Timing-safe comparisons (`crypto.timingSafeEqual`) for all authentication tokens.
   - Sliding-window rate limiters on public and administrative endpoints.

---

## 6. Production VPS Deployment & OTA Operations

### 6.1 Hostinger VPS Topology
* **Host / IP:** `148.135.137.245` (`srv1678100.hstgr.cloud`, Ubuntu 24.04 LTS).
* **SSH Port & Key:** Port `2222`, Identity file `C:\Users\USER\.ssh\id_rsa_dbm`.
* **Upload Protocol:** `scp -O -P 2222` (Legacy SCP flag `-O` is strictly required; modern SFTP subsystem is restricted on sshd).
* **Remote Application Directory:** `/var/www/pasa-server/`.
* **Process Manager:** PM2 process `pasa-server` (ID 27).

### 6.2 Production Keystore & Cryptographic Identity
* **Keystore File:** `e:\Projects\PrivateApp\pasa-release-key.jks`.
* **Keystore Properties:** `keystore.properties` (password: `PasaSentinel@2026#Secure`, alias: `pasa_sentinel`).
* **Certificate DN:** `CN=PASA Sentinel, OU=Security, O=Izhaan Intellect, L=Dhaka, C=BD`.
* **Certificate SHA-256:** `0c8f62dd8934d3b73e12d965742da29e643bdc157bc859e5b6aa7454409ad57a`.
* **Current Production Release:**
  - **Version:** `v3.5.0` (Build `46`).
  - **APK Binary SHA-256:** `bf577b164e2b786102f037264abe6816e443a1ffa063abfb21835f21f8ebe20f`.
  - **CDN Endpoint:** `https://pasa.izhaanintellect.fun/releases/pasa-latest.apk`.
  - **OTA Manifest Route:** `GET https://pasa.izhaanintellect.fun/api/app/latest?current_version_code=<build>`.

### 6.3 Standard Deployment Workflow
1. **Compilation & Assembly (JDK 17):**
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
   .\gradlew assembleRelease
   ```
2. **Signature Verification:**
   ```powershell
   apksigner verify --verbose --print-certs releases/pasa-latest.apk
   ```
3. **Binary & Server Script Sync to VPS:**
   ```powershell
   scp -O -P 2222 -i ~/.ssh/id_ed25519 pasa-server/server.js pasa-server/upload_telegram_menu.js pasa-server/update_releases_v<ver>.js root@148.135.137.245:/var/www/pasa-server/
   scp -O -P 2222 -i ~/.ssh/id_ed25519 releases/pasa-v<ver>-<build>.apk root@148.135.137.245:/var/www/pasa-server/releases/
   ```
4. **Remote Activation & Symlinks:**
   ```bash
   ssh -p 2222 -i ~/.ssh/id_ed25519 root@148.135.137.245 "cd /var/www/pasa-server/releases && ln -sf pasa-v<ver>-<build>.apk pasa-latest.apk && ln -sf pasa-v<ver>-<build>.apk pasa-v<ver>.apk && cd /var/www/pasa-server && node update_releases_v<ver>.js && node upload_telegram_menu.js && pm2 restart pasa-server"
   ```
5. **Git Repository Push:**
   Commit with author `M S Rana <shohagrana15193@gmail.com>` and push to `origin main`.
