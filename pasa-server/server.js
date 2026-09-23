const express = require('express');
const cors = require('cors');
const multer = require('multer');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const EventEmitter = require('events');
const { renderCommercialLandingPage } = require('./landingPage');
const { loadJson, saveJson } = require('./lib/storage');
const { rateLimit } = require('./lib/rateLimit');
const { createLicensing } = require('./lib/licensing');
const {
  initDatabase,
  migrateFromJson,
  DeviceRepo,
  CommandRepo,
  ReleaseRepo,
  AuditRepo,
  EvidenceRepo
} = require('./lib/db');

// Load local .env if present (zero-dependency)
const envFilePath = path.join(__dirname, '.env');
if (fs.existsSync(envFilePath)) {
  try {
    const envLines = fs.readFileSync(envFilePath, 'utf8').split('\n');
    for (const line of envLines) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) continue;
      const idx = trimmed.indexOf('=');
      if (idx !== -1) {
        const key = trimmed.slice(0, idx).trim();
        let val = trimmed.slice(idx + 1).trim();
        if ((val.startsWith('"') && val.endsWith('"')) || (val.startsWith("'") && val.endsWith("'"))) {
          val = val.slice(1, -1);
        }
        if (!process.env[key]) process.env[key] = val;
      }
    }
  } catch (e) {
    console.warn('[ENV] Warning loading .env file:', e.message);
  }
}

const app = express();
const PORT = process.env.PORT || 8160;
const commandEmitter = new EventEmitter();
commandEmitter.setMaxListeners(100);
const pairingEmitter = new EventEmitter();
pairingEmitter.setMaxListeners(100);

// In-memory 6-digit OTP pairing store: code -> { deviceId, deviceName, createdAt, expiresAt, status, ownerChatId }
const activePairings = new Map();
// Anti-brute-force rate limiting per Telegram chatId: chatId -> { attempts, lockedUntil }
const telegramPairingAttempts = new Map();

// Official central PASA Bot (@Pas_agent_bot) token for Method 2 instant pairing
const PASA_CENTRAL_BOT_TOKEN = '8815969412:AAEN_BqiCldZVza93qApCbGn5hTrcAW9HxA';
const DEFAULT_BOT_TOKEN = process.env.BOT_TOKEN || PASA_CENTRAL_BOT_TOKEN;
const ADMIN_BOT_TOKEN = process.env.ADMIN_BOT_TOKEN || process.env.BOT_TOKEN || PASA_CENTRAL_BOT_TOKEN;
const ADMIN_CHAT_ID = String(process.env.ADMIN_CHAT_ID || '');
const BINANCE_PAY_ID = process.env.BINANCE_PAY_ID || '756303714';
const BINANCE_NICKNAME = process.env.BINANCE_NICKNAME || 'RBR48';

// Setup directories
const DATA_DIR = path.join(__dirname, 'data');
const UPLOADS_DIR = path.join(__dirname, 'uploads');
const EVIDENCE_DIR = path.join(UPLOADS_DIR, 'evidence');
if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });
if (!fs.existsSync(UPLOADS_DIR)) fs.mkdirSync(UPLOADS_DIR, { recursive: true });
if (!fs.existsSync(EVIDENCE_DIR)) fs.mkdirSync(EVIDENCE_DIR, { recursive: true });

function getAdminBotToken() {
  if (ADMIN_BOT_TOKEN) return ADMIN_BOT_TOKEN;
  if (DEFAULT_BOT_TOKEN) return DEFAULT_BOT_TOKEN;
  for (const dev of Object.values(devices)) {
    if (dev.botToken) return dev.botToken;
  }
  return '';
}

const DB_FILE = path.join(DATA_DIR, 'pasa.db');
const DEVICES_FILE = path.join(DATA_DIR, 'devices.json');
const COMMANDS_FILE = path.join(DATA_DIR, 'commands.json');
const SIGNING_KEY_FILE = path.join(DATA_DIR, 'server_signing_key.json');
const ADMIN_SECRET_FILE = path.join(DATA_DIR, 'admin_secret.json');
const LOGS_FILE = path.join(DATA_DIR, 'security_logs.json');
const SERVER_KEY_ID = 'pasa-server-1';
const RELEASES_DIR = path.join(__dirname, 'releases');
if (!fs.existsSync(RELEASES_DIR)) fs.mkdirSync(RELEASES_DIR, { recursive: true });
const RELEASES_FILE = path.join(DATA_DIR, 'app_releases.json');
const GPS_FILE = path.join(DATA_DIR, 'gps_history.json');
const LICENSES_FILE = path.join(DATA_DIR, 'licenses.json');
const ED25519_KEY_FILE = path.join(DATA_DIR, 'license_ed25519_key.json');
const ORDERS_FILE = path.join(DATA_DIR, 'license_orders.json');
let licenseOrders = loadJson(ORDERS_FILE, {});

// Strategy 1 Zero-Storage: Ephemeral RAM buffer only, zero disk writes for user media
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 25 * 1024 * 1024 } // 25MB max memory buffer
});

const apkUpload = multer({
  dest: RELEASES_DIR,
  limits: { fileSize: 200 * 1024 * 1024 }, // 200MB max for APKs
  fileFilter: (req, file, cb) => {
    cb(null, true); // Accept all, validate after
  }
});

// CORS: restrict to a configured browser origin (the admin console / landing page).
// Device API calls come from the Android app (no Origin header) and are unaffected
// by CORS, so locking this down does not break the agent. Set ALLOWED_ORIGIN in .env;
// defaults to same-origin only (no cross-origin browser access).
const ALLOWED_ORIGIN = process.env.ALLOWED_ORIGIN || '';
app.use(cors({ origin: ALLOWED_ORIGIN ? ALLOWED_ORIGIN.split(',').map(s => s.trim()) : false }));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.set('trust proxy', 1); // behind nginx; makes req.ip the real client address

// Rate limiter imported from ./lib/rateLimit.
// Brute-force protection on the sensitive auth/enrollment endpoints.
const adminAuthLimiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 10, message: 'Too many admin auth attempts. Try again later.' });
const deviceRegisterLimiter = rateLimit({ windowMs: 60 * 60 * 1000, max: 30, message: 'Too many device registrations from this IP.' });

// Commercial licensing endpoints are disabled by default for personal deployments.
// Set ENABLE_LICENSING=true in .env to expose the purchase/webhook/lookup routes.
const LICENSING_ENABLED = process.env.ENABLE_LICENSING !== undefined ? String(process.env.ENABLE_LICENSING).toLowerCase() === 'true' : true;
function licensingGuard(req, res, next) {
  if (!LICENSING_ENABLED) {
    return res.status(410).json({ ok: false, description: 'Licensing is disabled on this deployment.' });
  }
  next();
}

// Initialize PASA 3.0 SQLite Control Plane (WAL mode)
initDatabase(DB_FILE);
migrateFromJson(DATA_DIR);

let devices = DeviceRepo.getAll();
if (Object.keys(devices).length === 0) {
  devices = loadJson(DEVICES_FILE, {});
  for (const d of Object.values(devices)) {
    try { DeviceRepo.upsert(d); } catch (_) {}
  }
}

let commands = CommandRepo.getAllPending();
// Track command IDs that have already been completed, so the watchdog never
// fires a false-positive timeout alert for commands that finished successfully.
const completedCommandIds = new Set();
if (Object.keys(commands).length === 0) {
  commands = loadJson(COMMANDS_FILE, {});
}
let gpsHistory = loadJson(GPS_FILE, {}); // deviceId -> [ { lat, lon, timestamp, iso, ...meta } ]

function persistDevice(deviceId) {
  if (devices[deviceId]) {
    try { DeviceRepo.upsert(devices[deviceId]); } catch (err) { console.error('[SQLite] Device upsert error:', err.message); }
  }
  saveJson(DEVICES_FILE, devices);
}

function persistCommand(devId, cmd) {
  if (cmd) {
    try {
      CommandRepo.add({
        id: cmd.id,
        deviceId: devId,
        command: cmd.command,
        args: cmd.args,
        chatId: cmd.chatId,
        envelope: cmd.envelope,
        createdAt: cmd.createdAt
      });
    } catch (err) { console.error('[SQLite] Command add error:', err.message); }
  }
  saveJson(COMMANDS_FILE, commands);
}

function removeCommand(devId, cmdId, response = '') {
  if (commands[devId] && cmdId) {
    commands[devId] = commands[devId].filter(c => c.id !== cmdId);
    try { CommandRepo.complete(cmdId, typeof response === 'string' ? response : JSON.stringify(response)); } catch (err) { console.error('[SQLite] Command clear error:', err.message); }
    saveJson(COMMANDS_FILE, commands);
    // Mark as completed so the watchdog does not fire a false-positive alert
    completedCommandIds.add(cmdId);
  }
}

function recordDeviceLocation(deviceId, lat, lon, meta = {}) {
  // Strategy 1 Zero-Storage: Zero GPS coordinates or historical tracks are ever saved.
  // Device coordinates are transmitted directly to the user's private Telegram bot.
  return null;
}

// --- Commercial Licensing & Subscription Engine (see ./lib/licensing.js) ---
// logSecurityEvent is a hoisted function declaration defined below; devices is
// declared above. Device coupling is injected so the module stays self-contained.
const licensing = createLicensing({
  licensesFile: LICENSES_FILE,
  ed25519KeyFile: ED25519_KEY_FILE,
  loadJson,
  saveJson,
  logSecurityEvent,
  getDevice: (id) => devices[id],
  persistDevices: () => {
    for (const id of Object.keys(devices)) persistDevice(id);
  }
});

// --- Master Admin Secret Management ---

let ADMIN_SECRET = process.env.ADMIN_SECRET;
if (!ADMIN_SECRET) {
  if (fs.existsSync(ADMIN_SECRET_FILE)) {
    try {
      const saved = JSON.parse(fs.readFileSync(ADMIN_SECRET_FILE, 'utf8'));
      ADMIN_SECRET = saved.adminSecret;
    } catch (err) {
      console.error('[Admin Auth] Error reading admin_secret.json:', err.message);
    }
  }
  if (!ADMIN_SECRET) {
    ADMIN_SECRET = crypto.randomBytes(24).toString('hex');
    saveJson(ADMIN_SECRET_FILE, {
      adminSecret: ADMIN_SECRET,
      generatedAt: new Date().toISOString()
    });
    console.log(`\n=============================================================`);
    console.log(`[Admin Auth] Generated Initial Master Admin Access Key:`);
    console.log(`👉 ${ADMIN_SECRET}`);
    console.log(`Use this key to authenticate at https://<domain>/pasa/admin`);
    console.log(`=============================================================\n`);
  }
}

// --- Security Audit Event Logs (Persistent SQLite + Circular JSON Buffer) ---

let securityLogs = loadJson(LOGS_FILE, []);
if (!Array.isArray(securityLogs)) securityLogs = [];

function logSecurityEvent(type, details = {}) {
  const event = {
    id: 'evt_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6),
    timestamp: Date.now(),
    iso: new Date().toISOString(),
    type,
    ...details
  };
  securityLogs.unshift(event);
  if (securityLogs.length > 200) {
    securityLogs = securityLogs.slice(0, 200);
  }
  saveJson(LOGS_FILE, securityLogs);

  // Persist to SQLite audit_logs table
  AuditRepo.log(
    type,
    details.deviceId || '',
    details.chatId || '',
    details,
    details.ip || ''
  );

  return event;
}

// --- Command Queue Maintenance (Purge stale commands >24h) ---

function cleanupStaleCommands() {
  const cutoff = Date.now() - 24 * 60 * 60 * 1000;
  let cleaned = 0;
  for (const devId of Object.keys(commands)) {
    const origLen = (commands[devId] || []).length;
    commands[devId] = (commands[devId] || []).filter(c => (c.createdAt || 0) > cutoff);
    cleaned += (origLen - commands[devId].length);
  }
  if (cleaned > 0) {
    saveJson(COMMANDS_FILE, commands);
    console.log(`[Maintenance] Purged ${cleaned} stale command(s) older than 24h.`);
  }
}
cleanupStaleCommands();
setInterval(cleanupStaleCommands, 6 * 60 * 60 * 1000);

// --- 7-Day Evidence Retention Auto-Purge Job (PASA 3.0 Vault) ---

function cleanupExpiredEvidence() {
  try {
    const purged = EvidenceRepo.purgeExpired(EVIDENCE_DIR);
    if (purged > 0) {
      console.log(`[Maintenance] Auto-purged ${purged} expired evidence files from vault (>7 days).`);
    }
  } catch (err) {
    console.error('[Maintenance] Evidence purge error:', err.message);
  }
}
cleanupExpiredEvidence();
setInterval(cleanupExpiredEvidence, 6 * 60 * 60 * 1000);

// --- Admin Authentication Middleware ---

function authenticateAdmin(req, res, next) {
  const token = req.headers['x-pasa-admin-key']
    || (req.headers['authorization'] && req.headers['authorization'].startsWith('Bearer ') ? req.headers['authorization'].split(' ')[1].trim() : null)
    || (req.query && req.query.key);

  if (!token || typeof token !== 'string') {
    return res.status(401).json({ ok: false, description: 'Unauthorized: Admin access key required' });
  }

  const expectedBuf = Buffer.from(ADMIN_SECRET);
  const actualBuf = Buffer.from(token);

  if (expectedBuf.length === actualBuf.length && crypto.timingSafeEqual(expectedBuf, actualBuf)) {
    return next();
  }

  logSecurityEvent('ADMIN_LOGIN_FAILURE', {
    ip: req.ip || (req.connection && req.connection.remoteAddress) || 'unknown',
    userAgent: req.headers['user-agent'] || 'unknown'
  });

  return res.status(401).json({ ok: false, description: 'Unauthorized: Invalid admin access key' });
}

// --- Ed25519 Server Command Signing Key (ASTRA Layer) ---

let serverPrivateKey;
let serverPublicJwk;

function initServerSigningKey() {
  try {
    if (fs.existsSync(SIGNING_KEY_FILE)) {
      const saved = JSON.parse(fs.readFileSync(SIGNING_KEY_FILE, 'utf8'));
      serverPrivateKey = crypto.createPrivateKey({ key: saved.privateJwk, format: 'jwk' });
      serverPublicJwk = saved.publicJwk;
      console.log(`[Crypto] Loaded existing Ed25519 signing key (${SERVER_KEY_ID})`);
      return;
    }
  } catch (err) {
    console.error('[Crypto] Error loading existing key, generating new one:', err);
  }

  const { publicKey, privateKey } = crypto.generateKeyPairSync('ed25519');
  serverPrivateKey = privateKey;
  const pubJwk = publicKey.export({ format: 'jwk' });
  pubJwk.kid = SERVER_KEY_ID;
  serverPublicJwk = pubJwk;

  const privJwk = privateKey.export({ format: 'jwk' });
  privJwk.kid = SERVER_KEY_ID;

  saveJson(SIGNING_KEY_FILE, {
    kid: SERVER_KEY_ID,
    publicJwk: serverPublicJwk,
    privateJwk: privJwk,
    createdAt: new Date().toISOString()
  });
  console.log(`[Crypto] Generated new Ed25519 server signing key (${SERVER_KEY_ID})`);
}

initServerSigningKey();

/**
 * Signs a command in a JWS compact serialization envelope using the server's Ed25519 private key.
 */
function signCommandEnvelope(deviceId, action, args = [], chatId = 0, cmdId = null) {
  const id = cmdId || ('cmd_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7));
  const device = devices[deviceId] || {};
  const sequence = (device.lastSequence || 0) + 1;
  device.lastSequence = sequence;
  persistDevice(deviceId);

  const nonce = crypto.randomBytes(18).toString('base64url');
  const now = new Date();
  const issuedAt = now.toISOString();
  const expiresAt = new Date(now.getTime() + 600000).toISOString(); // 10 minutes validity window to accommodate background intervals & clock skew

  const header = {
    alg: 'EdDSA',
    typ: 'JWT',
    kid: SERVER_KEY_ID
  };

  const payload = {
    commandId: id,
    deviceId,
    action: action.toUpperCase(),
    args,
    sequence,
    nonce,
    issuedAt,
    expiresAt,
    chatId: Number(chatId) || 0
  };

  const headerB64 = Buffer.from(JSON.stringify(header)).toString('base64url');
  const payloadB64 = Buffer.from(JSON.stringify(payload)).toString('base64url');
  const signingInput = `${headerB64}.${payloadB64}`;

  const sig = crypto.sign(null, Buffer.from(signingInput, 'utf8'), serverPrivateKey);
  const sigB64 = sig.toString('base64url');

  return `${signingInput}.${sigB64}`;
}

// --- Device Proof Verification & Authentication Middleware ---

const seenJtis = new Map();

function cleanExpiredJtis() {
  const now = Date.now();
  for (const [jti, exp] of seenJtis.entries()) {
    if (exp <= now) seenJtis.delete(jti);
  }
}

function verifyDeviceProofOrBearer(req, res, next) {
  const proofHeader = req.headers['x-pasa-device-proof'];

  // 1. If proof header is present, perform cryptographic verification (StrongBox/TEE proof)
  if (proofHeader && typeof proofHeader === 'string') {
    try {
      const parts = proofHeader.split('.');
      if (parts.length === 3) {
        const [headerB64, payloadB64, sigB64] = parts;
        const header = JSON.parse(Buffer.from(headerB64, 'base64url').toString('utf8'));
        const payload = JSON.parse(Buffer.from(payloadB64, 'base64url').toString('utf8'));

        if (header.alg === 'ES256') {
          const deviceId = payload.deviceId;
          const device = devices[deviceId];

          if (device) {
            const nowSec = Math.floor(Date.now() / 1000);
            const isNotExpired = !payload.exp || payload.exp > nowSec;

            // Single-use JTI replay check
            const jti = payload.jti;
            cleanExpiredJtis();

            if (isNotExpired && jti && !seenJtis.has(jti)) {
              seenJtis.set(jti, (payload.exp || (nowSec + 120)) * 1000);

              // Only trust the proof header when the device has enrolled a
              // hardware public key. Without one there is nothing to verify
              // the signature against, so an unsigned-but-well-formed header
              // must NOT authenticate the request — fall through to the
              // Bearer API key check below instead (see step 2).
              if (device.publicKeyJwk) {
                try {
                  const pubJwk = typeof device.publicKeyJwk === 'string'
                    ? JSON.parse(device.publicKeyJwk)
                    : device.publicKeyJwk;
                  const ecKey = crypto.createPublicKey({ key: pubJwk, format: 'jwk' });
                  const signingInput = Buffer.from(`${headerB64}.${payloadB64}`, 'utf8');
                  const sigBuf = Buffer.from(sigB64, 'base64url');

                  const verified = crypto.verify('sha256', signingInput, { key: ecKey, dsaEncoding: 'ieee-p1363' }, sigBuf);
                  if (verified) {
                    req.device = device;
                    req.deviceAuthMode = 'hardware_p256_verified';
                    return next();
                  }
                } catch (cryptoErr) {
                  console.warn(`[Crypto] Signature check failed for ${deviceId}:`, cryptoErr.message);
                }
              } else {
                console.warn(`[Crypto] Rejected unsigned device proof for ${deviceId}: no hardware key enrolled`);
              }
            }
          }
        }
      }
    } catch (e) {
      console.warn('[Crypto] Proof parsing error:', e.message);
    }
  }

  // 2. Fall back to Bearer API key authentication
  const authHeader = req.headers['authorization'];
  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.split(' ')[1].trim();

    const device = Object.values(devices).find(d => d.apiKey === token);
    if (device) {
      req.device = device;
      req.deviceAuthMode = 'bearer_api_key';
      return next();
    }

    const deviceId = req.query.deviceId || (req.body && req.body.deviceId);
    if (deviceId && devices[deviceId] && devices[deviceId].apiKey === token) {
      req.device = devices[deviceId];
      req.deviceAuthMode = 'bearer_api_key';
      return next();
    }
  }

  return res.status(401).json({ ok: false, description: 'Unauthorized: Invalid credentials or expired device proof' });
}

// Map of active bot pollers: token -> { isRunning, offset }
const activePollers = new Map();

// --- Telegram Bot API Helpers (VPS-Side) ---

async function callTelegram(token, method, body = null, isMultipart = false, formData = null) {
  const url = `https://api.telegram.org/bot${token}/${method}`;
  try {
    let res;
    if (isMultipart && formData) {
      res = await fetch(url, { method: 'POST', body: formData });
    } else {
      const headers = body ? { 'Content-Type': 'application/json' } : {};
      res = await fetch(url, {
        method: body ? 'POST' : 'GET',
        headers,
        body: body ? JSON.stringify(body) : null
      });
    }

    const contentType = res.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      const data = await res.json();
      if (!data.ok) {
        console.error(`[Telegram API] Method ${method} failed:`, data.description);
        // Resilient fallback: If message fails due to entity parsing error, automatically retry without HTML parse_mode
        if (body && body.parse_mode === 'HTML' && (data.description || '').includes("can't parse entities")) {
          console.warn(`[Telegram API] Retrying ${method} without HTML parse_mode due to entity error`);
          const plainBody = { ...body };
          delete plainBody.parse_mode;
          const retryRes = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(plainBody)
          });
          return await retryRes.json();
        }
      }
      return data;
    }
    const text = await res.text();
    return { ok: false, description: `HTTP ${res.status}: ${text.substring(0, 200)}` };
  } catch (err) {
    console.error(`[Telegram API] Error calling ${method}:`, err.message);
    return { ok: false, description: err.message };
  }
}

