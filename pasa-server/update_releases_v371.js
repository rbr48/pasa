const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.7.1-66.apk');
let sha256 = '24465711c5b08d7afb4b84f6519ecc84ada3876bc77f2b59e146869347dc5bd3';
let size = 19217342;

if (fs.existsSync(apkPath)) {
  const buf = fs.readFileSync(apkPath);
  size = buf.length;
  sha256 = crypto.createHash('sha256').update(buf).digest('hex');
  console.log(`Calculated sha256: ${sha256}, size: ${size}`);
} else {
  console.warn(`APK not found locally at ${apkPath}, using precomputed hash.`);
}

const p = path.join(__dirname, 'data', 'app_releases.json');
let list = [];
try {
  list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));
} catch (_) {
  list = [];
}

const filteredList = list.filter(r => r.versionCode !== 66);

const newRel = {
  versionCode: 66,
  versionName: "3.7.1",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.7.1-66.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.7.1-66.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.7.1: Telegram Bot C2 Connection Fix — resolved RFC 3986 relative URL scheme parsing error on TelegramApi endpoints and added robust copy-paste token sanitization; includes all v3.7.0 Strategic Roadmap features (Pre-Unlock Direct Boot SMS C2, /autostart OEM Dispatcher, Air-Gapped Ed25519 SMS License Renewal, and Full Kernel Security Event Stream)."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 66");

for (const r of filteredList) {
  const mapped = {
    versionCode: r.versionCode,
    versionName: r.versionName,
    filename: r.fileName || r.filename || ('pasa-v' + r.versionName + '.apk'),
    downloadUrl: r.downloadUrl || ('https://pasa.izhaanintellect.fun/releases/' + (r.fileName || r.filename || 'pasa-v' + r.versionName + '.apk')),
    fileSize: r.fileSize || r.size || 0,
    sha256: r.sha256 || '',
    changelog: r.changelog || r.releaseNotes || '',
    publishedAt: r.publishedAt || r.releaseDate || new Date().toISOString()
  };
  ReleaseRepo.add(mapped);
}

console.log("Successfully synchronized release catalog in pasa.db to v3.7.1 (Build 66)");
