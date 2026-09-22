# 🛡️ PASA SENTINEL - COMPLETE FEATURE ANALYSIS
## Private Android Security Agent v3.5.1+

**Date**: September 22, 2026  
**Analysis Scope**: Complete application audit & feature catalog  
**Build Status**: Production Ready (Build 36+)

---

## 📋 EXECUTIVE SUMMARY

**PASA Sentinel** is a comprehensive enterprise-grade Android security and device protection platform with 60+ commands covering:
- Remote device control & emergency containment
- Forensic evidence capture (photos, video, audio, SMS)
- Tamper detection & anti-tampering hardening
- Location tracking (GPS, cell tower triangulation)
- Secure communication (Telegram bot, SMS commands)
- Device ownership & kiosk mode management
- Application firewall & access control

---

## 🏗️ TECHNICAL ARCHITECTURE

### Core Services
1. **PasaService** - Foreground guardian service (data sync, location, camera, microphone)
2. **AccessibilityScreenCaptureService** - Screenshot capture via accessibility framework
3. **Device Admin Receiver** - Enterprise device management
4. **Boot Receiver** - Auto-resurrection on device startup
5. **Watchdog Receiver** - Android 14-16 resurrection mechanism

### Communication Channels
- **Telegram Bot** - Primary command interface (Telegram API)
- **SMS Commands** - Offline backup (PASA \<credential\> /command)
- **Backend VPS** - Optional cloud command relay (izhaanintellect.fun)

### Security Framework
- **Dagger Hilt** - Dependency injection
- **Device Owner API** - Enterprise kiosk mode
- **Hardware Escrow Tokens** - Secure PIN resets
- **TOTP/Master Password** - Multi-factor SMS authentication

---

## 🎯 COMMAND CATEGORIES (60+ Commands)

### 1. 🔐 EMERGENCY CONTAINMENT (Device Locking & Unlock)

| Command | Function | Auth Required | Device Owner |
|---------|----------|---|---|
| `/lock` | Lock device immediately, activate Lost Mode Guard | ✅ | Recommended |
| `/lock_message <text>` | Set custom message on lockscreen | ✅ | Optional |
| `/lock_pin <4-8 digits>` | Local emergency PIN lock (bypass with SMS/Telegram) | ✅ | No |
| `/set_os_pin <4-16 digits>` | Overwrite Android OS hardware PIN (remote reset capability) | ✅ | **REQUIRED** |
| `/unlock` | Release device from Lost Mode Guard, disable lockdown | ✅ | Optional |
| `/wipe` | Emergency factory reset (requires confirmation) | ✅ | **REQUIRED** |
| `/wipe_confirm <password>` | Confirm factory reset (60s window after /wipe) | ✅ | **REQUIRED** |
| `/set_master_pin <4-32 chars>` | Update master password remotely | ✅ | Optional |

**Details:**
- Lost Mode Guard: Full-screen lockscreen with remote-only unlock (Telegram/SMS)
- PIN entry **DISABLED** when in Lost Mode (Device Owner active)
- Keyguard features disabled: PIN, Pattern, Fingerprint, Face, Biometric
- Touch detection: Captures covert photos to Telegram when screen touched

---

### 2. 👑 ENTERPRISE DEVICE OWNER DEFENSE

| Command | Function | Auth | DO Required | Impact |
|---------|----------|------|---|---|
| `/device_owner` | Full telemetry dashboard (status, permissions, restrictions) | ✅ | ✅ | Info only |
| `/antitamper on\|off\|status` | Lock safe boot, airplane mode, factory reset | ✅ | ✅ | Critical |
| `/usb_lock on\|off\|status` | Kill USB data signaling (AC charging only) | ✅ | ✅ | Critical |
| `/notification hide\|show\|toggle` | Hide/restore notification shade & quick settings | ✅ | ✅ | High |
| `/self_heal` | Lock permissions permanently as unrevokable | ✅ | ✅ | High |
| `/freeze <package>` | Hide application into shadow vault (invisible) | ✅ | ✅ | High |
| `/unfreeze <package>` | Restore hidden application | ✅ | ✅ | High |
| `/frozen` | List all frozen/hidden applications | No | ✅ | Info only |
| `/biometrics on\|off` | Disable fingerprint/face (force Master PIN only) | ✅ | ✅ | High |
| `/dns quad9\|cloudflare\|adguard\|off` | System-wide encrypted DNS-over-TLS (prevents ISP tracking) | ✅ | ✅ | High |
| `/reboot` | Remotely restart device hardware | ✅ | ✅ | High |

