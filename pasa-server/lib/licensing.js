'use strict';
const crypto = require('crypto');

/**
 * Commercial licensing & subscription engine.
 *
 * Self-contained module: it owns the in-memory `licenses` map and persists it
 * through the injected storage helpers. Device coupling is injected via
 * getDevice()/persistDevices() so this module never imports server state directly.
 *
 * @param {object} deps
 * @param {string} deps.licensesFile   Path to licenses.json
 * @param {function} deps.loadJson     (file, default) => data
 * @param {function} deps.saveJson     (file, data) => void
 * @param {function} deps.logSecurityEvent (type, details) => void
 * @param {function} deps.getDevice    (deviceId) => device | undefined
 * @param {function} deps.persistDevices () => void   (persists the devices map)
 */
function createLicensing({ licensesFile, ed25519KeyFile, loadJson, saveJson, logSecurityEvent, getDevice, persistDevices }) {
  let licenses = loadJson(licensesFile, {});
  let ed25519Key = ed25519KeyFile ? loadJson(ed25519KeyFile, null) : null;

  function signDeviceCertificate(deviceHash, tier, _ignoredExpiresAt, licenseKey) {
    if (!ed25519Key || !ed25519Key.privPem) return null;
    const certExpiresAt = Date.now() + (7 * 24 * 60 * 60 * 1000); // Always 7 days
    try {
      const payloadObj = {
        deviceHash: String(deviceHash).trim(),
        tier: String(tier).trim().toUpperCase(),
        expiresAt: certExpiresAt,
        issuedAt: Date.now(),
        key: licenseKey ? String(licenseKey).trim().toUpperCase() : ''
      };
      const payloadStr = Buffer.from(JSON.stringify(payloadObj), 'utf8').toString('base64');
      const privateKey = crypto.createPrivateKey(ed25519Key.privPem);
      const sig = crypto.sign(null, Buffer.from(payloadStr, 'utf8'), privateKey);
      return {
        payload: payloadStr,
        signature: sig.toString('hex')
      };
    } catch (err) {
      console.error('[Ed25519 Sign Error]:', err.message);
      return null;
    }
  }

  function generateLicenseKey(tier = 'PRO') {
    const cleanTier = tier.toUpperCase().includes('LIFE') ? 'LIFE' : 'PRO';
    const seg1 = crypto.randomBytes(2).toString('hex').toUpperCase();
    const seg2 = crypto.randomBytes(2).toString('hex').toUpperCase();
    const seg3 = crypto.randomBytes(2).toString('hex').toUpperCase();
    return `PASA-${cleanTier}-${seg1}-${seg2}-${seg3}`;
  }

  function createLicense(email, tier = 'PRO_ANNUAL', maxDevices = 1, meta = {}) {
    const key = generateLicenseKey(tier);
    const now = Date.now();
    let expiresAt = null;
    if (tier === 'TRIAL') {
      expiresAt = now + 7 * 24 * 60 * 60 * 1000; // 7 days
    } else if (tier === 'PRO_ANNUAL') {
      expiresAt = now + 365 * 24 * 60 * 60 * 1000; // 1 year
    } else if (tier === 'PRO_LIFETIME' || tier === 'PRO_ENTERPRISE') {
      expiresAt = now + 100 * 365 * 24 * 60 * 60 * 1000; // 100 years
    }

    licenses[key] = {
      key,
      email: (email || '').trim().toLowerCase(),
      tier,
      maxDevices: Math.max(parseInt(maxDevices, 10) || 1, 1),
      activatedDevices: [],
      createdAt: now,
      expiresAt,
      status: 'ACTIVE',
      paymentMethod: meta.paymentMethod || 'BINANCE_PAY',
      binancePayId: meta.binancePayId || '756303714',
      binanceTxId: meta.binanceTxId || '',
      nickname: 'RBR48'
    };
    saveJson(licensesFile, licenses);
    logSecurityEvent('LICENSE_CREATED', { key, email, tier, maxDevices, paymentMethod: 'BINANCE_PAY', binanceTxId: meta.binanceTxId });
    return licenses[key];
  }

  function activateLicense(key, deviceHash) {
    if (!key || typeof key !== 'string') return { ok: false, message: 'Invalid license key format' };
    const cleanKey = key.trim().toUpperCase();
    const lic = licenses[cleanKey];
    if (!lic) return { ok: false, message: 'License key not found. Please check your key or buy one at https://pasa.izhaanintellect.fun/#pricing' };
    if (lic.status !== 'ACTIVE') return { ok: false, message: `License is ${lic.status}` };
    if (lic.revoked === 1) return { ok: false, message: 'License has been revoked. Contact support.' };
    if (lic.expiresAt && Date.now() > lic.expiresAt) {
      lic.status = 'EXPIRED';
      saveJson(licensesFile, licenses);
      return { ok: false, message: 'License key has expired' };
    }

    // Check if this deviceHash already activated — re-issue certificate
    if (!lic.activatedDeviceHashes) lic.activatedDeviceHashes = [];
    if (lic.activatedDeviceHashes.includes(deviceHash)) {
      const certExpiresAt = Date.now() + (7 * 24 * 60 * 60 * 1000);
      const cert = signDeviceCertificate(deviceHash, lic.tier, certExpiresAt, cleanKey);
      const daysLeft = lic.expiresAt ? Math.max(0, Math.ceil((lic.expiresAt - Date.now()) / (24 * 60 * 60 * 1000))) : 99999;
      return { ok: true, message: 'License re-confirmed', tier: lic.tier,
        daysLeft, expiresAt: lic.expiresAt, certificate: cert };
    }

    // Check max device limit
    if (lic.activatedDeviceHashes.length >= (lic.maxDevices || 1)) {
      return { ok: false, message: `License already activated on maximum ${lic.maxDevices || 1} device(s). Contact support to transfer.` };
    }

    // Activate: add deviceHash (no PII stored, no device object touched)
    lic.activatedDeviceHashes.push(deviceHash);
    saveJson(licensesFile, licenses);

    logSecurityEvent('LICENSE_ACTIVATED', { key: cleanKey, deviceHashPrefix: deviceHash.substring(0, 12) + '...', tier: lic.tier });
    const daysLeft = lic.expiresAt ? Math.max(0, Math.ceil((lic.expiresAt - Date.now()) / (24 * 60 * 60 * 1000))) : 99999;
    const certExpiresAt = Date.now() + (7 * 24 * 60 * 60 * 1000);
    const cert = signDeviceCertificate(deviceHash, lic.tier, certExpiresAt, cleanKey);
    return {
      ok: true,
      message: `License activated successfully (${lic.tier})`,
      tier: lic.tier,
      daysLeft,
      expiresAt: lic.expiresAt,
      certificate: cert
    };
  }

  function getDeviceLicenseStatus(deviceHashOrId) {
    const now = Date.now();
    let foundLic = null;

    // 1. Scan all licenses for this deviceHash or deviceId
    for (const key of Object.keys(licenses)) {
      const lic = licenses[key];
      if (lic) {
        if (Array.isArray(lic.activatedDeviceHashes) && lic.activatedDeviceHashes.includes(deviceHashOrId)) {
          foundLic = lic;
          break;
        }
        if (Array.isArray(lic.activatedDevices) && lic.activatedDevices.includes(deviceHashOrId)) {
          foundLic = lic;
          break;
        }
      }
    }

    if (foundLic) {
      // Revocation check
      if (foundLic.revoked === 1 || foundLic.status === 'REVOKED') {
        return {
          hasPro: false, tier: 'REVOKED', status: 'REVOKED',
          isTrial: false, daysLeft: 0, certificate: null
        };
      }
      // Expiry check
      if (foundLic.expiresAt && now > foundLic.expiresAt) {
        return {
          hasPro: false, tier: foundLic.tier, status: 'EXPIRED',
          isTrial: false, daysLeft: 0, certificate: null
        };
      }
      // Active — issue fresh 7-day certificate
      const certExpiresAt = Date.now() + (7 * 24 * 60 * 60 * 1000);
      const cert = signDeviceCertificate(deviceHashOrId, foundLic.tier, certExpiresAt, foundLic.key);
      const daysLeft = foundLic.expiresAt ? Math.max(0, Math.ceil((foundLic.expiresAt - now) / (24 * 60 * 60 * 1000))) : 99999;
      return {
        hasPro: true,
        tier: foundLic.tier,
        status: 'ACTIVE',
        isTrial: false,
        daysLeft,
        expiresAt: foundLic.expiresAt,
        licenseKey: foundLic.key,
        certificate: cert
      };
    }

    // 2. Calculate remaining 7-day evaluation period
    const dev = typeof getDevice === 'function' ? getDevice(deviceHashOrId) : null;
    const registeredAt = (dev && dev.registeredAt) || (dev && dev.createdAt) || null;

    if (registeredAt) {
      const trialDuration = 7 * 24 * 60 * 60 * 1000;
      const trialExpiresAt = registeredAt + trialDuration;
      const msLeft = trialExpiresAt - now;

      if (now < trialExpiresAt) {
        const days = Math.floor(msLeft / (24 * 60 * 60 * 1000));
        const hours = Math.floor((msLeft % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000));
        const minutes = Math.floor((msLeft % (60 * 60 * 1000)) / (60 * 1000));
        const certExpiresAt = Math.min(now + (7 * 24 * 60 * 60 * 1000), trialExpiresAt);
        const cert = signDeviceCertificate(deviceHashOrId, 'FREE_TRIAL', certExpiresAt, null);

        return {
          hasPro: true,
          tier: 'FREE_TRIAL',
          status: 'TRIAL',
          isTrial: true,
          daysLeft: days,
          hoursLeft: Math.max(0, hours),
          minutesLeft: Math.max(0, minutes),
          msLeft,
          expiresAt: trialExpiresAt,
          licenseKey: null,
          certificate: cert
        };
      } else {
        return {
          hasPro: false,
          tier: 'EXPIRED_TRIAL',
          status: 'EXPIRED',
          isTrial: true,
          daysLeft: 0,
          hoursLeft: 0,
          minutesLeft: 0,
          msLeft: 0,
          expiresAt: trialExpiresAt,
          licenseKey: null,
          certificate: null
        };
      }
    }

    // Fallback: active 7-day evaluation
    return {
      hasPro: true,
      tier: 'FREE_TRIAL',
      status: 'TRIAL',
      isTrial: true,
      daysLeft: 7,
      hoursLeft: 0,
      minutesLeft: 0,
      msLeft: 7 * 24 * 60 * 60 * 1000,
      expiresAt: now + (7 * 24 * 60 * 60 * 1000),
      licenseKey: null,
      certificate: null
    };
  }

  function getTrialBanner(deviceId) {
    if (!deviceId) return null;
    const lic = getDeviceLicenseStatus(deviceId);
    if (!lic || !lic.isTrial) {
      return null;
    }
    if (lic.status === 'TRIAL' || lic.status === 'ACTIVE') {
      const days = lic.daysLeft != null ? lic.daysLeft : 7;
      const hours = lic.hoursLeft != null ? lic.hoursLeft : 0;
      if (days > 1) {
        return `🔴 <b>TRIAL: ${days} days${hours > 0 ? `, ${hours} hours` : ''} remaining — Unlock Lifetime Shield: /license</b>`;
      } else if (days === 1) {
        return `🔴 <b>TRIAL: 1 day${hours > 0 ? `, ${hours} hours` : ''} remaining — Unlock Lifetime Shield: /license</b>`;
      } else if (hours > 0) {
        return `🔴 <b>TRIAL EXPIRING: ${hours}h ${lic.minutesLeft || 0}m remaining — Unlock Lifetime Shield: /license</b>`;
      } else {
        return `🔴 <b>TRIAL: Active (${days}d remaining) — Unlock Lifetime Shield: /license</b>`;
      }
    }
    if (lic.status === 'EXPIRED') {
      return `🛑 <b>TRIAL EXPIRED: All security features locked. Activate: /license</b>`;
    }
    return null;
  }

  function lookupLicense(query) {
    if (!query || typeof query !== 'string') return null;
    const q = query.trim().toLowerCase();
    for (const lic of Object.values(licenses)) {
      if (lic.key.toLowerCase() === q || (lic.email && lic.email.toLowerCase() === q)) {
        const daysLeft = lic.expiresAt ? Math.max(0, Math.ceil((lic.expiresAt - Date.now()) / (24 * 60 * 60 * 1000))) : 99999;
        return {
          key: lic.key,
          email: lic.email,
          tier: lic.tier,
          maxDevices: lic.maxDevices,
          activatedCount: lic.activatedDevices ? lic.activatedDevices.length : 0,
          status: lic.status,
          daysLeft,
          expiresAt: lic.expiresAt,
          createdAt: lic.createdAt,
          paymentMethod: lic.paymentMethod || 'BINANCE_PAY',
          binancePayId: lic.binancePayId || '756303714',
          binanceTxId: lic.binanceTxId || '',
          nickname: lic.nickname || 'RBR48'
        };
      }
    }
    return null;
  }

  // Returns all licenses (for the admin listing endpoint).
  function listLicenses() {
    return Object.values(licenses);
  }

  function revokeLicense(key, reason = 'REVOKED') {
    if (!key || typeof key !== 'string') return false;
    const cleanKey = key.trim().toUpperCase();
    const lic = licenses[cleanKey];
    if (lic) {
      lic.status = 'REVOKED';
      lic.revoked = 1;
      lic.revokeReason = reason;
      saveJson(licensesFile, licenses);
      logSecurityEvent('LICENSE_REVOKED', { key: cleanKey, reason });
      return true;
    }
    return false;
  }

  return {
    generateLicenseKey,
    createLicense,
    activateLicense,
    revokeLicense,
    getDeviceLicenseStatus,
    getTrialBanner,
    lookupLicense,
    listLicenses
  };
}

module.exports = { createLicensing };
