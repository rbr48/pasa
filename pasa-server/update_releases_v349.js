const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.4.9-45.apk');
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

const filteredList = list.filter(r => r.versionCode !== 45);

const newRel = {
  versionCode: 45,
  versionName: "3.4.9",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.4.9-45.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.4.9-45.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.4.9: Remote Outbound Calling, Smart App Lockout & Storage Forensics Suite — Remotely dial any phone number with automatic speakerphone routing (/call <number> [speaker]), smart keyword app freezing for Gallery, Phone, and Storage (/lock_app, /unlock_app), recent camera roll extraction direct to Telegram (/gallery_latest [count]), storage file downloader up to 50MB (/getfile <path>), storage directory browser (/list_files [dir]), air-gapped SMS dialing and app locking fallback (PASA <PIN> /call, /lock_app), and interactive 6-hub Telegram console wizards."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 45");

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
console.log("Synced all releases to SQLite ReleaseRepo. Done.");