**Device Owner Requirements:**
- Set via ADB: `adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin`
- Enables kiosk mode, lock task, comprehensive anti-tamper protection
- Unrevocable except via factory reset

---

### 3. 🔒 ADVANCED SECURITY MONITORING

| Command | Function | Auth | Pro |
|---------|----------|------|-----|
| `/sim_lock enable\|disable` | Prevent SIM swap attacks (alert on SIM change) | ✅ | No |
| `/sim_lock whitelist <imsi>` | Whitelist known SIM cards | ✅ | No |
| `/sim_lock alert_action` | Action on unauthorized SIM (wipe/alert) | ✅ | No |
| `/vibrate_pulse [count\|sos\|location]` | Locate via vibration patterns (distinctive rhythms) | ✅ | No |
| `/pattern_guard enable\|disable` | Monitor unlock attempts, capture face on fail | ✅ | No |
| `/app_firewall enable\|disable` | Block RAT/remote access tools from network | ✅ | No |
| `/battery_alert enable\|disable` | Monitor charging patterns for tampering | ✅ | No |
| `/tamper_detect enable\|disable\|scan` | **FIXED**: Real root detection (/system/bin/su, /data/adb/magisk, KSU) | ✅ | No |

**Tamper Detection Monitors:**
- Root binaries: su, Magisk, KernelSU, SuperSU
- Debugger attachment
- Emulator detection
- Hook/injection frameworks (Xposed, Frida)
- APK signature tampering
- SELinux violations

---

### 4. 🛡️ TAMPER-PROOF HARDENING

| Command | Function | Auth | Status |
|---------|----------|------|--------|
| `/harden_boot lock\|unlock\|status` | Lock recovery mode, prevent factory reset | ✅ | Active |
| `/factory_reset_defense status` | Show FRP layers & protection details | ✅ | Active |
| `/dead_drop enable\|disable\|upload` | Backup evidence to encrypted vault (DISABLED - non-functional) | ✅ | ⚠️ Disabled |

---

### 5. 📍 LOCATION & TRACKING (GPS + Cell Tower)

| Command | Function | Auth | Details |
|---------|----------|------|---------|
| `/locate` | Single high-accuracy GPS fix + Google Maps pin | ✅ | Fresh GNSS + fallback to cached |
| `/tower` | Dual-SIM cell tower triangulation + RF signal telemetry | ✅ | No GPS required |
| `/sim [slot]` | Display active SIM info, carrier, signal strength | No | Info only |
| `/track <minutes>` | Continuous periodic GPS tracking (configurable interval) | ✅ | Pro feature |
| `/track stop` | Deactivate tracking | ✅ | Pro feature |
| `/geofence here 200` | Set safe zone, alert on exit (radius in meters) | ✅ | Pro feature |
| `/geofence status\|on\|off` | Safe zone status & toggle | ✅ | Pro feature |
| `/smssetup` | Enroll in TOTP (6-digit codes) for secure SMS commands | No | Setup only |

**Location Methods:**
1. GPS (GNSS) - High accuracy when locked
2. Cell tower triangulation - Works without GPS
3. IP-based geolocation - WiFi fallback

---

### 6. 📸 FORENSICS & MEDIA CAPTURE

#### Photo & Screenshot Capture

| Command | Function | Auth | Pro | Zero-Blackout |
|---------|----------|------|-----|---|
| `/snap front\|back\|both` | Covert snapshot(s) | ✅ | No | ✅ Headless |
| `/screenshot` | Silent full-screen capture via accessibility | ✅ | Trial | No |
| `/screen_burst [5-10]` | Rapid 5-10 frame montage | ✅ | Trial | No |

**Zero-Blackout Dual-Path Architecture:**
1. **Primary**: StealthVideoManager (headless, no screen flicker)
2. **Fallback**: StealthCaptureBridge (via lockscreen activity if needed)

#### Video Recording

| Command | Function | Auth | Pro | Details |
|---------|----------|------|-----|---------|
| `/video front\|back <seconds>` | Silent camera video (1-60s) | ✅ | Trial | Headless capture |
| `/livestream [front\|back] [mins]` | Near-live sequential video stream | ✅ | **YES** | 5-second segments, up to 30 min |
| `/stopstream` | Stop active live stream | ✅ | Yes | Cancels ongoing stream |
| `/livestream_diag` | Troubleshoot livestream configuration | No | Yes | Info only |

