const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.4.7-43.apk');
let sha256 = '8bc8e0003082a10b3b8e6526f13d48d9b15079e084d3c6f430e04103c1366681';
let size = 19118403;

if (fs.existsSync(apkPath)) {
  const buf = fs.readFileSync(apkPath);
  size = buf.length;
  sha256 = crypto.createHash('sha256').update(buf).digest('hex');
  console.log(`Calculated sha256: ${sha256}, size: ${size}`);
} else {
  console.warn(`APK not found locally at ${apkPath}, using precomputed hash`);
}

const p = path.join(__dirname, 'data', 'app_releases.json');
let list = [];
try {
  list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));
} catch (_) {
  list = [];
}

const filteredList = list.filter(r => r.versionCode !== 43);

const newRel = {
  versionCode: 43,
  versionName: "3.4.7",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.4.7-43.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.4.7-43.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.4.7: LiveStream Camera & Audio Elevation, Zero-Lag A11y & Background Sensor Threading — Dynamic FOREGROUND_SERVICE_TYPE_CAMERA & MICROPHONE elevation; Dedicated background HandlerThreads for accelerometer and proximity traps eliminating UI frame drops; Restrictive A11y event filtering; Streamlined sovereign Telegram setup."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 43");

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

console.log("ReleaseRepo successfully updated with all releases up to build 43!");
