'use strict';

const path = require('path');
const fs = require('fs');
const { DatabaseSync } = require('node:sqlite');

/**
 * PASA 3.0 SQLite Control Plane Database (WAL Mode).
 * Provides ACID persistence, zero file locks, and high concurrency.
 */

let db = null;

function initDatabase(dbPath) {
  if (db) return db;

  const dbDir = path.dirname(dbPath);
  if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
  }

  db = new DatabaseSync(dbPath);

  // Performance and concurrency settings
  db.exec('PRAGMA journal_mode = WAL;');
  db.exec('PRAGMA synchronous = NORMAL;');
  db.exec('PRAGMA busy_timeout = 5000;');

  // Schema creation
  db.exec(`
    CREATE TABLE IF NOT EXISTS devices (
      deviceId TEXT PRIMARY KEY,
      botToken TEXT,
      ownerChatId TEXT,
      deviceName TEXT,
      model TEXT,
      osVersion TEXT,
      battery INTEGER,
      batteryStatus TEXT,
      isArmed INTEGER DEFAULT 1,
      apiKey TEXT,
      publicKeyJwk TEXT,
      attestationChain TEXT,
      lastSequence INTEGER DEFAULT 0,
      lastLocation TEXT,
      publicKey TEXT,
      lastSeen INTEGER,
      lastIp TEXT,
      createdAt INTEGER
    );

    CREATE TABLE IF NOT EXISTS commands (
      id TEXT PRIMARY KEY,
      deviceId TEXT NOT NULL,
      command TEXT NOT NULL,
      args TEXT,
      chatId TEXT,
      status TEXT DEFAULT 'PENDING',
      createdAt INTEGER NOT NULL,
      deliveredAt INTEGER,
      envelope TEXT,
      response TEXT,
      completedAt INTEGER
    );
    CREATE INDEX IF NOT EXISTS idx_commands_dev_status ON commands (deviceId, status);

    CREATE TABLE IF NOT EXISTS app_releases (
      versionCode INTEGER PRIMARY KEY,
      versionName TEXT NOT NULL,
      filename TEXT NOT NULL,
      downloadUrl TEXT NOT NULL,
      fileSize INTEGER NOT NULL,
      sha256 TEXT NOT NULL,
      changelog TEXT,
      publishedAt TEXT
    );

    CREATE TABLE IF NOT EXISTS evidence (
      id TEXT PRIMARY KEY,
      commandId TEXT,
      deviceId TEXT,
      type TEXT,
      filename TEXT NOT NULL,
      fileSize INTEGER,
      sha256 TEXT,
      isEncrypted INTEGER DEFAULT 0,
      mimeType TEXT,
      createdAt INTEGER NOT NULL,
      expiresAt INTEGER NOT NULL
    );
    CREATE INDEX IF NOT EXISTS idx_evidence_expires ON evidence (expiresAt);

    CREATE TABLE IF NOT EXISTS audit_logs (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      eventType TEXT NOT NULL,
      deviceId TEXT,
      chatId TEXT,
      details TEXT,
      ip TEXT,
      timestamp INTEGER NOT NULL
    );
    CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs (timestamp DESC);

    CREATE TABLE IF NOT EXISTS licenses (
      key TEXT PRIMARY KEY,
      tier TEXT NOT NULL,
      status TEXT DEFAULT 'ACTIVE',
      maxDevices INTEGER DEFAULT 1,
      customerEmail TEXT,
      orderId TEXT,
      createdAt INTEGER NOT NULL,
      expiresAt INTEGER,
      revoked INTEGER DEFAULT 0,
      revokeReason TEXT
    );

    CREATE TABLE IF NOT EXISTS device_licenses (
      deviceHash TEXT PRIMARY KEY,
      licenseKey TEXT NOT NULL,
      activatedAt INTEGER NOT NULL
    );
    `);

  // Safe schema migrations for existing databases
  const existingCols = (tableName) => {
    try {
      return db.prepare(`PRAGMA table_info(${tableName})`).all().map(r => r.name);
    } catch(e) { return []; }
  };
  if (!existingCols('licenses').includes('revoked')) {
    db.exec('ALTER TABLE licenses ADD COLUMN revoked INTEGER DEFAULT 0;');
    db.exec('ALTER TABLE licenses ADD COLUMN revokeReason TEXT;');
  }

  return db;
}

