const { DatabaseSync } = require('node:sqlite');
const db = new DatabaseSync('/var/www/pasa-server/data/pasa.db');

const release = {
  versionCode: 57,
  versionName: '3.6.1',
  filename: 'pasa-v3.6.1-57.apk',
  downloadUrl: 'https://pasa.izhaanintellect.fun/releases/pasa-v3.6.1-57.apk',
  fileSize: 19079342,
  sha256: '3885be9cd9df009404dc3dc45d1b7bd00136ed715eec383fea0e9af50644ceba',
  changelog: 'v3.6.1: Zero-Data Unblocked Sovereign Proxy — Routes Telegram C2 commands through stateless, memory-only Cloudflare/Nginx reverse proxy to bypass regional ISP/BTRC blocks without requiring VPN. Zero logs, zero caching, zero customer data stored.',
  publishedAt: new Date().toISOString()
};

db.prepare(`
  INSERT INTO app_releases (versionCode, versionName, filename, downloadUrl, fileSize, sha256, changelog, publishedAt)
  VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  ON CONFLICT(versionCode) DO UPDATE SET
    versionName=excluded.versionName,
    filename=excluded.filename,
    downloadUrl=excluded.downloadUrl,
    fileSize=excluded.fileSize,
    sha256=excluded.sha256,
    changelog=excluded.changelog,
    publishedAt=excluded.publishedAt
`).run(
  release.versionCode,
  release.versionName,
  release.filename,
  release.downloadUrl,
  release.fileSize,
  release.sha256,
  release.changelog,
  release.publishedAt
);

console.log('✅ SQLite updated successfully with release v3.6.1 (Build 57)!');
const latest = db.prepare('SELECT * FROM app_releases ORDER BY versionCode DESC LIMIT 1').get();
console.log('Latest in DB:', latest);
