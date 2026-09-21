const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.2.6-31.apk');
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

// Filter out any existing v31 entry
const filteredList = list.filter(r => r.versionCode !== 31);

const newRel = {
  versionCode: 31,
  versionName: "3.2.6",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.2.6-31.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.2.6-31.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.2.6: Native Lockscreen Duress PIN Detection & System Escrow Prompt: Automated hardware token arming via Android Keyguard ConfirmDeviceCredential (EscrowActivationActivity); Native lockscreen keypad digit capture via Accessibility for instant Duress PIN SOS beacons, covert attacker selfies, and 2-minute live GPS tracking; In-app PIN unlock dialog with duress decoy support on Lost Mode overlay."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 31");

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
