# 🛡️ PASA Sentinel — Agent Directives & Architecture

Please see [GEMINI.md](file:///e:/Projects/PrivateApp/GEMINI.md) for the comprehensive technical specification, file map, and operational rules.

### Quick Reference:
- **Zero-Storage Rule:** Never write surveillance media or GPS coordinates to server disk. All user evidence is direct-to-Telegram and shredded immediately from device RAM/cache.
- **Android Target:** Android 8.0 – 16 (API 26 – 36, compileSdk 36, targetSdk 36).
- **Current Production Build:** v3.5.9 (Build 55), signed with `pasa-release-key.jks` (Scheme v2).
- **Core Modules:**
  - Android client: `app/src/main/java/com/izhaanintellect/pasa/`
  - VPS server: `pasa-server/` (Node.js 22, SQLite WAL mode, PM2 process `pasa-server` (ID 27) on `148.135.137.245:2222`)
  - Web landing: `pasa-commercial-web/` (Docker container `pasa-commercial-app` on port `8165`, Nginx serving `https://pasa.izhaanintellect.fun/`)
  - Release binaries: `releases/` (`pasa-v3.5.9-55.apk`, `pasa-latest.apk`, `PASA-Device-Owner-Setup-Kit.zip`)
  - Setup kit: `pasa-setup-kit/` (Windows Device Owner provisioning wizard)
- **Security & C2:** Knox Device Owner (`LOCK_TASK_FEATURE_NONE`), Hardware Escrow Tokens, 87 Telegram C2 commands with 6-Hub Interactive Command Console, Cryptographic SIM Tray Lock (`/sim_tray_lock`), Anti-EDL/BROM Dead Man's Switch (`/deadman`), Heat-Gun Thermal Anomaly Trap (`/thermal`), Permanent OEM Bootloader Flashing Lockout Hardening, Remote Outbound Calling with Dual-SIM Selection (`/call <number> sim1|sim2`), Unified Extraction Pagination, Instant Document Exports & Search (`/contacts`, `/sms_log`, `/call_log`, `/history`, `/list_files`), Smart App Lockout (`/lock_app`, `/unlock_app`), Storage & Camera Roll Extraction (`/gallery_latest`, `/getfile`), Air-Gapped SMS Fallback Guide (`/sms_help`), Permanent Notification Suppression (`/notification`), SIM & Cellular Carrier RF Telemetry (`/sim`), Direct Outbound SMS (`/sendsms`), SIM Swap Guard (`/sim_lock`), Tactile Device Locator (`/vibrate_pulse`), Failed Pattern Guard (`/pattern_guard`), App Network Isolation Firewall (`/app_firewall`), Battery Health & Drain Monitor (`/battery_alert`), Hardware Camera Lockout (`/camera_lock`), Peripheral Lockout (`/bluetooth_lock`, `/mic_mute`), Lockscreen Canvas (`/lockscreen_info`), Autolock Policy (`/autolock`), Emergency Wi-Fi Auto-Provisioning (`/wifi_connect`), Kernel OS Security Auditing (`/security_audit`), Silent App Uninstaller (`/app_uninstall`), TOTP SMS fallback, Ed25519 offline license verification, System-Wide Encrypted DNS (`/dns`), Cell Tower Triangulation (`/tower`), Locked USB Insertion Killswitch, Sterile Sandbox Decoy OS, and Autonomous Sensor Traps (Snatch, Charger, Pocket).
- **Licensing:** Strict 7-day trial hard lockout; all features are premium-gated after trial expiry. Red countdown banner displayed in Telegram bot during trial period.
