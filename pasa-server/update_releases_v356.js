const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.5.6-52.apk');
let sha256 = '';
let size = 0;

if (fs.existsSync(apkPath)) {
  const buf = fs.readFileSync(apkPath);
  size = buf.length;
  sha256 = crypto.createHash('sha256').update(buf).digest('hex');
  console.log(`Calculated sha256: ${sha256}, size: ${size}`);
} else {
  console.warn(`APK not found locally at ${apkPath}`);
}

const p = path.join(__dirname, 'data', 'app_releases.json');
let list = [];
try {
  list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));
} catch (_) {
  list = [];
}

const filteredList = list.filter(r => r.versionCode !== 52);

const newRel = {
  versionCode: 52,
  versionName: "3.5.6",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.5.6-52.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.5.6-52.apk",
  sha256: sha256 || 'becdb9b13beaac9d790960fc83cbafb957c788800a0f911c284903d1621fa5c8',
  size: size || 19250443,
  mandatory: false,
  releaseNotes: "v3.5.6: Security Hardening & Zero-Trust Architecture — Fail-closed cryptographic envelope verification on Android client, SELinux enforcement false-positive fix on modern Android 16/Moto devices, unassigned ownerChatId hijacking defense, Wipe 2FA PIN confirmation, and 100% Sovereign (Direct Telegram) Mode."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 52");

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

console.log("Synced all releases to SQLite DB successfully!");
