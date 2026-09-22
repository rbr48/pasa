const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.4.6-42.apk');
let sha256 = '0a4ed06c32092c0137f20f73c1187476af43f2fcc18b702f562bd7a0e1d7c89e';
let size = 19119002;

if (fs.existsSync(apkPath)) {
  const buf = fs.readFileSync(apkPath);
  size = buf.length;
  sha256 = crypto.createHash('sha256').update(buf).digest('hex');
  console.log(`Calculated sha256: ${sha256}, size: ${size}`);
} else {
  console.warn(`APK not found locally at ${apkPath}, will be updated when present`);
}

const p = path.join(__dirname, 'data', 'app_releases.json');
let list = [];
try {
  list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));
} catch (_) {
  list = [];
}

const filteredList = list.filter(r => r.versionCode !== 42);

const newRel = {
  versionCode: 42,
  versionName: "3.4.6",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.4.6-42.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.4.6-42.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.4.6: Resilient VPS Gateway Relay & ISP Firewall Bypass — Automatic VPS Gateway long-polling when server is configured (bypassing ISP/BTRC Telegram API blocks); Restored seamless C2 command dispatching for all paired devices; Real-time status reporting and instant duress execution."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 42");

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

console.log("ReleaseRepo successfully updated with all releases up to build 42!");