// Register all commands in Telegram menu autocomplete
async function registerTelegramBotCommands(token) {
  const commandsList = [
    { command: "menu", description: "📱 Open interactive touchscreen control panel" },
    { command: "help", description: "📖 Show full help manual & command guide" },
    { command: "status", description: "📊 Live battery, storage, RAM & sensor telemetry" },
    { command: "selftest", description: "🩺 Run 9-point security, GPS & sensor audit" },
    { command: "info", description: "ℹ️ Hardware specs, SIM details & OS version" },
    { command: "locate", description: "📍 Acquire instant GPS fix & Google Maps pin" },
    { command: "tower", description: "📡 Cell tower triangulation & signal RF telemetry" },
    { command: "sim", description: "📶 Active SIM slots, carrier name & signal RF" },
    { command: "sim_lock", description: "🛡️ SIM swap guard & ICCID whitelist lock" },
    { command: "sendsms", description: "✉️ Send outbound SMS directly via SIM slot" },
    { command: "track", description: "🛰️ Start continuous live GPS tracking" },
    { command: "track_stop", description: "🛑 Stop continuous GPS tracking" },
    { command: "geofence", description: "🌐 Configure safe zone radius & breach alerts" },
    { command: "snap", description: "📸 Capture stealth photo (front, rear, or both)" },
    { command: "livestream", description: "🔴 Stream near-live camera video to Telegram" },
    { command: "stopstream", description: "⏹️ Stop active camera live stream" },
    { command: "screenshot", description: "📱 Silent full-screen capture via Accessibility" },
    { command: "screen_burst", description: "🎞️ Rapid 5-10 frame montage of intruder activity" },
    { command: "screenrecord", description: "🎥 Covert HD MP4 screen recording (5-60s)" },
    { command: "video", description: "📹 Record stealth camera video (1-60s)" },
    { command: "record", description: "🎙️ Record ambient microphone audio clip" },
    { command: "clipboard", description: "📋 Read current device clipboard text" },
    { command: "lock", description: "🔒 Lock screen with custom PIN & emergency banner" },
    { command: "lock_message", description: "💬 Set urgent alert message on lockscreen" },
    { command: "lock_pin", description: "🔑 Lock phone with custom 4-8 digit PIN" },
    { command: "set_os_pin", description: "🔐 Overwrite hardware OS lockscreen PIN (Device Owner)" },
    { command: "set_master_pin", description: "🔑 Set cryptographic master PIN for remote control" },
    { command: "unlock", description: "🔓 Dismiss Lost Mode & unlock device screen" },
    { command: "fakeshutdown", description: "🕶️ Fake shutdown: blackout screen & silent traps" },
    { command: "wake", description: "☀️ Restore device from Fake Shutdown blackout" },
    { command: "ring", description: "🚨 Trigger maximum volume emergency siren" },
    { command: "ring_stop", description: "🔇 Silence active emergency alarm siren" },
    { command: "vibrate_pulse", description: "📳 Locate device silently via tactile vibrations" },
    { command: "pattern_guard", description: "👁️ Failed pattern/PIN intrusion monitor & mugshot" },
    { command: "app_firewall", description: "🧱 Block RAT & spyware network outbound telemetry" },
    { command: "battery_alert", description: "🔋 Monitor abnormal drain & charging disconnects" },
    { command: "harden_boot", description: "🔒 Lock recovery mode & prevent unauthorized reset" },
    { command: "tamper_detect", description: "🔍 Scan for root, debuggers, hooks & emulators" },
    { command: "dead_drop", description: "☁️ Backup evidence to encrypted local/cloud vault" },
    { command: "message", description: "📢 Display urgent fullscreen alert on device" },
    { command: "duress_pin", description: "🆘 Set decoy coercion PIN for emergency SOS" },
    { command: "trap", description: "🛡️ Arm sensor traps (snatch, charger, pocket)" },
    { command: "shred", description: "🗑️ Cryptographically shred sensitive files" },
    { command: "device_owner", description: "👑 Check Device Owner & Kiosk hardware lock" },
    { command: "antitamper", description: "🛡️ Safe boot, airplane mode & factory reset lock" },
    { command: "usb_lock", description: "🔌 Cut USB data signaling pins (charge only)" },
    { command: "camera_lock", description: "📷 Hardware camera killswitch (anti-spy lockout)" },
    { command: "bluetooth_lock", description: "📡 Hardware Bluetooth & sharing killswitch" },
    { command: "mic_mute", description: "🔇 Hardware master audio mute (HAL level)" },
    { command: "lockscreen_info", description: "📱 Pin contact/recovery info to OS lockscreen" },
    { command: "autolock", description: "⏱️ Enforce screen inactivity autolock timeout" },
    { command: "wifi_connect", description: "📶 Emergency Wi-Fi auto-provisioning while locked" },
    { command: "security_audit", description: "📑 Inspect kernel OS security audit logs" },
    { command: "notification", description: "🔕 Permanent notification drawer suppression" },
    { command: "self_heal", description: "✨ Permanently lock app permissions as managed" },
    { command: "freeze", description: "🧊 Vanish banking & private apps into shadow vault" },
    { command: "unfreeze", description: "🔥 Restore hidden applications to launcher" },
    { command: "frozen", description: "📦 List currently frozen shadow vault apps" },
    { command: "biometrics", description: "🚫 Biometric coercion killswitch (forces Master PIN)" },
    { command: "dns", description: "🛡️ Enforce system-wide Private DNS-over-TLS" },
    { command: "reboot", description: "🔄 Remotely restart phone hardware (Device Owner)" },
    { command: "stealth", description: "👁️ Toggle PASA app icon in launcher" },
    { command: "hide", description: "🔇 Hide PASA app icon from phone launcher" },
    { command: "show", description: "👁️ Restore PASA app icon to phone launcher" },
    { command: "contacts", description: "👥 Search device address book contacts" },
    { command: "call_log", description: "📞 View incoming and outgoing call history" },
    { command: "sms_log", description: "💬 View recent SMS inbox messages" },
    { command: "history", description: "📜 View recent command audit execution trail" },
    { command: "network", description: "🌐 Current IP, Wi-Fi SSID & cell carrier info" },
    { command: "apps", description: "📦 List installed applications" },
    { command: "app_uninstall", description: "❌ Silently uninstall package (Device Owner)" },
    { command: "smssetup", description: "📲 Enroll TOTP for secure offline SMS commands" },
    { command: "license", description: "🔑 Check Pro license status or activate key" },
    { command: "check_update", description: "🔄 Check for OTA app updates" },
    { command: "update_confirm", description: "⚡ Download and install pending OTA update" },
    { command: "wipe", description: "⚠️ Emergency remote factory reset (requires auth)" },
    { command: "wipe_confirm", description: "💥 Confirm remote factory reset with password" }
  ];

  try {
    const res = await callTelegram(token, 'setMyCommands', { commands: commandsList });
    if (res && res.ok) {
      console.log(`[Telegram] Registered ${commandsList.length} bot commands with Telegram`);
    } else {
      console.warn(`[Telegram] Failed to register bot commands:`, res ? res.description : 'Unknown error');
    }
  } catch (e) {
    console.error(`[Telegram] Error setting bot commands:`, e.message);
  }

  // Set bottom Chat Menu Button to standard bot commands menu
  try {
    const res = await callTelegram(token, 'setChatMenuButton', {
      menu_button: {
        type: 'commands'
      }
    });
    if (res && res.ok) {
      console.log(`[Telegram] Reverted chat menu button to standard commands menu`);
    }
  } catch (e) {
    console.error(`[Telegram] Error setting chat menu button:`, e.message);
  }
}

// Persistent Bottom Reply Keyboard (Always pinned under chat input)
const PERSISTENT_REPLY_KEYBOARD = {
  keyboard: [
    [{ text: '📊 Status' }, { text: '📍 Locate' }, { text: '🚨 Siren' }],
    [{ text: '📸 Photo' }, { text: '📱 Screen' }, { text: '🎥 Video' }],
    [{ text: '🔒 Lock' }, { text: '🎛️ Hub Menu' }, { text: '👑 Device Owner' }],
    [{ text: '💬 Message' }, { text: '🛡️ Traps' }, { text: '🔑 License' }]
  ],
  resize_keyboard: true,
  is_persistent: true
};

// Conversational Interactive State Machine (5-minute session lifetime)
const userChatStates = new Map(); // chatId -> { state, data, timestamp }
function setChatState(chatId, state, data = {}) {
  userChatStates.set(String(chatId), { state, data, timestamp: Date.now() });
}
function getChatState(chatId) {
  const s = userChatStates.get(String(chatId));
  if (!s) return null;
  if (Date.now() - s.timestamp > 5 * 60 * 1000) {
    userChatStates.delete(String(chatId));
    return null;
  }
  return s;
}
function clearChatState(chatId) {
  userChatStates.delete(String(chatId));
}

// Interactive Dashboard & Submenu Keyboards
const DASHBOARD_KEYBOARD = {
  inline_keyboard: [
    [
      { text: '📍 Location & RF', callback_data: 'menu:location_hub' },
      { text: '📸 Covert Forensics', callback_data: 'menu:forensics_hub' }
    ],
    [
      { text: '🚨 Lockdown & Siren', callback_data: 'menu:lockdown_hub' },
      { text: '👑 Device Owner Suite', callback_data: 'menu:device_owner_hub' }
    ],
    [
      { text: '🛡️ Traps & Security', callback_data: 'menu:traps_hub' },
      { text: '📇 Extraction & Logs', callback_data: 'menu:data_hub' }
    ],
    [
      { text: '📊 Full Telemetry Status', callback_data: 'cmd:status' },
      { text: '🔄 Check OTA Update', callback_data: 'cmd:check_update' }
    ],
    [
      { text: '🔑 License & Pro', callback_data: 'menu:license' },
      { text: '📖 Help & All Commands', callback_data: 'cmd:help' }
    ]
  ]
};

