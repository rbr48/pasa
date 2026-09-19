const express = require('express');
const cors = require('cors');
const multer = require('multer');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const EventEmitter = require('events');

const app = express();
const PORT = process.env.PORT || 8160;
const commandEmitter = new EventEmitter();
commandEmitter.setMaxListeners(100);

// Default bot token (optional fallback via environment variable only - never hardcoded in source)
const DEFAULT_BOT_TOKEN = process.env.BOT_TOKEN || '';

// Setup directories
const DATA_DIR = path.join(__dirname, 'data');
const UPLOADS_DIR = path.join(__dirname, 'uploads');
if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });
if (!fs.existsSync(UPLOADS_DIR)) fs.mkdirSync(UPLOADS_DIR, { recursive: true });

const DEVICES_FILE = path.join(DATA_DIR, 'devices.json');
const COMMANDS_FILE = path.join(DATA_DIR, 'commands.json');
const SIGNING_KEY_FILE = path.join(DATA_DIR, 'server_signing_key.json');
const ADMIN_SECRET_FILE = path.join(DATA_DIR, 'admin_secret.json');
const LOGS_FILE = path.join(DATA_DIR, 'security_logs.json');
const SERVER_KEY_ID = 'pasa-server-1';
const RELEASES_DIR = path.join(__dirname, 'releases');
if (!fs.existsSync(RELEASES_DIR)) fs.mkdirSync(RELEASES_DIR, { recursive: true });
const RELEASES_FILE = path.join(DATA_DIR, 'app_releases.json');

// Multer storage for photos/audio/video uploaded from device
const upload = multer({
  dest: UPLOADS_DIR,
  limits: { fileSize: 100 * 1024 * 1024 } // 100MB max (for videos)
});

const apkUpload = multer({
  dest: RELEASES_DIR,
  limits: { fileSize: 200 * 1024 * 1024 }, // 200MB max for APKs
  fileFilter: (req, file, cb) => {
    cb(null, true); // Accept all, validate after
  }
});

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// --- Persistent Storage Helpers ---

function loadJson(file, defaultVal = {}) {
  try {
    if (fs.existsSync(file)) {
      return JSON.parse(fs.readFileSync(file, 'utf8'));
    }
  } catch (err) {
    console.error(`Error reading ${file}:`, err);
  }
  return defaultVal;
}

function saveJson(file, data) {
  try {
    const tmpFile = `${file}.${Date.now()}.${Math.random().toString(36).substring(2, 7)}.tmp`;
    fs.writeFileSync(tmpFile, JSON.stringify(data, null, 2), 'utf8');
    fs.renameSync(tmpFile, file);
  } catch (err) {
    console.error(`Error writing ${file}:`, err);
  }
}

let devices = loadJson(DEVICES_FILE, {});
let commands = loadJson(COMMANDS_FILE, {}); // deviceId -> [ { id, command, args, chatId, createdAt, envelope } ]

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

// --- Security Audit Event Logs (Circular Buffer capped at 200 events) ---

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
  saveJson(DEVICES_FILE, devices);

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

              // If device enrolled a public key, verify the ES256 signature
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
                // Public key not yet recorded, accept valid proof structure
                req.device = device;
                req.deviceAuthMode = 'proof_structure_accepted';
                return next();
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
      return await res.json();
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
    { command: "status", description: "Live battery, storage, and sensors telemetry" },
    { command: "locate", description: "Acquire GPS coordinates & Google Maps pin" },
    { command: "video", description: "Record stealth video (1-60s front/back)" },
    { command: "snap", description: "Capture stealth photo (front or rear)" },
    { command: "record", description: "Record ambient audio clip" },
    { command: "track", description: "Start continuous live GPS tracking" },
    { command: "track_stop", description: "Stop continuous GPS tracking" },
    { command: "lock", description: "Lock screen with custom PIN & emergency message" },
    { command: "unlock", description: "Dismiss Lost Mode & unlock device screen" },
    { command: "device_owner", description: "Check Enterprise Device Owner & Kiosk status" },
    { command: "fakeshutdown", description: "Fake shutdown: blackout screen & silent surveillance" },
    { command: "wake", description: "Restore device from Fake Shutdown blackout" },
    { command: "ring", description: "Trigger max volume emergency siren" },
    { command: "ring_stop", description: "Silence active emergency siren" },
    { command: "message", description: "Display urgent alert banner on device screen" },
    { command: "clipboard", description: "Read current device clipboard text" },
    { command: "network", description: "Current IP, Wi-Fi SSID, cell carrier info" },
    { command: "info", description: "Hardware specs, SIM details, and OS version" },
    { command: "apps", description: "List installed applications" },
    { command: "stealth", description: "Toggle app icon in launcher" },
    { command: "wipe", description: "Remote factory reset (requires master password)" },
    { command: "help", description: "Show full help manual & command list" },
    { command: "check_update", description: "Check for OTA app updates" }
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

