const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.4.3-39.apk');
let sha256 = 'd603f3d470607687ed5ad254534abdc0b849a168e6fc52a70fa525953674ad90';
let size = 19119021;

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

const filteredList = list.filter(r => r.versionCode !== 39);

const newRel = {
  versionCode: 39,
  versionName: "3.4.3",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.4.3-39.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.4.3-39.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.4.3: Factory Reset Defense & Core Resilience Hardening — Recovery Mode & Fastboot Lockout (/harden_boot); Real-time Threat & Tamper Detection (/tamper_detect); Boot Gap & Time Anomaly Analysis (/boot_gap); JobScheduler Daemon Resurrection Core; Enhanced Streaming Screen Recorder."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 39");

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

console.log("ReleaseRepo successfully updated with all releases up to build 39!");
