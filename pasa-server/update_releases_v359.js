const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.5.9-55.apk');
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

const filteredList = list.filter(r => r.versionCode !== 55);

const newRel = {
  versionCode: 55,
  versionName: "3.5.9",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.5.9-55.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.5.9-55.apk",
  sha256: sha256 || 'a217075d4a7196b68bffd958222f11fb49054a3a64be904c489099a9132eb68b',
  size: size || 19234190,
  mandatory: false,
  releaseNotes: "v3.5.9: Sovereign Hardening & Independent Audit Compliance — Complete R8 bytecode log stripping (CWE-532), SetupActivity anti-snooping FLAG_SECURE and keyboard IME dictionary learning prevention, Cloudflare-resilient multi-root CA certificate pinning for sovereign C2 infrastructure, and explicit domain network security policy."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 55");

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