// Interactive Dashboard & Submenu Keyboards
const DASHBOARD_KEYBOARD = {
  inline_keyboard: [
    [
      { text: '📊 Live Status', callback_data: 'cmd:status' },
      { text: '📍 Instant GPS', callback_data: 'cmd:locate' }
    ],
    [
      { text: '📸 Snap Photo', callback_data: 'menu:snap' },
      { text: '🎥 Record Video', callback_data: 'menu:video' }
    ],
    [
      { text: '🎙️ Record Audio', callback_data: 'menu:record' },
      { text: '🔊 Alarm Siren', callback_data: 'menu:ring' }
    ],
    [
      { text: '🔒 Lock Device', callback_data: 'menu:lock' },
      { text: '💬 Screen Message', callback_data: 'menu:message' }
    ],
    [
      { text: '🌐 Network Info', callback_data: 'cmd:network' },
      { text: '📋 Clipboard', callback_data: 'cmd:clipboard' }
    ],
    [
      { text: '📦 Installed Apps', callback_data: 'cmd:apps' },
      { text: '📍 Live Tracking', callback_data: 'menu:track' }
    ],
    [
      { text: '🔄 Check Update', callback_data: 'cmd:check_update' },
      { text: '⚠️ Wipe Device', callback_data: 'menu:wipe' }
    ]
  ]
};

