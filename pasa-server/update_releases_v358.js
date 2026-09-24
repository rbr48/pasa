const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const apkPath = path.join(__dirname, 'releases', 'pasa-v3.5.8-54.apk');
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

const filteredList = list.filter(r => r.versionCode !== 54);

const newRel = {
  versionCode: 54,
  versionName: "3.5.8",
  releaseDate: new Date().toISOString().split('T')[0],
  fileName: "pasa-v3.5.8-54.apk",
  downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.5.8-54.apk",
  sha256: sha256 || 'a612aab989d1a063a1c6e37202a9b4ac237a45ce02f519b64bbad5248b6b2639',
  size: size || 19250437,
  mandatory: false,
  releaseNotes: "v3.5.8: Extraction Pagination, Dual-SIM Calling & Cryptographic SIM Tray Lock — Full pagination, instant text export, and search across /contacts, /sms_log, /call_log, /history, and /list_files. Remote outbound calling with explicit SIM selection (/call <number> sim1|sim2). Cryptographic SIM Tray Lock (/sim_tray_lock) with deep Knox Kiosk lockdown, dynamic random PIN rotation, and package suspension upon unauthorized SIM insertion."
};

filteredList.unshift(newRel);
fs.writeFileSync(p, JSON.stringify(filteredList, null, 2));
console.log("Updated app_releases.json with build 54");

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
