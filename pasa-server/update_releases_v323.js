const fs = require('fs');
const path = require('path');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const p = path.join(__dirname, 'data', 'app_releases.json');
const list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));

if (!list.some(r => r.versionCode === 28)) {
  const newRel = {
    versionCode: 28,
    versionName: "3.2.3",
    releaseDate: "2026-09-21",
    fileName: "pasa-v3.2.3-28.apk",
    downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.2.3-28.apk",
    sha256: "ef27c1a3762c56eb753abd2c2173d2d32721deec820cd89f3905d9a66e8b400e",
    size: 19051475,
    mandatory: false,
    releaseNotes: "v3.2.3: Device Owner Remote Reboot and Search Filters: Added remote hardware reboot (/reboot) via Enterprise Device Owner; Added keyword search filters and up to 50 records for /sms_log and /call_log with safe message length formatting."
  };
  list.unshift(newRel);
  fs.writeFileSync(p, JSON.stringify(list, null, 2));
  console.log("Added build 28 to app_releases.json");
}

const updatedList = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));
for (const r of updatedList) {
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

console.log("Latest release in SQLite:", JSON.stringify(ReleaseRepo.getLatest(), null, 2));
