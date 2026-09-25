const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.7.0-65.apk');
let sha256 = '53e39d901af1f4f1a221df8f4c86dbf5aa3a2842e355cc6e55698823c5b02061';
let size = 19217343;

if (fs.existsSync(apkPath)) {
  const buf = fs.readFileSync(apkPath);
  size = buf.length;
  sha256 = crypto.createHash('sha256').update(buf).digest('hex');
  console.log(`Calculated sha256: ${sha256}, size: ${size}`);
} else {
  console.warn(`APK not found locally at ${apkPath}, using precomputed hash.`);
}

const p = path.join(__dirname, 'data', 'app_releases.json');
let list = [];
try {
  list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));
} catch (_) {
  list = [];
}

const filteredList = list.filter(r => r.versionCode !== 65);

const newRel = {
  versionCode: 65,
  versionName: "3.7.0",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.7.0-65.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.7.0-65.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.7.0: Strategic Security Architecture — Device Protected Storage for Pre-Unlock SMS (cold-boot SMS C2 before first unlock), Automated OEM Autostart Intent Dispatcher (/autostart 1-tap whitelisting for Xiaomi HyperOS, Samsung OneUI, Huawei, ColorOS, Vivo), Air-Gapped Ed25519 SMS License Renewal (PASA LIC offline 160-char signed payload), and Full Kernel Security Event Stream (/security_audit full direct text document export with zero-leak credential redaction)."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 65");

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

console.log("Successfully synchronized release catalog in SQLite database. Latest release:");
console.log(ReleaseRepo.getLatest());