/**
 * Migration helper: Imports legacy JSON data into SQLite on initial startup.
 */
function migrateFromJson(dataDir) {
  if (!db) throw new Error('Database not initialized');

  // 1. Migrate devices.json
  const devicesFile = path.join(dataDir, 'devices.json');
  if (fs.existsSync(devicesFile)) {
    const rowCount = db.prepare('SELECT count(*) as count FROM devices').get().count;
    if (rowCount === 0) {
      try {
        const raw = JSON.parse(fs.readFileSync(devicesFile, 'utf8'));
        const insertDevice = db.prepare(`
          INSERT OR REPLACE INTO devices (
            deviceId, botToken, ownerChatId, deviceName, model, osVersion,
            battery, batteryStatus, isArmed, apiKey, publicKeyJwk, attestationChain,
            lastSequence, lastLocation, publicKey, lastSeen, lastIp, createdAt
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `);
        for (const [id, d] of Object.entries(raw)) {
          insertDevice.run(
            id,
            d.botToken || '',
            d.ownerChatId ? String(d.ownerChatId) : '',
            d.deviceName || 'Android',
            d.model || '',
            d.osVersion || '',
            d.battery || null,
            d.batteryStatus || '',
            d.isArmed !== undefined ? (d.isArmed ? 1 : 0) : 1,
            d.apiKey || '',
            d.publicKeyJwk ? (typeof d.publicKeyJwk === 'string' ? d.publicKeyJwk : JSON.stringify(d.publicKeyJwk)) : null,
            d.attestationChain ? (typeof d.attestationChain === 'string' ? d.attestationChain : JSON.stringify(d.attestationChain)) : null,
            d.lastSequence || 0,
            d.lastLocation ? (typeof d.lastLocation === 'string' ? d.lastLocation : JSON.stringify(d.lastLocation)) : null,
            d.publicKey || '',
            d.lastSeen || Date.now(),
            d.lastIp || '',
            d.registeredAt || d.createdAt || Date.now()
          );
        }
        console.log(`[Database Migration] Migrated ${Object.keys(raw).length} devices from devices.json`);
      } catch (err) {
        console.error('[Database Migration] Error migrating devices:', err.message);
      }
    }
  }

  // 2. Migrate app_releases.json
  const releasesFile = path.join(dataDir, 'app_releases.json');
  if (fs.existsSync(releasesFile)) {
    const rowCount = db.prepare('SELECT count(*) as count FROM app_releases').get().count;
    if (rowCount === 0) {
      try {
        const raw = JSON.parse(fs.readFileSync(releasesFile, 'utf8'));
        if (Array.isArray(raw)) {
          const insertRelease = db.prepare(`
            INSERT OR REPLACE INTO app_releases (versionCode, versionName, filename, downloadUrl, fileSize, sha256, changelog, publishedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
          `);
          for (const r of raw) {
            insertRelease.run(
              r.versionCode,
              r.versionName,
              r.filename,
              r.downloadUrl,
              r.fileSize,
              r.sha256,
              r.changelog || '',
              r.publishedAt || new Date().toISOString()
            );
          }
          console.log(`[Database Migration] Migrated ${raw.length} releases from app_releases.json`);
        }
      } catch (err) {
        console.error('[Database Migration] Error migrating app_releases:', err.message);
      }
    }
  }

  // 3. Migrate commands.json
  const commandsFile = path.join(dataDir, 'commands.json');
  if (fs.existsSync(commandsFile)) {
    const rowCount = db.prepare('SELECT count(*) as count FROM commands').get().count;
    if (rowCount === 0) {
      try {
        const raw = JSON.parse(fs.readFileSync(commandsFile, 'utf8'));
        const insertCmd = db.prepare(`
          INSERT OR REPLACE INTO commands (id, deviceId, command, args, chatId, status, createdAt, deliveredAt, envelope)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        `);
        let count = 0;
        for (const [devId, cmdList] of Object.entries(raw)) {
          if (Array.isArray(cmdList)) {
            for (const c of cmdList) {
              insertCmd.run(
                c.id,
                devId,
                c.command,
                JSON.stringify(c.args || []),
                c.chatId ? String(c.chatId) : '',
                'PENDING',
                c.createdAt || Date.now(),
                null,
                c.envelope ? JSON.stringify(c.envelope) : null
              );
              count++;
            }
          }
        }
        console.log(`[Database Migration] Migrated ${count} pending commands from commands.json`);
      } catch (err) {
        console.error('[Database Migration] Error migrating commands:', err.message);
      }
    }
  }
}

