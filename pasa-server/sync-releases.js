const fs = require('fs');
const path = require('path');
const { initDatabase, ReleaseRepo } = require('./lib/db');

initDatabase(path.join(__dirname, 'data', 'pasa.db'));

const raw = fs.readFileSync(path.join(__dirname, 'data', 'app_releases.json'), 'utf8').replace(/^\uFEFF/, '');
const data = JSON.parse(raw);

for (const r of data) {
  // Map JSON keys -> DB schema keys
  const mapped = {
    versionCode:  r.versionCode,
    versionName:  r.versionName,
    filename:     r.fileName   || r.filename   || ('pasa-v' + r.versionName + '.apk'),
    downloadUrl:  r.downloadUrl || ('https://pasa.izhaanintellect.fun/releases/' + (r.fileName || r.filename || 'pasa-v' + r.versionName + '.apk')),
    fileSize:     r.fileSize   || r.size       || 0,
    sha256:       r.sha256     || '',
    changelog:    r.changelog  || r.releaseNotes || '',
    publishedAt:  r.publishedAt || r.releaseDate  || new Date().toISOString()
  };
  ReleaseRepo.add(mapped);
  console.log('Synced release v' + r.versionName + ' (Build ' + r.versionCode + ')');
}
console.log('Latest release in SQLite:', JSON.stringify(ReleaseRepo.getLatest(), null, 2));
