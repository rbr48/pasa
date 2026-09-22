# 🛡️ PASA Sentinel — Agent Directives & Architecture

Please see [GEMINI.md](file:///d:/Software_and_Apps/PrivateApp/GEMINI.md) for the comprehensive technical specification, file map, and operational rules.

### Quick Reference:
- **Zero-Storage Rule:** Never write surveillance media or GPS coordinates to server disk. All user evidence is direct-to-Telegram and shredded immediately from device RAM/cache.
- **Android Target:** Android 8.0 – 16 (API 26 – 36, compileSdk 36, targetSdk 36).
- **Core Modules:**
  - Android client: `app/src/main/java/com/izhaanintellect/pasa/`
  - VPS server: `pasa-server/` (Node.js 22, SQLite WAL mode)
  - Web landing: `pasa-commercial-web/`
  - Release binaries: `releases/`
- **Security & C2:** Knox Device Owner (`LOCK_TASK_FEATURE_NONE`), Hardware Escrow Tokens, 46 Telegram C2 commands, TOTP SMS fallback, Ed25519 offline license verification.