// --- Data Access Repository ---

function parseJsonSafe(val, fallback = null) {
  if (!val) return fallback;
  if (typeof val !== 'string') return val;
  try {
    return JSON.parse(val);
  } catch (_) {
    return fallback;
  }
}

const DeviceRepo = {
  getAll() {
    const rows = db.prepare('SELECT * FROM devices').all();
    const map = {};
    for (const r of rows) {
      map[r.deviceId] = {
        deviceId: r.deviceId,
        botToken: r.botToken,
        ownerChatId: r.ownerChatId,
        deviceName: r.deviceName,
        model: r.model,
        osVersion: r.osVersion,
        battery: r.battery,
        batteryStatus: r.batteryStatus,
        isArmed: Boolean(r.isArmed),
        apiKey: r.apiKey || '',
        publicKeyJwk: parseJsonSafe(r.publicKeyJwk, null),
        attestationChain: parseJsonSafe(r.attestationChain, []),
        lastSequence: r.lastSequence || 0,
        lastLocation: parseJsonSafe(r.lastLocation, null),
        publicKey: r.publicKey,
        lastSeen: r.lastSeen,
        lastIp: r.lastIp,
        registeredAt: r.createdAt,
        createdAt: r.createdAt
      };
    }
    return map;
  },

  get(deviceId) {
    const r = db.prepare('SELECT * FROM devices WHERE deviceId = ?').get(deviceId);
    if (!r) return null;
    return {
      deviceId: r.deviceId,
      botToken: r.botToken,
      ownerChatId: r.ownerChatId,
      deviceName: r.deviceName,
      model: r.model,
      osVersion: r.osVersion,
      battery: r.battery,
      batteryStatus: r.batteryStatus,
      isArmed: Boolean(r.isArmed),
      apiKey: r.apiKey || '',
      publicKeyJwk: parseJsonSafe(r.publicKeyJwk, null),
      attestationChain: parseJsonSafe(r.attestationChain, []),
      lastSequence: r.lastSequence || 0,
      lastLocation: parseJsonSafe(r.lastLocation, null),
      publicKey: r.publicKey,
      lastSeen: r.lastSeen,
      lastIp: r.lastIp,
      registeredAt: r.createdAt,
      createdAt: r.createdAt
    };
  },

  upsert(d) {
    db.prepare(`
      INSERT INTO devices (
        deviceId, botToken, ownerChatId, deviceName, model, osVersion,
        battery, batteryStatus, isArmed, apiKey, publicKeyJwk, attestationChain,
        lastSequence, lastLocation, publicKey, lastSeen, lastIp, createdAt
      ) VALUES (
        @deviceId, @botToken, @ownerChatId, @deviceName, @model, @osVersion,
        @battery, @batteryStatus, @isArmed, @apiKey, @publicKeyJwk, @attestationChain,
        @lastSequence, @lastLocation, @publicKey, @lastSeen, @lastIp, @createdAt
      )
      ON CONFLICT(deviceId) DO UPDATE SET
        botToken = coalesce(@botToken, botToken),
        ownerChatId = coalesce(@ownerChatId, ownerChatId),
        deviceName = coalesce(@deviceName, deviceName),
        model = coalesce(@model, model),
        osVersion = coalesce(@osVersion, osVersion),
        battery = coalesce(@battery, battery),
        batteryStatus = coalesce(@batteryStatus, batteryStatus),
        isArmed = coalesce(@isArmed, isArmed),
        apiKey = coalesce(@apiKey, apiKey),
        publicKeyJwk = coalesce(@publicKeyJwk, publicKeyJwk),
        attestationChain = coalesce(@attestationChain, attestationChain),
        lastSequence = coalesce(@lastSequence, lastSequence),
        lastLocation = coalesce(@lastLocation, lastLocation),
        publicKey = coalesce(@publicKey, publicKey),
        lastSeen = coalesce(@lastSeen, lastSeen),
        lastIp = coalesce(@lastIp, lastIp)
    `).run({
      deviceId: d.deviceId,
      botToken: d.botToken || '',
      ownerChatId: d.ownerChatId ? String(d.ownerChatId) : '',
      deviceName: d.deviceName || 'Android',
      model: d.model || '',
      osVersion: d.osVersion || '',
      battery: d.battery !== undefined ? d.battery : null,
      batteryStatus: d.batteryStatus || '',
      isArmed: d.isArmed !== undefined ? (d.isArmed ? 1 : 0) : 1,
      apiKey: d.apiKey || '',
      publicKeyJwk: d.publicKeyJwk ? (typeof d.publicKeyJwk === 'string' ? d.publicKeyJwk : JSON.stringify(d.publicKeyJwk)) : null,
      attestationChain: d.attestationChain ? (typeof d.attestationChain === 'string' ? d.attestationChain : JSON.stringify(d.attestationChain)) : null,
      lastSequence: d.lastSequence || 0,
      lastLocation: d.lastLocation ? (typeof d.lastLocation === 'string' ? d.lastLocation : JSON.stringify(d.lastLocation)) : null,
      publicKey: d.publicKey || '',
      lastSeen: d.lastSeen || Date.now(),
      lastIp: d.lastIp || '',
      createdAt: d.registeredAt || d.createdAt || Date.now()
    });
  }
};

