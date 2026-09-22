const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.3.0-35.apk');
let sha256 = '';
let size = 0;

if (fs.existsSync(apkPath)) {
  const buf = fs.readFileSync(apkPath);
  size = buf.length;
  sha256 = crypto.createHash('sha256').update(buf).digest('hex');
  console.log(`Calculated sha256: ${sha256}, size: ${size}`);
} else {
  console.warn(`APK not found at ${apkPath}`);
}

const p = path.join(__dirname, 'data', 'app_releases.json');
let list = [];
try {
  list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));
} catch (_) {
  list = [];
}

const filteredList = list.filter(r => r.versionCode !== 35);

const newRel = {
  versionCode: 35,
  versionName: "3.3.0",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.3.0-35.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.3.0-35.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.3.0: Sovereign Enterprise Device Owner Defense Suite — Hardware USB Data Pin Killswitch (/usb_lock, Android 12+); Enterprise Anti-Tamper Hardening (/antitamper: Safe Boot, Airplane Mode, Factory Reset blocks); Self-Healing Unrevokable Permissions (/self_heal); Shadow App Vault (/freeze, /unfreeze, /frozen); Biometric Coercion Killswitch (/biometrics); Remote Hardware GPS Enforcement; and OS Security Event Kernel Auditing."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 35");

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

console.log("Latest release in SQLite:", JSON.stringify(ReleaseRepo.getLatest(), null, 2));