**Livestream Features:**
- Segment-based (5-second intervals for reliability)
- Retry logic on network failures
- Automatic fallback between headless & activity-based capture
- Direct Telegram dispatch (zero storage persistence)
- Up to 30-minute continuous streams

#### Audio Recording

| Command | Function | Auth | Pro | Stealth |
|---------|----------|------|-----|---------|
| `/record <seconds>` | Ambient microphone recording (1-300s) | ✅ | Trial | Silent |
| `/record stop` | Stop active recording | ✅ | Trial | - |

#### Screen Recording

| Command | Function | Auth | Pro | Format |
|---------|----------|------|-----|--------|
| `/screenrecord <seconds>` | MP4 screen recording | ✅ | **YES** | MP4 video |

---

### 7. 📇 DATA EXTRACTION & AUDIT LOGS

| Command | Function | Auth | Scope |
|---------|----------|------|-------|
| `/history [count]` | View recent command execution logs | ✅ | Last N commands |
| `/contacts [search]` | Read device address book | ✅ | All or filtered |
| `/call_log [count]` | View incoming/outgoing call history | ✅ | Last N calls |
| `/sms_log [count]` | View recent SMS messages | ✅ | Last N SMS |
| `/sendsms <number> <message>` | Send SMS via default SIM | ✅ | Text message |
| `/sendsms sim1\|sim2 <number> <message>` | Send SMS from specific SIM slot | ✅ | Dual-SIM support |
| `/clipboard` | Read device clipboard content | ✅ | Current clipboard |

**Security Posture:**
- Phone numbers & message content **REMOVED** from Logcat (fixed this session)
- SMS dispatch includes rate-limiting (prevents spam)

---

### 8. 🔊 SIREN & DISPLAY ALERTS

| Command | Function | Auth | Override Silent |
|---------|----------|------|---|
| `/ring [seconds]` | Maximum-volume emergency alarm | ✅ | ✅ Yes |
| `/ring stop` | Silence alarm immediately | ✅ | - |
| `/message <text>` | Fullscreen urgent alert (bypasses lockscreen) | ✅ | - |

---

### 9. 🎭 DECEPTION & TRAP SYSTEMS

| Command | Function | Auth | Pro | Status |
|---------|----------|------|-----|--------|
| `/fakeshutdown` | Simulated power-off blackout (black screen, disabled power button) | ✅ | Yes | **FIXED** (timing) |
| `/wake` | Exit blackout deception, restore normal screen | ✅ | Yes | Works |
| `/duress_pin <4-8 digits>` | Configure emergency duress PIN trigger | ✅ | Yes | **FIXED** (timing) |
| `/trap on\|off\|status` | Anti-snatch, charger theft, pocket traps | ✅ | Yes | Forensic capture |
| `/shred <file_path>` | Cryptographic file destruction (secure deletion) | ✅ | Yes | DoD-standard |

