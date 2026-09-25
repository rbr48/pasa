const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.6.8-64.apk');
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

const filteredList = list.filter(r => r.versionCode !== 64);

const newRel = {
  versionCode: 64,
  versionName: "3.6.8",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.6.8-64.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.6.8-64.apk",
  sha256: sha256 || '3ab959d553d316d0690e07edc292b03d00f154d38f855bbb55103b0cac5e7558',
  size: size || 19217293,
  mandatory: false,
  releaseNotes: "v3.6.8: Zero-Trust Master Password & Offline TOTP Hardening — Eliminates rogue server/admin threat model by enforcing client-side Master Password verification across all destructive and device-altering commands (/lock, /unlock, /fakeshutdown, /wake, /set_os_pin, /set_master_pin, /duress_pin, /smssetup, /app_uninstall, /gallery_latest, /getfile, /sim_tray_lock, /antitamper, /usb_lock). Offline cellular SMS fallback seamlessly authenticates with 6-digit TOTP or Master Password."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 64");

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

console.log("Successfully synchronized release catalog in SQLite database. Latest release:");
console.log(ReleaseRepo.getLatest());