const SUBMENUS = {
  'menu:screen': {
    text: '📱 <b>Covert Screen Surveillance Suite</b>\n━━━━━━━━━━━━━━━━━━━━\nCapture real-time intruder screen activity silently with zero popups or system notifications:\n\n• <b>Silent Screenshot</b>: Native A11y capture (API 30+). Returns full-res PNG directly to chat.\n• <b>Screen Burst</b>: Stitches 3–10 rapid frames over 10s into a multi-frame storyboard grid image.\n• <b>Screen Recording</b>: Covert HD MP4 video via Device Owner shell (5–60s).\n\n<i>Choose capture action below:</i>',
    keyboard: {
      inline_keyboard: [
        [
          { text: '📸 Instant Screenshot', callback_data: 'cmd:screenshot' }
        ],
        [
          { text: '🎞️ Screen Burst (5 frames)', callback_data: 'cmd:screen_burst:5' },
          { text: '🎞️ Screen Burst (10 frames)', callback_data: 'cmd:screen_burst:10' }
        ],
        [
          { text: '🎥 Record Screen (15s)', callback_data: 'cmd:screenrecord:15' },
          { text: '🎥 Record Screen (30s)', callback_data: 'cmd:screenrecord:30' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:stealth': {
    text: '👁️ <b>App Icon Stealth & Launcher Cloaking</b>\n━━━━━━━━━━━━━━━━━━━━\nControl whether PASA Sentinel is visible in the phone’s app drawer:\n\n• <b>Hide Icon</b>: Strips the app icon from the launcher & app drawer completely. All background monitoring remains 100% active.\n• <b>Show Icon</b>: Restores the app icon back to the launcher & app drawer.\n• <b>Toggle</b>: Reverses current visibility.\n\n<i>Choose stealth action below:</i>',
    keyboard: {
      inline_keyboard: [
        [
          { text: '🔇 Hide App Icon', callback_data: 'cmd:stealth:hide' },
          { text: '👁️ Show App Icon', callback_data: 'cmd:stealth:show' }
        ],
        [
          { text: '🔄 Toggle Visibility', callback_data: 'cmd:stealth' }
        ],
        [
          { text: '🔙 Back to Tools', callback_data: 'menu:tools' },
          { text: '🏠 Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:tools': {
    text: '⚙️ <b>Advanced Security & Defense Tools</b>\n━━━━━━━━━━━━━━━━━━━━\nSpecialized counter-measures, stealth options, and emergency utilities:\n\n• <b>Duress Mode</b>: Set an emergency decoy PIN that unlocks to safe screen while triggering silent SOS.\n• <b>File Shredder</b>: Multi-pass cryptographic sanitization with PRNG + zero-fill.\n• <b>Device Owner</b>: Enterprise hardware lock task mode & kiosk protection.\n• <b>Stealth Mode</b>: Hide or reveal PASA app icon in launcher.\n• <b>Remote Wipe</b>: Irreversible factory reset.',
    keyboard: {
      inline_keyboard: [
        [
          { text: '🔑 Duress Mode', callback_data: 'menu:duress' },
          { text: '🗑️ File Shredder', callback_data: 'menu:shred' }
        ],
        [
          { text: '🛡️ Device Owner Check', callback_data: 'cmd:device_owner' },
          { text: '👁️ Stealth & Cloak Menu', callback_data: 'menu:stealth' }
        ],
        [
          { text: '⚠️ Remote Factory Wipe', callback_data: 'menu:wipe' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:traps': {
    text: '🛡️ <b>Autonomous Edge Defense Traps</b>\n━━━━━━━━━━━━━━━━━━━━\nAutonomous sensors react instantly without waiting for remote signals:\n\n• <b>Snatch-and-Run</b>: Senses violent acceleration spikes (>2.6G) and locks immediately.\n• <b>Charger Disconnect</b>: Triggers if phone is unplugged while locked.\n\n<b>Commands:</b>\n• <code>/trap on</code> — Arm all traps\n• <code>/trap off</code> — Disarm all traps\n• <code>/trap snatch on|off</code>\n• <code>/trap charger on|off</code>',
    keyboard: {
      inline_keyboard: [
        [
          { text: '🟢 Arm All Traps', callback_data: 'cmd:trap:on' },
          { text: '🔴 Disarm Traps', callback_data: 'cmd:trap:off' }
        ],
        [
          { text: '🏃 Toggle Snatch Trap', callback_data: 'cmd:trap:snatch' },
          { text: '🔌 Toggle Charger Trap', callback_data: 'cmd:trap:charger' }
        ],
        [
          { text: '📊 Check Traps Status', callback_data: 'cmd:trap:status' },
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:message': {
    text: '💬 <b>Display Screen Alert Message</b>\n━━━━━━━━━━━━━━━━━━━━\nBroadcast an urgent lost-mode alert or emergency contact banner directly over the phone\'s lockscreen with a 1-tap call button.\n\nTap a quick template below or choose <b>✍️ Type Custom Message</b>:',
    keyboard: {
      inline_keyboard: [
        [
          { text: '📱 "Lost phone! Please call owner."', callback_data: 'msg_preset:lost' }
        ],
        [
          { text: '⚠️ "Stolen device! Police GPS tracking active."', callback_data: 'msg_preset:stolen' }
        ],
        [
          { text: '💰 "Reward offered if returned! Call owner."', callback_data: 'msg_preset:reward' }
        ],
        [
          { text: '✍️ Type Custom Message', callback_data: 'wizard:msg:custom' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:snap': {
    text: '📸 <b>Photo Forensic Capture</b>\nSelect camera sensor to trigger silently:',
    keyboard: {
      inline_keyboard: [
        [
          { text: '🤳 Front Selfie Camera', callback_data: 'cmd:snap:front' },
          { text: '📷 Rear Main Camera', callback_data: 'cmd:snap:back' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:video': {
    text: '🎥 <b>Stealth Video & Live Stream</b>\nRecord short clips or stream live camera video directly to Telegram:',
    keyboard: {
      inline_keyboard: [
        [
          { text: '🔴 Live Stream (Front)', callback_data: 'cmd:livestream:front:5' },
          { text: '🔴 Live Stream (Rear)', callback_data: 'cmd:livestream:back:5' }
        ],
        [
          { text: '🤳 Front Clip (15s)', callback_data: 'cmd:video:front:15' },
          { text: '📷 Rear Clip (15s)', callback_data: 'cmd:video:back:15' }
        ],
        [
          { text: '🤳 Front Clip (30s)', callback_data: 'cmd:video:front:30' },
          { text: '📷 Rear Clip (30s)', callback_data: 'cmd:video:back:30' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:record': {
    text: '🎙️ <b>Ambient Microphone Forensics</b>\nChoose audio recording duration:',
    keyboard: {
      inline_keyboard: [
        [
          { text: '⏱️ 15 Seconds', callback_data: 'cmd:record:15' },
          { text: '⏱️ 30 Seconds', callback_data: 'cmd:record:30' },
          { text: '⏱️ 60 Seconds', callback_data: 'cmd:record:60' }
        ],
        [
          { text: '🛑 Stop Active Recording', callback_data: 'cmd:record:stop' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:ring': {
    text: '🔊 <b>Emergency Alarm Siren</b>\nTrigger maximum volume siren (overrides silent/vibrate):',
    keyboard: {
      inline_keyboard: [
        [
          { text: '🚨 Sound Siren (30s)', callback_data: 'cmd:ring:30' },
          { text: '🚨 Sound Siren (60s)', callback_data: 'cmd:ring:60' }
        ],
        [
          { text: '🔕 Silence Siren', callback_data: 'cmd:ring_stop' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:track': {
    text: '📍 <b>Continuous Live GPS Tracking</b>\nStream periodic location telemetry updates:',
    keyboard: {
      inline_keyboard: [
        [
          { text: '▶️ Track Every 2 min', callback_data: 'cmd:track:2' },
          { text: '▶️ Track Every 5 min', callback_data: 'cmd:track:5' }
        ],
        [
          { text: '⏹️ Stop Live Tracking', callback_data: 'cmd:track_stop' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:lock': {
    text: '🔒 <b>Device Defense & Lockout Modes</b>\n━━━━━━━━━━━━━━━━━━━━\nChoose an instant defense action:',
    keyboard: {
      inline_keyboard: [
        [
          { text: '🔒 Instant Screen Lock', callback_data: 'cmd:lock' },
          { text: '🔓 Remote Unlock', callback_data: 'cmd:unlock' }
        ],
        [
          { text: '🔑 Lock with PIN 1234', callback_data: 'lock_preset:1234' },
          { text: '🛡️ Set Custom PIN Lock', callback_data: 'wizard:lock:custom' }
        ],
        [
          { text: '🕶️ Fake Shutdown', callback_data: 'cmd:fakeshutdown' },
          { text: '☀️ Wake from Blackout', callback_data: 'cmd:wake' }
        ],
        [
          { text: '🛡️ Device Owner Check', callback_data: 'cmd:device_owner' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:duress': {
    text: '🔑 <b>Emergency Duress Mode</b>\n━━━━━━━━━━━━━━━━━━━━\nIf forced by an intruder to unlock your phone, entering your decoy Duress PIN:\n\n1. Closes the lock overlay as if unlocked (prevents physical danger)\n2. Silently captures front & rear stealth selfies\n3. Obtains high-accuracy GPS coordinates\n4. Transmits an urgent 🚨 SOS Beacon to Telegram!',
    keyboard: {
      inline_keyboard: [
        [
          { text: '✍️ Set New Duress PIN', callback_data: 'wizard:duress:set' },
          { text: '📊 Check Duress Status', callback_data: 'cmd:duress_pin:status' }
        ],
        [
          { text: '❌ Clear Duress PIN', callback_data: 'cmd:duress_pin:clear' },
          { text: '🔙 Back to Tools', callback_data: 'menu:tools' }
        ]
      ]
    }
  },
  'menu:shred': {
    text: '🗑️ <b>Cryptographic File Shredder</b>\n━━━━━━━━━━━━━━━━━━━━\nPermanently overwrite confidential files with 3-pass PRNG noise and zero-fill before deletion.\n\nSelect target directory to sanitize:',
    keyboard: {
      inline_keyboard: [
        [
          { text: '📥 Shred Downloads', callback_data: 'wizard:shred:downloads' },
          { text: '📄 Shred Documents', callback_data: 'wizard:shred:documents' }
        ],
        [
          { text: '📸 Shred Camera Photos', callback_data: 'wizard:shred:camera' }
        ],
        [
          { text: '🔙 Back to Tools', callback_data: 'menu:tools' }
        ]
      ]
    }
  },
  'menu:wipe': {
    text: '⚠️ <b>DESTRUCTIVE ACTION: REMOTE FACTORY RESET</b>\n\nThis will irreversibly erase all user storage, accounts, and cryptographic keys.\nAre you absolutely sure?',
    keyboard: {
      inline_keyboard: [
        [
          { text: '⚠️ CONFIRM FACTORY WIPE', callback_data: 'cmd:wipe' }
        ],
        [
          { text: '❌ Cancel (Safe)', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:location_hub': {
    text:
      `📍 <b>Location & Cellular RF Telemetry Hub</b>\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `Acquire high-precision satellite positioning, indoor cell tower triangulation, and cellular RF telemetry:\n\n` +
      `• <b>Instant GPS Fix:</b> Forcibly powers on hardware GNSS radio and generates pinpoint Google Maps coordinate link.\n` +
      `• <b>Cell Tower Triangulation:</b> Reads multi-SIM MCC, MNC, LAC/TAC, CID, and RSSI (dBm) for zero-satellite indoor tracking.\n` +
      `• <b>Continuous Live Tracking:</b> Autonomous periodic telemetry beacon (2 min / 5 min intervals).\n` +
      `• <b>Safezone Geofence:</b> Arms radial boundary alerting upon perimeter departure.\n` +
      `• <b>SIM Telemetry:</b> Reports active carriers, SIM slots, subscription IDs, and signal strengths.\n\n` +
      `<i>Select action:</i>`,
    keyboard: {
      inline_keyboard: [
        [
          { text: '📍 Instant GPS Fix', callback_data: 'cmd:locate' },
          { text: '📡 Cell Tower Triangulation', callback_data: 'cmd:tower' }
        ],
        [
          { text: '🛰️ Continuous Live Tracking', callback_data: 'menu:track' },
          { text: '🌐 Safezone Geofence', callback_data: 'cmd:geofence' }
        ],
        [
          { text: '📶 SIM & Radio Telemetry', callback_data: 'cmd:sim' },
          { text: '🛡️ SIM Swap Guard', callback_data: 'cmd:sim_lock' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:forensics_hub': {
    text:
      `📸 <b>Covert Forensics & Surveillance Suite</b>\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `Headless, zero-display surveillance streams direct-to-Telegram with instant memory shredding:\n\n` +
      `• <b>Stealth Photos:</b> Zero-blackout front selfie or rear lens capture.\n` +
      `• <b>Silent Screenshot:</b> Non-intrusive Accessibility capture without dialogs.\n` +
      `• <b>Screen Burst:</b> 5–10 frame composite storyboard showing intruder behavior.\n` +
      `• <b>HD Screenrecord:</b> Covert MP4 screen capture (15s–30s).\n` +
      `• <b>Stealth Video:</b> Silent CameraX headless video recording.\n` +
      `• <b>Livestream:</b> Near-realtime progressive camera stream.\n` +
      `• <b>Ambient Mic:</b> High-fidelity PCM/AAC room wiretap.\n` +
      `• <b>Clipboard:</b> Read live clipboard memory.\n\n` +
      `<i>Select forensic tool:</i>`,
    keyboard: {
      inline_keyboard: [
        [
          { text: '🤳 Front Selfie', callback_data: 'cmd:snap:front' },
          { text: '📷 Rear Camera', callback_data: 'cmd:snap:back' },
          { text: '📸 Dual Snap', callback_data: 'cmd:snap:both' }
        ],
        [
          { text: '📱 Screenshot', callback_data: 'cmd:screenshot' },
          { text: '🎞️ Screen Burst (5f)', callback_data: 'cmd:screen_burst:5' }
        ],
        [
          { text: '🎥 Record Video (15s)', callback_data: 'cmd:video:front:15' },
          { text: '📹 Screen Record (15s)', callback_data: 'cmd:screenrecord:15' }
        ],
        [
          { text: '🎙️ Ambient Mic (30s)', callback_data: 'cmd:record:30' },
          { text: '🔴 Live Stream (5f)', callback_data: 'cmd:livestream:front:5' }
        ],
        [
          { text: '📋 Read Clipboard', callback_data: 'cmd:clipboard' },
          { text: '⚙️ More Capture Options', callback_data: 'menu:screen' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:lockdown_hub': {
    text:
      `🚨 <b>Emergency Lockdown & Alarm Hub</b>\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `Instant physical lockout, tactile localization, and deception defenses:\n\n` +
      `• <b>Lock Screen:</b> Enforces full-screen Kiosk defense overlay with recovery banner.\n` +
      `• <b>Custom PIN Lock:</b> Sets 4–8 digit one-time lockout PIN.\n` +
      `• <b>Emergency Siren:</b> Sounds deafening alarm bypassing silent & vibrate modes.\n` +
      `• <b>Tactile Locator:</b> Secret rhythmic vibration patterns without audible alert.\n` +
      `• <b>Fake Shutdown:</b> Simulates OEM power-off with 0-nit blackout canvas.\n` +
      `• <b>Screen Banner:</b> Broadcasts urgent return message onto display.\n\n` +
      `<i>Select lockout action:</i>`,
    keyboard: {
      inline_keyboard: [
        [
          { text: '🔒 Instant Lock', callback_data: 'cmd:lock' },
          { text: '🔓 Remote Unlock', callback_data: 'cmd:unlock' }
        ],
        [
          { text: '🔑 Set Custom PIN Lock', callback_data: 'wizard:lock:custom' },
          { text: '💬 Lockscreen Message', callback_data: 'menu:message' }
        ],
        [
          { text: '🚨 Sound Siren (30s)', callback_data: 'cmd:ring:30' },
          { text: '🔕 Stop Siren', callback_data: 'cmd:ring_stop' }
        ],
        [
          { text: '📳 Tactile SOS Vibrate', callback_data: 'cmd:vibrate_pulse:sos' },
          { text: '📳 Pulse Vibrate', callback_data: 'cmd:vibrate_pulse:pulse' }
        ],
        [
          { text: '🕶️ Fake Shutdown (Blackout)', callback_data: 'cmd:fakeshutdown' },
          { text: '☀️ Wake Device', callback_data: 'cmd:wake' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:device_owner_hub': {
    text:
      `👑 <b>Enterprise Knox Device Owner Suite</b>\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `Low-level hardware policies, HAL-level killswitches, and anti-forensics:\n\n` +
      `• <b>Hardware Camera Lock:</b> Disables all front and rear cameras OS-wide.\n` +
      `• <b>Bluetooth Lock:</b> Disallows all Bluetooth pairings and file transfers.\n` +
      `• <b>Hardware Mic Mute:</b> Mutes all audio recording at the HAL level.\n` +
      `• <b>USB Data Pin Killswitch:</b> Cuts data pins (defeats GrayKey, Cellebrite, juice-jacking).\n` +
      `• <b>Lockscreen Info:</b> Pins permanent owner contact info to OS keyguard.\n` +
      `• <b>Autolock:</b> Enforces custom screen inactivity timeout policy.\n` +
      `• <b>Wi-Fi Provisioning:</b> Forces connection to known Wi-Fi while locked.\n` +
      `• <b>Security Audit:</b> Inspects kernel security logs (ADB shells, KeyStore tampered).\n` +
      `• <b>Encrypted DNS:</b> Enforces system-wide DNS-over-TLS (Quad9/Cloudflare).\n` +
      `• <b>Remote Reboot:</b> Restarts phone hardware remotely.\n\n` +
      `<i>Select Device Owner policy:</i>`,
    keyboard: {
      inline_keyboard: [
        [
          { text: '📷 Cam Lock ON', callback_data: 'cmd:camera_lock:on' },
          { text: '🔓 Cam Lock OFF', callback_data: 'cmd:camera_lock:off' }
        ],
        [
          { text: '📡 BT Lock ON', callback_data: 'cmd:bluetooth_lock:on' },
          { text: '🔓 BT Lock OFF', callback_data: 'cmd:bluetooth_lock:off' }
        ],
        [
          { text: '🔇 Mic Mute ON', callback_data: 'cmd:mic_mute:on' },
          { text: '🔊 Mic Mute OFF', callback_data: 'cmd:mic_mute:off' }
        ],
        [
          { text: '🔌 USB Lock ON', callback_data: 'cmd:usb_lock:on' },
          { text: '🔓 USB Lock OFF', callback_data: 'cmd:usb_lock:off' }
        ],
        [
          { text: '📱 Lockscreen Banner', callback_data: 'wizard:lockscreen_info' },
          { text: '⏱️ Autolock (30s)', callback_data: 'cmd:autolock:30' }
        ],
        [
          { text: '📑 Security Audit Log', callback_data: 'cmd:security_audit' },
          { text: '🔄 Remote Reboot', callback_data: 'cmd:reboot' }
        ],
        [
          { text: '🛡️ Anti-Tamper ON', callback_data: 'cmd:antitamper:on' },
          { text: '🌐 Encrypted DNS (Quad9)', callback_data: 'cmd:dns:quad9' }
        ],
        [
          { text: '👑 Verify DO Status', callback_data: 'cmd:device_owner' },
          { text: '✨ Self-Heal Permissions', callback_data: 'cmd:self_heal' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:traps_hub': {
    text:
      `🛡️ <b>Autonomous Sensor Traps & Anti-Theft Hub</b>\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `Autonomous on-device edge detection triggers instantly without remote commands:\n\n` +
      `• <b>Kinetic Snatch Trap:</b> Accelerometer detects violent grabs (>2.6G), locks phone, snaps mugshot, and alerts.\n` +
      `• <b>Charger Disconnect Trap:</b> Alerts immediately if AC power is unplugged while locked.\n` +
      `• <b>Pocket / Bag Extraction:</b> Triggers if proximity sensor is uncovered while locked without unlock.\n` +
      `• <b>Failed Pattern Guard:</b> Snaps stealth selfies upon repeated wrong PIN / pattern attempts.\n` +
      `• <b>SIM Swap Guard:</b> Locks device if unknown SIM card is inserted.\n` +
      `• <b>App Network Firewall:</b> Isolates malicious background apps from outbound telemetry.\n` +
      `• <b>Battery Drain Alert:</b> Alerts on rapid battery drain detecting background wiretaps.\n` +
      `• <b>Decoy Duress PIN:</b> Coercion unlock that opens sterile decoy OS and broadcasts SOS.\n\n` +
      `<i>Manage sensor traps:</i>`,
    keyboard: {
      inline_keyboard: [
        [
          { text: '🟢 Arm All Traps', callback_data: 'cmd:trap:on' },
          { text: '🔴 Disarm All Traps', callback_data: 'cmd:trap:off' }
        ],
        [
          { text: '🏃 Snatch Trap', callback_data: 'cmd:trap:snatch' },
          { text: '🔌 Charger Trap', callback_data: 'cmd:trap:charger' }
        ],
        [
          { text: '👁️ Pattern Guard (3 tries)', callback_data: 'cmd:pattern_guard:enable' },
          { text: '🛡️ SIM Swap Guard', callback_data: 'cmd:sim_lock:enable' }
        ],
        [
          { text: '🧱 App Firewall ON', callback_data: 'cmd:app_firewall:enable' },
          { text: '🔋 Battery Alert ON', callback_data: 'cmd:battery_alert:enable' }
        ],
        [
          { text: '🆘 Decoy Duress PIN', callback_data: 'menu:duress' },
          { text: '🔍 Anti-Tamper Scan', callback_data: 'cmd:tamper_detect' }
        ],
        [
          { text: '📊 Traps Telemetry Status', callback_data: 'cmd:trap:status' },
          { text: '👁️ Stealth Launcher Cloak', callback_data: 'menu:stealth' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  },
  'menu:data_hub': {
    text:
      `📇 <b>Extraction, Telemetry & System Logs Hub</b>\n` +
      `━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
      `Extract forensic data, inspect communications, manage apps, and review audit trails:\n\n` +
      `• <b>Contacts:</b> Query device address book names & phone numbers.\n` +
      `• <b>Call History:</b> View recent incoming, outgoing, and missed calls.\n` +
      `• <b>SMS Inbox:</b> View recent SMS text messages and 2FA OTP codes.\n` +
      `• <b>Direct Outbound SMS:</b> Dispatch SMS message via SIM slot directly.\n` +
      `• <b>Audit Trail:</b> Inspect recent command execution history.\n` +
      `• <b>Installed Apps:</b> List installed applications and package identifiers.\n` +
      `• <b>Network Telemetry:</b> Current IP, Wi-Fi SSID, and cellular network status.\n\n` +
      `<i>Select extraction option:</i>`,
    keyboard: {
      inline_keyboard: [
        [
          { text: '👥 Contacts', callback_data: 'cmd:contacts' },
          { text: '📞 Call Log', callback_data: 'cmd:call_log' }
        ],
        [
          { text: '💬 SMS Inbox', callback_data: 'cmd:sms_log' },
          { text: '✉️ Send Outbound SMS', callback_data: 'wizard:sendsms' }
        ],
        [
          { text: '📜 Command Audit Trail', callback_data: 'cmd:history' },
          { text: '📦 Installed Apps', callback_data: 'cmd:apps' }
        ],
        [
          { text: '🌐 Network Telemetry', callback_data: 'cmd:network' },
          { text: '📲 SMS TOTP Setup', callback_data: 'cmd:smssetup' }
        ],
        [
          { text: '🗑️ File Shredder', callback_data: 'menu:shred' },
          { text: '⚠️ Remote Factory Wipe', callback_data: 'menu:wipe' }
        ],
        [
          { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
        ]
      ]
    }
  }
};

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function getActiveDeviceForChat(token, chatId) {
  const matching = Object.values(devices)
    .filter(d => d.botToken === token && (d.ownerChatId == chatId || !d.ownerChatId))
    .sort((a, b) => (b.lastSeen || 0) - (a.lastSeen || 0));
  return matching[0] || null;
}

function buildDashboardText(chatId, activeDev) {
  let statusBadge = '🔴 Offline';
  let deviceDetails = '';

  if (activeDev) {
    const diffSec = Math.floor((Date.now() - (activeDev.lastSeen || 0)) / 1000);
    if (diffSec < 90) {
      statusBadge = `🟢 Online (${diffSec}s ago)`;
    } else if (diffSec < 600) {
      statusBadge = `🟡 Idle (${Math.floor(diffSec / 60)}m ago)`;
    } else {
      const hours = Math.floor(diffSec / 3600);
      statusBadge = hours > 0 ? `🔴 Offline (${hours}h ago)` : `🔴 Offline (${Math.floor(diffSec / 60)}m ago)`;
    }

    let licBadge = '⏳ Standard / Trial';
    try {
      const lic = licensing.getDeviceLicenseStatus(activeDev.deviceId);
      if (lic.tier === 'PRO_LIFETIME') licBadge = '💎 Pro Lifetime';
      else if (lic.tier === 'PRO_ANNUAL') licBadge = '⭐ Pro Annual';
      else if (lic.tier === 'PRO_ENTERPRISE') licBadge = '🏢 Enterprise';
      else if (lic.tier === 'FREE_TRIAL') licBadge = `⏳ Trial (${lic.daysLeft}d left)`;
    } catch (_) {}

    const hwKeyBadge = activeDev.publicKeyJwk ? '🛡️ StrongBox TEE' : '🔒 Software Keystore';

    deviceDetails =
      `📱 <b>Linked Device:</b> <b>${escapeHtml(activeDev.deviceName || 'Android Device')}</b>\n` +
      `🆔 <b>Device ID:</b> <code>${escapeHtml(activeDev.deviceId)}</code>\n` +
      `📡 <b>Connection:</b> ${statusBadge}\n` +
      `👑 <b>Security Posture:</b> ${licBadge} | ${hwKeyBadge}`;
  } else {
    deviceDetails =
      `⚠️ <b>No Active Device Linked</b>\n` +
      `<i>Complete initial setup on your Android device to bind this Telegram console.</i>`;
  }

  return (
    `🛡️ <b>PASA SENTINEL — COMMAND CONSOLE</b>\n` +
    `<i>Sovereign Mobile Defense & Covert Counter-Surveillance</i>\n` +
    `━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
    `👤 <b>Operator Chat ID:</b> <code>${chatId}</code>\n` +
    `${deviceDetails}\n` +
    `━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
    `Select a command center hub below to dispatch authenticated actions:`
  ).trim();
}

async function dispatchCommandToDevice(token, chatId, command, args = [], notifyTelegram = true) {
  const matchingDeviceIds = Object.keys(devices)
    .filter(id => {
      const d = devices[id];
      return d.botToken === token && (d.ownerChatId == chatId || !d.ownerChatId);
    })
    .sort((a, b) => (devices[b].lastSeen || 0) - (devices[a].lastSeen || 0));

  if (matchingDeviceIds.length === 0) {
    if (notifyTelegram) {
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: '⚠️ No PASA device registered yet. Please complete setup on your phone.',
        parse_mode: 'HTML'
      });
    }
    return;
  }

  const targetDeviceId = matchingDeviceIds[0];
  const cmdId = 'cmd_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
  const action = command.replace(/^\//, '').toUpperCase();
  const formattedCmd = args.length > 0 ? `${command} ${args.join(' ')}` : command;

  // Target the active device (and any recent device seen within last 30 minutes)
  const recentCutoff = Date.now() - 30 * 60 * 1000;
  const targetDeviceIds = matchingDeviceIds.filter(id => id === targetDeviceId || (devices[id].lastSeen || 0) > recentCutoff);

  // Enqueue for relevant devices and notify long-pollers immediately
  for (const devId of targetDeviceIds) {
    if (!commands[devId]) commands[devId] = [];
    const signedEnvelope = signCommandEnvelope(devId, action, args, chatId, cmdId);

    const cmdRecord = {
      id: cmdId,
      command,
      args,
      chatId,
      createdAt: Date.now(),
      envelope: signedEnvelope
    };
    commands[devId].push(cmdRecord);
    persistCommand(devId, cmdRecord);

    // Notify any active HTTP long-poll connection for this device
    commandEmitter.emit('command:' + devId, commands[devId]);
  }

  logSecurityEvent('COMMAND_DISPATCHED', {
    command: formattedCmd,
    deviceId: targetDeviceId,
    chatId: chatId ? String(chatId) : 'unknown'
  });

  const activeDevice = devices[targetDeviceId];
  const lastSeenSec = Math.floor((Date.now() - (activeDevice.lastSeen || 0)) / 1000);
  const statusNote = lastSeenSec < 60 ? `Online (${lastSeenSec}s ago)` : `Last active ${lastSeenSec}s ago`;

  if (notifyTelegram) {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: `⏳ Command <code>${formattedCmd}</code> signed & dispatched to <b>${activeDevice.deviceName || targetDeviceId}</b> (${statusNote}).\n\nAwaiting telemetry...`,
      parse_mode: 'HTML'
    });
  }

  // Safety watchdog: alert Telegram only if the command is STILL pending after its allotted time.
  // Media commands (/record, /snap, /video) are given 300s; everything else 120s.
  const MEDIA_COMMANDS = ['/record', '/audio', '/mic', '/snap', '/photo', '/camera', '/video', '/videocap', '/vr'];
  const watchdogMs = MEDIA_COMMANDS.includes(command) ? 300000 : 120000;
  setTimeout(async () => {
    try {
      if (!notifyTelegram) return;
      // If the command was already completed (removeCommand was called), skip alert entirely.
      if (completedCommandIds.has(cmdId)) {
        completedCommandIds.delete(cmdId); // GC: remove after watchdog window passes
        return;
      }
      let wasPending = false;
      for (const devId of targetDeviceIds) {
        if (commands[devId] && commands[devId].some(c => c.id === cmdId)) {
          removeCommand(devId, cmdId, 'TIMEOUT');
          wasPending = true;
        }
      }
      // Double-check: was it completed between the Set check and the array check?
      if (!wasPending || completedCommandIds.has(cmdId)) {
        completedCommandIds.delete(cmdId);
        return;
      }
      const watchdogSec = Math.round(watchdogMs / 1000);
      const currentDev = devices[targetDeviceId] || {};
      const secAgo = Math.floor((Date.now() - (currentDev.lastSeen || 0)) / 1000);
      console.warn(`[Command Watchdog] Command ${cmdId} (${command}) timed out after ${watchdogSec}s for ${targetDeviceId}`);

      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: `⚠️ <b>Command Timeout:</b> <code>${formattedCmd}</code> did not receive telemetry within ${watchdogSec}s.\n\n` +
              `📱 <b>Device:</b> ${currentDev.deviceName || targetDeviceId}\n` +
              `⏱️ <b>Last Check-in:</b> ${secAgo}s ago\n\n` +
              `<i>Note: If the phone is locked, ensure Battery Optimization is set to "Unrestricted" in device App Info.</i>`,
        parse_mode: 'HTML'
      });
    } catch (watchdogErr) {
      console.error('[Command Watchdog] Error in timeout handler:', watchdogErr.message);
    }
  }, watchdogMs);
}

// Dedicated instant OTA release handler for Telegram
async function handleCheckUpdateCommand(token, chatId, args = []) {
  let latest = ReleaseRepo.getLatest();
  if (!latest) {
    const releases = loadJson(RELEASES_FILE, []);
    if (Array.isArray(releases) && releases.length > 0) {
      latest = releases[0];
    }
  }

  if (!latest) {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: '🔄 <b>PASA Sentinel OTA Center</b>\n━━━━━━━━━━━━━━━━━━━━\n⚠️ <i>No published app releases found on server.</i>',
      parse_mode: 'HTML',
      reply_markup: DASHBOARD_KEYBOARD
    });
    return;
  }

  const activeDev = getActiveDeviceForChat(token, chatId);
  const sizeMb = latest.fileSize ? (latest.fileSize / (1024 * 1024)).toFixed(1) + ' MB' : '18.1 MB';
  const pubDate = latest.publishedAt ? new Date(latest.publishedAt).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' }) : 'Recent';
  const cleanChangelog = (latest.changelog || 'Performance & security improvements.')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/&amp;lt;/g, '&lt;')
    .replace(/&amp;gt;/g, '&gt;')
    .replace(/&amp;amp;/g, '&amp;');

  let deviceStatusStr = '';
  if (activeDev) {
    const lastSeenSec = Math.floor((Date.now() - (activeDev.lastSeen || 0)) / 1000);
    const statusNote = lastSeenSec < 60 ? `Online (${lastSeenSec}s ago)` : `Last active ${lastSeenSec}s ago`;
    deviceStatusStr = `📱 <b>Linked Device:</b> ${activeDev.deviceName || activeDev.deviceId} (<code>${statusNote}</code>)\n`;
  }

  const updateCardText =
    `🔄 <b>PASA Sentinel OTA Release Center</b>\n` +
    `━━━━━━━━━━━━━━━━━━━━\n` +
    `📦 <b>Latest Sovereign Build:</b> v${latest.versionName} (Build ${latest.versionCode})\n` +
    `📅 <b>Release Date:</b> ${pubDate}\n` +
    `💾 <b>Package Size:</b> ${sizeMb}\n` +
    `🔒 <b>SHA-256:</b> <code>${(latest.sha256 || '').substring(0, 16)}...</code>\n` +
    deviceStatusStr +
    `\n📋 <b>What's New:</b>\n<i>${cleanChangelog}</i>\n\n` +
    `<i>Tap below to download APK directly or push remote update to your device:</i>`;

  const downloadUrl = latest.downloadUrl || `https://pasa.izhaanintellect.fun/releases/${latest.filename || 'pasa-sentinel-latest.apk'}`;

  const keyboardButtons = [
    [
      { text: `⬇️ Download APK (v${latest.versionName})`, url: downloadUrl }
    ]
  ];

  if (activeDev) {
    keyboardButtons.push([
      { text: '⚡ Install Update on Phone', callback_data: 'dev_cmd:update_confirm' }
    ]);
  }

  keyboardButtons.push([
    { text: '🔄 Refresh', callback_data: 'cmd:check_update' },
    { text: '🔙 Dashboard', callback_data: 'menu:main' }
  ]);

  await callTelegram(token, 'sendMessage', {
    chat_id: chatId,
    text: updateCardText,
    parse_mode: 'HTML',
    reply_markup: {
      inline_keyboard: keyboardButtons
    }
  });

  // Also query the active device in the background silently
  if (activeDev) {
    await dispatchCommandToDevice(token, chatId, '/check_update', [], false);
  }
}

// Telegram Bot long-poller loop
function startBotPoller(token) {
  if (!token || typeof token !== 'string' || token.trim().length === 0) return;
  token = token.trim();

  if (activePollers.has(token) && activePollers.get(token).isRunning) {
    return;
  }

  const pollerState = { isRunning: true, offset: 0 };
  activePollers.set(token, pollerState);

  console.log(`[Telegram Poller] Started poller for token: ...${token.slice(-8)}`);
  registerTelegramBotCommands(token);

  (async () => {
    while (pollerState.isRunning) {
      try {
        const url = `https://api.telegram.org/bot${token}/getUpdates?timeout=25${pollerState.offset ? `&offset=${pollerState.offset}` : ''}`;
        const res = await fetch(url);
        const contentType = res.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) {
          console.warn(`[Telegram Poller] Unexpected content-type (${res.status}): ${contentType}`);
          await new Promise(r => setTimeout(r, 5000));
          continue;
        }

        const data = await res.json();

        if (data && data.ok && Array.isArray(data.result)) {
          for (const update of data.result) {
            pollerState.offset = update.update_id + 1;
            try {
              await handleTelegramUpdate(token, update);
            } catch (handlerErr) {
              console.error('[Telegram Poller] Error in update handler:', handlerErr);
            }
          }
        } else if (data && !data.ok) {
          console.warn(`[Telegram Poller] API returned not OK:`, data.description);
          await new Promise(r => setTimeout(r, 5000));
        }
      } catch (err) {
        console.error('[Telegram Poller] Network error, backing off 5s:', err.message);
        await new Promise(r => setTimeout(r, 5000));
      }
    }
  })();
}

// Handle an incoming update from Telegram (both messages and callback button clicks)
async function handleTelegramUpdate(token, update) {
  // 1. Handle Inline Keyboard Button Clicks
  if (update.callback_query) {
    const query = update.callback_query;
    const chatId = query.message.chat.id;
    const messageId = query.message.message_id;
    const data = query.data || '';

    console.log(`[Telegram Callback] Received: ${data} from chatId: ${chatId}`);

    // Immediately acknowledge callback query with brief tactile toast
    await callTelegram(token, 'answerCallbackQuery', {
      callback_query_id: query.id,
      text: '⏳ Action processing...'
    });

    // Two-Step Binance Pay Order Approval / Rejection Handlers
    if (data.startsWith('lic:approve:') || data.startsWith('lic:reject:')) {
      if (String(chatId) !== String(ADMIN_CHAT_ID)) {
        console.warn(`[Licensing Security] Unauthorized license action attempt from chatId: ${chatId}. Expected admin: ${ADMIN_CHAT_ID}`);
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `⛔ <b>Access Denied:</b> Only designated Administrator (<code>${ADMIN_CHAT_ID}</code>) can verify or approve licenses.`,
          parse_mode: 'HTML'
        });
        return;
      }
    }

    if (data.startsWith('lic:approve:')) {
      const orderId = data.substring('lic:approve:'.length);
      const order = licenseOrders[orderId];
      if (!order) {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `⚠️ Order <code>${orderId}</code> was not found.`,
          parse_mode: 'HTML'
        });
        return;
      }
      if (order.status === 'APPROVED') {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `ℹ️ Order <code>${orderId}</code> is already marked as <b>APPROVED</b>.`,
          parse_mode: 'HTML'
        });
        return;
      }

      order.status = 'APPROVED';
      order.approvedAt = Date.now();
      saveJson(ORDERS_FILE, licenseOrders);

      const methodLabel = order.provider === 'bkash' ? 'bKash Send Money' : 'Binance Pay';
      await callTelegram(token, 'editMessageText', {
        chat_id: chatId,
        message_id: messageId,
        text: `✅ <b>PAYMENT VERIFIED & APPROVED!</b>\n` +
              `━━━━━━━━━━━━━━━━━━━━\n` +
              `<b>Order ID:</b> <code>${orderId}</code>\n` +
              `<b>Method:</b> ${methodLabel}\n` +
              `<b>Plan:</b> ${order.tier} (${order.price || ''})\n` +
              `<b>Buyer:</b> <code>${order.email}</code>\n` +
              `<b>Submitted TX:</b> <code>${order.txId || order.binanceTxId || 'N/A'}</code>\n` +
              `<b>Active Key:</b> <code>${order.licenseKey}</code>\n\n` +
              `🛡️ <i>Payment confirmed. License key remains ACTIVE.</i>`,
        parse_mode: 'HTML'
      });
      return;
    }

    if (data.startsWith('lic:reject:')) {
      const orderId = data.substring('lic:reject:'.length);
      const order = licenseOrders[orderId];
      if (!order) {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `⚠️ Order <code>${orderId}</code> was not found.`,
          parse_mode: 'HTML'
        });
        return;
      }
      if (order.status === 'REJECTED') {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `ℹ️ Order <code>${orderId}</code> is already marked as <b>REJECTED</b>.`,
          parse_mode: 'HTML'
        });
        return;
      }

      order.status = 'REJECTED';
      order.rejectedAt = Date.now();
      saveJson(ORDERS_FILE, licenseOrders);

      if (order.licenseKey && typeof licensing.revokeLicense === 'function') {
        licensing.revokeLicense(order.licenseKey, 'PAYMENT_REJECTED_BY_ADMIN');
      }

      const methodLabel = order.provider === 'bkash' ? 'bKash Send Money' : 'Binance Pay';
      await callTelegram(token, 'editMessageText', {
        chat_id: chatId,
        message_id: messageId,
        text: `❌ <b>ORDER REJECTED & KEY REVOKED</b>\n` +
              `━━━━━━━━━━━━━━━━━━━━\n` +
              `<b>Order ID:</b> <code>${orderId}</code>\n` +
              `<b>Method:</b> ${methodLabel}\n` +
              `<b>Buyer:</b> <code>${order.email}</code>\n` +
              `<b>Submitted TX:</b> <code>${order.txId || order.binanceTxId || 'None'}</code>\n` +
              `<b>Revoked Key:</b> <code>${order.licenseKey || 'None'}</code>\n\n` +
              `🚫 <i>Voided. The license key has been revoked and can no longer activate any device.</i>`,
        parse_mode: 'HTML'
      });
      return;
    }

    if (data === 'menu:main') {
      clearChatState(chatId);
      const activeDev = getActiveDeviceForChat(token, chatId);
      const text = buildDashboardText(chatId, activeDev);
      await callTelegram(token, 'editMessageText', {
        chat_id: chatId,
        message_id: messageId,
        text: text,
        parse_mode: 'HTML',
        reply_markup: DASHBOARD_KEYBOARD
      });
      return;
    }

    if (data === 'menu:license') {
      clearChatState(chatId);
      const activeDev = getActiveDeviceForChat(token, chatId);
      let statusDetails = '';
      if (!activeDev) {
        statusDetails = '⚠️ <i>No active device linked to this chat yet.</i>';
      } else {
        const lic = licensing.getDeviceLicenseStatus(activeDev.deviceId);
        const tierBadge = lic.tier === 'PRO_LIFETIME' ? '💎 Pro Lifetime (Sovereign)' :
                          lic.tier === 'PRO_ANNUAL' ? '⭐ Pro Annual' :
                          lic.tier === 'PRO_ENTERPRISE' ? '🏢 Fleet / Enterprise' :
                          lic.tier === 'FREE_TRIAL' ? '⏳ 7-Day Free Trial (Active)' : '❌ Trial Expired';
        const remaining = lic.daysLeft > 9000 ? 'Permanent Sovereign Access' : `${lic.daysLeft} day(s) remaining`;
        statusDetails = `<b>Device:</b> <code>${activeDev.deviceId}</code> (${activeDev.deviceName || 'Android'})\n` +
                        `<b>License Tier:</b> ${tierBadge}\n` +
                        `<b>Status:</b> <code>${lic.status}</code>\n` +
                        `<b>Validity:</b> ${remaining}\n` +
                        (lic.licenseKey ? `<b>Bound Key:</b> <code>${lic.licenseKey}</code>\n` : '');
      }

      const licKeyboard = {
        inline_keyboard: [
          [
            { text: '🔑 Activate License Key', callback_data: 'wizard:license:activate' },
            { text: '🔄 Refresh Status', callback_data: 'menu:license' }
          ],
          [
            { text: '🛒 Buy / Upgrade Pro License', url: 'https://pasa.izhaanintellect.fun/#pricing' }
          ],
          [
            { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
          ]
        ]
      };

      await callTelegram(token, 'editMessageText', {
        chat_id: chatId,
        message_id: messageId,
        text: `🔐 <b>PASA Commercial Licensing & Pro Status</b>\n━━━━━━━━━━━━━━━━━━━━\n${statusDetails}\n\n<i>To bind a purchased key, tap <b>Activate License Key</b> below or send:</i>\n<code>/license activate PASA-PRO-XXXX-XXXX</code>`,
        parse_mode: 'HTML',
        reply_markup: licKeyboard
      });
      return;
    }

    if (data === 'wizard:license:activate') {
      setChatState(chatId, 'WAITING_FOR_LICENSE_KEY');
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: '🔑 <b>Activate PASA Pro License</b>\n━━━━━━━━━━━━━━━━━━━━\nPlease reply with your License Key (e.g. <code>PASA-PRO-XXXX-XXXX-XXXX</code>):',
        parse_mode: 'HTML',
        reply_markup: {
          inline_keyboard: [[{ text: '❌ Cancel', callback_data: 'cancel:wizard' }]]
        }
      });
      return;
    }

    if (SUBMENUS[data]) {
      clearChatState(chatId);
      const sub = SUBMENUS[data];
      await callTelegram(token, 'editMessageText', {
        chat_id: chatId,
        message_id: messageId,
        text: sub.text,
        parse_mode: 'HTML',
        reply_markup: sub.keyboard
      });
      return;
    }

    // Quick Message Presets
    if (data.startsWith('msg_preset:')) {
      const preset = data.split(':')[1];
      let msg = "Lost phone! Please call owner immediately.";
      if (preset === 'stolen') msg = "Stolen device! Police GPS tracking is active on this phone.";
      if (preset === 'reward') msg = "Reward offered if returned! Please contact the owner.";
      await dispatchCommandToDevice(token, chatId, '/message', [msg]);
      return;
    }

    // Interactive Screen Message Wizard
    if (data === 'wizard:msg:custom') {
      setChatState(chatId, 'WAITING_FOR_SCREEN_MESSAGE');
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: '✍️ <b>Custom Screen Message</b>\n━━━━━━━━━━━━━━━━━━━━\nPlease type your message below to broadcast it directly onto the phone screen:',
        parse_mode: 'HTML',
        reply_markup: {
          inline_keyboard: [[{ text: '❌ Cancel', callback_data: 'cancel:wizard' }]]
        }
      });
      return;
    }

    // Lock Presets & Wizard
    if (data === 'lock_preset:1234') {
      await dispatchCommandToDevice(token, chatId, '/lock', ['1234', 'Lost Mode Active']);
      return;
    }

    if (data === 'wizard:lock:custom') {
      setChatState(chatId, 'WAITING_FOR_LOCK_PIN');
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: '🔒 <b>Set Custom Lock PIN</b>\n━━━━━━━━━━━━━━━━━━━━\nPlease reply with a 4 to 8 digit PIN (e.g. <code>5892</code>) to lock the screen with:',
        parse_mode: 'HTML',
        reply_markup: {
          inline_keyboard: [[{ text: '❌ Cancel', callback_data: 'cancel:wizard' }]]
        }
      });
      return;
    }

    // Duress Wizard
    if (data === 'wizard:duress:set') {
      setChatState(chatId, 'WAITING_FOR_DURESS_PIN');
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: '🔑 <b>Configure Emergency Duress PIN</b>\n━━━━━━━━━━━━━━━━━━━━\nPlease reply with a 4 to 8 digit decoy PIN (e.g. <code>9999</code>).\n\n<i>Entering this PIN on the locked phone closes the screen while secretly snapping photos and broadcasting an SOS beacon.</i>',
        parse_mode: 'HTML',
        reply_markup: {
          inline_keyboard: [[{ text: '❌ Cancel', callback_data: 'cancel:wizard' }]]
        }
      });
      return;
    }

    // File Shredder Wizard
    if (data.startsWith('wizard:shred:')) {
      const target = data.split(':')[2] || 'downloads';
      setChatState(chatId, 'WAITING_FOR_SHRED_PASSWORD', { target });
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: `⚠️ <b>CONFIRM CRYPTOGRAPHIC SHRED: ${target.toUpperCase()}</b>\n━━━━━━━━━━━━━━━━━━━━\nThis will permanently overwrite and destroy all files in <code>${target}</code> with 3-pass PRNG noise.\n\nPlease reply with your <b>Master Password</b> to authorize:`,
        parse_mode: 'HTML',
        reply_markup: {
          inline_keyboard: [[{ text: '❌ Cancel', callback_data: 'cancel:wizard' }]]
        }
      });
      return;
    }

    // Lockscreen Emergency Banner Wizard
    if (data === 'wizard:lockscreen_info') {
      setChatState(chatId, 'WAITING_FOR_LOCKSCREEN_INFO');
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: '📱 <b>Set Lockscreen Emergency Info</b>\n━━━━━━━━━━━━━━━━━━━━\nPlease reply with the owner contact or recovery message you want pinned to the Android lockscreen display:\n\n<i>Example:</i> <code>Owner: John Doe (+1-555-0199). If found, please return.</code>',
        parse_mode: 'HTML',
        reply_markup: {
          inline_keyboard: [[{ text: '❌ Cancel', callback_data: 'cancel:wizard' }]]
        }
      });
      return;
    }

    // Send SMS Wizard
    if (data === 'wizard:sendsms') {
      setChatState(chatId, 'WAITING_FOR_SENDSMS');
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: '✉️ <b>Send Outbound SMS via Device</b>\n━━━━━━━━━━━━━━━━━━━━\nPlease reply with the recipient phone number and message in format:\n<code>&lt;number&gt; &lt;message&gt;</code>\n\n<i>Example:</i> <code>+1234567890 Hello from PASA Sentinel</code>',
        parse_mode: 'HTML',
        reply_markup: {
          inline_keyboard: [[{ text: '❌ Cancel', callback_data: 'cancel:wizard' }]]
        }
      });
      return;
    }

    // Cancel Active Wizard
    if (data === 'cancel:wizard') {
      clearChatState(chatId);
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: '❌ Action cancelled.',
        parse_mode: 'HTML',
        reply_markup: DASHBOARD_KEYBOARD
      });
      return;
    }

    if (data === 'cmd:check_update') {
      await handleCheckUpdateCommand(token, chatId);
      return;
    }

    if (data === 'dev_cmd:update_confirm') {
      await dispatchCommandToDevice(token, chatId, '/update_confirm', []);
      return;
    }

    if (data.startsWith('cmd:')) {
      const parts = data.split(':');
      const cmdName = '/' + parts[1];
      const cmdArgs = parts.slice(2);
      await dispatchCommandToDevice(token, chatId, cmdName, cmdArgs);
      return;
    }
    return;
  }

  // 2. Handle Text Messages
  if (!update.message || !update.message.text) return;

  const chatId = update.message.chat.id;
  const rawText = update.message.text.trim();
  const lowerText = rawText.toLowerCase();

  console.log(`[Telegram Message] Received from chatId ${chatId}: "${rawText}"`);

  // 1. Check Active Conversational State (Wizard inputs) FIRST
  const activeState = getChatState(chatId);
  if (activeState) {
    if (activeState.state === 'WAITING_FOR_SCREEN_MESSAGE') {
      clearChatState(chatId);
      await dispatchCommandToDevice(token, chatId, '/message', [rawText]);
      return;
    }

    if (activeState.state === 'WAITING_FOR_LOCK_PIN') {
      if (!/^\d{4,8}$/.test(rawText)) {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: '⚠️ <b>Invalid PIN:</b> Must be 4 to 8 digits (e.g. <code>5892</code>). Please try again or tap Cancel.',
          parse_mode: 'HTML',
          reply_markup: {
            inline_keyboard: [[{ text: '❌ Cancel', callback_data: 'cancel:wizard' }]]
          }
        });
        return;
      }
      clearChatState(chatId);
      await dispatchCommandToDevice(token, chatId, '/lock', [rawText, 'Lost Mode Active']);
      return;
    }

    if (activeState.state === 'WAITING_FOR_DURESS_PIN') {
      if (!/^\d{4,8}$/.test(rawText)) {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: '⚠️ <b>Invalid PIN:</b> Must be 4 to 8 digits. Please try again or tap Cancel.',
          parse_mode: 'HTML',
          reply_markup: {
            inline_keyboard: [[{ text: '❌ Cancel', callback_data: 'cancel:wizard' }]]
          }
        });
        return;
      }
      clearChatState(chatId);
      await dispatchCommandToDevice(token, chatId, '/duress_pin', [rawText]);
      return;
    }

    if (activeState.state === 'WAITING_FOR_SHRED_PASSWORD') {
      const target = activeState.data?.target || 'downloads';
      clearChatState(chatId);
      await dispatchCommandToDevice(token, chatId, '/shred', [rawText, target]);
      return;
    }

    if (activeState.state === 'WAITING_FOR_LOCKSCREEN_INFO') {
      clearChatState(chatId);
      await dispatchCommandToDevice(token, chatId, '/lockscreen_info', [rawText]);
      return;
    }

    if (activeState.state === 'WAITING_FOR_SENDSMS') {
      clearChatState(chatId);
      const parts = rawText.split(/\s+/);
      const number = parts[0];
      const smsBody = parts.slice(1).join(' ');
      if (!number || !smsBody) {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: '⚠️ <b>Invalid Format:</b> Please provide both recipient number and message (e.g. <code>+1234567890 Hello</code>).',
          parse_mode: 'HTML'
        });
        return;
      }
      await dispatchCommandToDevice(token, chatId, '/sendsms', [number, smsBody]);
      return;
    }

    if (activeState.state === 'WAITING_FOR_LICENSE_KEY') {
      clearChatState(chatId);
      const cleanKey = rawText.trim().toUpperCase();
      const activeDev = getActiveDeviceForChat(token, chatId);
      if (!activeDev) {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: '⚠️ <b>Activation Failed:</b> No active device linked to this chat.',
          parse_mode: 'HTML'
        });
        return;
      }
      const actRes = licensing.activateLicense(cleanKey, activeDev.deviceId);
      if (actRes.ok) {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `🎉 <b>License Activated Successfully!</b>\n━━━━━━━━━━━━━━━━━━━━\n` +
                `⭐ <b>Tier:</b> ${actRes.tier}\n` +
                `📱 <b>Device:</b> ${activeDev.deviceName}\n` +
                `🔑 <b>Key:</b> <code>${cleanKey}</code>\n\n` +
                `All sovereign defensive capabilities and continuous OTA updates are now permanently unlocked.`,
          parse_mode: 'HTML'
        });
      } else {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `❌ <b>License Activation Failed</b>\n${actRes.message || 'Invalid or revoked license key.'}`,
          parse_mode: 'HTML'
        });
      }
      return;
    }
  }

  // --- Method 2: Instant 6-Digit Pairing Handler (@Pas_agent_bot) ---
  let possiblePairCode = null;
  const startPairMatch = rawText.match(/^\/start\s+(?:pair_)?(\d{6})$/i);
  if (startPairMatch) {
    possiblePairCode = startPairMatch[1];
  } else {
    // Only check bare 6-digit numbers if the chat has no active linked device, or if explicitly prefixed with pair
    const activeDev = getActiveDeviceForChat(token, chatId);
    const isExplicitPair = /^(?:\/)?pair\b/i.test(rawText);

    if (!activeDev || isExplicitPair) {
      const directDigits = rawText.replace(/\s+/g, '');
      if (/^\d{6}$/.test(directDigits)) {
        possiblePairCode = directDigits;
      } else {
        const pairPrefixMatch = rawText.match(/^(?:pair\s+)?(\d{3})\s*(\d{3})$/i);
        if (pairPrefixMatch) {
          possiblePairCode = pairPrefixMatch[1] + pairPrefixMatch[2];
        }
      }
    }
  }

  if (possiblePairCode) {
    const attemptInfo = telegramPairingAttempts.get(chatId) || { attempts: 0, lockedUntil: 0 };
    if (attemptInfo.lockedUntil > Date.now()) {
      const waitMin = Math.ceil((attemptInfo.lockedUntil - Date.now()) / (60 * 1000));
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: `⛔ <b>Too Many Failed Attempts:</b> You are temporarily locked from device pairing. Please wait ${waitMin} minutes before trying again.`,
        parse_mode: 'HTML'
      });
      return;
    }

    const entry = activePairings.get(possiblePairCode);
    if (entry && entry.status === 'PENDING' && entry.expiresAt > Date.now()) {
      entry.status = 'CLAIMED';
      entry.ownerChatId = String(chatId);
      telegramPairingAttempts.delete(chatId);

      if (devices[entry.deviceId]) {
        devices[entry.deviceId].ownerChatId = String(chatId);
        persistDevice(devices[entry.deviceId]);
      }

      pairingEmitter.emit('paired:' + possiblePairCode, {
        deviceId: entry.deviceId,
        ownerChatId: String(chatId)
      });

      logSecurityEvent('DEVICE_PAIRED_INSTANT_OTP', {
        deviceId: entry.deviceId,
        chatId: String(chatId),
        code: possiblePairCode
      });

      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: `🎉 <b>PASA Sentinel Paired Successfully!</b>\n━━━━━━━━━━━━━━━━━━━━\n` +
              `📱 <b>Device:</b> ${entry.deviceName || 'Android Device'}\n` +
              `🆔 <b>ID:</b> <code>${entry.deviceId}</code>\n` +
              `🛡️ <b>Control Plane:</b> Sovereign Hardware Protection Active\n\n` +
              `Your Android device is now securely linked to this Telegram account. You can dispatch commands or use the interactive tactical console below:`,
        parse_mode: 'HTML',
        reply_markup: PERSISTENT_REPLY_KEYBOARD
      });

      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: buildDashboardText(chatId, devices[entry.deviceId] || { deviceName: entry.deviceName, deviceId: entry.deviceId }),
        parse_mode: 'HTML',
        reply_markup: DASHBOARD_KEYBOARD
      });
      return;
    } else {
      attemptInfo.attempts = (attemptInfo.attempts || 0) + 1;
      if (attemptInfo.attempts >= 3) {
        attemptInfo.lockedUntil = Date.now() + 60 * 60 * 1000; // 1 hour lockout
        telegramPairingAttempts.set(chatId, attemptInfo);
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `⛔ <b>Maximum Attempts Exceeded (3/3):</b> Pairing access locked for 1 hour. Please verify the code displayed on your physical phone screen.`,
          parse_mode: 'HTML'
        });
      } else {
        telegramPairingAttempts.set(chatId, attemptInfo);
        const remaining = 3 - attemptInfo.attempts;
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `❌ <b>Invalid or Expired Pairing Code:</b> The code <code>${possiblePairCode}</code> was not found or has expired (${attemptInfo.attempts}/3 attempts, ${remaining} remaining). Please check the code on your phone screen.`,
          parse_mode: 'HTML'
        });
      }
      return;
    }
  }


  // Handle /help explicitly (dispatches complete 77-command categorized manual)
  if (lowerText === '/help' || lowerText === 'help') {
    await dispatchCommandToDevice(token, chatId, '/help', []);
    return;
  }

  // Handle /start, /menu or Persistent Keyboard Hub Menu button
  if (
    lowerText === '/start' ||
    lowerText === '/menu' ||
    lowerText === 'menu' ||
    lowerText.includes('hub menu') ||
    lowerText.includes('control panel') ||
    lowerText === 'dashboard'
  ) {
    const activeDev = getActiveDeviceForChat(token, chatId);
    const helpText = buildDashboardText(chatId, activeDev);

    // Pin persistent keyboard first
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: '🛡️ <b>PASA Guardian Console Ready</b>\nQuick action buttons are pinned at the bottom of your screen.',
      parse_mode: 'HTML',
      reply_markup: PERSISTENT_REPLY_KEYBOARD
    });

    // Send interactive inline dashboard
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: helpText,
      parse_mode: 'HTML',
      reply_markup: DASHBOARD_KEYBOARD
    });
    return;
  }

  // Handle Hub Shortcut Words
  if (lowerText.includes('device owner') || lowerText === '👑 device owner') {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:device_owner_hub'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:device_owner_hub'].keyboard
    });
    return;
  }

  if (lowerText.includes('location hub') || lowerText === '📍 location & rf') {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:location_hub'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:location_hub'].keyboard
    });
    return;
  }

  if (lowerText.includes('forensics hub') || lowerText === '📸 covert forensics') {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:forensics_hub'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:forensics_hub'].keyboard
    });
    return;
  }

  if (lowerText.includes('data hub') || lowerText.includes('extraction & logs') || lowerText === '📇 extraction & logs') {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:data_hub'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:data_hub'].keyboard
    });
    return;
  }

  // Natural Language & Persistent Keyboard Mapping
  if (lowerText.includes('status') || lowerText.includes('battery')) {
    await dispatchCommandToDevice(token, chatId, '/status', []);
    return;
  }

  if (lowerText.includes('locate') || lowerText.includes('location') || lowerText.includes('gps') || lowerText === 'where' || lowerText === 'map' || lowerText.includes('tactical map')) {
    await dispatchCommandToDevice(token, chatId, '/locate', []);
    return;
  }

  if (lowerText === '🚨 siren' || lowerText === 'siren' || lowerText === 'alarm' || lowerText === 'ring') {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:ring'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:ring'].keyboard
    });
    return;
  }

  if (lowerText === 'stop siren' || lowerText === 'silence' || lowerText === 'stop alarm') {
    await dispatchCommandToDevice(token, chatId, '/ring_stop', []);
    return;
  }

  if (lowerText.includes('photo') || lowerText.includes('snap') || lowerText.includes('selfie')) {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:snap'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:snap'].keyboard
    });
    return;
  }

  if (
    lowerText === '📱 screen' ||
    lowerText === 'screen' ||
    lowerText === 'screenshot' ||
    lowerText.includes('screenshot') ||
    lowerText.includes('screen burst') ||
    lowerText.includes('screenrecord')
  ) {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:screen'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:screen'].keyboard
    });
    return;
  }

  if (lowerText === '🎥 video' || lowerText === 'video') {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:video'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:video'].keyboard
    });
    return;
  }

  if (lowerText === '🎙️ audio' || lowerText === 'audio' || lowerText === 'mic') {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:record'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:record'].keyboard
    });
    return;
  }

  if (lowerText === '🔒 lock') {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:lockdown_hub'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:lockdown_hub'].keyboard
    });
    return;
  }

  if (lowerText === 'lock') {
    await dispatchCommandToDevice(token, chatId, '/lock', []);
    return;
  }

  if (lowerText === 'unlock') {
    await dispatchCommandToDevice(token, chatId, '/unlock', []);
    return;
  }

  if (lowerText === 'fakeshutdown' || lowerText === 'blackout') {
    await dispatchCommandToDevice(token, chatId, '/fakeshutdown', []);
    return;
  }

  if (lowerText === 'wake') {
    await dispatchCommandToDevice(token, chatId, '/wake', []);
    return;
  }

  if (lowerText === '💬 message' || lowerText === 'message') {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:message'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:message'].keyboard
    });
    return;
  }

  if (lowerText.includes('trap')) {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:traps_hub'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:traps_hub'].keyboard
    });
    return;
  }

  if (lowerText === '🗑️ shred' || lowerText === 'shred' || lowerText === '/shred') {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:shred'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:shred'].keyboard
    });
    return;
  }

  if (lowerText === '🔑 duress' || lowerText === 'duress' || lowerText === '/duress' || lowerText === '/duress_pin') {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: SUBMENUS['menu:duress'].text,
      parse_mode: 'HTML',
      reply_markup: SUBMENUS['menu:duress'].keyboard
    });
    return;
  }

  if (lowerText === '🔑 license' || lowerText === 'license' || lowerText.startsWith('/license') || lowerText.startsWith('/pro')) {
    const parts = rawText.split(/\s+/);
    const subCmd = (parts[1] || '').toLowerCase();
    const activeDev = getActiveDeviceForChat(token, chatId);

    if (subCmd === 'activate') {
      const key = parts[2];
      if (!key) {
        setChatState(chatId, 'WAITING_FOR_LICENSE_KEY');
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: '🔑 <b>Activate PASA Pro License</b>\n━━━━━━━━━━━━━━━━━━━━\nPlease reply with your License Key (e.g. <code>PASA-PRO-XXXX-XXXX-XXXX</code>):',
          parse_mode: 'HTML',
          reply_markup: {
            inline_keyboard: [[{ text: '❌ Cancel', callback_data: 'cancel:wizard' }]]
          }
        });
        return;
      }
      if (!activeDev) {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: '⚠️ Cannot activate license: No active device linked to this chat.',
          parse_mode: 'HTML'
        });
        return;
      }
      const actRes = licensing.activateLicense(key, activeDev.deviceId);
      if (actRes.ok) {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `✅ <b>License Activated Successfully!</b>\n━━━━━━━━━━━━━━━━━━━━\n<b>Tier:</b> ${actRes.tier}\n<b>Device:</b> <code>${activeDev.deviceId}</code>\n<b>Validity:</b> ${actRes.daysLeft > 9000 ? 'Permanent Lifetime' : actRes.daysLeft + ' days'}`,
          parse_mode: 'HTML',
          reply_markup: DASHBOARD_KEYBOARD
        });
      } else {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `❌ <b>Activation Failed:</b> ${actRes.message}`,
          parse_mode: 'HTML'
        });
      }
      return;
    }

    if (subCmd === 'issue' || subCmd === 'create') {
      if (String(chatId) !== String(ADMIN_CHAT_ID)) {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `⛔ <b>Access Denied:</b> Only designated Administrator (<code>${ADMIN_CHAT_ID}</code>) can issue licenses directly.`,
          parse_mode: 'HTML'
        });
        return;
      }
      const targetEmail = parts[2] || 'manual-admin@pasa.sec';
      const targetTier = (parts[3] || 'PRO_LIFETIME').toUpperCase();
      const maxDevs = parseInt(parts[4], 10) || (targetTier === 'PRO_ENTERPRISE' ? 10 : 3);
      const newLic = licensing.createLicense(targetEmail, targetTier, maxDevs, { paymentMethod: 'ADMIN_MANUAL_ISSUE' });
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: `👑 <b>ADMIN LICENSE ISSUED</b>\n━━━━━━━━━━━━━━━━━━━━\n` +
              `<b>Key:</b> <code>${newLic.key}</code>\n` +
              `<b>Recipient:</b> <code>${targetEmail}</code>\n` +
              `<b>Tier:</b> ${newLic.tier}\n` +
              `<b>Max Devices:</b> ${newLic.maxDevices}\n` +
              `<b>Status:</b> ${newLic.status}\n\n` +
              `<i>Share this key with the client or buyer.</i>`,
        parse_mode: 'HTML'
      });
      return;
    }

    if (subCmd === 'pending') {
      if (String(chatId) !== String(ADMIN_CHAT_ID)) {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `⛔ <b>Access Denied:</b> Only designated Administrator (<code>${ADMIN_CHAT_ID}</code>) can view pending orders.`,
          parse_mode: 'HTML'
        });
        return;
      }
      const pendingOrders = Object.values(licenseOrders).filter(o => o.status === 'PENDING_APPROVAL');
      if (pendingOrders.length === 0) {
        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `✅ <b>No Pending Orders:</b> All Binance Pay orders are processed.`,
          parse_mode: 'HTML'
        });
        return;
      }
      let summary = `📋 <b>Pending Binance Pay Orders (${pendingOrders.length})</b>\n━━━━━━━━━━━━━━━━━━━━\n`;
      for (const po of pendingOrders.slice(0, 10)) {
        summary += `• <b>Order:</b> <code>${po.orderId}</code> | ${po.tier} ($${po.amountUsdt})\n  Email: <code>${po.email}</code>\n  TX: <code>${po.binanceTxId || 'None'}</code>\n`;
      }
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: summary,
        parse_mode: 'HTML'
      });
      return;
    }

    if (subCmd === 'buy' || subCmd === 'pricing') {
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: `💎 <b>PASA Sovereign Pro Licensing</b>\n━━━━━━━━━━━━━━━━━━━━\n• <b>Pro Lifetime ($29 USD / ৳3,490):</b> 3 Devices, Lifetime updates & VIP support\n• <b>Fleet Enterprise ($84 USD / ৳9,990):</b> Up to 10 Devices\n\n👉 <b>Instant Web Checkout & Key Delivery:</b>\nhttps://pasa.izhaanintellect.fun/#pricing`,
        parse_mode: 'HTML',
        reply_markup: {
          inline_keyboard: [
            [{ text: '🛒 Open Web Checkout', url: 'https://pasa.izhaanintellect.fun/#pricing' }],
            [{ text: '🔑 Activate Key', callback_data: 'wizard:license:activate' }]
          ]
        }
      });
      return;
    }

    // Default: Show License Status
    let statusDetails = '';
    if (!activeDev) {
      statusDetails = '⚠️ <i>No active device linked to this chat yet.</i>';
    } else {
      const lic = licensing.getDeviceLicenseStatus(activeDev.deviceId);
      const tierBadge = lic.tier === 'PRO_LIFETIME' ? '💎 Pro Lifetime (Sovereign)' :
                        lic.tier === 'PRO_ANNUAL' ? '⭐ Pro Annual' :
                        lic.tier === 'PRO_ENTERPRISE' ? '🏢 Fleet / Enterprise' :
                        lic.tier === 'FREE_TRIAL' ? '⏳ 7-Day Free Trial (Active)' : '❌ Trial Expired';
      const remaining = lic.daysLeft > 9000 ? 'Permanent Sovereign Access' : `${lic.daysLeft} day(s) remaining`;
      statusDetails = `<b>Device:</b> <code>${activeDev.deviceId}</code> (${activeDev.deviceName || 'Android'})\n` +
                      `<b>Tier:</b> ${tierBadge}\n` +
                      `<b>Status:</b> <code>${lic.status}</code>\n` +
                      `<b>Validity:</b> ${remaining}\n` +
                      (lic.licenseKey ? `<b>Bound Key:</b> <code>${lic.licenseKey}</code>\n` : '');
    }

    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: `🔐 <b>PASA Commercial Licensing & Pro Status</b>\n━━━━━━━━━━━━━━━━━━━━\n${statusDetails}\n\n<i>To bind a purchased key, tap <b>Activate License Key</b> below or send:</i>\n<code>/license activate PASA-PRO-XXXX-XXXX</code>`,
      parse_mode: 'HTML',
      reply_markup: {
        inline_keyboard: [
          [
            { text: '🔑 Activate License Key', callback_data: 'wizard:license:activate' },
            { text: '🛒 Buy Pro License', url: 'https://pasa.izhaanintellect.fun/#pricing' }
          ],
          [
            { text: '🔙 Back to Dashboard', callback_data: 'menu:main' }
          ]
        ]
      }
    });
    return;
  }

  // Parse slash commands or standard arguments
  const parts = rawText.split(/\s+/);
  const command = parts[0].toLowerCase();
  const args = parts.slice(1);

  if (command.startsWith('/')) {
    // Handle empty /message command with immediate interactive picker
    if ((command === '/message' || command === '/msg' || command === '/alert_screen') && args.length === 0) {
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: SUBMENUS['menu:message'].text,
        parse_mode: 'HTML',
        reply_markup: SUBMENUS['menu:message'].keyboard
      });
      return;
    }

    if (command === '/check_update' || command === '/update') {
      await handleCheckUpdateCommand(token, chatId, args);
      return;
    }

    if (command === '/update_confirm') {
      await dispatchCommandToDevice(token, chatId, '/update_confirm', args);
      return;
    }

    // Dispatch standard slash command
    await dispatchCommandToDevice(token, chatId, command, args);
    return;
  }

  // If command was written without leading slash (e.g. "locate", "info", "network")
  if (command === 'check_update' || command === 'update') {
    await handleCheckUpdateCommand(token, chatId, args);
    return;
  }

  if (command === 'update_confirm') {
    await dispatchCommandToDevice(token, chatId, '/update_confirm', args);
    return;
  }

  const directCmds = [
    'status', 'locate', 'info', 'network', 'clipboard', 'apps', 'device_owner',
    'wipe', 'history', 'contacts', 'call_log', 'sms_log',
    'smssetup', 'geofence', 'screenshot', 'screen_burst',
    'screenrecord', 'burst', 'screen', 'selftest', 'health', 'diagnostics',
    'lock_message', 'lock_pin', 'set_os_pin', 'reset_pin', 'app_uninstall', 'wipe_confirm', 'track', 'track_stop',
    'ring_stop', 'shred', 'trap', 'duress_pin', 'stealth', 'hide', 'show',
    'livestream', 'stopstream', 'stream', 'reboot', 'restart'
  ];
  if (directCmds.includes(command)) {
    await dispatchCommandToDevice(token, chatId, '/' + command, args);
    return;
  }

  // Friendly Fallback
  await callTelegram(token, 'sendMessage', {
    chat_id: chatId,
    text: `💡 <b>PASA Command Helper</b>\nI didn't recognize "<i>${rawText.substring(0, 50)}</i>".\n\nTap a button below or select from the quick menu at the bottom of your screen:`,
    parse_mode: 'HTML',
    reply_markup: DASHBOARD_KEYBOARD
  });
}