**Fake Shutdown Features:**
- Power button disabled during blackout
- Touch detection captures covert photos
- Locked in kiosk mode (can't exit)
- Only `/wake` command releases (requires auth)
- Android 16 timing: 300ms lock delay + 500ms activity delay + retry logic

**Duress Features:**
- Hidden PIN trigger (different from master password)
- Activates emergency SOS, photo capture, GPS tracking
- Can send alerts to alternative contacts

---

### 10. 📊 TELEMETRY & SYSTEM INFORMATION

| Command | Function | Auth | Pro |
|---------|----------|------|-----|
| `/status` | Battery %, RAM, storage, uptime, screen state | No | No |
| `/info` | Hardware specs, OS version, security patch, display, kernel | No | No |
| `/network` | WiFi SSID, IP address, cellular signal strength | No | No |
| `/apps` | List all installed applications | No | No |
| `/app_uninstall <package>` | Uninstall application remotely | ✅ | No |
| `/selftest` | 9-point security audit (sensors, permissions, connectivity) | No | No |
| `/check_update` | Check for OTA app updates | No | No |
| `/update_confirm` | Install pending OTA update | ✅ | No |
| `/hide` / `/show` | Toggle app icon visibility in launcher | ✅ | No |

---

## 🔐 SECURITY FEATURES (Session Updates)

### Critical Fixes Applied (Session: Sept 22, 2026)

#### 1. Android 16 Timing Issues - RESOLVED ✅
**Duress PIN (`/set_os_pin`)**:
- Added 500ms delay post-credential verification
- 3-attempt retry with exponential backoff
- Token activation no longer race condition

**Lock Feature (`/lock`)**:
- 300ms delay after `dpm.lockNow()`
- 500ms delay after AlertMessageActivity launch
- 3-attempt lock task retry
- Keyguard PIN/Pattern/Biometric **DISABLED**

#### 2. Photo Evidence Capture - ENHANCED ✅
**Touch Detection → Telegram**:
- Screen touch detection: ✅ Logging
- Photo capture: ✅ 2-attempt retry + logging
- Telegram dispatch: ✅ 2-attempt retry with 500ms delay
- Location pinning: ✅ Auto-sent with photo

#### 3. Unlock System - REMOTE ONLY ✅
**Removed Features**:
- ❌ PIN entry screen (UI element deleted)
- ❌ Local PIN unlock
- ❌ Pattern unlock in Lost Mode
- ❌ Biometric unlock in Lost Mode

**Active Unlock Methods**:
- ✅ Telegram: `/unlock` command
- ✅ SMS: `PASA <master_password> /unlock`

#### 4. Information Disclosure - FIXED ✅
**Logcat Sanitization**:
- ✅ Phone numbers removed from SMS logs
- ✅ Message content removed from SMS logs
- ✅ Bot token info removed from logs
- ✅ Root detection logic corrected

---

## 🔑 AUTHENTICATION & ACCESS CONTROL

### Command Authentication Hierarchy

**Tier 1: No Auth Required**
- `/help`, `/status`, `/info`, `/network`, `/apps`, `/sim`, `/frozen`, `/selftest`, `/clipboard` (read-only), etc.

**Tier 2: Master Password Required**
- All sensitive commands: `/lock`, `/unlock`, `/wipe`, `/snap`, `/record`, `/livestream`, `/video`, etc.
- Pattern: `args[0]` = password, verified via `authManager.verifyMasterPassword()`

**Tier 3: SMS Commands (Dual Auth)**
- Format: `PASA <credential> /command [args]`
- Credential can be: 6-digit TOTP (preferred) OR Master Password (fallback)
- Rate limiting: 5 failed attempts = 15-minute lockout
- Example: `PASA 493021 /locate`

### Device Owner Permissions
- Activates via ADB: `adb shell dpm set-device-owner com.izhaanintellect.pasa/.admin.PasaDeviceAdmin`
- Commands requiring DO:
  - `/set_os_pin` (hardware PIN reset)
  - `/wipe` (factory reset)
  - `/antitamper` (safe boot lock)
  - `/usb_lock` (data signaling)
  - `/biometrics` (disable biometric entry)
  - `/dns` (system DNS)
  - Plus kiosk mode enhancements for `/lock`

---

## 📱 COMMUNICATION CHANNELS

### 1. Telegram Bot (Primary)
- Command interface: All 60+ commands
- File uploads: Photos, videos, audio, evidence
- Real-time notifications: Alerts, tamper detection, lock status
- Backend: Telegram Bot API + TelegramApi wrapper

### 2. SMS Commands (Offline Fallback)
- Format: `PASA <credential> /command [args]`
- Supported: `/locate`, `/status`, `/usb_lock`, `/biometrics`, `/lock`, `/unlock`, `/ring`, `/snap`, `/fakeshutdown`, `/wake`, etc.
- TOTP enrollment: `/smssetup` (6-digit rotating codes)
- Rate limiting: Built-in anti-brute-force (15-min lockout after 5 fails)
- Carrier: Works on any GSM network (SMS only, no data required)

### 3. Backend VPS (Optional)
- Server: izhaanintellect.fun
- Purpose: Command relay, media persistence option
- Configurable: User can choose Telegram direct OR VPS relay
- Security: Device ID + token-based auth

---

## 🎯 PROFESSIONAL FEATURES (PRO/TRIAL)

### PRO-Tier Commands
- **Livestream** (`/livestream`) - 5-30 min near-live video
- **Screen Recording** (`/screenrecord`) - MP4 video
- **Fake Shutdown** (`/fakeshutdown`) - Blackout deception
- **Duress PIN** (`/duress_pin`) - Hidden PIN trigger
- **Trap System** (`/trap`) - Anti-theft forensics
- **File Shred** (`/shred`) - Secure deletion
- **Tracking** (`/track`) - Continuous GPS
- **Geofence** (`/geofence`) - Safe zones

### TRIAL-Tier Commands
- `/video` - Camera recording
- `/screenshot` - Screen capture
- `/screen_burst` - Rapid frame montage
- `/record` - Audio recording

---

## 🌐 NETWORK & INFRASTRUCTURE

### Servers
1. **Telegram Bot API** (api.telegram.org)
2. **Backend VPS** (izhaanintellect.fun)
3. **Google Maps API** (maps.googleapis.com)
4. **Cell Tower Database** (towers.google.com)

### Network Security
- **Certificate Pinning**: Pending implementation (HIGH priority)
- **DNS-over-TLS**: `/dns` command locks system-wide
- **Private DNS**: `/dns` supports Quad9, Cloudflare, AdGuard
- **USB Data Control**: `/usb_lock` disables data pins (Android 12+)

---

## 📊 STATISTICS

| Metric | Count |
|--------|-------|
| Total Commands | 60+ |
| Device Owner-Only | 11 |
| SMS-Compatible | 15+ |
| Pro-Tier Features | 8 |
| Trial-Tier Features | 4 |
| Commands Requiring Auth | 50+ |
| Read-Only Commands | 10+ |

---

## 🛠️ TECHNICAL COMPONENTS

### Activities
1. **SetupActivity** - Initial configuration
2. **AlertMessageActivity** - Lost Mode lockscreen (remote-only unlock)
3. **StealthCaptureActivity** - Hidden camera/video capture
4. **FakeShutdownActivity** - Blackout deception screen
5. **EscrowActivationActivity** - Hardware PIN token enrollment (fixed timing)
6. **DuressUnlockActivity** - Duress trigger & emergency unlock

### Services
1. **PasaService** - Foreground guardian (cameras, mic, location, data)
2. **AccessibilityScreenCaptureService** - Screenshot via accessibility API

### Receivers
1. **PasaDeviceAdmin** - Enterprise device management
2. **BootReceiver** - Auto-start on device boot
3. **SmsCommandReceiver** - SMS command processing (PASA format)
4. **PasaWatchdogReceiver** - Android 14-16 resurrection

### Managers
1. **AuthManager** - Master password verification
2. **PreferencesManager** - Settings persistence
3. **CommandExecutor** - Command dispatch
4. **LocationTracker** - GPS + cached location
5. **AudioRecorderManager** - Microphone recording
6. **StealthVideoManager** - Headless video capture
7. **TelegramApi** - Bot command relay
8. **PasaBackendApi** - VPS relay (optional)

---

## ⚠️ SECURITY ADVISORIES

### Resolved Issues
1. ✅ **DeadDropCommand** - Disabled (non-functional feature)
2. ✅ **Root Detection** - Fixed (now checks actual `/system` paths)
3. ✅ **Logcat Leakage** - Phone numbers & SMS content removed
4. ✅ **Bot Token Exposure** - Error messages sanitized
5. ✅ **Android 16 Timing** - Delays + retry logic implemented
6. ✅ **PIN Unlock Bypass** - Keyguard features disabled in Lost Mode
7. ✅ **Telegram Dispatch** - Retry logic added, photos now sent reliably

### Pending Improvements
1. ⏳ **Certificate Pinning** - Not yet implemented (HIGH priority)
2. ⏳ **Auth on Remaining 11 Commands** - Some commands still missing password checks

---

## 📋 CHECKLIST: CURRENT STATE (v3.5.1+)

- [x] 60+ commands implemented
- [x] Device Owner kiosk mode
- [x] Telegram & SMS control
- [x] Forensic capture (photos, video, audio)
- [x] GPS + cell tower tracking
- [x] Tamper detection & hardening
- [x] Lost Mode Guard (remote-only unlock)
- [x] Android 16 timing fixes
- [x] Pin entry screen removed
- [x] Keyguard PIN/pattern/biometric disabled
- [x] Photo evidence capture to Telegram
- [x] Logcat information disclosure fixed
- [ ] Certificate pinning
- [ ] Authentication on final 11 commands

---

## 🎯 DEPLOYMENT NOTES

**Supported Android Versions**: Android 8.0+ (API 26+)  
**Device Owner Required For**: Hardware PIN reset, factory reset defense, USB data control, comprehensive lockdown  
**Permissions**: 40+ declared (location, camera, microphone, SMS, contacts, network, etc.)  
**Storage**: ~15-20MB app size + media cache  
**Network**: Requires internet for Telegram, optional for SMS-only mode  
**Backend**: Telegram bot token required (VPS optional)

---

**Report Generated**: September 22, 2026  
**Analysis Completeness**: 100% (All 60+ commands documented)  
**Status**: Production Ready for Deployment
