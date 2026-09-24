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
│       │   │   ├── PasaApp.kt           # App entrypoint, Hilt init, notification channels, watchdog restart, global UncaughtExceptionHandler
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
├── pasa-commercial-web/                 # Commercial Marketing & Pricing Frontend (Docker/Nginx)
│   ├── Dockerfile                       # Builds nginx:alpine container
│   ├── docker-compose.yml               # Exposes port 8165 → container port 80
│   ├── nginx.conf                       # Static file serving with gzip and caching
│   ├── scripts/
│   │   └── build_landing.js             # Node.js build script generating index.html from data objects
│   └── public/                          # Static web root served by Nginx
│       ├── index.html                   # "The Intercept Room" operations console (bilingual EN/BN, JSON-LD Schema)
│       ├── llms.txt                     # Standardized AI search crawler knowledge base (Perplexity, ChatGPT, Claude)
│       ├── .well-known/security.txt     # RFC 9116 security disclosure standard
│       ├── security.txt                 # Root fallback RFC 9116 standard
│       ├── robots.txt                   # Search & AI bot crawl directives
│       ├── sitemap.xml                  # Multi-priority XML sitemap with hreflang and image tags
│       ├── knox-anti-uninstall.html     # Programmatic pillar page: Knox anti-uninstall architecture
│       ├── offline-sms-tracker.html     # Programmatic pillar page: Air-gapped offline SMS tracking
│       ├── cellebrite-usb-blocker.html  # Programmatic pillar page: Cellebrite/GrayKey USB killswitch
│       ├── anti-snatch-alarm.html       # Programmatic pillar page: Kinetic 2.65G accelerometer trap
│       ├── pasa-vs-google-find-my-device.html # Programmatic pillar page: Architectural showdown vs Google FMD
│       ├── manual.html                  # Interactive bilingual field manual
│       ├── USER_MANUAL_EN.md / _BN.md   # Complete technical user manuals
│       ├── AUDIT_TRAIL.md               # Cryptographic system audit log
│       ├── INDEPENDENT_AUDIT_REPORT.json# Automated security audit verification
│       ├── favicon.png                  # PASA logo
│       ├── assets/img/                  # Logo, bKash icon, and enterprise provisioning QR SVGs
│       ├── releases/                    # Hosted production binaries
│       │   ├── pasa-latest.apk          # Current production APK (v3.5.8-54)
│       │   └── PASA-Device-Owner-Setup-Kit.zip # Windows guided setup wizard
│       ├── privacy.html                 # Privacy policy
│       └── terms.html                   # Terms of service
├── pasa-setup-kit/                      # Windows Device Owner Setup Kit (non-technical user wizard)
│   ├── PASA Device Owner Setup.bat      # Double-click launcher — runs setup.ps1 with Bypass policy
│   ├── setup.ps1                        # 6-step guided PowerShell wizard (ADB download → DPM provisioning)
│   └── README.md                        # Full docs: T&Cs, why accounts must be removed, troubleshooting
├── releases/                            # Git-tracked release binaries & distribution packages
│   ├── pasa-v3.5.8-54.apk               # Current signed production APK
│   ├── pasa-latest.apk                  # Symlink → current APK
│   ├── PASA-Device-Owner-Setup-Kit.zip  # Current Windows setup kit (12 KB, no APK bundled)
│   └── PASA-Setup-Kit-v3.5.4.zip        # Legacy versioned kit
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
* **Cryptographic SIM Tray Lock (`/sim_tray_lock`):** Deep Device Owner lockdown layer on top of `/sim_lock`. When armed and an unauthorized SIM is inserted:
  1. Rotates lockscreen credentials to a secret 8-digit random PIN using Knox hardware escrow tokens (`dpm.resetPasswordWithToken`). The secret PIN is transmitted only to the owner via Telegram.
  2. Suspends all third-party applications (`dpm.setPackagesSuspended`) — phone becomes a complete brick to the unauthorized handler with zero apps launchable. PASA itself is explicitly excluded and remains 100% active.
  3. Knox Kiosk Lost Mode engaged, biometrics disabled (forcing PIN only), factory reset blocked.
  4. Remotely reversible only by owner via `/sim_tray_lock release`.
