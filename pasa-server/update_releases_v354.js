const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.5.4-50.apk');
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

const filteredList = list.filter(r => r.versionCode !== 50);

const newRel = {
  versionCode: 50,
  versionName: "3.5.4",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.5.4-50.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.5.4-50.apk",
  sha256: sha256,
  size: size,
  mandatory: false,
  releaseNotes: "v3.5.4: Commercial Tier Rebalancing & Enterprise Sovereignty — Core loss-recovery tools (/locate, /tower, /ring, /vibrate_pulse, /lock, /message, /lockscreen_info, /status, /reboot, /wipe) are Lifetime Free. Advanced covert surveillance (/snap, /video, /record, /screenrecord), remote storage extraction (/gallery_latest, /getfile), Knox enterprise hardware controls (/usb_lock, /camera_lock, /antitamper), and autonomous traps (/trap, /sim_lock, /duress_pin) are exclusively gated behind Pro tier."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 50");

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
console.log("Release repository updated successfully for v3.5.4 (Build 50).");
