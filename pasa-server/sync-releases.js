const fs = require('fs');
const path = require('path');
const { initDatabase, ReleaseRepo } = require('./lib/db');

initDatabase(path.join(__dirname, 'data', 'pasa.db'));

const data = JSON.parse(fs.readFileSync(path.join(__dirname, 'data', 'app_releases.json'), 'utf8'));
for (const r of data) {
  ReleaseRepo.add(r);
  console.log(`Synced release v${r.versionName} (Build ${r.versionCode})`);
}
console.log('Latest release in SQLite:', ReleaseRepo.getLatest());