function initPollers() {
  for (const device of Object.values(devices)) {
    if (device.botToken) {
      startBotPoller(device.botToken);
    }
  }
}

// --- REST Endpoints ---

// 0. Root Status & Commercial Landing Page
app.get('/', (req, res) => {
  // If API client explicitly requests JSON, return service status manifest
  if (req.query.format === 'json' || (req.headers.accept && req.headers.accept.includes('application/json'))) {
    return res.json({
      status: 'ok',
      service: 'pasa-server',
      message: 'PASA Sentinel (Private Android Security Agent) Control Plane is Online',
      version: '2.3.0',
      cryptoSigningKeyId: SERVER_KEY_ID,
      uptime: Math.floor(process.uptime()),
      endpoints: [
        '/health',
        '/admin',
        '/api/license/check',
        '/api/license/purchase',
        '/api/license/activate',
        '/api/license/lookup',
        '/api/webhook/payment',
        '/api/admin/licenses',
        '/api/admin/verify',
        '/api/admin/devices',
        '/api/admin/commands',
        '/api/admin/logs',
        '/api/verify-bot',
        '/api/device/register',
        '/api/device/poll',
        '/api/device/response',
        '/api/device/alert',
        '/api/app/latest',
        '/api/app/download/:filename',
        '/api/app/upload',
        '/api/app/releases'
      ]
    });
  }

  // Render high-converting cybersecurity product landing page
  const releases = loadJson(RELEASES_FILE, []);
  const latestRelease = (Array.isArray(releases) && releases.length > 0) ? releases[0] : null;
  const html = renderCommercialLandingPage({
    latestRelease,
    totalDevices: Object.keys(devices).length,
    activePollersCount: activePollers.size
  });
  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.send(html);
});

