# 🛡️ PASA Sentinel (Private Android Security Agent) — Master Technical FAQ & Defense Field Guide

> **Version:** v3.7.17 (Build 82)  
> **Target OS:** Android 8.0 – Android 16 (API 26 – 36, compileSdk 36, targetSdk 36)  
> **Defense Philosophy:** Sovereign Zero-Data Direct-to-Telegram • Knox-Grade Device Owner • Dual-Channel C2 (Telegram + TOTP Air-Gapped SMS) • Hardware Escrow Tokens • Anti-EDL/BROM Defense • Cyber Defense Suite  
> **Language:** English | [বাংলা সংস্করণ (Bengali Version)](FAQ.md)

---

## 📑 Table of Contents

1. [General & Fundamental Concepts](#1-general--fundamental-concepts)
2. [Privacy, Zero-Storage & Data Security](#2-privacy-zero-storage--data-security)
3. [Knox Device Owner & Anti-Uninstall Persistence](#3-knox-device-owner--anti-uninstall-persistence)
4. [Theft Scenarios, Physical SIM & eSIM Defense](#4-theft-scenarios-physical-sim--esim-defense)
5. [Extreme Hardware, EDL 9008, BROM & Lab Attacks](#5-extreme-hardware-edl-9008-brom--lab-attacks)
6. [Hardware Escrow Token & Screen Unlock](#6-hardware-escrow-token--screen-unlock)
7. [Covert Forensics, Camera, Audio & Screen Capture](#7-covert-forensics-camera-audio--screen-capture)
8. [Shadow App Vault, File Extraction & Network Firewall](#8-shadow-app-vault-file-extraction--network-firewall)
9. [Air-Gapped Cellular SMS Fallback & TOTP](#9-air-gapped-cellular-sms-fallback--totp)
10. [Autonomous Sensor Traps & Deception](#10-autonomous-sensor-traps--deception)
11. [Battery, Performance & Compatibility](#11-battery-performance--compatibility)
12. [Advanced Mobile Cyber Defense Suite](#12-advanced-mobile-cyber-defense-suite)
13. [Legal Legitimacy, Police Compliance & Licensing](#13-legal-legitimacy-police-compliance--licensing)

---

## 1. General & Fundamental Concepts

### Question 1.1: Why do I need PASA Sentinel when Google "Find My Device" or Samsung "SmartThings" already exist?
**Answer:**  
Google Find My Device and Samsung SmartThings depend entirely on an active internet connection and standard user-space Android permissions. A professional street thief acts within the first **5 to 10 seconds** of snatching or stealing a device:
1. They swipe down the lockscreen notification shade to toggle **Airplane Mode** on or immediately pop out the SIM card. The instant mobile data or Wi-Fi drops, Google and Samsung trackers become completely blind and useless.
2. They hold down the physical power button to shut down or reboot the device.

**How PASA Sentinel neutralizes this vulnerability:**
- **Lockscreen Status Bar Lockout:** Completely disables dragging down the notification shade or Quick Settings panel while locked (`setStatusBarDisabled(true)`) and prohibits toggling Airplane Mode at the OS level (`DISALLOW_AIRPLANE_MODE`).
- **Air-Gapped Cellular SMS Fallback:** Even when internet data is cut, the cellular radio stays connected to nearby cell towers. Sending an encrypted SMS command powers up the hardware GPS receiver chip and texts back exact Google Maps pin coordinates directly from satellite constellations.
- **Auto Power-Menu Fake Shutdown Trap:** When a thief attempts to hold down the power button to turn off the phone, PASA intercepts the power dialog, simulates an OEM shutdown animation, and drops display brightness to a 0-nit black canvas. The thief believes the phone is powered off, while background services covertly capture front-camera mugshots and live GPS coordinates.

---

### Question 1.2: There are many anti-theft apps on Google Play Store. How does PASA Sentinel differ?
**Answer:**  
Play Store anti-theft apps run inside standard third-party app sandboxes. Due to Google Play developer policies, they cannot access low-level OS supervisory controls. Thieves easily neutralize them by booting into "Safe Mode" or simply revoking permissions. Furthermore, commercial trackers (Life360, Prey, Cerberus) continuously collect and store your real-time GPS tracks, personal photos, and call logs on their corporate cloud servers while billing recurring annual subscriptions.

PASA Sentinel operates on an **Android Enterprise Device Owner (Knox-Grade)** architecture. It executes with supervisor privileges directly under the Linux kernel, preventing uninstallation, deactivation, or safe mode bypass. Most importantly, it operates under a strict **Zero-Storage Sovereign Privacy Model**—no user media or location history ever touches any corporate cloud server.

---

### Question 1.3: Is PASA Sentinel available on the Google Play Store? Is it safe if it isn't?
**Answer:**  
No, PASA Sentinel is intentionally distributed outside Google Play Store because:
1. Google Play policies forbid apps from implementing OS lockscreen overrides, USB data pin hardware killswitches, silent headless camera captures, uninstallation graying, or SMS telephony filtering.
2. PASA adheres to a sovereign zero-telemetry architecture that avoids Big-Tech tracking libraries.

PASA's source code is 100% open-source and auditable on GitHub ([rbr48/pasa](https://github.com/rbr48/pasa)). It is compiled with zero adware, zero analytics trackers, and passes VirusTotal with clean verdicts across 65+ leading antivirus engines.

---

### Question 1.4: Does PASA Sentinel require rooting the phone?
**Answer:**  
**No root permissions are required (Zero Root Required).**  
Rooting actually compromises Android's hardware security architecture (Knox, TEE, StrongBox KeyStore). PASA operates using Google's official, enterprise-grade **Android Enterprise Device Owner** API. By connecting your device to a computer via USB cable just once and executing our automated 1-click wizard, PASA is granted supervisor authority recognized by the Android OS itself—providing security deeper and more stable than root.

---

## 2. Privacy, Zero-Storage & Data Security

### Question 2.1: The app accesses camera, microphone, and GPS sensors. Can PASA developers or third parties spy on me?
**Answer:**  
**Never. This is our non-negotiable "Strategy 1: Direct-to-Telegram Zero-Storage" architecture.**

Unlike commercial platforms, PASA maintains zero central cloud databases for surveillance media:
1. Whenever a photo (`/snap`), video (`/video`), screenshot (`/screenshot`), or ambient audio wiretap (`/record`) is captured, it is encrypted and streamed directly from device RAM to your private Telegram Bot API endpoint (`api.telegram.org`).
2. The instant delivery completes (or fails), temporary local cache buffers are overwritten using cryptographic multi-pass shredding and deleted immediately.
3. The PASA control server acts purely as an ephemeral signaling gate (RAM-only buffers) and never writes surveillance media or GPS tracks to physical disk.

---

### Question 2.2: How secure is my data on Telegram?
**Answer:**  
All Telegram Bot API communications are secured via TLS 1.3 encryption. Only the unique Telegram Chat ID configured during initial device provisioning is authorized to execute commands. Messages from any other Telegram account are instantly rejected (`⛔ Access Denied`). With Telegram Two-Factor Authentication (2FA) enabled on your personal account, unauthorized actors cannot access your C2 console.

---

### Question 2.3: If the phone is offline when evidence is captured, will photos appear in the Gallery where a thief could see them?
**Answer:**  
No. When the phone is offline, captured evidence is never written to public shared storage (`/sdcard/DCIM` or `/sdcard/Pictures`). Files reside strictly inside PASA's private, encrypted internal sandbox database (Room with SQLCipher / Tink AES-256 GCM). They remain completely invisible to file managers and gallery apps, and are automatically transmitted and shredded the moment internet connectivity is restored.

---

### Question 2.4: What is the Sovereign Zero-Data Architecture? Do my botToken and chatId leave my phone?
**Answer:**  
**No, they never leave your device.**  
Starting in version v3.6.0+, PASA Sentinel operates in strict **Sovereign Mode**:
1. Your private Telegram `botToken` and `ownerChatId` are stored exclusively inside hardware-backed `EncryptedSharedPreferences`. They are never transmitted to PASA's servers.
2. The Android client polls the official Telegram Bot API (`api.telegram.org`) directly from the handset.
3. License activations and periodic status verifications transmit only an irreversible, salted cryptographic hash (`SHA256(deviceId:key)`). No IMEI, phone number, or raw device identity is ever exposed to our control plane.

---

## 3. Knox Device Owner & Anti-Uninstall Persistence

### Question 3.1: Can a thief open Settings and uninstall or force-stop PASA Sentinel?
**Answer:**  
**Impossible.**  
Because PASA Sentinel is provisioned as an Android Enterprise Device Owner, the OS permanently grays out the "Uninstall", "Disable", and "Force Stop" buttons in System Settings. In App Info, the system displays: *"Managed by your organization"*. Any attempt to remove or disable the package is blocked by the Linux kernel.

---

### Question 3.2: Can a thief reboot into Safe Mode to bypass PASA Sentinel?
**Answer:**  
No. When PASA's anti-tamper policy (`/antitamper on`) is armed, it enforces the OS-level `DISALLOW_SAFE_BOOT` restriction. Even if the thief holds the physical volume and power buttons during boot, the system kernel will bypass Safe Mode and boot directly into standard protected operation.

---

### Question 3.3: How does the legitimate owner uninstall the app if they decide to remove it?
**Answer:**  
The legitimate owner can remove the application at any time through authorized administrative channels:
1. Sending the verified admin command `/app_uninstall <MasterPIN>` via Telegram or authenticated SMS.
2. Removing Device Owner status from a connected computer via ADB:
   ```bash
   adb shell dpm remove-active-admin com.izhaanintellect.pasa/.admin.PasaDeviceAdmin
   ```
Once administrative privileges are released, PASA uninstalls like any standard Android application.

---

### Question 3.4: Is provisioning Device Owner mode complicated for non-technical users?
**Answer:**  
**Not at all.**  
While Device Owner previously required manual command-line execution, we provide the **PASA Windows Setup Kit (1-Click Guided Wizard)**:
- No coding or manual command typing required.
- Extract the zip file on a Windows computer and double-click `PASA Device Owner Setup.bat`.
- The wizard automatically downloads official Google ADB platform-tools, verifies USB debugging, guides you through removing secondary accounts, and activates Device Owner mode in under 2 minutes.

---

## 4. Theft Scenarios, Physical SIM & eSIM Defense

### Question 4.1: What happens if a thief ejects the physical SIM card immediately after snatching the device?
**Answer:**  
PASA features an **Autonomous Physical SIM Ejection Trap (`/sim_lock`):**
1. The moment the SIM tray is pulled (within 1 second of hardware interrupt), PASA immediately engages a Knox Kiosk Lost Mode lockdown.
2. The notification shade, navigation buttons, power menu, and USB data connections are locked down.
3. Even without cellular data or Wi-Fi, the internal GNSS satellite receiver hardware is forcibly powered on (`dpm.setLocationEnabled(true)`), capturing mugshots and buffering coordinates.

---

### Question 4.2: What if the thief inserts their own SIM card into the phone?
**Answer:**  
This is a fatal mistake for the thief!
- Upon detecting an unauthorized foreign SIM card, PASA silently transmits an emergency outbound SMS via `SmsManager` to the owner's pre-configured emergency phone number (`/sim_lock phone <number>`).
- The alert contains the device IMEI, active carrier details, and a live Google Maps satellite fix.
- **The decisive breakthrough:** When the SMS lands on the owner's phone, **the thief's mobile phone number is displayed directly on Caller ID**. The thief's identity is unmasked, and the owner can now send air-gapped SMS C2 commands directly to that number.

---

### Question 4.3: If my device uses an eSIM, can a thief delete or disable the eSIM profile from Settings?
**Answer:**  
No. PASA enforces `UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS` at the system level. Access to cellular configuration menus is prohibited, making it impossible to erase eSIM profiles or toggle cellular radio states while locked.

---

### Question 4.4: If there is no SIM card and Wi-Fi is disabled, how can I control the phone remotely?
**Answer:**  
If a secondary eSIM is installed or cellular tower connectivity is present, PASA's **Air-Gapped Cellular SMS Fallback** responds to SMS commands sent from any standard feature phone:
- `PASA <PIN> /locate` — Activates GPS and texts back Google Maps coordinates.
- `PASA <PIN> /lock` — Forcibly locks the device keyguard.
- `PASA <PIN> /ring 60` — Sounds a maximum-volume audible alarm for 60 seconds.
- `PASA <PIN> /wipe` — Triggers an irrecoverable cryptographic factory reset.

---

### Question 4.5: What is the Cryptographic SIM Tray Lock (`/sim_tray_lock`)?
**Answer:**  
This represents PASA's deepest hardware lockdown tier. When armed and an unauthorized SIM card is inserted:
1. **Automated Lockscreen Rotation:** PASA resets the OS lockscreen credentials to a secret 8-digit random PIN using Knox hardware escrow tokens (`resetPasswordWithToken`). This PIN is transmitted exclusively to the owner's Telegram bot.
2. **Total Application Suspension:** Suspends all third-party applications at the OS level (`dpm.setPackagesSuspended`). The phone becomes a completely unresponsive brick—zero apps can be launched.
3. **PASA Isolation:** PASA itself is excluded from suspension, remaining 100% operational to transmit satellite GPS beacons and mugshots. The phone can only be restored when the owner issues `/sim_tray_lock release`.

---

## 5. Extreme Hardware, EDL 9008, BROM & Lab Attacks

### Question 5.1: What if a thief attempts a hard factory reset from Android Recovery Mode?
**Answer:**  
PASA defends against recovery exploits across three distinct tiers:
1. **Boot Hardening:** Prohibits factory resets via `DISALLOW_FACTORY_RESET` and blocks hardware button combinations via `/harden_boot`.
2. **Factory Reset Protection (FRP):** Even if wiped via custom hardware tools, Android's cryptographic FRP lock engages immediately upon reboot, preventing device setup without the original Google account credentials.
3. **Samsung Knox Persistence:** On Knox-enabled devices, Device Owner policies persist across system resets.

---

### Question 5.2: What if an attacker connects the phone to computer forensics tools (Cellebrite, GrayKey, BadUSB)?
**Answer:**  
PASA features an active **Hardware USB Data Pin Killswitch (`/usb_lock`):**
- Utilizing Android 12+ (API 31+) Device Owner controls, PASA physically disables USB data signaling pins on the controller chip (`setUsbDataSignalingEnabled(false)`).
- The device draws AC power to charge normally, but all data communication (MTP, ADB, PTP) is disconnected at the physical layer. Forensic extraction boxes fail to detect any connected device.

---

### Question 5.3: What if an attacker disassembles the phone to short motherboard test points for Qualcomm EDL (9008) or MediaTek BROM flashing?
**Answer:**  
*Technical reality:* Emergency Download Mode (EDL 9008) and BootROM (BROM) operate directly at the silicon SoC layer before the Android Linux kernel boots. No software application can execute commands inside bare-metal boot ROMs.

**However, PASA neutralizes this threat vector using three proactive counter-measures:**
1. **Heat-Gun Thermal Anomaly Trap (`/thermal`):**  
   To access motherboard test points, technicians must use a heat gun ($80^\circ\text{C}-100^\circ\text{C}$) to soften rear panel adhesives. PASA continuously monitors internal battery thermal sensors. If temperatures exceed 48°C while locked, PASA immediately kills USB data pins, engages kiosk lock, captures front-camera mugshots of the technician, and broadcasts an emergency SOS.
2. **Permanent OEM Bootloader Lockout:**  
   Device Owner permanently enforces `setOemUnlockAllowed(component, false)`. Fastboot flashing commands are rejected by the bootloader (`FAILED: Flashing Unlock is not allowed`).
3. **Dead Man's Switch Auto-Destruct (`/deadman`):**  
   Before lab disassembly, thieves must keep the phone offline. If PASA receives no heartbeat or unlock within a configurable window (e.g., 6 hours), the system executes an automated cryptographic wipe (`dpm.wipeData(0)`). When test points are finally shorted, zero bytes of user data remain on the UFS/eMMC storage chips.

---

### Question 5.4: What if the phone is placed in a signal-blocking Faraday bag?
**Answer:**  
This is precisely why PASA includes the **Autonomous Dead Man's Switch (`/deadman`):**
- It operates completely independently of external network connectivity using an internal hardware countdown timer.
- If the device remains isolated from owner heartbeats and unlock events for the configured duration (e.g., 6 or 12 hours), PASA confirms device seizure and executes an irreversible cryptographic storage wipe.

---

### Question 5.5: Can the Dead Man's Switch accidentally wipe my phone during normal everyday use?
**Answer:**  
**Never.**  
Every time you unlock your phone with your fingerprint, face, or PIN (`ACTION_USER_PRESENT`), the countdown timer resets to zero. Routine Telegram commands, SMS interactions, or `/deadman heartbeat` also refresh the timer. It only trips if the device is stolen, locked, and completely isolated for extended periods.

---

## 6. Hardware Escrow Token & Screen Unlock

### Question 6.1: Android 14, 15, and 16 deprecated `resetPassword()`. How does PASA change the lockscreen PIN remotely?
**Answer:**  
Google deprecated legacy password reset APIs for basic device administrators, but introduced **Cryptographic Hardware Escrow Tokens** for enterprise Device Owners:
1. During setup, PASA generates a secure 32-byte cryptographic random token stored in encrypted storage.
2. Upon the first physical unlock by the owner, Android's hardware security module (StrongBox / TEE) enrolls this token (`setResetPasswordToken`).
3. When you issue `/set_os_pin <new_pin>` via Telegram or SMS, PASA passes this hardware escrow token to overwrite the OS lockscreen credentials (`resetPasswordWithToken`) instantly without wiping data.

---

### Question 6.2: What happens if an intruder attempts to guess my pattern or PIN?
**Answer:**  
PASA's **Failed Pattern Guard (`/pattern_guard`):**
- After 3 consecutive incorrect attempts on the lockscreen, the front camera silently captures a high-resolution photo of the perpetrator.
- The photo is transmitted immediately to your Telegram bot along with the exact timestamp and attempt count.

---

## 7. Covert Forensics, Camera, Audio & Screen Capture

### Question 7.1: Will the camera flash, play shutter sounds, or flicker the screen when capturing photos?
**Answer:**  
**Completely silent and invisible (Zero-Blackout Headless CameraX Architecture):**
1. **Headless Execution:** Bypasses UI activities; CameraX binds directly to a background `ServiceLifecycleOwner`.
2. **Zero Screen Flicker:** The display does not flicker, dim, or wake up.
3. **Hardware Muted:** System camera shutter sounds are muted at the HAL layer, and the LED flashlight remains off.

---

### Question 7.2: Can PASA record ambient audio wiretaps or live streams?
**Answer:**  
Yes:
- `/record <seconds>` — Silently records high-fidelity ambient audio (16kHz AAC) via device microphones and delivers it as an audio message on Telegram.
- `/video front|back <seconds>` — Records stealth video clips without displaying a viewfinder.
- `/livestream [front|back]` — Transmits continuous video frames to your private Telegram bot, functioning as a real-time surveillance feed.

---

### Question 7.3: How can I locate my phone if it is trapped in an underground basement where satellite GPS cannot reach?
**Answer:**  
PASA's **Dual-SIM Cell Tower Triangulation (`/tower`):**
- Even without satellite line-of-sight, the cellular modem communicates with surrounding mobile towers.
- The `/tower` command scans active and neighboring 4G/5G/LTE cell towers across both SIM slots (MCC, MNC, LAC/TAC, Cell ID, signal strength in dBm) to enable accurate indoor localization.

---

## 8. Shadow App Vault, File Extraction & Network Firewall

### Question 8.1: Can an attacker access my banking, crypto, or private messaging apps if coerced to unlock my phone?
**Answer:**  
No. PASA includes a **Shadow App Vault (`/freeze` / `/lock_app`):**
- Sending `/freeze bkash` or `/lock_app binance` via Telegram or SMS instructs Device Owner to hide the package (`setApplicationHidden(pkg, true)`).
- The application vanishes instantly from the launcher, app drawer, system settings, and running process tables without losing data.
- Sending `/unfreeze` restores the app and all its data intact.

---

### Question 8.2: Can I remotely extract important documents or recent gallery photos from my lost device?
**Answer:**  
Yes, PASA provides full remote forensic storage extraction:
- `/gallery_latest 5` — Dispatches the 5 most recent photos from the camera roll directly to Telegram.
- `/getfile /sdcard/Documents/passwords.txt` — Uploads any specific file as a Telegram document (up to 50 MB).
- `/list_files /sdcard/Download` — Browses directory hierarchies remotely.
- `/shred <path>` — Permanently destroys sensitive files using multi-pass cryptographic overwriting.

---

### Question 8.3: What is the App Network Isolation Firewall (`/app_firewall`)?
**Answer:**  
If you suspect spyware, RATs, or compromised apps are active on your device, `/app_firewall block <package>` cuts all inbound and outbound network sockets for that specific package at the system level, preventing data exfiltration.

---

### Question 8.4: How do Permanent Notification Suppression (`/notification`) and Encrypted DNS (`/dns`) work?
**Answer:**  
1. **Permanent Notification Suppression (`/notification hide`):** On Android 13+ (API 33+), Device Owner permanently revokes notification permissions (`PERMISSION_GRANT_STATE_DENIED`). The foreground service remains 100% active while removing all visible icons from the status bar and lockscreen.
2. **System-Wide Encrypted DNS (`/dns`):** Enforces DNS-over-TLS (DoT) globally across all cellular and Wi-Fi connections (Quad9, Cloudflare, AdGuard), preventing ISP snooping and rogue captive portal hijacking.

---

## 9. Air-Gapped Cellular SMS Fallback & TOTP

### Question 9.1: What is the syntax for SMS commands, and can an attacker spoof SMS messages to control my phone?
**Answer:**  
Unauthorized third parties cannot execute SMS commands. Every command requires cryptographic authentication:
```text
PASA <MasterPIN_or_TOTP> <command>
```
Examples:
- `PASA 5892 /locate` (Fetch satellite GPS fix)
- `PASA 5892 /status` (Inspect battery, lock, and security health)
- `PASA 5892 /usb_lock on` (Disable physical USB data pins)
- `PASA 5892 /camera_lock on` (Disable hardware cameras)
- `PASA 5892 /ring 60` (Sound max-volume alarm for 60 seconds)
- `PASA 5892 /fakeshutdown` (Engage 0-nit fake shutdown)
- `PASA 5892 /deadman status` (Inspect auto-destruct countdown)

Sending 3 consecutive incorrect PINs temporarily blacklists the originating number.

---

### Question 9.2: How does TOTP (Time-based One-Time Password) SMS fallback work?
**Answer:**  
PASA implements RFC 6238 TOTP authentication (compatible with Google Authenticator). Generating a QR code via `/smssetup` allows you to authorize offline SMS commands using dynamic 6-digit rolling codes (`PASA 419582 /locate`). Even if an adversary observes your SMS command, the code expires within 30 seconds and cannot be replayed.

---

## 10. Autonomous Sensor Traps & Deception

### Question 10.1: How does PASA detect a thief snatching the phone from my hand?
**Answer:**  
PASA features an integrated **Kinetic Snatch Detection Trap:**
- The 3-axis accelerometer continuously evaluates vector magnitude: $\sqrt{x^2 + y^2 + z^2} > 26.0\text{ m/s}^2$ (~2.65G acceleration).
- Sudden kinetic jerks caused by street snatchers trigger an instant screen lock, engage Knox Kiosk mode, snap a front-camera selfie, and broadcast an emergency alert to Telegram.

---

### Question 10.2: How do Fake Shutdown and the Auto Power-Menu Trap operate?
**Answer:**  
Street thieves instinctively hold the physical power button to switch off stolen devices. PASA's **Auto Power-Menu Interception Trap (`/fakeshutdown auto on`)** counters this behavior:
1. **Power Menu Interception:** When the power button is held on the lockscreen, PASA intercepts and dismisses the OEM power dialog before it can be tapped.
2. **0-Nit Black Canvas:** The phone displays an authentic OEM shutdown animation and drops screen brightness to 0 nits with fullscreen kiosk flags.
3. **Covert Surveillance:** The device appears completely dead. Any tap on the screen or button silently triggers front-camera mugshots and satellite GPS tracking sent to Telegram.
4. **Restoration:** Sending `/wake` from Telegram or tapping a secret multi-touch sequence restores full display brightness.

---

### Question 10.3: What if an attacker forces me to unlock the device at gunpoint or under physical duress?
**Answer:**  
PASA provides two defense layers:
1. **Biometric Killswitch (`/biometrics off`):** Disables fingerprint and face unlock remotely, forcing complex alphanumeric passphrase authentication.
2. **Decoy Duress PIN:** If forced to unlock under duress, enter your pre-configured Duress PIN instead of your real PIN.
   - The device unlocks into a **Sterile Sandbox Decoy OS**.
   - All banking, cryptocurrency, and confidential messaging apps vanish instantly via `setApplicationHidden`.
   - The front camera silently captures the coercer's face and transmits an emergency SOS distress signal to Telegram.

---

### Question 10.4: How does the Pocket & Bag Extraction Trap work?
**Answer:**  
PASA's **Pocket Trap (`/trap pocket on`):**
- When the device is locked in a pocket or bag, proximity sensors register an obstructed state.
- Once removed, a 5-second grace period begins. If the legitimate owner does not unlock the device within 5 seconds, PASA confirms unauthorized extraction, engages kiosk lock, captures a photo, and sounds the alarm.

---

## 11. Battery, Performance & Compatibility

### Question 11.1: Will PASA Sentinel drain my device battery?
**Answer:**  
**No.**  
PASA is engineered to comply with Android's standard power-saving policies:
- In idle states, PASA respects Android Doze Mode and remains in a low-power sleep state.
- Sensor traps use hardware interrupts and acquire micro-wake locks only upon specific physical events (charger disconnect, SIM pull, kinetic snatch).
- Typical daily battery consumption averages **1% to 1.5%**.

---

### Question 11.2: Will OEM aggressive battery managers (MIUI/HyperOS, ColorOS, Funtouch, OneUI) kill PASA in the background?
**Answer:**  
No, due to three architectural guarantees:
1. **Device Owner Immunity:** As an enterprise Device Owner, third-party OEM battery killers are restricted by the Android OS from terminating the supervisor process.
2. **Foreground Service Daemon:** PASA maintains an active foreground service with persistent notification channels.
3. **Watchdog Auto-Resurrection:** If killed due to extreme memory pressure, AlarmManager and direct-boot receivers resurrect the daemon within 5 seconds.

---

## 12. Advanced Mobile Cyber Defense Suite

### Question 12.1: What is the Accessibility Trojan Shield (`/a11y_shield`)?
**Answer:**  
Modern Android banking trojans abuse Accessibility Services to capture keystrokes, record screens, and steal OTP codes. PASA Sentinel continuously audits active accessibility bindings at the OS level, alerting the owner and blocking unauthorized third-party accessibility exploitation.

---

### Question 12.2: How does the Locked-State USB Auto-Killswitch (`/usb_autolock`) operate?
**Answer:**  
Physical data extraction attacks occur primarily while devices are locked. When `/usb_autolock on` is active, locking the screen immediately disables USB data signaling pins. Forensic extraction boxes cannot access device storage until the owner physically unlocks the device.

---

### Question 12.3: What is the Crypto Clipper Trap (`/clipper_guard`)?
**Answer:**  
Clipper malware monitors the system clipboard to replace copied cryptocurrency wallet addresses (Bitcoin, Ethereum, Solana, USDT) with attacker addresses. PASA's Clipper Guard monitors clipboard operations, alerts on address hijacking, and neutralizes clipboard tampering.

---

### Question 12.4: How does Sideload & App Install Lockdown (`/app_install_lock`) protect the device?
**Answer:**  
Enforces `DISALLOW_INSTALL_APPS` and `DISALLOW_INSTALL_UNKNOWN_SOURCES`. All APK sideloading and rogue package installations are blocked at the package manager level, neutralizing physical drive-by malware installations.

---

### Question 12.5: What is the Ransomware Canary Tripwire Guard (`/canary_guard`)?
**Answer:**  
PASA places hidden canary decoy files across internal and external storage volumes. If mobile ransomware attempts to encrypt or alter files, touching a canary file immediately trips the alert, kills malicious processes, locks the device, and notifies the owner via Telegram.

---

### Question 12.6: How does the 2FA / OTP Interception Guard (`/otp_guard`) protect accounts?
**Answer:**  
Monitors SMS and notification channels at the OS level to ensure unauthorized third-party applications cannot read, forward, or scrape sensitive 2-factor authentication codes and banking OTPs.

---

### Question 12.7: Why is the Anti-2G / IMSI Catcher Shield (`/anti_2g`) necessary?
**Answer:**  
Adversaries use portable cell-site simulators (Stingrays / IMSI catchers) to force smartphones down to unencrypted 2G (GSM) networks to eavesdrop on calls and intercept SMS messages. On Android 12+, PASA permanently disables 2G cellular radio hardware (`setCellular2gDisabled(true)`), keeping the device strictly on encrypted 4G and 5G networks.

---

### Question 12.8: How do Remote Outbound Calling and Direct SMS (`/call` and `/sendsms`) work?
**Answer:**  
Using verified Telegram C2 commands, you can remotely instruct the phone to place outbound phone calls (`/call <number> sim1 speaker`) or transmit direct SMS messages (`/sendsms sim2 <number> <message>`) with explicit dual-SIM slot selection. This is invaluable for verifying unknown SIM phone numbers via caller ID.

---

### Question 12.9: What is the Covert Tactile Device Locator (`/vibrate_pulse`)?
**Answer:**  
If a stolen phone is suspected to be nearby in a crowded public venue, sounding an audible siren might alert the thief to flee or destroy the device. The tactile locator triggers discrete, silent vibration patterns (pulse, SOS Morse code, continuous) to help locate the phone covertly.

---

## 13. Legal Legitimacy, Police Compliance & Licensing

### Question 13.1: Can evidence gathered by PASA Sentinel be submitted to law enforcement for official investigations?
**Answer:**  
**Yes, 100% legally and effectively.**  
PASA Sentinel is an authorized device defense tool protecting personal property. Forensic evidence packages delivered to Telegram include:
1. Device hardware IMEI and serial numbers.
2. Timestamped GNSS satellite coordinates with Google Maps links.
3. Connected cellular tower IDs (Cell ID and LAC).
4. High-resolution front-camera perpetrator mugshots.
5. Perpetrator cellular phone numbers captured via foreign SIM insertion.

Presenting these records to cyber crime investigators or law enforcement provides actionable intelligence to track perpetrators and recover stolen hardware.

---

### Question 13.2: Is PASA Sentinel a monthly subscription?
**Answer:**  
No recurring subscription fees.
- **Lifetime Pro License:** A one-time license purchase grants lifetime access to all core features, security patches, and future updates.
- Licenses are signed with Ed25519 asymmetric cryptography and verify offline without requiring internet access.
- **Customer Self-Service Portal ([https://pasa.izhaanintellect.fun/portal](https://pasa.izhaanintellect.fun/portal)):** Customers can log in anytime to inspect license states and perform Zero-Touch QR code activations in under 10 seconds.

---

### Question 13.3: What if I encounter issues? Is there a refund guarantee?
**Answer:**  
Yes. We offer an unconditional **7-Day Money-Back Guarantee** backed by dedicated technical support. Refunds via Binance Pay or local payment gateways are processed promptly upon request.

---

### 📞 Contact & Official Channels
- **Official Website & Portal:** [https://pasa.izhaanintellect.fun](https://pasa.izhaanintellect.fun)
- **Customer Self-Service Portal:** [https://pasa.izhaanintellect.fun/portal](https://pasa.izhaanintellect.fun/portal)
- **Telegram Support Bot:** `@Pas_agent_bot`
- **GitHub Open-Source Repository:** [rbr48/pasa](https://github.com/rbr48/pasa)
- **Chief Developer & Security Architect:** M S Rana
