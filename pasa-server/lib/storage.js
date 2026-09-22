'use strict';
const fs = require('fs');

// Persistent JSON storage helpers with atomic writes (temp file + rename).

function loadJson(file, defaultVal = {}) {
  try {
    if (fs.existsSync(file)) {
      const content = fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
      return JSON.parse(content);
    }
  } catch (err) {
    console.error(`Error reading ${file}:`, err);
  }
  return defaultVal;
}

function saveJson(file, data) {
  const tmpFile = `${file}.${Date.now()}.${Math.random().toString(36).substring(2, 7)}.tmp`;
  try {
    fs.writeFileSync(tmpFile, JSON.stringify(data, null, 2), 'utf8');
    try {
      fs.renameSync(tmpFile, file);
    } catch (renameErr) {
      // On Windows, renameSync can fail with EPERM/EBUSY if destination exists or is locked.
      // Fallback to copy and unlink.
      fs.copyFileSync(tmpFile, file);
      try { fs.unlinkSync(tmpFile); } catch (_) {}
    }
  } catch (err) {
    console.error(`Error writing ${file}:`, err);
    try { if (fs.existsSync(tmpFile)) fs.unlinkSync(tmpFile); } catch (_) {}
  }
}

module.exports = { loadJson, saveJson };
