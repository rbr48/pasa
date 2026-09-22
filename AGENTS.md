# 🛡️ PASA Sentinel — Agent Directives & Architecture

Please see [GEMINI.md](file:///d:/Software_and_Apps/PrivateApp/GEMINI.md) for the comprehensive technical specification, file map, and operational rules.

### Quick Reference:
- **Zero-Storage Rule:** Never write surveillance media or GPS coordinates to server disk. All user evidence is direct-to-Telegram and shredded immediately from device RAM/cache.
- **Android Target:** Android 8.0 – 16 (API 26 – 36, compileSdk 36, targetSdk 36).
- **Current Production Build:** v3.4.6 (Build 42), signed with `pasa-release-key.jks` (Scheme v2).
- **Core Modules:**
  - Android client: `app/src/main/java/com/izhaanintellect/pasa/`
  - VPS server: `pasa-server/` (Node.js 22, SQLite WAL mode, PM2 process `pasa-server` on `148.135.137.245:2222`)
  - Web landing: `pasa-commercial-web/`
  - Release binaries: `releases/` (`pasa-v3.4.6-42.apk`, `pasa-latest.apk`)
- **Security & C2:** Knox Device Owner (`LOCK_TASK_FEATURE_NONE`), Hardware Escrow Tokens, 63 Telegram C2 commands, Permanent Notification Suppression (`/notification`), SIM & Cellular Carrier RF Telemetry (`/sim`), Direct Outbound SMS (`/sendsms`), SIM Swap Guard (`/sim_lock`), Tactile Device Locator (`/vibrate_pulse`), Failed Pattern Guard (`/pattern_guard`), App Network Isolation Firewall (`/app_firewall`), Battery Health & Drain Monitor (`/battery_alert`), TOTP SMS fallback, Ed25519 offline license verification, System-Wide Encrypted DNS (`/dns`), Cell Tower Triangulation (`/tower`), Locked USB Insertion Killswitch, Sterile Sandbox Decoy OS, and Autonomous Sensor Traps (Snatch, Charger, Pocket).
