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
function createLicensing({ licensesFile, loadJson, saveJson, logSecurityEvent, getDevice, persistDevices }) {
  let licenses = loadJson(licensesFile, {});

  function generateLicenseKey(tier = 'PRO') {
    const cleanTier = tier.toUpperCase().includes('LIFE') ? 'LIFE' : 'PRO';
    const seg1 = crypto.randomBytes(2).toString('hex').toUpperCase();
    const seg2 = crypto.randomBytes(2).toString('hex').toUpperCase();
    const seg3 = crypto.randomBytes(2).toString('hex').toUpperCase();
    return `PASA-${cleanTier}-${seg1}-${seg2}-${seg3}`;
  }

  function createLicense(email, tier = 'PRO_ANNUAL', maxDevices = 1) {
    const key = generateLicenseKey(tier);
    const now = Date.now();
    let expiresAt = null;
    if (tier === 'TRIAL') {
      expiresAt = now + 7 * 24 * 60 * 60 * 1000; // 7 days
    } else if (tier === 'PRO_ANNUAL') {
      expiresAt = now + 365 * 24 * 60 * 60 * 1000; // 1 year
    } else if (tier === 'PRO_LIFETIME') {
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
      status: 'ACTIVE'
    };
    saveJson(licensesFile, licenses);
    logSecurityEvent('LICENSE_CREATED', { key, email, tier, maxDevices });
    return licenses[key];
  }

  function activateLicense(key, deviceId) {
    if (!key || typeof key !== 'string') return { ok: false, message: 'Invalid license key format' };
    const cleanKey = key.trim().toUpperCase();
    const lic = licenses[cleanKey];
    if (!lic) return { ok: false, message: 'License key not found. Please check your key or buy one at https://izhaanintellect.fun/pasa/' };
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
    return {
      ok: true,
      message: `License activated successfully (${lic.tier})`,
      tier: lic.tier,
      daysLeft,
      expiresAt: lic.expiresAt
    };
  }

  function getDeviceLicenseStatus(deviceId) {
    const dev = getDevice(deviceId);
    const now = Date.now();

    // 1. Check bound active license
    if (dev && dev.licenseKey && licenses[dev.licenseKey]) {
      const lic = licenses[dev.licenseKey];
      if (lic.status === 'ACTIVE' && (!lic.expiresAt || lic.expiresAt > now)) {
        const daysLeft = lic.expiresAt ? Math.max(0, Math.ceil((lic.expiresAt - now) / (24 * 60 * 60 * 1000))) : 99999;
        return {
          hasPro: true,
          tier: lic.tier,
          status: 'ACTIVE',
          isTrial: false,
          daysLeft,
          expiresAt: lic.expiresAt,
          licenseKey: lic.key
        };
      }
    }

    // 2. Default 7-day trial from registration time
    const registeredAt = (dev && dev.registeredAt) || now;
    const trialDuration = 7 * 24 * 60 * 60 * 1000;
    const trialExpiresAt = registeredAt + trialDuration;
    const trialDaysLeft = Math.max(0, Math.ceil((trialExpiresAt - now) / (24 * 60 * 60 * 1000)));

    if (now < trialExpiresAt) {
      return {
        hasPro: true,
        tier: 'FREE_TRIAL',
        status: 'TRIAL',
        isTrial: true,
        daysLeft: trialDaysLeft,
        expiresAt: trialExpiresAt,
        licenseKey: null
      };
    }

    return {
      hasPro: false,
      tier: 'EXPIRED_TRIAL',
      status: 'EXPIRED',
      isTrial: true,
      daysLeft: 0,
      expiresAt: trialExpiresAt,
      licenseKey: null
    };
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
          activatedCount: lic.activatedDevices.length,
          status: lic.status,
          daysLeft,
          expiresAt: lic.expiresAt,
          createdAt: lic.createdAt
        };
      }
    }
    return null;
  }

  // Returns all licenses (for the admin listing endpoint).
  function listLicenses() {
    return Object.values(licenses);
  }

  return {
    generateLicenseKey,
    createLicense,
    activateLicense,
    getDeviceLicenseStatus,
    lookupLicense,
    listLicenses
  };
}

module.exports = { createLicensing };
