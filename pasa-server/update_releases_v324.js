const fs = require('fs');
const path = require('path');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const p = path.join(__dirname, 'data', 'app_releases.json');
const list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));

if (!list.some(r => r.versionCode === 29)) {
  const newRel = {
    versionCode: 29,
    versionName: "3.2.4",
    releaseDate: "2026-09-21",
    fileName: "pasa-v3.2.4-29.apk",
    downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.2.4-29.apk",
    sha256: "bdffd86a972fef1c0094d511329eca67276f950e8720ef0bbd320c3214c21562",
    size: 19051821,
    mandatory: false,
    releaseNotes: "v3.2.4: Duress PIN, Live GPS Tracking & OS Escrow Token Resiliency: Fixed server command routing for /duress_pin <pin> so decoy coercion PINs register immediately without being swallowed by the menu; Enhanced /track continuous live tracking with immediate initial GPS fix and active polling loop; Preserved Android Enterprise Keyguard escrow tokens in PasaDeviceAdmin preventing token invalidation on service restarts."
  };
  list.unshift(newRel);
  fs.writeFileSync(p, JSON.stringify(list, null, 2));
  console.log("Added build 29 to app_releases.json");
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