// 1. Health Check
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'pasa-server',
    version: '2.1.0',
    cryptoSigningKeyId: SERVER_KEY_ID,
    uptime: Math.floor(process.uptime()),
    timestamp: new Date().toISOString(),
    devicesCount: Object.keys(devices).length,
    activePollersCount: activePollers.size
  });
});

// --- Method 2: Instant 6-Digit Pairing Endpoints ---

// POST /api/pair/init - Generate 6-digit OTP pairing code for device
app.post('/api/pair/init', (req, res) => {
  const { deviceId, deviceName } = req.body || {};
  if (!deviceId) {
    return res.status(400).json({ ok: false, message: 'deviceId is required' });
  }

  const now = Date.now();
  // Evict expired pairings
  for (const [c, entry] of activePairings.entries()) {
    if (entry.expiresAt < now) {
      activePairings.delete(c);
    }
  }

  // Generate 6-digit cryptographic random code (100000 - 999999)
  let code = '';
  for (let i = 0; i < 20; i++) {
    const candidate = crypto.randomInt(100000, 999999).toString();
    if (!activePairings.has(candidate)) {
      code = candidate;
      break;
    }
  }

  const expiresAt = now + 3 * 60 * 1000; // 3-minute validity
  activePairings.set(code, {
    deviceId: String(deviceId),
    deviceName: deviceName ? String(deviceName) : 'Android Device',
    createdAt: now,
    expiresAt,
    status: 'PENDING',
    ownerChatId: null
  });

  const formattedCode = `${code.substring(0, 3)} ${code.substring(3)}`;
  console.log(`[Pairing Init] Created pairing code ${formattedCode} for device ${deviceId}`);

  res.json({
    ok: true,
    code,
    formattedCode,
    expiresInSeconds: 180,
    botUsername: 'Pas_agent_bot'
  });
});