* **Battery Health & Rapid Drain Alert (`/battery_alert`):** Proactive battery telemetry alerting owner on critical low levels, abnormal rapid drain (detecting background surveillance/tethers), or unauthorized charger disconnection (`/battery_alert [status|enable|disable|threshold]`).
* **Dual-SIM Cell Tower Triangulation (`/tower`):** Scans LTE/5G NR/GSM cell identities (MCC, MNC, LAC/TAC, CID, dBm) across all active subscriptions for resilient indoor localization without satellite reception.

### 3.5 Deception & Covert Tactile Feedback
* **Fake Shutdown Deception (`FakeShutdownActivity`):**
  - Simulates authentic OEM power-down animation, then drops brightness to 0-nit black canvas with `WindowManager.LayoutParams.FLAG_FULLSCREEN` and `SYSTEM_UI_FLAG_IMMERSIVE_STICKY`.
  - Phone appears completely dead. Screen taps silently trigger front camera mugshots and GPS beacons.
  - Dismissed remotely via `/wake` command or secret multi-tap sequence.
* **Covert Tactile Device Locator (`/vibrate_pulse`):** Custom vibration sequences (intermittent pulse, SOS Morse code `...---...`, continuous) for locating device covertly without loud audio sirens alerting thieves in hostile environments (`/vibrate_pulse [pulse|sos|continuous|stop]`).

### 3.6 Air-Gapped Cellular SMS Fallback & Telephony C2 (`SmsCommandReceiver`)
* Listens on `SMS_RECEIVED` (priority 999) with direct boot awareness.
* Syntax: `PASA <6-digit-TOTP-or-MasterPIN> <command>` (e.g. `PASA 419582 /locate` or `PASA 419582 /status`).
* Validates TOTP code against hardware clock using enrolled secret key (RFC 6238, window tolerance ±3 steps) or master cryptographic passphrase.
* **Dual-SIM Routing:** Dynamically extracts `subscriptionId` from incoming SMS and dispatches responses via the receiving SIM's `SmsManager`.
* **Remote Outbound Calling with Dual-SIM Selection (`/call`):** Remotely places outbound phone calls via `TelecomManager.placeCall()` with explicit SIM slot binding (`EXTRA_PHONE_ACCOUNT_HANDLE`). Supports `/call <number> [sim1|sim2] [speaker|earpiece]` and `/call status` to inspect all active call-capable carrier accounts.
* **Direct Outbound Cellular SMS (`/sendsms`):** Transmits SMS messages directly via cellular radio (`/sendsms [sim1|sim2] <number> <msg>`). Used to verify unknown device phone numbers via caller ID when carriers do not store the MSISDN on the SIM card chip.
* **Unified Extraction Pagination & Text Document Exports:** Full pagination, keyword search, and instant text exports across `/contacts`, `/sms_log`, `/call_log`, `/history`, and `/list_files`. Exports write to `context.cacheDir` and are delivered directly via Telegram `sendDocument` as `.txt` files.
* **SIM & Cellular Carrier Telemetry (`/sim`):** Displays active SIM slots, carrier names, subscription IDs, signal strength levels, network types (2G/3G/4G/5G), and MCC/MNC codes.
* **Air-Gapped SMS Fallback Guide (`/sms_help`):** Comprehensive on-device and Telegram offline cheatsheet with 1-tap copyable monospace templates (`<code>PASA <PIN> /locate</code>`, etc.) and step-by-step TOTP enrollment guidance (`/smssetup`).
* **Complete Air-Gapped Command Coverage:** Every single command responds via SMS: `/locate`, `/status`, `/usb_lock`, `/camera_lock`, `/bluetooth_lock`, `/mic_mute`, `/wifi_connect`, `/lockscreen_info`, `/autolock`, `/app_uninstall`, `/reboot`, `/security_audit`, `/antitamper`, `/biometrics`, `/ring`, `/ring_stop`, `/lock`, `/unlock`, `/fakeshutdown`, `/wake`, `/set_master_pin <pin>`, `/wipe`, `/wipe_confirm`, `/sim_tray_lock`, `/sms_help`. All SMS responses are stripped of HTML tags for clean SMS delivery.


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
  - Anti-EDL Dead Man's Switch timeout (`wizard:deadman`)
  - Heat-gun Thermal Anomaly Trap config (`wizard:thermal`)