const CommandRepo = {
  getPendingForDevice(deviceId) {
    const rows = db.prepare(`
      SELECT * FROM commands 
      WHERE deviceId = ? AND status = 'PENDING'
      ORDER BY createdAt ASC
    `).all(deviceId);

    return rows.map(r => ({
      id: r.id,
      command: r.command,
      args: r.args ? JSON.parse(r.args) : [],
      chatId: r.chatId,
      createdAt: r.createdAt,
      envelope: r.envelope ? JSON.parse(r.envelope) : null
    }));
  },

  getAllPending() {
    const rows = db.prepare(`SELECT * FROM commands WHERE status = 'PENDING' ORDER BY createdAt ASC`).all();
    const map = {};
    for (const r of rows) {
      if (!map[r.deviceId]) map[r.deviceId] = [];
      map[r.deviceId].push({
        id: r.id,
        command: r.command,
        args: r.args ? JSON.parse(r.args) : [],
        chatId: r.chatId,
        createdAt: r.createdAt,
        envelope: r.envelope ? JSON.parse(r.envelope) : null
      });
    }
    return map;
  },

  add(cmd) {
    db.prepare(`
      INSERT OR REPLACE INTO commands (id, deviceId, command, args, chatId, status, createdAt, envelope)
      VALUES (?, ?, ?, ?, ?, 'PENDING', ?, ?)
    `).run(
      cmd.id,
      cmd.deviceId,
      cmd.command,
      JSON.stringify(cmd.args || []),
      cmd.chatId ? String(cmd.chatId) : '',
      cmd.createdAt || Date.now(),
      cmd.envelope ? JSON.stringify(cmd.envelope) : null
    );
  },

  markDelivered(commandId) {
    db.prepare(`UPDATE commands SET status = 'DELIVERED', deliveredAt = ? WHERE id = ?`).run(Date.now(), commandId);
  },

  complete(commandId, response = '') {
    db.prepare(`UPDATE commands SET status = 'COMPLETED', response = ?, completedAt = ? WHERE id = ?`).run(
      response,
      Date.now(),
      commandId
    );
  },

  clearForDevice(deviceId) {
    db.prepare(`DELETE FROM commands WHERE deviceId = ?`).run(deviceId);
  },

  clearCommand(deviceId, commandId) {
    db.prepare(`DELETE FROM commands WHERE deviceId = ? AND id = ?`).run(deviceId, commandId);
  }
};

