#!/usr/bin/env node
/**
 * PASA Sentinel — VPS Security Audit Script
 * Run: node scripts/security-check.js
 *
 * Checks for common misconfigurations that could compromise user privacy or
 * allow unauthorized access to the C2 control plane.
 */

'use strict';

const fs   = require('fs');
const path = require('path');
const crypto = require('crypto');

const ROOT = path.join(__dirname, '..');
const ENV_PATH = path.join(ROOT, '.env');

// ── Load .env ─────────────────────────────────────────────────────────────────
const env = {};
if (fs.existsSync(ENV_PATH)) {
  const lines = fs.readFileSync(ENV_PATH, 'utf8').split('\n');
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const idx = trimmed.indexOf('=');
    if (idx !== -1) {
      const k = trimmed.slice(0, idx).trim();
      let v = trimmed.slice(idx + 1).trim();
      if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) v = v.slice(1, -1);
      env[k] = v;
    }
  }
}

const results = { pass: [], warn: [], critical: [] };

function pass(msg)     { results.pass.push(msg); }
function warn(msg)     { results.warn.push(msg); }
function critical(msg) { results.critical.push(msg); }

// ── 1. Known-compromised token check ─────────────────────────────────────────
const KNOWN_COMPROMISED_HASH = '13b35520eec9a79fa4f686caad9dc2068e82a93946fe9f33d7b00329598ef01f';
const tokensToCheck = ['BOT_TOKEN', 'PASA_CENTRAL_BOT_TOKEN', 'ADMIN_BOT_TOKEN'];
let tokenFound = false;
for (const key of tokensToCheck) {
  if (env[key]) {
    const hash = crypto.createHash('sha256').update(env[key]).digest('hex');
    if (hash === KNOWN_COMPROMISED_HASH) {
      critical(`${key} matches a known-compromised token. Rotate in @BotFather NOW.`);
      tokenFound = true;
    }
  }
}
if (!tokenFound) pass('No known-compromised bot tokens detected.');

// ── 2. ADMIN_CHAT_ID ──────────────────────────────────────────────────────────
const adminChatId = env['ADMIN_CHAT_ID'] || '';
if (!adminChatId || adminChatId === '0' || adminChatId.includes('YOUR_')) {
  critical('ADMIN_CHAT_ID is not set. Any user who finds your bot token can issue commands.');
} else if (!/^\d+$/.test(adminChatId)) {
  warn('ADMIN_CHAT_ID does not look like a numeric Telegram user ID.');
} else {
  pass(`ADMIN_CHAT_ID is set (${adminChatId.slice(0, 3)}***).`);
}

// ── 3. BOT_TOKEN presence ─────────────────────────────────────────────────────
const botToken = env['BOT_TOKEN'] || '';
if (!botToken || botToken.includes('YOUR_')) {
  critical('BOT_TOKEN is not configured.');
} else if (!/^\d+:[\w-]{35,}$/.test(botToken)) {
  warn('BOT_TOKEN format looks unusual. Expected format: 123456789:ABC-DEF...');
} else {
  pass('BOT_TOKEN is present and format looks valid.');
}

// ── 4. ADMIN_KEY strength ─────────────────────────────────────────────────────
const adminKey = env['ADMIN_KEY'] || '';
if (!adminKey || adminKey.includes('REPLACE_')) {
  critical('ADMIN_KEY is not set. Admin API endpoints are unprotected.');
} else if (adminKey.length < 32) {
  warn(`ADMIN_KEY is only ${adminKey.length} chars. Minimum recommended: 32 random bytes (64 hex chars).`);
} else {
  pass(`ADMIN_KEY is set (${adminKey.length} chars — ${adminKey.length >= 64 ? 'strong' : 'acceptable'}).`);
}

// ── 5. .env file permissions (unix only) ─────────────────────────────────────
try {
  const stat = fs.statSync(ENV_PATH);
  const mode = stat.mode & 0o777;
  if (process.platform !== 'win32') {
    if ((mode & 0o044) !== 0) {
      warn(`.env file is group/world readable (mode: 0${mode.toString(8)}). Run: chmod 600 .env`);
    } else {
      pass(`.env file permissions are secure (mode: 0${mode.toString(8)}).`);
    }
  } else {
    pass('.env file exists (Windows — manual permission check recommended).');
  }
} catch (_) {
  warn('.env file not found. Copy .env.example to .env and configure it.');
}

// ── 6. Zero-storage compliance: evidence directory ───────────────────────────
const evidenceDir = path.join(ROOT, 'uploads', 'evidence');
if (fs.existsSync(evidenceDir)) {
  const files = fs.readdirSync(evidenceDir).filter(f => !f.startsWith('.'));
  if (files.length > 0) {
    critical(`${files.length} file(s) in uploads/evidence — zero-storage violation! Media must not persist on server disk.`);
    console.log('  Files:', files.slice(0, 5).join(', ') + (files.length > 5 ? '...' : ''));
  } else {
    pass('uploads/evidence directory is empty — zero-storage policy intact.');
  }
} else {
  pass('uploads/evidence directory does not exist yet — zero-storage policy intact.');
}

// ── 7. Signing key on server disk ─────────────────────────────────────────────
const jksPath = path.join(ROOT, '..', 'pasa-release-key.jks');
if (fs.existsSync(jksPath)) {
  warn('pasa-release-key.jks found on this machine. Signing keys should be stored on an air-gapped device, not the VPS.');
} else {
  pass('Release signing key (pasa-release-key.jks) is NOT present on this VPS — good.');
}

// ── 8. CORS origin is configured ─────────────────────────────────────────────
const origin = env['ALLOWED_ORIGIN'] || '';
if (!origin) {
  warn('ALLOWED_ORIGIN is not set. All cross-origin browser access to the API is blocked (safe default, but verify this is intentional).');
} else {
  pass(`ALLOWED_ORIGIN is set to: ${origin}`);
}

// ── Print Report ──────────────────────────────────────────────────────────────
const line = '='.repeat(65);
console.log('\n' + line);
console.log(' PASA Sentinel — Security Audit Report');
console.log(line);

if (results.critical.length > 0) {
  console.log('\n🔴 CRITICAL ISSUES (fix immediately):');
  results.critical.forEach(m => console.log(`   ✗ ${m}`));
}

if (results.warn.length > 0) {
  console.log('\n🟡 WARNINGS (should fix):');
  results.warn.forEach(m => console.log(`   ⚠ ${m}`));
}

if (results.pass.length > 0) {
  console.log('\n🟢 PASSED:');
  results.pass.forEach(m => console.log(`   ✓ ${m}`));
}

console.log('\n' + line);
const score = results.pass.length;
const total = score + results.warn.length + results.critical.length;
console.log(` Score: ${score}/${total} checks passed`);
if (results.critical.length > 0) {
  console.log(' Status: ❌ CRITICAL — address issues above before deploying to users');
} else if (results.warn.length > 0) {
  console.log(' Status: ⚠️  WARNINGS — review and resolve before production');
} else {
  console.log(' Status: ✅ SECURE — all checks passed');
}
console.log(line + '\n');

process.exit(results.critical.length > 0 ? 2 : results.warn.length > 0 ? 1 : 0);
