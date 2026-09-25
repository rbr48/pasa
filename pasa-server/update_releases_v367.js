const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.6.7-63.apk');
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

const filteredList = list.filter(r => r.versionCode !== 63);

const newRel = {
  versionCode: 63,
  versionName: "3.6.7",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.6.7-63.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.6.7-63.apk",
  sha256: sha256 || 'b10ab5be4e4a47a361c4f08bc8f6d46e328a810ab97d3aa01711187acc828dcf',
  size: size || 19200899,
  mandatory: false,
  releaseNotes: "v3.6.7: Zero-Knowledge Blind Telegram Tunnel & Sovereign On-Device C2 Console — Resolves regional Telegram bot API blockades via stateless HTTP CONNECT blind tunnel on VPS (port 8443, zero VPS TLS termination or logging). Eliminates VPS bot polling entirely to guarantee 100% sovereign anonymous operation (bot token and owner chat ID never leave device). Complete on-device command console with 6-hub interactive inline navigation, callback query responses, and direct Telegram polling."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 63");

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
