#!/usr/bin/env node
'use strict';

/**
 * 🛡️ PASA Sentinel — Air-Gapped SMS License Generator
 *
 * Generates compact 160-character Ed25519 digital signature payloads for
 * renewing licenses on air-gapped devices operating without internet or mobile data.
 *
 * Usage:
 *   node generate_sms_license.js <deviceIdPrefix> [tier] [durationDays]
 *
 * Example:
 *   node generate_sms_license.js pasa_6d2dc77f PRO_LIFETIME 365
 *   node generate_sms_license.js * PRO 30
 */

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const keyPath = path.join(__dirname, '..', 'data', 'license_ed25519_key.json');
if (!fs.existsSync(keyPath)) {
  console.error('❌ License Ed25519 key not found at:', keyPath);
  process.exit(1);
}

const keyData = JSON.parse(fs.readFileSync(keyPath, 'utf8'));
if (!keyData.privPem) {
  console.error('❌ Private key PEM missing in keyfile.');
  process.exit(1);
}

const privateKey = crypto.createPrivateKey(keyData.privPem);

const deviceId = (process.argv[2] || '*').trim();
const tier = (process.argv[3] || 'PRO_LIFETIME').trim().toUpperCase();
const days = parseInt(process.argv[4] || '365', 10);
const expiresSec = Math.floor(Date.now() / 1000) + (days * 86400);

const dataStr = `${deviceId}:${tier}:${expiresSec}`;
const sig = crypto.sign(null, Buffer.from(dataStr, 'utf8'), privateKey);
const sigBase64 = sig.toString('base64url');

const smsPayload = `PASA LIC ${dataStr}|${sigBase64}`;

console.log('\n================================================================');
console.log('🛡️  PASA SENTINEL — AIR-GAPPED ED25519 SMS LICENSE PAYLOAD');
console.log('================================================================');
console.log(`Target Device:   ${deviceId}`);
console.log(`License Tier:    ${tier}`);
console.log(`Valid Duration:  ${days} days (Expires: ${new Date(expiresSec * 1000).toISOString().split('T')[0]})`);
console.log(`Payload Length:  ${smsPayload.length} chars (GSM 7-bit single SMS limit: 160)`);
console.log('----------------------------------------------------------------');
console.log('📱 SEND VIA SMS TO TARGET PHONE:');
console.log(`\n${smsPayload}\n`);
console.log('================================================================\n');