// GET /api/pair/status/:code - Poll pairing status
app.get('/api/pair/status/:code', (req, res) => {
  const code = (req.params.code || '').replace(/\s+/g, '');
  const entry = activePairings.get(code);

  if (!entry) {
    return res.json({ ok: false, status: 'NOT_FOUND', message: 'Pairing code not found or expired' });
  }

  if (entry.status === 'CLAIMED') {
    return res.json({
      ok: true,
      status: 'CLAIMED',
      deviceId: entry.deviceId,
      ownerChatId: entry.ownerChatId,
      botToken: DEFAULT_BOT_TOKEN
    });
  }

  if (entry.expiresAt < Date.now()) {
    activePairings.delete(code);
    return res.json({ ok: false, status: 'EXPIRED', message: 'Pairing code has expired' });
  }

  res.json({
    ok: true,
    status: 'PENDING',
    remainingSeconds: Math.max(0, Math.floor((entry.expiresAt - Date.now()) / 1000))
  });
});

// --- Legal Terms of Service & EULA Endpoints ---

app.get('/terms', (req, res) => {
  const possibleHtmlPaths = [
    path.join(__dirname, 'terms.html'),
    path.join(__dirname, '..', 'pasa-commercial-web', 'public', 'terms.html'),
    '/var/www/pasa-server/terms.html',
    '/var/www/pasa-commercial-web/public/terms.html'
  ];
  for (const p of possibleHtmlPaths) {
    if (fs.existsSync(p)) {
      return res.sendFile(path.resolve(p));
    }
  }

  const possiblePaths = [
    path.join(__dirname, '..', 'TERMS.md'),
    path.join(__dirname, 'TERMS.md'),
    '/var/www/pasa-server/TERMS.md'
  ];
  let mdContent = '';
  for (const p of possiblePaths) {
    if (fs.existsSync(p)) {
      mdContent = fs.readFileSync(p, 'utf8');
      break;
    }
  }
  if (!mdContent) {
    mdContent = '# PASA Sentinel — Terms of Service\nPlease refer to the official documentation on GitHub.';
  }

  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.send(`<!DOCTYPE html><html><body style="background:#000;color:#fff;font-family:sans-serif;padding:40px;"><pre>${mdContent}</pre></body></html>`);
});

app.get('/api/terms', (req, res) => {
  const possiblePaths = [
    path.join(__dirname, '..', 'TERMS.md'),
    path.join(__dirname, 'TERMS.md'),
    '/var/www/pasa-server/TERMS.md'
  ];
  let mdContent = '';
  for (const p of possiblePaths) {
    if (fs.existsSync(p)) {
      mdContent = fs.readFileSync(p, 'utf8');
      break;
    }
  }
  res.json({ ok: true, terms: mdContent });
});

// 2. Verify Bot Token cleanly
app.post('/api/verify-bot', async (req, res) => {
  try {
    const { token } = req.body;
    if (!token || typeof token !== 'string') {
      return res.status(400).json({ ok: false, description: 'Missing bot token' });
    }

    const cleanToken = token.trim();
    const tgRes = await callTelegram(cleanToken, 'getMe');

    if (tgRes && tgRes.ok) {
      return res.json({
        ok: true,
        bot: {
          id: tgRes.result.id,
          username: tgRes.result.username,
          firstName: tgRes.result.first_name
        }
      });
    }

    res.status(400).json({
      ok: false,
      description: tgRes.description || 'Invalid Telegram Bot token'
    });
  } catch (err) {
    console.error('Error verifying bot:', err);
    res.status(500).json({ ok: false, description: err.message });
  }
});

// 3. Register Device with Hardware Key Exchange
app.post('/api/device/register', deviceRegisterLimiter, (req, res) => {
  try {
    const { deviceId, deviceName, botToken, ownerChatId, masterPasswordHash, email, publicKeyJwk, attestationChain } = req.body;
    if (!deviceId || !botToken) {
      return res.status(400).json({ ok: false, description: 'deviceId and botToken required' });
    }

    const existingDev = devices[deviceId];
    if (existingDev) {
      const authHeader = (req.headers.authorization || '').replace(/^Bearer\s+/i, '').trim();
      const adminPass = req.headers['x-admin-password'] || (req.body && req.body.adminPassword);
      const isAuthenticated = (authHeader && authHeader === existingDev.apiKey) || (adminPass && adminPass === getAdminSecret());
      if (!isAuthenticated) {
        return res.status(403).json({ ok: false, description: 'Device ID already registered. Valid apiKey or admin auth required to update.' });
      }
    }

    const apiKey = crypto.randomBytes(32).toString('hex');
    const existingDevObj = existingDev || {};
    devices[deviceId] = {
      ...existingDevObj,
      deviceId,
      deviceName: deviceName || existingDevObj.deviceName || 'Android Device',
      botToken: botToken.trim(),
      ownerChatId: ownerChatId || existingDevObj.ownerChatId || '',
      email: email || existingDevObj.email || '',
      apiKey: apiKey,
      publicKeyJwk: publicKeyJwk || existingDevObj.publicKeyJwk || null,
      attestationChain: attestationChain || existingDevObj.attestationChain || [],
      lastSequence: existingDevObj.lastSequence || 0,
      licenseKey: existingDevObj.licenseKey,
      licenseTier: existingDevObj.licenseTier,
      licenseExpiresAt: existingDevObj.licenseExpiresAt,
      registeredAt: existingDevObj.registeredAt || Date.now(),
      lastSeen: Date.now()
    };
    persistDevice(deviceId);

    // Launch Telegram poller for this bot token on VPS
    startBotPoller(botToken.trim());

    logSecurityEvent('DEVICE_REGISTERED', {
      deviceId,
      deviceName: deviceName || 'Android Device',
      ownerChatId: ownerChatId || '',
      hasHardwareKey: !!publicKeyJwk
    });

    res.json({
      ok: true,
      message: 'Device registered successfully with hardware key binding',
      deviceId,
      apiKey,
      signingKeyId: SERVER_KEY_ID,
      commandSigningPublicJwk: JSON.stringify(serverPublicJwk)
    });
  } catch (err) {
    res.status(500).json({ ok: false, description: err.message });
  }
});

// 4. Poll Pending Commands (with Ed25519 envelopes and HTTP long-polling support)
app.get('/api/device/poll', verifyDeviceProofOrBearer, (req, res) => {
  const { deviceId } = req.query;
  const timeoutSec = Math.min(Math.max(parseInt(req.query.timeout, 10) || 0, 0), 30);
  if (!deviceId) return res.status(400).json({ ok: false, description: 'Missing deviceId' });
  if (req.device && req.device.deviceId !== deviceId) {
    return res.status(403).json({ ok: false, description: 'Forbidden: Device ID mismatch' });
  }

  if (devices[deviceId]) {
    devices[deviceId].lastSeen = Date.now();
    persistDevice(deviceId);
  }

  const deviceCommands = commands[deviceId] || [];
  if (deviceCommands.length > 0 || timeoutSec === 0) {
    return res.json({ ok: true, commands: deviceCommands });
  }

  let timer = null;
  let responded = false;

  const onCommand = (newCmds) => {
    if (responded) return;
    responded = true;
    if (timer) clearTimeout(timer);
    if (devices[deviceId]) {
      devices[deviceId].lastSeen = Date.now();
      persistDevice(deviceId);
    }
    res.json({ ok: true, commands: newCmds || [] });
  };

  commandEmitter.once('command:' + deviceId, onCommand);

  timer = setTimeout(() => {
    if (responded) return;
    responded = true;
    commandEmitter.removeListener('command:' + deviceId, onCommand);
    if (devices[deviceId]) {
      devices[deviceId].lastSeen = Date.now();
      persistDevice(deviceId);
    }
    res.json({ ok: true, commands: [] });
  }, timeoutSec * 1000);

  req.on('close', () => {
    if (!responded) {
      responded = true;
      if (timer) clearTimeout(timer);
      commandEmitter.removeListener('command:' + deviceId, onCommand);
    }
  });
});

// 5. Device Response (Forwarding photos, audio, video, GPS to Telegram)
app.post('/api/device/response', verifyDeviceProofOrBearer, upload.fields([
  { name: 'photo', maxCount: 1 },
  { name: 'audio', maxCount: 1 },
  { name: 'video', maxCount: 1 },
  { name: 'evidence', maxCount: 5 }
]), async (req, res) => {
  const allUploadedFiles = [];
  if (req.files) {
    for (const field of Object.values(req.files)) {
      for (const f of field) {
        if (f && f.path) allUploadedFiles.push(f.path);
      }
    }
  }

  try {
    const { deviceId, commandId, message, latitude, longitude } = req.body;
    if (req.device && req.device.deviceId !== deviceId) {
      return res.status(403).json({ ok: false, description: 'Forbidden: Device ID mismatch' });
    }
    const files = req.files || {};

    const device = devices[deviceId];
    if (!device) {
      return res.status(404).json({ ok: false, description: 'Device not registered' });
    }

    const token = device.botToken;
    const chatId = device.ownerChatId;

    // Remove command from queue and SQLite DB
    if (commandId) {
      removeCommand(deviceId, commandId, message || 'COMPLETED');
    }

    // Strategy 1 Zero-Storage: Zero evidence files or media are persisted on server disk.
    // Evidence is routed directly to Telegram with zero server retention.

    logSecurityEvent('DEVICE_RESPONSE', {
      deviceId,
      commandId: commandId || null,
      hasPhoto: !!(files.photo && files.photo.length > 0),
      hasAudio: !!(files.audio && files.audio.length > 0),
      hasVideo: !!(files.video && files.video.length > 0),
      hasLocation: !!(latitude && longitude)
    });

    const hasMedia = !!((files.photo && files.photo.length > 0) ||
                        (files.audio && files.audio.length > 0) ||
                        (files.video && files.video.length > 0));

    // 1. Deliver text message (only if no media, so media caption carries the message cleanly)
    if (message && chatId && !hasMedia) {
      const sendOptions = {
        chat_id: chatId,
        text: message,
        parse_mode: 'HTML'
      };
      if (message.includes('/update_confirm')) {
        sendOptions.reply_markup = {
          inline_keyboard: [
            [{ text: '⚡ Install Update Now', callback_data: 'dev_cmd:update_confirm' }]
          ]
        };
      }
      await callTelegram(token, 'sendMessage', sendOptions);
    }

    // 2. Deliver photo if captured (with quick action buttons)
    // 2. Deliver photo if captured (with quick action buttons)
    if (files.photo && files.photo.length > 0 && chatId) {
      const photoFile = files.photo[0];
      const fileBuffer = photoFile.buffer || (photoFile.path && fs.existsSync(photoFile.path) ? fs.readFileSync(photoFile.path) : null);
      if (fileBuffer) {
        const formData = new FormData();
        formData.append('chat_id', chatId);
        const blob = new Blob([fileBuffer], { type: photoFile.mimetype || 'image/jpeg' });
        formData.append('photo', blob, 'photo.jpg');
        formData.append('caption', message || '📸 Captured photo');

        const photoActionKeyboard = {
          inline_keyboard: [
            [
              { text: '🤳 Snap Front', callback_data: 'cmd:snap:front' },
              { text: '📷 Snap Back', callback_data: 'cmd:snap:back' }
            ],
            [
              { text: '🎥 Video (15s)', callback_data: 'cmd:video:front:15' },
              { text: '🔒 Lock Device', callback_data: 'cmd:lock' }
            ]
          ]
        };
        formData.append('reply_markup', JSON.stringify(photoActionKeyboard));

        await callTelegram(token, 'sendPhoto', null, true, formData);
      }
    }

    // 3. Deliver audio if recorded (with quick action buttons)
    if (files.audio && files.audio.length > 0 && chatId) {
      const audioFile = files.audio[0];
      const fileBuffer = audioFile.buffer || (audioFile.path && fs.existsSync(audioFile.path) ? fs.readFileSync(audioFile.path) : null);
      if (fileBuffer) {
        const formData = new FormData();
        formData.append('chat_id', chatId);
        const blob = new Blob([fileBuffer], { type: audioFile.mimetype || 'audio/m4a' });
        formData.append('audio', blob, 'recording.m4a');
        formData.append('caption', message || '🎙️ Audio recording');

        const audioActionKeyboard = {
          inline_keyboard: [
            [
              { text: '🎙️ Record 30s', callback_data: 'cmd:record:30' },
              { text: '🎙️ Record 60s', callback_data: 'cmd:record:60' }
            ],
            [
              { text: '📍 Instant GPS', callback_data: 'cmd:locate' }
            ]
          ]
        };
        formData.append('reply_markup', JSON.stringify(audioActionKeyboard));

        await callTelegram(token, 'sendAudio', null, true, formData);
      }
    }

    // 4. Deliver video if recorded (with quick action buttons)
    if (files.video && files.video.length > 0 && chatId) {
      const videoFile = files.video[0];
      const fileBuffer = videoFile.buffer || (videoFile.path && fs.existsSync(videoFile.path) ? fs.readFileSync(videoFile.path) : null);
      if (fileBuffer) {
        const formData = new FormData();
        formData.append('chat_id', chatId);
        const blob = new Blob([fileBuffer], { type: videoFile.mimetype || 'video/mp4' });
        formData.append('video', blob, 'video.mp4');
        formData.append('caption', message || '🎥 Captured video');

        const isLiveStream = message && (message.includes('LIVE [Seg') || message.includes('🔴 LIVE'));
        const videoActionKeyboard = isLiveStream ? {
          inline_keyboard: [
            [
              { text: '⏹️ Stop Live Stream', callback_data: 'cmd:stopstream' }
            ]
          ]
        } : {
          inline_keyboard: [
            [
              { text: '🎥 Record Again', callback_data: 'cmd:video:front:15' },
              { text: '🔒 Lock Device', callback_data: 'cmd:lock' }
            ],
            [
              { text: '📍 Instant GPS', callback_data: 'cmd:locate' },
              { text: '🚨 Siren', callback_data: 'cmd:ring:60' }
            ]
          ]
        };
        formData.append('reply_markup', JSON.stringify(videoActionKeyboard));

        await callTelegram(token, 'sendVideo', null, true, formData);
      }
    }

    // Fallback: If device uploaded evidence vault file without media
    if (files.evidence && files.evidence.length > 0 && !hasMedia && chatId) {
      for (const ev of files.evidence) {
        try {
          const formData = new FormData();
          formData.append('chat_id', chatId);
          const fileBuffer = fs.readFileSync(ev.path);
          const blob = new Blob([fileBuffer], { type: ev.mimetype || 'application/octet-stream' });
          formData.append('document', blob, path.basename(ev.path));
          formData.append('caption', message || '🔒 Encrypted Evidence Vault file');
          await callTelegram(token, 'sendDocument', null, true, formData);
        } catch (e) {
          console.error('Failed to relay evidence document to Telegram:', e);
        }
      }
    }

    // 5. Deliver GPS location pin & record history (with tactical action buttons)
    if (latitude && longitude) {
      recordDeviceLocation(deviceId, latitude, longitude, { source: 'response' });
      if (chatId) {
        const locationActionKeyboard = {
          inline_keyboard: [
            [
              { text: '🔄 Refresh GPS', callback_data: 'cmd:locate' },
              { text: '📍 Live Tracking', callback_data: 'menu:track' }
            ],
            [
              { text: '🚨 Sound Siren', callback_data: 'cmd:ring:60' },
              { text: '🔒 Lock Device', callback_data: 'cmd:lock' }
            ]
          ]
        };

        await callTelegram(token, 'sendLocation', {
          chat_id: chatId,
          latitude: parseFloat(latitude),
          longitude: parseFloat(longitude),
          reply_markup: locationActionKeyboard
        });
      }
    }

    res.json({ ok: true, message: 'Response relayed to Telegram' });
  } catch (err) {
    console.error('Error handling device response:', err);
    res.status(500).json({ ok: false, description: err.message });
  } finally {
    for (const fPath of allUploadedFiles) {
      try {
        if (fs.existsSync(fPath)) fs.unlinkSync(fPath);
      } catch (_) {}
    }
  }
});

// 6. Security Alert
app.post('/api/device/alert', verifyDeviceProofOrBearer, upload.fields([
  { name: 'photo', maxCount: 1 }
]), async (req, res) => {
  const allUploadedFiles = [];
  if (req.files) {
    for (const field of Object.values(req.files)) {
      for (const f of field) {
        if (f && f.path) allUploadedFiles.push(f.path);
      }
    }
  }

  try {
    const { deviceId, alertType, message, latitude, longitude } = req.body;
    if (req.device && req.device.deviceId !== deviceId) {
      return res.status(403).json({ ok: false, description: 'Forbidden: Device ID mismatch' });
    }
    const files = req.files || {};

    const device = devices[deviceId];
    if (!device) {
      return res.status(404).json({ ok: false, description: 'Device not registered' });
    }

    const token = device.botToken;
    const chatId = device.ownerChatId;

    if (!chatId) {
      return res.status(400).json({ ok: false, description: 'No ownerChatId configured' });
    }

    logSecurityEvent('SECURITY_ALERT', {
      deviceId,
      alertType: alertType || 'INTRUSION_DETECTED',
      message: (message || '').substring(0, 150),
      latitude: latitude || null,
      longitude: longitude || null
    });

    const alertHeader = `🚨 <b>SECURITY ALERT: ${alertType || 'INTRUSION DETECTED'}</b>\n━━━━━━━━━━━━━━━━━━━━\n`;
    const fullText = alertHeader + (message || '');

    const alertEmergencyKeyboard = {
      inline_keyboard: [
        [
          { text: '🚨 Sound Siren (60s)', callback_data: 'cmd:ring:60' },
          { text: '🔒 Instant Lock', callback_data: 'cmd:lock' }
        ],
        [
          { text: '🤳 Snap Intruder', callback_data: 'cmd:snap:front' },
          { text: '📍 Track Live', callback_data: 'cmd:track:2' }
        ],
        [
          { text: '🕶️ Fake Shutdown', callback_data: 'cmd:fakeshutdown' },
          { text: '💬 Screen Alert', callback_data: 'menu:message' }
        ]
      ]
    };

    // Send Alert Message
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: fullText,
      parse_mode: 'HTML',
      reply_markup: alertEmergencyKeyboard
    });

    // Send Intruder Photo (ephemeral in-memory buffer, zero disk storage)
    if (files.photo && files.photo.length > 0) {
      const photoFile = files.photo[0];
      const fileBuffer = photoFile.buffer || (photoFile.path && fs.existsSync(photoFile.path) ? fs.readFileSync(photoFile.path) : null);
      if (fileBuffer) {
        const formData = new FormData();
        formData.append('chat_id', chatId);
        const blob = new Blob([fileBuffer], { type: photoFile.mimetype || 'image/jpeg' });
        formData.append('photo', blob, 'intruder.jpg');
        formData.append('caption', '🚨 Intruder Capture');
        formData.append('reply_markup', JSON.stringify(alertEmergencyKeyboard));

        await callTelegram(token, 'sendPhoto', null, true, formData);
      }
    }

    // Send Location Pin & record history
    if (latitude && longitude) {
      recordDeviceLocation(deviceId, latitude, longitude, { alertType, source: 'alert' });
      await callTelegram(token, 'sendLocation', {
        chat_id: chatId,
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        reply_markup: alertEmergencyKeyboard
      });
    }

    res.json({ ok: true, message: 'Alert delivered to Telegram' });
  } catch (err) {
    console.error('Error handling alert:', err);
    res.status(500).json({ ok: false, description: err.message });
  } finally {
    for (const fPath of allUploadedFiles) {
      try {
        if (fs.existsSync(fPath)) fs.unlinkSync(fPath);
      } catch (_) {}
    }
  }
});

