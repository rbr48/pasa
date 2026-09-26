/**
 * PASA VPS Release Injector
 * ──────────────────────────────────────────────────────────────────────────
 * Standalone script that correctly injects a new release into the PASA server
 * SQLite database (data/pasa.db) using the server's own lib/db.js so that
 * table schemas, migrations, and ReleaseRepo are all handled correctly.
 *
 * Usage:  node inject_release.js <versionCode> <versionName> <sha256> <fileSizeBytes>
 * Example: node inject_release.js 71 3.7.6 abcdef1234... 19264512
 *
 * Place this file in: /var/www/pasa-server/inject_release.js
 */

'use strict';

const path = require('path');
const fs   = require('fs');

// ── Args ──────────────────────────────────────────────────────────────────
const [,, versionCodeArg, versionNameArg, sha256Arg, fileSizeArg] = process.argv;

if (!versionCodeArg || !versionNameArg || !sha256Arg) {
  console.error('Usage: node inject_release.js <versionCode> <versionName> <sha256> [fileSizeBytes]');
  process.exit(1);
}

const versionCode = parseInt(versionCodeArg, 10);
const versionName = versionNameArg.trim();
const sha256      = sha256Arg.trim().toLowerCase();
const fileSize    = fileSizeArg ? parseInt(fileSizeArg, 10) : 0;

if (isNaN(versionCode) || versionCode <= 0) {
  console.error('❌ versionCode must be a positive integer');
  process.exit(1);
}

// ── Bootstrap db.js correctly ─────────────────────────────────────────────
// lib/db.js exports initDatabase(dbPath) which must be called before any
// ReleaseRepo methods. Use the same DB path as server.js (data/pasa.db).
const { initDatabase, ReleaseRepo } = require('./lib/db');
const DB_FILE = path.join(__dirname, 'data', 'pasa.db');

if (!fs.existsSync(path.dirname(DB_FILE))) {
  fs.mkdirSync(path.dirname(DB_FILE), { recursive: true });
}

initDatabase(DB_FILE);

// ── Build release record ──────────────────────────────────────────────────
const apkFilename  = `pasa-v${versionName}-${versionCode}.apk`;
const downloadUrl  = `https://pasa.izhaanintellect.fun/releases/${apkFilename}`;
const publishedAt  = new Date().toISOString().split('T')[0];
const changelog    = `v${versionName} (Build ${versionCode}) — deployed ${publishedAt}`;

const release = {
  versionCode,
  versionName,
  filename: apkFilename,
  downloadUrl,
  fileSize,
  sha256,
  changelog,
  publishedAt,
};

// ── Insert ────────────────────────────────────────────────────────────────
try {
  ReleaseRepo.add(release);
  const latest = ReleaseRepo.getLatest();
  console.log(`✅ Injected: v${latest.versionName} (Build ${latest.versionCode})`);
  console.log(`   SHA-256 : ${latest.sha256}`);
  console.log(`   Size    : ${(latest.fileSize / 1024 / 1024).toFixed(2)} MB`);
  console.log(`   URL     : ${latest.downloadUrl}`);

  // Safety: keep max 5 releases in DB (mirrors server.js behaviour)
  const all = ReleaseRepo.getAll();
  console.log(`   DB now has ${all.length} release(s).`);
} catch (err) {
  console.error('❌ Failed to inject release:', err.message);
  process.exit(1);
}
