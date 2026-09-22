const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.4.8-44.apk');
let sha256 = '6e84bc88e1d42aff7543445bb55725b08b43e087a8e92fbf268423261844e78b';
let size = 19134782;

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

const filteredList = list.filter(r => r.versionCode !== 44);

const newRel = {
  versionCode: 44,
  versionName: "3.4.8",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.4.8-44.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.4.8-44.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.4.8: Enterprise Device Owner Sovereign Suite & Air-Gapped Cellular SMS Fallback — Hardware camera lockout (/camera_lock), Bluetooth radio & sharing lockout (/bluetooth_lock), Hardware HAL audio master mute (/mic_mute), OS lockscreen banner canvas (/lockscreen_info), Screen inactivity timeout policy (/autolock), Emergency Wi-Fi provisioning (/wifi_connect), Kernel OS security audit logging (/security_audit), Silent uninstallation (/app_uninstall), and complete air-gapped cellular SMS fallback control for all commands when internet is unavailable."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 44");

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
