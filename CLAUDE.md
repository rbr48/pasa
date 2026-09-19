# CLAUDE.md

Guidance for Claude Code (and humans) working in this repository.

## What this is

**PASA (Private Android Security Agent)** — an owner-controlled Android anti-theft
/ device-recovery system, controlled remotely via a Telegram bot. It is intended
for protecting your **own** device (locate, lock, wipe, capture evidence, alerts).

## Repository layout

| Path | What it is | Stack |
|---|---|---|
| `app/` | Android agent (the phone app) | Kotlin, Hilt, Room+SQLCipher, CameraX, Retrofit |
| `pasa-server/` | Control plane + Telegram gateway | Node.js / Express, flat-file JSON store |
| `pasa-commercial-web/` | Marketing landing page (showcase) | Static HTML + Tailwind, Docker/nginx |
| `ASTRA_SENTINEL_FULL_PROJECT_SPEC.md` | Architecture/spec document | — |

### Android module (`app/src/main/java/com/izhaanintellect/pasa/`)
- `service/PasaService.kt` — persistent foreground service; long-polls for commands, runs detectors.
- `bot/` — `CommandParser`, `CommandExecutor` (routes to command handlers), `TelegramApi`.
- `commands/` — one class per remote command (implements `Command`). Register new ones in `CommandExecutor` (constructor + `resolveHandler`).
- `detection/` — autonomous receivers/managers: power, SIM change, snatch traps, geofence, failed-unlock, boot, watchdog.
- `crypto/` — `CommandVerifier` (Ed25519 command envelopes), `DeviceAuth` (ES256 device proofs), `ReplayStore`.
- `security/` — `AuthManager` (PBKDF2 master password), `OTPManager`, `Totp` (RFC 6238 for SMS auth).
- `data/` — `PreferencesManager` (EncryptedSharedPreferences), Room DB (`CommandLog`).

## Build / test / run

**Android** (requires the Android SDK; set `ANDROID_HOME` or `local.properties` `sdk.dir=`):
```bash
./gradlew :app:compileDebugKotlin   # compile check
./gradlew :app:testDebugUnitTest    # JVM unit tests (app/src/test)
./gradlew :app:assembleDebug        # build debug APK
```
- JDK 17+ required. Do **not** hardcode `org.gradle.java.home` in the committed
  `gradle.properties` (it breaks other machines); rely on `JAVA_HOME`.
- Release signing reads `keystore.properties` (git-ignored).

**Server**:
```bash
cd pasa-server
npm install
npm start                # listens on PORT (default 8160)
node --check server.js   # quick syntax check
```
Config via `pasa-server/.env` (see `.env.example`). For personal use keep
`ENABLE_LICENSING=false`; set `ADMIN_SECRET` and optionally `ALLOWED_ORIGIN`.

## Conventions & gotchas

- **Adding a command:** create `commands/XxxCommand.kt` implementing `Command`
  (`@Singleton @Inject constructor`), then add it to `CommandExecutor`'s
  constructor and `resolveHandler` map, and a line in `HelpCommand`. Hilt wires it
  automatically. Commands are also reachable offline via `SmsCommandReceiver`.
- **Two command paths:** VPS backend (Ed25519-signed envelopes, verified by
  `CommandVerifier`) and a direct-Telegram fallback (authorized by owner chat id
  only). Prefer the signed path for privileged actions.
- **SMS auth** uses TOTP (`Totp` + `/smssetup`); the master password over SMS is a
  deprecated fallback. Never reintroduce sending the master password in cleartext.
- **Device Owner:** the strong lockdowns (block airplane mode / mobile data,
  kiosk, uninstall) only work when provisioned as Device Owner; otherwise they
  no-op. They cannot prevent a forced power-off or SIM removal.
- **Secrets:** never commit `keystore.properties`, `*.jks`, `.env`, or the
  server's `data/` (bot token, signing key, uploads). Already git-ignored.
- **Storage:** server persists to flat JSON files under `pasa-server/data/`;
  uploaded media under `pasa-server/uploads/`.

## Security-sensitive files (review carefully before changing)
`crypto/CommandVerifier.kt`, `crypto/DeviceAuth.kt`, `security/AuthManager.kt`,
`security/Totp.kt`, `data/PreferencesManager.kt`, and `pasa-server/server.js`
(admin auth, signing, rate limiting). See also `pasa-server/SECURITY_HARDENING.md`.
