// scripts/verify_independent_audit.js
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const https = require('https');

const apkPath = path.resolve(__dirname, '../releases/pasa-v3.5.6-52.apk');
const setupKitPath = path.resolve(__dirname, '../releases/PASA-Device-Owner-Setup-Kit.zip');

if (!fs.existsSync(apkPath)) {
  console.error('❌ Error: APK not found at ' + apkPath);
  process.exit(1);
}

const apkBuffer = fs.readFileSync(apkPath);
const apkSha256 = crypto.createHash('sha256').update(apkBuffer).digest('hex');
const apkMd5 = crypto.createHash('md5').update(apkBuffer).digest('hex');
const apkSha1 = crypto.createHash('sha1').update(apkBuffer).digest('hex');
const apkSize = apkBuffer.length;

let setupKitSha256 = 'N/A';
let setupKitSize = 0;
if (fs.existsSync(setupKitPath)) {
  const kitBuffer = fs.readFileSync(setupKitPath);
  setupKitSha256 = crypto.createHash('sha256').update(kitBuffer).digest('hex');
  setupKitSize = kitBuffer.length;
}

console.log('═══════════════════════════════════════════════════════════════════════');
console.log('🛡️ PASA SENTINEL — INDEPENDENT SECURITY & INTEGRITY AUDIT');
console.log('═══════════════════════════════════════════════════════════════════════');
console.log(`[Target APK]:        pasa-v3.5.6-52.apk`);
console.log(`[File Size]:         ${apkSize.toLocaleString()} bytes (${(apkSize / 1024 / 1024).toFixed(2)} MB)`);
console.log(`[SHA-256 Digest]:    ${apkSha256}`);
console.log(`[SHA-1 Digest]:      ${apkSha1}`);
console.log(`[MD5 Digest]:        ${apkMd5}`);
console.log('───────────────────────────────────────────────────────────────────────');
console.log(`[Windows Setup Kit]: PASA-Device-Owner-Setup-Kit.zip`);
console.log(`[Setup Kit Size]:    ${setupKitSize.toLocaleString()} bytes (${(setupKitSize / 1024).toFixed(1)} KB)`);
console.log(`[Setup Kit SHA-256]: ${setupKitSha256}`);
console.log('═══════════════════════════════════════════════════════════════════════');

const vtUrl = `https://www.virustotal.com/gui/file/${apkSha256}`;
console.log('\n🌐 1. INDEPENDENT VIRUSTOTAL CONSENSUS AUDIT:');
console.log(`   Direct Audit URL: ${vtUrl}`);

console.log('\n🔍 2. LIVE PRODUCTION OTA MANIFEST VERIFICATION:');

function fetchJson(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          reject(e);
        }
      });
    }).on('error', reject);
  });
}

(async () => {
  try {
    const ota = await fetchJson('https://pasa.izhaanintellect.fun/api/app/latest?current_version_code=0');
    if (ota.ok && ota.latest) {
      console.log(`   Live Server Version: v${ota.latest.versionName} (Build ${ota.latest.versionCode})`);
      console.log(`   Live Server SHA-256: ${ota.latest.sha256}`);
      if (ota.latest.sha256.toLowerCase() === apkSha256.toLowerCase()) {
        console.log(`   ✅ BIT-EXACT MATCH: Local APK matches remote production deployment!`);
      } else {
        console.log(`   ❌ MISMATCH: Local APK does not match remote deployment!`);
      }
    } else {
      console.log(`   ⚠️ Could not retrieve live manifest details.`);
    }
  } catch (err) {
    console.log(`   ⚠️ Network check failed: ${err.message}`);
  }

  console.log('\n📋 3. INDEPENDENT AUDIT SUMMARY FOR PUBLIC SHARING:');
  const summary = {
    audit_date: new Date().toISOString(),
    binary: 'pasa-v3.5.6-52.apk',
    version: '3.5.6',
    build: 52,
    sha256: apkSha256,
    sha1: apkSha1,
    md5: apkMd5,
    size_bytes: apkSize,
    virustotal_report: vtUrl,
    mobsf_ci_pipeline: 'https://github.com/rbr48/pasa/actions/workflows/security-audit.yml',
    codeql_security: 'https://github.com/rbr48/pasa/security/code-scanning'
  };

  const summaryPath = path.resolve(__dirname, '../INDEPENDENT_AUDIT_REPORT.json');
  fs.writeFileSync(summaryPath, JSON.stringify(summary, null, 2), 'utf8');
  console.log(`   Report saved to: ${summaryPath}`);
  console.log('═══════════════════════════════════════════════════════════════════════\n');
})();
