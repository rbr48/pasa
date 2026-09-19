/**
 * PASA Sentinel — Commercial Cybersecurity Landing Page Generator
 * Ultra-Modern 2026 Sovereign Mobile Defense Landing Page
 */

const fs = require('fs');
const path = require('path');

function renderCommercialLandingPage({ latestRelease, totalDevices, activePollersCount }) {
  // 1. VPS Production path
  const vpsOptPath = '/opt/pasa-commercial-web/public/index.html';
  if (fs.existsSync(vpsOptPath)) {
    return fs.readFileSync(vpsOptPath, 'utf8');
  }

  // 2. Local development sibling path
  const htmlPath = path.join(__dirname, '..', 'pasa-commercial-web', 'public', 'index.html');
  if (fs.existsSync(htmlPath)) {
    return fs.readFileSync(htmlPath, 'utf8');
  }

  // Self-contained fallback in case commercial web directory is not present
  const localHtmlPath = path.join(__dirname, 'public', 'index.html');
  if (fs.existsSync(localHtmlPath)) {
    return fs.readFileSync(localHtmlPath, 'utf8');
  }

  return `<!DOCTYPE html><html><head><title>PASA Sentinel</title></head><body><h1>PASA Sentinel Control Plane Online</h1></body></html>`;
}

module.exports = { renderCommercialLandingPage };