const ReleaseRepo = {
  getAll() {
    return db.prepare(`SELECT * FROM app_releases ORDER BY versionCode DESC`).all();
  },

  getLatest() {
    return db.prepare(`SELECT * FROM app_releases ORDER BY versionCode DESC LIMIT 1`).get() || null;
  },

  add(r) {
    db.prepare(`
      INSERT OR REPLACE INTO app_releases (versionCode, versionName, filename, downloadUrl, fileSize, sha256, changelog, publishedAt)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `).run(
      r.versionCode,
      r.versionName,
      r.filename,
      r.downloadUrl,
      r.fileSize,
      r.sha256,
      r.changelog || '',
      r.publishedAt || new Date().toISOString()
    );

    // Keep max 5 releases in DB
    const old = db.prepare(`SELECT versionCode, filename FROM app_releases ORDER BY versionCode DESC LIMIT -1 OFFSET 5`).all();
    for (const o of old) {
      db.prepare(`DELETE FROM app_releases WHERE versionCode = ?`).run(o.versionCode);
    }
    return old; // Caller can delete files if needed
  }
};

const AuditRepo = {
  log(eventType, deviceId, chatId, details, ip) {
    try {
      db.prepare(`
        INSERT INTO audit_logs (eventType, deviceId, chatId, details, ip, timestamp)
        VALUES (?, ?, ?, ?, ?, ?)
      `).run(
        eventType,
        deviceId || '',
        chatId ? String(chatId) : '',
        details ? JSON.stringify(details) : '',
        ip || '',
        Date.now()
      );
    } catch (e) {
      console.error('[Audit Log] Failed to insert log:', e.message);
    }
  },

  getRecent(limit = 50, offset = 0) {
    return db.prepare(`
      SELECT * FROM audit_logs 
      ORDER BY timestamp DESC 
      LIMIT ? OFFSET ?
    `).all(limit, offset).map(r => ({
      id: r.id,
      eventType: r.eventType,
      deviceId: r.deviceId,
      chatId: r.chatId,
      details: r.details ? JSON.parse(r.details) : null,
      ip: r.ip,
      timestamp: r.timestamp
    }));
  }
};

const EvidenceRepo = {
  recordEvidence(e) {
    const ttlMs = 7 * 24 * 60 * 60 * 1000; // 7 days retention
    db.prepare(`
      INSERT INTO evidence (id, commandId, deviceId, type, filename, fileSize, sha256, isEncrypted, mimeType, createdAt, expiresAt)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(
      e.id,
      e.commandId || '',
      e.deviceId || '',
      e.type || '',
      e.filename,
      e.fileSize || 0,
      e.sha256 || '',
      e.isEncrypted ? 1 : 0,
      e.mimeType || 'application/octet-stream',
      Date.now(),
      Date.now() + ttlMs
    );
  },

  getExpired() {
    return db.prepare(`SELECT * FROM evidence WHERE expiresAt <= ?`).all(Date.now());
  },

  deleteRecord(id) {
    db.prepare(`DELETE FROM evidence WHERE id = ?`).run(id);
  },

  purgeExpired(evidenceDir) {
    const expired = this.getExpired();
    let purged = 0;
    for (const item of expired) {
      if (evidenceDir && item.filename) {
        const filePath = path.join(evidenceDir, item.filename);
        try {
          if (fs.existsSync(filePath)) {
            fs.unlinkSync(filePath);
          }
        } catch (err) {
          console.error(`[Evidence Purge] Failed to unlink ${item.filename}:`, err.message);
        }
      }
      this.deleteRecord(item.id);
      purged++;
    }
    return purged;
  }
};

module.exports = {
  initDatabase,
  migrateFromJson,
  DeviceRepo,
  CommandRepo,
  ReleaseRepo,
  AuditRepo,
  EvidenceRepo
};
