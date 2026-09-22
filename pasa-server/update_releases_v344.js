const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.4.4-40.apk');
let sha256 = '40fe9783ae3c63911d5243ebccbca0b33de28cac8d7301abbd8b8c9d57414500';
let size = 19119006;

if (fs.existsSync(apkPath)) {
  const buf = fs.readFileSync(apkPath);
  size = buf.length;
  sha256 = crypto.createHash('sha256').update(buf).digest('hex');
  console.log(`Calculated sha256: ${sha256}, size: ${size}`);
} else {
  console.warn(`APK not found locally at ${apkPath}, using precalculated values`);
}

const p = path.join(__dirname, 'data', 'app_releases.json');
let list = [];
try {
  list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));
} catch (_) {
  list = [];
}

const filteredList = list.filter(r => r.versionCode !== 40);

const newRel = {
  versionCode: 40,
  versionName: "3.4.4",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.4.4-40.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.4.4-40.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.4.4: Pure Sovereign Architecture & Forensics Hardening — Direct-to-Telegram primary C2; Offline on-device Ed25519 licensing (/license status, /license activate); Fixed Lost Mode screen-touch silent mugshot trap via headless CameraX and concurrent GNSS fix; Password-free Fake Shutdown and Wake (/fakeshutdown, /wake); Automated 'Controlled permissions' system alert suppression."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 40");

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

console.log("ReleaseRepo successfully updated with all releases up to build 40!");
