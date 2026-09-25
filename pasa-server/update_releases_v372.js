const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.7.2-67.apk');
let sha256 = '';
let size = 0;

if (fs.existsSync(apkPath)) {
  const buf = fs.readFileSync(apkPath);
  size = buf.length;
  sha256 = crypto.createHash('sha256').update(buf).digest('hex');
  console.log(`Calculated sha256: ${sha256}, size: ${size}`);
} else {
  console.error(`APK not found locally at ${apkPath}`);
  process.exit(1);
}

const p = path.join(__dirname, 'data', 'app_releases.json');
let list = [];
try {
  list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));
} catch (_) {
  list = [];
}

const filteredList = list.filter(r => r.versionCode !== 67);

const newRel = {
  versionCode: 67,
  versionName: "3.7.2",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.7.2-67.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.7.2-67.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.7.2: 100% Pure Sovereign Direct-to-Telegram Architecture — completely removed hidden VPS server URL configuration, card layout, and backend dependencies. Zero customer PII or bot tokens transmitted to any external server. Polling is direct from phone to official Telegram API."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 67");

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

console.log("Successfully synchronized release catalog in pasa.db to v3.7.2 (Build 67)");