// --- Phase 5: Web Console Admin Endpoints ---

app.post('/api/admin/verify', adminAuthLimiter, (req, res) => {
  const { key } = req.body || {};
  if (!key || typeof key !== 'string') {
    return res.status(400).json({ ok: false, description: 'Access key is required' });
  }
  const expectedBuf = Buffer.from(ADMIN_SECRET);
  const actualBuf = Buffer.from(key);
  if (expectedBuf.length === actualBuf.length && crypto.timingSafeEqual(expectedBuf, actualBuf)) {
    logSecurityEvent('ADMIN_LOGIN_SUCCESS', {
      ip: req.ip || (req.connection && req.connection.remoteAddress) || 'unknown'
    });
    return res.json({ ok: true, message: 'Admin authentication verified' });
  }
  logSecurityEvent('ADMIN_LOGIN_FAILURE', {
    ip: req.ip || (req.connection && req.connection.remoteAddress) || 'unknown'
  });
  return res.status(401).json({ ok: false, description: 'Invalid admin access key' });
});

app.get('/api/admin/devices', authenticateAdmin, (req, res) => {
  const list = Object.values(devices).map(d => ({
    deviceId: d.deviceId,
    deviceName: d.deviceName,
    ownerChatId: d.ownerChatId,
    lastSeen: d.lastSeen,
    registeredAt: d.registeredAt,
    lastSequence: d.lastSequence || 0,
    hasHardwareKey: !!d.publicKeyJwk,
    attestationCertCount: (d.attestationChain || []).length,
    pendingCommandsCount: (commands[d.deviceId] || []).length
  }));
  res.json({ ok: true, count: list.length, devices: list });
});

app.get('/api/admin/commands', authenticateAdmin, (req, res) => {
  res.json({ ok: true, commands });
});

app.get('/api/admin/logs', authenticateAdmin, (req, res) => {
  const limit = Math.min(parseInt(req.query.limit, 10) || 50, 200);
  const offset = parseInt(req.query.offset, 10) || 0;
  const dbLogs = AuditRepo.getRecent(limit, offset);
  if (dbLogs.length > 0) {
    return res.json({ ok: true, count: dbLogs.length, logs: dbLogs });
  }
  res.json({ ok: true, count: securityLogs.length, logs: securityLogs });
});

app.get('/api/admin/devices/:deviceId/location-history', authenticateAdmin, (req, res) => {
  const { deviceId } = req.params;
  const history = gpsHistory[deviceId] || [];
  res.json({ ok: true, deviceId, count: history.length, history });
});

// --- Phase 6: OTA App Update Endpoints ---

// 6a. Check latest app version (public — called by Android app)
app.get('/api/app/latest', (req, res) => {
  let latest = ReleaseRepo.getLatest();
  if (!latest) {
    const releases = loadJson(RELEASES_FILE, []);
    if (Array.isArray(releases) && releases.length > 0) {
      latest = releases[0];
      ReleaseRepo.add(latest);
    }
  }

  if (!latest) {
    return res.json({
      ok: true,
      update_available: false,
      message: 'No releases published yet'
    });
  }

  const currentVersionCode = parseInt(req.query.current_version_code, 10) || 0;
  res.json({
    ok: true,
    update_available: latest.versionCode > currentVersionCode,
    latest: {
      versionCode: latest.versionCode,
      versionName: latest.versionName,
      downloadUrl: latest.downloadUrl,
      fileSize: latest.fileSize,
      sha256: latest.sha256,
      changelog: latest.changelog || '',
      publishedAt: latest.publishedAt
    }
  });
});

// 6b. Download APK file (public)
app.get(['/api/app/download/:filename', '/releases/:filename'], (req, res) => {
  const filename = path.basename(req.params.filename); // Sanitize
  const filePath = path.join(RELEASES_DIR, filename);
  if (!fs.existsSync(filePath)) {
    return res.status(404).json({ ok: false, description: 'Release file not found' });
  }
  res.setHeader('Content-Type', 'application/vnd.android.package-archive');
  res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
  res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
  const stat = fs.statSync(filePath);
  res.setHeader('Content-Length', stat.size);
  const readStream = fs.createReadStream(filePath);
  readStream.pipe(res);
});

// 6c. Upload new APK release (admin authenticated)
app.post('/api/app/upload', authenticateAdmin, apkUpload.single('apk'), (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ ok: false, description: 'No APK file uploaded' });
    }

    const { version_code, version_name, changelog } = req.body;
    if (!version_code || !version_name) {
      // Cleanup uploaded file
      try { fs.existsSync(req.file.path) && fs.unlinkSync(req.file.path); } catch (_) {}
      return res.status(400).json({ ok: false, description: 'version_code and version_name are required' });
    }

    const versionCode = parseInt(version_code, 10);
    const safeVersionName = version_name.replace(/[^a-zA-Z0-9._-]/g, '_');
    const apkFilename = `pasa-v${safeVersionName}-${versionCode}.apk`;
    const destPath = path.join(RELEASES_DIR, apkFilename);

    // Move uploaded file to releases directory with proper name
    fs.renameSync(req.file.path, destPath);

    // Calculate SHA-256 hash
    const fileBuffer = fs.readFileSync(destPath);
    const sha256 = crypto.createHash('sha256').update(fileBuffer).digest('hex');
    const fileSize = fileBuffer.length;

    const release = {
      versionCode,
      versionName: version_name,
      filename: apkFilename,
      downloadUrl: `api/app/download/${apkFilename}`,
      fileSize,
      sha256,
      changelog: changelog || '',
      publishedAt: new Date().toISOString()
    };

    // Save to SQLite ReleaseRepo
    const oldReleases = ReleaseRepo.add(release);
    for (const old of oldReleases) {
      const oldPath = path.join(RELEASES_DIR, old.filename);
      try { if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath); } catch (_) {}
    }

    // Mirror to JSON for fallback
    let releases = loadJson(RELEASES_FILE, []);
    if (!Array.isArray(releases)) releases = [];
    releases.unshift(release);
    if (releases.length > 5) releases = releases.slice(0, 5);
    saveJson(RELEASES_FILE, releases);

    logSecurityEvent('APP_RELEASE_PUBLISHED', {
      versionCode,
      versionName: version_name,
      sha256,
      fileSize
    });

    console.log(`[OTA] Published PASA v${version_name} (code ${versionCode}), SHA-256: ${sha256}`);

    res.json({
      ok: true,
      message: `APK v${version_name} (${versionCode}) published successfully`,
      release
    });
  } catch (err) {
    // Cleanup on error
    if (req.file && req.file.path) {
      try { fs.existsSync(req.file.path) && fs.unlinkSync(req.file.path); } catch (_) {}
    }
    console.error('[OTA] Upload error:', err);
    res.status(500).json({ ok: false, description: err.message });
  }
});

// 6d. List all published releases (admin authenticated)
app.get('/api/app/releases', authenticateAdmin, (req, res) => {
  let releases = ReleaseRepo.getAll();
  if (!releases || releases.length === 0) {
    releases = loadJson(RELEASES_FILE, []);
  }
  res.json({ ok: true, count: releases.length, releases });
});

// --- Phase 7: Commercial Licensing & Checkout Endpoints ---

// 7a. Purchase / Generate License Key
app.post('/api/license/purchase', licensingGuard, async (req, res) => {
  try {
    const { email, tier, provider, txId, binanceTxId, price } = req.body || {};
    if (!email || typeof email !== 'string' || !email.includes('@')) {
      return res.status(400).json({ ok: false, description: 'A valid email address is required' });
    }

    const cleanTier = (tier || 'PRO_LIFETIME').toUpperCase();
    const effectiveProvider = (provider || (binanceTxId ? 'binance' : 'bkash')).toLowerCase();
    const effectiveTxId = (txId || binanceTxId || '').trim();

    let maxDevices = 3;
    if (cleanTier === 'PRO_ENTERPRISE') maxDevices = 10;
    if (cleanTier === 'COMMUNITY_TRIAL') maxDevices = 1;

    let priceDisplay = '৳3,490';
    if (cleanTier === 'PRO_ENTERPRISE') priceDisplay = '৳9,990';
    if (price) priceDisplay = '৳' + Number(price).toLocaleString('en-BD');

    // Deduplication check: prevent duplicate alerts if user double clicks or retries within 2 minutes
    const normalizedEmail = email.trim().toLowerCase();
    for (const existing of Object.values(licenseOrders)) {
      if (existing.email === normalizedEmail && existing.txId === effectiveTxId && (Date.now() - existing.createdAt < 120000)) {
        return res.json({
          ok: true,
          pending: true,
          orderId: existing.orderId,
          license: {
            key: existing.licenseKey,
            tier: existing.tier,
            maxDevices: maxDevices
          },
          message: 'Existing order retrieved.'
        });
      }
    }

    const orderId = 'ord_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);

    // Create and provision the sovereign license key immediately
    const lic = licensing.createLicense(email.trim().toLowerCase(), cleanTier, maxDevices, {
      paymentMethod: effectiveProvider === 'bkash' ? 'BKASH_SEND_MONEY' : 'BINANCE_PAY',
      txId: effectiveTxId,
      binanceTxId: effectiveTxId,
      binancePayId: BINANCE_PAY_ID,
      price: priceDisplay
    });

    licenseOrders[orderId] = {
      orderId,
      email: email.trim().toLowerCase(),
      tier: cleanTier,
      provider: effectiveProvider,
      price: priceDisplay,
      txId: effectiveTxId,
      status: 'PENDING_VERIFICATION',
      licenseKey: lic.key,
      createdAt: Date.now()
    };
    saveJson(ORDERS_FILE, licenseOrders);

    // Notify registered administrator on Telegram
    try {
      const adminToken = getAdminBotToken();
      if (adminToken && ADMIN_CHAT_ID) {
        const paymentTitle = effectiveProvider === 'bkash' ? '🟣 NEW bKash SEND MONEY ORDER' : '🟡 NEW BINANCE PAY ORDER';
        const paymentTarget = effectiveProvider === 'bkash' ? '01737-910040 (Personal bKash)' : `Binance Pay ID: <code>${BINANCE_PAY_ID}</code> (${BINANCE_NICKNAME})`;

        const adminAlert =
          `💰 <b>${paymentTitle}</b>\n` +
          `━━━━━━━━━━━━━━━━━━━━\n` +
          `<b>Order ID:</b> <code>${orderId}</code>\n` +
          `<b>Plan:</b> ${cleanTier} (${priceDisplay})\n` +
          `<b>Buyer:</b> <code>${email.trim()}</code>\n` +
          `<b>Target Account:</b> ${paymentTarget}\n` +
          `<b>Submitted TX ID:</b> <code>${effectiveTxId || 'None'}</code>\n` +
          `<b>Provisioned Key:</b> <code>${lic.key}</code>\n\n` +
          `<i>👉 Verify in your ${effectiveProvider === 'bkash' ? 'bKash App' : 'Binance Pay'} that this amount was received.</i>`;

        callTelegram(adminToken, 'sendMessage', {
          chat_id: ADMIN_CHAT_ID,
          text: adminAlert,
          parse_mode: 'HTML',
          reply_markup: {
            inline_keyboard: [
              [
                { text: '✅ Verified & Keep Active', callback_data: `lic:approve:${orderId}` },
                { text: '❌ Reject & Revoke Key', callback_data: `lic:reject:${orderId}` }
              ]
            ]
          }
        }).catch(err => console.error('[Order Alert] Telegram notify failed:', err.message));
      } else {
        console.warn('[Order Alert] No botToken or ADMIN_CHAT_ID available to dispatch order alert.');
      }
    } catch (e) {
      console.error('[Order Alert] Error notifying admin:', e.message);
    }

    res.json({
      ok: true,
      pending: true,
      orderId,
      license: {
        key: lic.key,
        tier: lic.tier,
        maxDevices: lic.maxDevices
      },
      message: 'License provisioned successfully.'
    });
  } catch (err) {
    console.error('[License Purchase Error]:', err);
    res.status(500).json({ ok: false, description: err.message });
  }
});

// Check status of pending Binance Pay order (polled by browser)
app.get('/api/license/order-status', (req, res) => {
  const orderId = req.query.orderId;
  if (!orderId) {
    return res.status(400).json({ ok: false, description: 'orderId is required' });
  }
  const order = licenseOrders[orderId];
  if (!order) {
    return res.status(404).json({ ok: false, description: 'Order not found' });
  }
  res.json({
    ok: true,
    status: order.status,
    orderId: order.orderId,
    tier: order.tier,
    email: order.email,
    licenseKey: order.licenseKey
  });
});

// 7a-2. CISO & Enterprise Fleet Inquiry Handler
const CISO_INQUIRIES_FILE = path.join(DATA_DIR, 'ciso_inquiries.json');
let cisoInquiries = loadJson(CISO_INQUIRIES_FILE, []);

app.post('/api/ciso-inquiry', (req, res) => {
  try {
    const { name, email, org, scope, notes } = req.body || {};
    if (!email || !org) {
      return res.status(400).json({ ok: false, description: 'Work Email and Organization are required.' });
    }

    const inquiry = {
      id: 'CISO-' + Date.now().toString(36).toUpperCase(),
      name: (name || '').trim(),
      email: (email || '').trim(),
      org: (org || '').trim(),
      scope: (scope || '10-25').trim(),
      notes: (notes || '').trim(),
      createdAt: new Date().toISOString()
    };

    cisoInquiries.push(inquiry);
    saveJson(CISO_INQUIRIES_FILE, cisoInquiries);

    // Alert Administrator on Telegram
    try {
      const adminToken = getAdminBotToken();
      if (adminToken && ADMIN_CHAT_ID) {
        const adminAlert =
          `🏢 <b>NEW CISO / ENTERPRISE INQUIRY</b>\n` +
          `━━━━━━━━━━━━━━━━━━━━\n` +
          `<b>Inquiry ID:</b> <code>${inquiry.id}</code>\n` +
          `<b>Security Lead:</b> ${inquiry.name || 'Enterprise Officer'}\n` +
          `<b>Work Email:</b> <code>${inquiry.email}</code>\n` +
          `<b>Organization:</b> <b>${inquiry.org}</b>\n` +
          `<b>Fleet Scope:</b> ${inquiry.scope} Endpoints\n` +
          `<b>Requirements:</b>\n` +
          `<i>${inquiry.notes || 'None specified'}</i>\n` +
          `━━━━━━━━━━━━━━━━━━━━\n` +
          `<i>👉 Reply to: <code>${inquiry.email}</code> | Forwarded to izhaanintellect@gmail.com</i>`;

        callTelegram(adminToken, 'sendMessage', {
          chat_id: ADMIN_CHAT_ID,
          text: adminAlert,
          parse_mode: 'HTML'
        }).catch(err => console.error('[CISO Alert] Telegram notify failed:', err.message));
      }
    } catch (e) {
      console.error('[CISO Alert] Error dispatching Telegram alert:', e.message);
    }

    res.json({
      ok: true,
      inquiryId: inquiry.id,
      message: 'CISO inquiry received. A security engineer will connect shortly.'
    });
  } catch (err) {
    console.error('[CISO Inquiry Error]:', err);
    res.status(500).json({ ok: false, description: err.message });
  }
});

// 7b. Activate License on Device
app.post('/api/license/activate', (req, res) => {
  const { key, deviceId } = req.body || {};
  if (!key || !deviceId) {
    return res.status(400).json({ ok: false, description: 'Both key and deviceId are required' });
  }
  const result = licensing.activateLicense(key, deviceId);
  if (!result.ok) {
    return res.status(400).json(result);
  }
  res.json(result);
});

// 7c. Check Device License Status
app.get('/api/license/check', (req, res) => {
  const deviceId = req.query.deviceId;
  if (!deviceId) {
    return res.status(400).json({ ok: false, description: 'deviceId query parameter is required' });
  }
  const status = licensing.getDeviceLicenseStatus(deviceId);
  res.json({ ok: true, deviceId, ...status });
});

// 7d. Lookup License by Key or Email
app.post('/api/license/lookup', licensingGuard, (req, res) => {
  const { query } = req.body || {};
  if (!query) {
    return res.status(400).json({ ok: false, description: 'query (email or key) is required' });
  }
  const found = licensing.lookupLicense(query);
  if (!found) {
    return res.status(404).json({ ok: false, description: 'No active license found matching query' });
  }
  res.json({ ok: true, license: found });
});

// Visitor counter endpoint (starts with 2050)
const VISITORS_FILE = path.join(DATA_DIR, 'visitors.json');
let visitorStats = { count: 2050 };
try {
  if (fs.existsSync(VISITORS_FILE)) {
    visitorStats = JSON.parse(fs.readFileSync(VISITORS_FILE, 'utf8'));
    if (!visitorStats.count || visitorStats.count < 2050) {
      visitorStats.count = 2050;
    }
  } else {
    fs.writeFileSync(VISITORS_FILE, JSON.stringify(visitorStats, null, 2));
  }
} catch (_) {
  visitorStats = { count: 2050 };
}

app.get('/api/stats/visitors', (req, res) => {
  visitorStats.count = (visitorStats.count || 2050) + 1;
  try {
    fs.writeFileSync(VISITORS_FILE, JSON.stringify(visitorStats, null, 2));
  } catch (_) {}
  res.json({ ok: true, count: visitorStats.count });
});

// 7e. Payment Webhook Receiver (Stripe / LemonSqueezy / Paddle / bKash / Crypto)
app.post('/api/webhook/payment', licensingGuard, (req, res) => {
  const secret = process.env.PAYMENT_WEBHOOK_SECRET;
  if (secret) {
    const signature = req.headers['x-webhook-signature'] || req.headers['x-hub-signature-256'] || '';
    const hmac = crypto.createHmac('sha256', secret);
    const digest = 'sha256=' + hmac.update(JSON.stringify(req.body)).digest('hex');
    const sigBuffer = Buffer.from(signature);
    const digestBuffer = Buffer.from(digest);
    if (sigBuffer.length !== digestBuffer.length || !crypto.timingSafeEqual(sigBuffer, digestBuffer)) {
      return res.status(401).json({ ok: false, description: 'Invalid webhook signature' });
    }
  } else if (process.env.NODE_ENV === 'production') {
    return res.status(500).json({ ok: false, description: 'Webhook secret not configured on production' });
  }

  const payload = req.body || {};
  console.log('[Payment Webhook] Event received:', JSON.stringify(payload).substring(0, 150));
  const email = payload.email || payload.customer_email || (payload.data && payload.data.object && payload.data.object.customer_email) || 'customer@pasa.sec';
  const tier = payload.tier || payload.plan || 'PRO_ANNUAL';
  const license = licensing.createLicense(email, tier);
  logSecurityEvent('PAYMENT_WEBHOOK_FULFILLED', { email, tier, key: license.key });
  res.json({ ok: true, received: true, message: 'License provisioned successfully.' });
});

// 7f. Admin License Management
app.get('/api/admin/licenses', authenticateAdmin, (req, res) => {
  const list = licensing.listLicenses();
  res.json({ ok: true, count: list.length, licenses: list });
});

