/**
 * pasa-server/scripts/broadcast_channel.js
 * 
 * Automatically posts production release announcements, cryptographic digests,
 * and VirusTotal verification links to the official Telegram channel:
 * @pasa_sentinel_official
 * 
 * Usage:
 *   node pasa-server/scripts/broadcast_channel.js [optional_custom_message]
 */

const fs = require('fs');
const path = require('path');
const https = require('https');

const SERVER_DIR = path.resolve(__dirname, '..');
const ENV_FILE = path.join(SERVER_DIR, '.env');
const RELEASES_FILE = path.join(SERVER_DIR, 'data', 'app_releases.json');
const CHANNEL_HANDLE = '@pasa_sentinel_official';

// Load bot token from environment or .env
let botToken = process.env.PASA_CENTRAL_BOT_TOKEN || process.env.BOT_TOKEN;
if (!botToken && fs.existsSync(ENV_FILE)) {
  const envContent = fs.readFileSync(ENV_FILE, 'utf8');
  const match = envContent.match(/PASA_CENTRAL_BOT_TOKEN=([^\r\n]+)/) || envContent.match(/BOT_TOKEN=([^\r\n]+)/);
  if (match) botToken = match[1].trim();
}

if (!botToken) {
  // Default to central support bot token if not set
  botToken = '8731444238:AAE2jcWkXLNO8n2bc3XvDZDUAGiPTy7nPqY';
}

// Load latest release info
let latestRelease = null;
if (fs.existsSync(RELEASES_FILE)) {
  try {
    const list = JSON.parse(fs.readFileSync(RELEASES_FILE, 'utf8').replace(/^\uFEFF/, ''));
    if (Array.isArray(list) && list.length > 0) {
      latestRelease = list[0];
    }
  } catch (err) {
    console.error('Failed to parse releases catalog:', err.message);
  }
}

if (!latestRelease) {
  console.error('No release found to broadcast.');
  process.exit(1);
}

const customMsg = process.argv[2];

const releaseText = customMsg || `🚀 <b>NEW PRODUCTION RELEASE: PASA Sentinel v${latestRelease.versionName} (Build ${latestRelease.versionCode})</b>
━━━━━━━━━━━━━━━━━━━━
A new verified release of <b>PASA Sentinel</b> has been deployed to the sovereign distribution network.

📦 <b>Binary Details:</b>
• <b>File:</b> <code>${latestRelease.fileName}</code>
• <b>Size:</b> ${(latestRelease.size / (1024 * 1024)).toFixed(2)} MB (${latestRelease.size.toLocaleString()} bytes)
• <b>Target OS:</b> Android 8.0 – 16 (Knox Device Owner)
• <b>SHA-256 Digest:</b>
<code>${latestRelease.sha256}</code>

📝 <b>Changelog & Field Notes:</b>
${latestRelease.releaseNotes || 'Hardened security and operational enhancements.'}

🛡️ <b>Independent Integrity & Audit:</b>
• <b>VirusTotal Multi-AV:</b> <a href="https://www.virustotal.com/gui/file/${latestRelease.sha256}">Inspect Consensus Report</a>
• <b>Audit Artifacts:</b> <a href="https://pasa.izhaanintellect.fun/audit/independent_audit_summary.md">SARIF Audit Attestation</a>

📥 <b>Download & Provisioning:</b>
• <b>Direct APK:</b> <a href="https://pasa.izhaanintellect.fun/releases/${latestRelease.fileName}">Download ${latestRelease.fileName}</a>
• <b>Windows Setup Kit:</b> <a href="https://pasa.izhaanintellect.fun/releases/PASA-Device-Owner-Setup-Kit.zip">Download Setup Kit (ZIP)</a>
• <b>Zero-Touch Portal:</b> https://pasa.izhaanintellect.fun/portal`;

const payload = JSON.stringify({
  chat_id: CHANNEL_HANDLE,
  text: releaseText,
  parse_mode: 'HTML',
  disable_web_page_preview: false
});

console.log(`Broadcasting announcement to ${CHANNEL_HANDLE}...`);

const req = https.request('https://api.telegram.org/bot' + botToken + '/sendMessage', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(payload)
  }
}, res => {
  let body = '';
  res.on('data', chunk => body += chunk);
  res.on('end', () => {
    try {
      const resp = JSON.parse(body);
      if (resp.ok) {
        console.log(`✅ Successfully broadcasted announcement to ${CHANNEL_HANDLE} (Message ID: ${resp.result.message_id})`);
      } else {
        console.error(`❌ Broadcast failed:`, resp.description);
      }
    } catch (e) {
      console.error('Invalid response from Telegram API:', body);
    }
  });
});

req.on('error', err => {
  console.error('Network error during broadcast:', err.message);
});

req.write(payload);
req.end();
