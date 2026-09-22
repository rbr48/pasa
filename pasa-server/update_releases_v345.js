const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.4.5-41.apk');
let sha256 = 'f24aad1dd365091150e29d054bae0faec7abbfa71ad94aa19fac098c2e0923ce';
let size = 19119007;

if (fs.existsSync(apkPath)) {
  const buf = fs.readFileSync(apkPath);
  size = buf.length;
  sha256 = crypto.createHash('sha256').update(buf).digest('hex');
  console.log(`Calculated sha256: ${sha256}, size: ${size}`);
} else {
  console.warn(`APK not found locally at ${apkPath}, will be updated when present or precalculated`);
}

const p = path.join(__dirname, 'data', 'app_releases.json');
let list = [];
try {
  list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));
} catch (_) {
  list = [];
}

const filteredList = list.filter(r => r.versionCode !== 41);

const newRel = {
  versionCode: 41,
  versionName: "3.4.5",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.4.5-41.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.4.5-41.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.4.5: Hardened Lock & Unlock Orchestration & Covert Duress Anti-Coercion Suite — Resilient screen-touch mugshot trap with front-camera fallback; Fixed lockscreen biometrics restoration and banner clearing on /unlock; Zero-hang Decoy Duress unlock with asynchronous keyguard dismissal and hardware gesture swipe-up; Auto-enrolled cryptographic escrow tokens on /duress_pin; Android 16 post-update resurrection via exact AlarmManager watchdog."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 41");

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

console.log("ReleaseRepo successfully updated with all releases up to build 41!");
