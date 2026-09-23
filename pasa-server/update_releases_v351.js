const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.5.1-47.apk');
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

const filteredList = list.filter(r => r.versionCode !== 47);

const newRel = {
  versionCode: 47,
  versionName: "3.5.1",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.5.1-47.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.5.1-47.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.5.1: Deep Hardware Anti-EDL & Bootloader Hardening Suite — Anti-EDL Dead Man's Switch (/deadman) autonomous offline countdown auto-destruct timer (triggers irreversible dpm.wipeData(0) cryptographic factory reset if stolen phone is isolated offline without owner contact), Thermal Anomaly Trap (/thermal) detecting heat-gun back-cover ungluing (>48°C) for Qualcomm 9008 EDL / MediaTek BROM test-point attacks (instantly severs USB data pins via setUsbDataSignalingEnabled(false), locks Knox Kiosk, snaps mugshot, and alerts Telegram SOS), permanent OEM unlock lockout hardening (setOemUnlockAllowed(false), DISALLOW_DEBUGGING_FEATURES, DEVELOPMENT_SETTINGS_ENABLED=0 blocking fastboot flashing), full air-gapped SMS control (/deadman, /thermal), and interactive Telegram console traps hub integration."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 47");

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
console.log("Synchronized ReleaseRepo with build 47");