* **1-Tap Copyable SMS Templates:** Interactive template generator (`sms_template:*`) outputting ready-to-send monospace SMS strings for air-gapped emergency control.

### 3.9 Sovereign Multi-Tenant Privacy Guarantee (Zero Fleet Management Backdoor)
* **Zero Centralized Fleet Control:** All centralized web dashboards (`/dashboard`, `/admin`), WebSocket relays (`/ws/dashboard`), and fleet enumeration APIs (`/api/admin/devices`, `/api/admin/commands`, `/api/admin/logs`, `/fleet`, `/users`) are permanently eliminated.
* **Non-Custodial Architecture:** The server operates strictly as an ephemeral, zero-knowledge packet relay. Admins, developers, or server operators cannot list user devices, inspect user telemetry, or dispatch commands to users' phones.
* **Sovereign Telegram & SMS Isolation:** Each user's phone is controlled exclusively by that user's private Telegram bot credentials or air-gapped cryptographic TOTP SMS fallback. Device telemetry and surveillance evidence are delivered directly to the user's private Telegram chat and shredded from RAM immediately.

---

## 4. Complete Command Matrix (87 Telegram C2 Commands)

| Category | Commands |
|---|---|
| **Core & Diagnostics** | `/menu`, `/help`, `/status`, `/selftest`, `/info`, `/reboot`, `/battery_alert`, `/network` |
| **Enterprise Device Owner** | `/device_owner`, `/antitamper`, `/usb_lock`, `/camera_lock`, `/bluetooth_lock`, `/mic_mute`, `/lockscreen_info`, `/autolock`, `/wifi_connect`, `/security_audit`, `/notification`, `/self_heal`, `/freeze`, `/unfreeze`, `/frozen`, `/lock_app`, `/unlock_app`, `/biometrics`, `/dns`, `/app_firewall` |
| **Location & Cellular RF**| `/locate` (`/gps`, `/location`), `/tower`, `/sim`, `/sim_lock`, `/sim_tray_lock`, `/track`, `/track_stop`, `/geofence` |
| **Covert Forensics**   | `/snap`, `/screenshot`, `/screen_burst`, `/screenrecord`, `/video`, `/record`, `/livestream`, `/stopstream`, `/livestream_diag`, `/clipboard`, `/gallery_latest`, `/getfile`, `/list_files` |
| **Lockdown & Alert**   | `/lock`, `/lock_message`, `/lock_pin`, `/set_os_pin`, `/set_master_pin`, `/unlock`, `/fakeshutdown`, `/wake`, `/ring`, `/ring_stop`, `/vibrate_pulse`, `/message` |
| **Defense & Deception**| `/duress_pin`, `/pattern_guard`, `/trap`, `/thermal`, `/deadman` (`/dead_drop`), `/shred`, `/stealth` (`/hide`, `/show`), `/tamper_detect`, `/harden_boot`, `/factory_reset_defense` |
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
  - **Version:** `v3.5.8` (Build `54`).
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
   scp -O -P 2222 -i ~/.ssh/id_rsa_dbm pasa-server/server.js pasa-server/upload_telegram_menu.js root@148.135.137.245:/var/www/pasa-server/
   scp -O -P 2222 -i ~/.ssh/id_rsa_dbm releases/pasa-v<ver>-<build>.apk root@148.135.137.245:/var/www/pasa-server/releases/
   ```
4. **Remote Activation & Symlinks:**
   ```bash
   ssh -p 2222 -i ~/.ssh/id_rsa_dbm root@148.135.137.245 "cd /var/www/pasa-server/releases && ln -sf pasa-v<ver>-<build>.apk pasa-latest.apk && cd /var/www/pasa-server && pm2 restart pasa-server"
   ```
5. **Git Repository Push:**
   Commit with author `M S Rana <shohagrana15193@gmail.com>` and push to `origin main`.

### 6.4 Windows Device Owner Setup Kit
* **Purpose:** Guided wizard for non-technical customers to provision Device Owner without any command-line knowledge.
* **Kit Location:** `pasa-setup-kit/` (tracked in git).
* **Distribution ZIP:** `releases/PASA-Device-Owner-Setup-Kit.zip` (12 KB — no APK bundled).
* **Public Download URL:** `https://pasa.izhaanintellect.fun/releases/PASA-Device-Owner-Setup-Kit.zip`.
* **API Metadata:** `GET https://pasa.izhaanintellect.fun/api/app/setup-kit/info` → JSON with filename, sizeBytes, sizeMB, updatedAt, downloadUrl.
* **Wizard Steps (setup.ps1):**
  1. **ADB Setup** — checks PATH / known SDK locations / kit `adb/` subfolder; auto-downloads from `dl.google.com/android/repository/platform-tools-latest-windows.zip` if missing.
  2. **Connect Phone** — guides USB Debugging activation, waits for `adb devices` authorized state (60 s timeout).
  3. **Remove Accounts (Auto-Assisted)** — uses `adb shell dumpsys account` to detect accounts; calls `adb shell am start -a android.settings.ACCOUNT_SYNC_SETTINGS --es account_name <n> --es account_type <t>` to open each removal screen directly; filters system-internal account types that are harmless.
  4. **Smart Install** — calls `GET /api/app/latest?current_version_code=0`, reads installed versionCode via `adb shell dumpsys package`; **skips download entirely if installed build ≥ server build**; SHA-256 integrity verified before install.
  5. **Device Owner** — runs `adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin`; parses result for known error strings with user-friendly remediation.
  6. **Verification** — confirms install + Device Owner active; shows next-step instructions.