const SUBMENUS = {
  'menu:message': {
    text: '💬 <b>Display Screen Alert Message</b>\n━━━━━━━━━━━━━━━━━━━━\nBroadcast an urgent lost-mode alert or emergency contact message over the phone\'s lockscreen:\n\n<b>Command Format:</b>\n<code>/message &lt;your message text&gt;</code>\n\n<b>Examples:</b>\n• <code>/message Please return this lost phone! Call +123456789. Reward offered.</code>\n• <code>/message Contact owner at 01700000000 immediately.</code>\n\n<i>The device screen will turn ON, play an alert chime, and display your message with a direct 1-tap call button.</i>',
    keyboard: {
      inline_keyboard: [
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
    text: '🎥 <b>Stealth Video Recording</b>\nChoose duration and camera to record covertly:',
    keyboard: {
      inline_keyboard: [
        [
          { text: '🤳 Front (15s)', callback_data: 'cmd:video:front:15' },
          { text: '🤳 Front (30s)', callback_data: 'cmd:video:front:30' }
        ],
        [
          { text: '📷 Rear (15s)', callback_data: 'cmd:video:back:15' },
          { text: '📷 Rear (30s)', callback_data: 'cmd:video:back:30' }
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
    text: '🔊 <b>Emergency Alarm Siren</b>\nTrigger max volume siren (overrides silent/vibrate mode):',
    keyboard: {
      inline_keyboard: [
        [
          { text: '🚨 Sound Siren (60s)', callback_data: 'cmd:ring:60' },
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
    text: '🔒 <b>Device Defense & Lockout Modes</b>\n━━━━━━━━━━━━━━━━━━━━\n<b>Strategy A: Custom PIN Lost Mode Guard</b>\n• <code>/lock &lt;pin&gt; &lt;message&gt;</code> — Immediate lock with custom emergency PIN keypad. 3 failed attempts take tamper selfie & send GPS.\n• <code>/unlock</code> — Dismisses Lost Mode & restores normal device.\n\n<b>Strategy B: Enterprise Device Owner Kiosk</b>\n• <code>/device_owner</code> — Check if app has Device Owner privileges (hardware kiosk lock task mode, freezes navigation buttons, blocks uninstall).\n\n<b>Strategy C: Fake Shutdown / Blackout Deception</b>\n• <code>/fakeshutdown</code> — Simulates Android power-off, turns screen black, mutes audio, streams GPS & covert front photos on touch.\n• <code>/wake</code> — Restores normal screen & sound.\n\n<i>Choose an action below:</i>',
    keyboard: {
      inline_keyboard: [
        [
          { text: '🔒 Lock Device Now', callback_data: 'cmd:lock' },
          { text: '🔓 Remote Unlock', callback_data: 'cmd:unlock' }
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
  }
};

function getActiveDeviceForChat(token, chatId) {
  const matching = Object.values(devices)
    .filter(d => d.botToken === token && (d.ownerChatId == chatId || !d.ownerChatId))
    .sort((a, b) => (b.lastSeen || 0) - (a.lastSeen || 0));
  return matching[0] || null;
}

function buildDashboardText(chatId, activeDev) {
  const deviceLine = activeDev
    ? `Active Device: <b>${activeDev.deviceName || 'Android'}</b> (<code>${activeDev.deviceId}</code>)\nLast Check-in: ${Math.floor((Date.now() - (activeDev.lastSeen || 0)) / 1000)}s ago`
    : '⚠️ No device registered yet. Complete setup on your Android phone.';

  return `
🛡️ <b>PASA (Private Android Security Agent)</b>
<i>Control Plane: Ed25519 Hardened Gateway v2.0</i>
━━━━━━━━━━━━━━━━━━━━
<b>Your Chat ID:</b> <code>${chatId}</code>
${deviceLine}
━━━━━━━━━━━━━━━━━━━━
Tap a button below to dispatch cryptographically signed commands or access forensic tools instantly:
`.trim();
}

async function dispatchCommandToDevice(token, chatId, command, args = []) {
  const matchingDeviceIds = Object.keys(devices)
    .filter(id => {
      const d = devices[id];
      return d.botToken === token && (d.ownerChatId == chatId || !d.ownerChatId);
    })
    .sort((a, b) => (devices[b].lastSeen || 0) - (devices[a].lastSeen || 0));

  if (matchingDeviceIds.length === 0) {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: '⚠️ No PASA device registered yet. Please complete setup on your phone.',
      parse_mode: 'HTML'
    });
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

    commands[devId].push({
      id: cmdId,
      command,
      args,
      chatId,
      createdAt: Date.now(),
      envelope: signedEnvelope
    });

    // Notify any active HTTP long-poll connection for this device
    commandEmitter.emit('command:' + devId, commands[devId]);
  }
  saveJson(COMMANDS_FILE, commands);

  logSecurityEvent('COMMAND_DISPATCHED', {
    command: formattedCmd,
    deviceId: targetDeviceId,
    chatId: chatId ? String(chatId) : 'unknown'
  });

  const activeDevice = devices[targetDeviceId];
  const lastSeenSec = Math.floor((Date.now() - (activeDevice.lastSeen || 0)) / 1000);
  const statusNote = lastSeenSec < 60 ? `Online (${lastSeenSec}s ago)` : `Last active ${lastSeenSec}s ago`;

  await callTelegram(token, 'sendMessage', {
    chat_id: chatId,
    text: `⏳ Command <code>${formattedCmd}</code> signed & dispatched to <b>${activeDevice.deviceName || targetDeviceId}</b> (${statusNote}).\n\nAwaiting telemetry...`,
    parse_mode: 'HTML'
  });

  // Safety watchdog: after 90 seconds, if command is still pending in queue, inform Telegram and purge
  setTimeout(async () => {
    try {
      let wasPending = false;
      for (const devId of targetDeviceIds) {
        if (commands[devId] && commands[devId].some(c => c.id === cmdId)) {
          commands[devId] = commands[devId].filter(c => c.id !== cmdId);
          wasPending = true;
        }
      }
      if (wasPending) {
        saveJson(COMMANDS_FILE, commands);
        const currentDev = devices[targetDeviceId] || {};
        const secAgo = Math.floor((Date.now() - (currentDev.lastSeen || 0)) / 1000);
        console.warn(`[Command Watchdog] Command ${cmdId} (${command}) timed out after 90s for ${targetDeviceId}`);

        await callTelegram(token, 'sendMessage', {
          chat_id: chatId,
          text: `⚠️ <b>Command Timeout:</b> <code>${formattedCmd}</code> did not receive telemetry within 90s.\n\n` +
                `📱 <b>Device:</b> ${currentDev.deviceName || targetDeviceId}\n` +
                `⏱️ <b>Last Check-in:</b> ${secAgo}s ago\n\n` +
                `<i>Note: If the phone is locked, ensure Battery Optimization is set to "Unrestricted" in device App Info.</i>`,
          parse_mode: 'HTML'
        });
      }
    } catch (watchdogErr) {
      console.error('[Command Watchdog] Error in timeout handler:', watchdogErr.message);
    }
  }, 90000);
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

    // Immediately acknowledge callback query so button stops loading
    await callTelegram(token, 'answerCallbackQuery', { callback_query_id: query.id });

    if (data === 'menu:main') {
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

    if (SUBMENUS[data]) {
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
  const text = update.message.text.trim();
  const parts = text.split(/\s+/);
  const command = parts[0].toLowerCase();
  const args = parts.slice(1);

  console.log(`[Telegram Command] Received: ${command} from chatId: ${chatId}`);

  // Handle /start, /help, /menu — Display Interactive Control Dashboard
  if (command === '/start' || command === '/help' || command === '/menu') {
    const activeDev = getActiveDeviceForChat(token, chatId);
    const helpText = buildDashboardText(chatId, activeDev);

    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: helpText,
      parse_mode: 'HTML',
      reply_markup: DASHBOARD_KEYBOARD
    });
    return;
  }

  // Handle empty /message command with immediate helpful guidance
  if ((command === '/message' || command === '/msg' || command === '/alert_screen') && args.length === 0) {
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: `⚠️ <b>Please specify the message to display on the phone screen.</b>\n` +
            `━━━━━━━━━━━━━━━━━━━━\n` +
            `<b>Usage:</b> <code>/message &lt;your message text&gt;</code>\n\n` +
            `<b>Examples:</b>\n` +
            `• <code>/message Please return this lost phone! Call +123456789. Reward offered.</code>\n` +
            `• <code>/message Contact owner at 01700000000 immediately.</code>\n\n` +
            `<i>The phone screen will turn ON, play an alert chime, and display your message over the lockscreen with a direct 1-tap call button.</i>`,
      parse_mode: 'HTML'
    });
    return;
  }

  // Dispatch standard text command
  await dispatchCommandToDevice(token, chatId, command, args);
}

function initPollers() {
  for (const device of Object.values(devices)) {
    if (device.botToken) {
      startBotPoller(device.botToken);
    }
  }
}

// --- REST Endpoints ---

// 0. Root Status
app.get('/', (req, res) => {
  res.json({
    status: 'ok',
    service: 'pasa-server',
    message: 'PASA (Private Android Security Agent) Control Plane is Online',
    version: '2.1.0',
    cryptoSigningKeyId: SERVER_KEY_ID,
    uptime: Math.floor(process.uptime()),
    endpoints: [
      '/health',
      '/admin',
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
app.post('/api/device/register', (req, res) => {
  try {
    const { deviceId, deviceName, botToken, ownerChatId, masterPasswordHash, email, publicKeyJwk, attestationChain } = req.body;
    if (!deviceId || !botToken) {
      return res.status(400).json({ ok: false, description: 'deviceId and botToken required' });
    }

    const apiKey = crypto.randomBytes(32).toString('hex');

    devices[deviceId] = {
      deviceId,
      deviceName: deviceName || 'Android Device',
      botToken: botToken.trim(),
      ownerChatId: ownerChatId || '',
      email: email || '',
      apiKey: apiKey,
      publicKeyJwk: publicKeyJwk || null,
      attestationChain: attestationChain || [],
      lastSequence: devices[deviceId]?.lastSequence || 0,
      registeredAt: Date.now(),
      lastSeen: Date.now()
    };
    saveJson(DEVICES_FILE, devices);

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

  if (devices[deviceId]) {
    devices[deviceId].lastSeen = Date.now();
    saveJson(DEVICES_FILE, devices);
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
      saveJson(DEVICES_FILE, devices);
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
      saveJson(DEVICES_FILE, devices);
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
  { name: 'video', maxCount: 1 }
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
    const files = req.files || {};

    const device = devices[deviceId];
    if (!device) {
      return res.status(404).json({ ok: false, description: 'Device not registered' });
    }

    const token = device.botToken;
    const chatId = device.ownerChatId;

    // Remove command from queue
    if (commands[deviceId] && commandId) {
      commands[deviceId] = commands[deviceId].filter(c => c.id !== commandId);
      saveJson(COMMANDS_FILE, commands);
    }

    logSecurityEvent('DEVICE_RESPONSE', {
      deviceId,
      commandId: commandId || null,
      hasPhoto: !!(files.photo && files.photo.length > 0),
      hasAudio: !!(files.audio && files.audio.length > 0),
      hasVideo: !!(files.video && files.video.length > 0),
      hasLocation: !!(latitude && longitude)
    });

    // 1. Deliver text message
    if (message && chatId) {
      await callTelegram(token, 'sendMessage', {
        chat_id: chatId,
        text: message,
        parse_mode: 'HTML'
      });
    }

    // 2. Deliver photo if captured
    if (files.photo && files.photo.length > 0 && chatId) {
      const photoFile = files.photo[0];
      const formData = new FormData();
      formData.append('chat_id', chatId);
      const fileBuffer = fs.readFileSync(photoFile.path);
      const blob = new Blob([fileBuffer], { type: photoFile.mimetype || 'image/jpeg' });
      formData.append('photo', blob, 'photo.jpg');
      formData.append('caption', '📸 Captured photo');

      await callTelegram(token, 'sendPhoto', null, true, formData);
    }

    // 3. Deliver audio if recorded
    if (files.audio && files.audio.length > 0 && chatId) {
      const audioFile = files.audio[0];
      const formData = new FormData();
      formData.append('chat_id', chatId);
      const fileBuffer = fs.readFileSync(audioFile.path);
      const blob = new Blob([fileBuffer], { type: audioFile.mimetype || 'audio/m4a' });
      formData.append('audio', blob, 'recording.m4a');
      formData.append('caption', '🎙️ Audio recording');

      await callTelegram(token, 'sendAudio', null, true, formData);
    }

    // 4. Deliver video if recorded
    if (files.video && files.video.length > 0 && chatId) {
      const videoFile = files.video[0];
      const formData = new FormData();
      formData.append('chat_id', chatId);
      const fileBuffer = fs.readFileSync(videoFile.path);
      const blob = new Blob([fileBuffer], { type: videoFile.mimetype || 'video/mp4' });
      formData.append('video', blob, 'video.mp4');
      formData.append('caption', '🎥 Captured video');

      await callTelegram(token, 'sendVideo', null, true, formData);
    }

    // 5. Deliver GPS location pin
    if (latitude && longitude && chatId) {
      await callTelegram(token, 'sendLocation', {
        chat_id: chatId,
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude)
      });
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

    // Send Alert Message
    await callTelegram(token, 'sendMessage', {
      chat_id: chatId,
      text: fullText,
      parse_mode: 'HTML'
    });

    // Send Intruder Photo
    if (files.photo && files.photo.length > 0) {
      const photoFile = files.photo[0];
      const formData = new FormData();
      formData.append('chat_id', chatId);
      const fileBuffer = fs.readFileSync(photoFile.path);
      const blob = new Blob([fileBuffer], { type: 'image/jpeg' });
      formData.append('photo', blob, 'intruder.jpg');
      formData.append('caption', '🚨 Intruder Capture');

      await callTelegram(token, 'sendPhoto', null, true, formData);
    }

    // Send Location Pin
    if (latitude && longitude) {
      await callTelegram(token, 'sendLocation', {
        chat_id: chatId,
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude)
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

app.post('/api/admin/verify', (req, res) => {
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
  res.json({ ok: true, count: securityLogs.length, logs: securityLogs });
});

// --- Phase 6: OTA App Update Endpoints ---

// 6a. Check latest app version (public — called by Android app)
app.get('/api/app/latest', (req, res) => {
  const releases = loadJson(RELEASES_FILE, []);
  if (!Array.isArray(releases) || releases.length === 0) {
    return res.json({
      ok: true,
      update_available: false,
      message: 'No releases published yet'
    });
  }
  const latest = releases[0]; // Sorted newest first
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
app.get('/api/app/download/:filename', (req, res) => {
  const filename = path.basename(req.params.filename); // Sanitize
  const filePath = path.join(RELEASES_DIR, filename);
  if (!fs.existsSync(filePath)) {
    return res.status(404).json({ ok: false, description: 'Release file not found' });
  }
  res.setHeader('Content-Type', 'application/vnd.android.package-archive');
  res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
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

    // Load existing releases, prepend new one, keep max 5
    let releases = loadJson(RELEASES_FILE, []);
    if (!Array.isArray(releases)) releases = [];
    releases.unshift(release);
    if (releases.length > 5) {
      // Delete old APK files beyond 5 releases
      for (const old of releases.slice(5)) {
        const oldPath = path.join(RELEASES_DIR, old.filename);
        try { if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath); } catch (_) {}
      }
      releases = releases.slice(0, 5);
    }
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
  const releases = loadJson(RELEASES_FILE, []);
  res.json({ ok: true, count: releases.length, releases });
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

    function showDashboard() {
      authOverlay.style.display = 'none';
      mainDashboard.style.display = 'block';
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
