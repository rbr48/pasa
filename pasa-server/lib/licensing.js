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

  function signDeviceCertificate(deviceId, tier, expiresAt, licenseKey) {
    if (!ed25519Key || !ed25519Key.privPem) return null;
    try {
      const payloadObj = {
        deviceId: String(deviceId).trim(),
        tier: String(tier).trim().toUpperCase(),
        expiresAt: Number(expiresAt) || 0,
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

  function activateLicense(key, deviceId) {
    if (!key || typeof key !== 'string') return { ok: false, message: 'Invalid license key format' };
    const cleanKey = key.trim().toUpperCase();
    const lic = licenses[cleanKey];
    if (!lic) return { ok: false, message: 'License key not found. Please check your key or buy one at https://pasa.izhaanintellect.fun/#pricing' };
    if (lic.status !== 'ACTIVE') return { ok: false, message: `License is ${lic.status}` };
    if (lic.expiresAt && Date.now() > lic.expiresAt) {
      lic.status = 'EXPIRED';
      saveJson(licensesFile, licenses);
      return { ok: false, message: 'License key has expired' };
    }

    if (!lic.activatedDevices.includes(deviceId)) {
      if (lic.activatedDevices.length >= lic.maxDevices) {
        return {
          ok: false,
          message: `Device limit reached (${lic.maxDevices} device${lic.maxDevices > 1 ? 's' : ''} already bound)`
        };
      }
      lic.activatedDevices.push(deviceId);
      saveJson(licensesFile, licenses);
    }

    const dev = getDevice(deviceId);
    if (dev) {
      dev.licenseKey = cleanKey;
      dev.licenseTier = lic.tier;
      dev.licenseExpiresAt = lic.expiresAt;
      persistDevices();
    }

    logSecurityEvent('LICENSE_ACTIVATED', { key: cleanKey, deviceId, tier: lic.tier });
    const daysLeft = lic.expiresAt ? Math.max(0, Math.ceil((lic.expiresAt - Date.now()) / (24 * 60 * 60 * 1000))) : 99999;
    const cert = signDeviceCertificate(deviceId, lic.tier, lic.expiresAt, cleanKey);
    return {
      ok: true,
      message: `License activated successfully (${lic.tier})`,
      tier: lic.tier,
      daysLeft,
      expiresAt: lic.expiresAt,
      certificate: cert
    };
  }

  function getDeviceLicenseStatus(deviceId) {
    const dev = getDevice(deviceId);
    const now = Date.now();

    // 1. Check bound active license
    let activeLic = null;
    if (dev && dev.licenseKey && licenses[dev.licenseKey]) {
      activeLic = licenses[dev.licenseKey];
    } else {
      // Fallback: Check if this deviceId is present in activatedDevices of any active license
      for (const key of Object.keys(licenses)) {
        const lic = licenses[key];
        if (lic && Array.isArray(lic.activatedDevices) && lic.activatedDevices.includes(deviceId)) {
          activeLic = lic;
          if (dev) {
            dev.licenseKey = lic.key;
            dev.licenseTier = lic.tier;
            dev.licenseExpiresAt = lic.expiresAt;
            persistDevices();
          }
          break;
        }
      }
    }

    if (activeLic && activeLic.status === 'ACTIVE' && (!activeLic.expiresAt || activeLic.expiresAt > now)) {
      const daysLeft = activeLic.expiresAt ? Math.max(0, Math.ceil((activeLic.expiresAt - now) / (24 * 60 * 60 * 1000))) : 99999;
      const cert = signDeviceCertificate(deviceId, activeLic.tier, activeLic.expiresAt, activeLic.key);
      return {
        hasPro: true,
        tier: activeLic.tier,
        status: 'ACTIVE',
        isTrial: false,
        daysLeft,
        expiresAt: activeLic.expiresAt,
        licenseKey: activeLic.key,
        certificate: cert
      };
    }

    // 2. Default 7-day trial from registration time
    const registeredAt = (dev && dev.registeredAt) || now;
    const trialDuration = 7 * 24 * 60 * 60 * 1000;
    const trialExpiresAt = registeredAt + trialDuration;
    const msLeft = trialExpiresAt - now;
    const trialDaysLeft = Math.max(0, Math.ceil(msLeft / (24 * 60 * 60 * 1000)));

    if (now < trialExpiresAt) {
      const trialCert = signDeviceCertificate(deviceId, 'FREE_TRIAL', trialExpiresAt, null);
      const days = Math.floor(msLeft / (24 * 60 * 60 * 1000));
      const hours = Math.floor((msLeft % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000));
      const minutes = Math.floor((msLeft % (60 * 60 * 1000)) / (60 * 1000));
      return {
        hasPro: true,
        tier: 'FREE_TRIAL',
        status: 'TRIAL',
        isTrial: true,
        daysLeft: days,
        hoursLeft: hours,
        minutesLeft: minutes,
        msLeft: msLeft,
        expiresAt: trialExpiresAt,
        licenseKey: null,
        certificate: trialCert
      };
    }

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

  function getTrialBanner(deviceId) {
    if (!deviceId) return null;
    const lic = getDeviceLicenseStatus(deviceId);
    if (!lic || !lic.isTrial) {
      return null;
    }
    if (lic.status === 'TRIAL') {
      if (lic.daysLeft > 1) {
        return `🔴 <b>TRIAL: ${lic.daysLeft} days, ${lic.hoursLeft} hours remaining — Unlock Lifetime Shield: /license</b>`;
      } else if (lic.daysLeft === 1) {
        return `🔴 <b>TRIAL: 1 day, ${lic.hoursLeft} hours remaining — Unlock Lifetime Shield: /license</b>`;
      } else {
        return `🔴 <b>TRIAL EXPIRING: ${lic.hoursLeft}h ${lic.minutesLeft}m remaining — Unlock Lifetime Shield: /license</b>`;
      }
    }
    // Expired trial
    return `🛑 <b>TRIAL EXPIRED: All security features locked. Activate: /license</b>`;
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