* **Safety:** ZIP contains no APK, no secrets, no credentials. All network calls go to `dl.google.com` (ADB) and `pasa.izhaanintellect.fun` (APK). No phone data transmitted to any server. Safe to share publicly.
* **Updating Kit:** When releasing a new PASA version, rebuild ZIP with `Compress-Archive -Path pasa-setup-kit\* -DestinationPath releases\PASA-Device-Owner-Setup-Kit.zip` and SCP to VPS `releases/`. No code changes needed — wizard always pulls latest APK from server at runtime.

### 6.5 Commercial Web Landing Page & SEO Architecture (Docker/Nginx)
* **Container Name:** `pasa-commercial-app` (nginx:alpine, port `127.0.0.1:8165 → 80`).
* **Host Directory:** `/opt/pasa-commercial-web/public/` (NOT bind-mounted — files must be `docker cp`'d into the container).
* **Live Domain:** `https://pasa.izhaanintellect.fun/` (Cloudflare proxy, dynamic cache bypass).
* **Design Language:** "The Intercept Room" — dark operations console with:
  - Left module rail with live UTC clock, status dot, sound FX toggle, and EN/বাং language switcher.
  - 3D parallax holographic smartphone mockup with 60fps facial biometric tracking canvas.
  - "🚨 REPLAY SNATCH INTERCEPT" interactive simulation with Web Audio synthesized SFX (shutter click, stamp thud, alarm tone, sonar chirp).
  - 4-exhibit evidence wall (pin cards with red thread vector graphics).
  - 8-hub tabbed capabilities arsenal with threat mitigation badges.
  - 86-command terminal with search, category pills, and 1-tap copy.
  - Comparison matrix vs Google Find My Device and Play Store apps.
  - Intel briefing FAQ with 38 questions across 6 category tabs.
  - 3-tier pricing (Free Tactical Evaluation / Pro Lifetime $25 / Enterprise Fleet $99).
  - Bilingual (108 EN + 108 BN `data-i18n` translation keys, 0 missing).
* **4 Device Owner Provisioning Pathways (Zero-PC Capable):**
  1. **Windows 1-Click Setup Kit:** Automated PowerShell wizard detecting ADB, removing accounts, and running DPM provisioning in 60s.
  2. **Android 6-Tap Welcome Screen QR (Zero PC):** On factory reset or new device, tapping 6 times anywhere on the initial setup screen opens camera; scanning `qr-enterprise-provisioning.svg` installs and provisions Device Owner automatically.
  3. **WebADB Browser Setup (Mac/Linux/ChromeOS/Chromium Phone):** Direct WebUSB browser connection via `https://app.webadb.com` running the DPM command with no terminal required.
  4. **Manual Terminal ADB:** Standard `adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin`.
* **Hyper-Aggressive SEO & AI Discovery Engine:**
  - **JSON-LD Schema Graphs:** `SoftwareApplication` (4.9 rating, $25, SecurityApplication), `Organization` (Izhaan Intellect), `FAQPage` (6-question Knowledge Graph takeover), and `TechArticle` schemas.
  - **AI Search Specification (`/llms.txt`):** Authoritative markdown context engineered specifically for Perplexity, ChatGPT Search, Claude, and Gemini SGE.
  - **RFC 9116 Standard (`/.well-known/security.txt`):** Authoritative vulnerability reporting policy and security crawler trust score enhancement.
  - **5 Programmatic Tactical Pillar Pages:** Dedicated, lightweight HTML pages targeting exact-match commercial keywords:
    1. `/knox-anti-uninstall.html` (Knox Enterprise Device Owner anti-uninstall immunity).
    2. `/offline-sms-tracker.html` (Air-gapped TOTP RFC 6238 SMS command & control).
    3. `/cellebrite-usb-blocker.html` (Android 12+ hardware USB data pin killswitch).
    4. `/anti-snatch-alarm.html` (Kinetic 2.65G accelerometer snatch alarm & mugshot trap).
    5. `/pasa-vs-google-find-my-device.html` (Forensic comparison vs Google Find My Device).
  - **Sitemap & Crawl Directives:** Priority-weighted `sitemap.xml` with image & `hreflang` declarations; `robots.txt` welcoming standard and generative AI search crawlers.
* **Deployment Workflow:**
  1. Build HTML: `node pasa-commercial-web/scripts/build_landing.js` (or edit/author HTML directly).
  2. Upload: `scp -O -P 2222 -i ~/.ssh/id_rsa_dbm <files> root@148.135.137.245:/opt/pasa-commercial-web/public/`
  3. Docker cp & reload: `ssh root@148.135.137.245 -p 2222 "docker cp /opt/pasa-commercial-web/public/. pasa-commercial-app:/usr/share/nginx/html/ && docker exec pasa-commercial-app nginx -s reload"`
* **Nginx Config:** Gzip on, `try_files $uri $uri/ $uri.html /index.html`, 30-day cache for static assets.

---

## 7. Licensing Enforcement (v3.5.5+)

* **Strict 7-Day Trial Hard Lockout:** After 7-day trial expiration, ALL features are completely disabled. No single command executes without an active Pro or Enterprise license.
* **Red Countdown Banner:** Telegram bot displays a persistent 1-line red banner at the top of every response showing trial days remaining (e.g., `🔴 TRIAL: 5d 12h remaining`).
* **License Tiers:**
  - `⏳ Trial` — 7 days, core commands only.
  - `💎 Pro Lifetime` — $25 / ৳3,000 BDT, all 86 commands, lifetime OTA.
  - `🏢 Enterprise` — $99 / ৳12,000 BDT, 5 devices, dedicated relay server.
* **Offline Verification:** Ed25519-signed license certificates verified client-side in <0.2ms with no network dependency.
* **Payment Gateways:** Binance Pay (UID `756303714`, Nickname `RBR48`), bKash (official payment account requested via WhatsApp concierge **+880 1762-033445**; the number itself is NOT a bKash wallet).
* **Refund Policy:** Unconditional 24-hour 100% money-back guarantee.

---

## 8. Session Changelog & Operational Decisions (2026-09-24)

This section records significant architectural decisions and code changes made in the September 24 2026 session that all future agents must be aware of.

### 8.1 Contact & Identity Corrections
* **WhatsApp Support Contact:** `+8801762033445` is strictly for **WhatsApp Customer Support / Concierge only**. It is **NOT** a personal bKash wallet. Customers wishing to pay via bKash request the current official bKash account number directly via WhatsApp.
* **Telegram handle:** `https://t.me/rbr_48` has been **removed** from all public-facing pages and replaced with official support bot link (`@pasa_sentinel_bot`).

### 8.2 Official Customer Support Bot — `@pasa_sentinel_bot`
* **Token:** Was `8731444238:AAH9YHEvuZblvjMHyjPEfOdCVwaE1wLDTV4` — **THIS TOKEN IS COMPROMISED AND MUST BE ROTATED.**
* **Role:** Configured as the official public-facing customer support agent (not a device C2 bot). Handles pricing enquiries, setup help, license activation, and ticket relay to admin.
* **Routing:** In `server.js`, when `token === PASA_CENTRAL_BOT_TOKEN`, the update is routed to `handleCustomerSupportUpdate()` instead of `handleTelegramUpdate()`.

### 8.3 Security Hardening — server.js Changes (deployed to VPS, PM2 ID 27)

#### Wipe 2-Factor PIN Challenge
* **Problem:** Clicking "CONFIRM FACTORY WIPE" in the Telegram bot immediately dispatched `/wipe` to the device — one Telegram account hijack = instant irreversible device wipe.
* **Fix:** `cmd:wipe` callback now intercepts the dispatch and instead:
  1. Generates a cryptographically random 6-digit PIN via `crypto.randomInt()`.
  2. Stores it in `pendingWipeSessions` Map with a 5-minute TTL and 3-attempt lockout.
  3. Sends the PIN to the owner's Telegram chat.
  4. Sets chat state to `WAITING_FOR_WIPE_PIN`.
  5. Only dispatches `/wipe` to the device after the owner types the correct PIN back.
  6. Wrong PIN = decrement attempts; lockout = session cancelled; expired = session cancelled.
* **Key functions added:** `generateWipePin()`, `createWipeSession()`, `verifyWipePin()`.
* **State added:** `WAITING_FOR_WIPE_PIN` in the wizard state machine.

#### Startup Security Audit (`runStartupSecurityAudit()`)
* Runs automatically on every server boot (inside `app.listen` callback).
* Checks:
  1. Known-compromised bot token detection (hardcoded blacklist of leaked tokens).
  2. `ADMIN_CHAT_ID` presence and numeric validity.
  3. `BOT_TOKEN` presence and format.
  4. `ADMIN_KEY` length (must be ≥ 32 chars).
  5. `uploads/evidence/` directory for stale files — auto-purges them if found.
* Output goes to PM2 error log with `[PASA SECURITY AUDIT]` prefix.

#### Destructive Command Rate Limiter
* `const destructiveCmdLimiter` — max 3 destructive operations per hour per IP.
* Available for future use on wipe, factory reset, and shred endpoints.

### 8.4 New Files Added to VPS
| File | Purpose |
|---|---|
| `pasa-server/scripts/security-check.js` | Standalone CLI audit tool — run `node scripts/security-check.js` on VPS anytime to audit secrets strength, token integrity, zero-storage compliance, and .env permissions |
| `pasa-server/.env.example` | Template with all required env vars, generation instructions, and minimum strength requirements. Safe to commit to git. |

### 8.5 Action Checklist & Current Status
1. **✅ Rotate Bot Tokens in `@BotFather`:** COMPLETED. New tokens applied in `/var/www/pasa-server/.env`. Bot commands registered and polling active.
2. **✅ Set `ADMIN_KEY` in VPS `.env`:** COMPLETED. 64-character cryptographically random key generated and saved.
3. **✅ Set `ALLOWED_ORIGIN` in VPS `.env`:** COMPLETED. Configured to `https://pasa.izhaanintellect.fun`.
4. **✅ Automated Security Audit:** 8/8 checks passing (Status: SECURE).
5. **🟡 Move `pasa-release-key.jks` off Windows dev PC:** Recommended next step — copy to air-gapped USB drive, delete from `d:\Software_and_Apps\PrivateApp\pasa-release-key.jks`. Only plug in USB during APK signing/release builds.
6. **🟡 Enable Telegram 2FA:** User recommendation — Telegram Settings → Privacy & Security → Two-Step Verification.

### 8.6 VPS Deployment State
* **VPS:** `148.135.137.245`, SSH port `2222`, user `root`
* **Server path:** `/var/www/pasa-server/server.js`
* **PM2 process:** ID `27`, name `pasa-server`, status: `online`
* **Security Audit Status:** `8/8 checks passed (Score: 100%)`
* **Live Domain:** `https://pasa.izhaanintellect.fun/`

### 8.7 Known Security Threat Model (post-hardening)
| Threat | Mitigation | Residual Risk |
|---|---|---|
| Telegram account hijack → wipe device | ✅ Wipe 2FA PIN challenge | Low — attacker needs both Telegram access AND see the bot reply |
| Bot token exposure | ✅ Both tokens rotated & verified fresh | None (Zero compromised tokens) |
| Stale evidence files on VPS | ✅ Auto-purge on startup + zero-storage verified | None |
| Weak or missing ADMIN_KEY | ✅ 64-char crypto key active | None |
| JKS signing key on dev PC | ⚠️ Stored locally on Windows dev machine | 🟡 Recommended to move to air-gapped USB |
| Telegram message unencrypted | ❌ Cannot fix — Telegram design | Accepted risk; documented |
| VPS compromise → device mapping exposed | SQLite WAL, no GPS tracks | Medium — device↔chatId mapping in local DB |

### 8.8 Backdoors & Unauthorized Control Vectors Closed
1. **Unsigned VPS Command Injection Neutralized:**
   - In `PasaService.kt`, previous code executed commands even if `envelope.isNullOrBlank()`.
   - **Patch:** Enforced strict fail-closed cryptographic envelope verification. Any remote command from VPS without a valid Ed25519 signature is immediately rejected and reported.
2. **Device Hijacking via Unassigned `ownerChatId` Closed:**
   - In `server.js` `isDeviceMatchingBot`, previous code allowed loose matches if `ownerChatId` was null/empty.
   - **Patch:** Strictly requires `d.ownerChatId` to be set and strictly equal to `chatId`. No unowned or mismatched device can ever be claimed or commanded.
3. **Forced VPS Fallback in Onboarding Removed:**
   - In `SetupActivity.kt`, leaving Server URL blank previously fell back to `DEFAULT_SERVER_URL`.
   - **Patch:** Removed forced fallback. Users can now run in **100% Sovereign (Direct Telegram) Mode** where the phone communicates directly with `api.telegram.org` and never connects to any VPS.

### 8.9 Production Release v3.5.6 (Build 52)
* **Release Date:** 2026-09-24
* **Version Name:** `3.5.6` | **Version Code:** `52`
* **Artifact:** `releases/pasa-v3.5.6-52.apk` (19.25 MB, 19,250,443 bytes)
* **SHA-256:** `becdb9b13beaac9d790960fc83cbafb957c788800a0f911c284903d1621fa5c8`
* **Release Highlights:** SELinux false-positive fix, fail-closed Ed25519 envelope enforcement, direct Telegram sovereign mode.

### 8.10 Production Release v3.5.7 (Build 53)
* **Release Date:** 2026-09-24
* **Version Name:** `3.5.7` | **Version Code:** `53`
* **Artifact:** `releases/pasa-v3.5.7-53.apk` (19.25 MB, 19,250,438 bytes)
* **SHA-256:** `275bdd4d49649e9576aa14ae44c37a1bb58d309eb364acc77e9900f2163afb52`
* **Signing Key:** `pasa-release-key.jks` (v1 + v2 signed)
* **Release Highlights:**
  - **App Inventory Modernization (`AppManageCommand.kt`):** 35 items/page pagination (`/apps <N>`), instant text document export (`/apps export`), real-time keyword search (`/apps search <name>`).
  - **Forensic & Memory Hardening:** Automatic EXIF metadata stripping, multi-pass cryptographic zero-fill shredder (`PrivacyHygieneHelper.kt`), streaming HTTP uploads preventing OOM on large videos.

### 8.11 Production Release v3.5.8 (Build 54)
* **Release Date:** 2026-09-24
* **Version Name:** `3.5.8` | **Version Code:** `54`
* **Artifact:** `releases/pasa-v3.5.8-54.apk` (19.25 MB, 19,250,463 bytes)
* **SHA-256:** `592f4f4507c12b3c88053cf536110652431706204ba8bb971088ae3ad9b5cec5`
* **Signing Key:** `pasa-release-key.jks` (v1 + v2 signed)
* **Host Endpoints:**
  - OTA Check: `GET https://pasa.izhaanintellect.fun/api/app/latest`
  - Direct Download: `https://pasa.izhaanintellect.fun/releases/pasa-v3.5.8-54.apk`
  - Latest Symlink: `https://pasa.izhaanintellect.fun/releases/pasa-latest.apk`
* **Release Highlights:**
  - **Cryptographic SIM Tray Lock (`/sim_tray_lock`):** Deep Device Owner lockdown layer upon unauthorized SIM insertion. Generates a secret random 8-digit PIN via Knox hardware escrow tokens, suspends all third-party apps (`dpm.setPackagesSuspended`), locks Knox Kiosk Lost Mode, revokes biometrics, blocks factory resets, and transmits emergency unlock PIN + mugshot + GPS to Telegram. Reversible remotely via `/sim_tray_lock release`.
  - **Dual-SIM Remote Calling (`/call`):** Remotely places phone calls with explicit SIM slot binding via `TelecomManager.placeCall()` and `EXTRA_PHONE_ACCOUNT_HANDLE`. Supports `/call <number> [sim1|sim2] [speaker|earpiece]` and `/call status` to view active call-capable accounts.
  - **Unified Extraction Pagination & Document Exports:** Implemented across `/contacts`, `/sms_log`, `/call_log`, `/history`, and `/list_files`. Each command supports paging (`<command> <page>`), instant `.txt` file export (`<command> export`), and keyword search (`<command> search <query>`).
  - **Network Continuity Guarantee:** When `/sim_tray_lock` engages, PASA is explicitly exempted from package suspension, preserving full cellular data, Wi-Fi, and SMS command channels. Quick settings lockout prevents disabling Wi-Fi/mobile data.

