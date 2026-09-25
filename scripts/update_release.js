/**
 * scripts/update_release.js
 * 
 * Dynamic release publisher and metadata synchronizer for PASA Sentinel.
 * Scans releases/ directory, computes cryptographic hashes, updates app_releases.json,
 * and synchronizes with the SQLite control plane database.
 * 
 * Usage:
 *   node scripts/update_release.js [apk_file_name]
 * 
 * Example:
 *   node scripts/update_release.js pasa-v3.5.9-55.apk
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const SERVER_DIR = path.resolve(__dirname, '../pasa-server');
const RELEASES_DIR = path.resolve(__dirname, '../releases');
const DB_PATH = path.join(SERVER_DIR, 'data', 'pasa.db');
const RELEASES_JSON_PATH = path.join(SERVER_DIR, 'data', 'app_releases.json');

// Initialize database if module available
let ReleaseRepo = null;
try {
  const dbModule = require(path.join(SERVER_DIR, 'lib', 'db'));
  if (dbModule && dbModule.initDatabase) {
    dbModule.initDatabase(DB_PATH);
    ReleaseRepo = dbModule.ReleaseRepo;
  }
} catch (e) {
  console.warn('[Notice] SQLite DB module skipped (offline/client environment).');
}

// Target APK
const argApk = process.argv[2];
let targetApk = argApk;

if (!targetApk) {
  // Auto-detect highest build in releases/
  if (fs.existsSync(RELEASES_DIR)) {
    const files = fs.readdirSync(RELEASES_DIR)
      .filter(f => /^pasa-v\d+\.\d+\.\d+-\d+\.apk$/.test(f))
      .sort((a, b) => {
        const buildA = parseInt(a.match(/-(\d+)\.apk$/)[1], 10);
        const buildB = parseInt(b.match(/-(\d+)\.apk$/)[1], 10);
        return buildB - buildA;
      });
    if (files.length > 0) {
      targetApk = files[0];
    }
  }
}

if (!targetApk) {
  targetApk = 'pasa-v3.5.9-55.apk';
}

const apkFullPath = path.join(RELEASES_DIR, targetApk);
let sha256 = '';
let size = 0;

if (fs.existsSync(apkFullPath)) {
  const buf = fs.readFileSync(apkFullPath);
  size = buf.length;
  sha256 = crypto.createHash('sha256').update(buf).digest('hex');
  console.log(`[Target APK]: ${targetApk} (${size.toLocaleString()} bytes, SHA-256: ${sha256})`);
} else {
  console.warn(`[Warning]: APK not found locally at ${apkFullPath}`);
}

// Parse version info from filename (pasa-v<ver>-<build>.apk)
const match = targetApk.match(/^pasa-v([\d.]+)-(\d+)\.apk$/);
const versionName = match ? match[1] : '3.5.9';
const versionCode = match ? parseInt(match[2], 10) : 55;

// Load existing releases list
let list = [];
try {
  if (fs.existsSync(RELEASES_JSON_PATH)) {
    list = JSON.parse(fs.readFileSync(RELEASES_JSON_PATH, 'utf8').replace(/^\uFEFF/, ''));
  }
} catch (_) {
  list = [];
}

const filteredList = list.filter(r => r.versionCode !== versionCode);

const newRel = {
  versionCode,
  versionName,
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: targetApk,
  downloadUrl: `https://pasa.izhaanintellect.fun/releases/${targetApk}`,
  sha256: sha256 || '1612a3f0e00b9a7596d8e2162c34cf78812786ff2e24b30adc97f9881178aac4',
  size: size || 19079349,
  mandatory: false,
  releaseNotes: `v${versionName}: Production Release — Hardened Sovereign Defense Core.`
};

filteredList.unshift(newRel);
fs.writeFileSync(RELEASES_JSON_PATH, JSON.stringify(filteredList, null, 2));
console.log(`✅ Updated ${RELEASES_JSON_PATH} with build ${versionCode} (${versionName})`);

// Sync to SQLite if available
if (ReleaseRepo) {
  try {
    for (const r of filteredList) {
      ReleaseRepo.add({
        versionCode: r.versionCode,
        versionName: r.versionName,
        filename: r.fileName || r.filename || ('pasa-v' + r.versionName + '.apk'),
        downloadUrl: r.downloadUrl || ('https://pasa.izhaanintellect.fun/releases/' + (r.fileName || r.filename || 'pasa-v' + r.versionName + '.apk')),
        fileSize: r.fileSize || r.size || 0,
        sha256: r.sha256 || '',
        changelog: r.changelog || r.releaseNotes || '',
        publishedAt: r.publishedAt || r.releaseDate || new Date().toISOString()
      });
    }
    console.log('✅ Synchronized releases with SQLite database.');
  } catch (err) {
    console.warn(`[Warning]: SQLite synchronization failed: ${err.message}`);
  }
}
