const fs = require('fs');
const path = require('path');
const { initDatabase, ReleaseRepo } = require('./lib/db');

const dbPath = path.join(__dirname, 'data', 'pasa.db');
initDatabase(dbPath);

const p = path.join(__dirname, 'data', 'app_releases.json');
const list = JSON.parse(fs.readFileSync(p, 'utf8').replace(/^\uFEFF/, ''));

// Check if build 27 already exists
if (!list.some(r => r.versionCode === 27)) {
  const newRel = {
    versionCode: 27,
    versionName: "3.2.2",
    releaseDate: "2026-09-21",
    fileName: "pasa-v3.2.2-27.apk",
    downloadUrl: "https://pasa.izhaanintellect.fun/releases/pasa-v3.2.2-27.apk",
    sha256: "a5a3ec472c7ae5e522b9bf21f72a534f0d284601466f58d8fdcbc8a0b86352e5",
    size: 19035079,
    mandatory: false,
    releaseNotes: "v3.2.2: Android 16 CameraX Lifecycle and Execution Resiliency: Fix CameraX lifecycle resumption over lockscreen with dimmed display activation; Dynamic command execution deadlines (120s for video/screen recording, 150s for audio, 360s for livestreaming) eliminating false 45s timeouts; Explicit standalone /hide and /show command routing; Accessibility screen capture thread dispatch fixes."
  };
  list.unshift(newRel);
  fs.writeFileSync(p, JSON.stringify(list, null, 2));
  console.log("Added build 27 to app_releases.json");
}

// Sync to SQLite
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