// Serve Web Console Dashboard
app.get('/admin', (req, res) => {
  res.send(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>PASA Sentinel — Fleet Control Plane</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600&family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    :root {
      --bg: #070a12;
      --card-bg: rgba(16, 24, 40, 0.75);
      --card-border: rgba(30, 58, 110, 0.45);
      --primary: #00d4ff;
      --primary-glow: rgba(0, 212, 255, 0.25);
      --emerald: #10b981;
      --emerald-glow: rgba(16, 185, 129, 0.2);
      --amber: #f59e0b;
      --rose: #f43f5e;
      --text: #f1f5f9;
      --text-muted: #94a3b8;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background: var(--bg);
      background-image: 
        radial-gradient(at 0% 0%, rgba(0, 212, 255, 0.08) 0px, transparent 50%),
        radial-gradient(at 100% 100%, rgba(16, 185, 129, 0.06) 0px, transparent 50%);
      color: var(--text);
      font-family: 'Plus Jakarta Sans', sans-serif;
      min-height: 100vh;
      padding: 24px;
    }
    .container { max-width: 1200px; margin: 0 auto; }
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 24px;
      border-bottom: 1px solid var(--card-border);
      margin-bottom: 28px;
    }
    .brand { display: flex; align-items: center; gap: 14px; }
    .brand-logo {
      width: 44px; height: 44px;
      background: linear-gradient(135deg, #00d4ff, #0066cc);
      border-radius: 12px;
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 20px; color: #070a12;
      box-shadow: 0 0 20px var(--primary-glow);
    }
    .brand-text h1 { font-size: 20px; font-weight: 700; letter-spacing: -0.5px; }
    .brand-text p { font-size: 12px; color: var(--primary); font-family: 'JetBrains Mono', monospace; }
    .header-actions { display: flex; align-items: center; gap: 12px; }
    .badge {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 6px 12px; border-radius: 9999px;
      font-size: 12px; font-weight: 600; font-family: 'JetBrains Mono', monospace;
    }
    .badge-live { background: rgba(16, 185, 129, 0.15); color: var(--emerald); border: 1px solid rgba(16, 185, 129, 0.3); }
    .btn-lock {
      background: rgba(244, 63, 94, 0.15);
      color: var(--rose);
      border: 1px solid rgba(244, 63, 94, 0.3);
      padding: 6px 14px;
      border-radius: 8px;
      cursor: pointer;
      font-size: 12px;
      font-family: 'JetBrains Mono', monospace;
      font-weight: 600;
      transition: all 0.2s;
    }
    .btn-lock:hover {
      background: rgba(244, 63, 94, 0.25);
    }
    .pulse-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--emerald); animation: pulse 2s infinite; }
    @keyframes pulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(0.85); } }
    
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 16px;
      margin-bottom: 32px;
    }
    .stat-card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 20px;
      backdrop-filter: blur(12px);
    }
    .stat-label { font-size: 12px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; }
    .stat-value { font-size: 24px; font-weight: 700; color: var(--text); }
    .stat-sub { font-size: 12px; color: var(--primary); margin-top: 6px; font-family: 'JetBrains Mono', monospace; }
    
    .section-title { font-size: 18px; font-weight: 700; margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; }
    .card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 24px;
      backdrop-filter: blur(12px);
      margin-bottom: 32px;
    }
    table { width: 100%; border-collapse: collapse; text-align: left; }
    th {
      font-size: 11px; text-transform: uppercase; color: var(--text-muted);
      letter-spacing: 0.5px; padding: 12px 16px;
      border-bottom: 1px solid var(--card-border);
    }
    td { padding: 16px; border-bottom: 1px solid rgba(255, 255, 255, 0.04); font-size: 14px; }
    tr:last-child td { border-bottom: none; }
    .device-name { font-weight: 600; color: var(--text); margin-bottom: 4px; }
    .device-id { font-family: 'JetBrains Mono', monospace; font-size: 12px; color: var(--text-muted); }
    .key-badge {
      display: inline-flex; align-items: center; gap: 5px;
      padding: 4px 10px; border-radius: 6px;
      font-size: 11px; font-family: 'JetBrains Mono', monospace;
    }
    .key-strongbox { background: rgba(0, 212, 255, 0.15); color: #00d4ff; border: 1px solid rgba(0, 212, 255, 0.3); }
    .key-pending { background: rgba(245, 158, 11, 0.15); color: #f59e0b; border: 1px solid rgba(245, 158, 11, 0.3); }
    .status-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 6px; }
    .status-online { background: var(--emerald); }
    .status-idle { background: var(--text-muted); }
    .empty-state { text-align: center; padding: 36px 20px; color: var(--text-muted); }

    /* Event log styles */
    .evt-badge {
      display: inline-block; padding: 3px 8px; border-radius: 4px; font-size: 11px; font-family: 'JetBrains Mono', monospace; font-weight: 600;
    }
    .evt-alert { background: rgba(244, 63, 94, 0.2); color: var(--rose); border: 1px solid rgba(244, 63, 94, 0.4); }
    .evt-cmd { background: rgba(0, 212, 255, 0.2); color: var(--primary); border: 1px solid rgba(0, 212, 255, 0.4); }
    .evt-reg { background: rgba(16, 185, 129, 0.2); color: var(--emerald); border: 1px solid rgba(16, 185, 129, 0.4); }
    .evt-auth { background: rgba(245, 158, 11, 0.2); color: var(--amber); border: 1px solid rgba(245, 158, 11, 0.4); }

    /* Auth Gate Modal */
    #auth-overlay {
      position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
      background: rgba(7, 10, 18, 0.95);
      backdrop-filter: blur(16px);
      display: flex; align-items: center; justify-content: center;
      z-index: 9999;
    }
    .auth-box {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 20px;
      padding: 36px;
      max-width: 440px;
      width: 90%;
      text-align: center;
      box-shadow: 0 0 50px rgba(0, 212, 255, 0.15);
    }
    .auth-input {
      width: 100%;
      padding: 14px 16px;
      background: rgba(0, 0, 0, 0.4);
      border: 1px solid var(--card-border);
      border-radius: 10px;
      color: #fff;
      font-family: 'JetBrains Mono', monospace;
      font-size: 14px;
      margin: 18px 0 12px 0;
      outline: none;
    }
    .auth-input:focus { border-color: var(--primary); box-shadow: 0 0 10px var(--primary-glow); }
    .auth-btn {
      width: 100%;
      padding: 14px;
      background: linear-gradient(135deg, #00d4ff, #0066cc);
      color: #070a12;
      border: none;
      border-radius: 10px;
      font-weight: 700;
      font-size: 14px;
      cursor: pointer;
      transition: transform 0.1s, opacity 0.2s;
    }
    .auth-btn:hover { opacity: 0.95; transform: translateY(-1px); }
    .auth-error {
      color: var(--rose);
      font-size: 13px;
      margin-top: 10px;
      min-height: 20px;
    }
  </style>
</head>
<body>

  <!-- Auth Gate Modal -->
  <div id="auth-overlay" style="display: none;">
    <div class="auth-box">
      <div class="brand-logo" style="margin: 0 auto 16px auto;">🛡️</div>
      <h2 style="font-size: 20px; margin-bottom: 8px;">PASA Fleet Control Plane</h2>
      <p style="font-size: 13px; color: var(--text-muted);">Enter Master Admin Access Key to authenticate</p>
      <input type="password" id="admin-key-input" class="auth-input" placeholder="Enter Admin Key..." autocomplete="off" />
      <button class="auth-btn" id="btn-login">Unlock Control Plane</button>
      <div id="login-error" class="auth-error"></div>
    </div>
  </div>

  <div class="container" id="main-dashboard" style="display: none;">
    <header>
      <div class="brand">
        <div class="brand-logo">🛡️</div>
        <div class="brand-text">
          <h1>PASA Sentinel Control Plane</h1>
          <p>ED25519 HARDENED FLEET MANAGEMENT</p>
        </div>
      </div>
      <div class="header-actions">
        <div class="badge badge-live">
          <div class="pulse-dot"></div>
          <span>CONTROL PLANE v2.1 ACTIVE</span>
        </div>
        <button class="btn-lock" id="btn-logout" title="Lock Console">🔒 Lock Console</button>
      </div>
    </header>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">Protected Devices</div>
        <div class="stat-value" id="device-count">0</div>
        <div class="stat-sub">Hardware Enrolled</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Signing Algorithm</div>
        <div class="stat-value">Ed25519</div>
        <div class="stat-sub">Key: pasa-server-1</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Device Proof Scheme</div>
        <div class="stat-value">ES256 / P-256</div>
        <div class="stat-sub">StrongBox / TEE Bound</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Telegram Poller</div>
        <div class="stat-value" id="poller-count">Active</div>
        <div class="stat-sub">Webhook Gateway</div>
      </div>
    </div>

    <!-- Tactical Mission Control & Real-time GPS Map -->
    <div class="card" style="padding: 0; overflow: hidden; position: relative; margin-bottom: 32px;">
      <div style="padding: 16px 20px; border-bottom: 1px solid var(--card-border); display: flex; justify-content: space-between; align-items: center;">
        <div style="font-weight: 700; font-size: 16px; display: flex; align-items: center; gap: 8px;">
          <span>🛰️ Tactical Mission Control & GPS Fleet Map</span>
        </div>
        <div style="display: flex; gap: 10px; align-items: center;">
          <span class="badge badge-live" id="map-status-text">Live Radar</span>
        </div>
      </div>
      <div id="mission-map" style="height: 420px; width: 100%; background: #070a12;"></div>
      <div style="padding: 12px 20px; background: rgba(10, 16, 30, 0.85); border-top: 1px solid var(--card-border); display: flex; justify-content: space-between; align-items: center; font-size: 12px; font-family: 'JetBrains Mono', monospace; flex-wrap: wrap; gap: 8px;">
        <span id="map-info-text" style="color: var(--text-muted);">Tracking real-time coordinates, GPS drift circles, and historical breadcrumb trails.</span>
        <div style="display: flex; gap: 8px;">
          <button id="btn-fit-map" style="background: rgba(0,212,255,0.15); color: var(--primary); border: 1px solid rgba(0,212,255,0.3); padding: 4px 12px; border-radius: 6px; cursor: pointer; font-size: 11px; font-family: 'JetBrains Mono', monospace; font-weight: 600;">🎯 Center Fleet</button>
        </div>
      </div>
    </div>

    <!-- Device Fleet Status -->
    <div class="card">
      <div class="section-title">
        <span>Device Fleet Status</span>
        <span style="font-size: 12px; color: var(--text-muted); font-family: 'JetBrains Mono', monospace;" id="refresh-time">Auto-refreshing</span>
      </div>
      <div style="overflow-x: auto;">
        <table>
          <thead>
            <tr>
              <th>Device / Identifier</th>
              <th>Owner Chat ID</th>
              <th>Hardware Key Protection</th>
              <th>Sequence #</th>
              <th>Last Seen</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody id="devices-table-body">
            <tr><td colspan="6" class="empty-state">Loading protected devices...</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Cryptographic Command Queue -->
    <div class="card">
      <div class="section-title">
        <span>Cryptographic Command Queue</span>
      </div>
      <div id="commands-container">
        <div class="empty-state">No pending queued commands</div>
      </div>
    </div>

    <!-- Security Audit Event Log -->
    <div class="card">
      <div class="section-title">
        <span>Security Audit Event Log</span>
        <span style="font-size: 12px; color: var(--text-muted); font-family: 'JetBrains Mono', monospace;" id="logs-count">0 Events</span>
      </div>
      <div style="overflow-x: auto;">
        <table>
          <thead>
            <tr>
              <th>Event Type</th>
              <th>Details / Message</th>
              <th>Device ID</th>
              <th>Timestamp</th>
            </tr>
          </thead>
          <tbody id="logs-table-body">
            <tr><td colspan="4" class="empty-state">Loading security events...</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <script>
    const STORAGE_KEY = 'pasa_admin_access_key';
    const authOverlay = document.getElementById('auth-overlay');
    const mainDashboard = document.getElementById('main-dashboard');
    const adminKeyInput = document.getElementById('admin-key-input');
    const loginError = document.getElementById('login-error');
    const btnLogin = document.getElementById('btn-login');
    const btnLogout = document.getElementById('btn-logout');

    function getAdminKey() {
      // Check query parameter ?key=... or sessionStorage
      const params = new URLSearchParams(window.location.search);
      const urlKey = params.get('key');
      if (urlKey) {
        sessionStorage.setItem(STORAGE_KEY, urlKey);
        // Clean key from visible URL address bar
        window.history.replaceState({}, document.title, window.location.pathname);
        return urlKey;
      }
      return sessionStorage.getItem(STORAGE_KEY) || '';
    }

    function timeAgo(epochMs) {
      if (!epochMs) return 'Never';
      const sec = Math.floor((Date.now() - epochMs) / 1000);
      if (sec < 60) return sec + 's ago';
      if (sec < 3600) return Math.floor(sec / 60) + 'm ago';
      return Math.floor(sec / 3600) + 'h ago';
    }

    function showLoginModal(msg = '') {
      authOverlay.style.display = 'flex';
      mainDashboard.style.display = 'none';
      if (msg) loginError.textContent = msg;
      adminKeyInput.value = '';
      adminKeyInput.focus();
    }

    let missionMap = null;
    let deviceMarkers = {};
    let breadcrumbLayers = {};
    let allCoordinates = [];

    function initMissionMap() {
      if (missionMap) return;
      const mapEl = document.getElementById('mission-map');
      if (!mapEl || typeof L === 'undefined') return;

      try {
        missionMap = L.map('mission-map').setView([23.8103, 90.4125], 13);
        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
          attribution: '&copy; CARTO &copy; OpenStreetMap',
          subdomains: 'abcd',
          maxZoom: 19
        }).addTo(missionMap);

        const btnFit = document.getElementById('btn-fit-map');
        if (btnFit) {
          btnFit.addEventListener('click', () => {
            if (allCoordinates.length > 0 && missionMap) {
              missionMap.fitBounds(allCoordinates, { padding: [50, 50] });
            }
          });
        }
      } catch (e) {
        console.warn('Leaflet initialization deferred:', e);
      }
    }

    function showDashboard() {
      authOverlay.style.display = 'none';
      mainDashboard.style.display = 'block';
      setTimeout(initMissionMap, 150);
    }

    async function verifyAndLogin(key) {
      if (!key) {
        loginError.textContent = 'Please enter your Admin Access Key';
        return;
      }
      loginError.textContent = 'Verifying key...';
      try {
        const res = await fetch('./api/admin/verify', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ key })
        });
        const data = await res.json();
        if (data.ok) {
          sessionStorage.setItem(STORAGE_KEY, key);
          showDashboard();
          loadData();
        } else {
          loginError.textContent = data.description || 'Invalid admin key';
        }
      } catch (e) {
        loginError.textContent = 'Network error connecting to server';
      }
    }

    btnLogin.addEventListener('click', () => {
      verifyAndLogin(adminKeyInput.value.trim());
    });

    adminKeyInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') verifyAndLogin(adminKeyInput.value.trim());
    });

    btnLogout.addEventListener('click', () => {
      sessionStorage.removeItem(STORAGE_KEY);
      showLoginModal('Console locked.');
    });

    async function loadData() {
      const key = getAdminKey();
      if (!key) {
        showLoginModal();
        return;
      }

      try {
        const headers = { 'x-pasa-admin-key': key };

        // 1. Fetch Devices
        const resDev = await fetch('./api/admin/devices', { headers });
        if (resDev.status === 401) {
          sessionStorage.removeItem(STORAGE_KEY);
          showLoginModal('Session expired or unauthorized');
          return;
        }
        const dataDev = await resDev.json();

        if (dataDev.ok) {
          document.getElementById('device-count').textContent = dataDev.count || 0;
          const tbody = document.getElementById('devices-table-body');
          if (dataDev.devices.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No devices registered yet</td></tr>';
          } else {
            tbody.innerHTML = dataDev.devices.map(d => {
              const isRecent = d.lastSeen && (Date.now() - d.lastSeen < 180000);
              const keyHtml = d.hasHardwareKey 
                ? '<span class="key-badge key-strongbox">🔒 StrongBox / TEE (' + d.attestationCertCount + ' certs)</span>'
                : '<span class="key-badge key-pending">⏳ Pending Key Exchange</span>';
              
              return '<tr>' +
                '<td><div class="device-name">' + (d.deviceName || 'Android Device') + '</div><div class="device-id">' + d.deviceId + '</div></td>' +
                '<td><code style="font-family: monospace; color: var(--primary);">' + (d.ownerChatId || '—') + '</code></td>' +
                '<td>' + keyHtml + '</td>' +
                '<td><code style="font-family: monospace;">#' + (d.lastSequence || 0) + '</code></td>' +
                '<td>' + timeAgo(d.lastSeen) + '</td>' +
                '<td>' + (isRecent ? '<span style="color: var(--emerald);"><span class="status-dot status-online"></span>Online</span>' : '<span style="color: var(--text-muted);"><span class="status-dot status-idle"></span>Idle</span>') + '</td>' +
              '</tr>';
            }).join('');
          }

          // Plot location history on mission map
          if (missionMap && Array.isArray(dataDev.devices)) {
            allCoordinates = [];
            for (const d of dataDev.devices) {
              try {
                const locRes = await fetch('./api/admin/devices/' + encodeURIComponent(d.deviceId) + '/location-history', { headers });
                const locData = await locRes.json();
                if (locData.ok && Array.isArray(locData.history) && locData.history.length > 0) {
                  const latest = locData.history[0];
                  const latLng = [latest.lat, latest.lon];
                  allCoordinates.push(latLng);

                  if (deviceMarkers[d.deviceId]) {
                    deviceMarkers[d.deviceId].setLatLng(latLng);
                  } else {
                    deviceMarkers[d.deviceId] = L.circleMarker(latLng, {
                      radius: 9,
                      color: '#00d4ff',
                      fillColor: '#00d4ff',
                      fillOpacity: 0.9,
                      weight: 2
                    }).addTo(missionMap);
                  }
                  deviceMarkers[d.deviceId].bindPopup('<b>' + (d.deviceName || 'PASA Device') + '</b><br>Lat: ' + latest.lat.toFixed(5) + ', Lon: ' + latest.lon.toFixed(5) + '<br>Fix: ' + timeAgo(latest.timestamp));

                  const pathCoords = locData.history.map(pt => [pt.lat, pt.lon]);
                  if (breadcrumbLayers[d.deviceId]) {
                    breadcrumbLayers[d.deviceId].setLatLngs(pathCoords);
                  } else {
                    breadcrumbLayers[d.deviceId] = L.polyline(pathCoords, {
                      color: '#00d4ff',
                      weight: 3,
                      opacity: 0.55,
                      dashArray: '5, 8'
                    }).addTo(missionMap);
                  }
                }
              } catch (_) {}
            }
            if (allCoordinates.length > 0) {
              document.getElementById('map-status-text').textContent = allCoordinates.length + ' Device Fix(es) Plotted';
            }
          }
        }

        // 2. Fetch Commands
        const resCmd = await fetch('./api/admin/commands', { headers });
        const dataCmd = await resCmd.json();
        if (dataCmd.ok) {
          const cmdContainer = document.getElementById('commands-container');
          let allCmds = [];
          for (const [devId, cmds] of Object.entries(dataCmd.commands || {})) {
            if (Array.isArray(cmds) && cmds.length > 0) {
              allCmds.push(...cmds.map(c => ({ ...c, devId })));
            }
          }
          if (allCmds.length === 0) {
            cmdContainer.innerHTML = '<div class="empty-state">No pending queued commands across fleet</div>';
          } else {
            cmdContainer.innerHTML = '<table><thead><tr><th>Command ID</th><th>Target Device</th><th>Command</th><th>Queued Time</th></tr></thead><tbody>' +
              allCmds.map(c => (
                '<tr>' +
                  '<td><code style="font-family: monospace; color: var(--primary);">' + c.id + '</code></td>' +
                  '<td><code style="font-family: monospace;">' + c.devId + '</code></td>' +
                  '<td><b>' + c.command + '</b> ' + (c.args || []).join(' ') + '</td>' +
                  '<td>' + timeAgo(c.createdAt) + '</td>' +
                '</tr>'
              )).join('') + '</tbody></table>';
          }
        }

        // 3. Fetch Audit Logs
        const resLogs = await fetch('./api/admin/logs', { headers });
        const dataLogs = await resLogs.json();
        if (dataLogs.ok && Array.isArray(dataLogs.logs)) {
          document.getElementById('logs-count').textContent = dataLogs.logs.length + ' Events';
          const tbodyLogs = document.getElementById('logs-table-body');
          if (dataLogs.logs.length === 0) {
            tbodyLogs.innerHTML = '<tr><td colspan="4" class="empty-state">No audit logs recorded yet</td></tr>';
          } else {
            tbodyLogs.innerHTML = dataLogs.logs.slice(0, 30).map(l => {
              let badgeClass = 'evt-cmd';
              if (l.type.includes('ALERT')) badgeClass = 'evt-alert';
              else if (l.type.includes('REGISTER')) badgeClass = 'evt-reg';
              else if (l.type.includes('AUTH') || l.type.includes('ADMIN')) badgeClass = 'evt-auth';

              let detail = l.command || l.message || l.alertType || l.deviceName || JSON.stringify(l);
              if (typeof detail === 'object') detail = JSON.stringify(detail);

              return '<tr>' +
                '<td><span class="evt-badge ' + badgeClass + '">' + l.type + '</span></td>' +
                '<td>' + detail + '</td>' +
                '<td><code style="font-family: monospace; font-size: 12px;">' + (l.deviceId || '—') + '</code></td>' +
                '<td style="font-size: 12px; color: var(--text-muted);">' + timeAgo(l.timestamp) + '</td>' +
              '</tr>';
            }).join('');
          }
        }

        document.getElementById('refresh-time').textContent = 'Last synced: ' + new Date().toLocaleTimeString();
      } catch (e) {
        console.error('Error fetching admin telemetry:', e);
      }
    }

    // Init check
    const currentKey = getAdminKey();
    if (!currentKey) {
      showLoginModal();
    } else {
      showDashboard();
      loadData();
      setInterval(loadData, 4000);
    }
  </script>
</body>
</html>`);
});

// Start Server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`[PASA Control Plane] Server v2.1 listening on port ${PORT}`);
  initPollers();
  if (DEFAULT_BOT_TOKEN) {
    console.log(`[PASA Control Plane] Auto-starting poller for default bot token...`);
    startBotPoller(DEFAULT_BOT_TOKEN);
  }
});
