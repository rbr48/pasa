const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.4.2-38.apk');
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

const filteredList = list.filter(r => r.versionCode !== 38);

const newRel = {
  versionCode: 38,
  versionName: "3.4.2",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.4.2-38.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.4.2-38.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.4.2: 5 Enterprise Security Modules & Hardening — SIM Lock Swap Guard (/sim_lock: whitelist, swap auto-lock); Tactile Device Locator (/vibrate_pulse: pulse, SOS, continuous); Lockscreen Pattern Guard (/pattern_guard: failure threshold, auto-photo/lock); App Network Firewall (/app_firewall: RAT isolation, blacklist/whitelist); Battery Health & Drain Monitor (/battery_alert: drain rate, charging anomalies)."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 38");

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
