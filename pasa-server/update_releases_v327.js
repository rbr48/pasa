const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.2.7-32.apk');
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
const list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));

// Filter out any existing v32 entry
const filteredList = list.filter(r => r.versionCode !== 32);

const newRel = {
  versionCode: 32,
  versionName: "3.2.7",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.2.7-32.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.2.7-32.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.2.7: Full Lockscreen Physical Duress Unlock & Multi-Transport Escrow Alerting: Entering Duress PIN now instantly clears hardware lockscreen PIN via Device Owner escrow token, dispatches swipe-up and HOME actions, and launches DuressUnlockActivity to dismiss Keyguard to Home screen; Added gesture dispatch permissions in Accessibility service; Fixed backend dual-transport in EscrowActivationActivity to guarantee Telegram confirmations upon credential entry."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 32");

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
