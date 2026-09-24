const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.5.7-53.apk');
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

const filteredList = list.filter(r => r.versionCode !== 53);

const newRel = {
  versionCode: 53,
  versionName: "3.5.7",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.5.7-53.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.5.7-53.apk",
  sha256: sha256 || '275bdd4d49649e9576aa14ae44c37a1bb58d309eb364acc77e9900f2163afb52',
  size: size || 19250438,
  mandatory: false,
  releaseNotes: "v3.5.7: Enhanced App Inventory & Privacy Hardening — Full installed user apps pagination (/apps 2), instant full text inventory export (/apps export / /apps all), real-time app search (/apps search <query>), EXIF metadata stripping, multi-pass cryptographic disk shredder, and live video streaming support."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 53");

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
