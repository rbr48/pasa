# PASA Sentinel Architecture & Operational Rules

## Memory Anchor
This repository contains the complete source code for **PASA Sentinel (Private Android Security Agent)**:
- Target: Android 8.0 – 16 (API 26 – 36), compileSdk 36, targetSdk 36.
- Stack: Kotlin 2.0.0 / 1.9.22, Jetpack Compose / ViewBinding, Hilt DI, Room with SQLCipher, CameraX, WorkManager.
- Server: Node.js 22, Express, node:sqlite (WAL mode), Multer (memoryStorage only).
- C2 Console: Dual-channel Telegram Bot (@Pas_agent_bot or private dedicated bot) + air-gapped cellular SMS fallback.

## Inviolable Directives
1. **Strategy 1 Zero-Storage Guarantee:** Never write surveillance photos, audio wiretaps, video recordings, or GPS breadcrumbs to the VPS filesystem. All user evidence is direct-to-Telegram and shredded on device.
2. **Device Owner & Escrow Security:** Reset lockscreen passwords exclusively through cryptographic escrow tokens (`resetPasswordWithToken`). Always enforce Kiosk lockout (`LOCK_TASK_FEATURE_NONE`) and status bar disabling during emergencies.
3. **Android 14+ FGS Compliance:** All background camera/microphone acquisitions must dynamically elevate foreground service types (`dataSync|location|camera|microphone`).
4. **Offline Defense:** Maintain strict TOTP RFC 6238 authentication for cellular SMS commands and Ed25519 signature checks for offline licensing.
