'use strict';
const fs = require('fs');

// Persistent JSON storage helpers with atomic writes (temp file + rename).

function loadJson(file, defaultVal = {}) {
  try {
    if (fs.existsSync(file)) {
      return JSON.parse(fs.readFileSync(file, 'utf8'));
    }
  } catch (err) {
    console.error(`Error reading ${file}:`, err);
  }
  return defaultVal;
}

function saveJson(file, data) {
  try {
    const tmpFile = `${file}.${Date.now()}.${Math.random().toString(36).substring(2, 7)}.tmp`;
    fs.writeFileSync(tmpFile, JSON.stringify(data, null, 2), 'utf8');
    fs.renameSync(tmpFile, file);
  } catch (err) {
    console.error(`Error writing ${file}:`, err);
  }
}

module.exports = { loadJson, saveJson };
