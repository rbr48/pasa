/**
 * PASA Sentinel — Commercial Cybersecurity Landing Page Generator
 * High-converting, responsive, dark-mode cybersecurity product showcase
 */

function renderCommercialLandingPage({ latestRelease, totalDevices, activePollersCount }) {
  const version = latestRelease ? latestRelease.versionName : '2.3.0';
  const versionCode = latestRelease ? latestRelease.versionCode : 4;
  const downloadFilename = latestRelease ? latestRelease.filename : 'pasa-v2.3.0-4.apk';
  const sha256 = latestRelease ? latestRelease.sha256 : 'e8668348501a90aeba497fd4dbf6cf3b154c18ee478b6df58e0c42c6cd080dee';
  const fileSizeMb = latestRelease ? (latestRelease.fileSize / (1024 * 1024)).toFixed(1) : '18.0';

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>PASA Sentinel — The Sovereign Mobile Anti-Theft & Stealth Defense System</title>
  <meta name="description" content="PASA is an enterprise-grade, sovereign Android security agent. Silent Telegram C2, Dual-Channel Offline SMS, Autonomous Anti-Snatch Traps, Fake Shutdown, and Ed25519 Encryption.">
  <meta property="og:title" content="PASA Sentinel — Sovereign Android Anti-Theft & Defense">
  <meta property="og:description" content="Dual-channel Telegram & Offline SMS remote control. Capture stealth photos, track GPS without internet, and freeze intruders.">
  <meta property="og:type" content="website">
  <meta property="og:url" content="https://izhaanintellect.fun/pasa/">
  
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  
  <style>
    :root {
      --bg: #060913;
      --bg-alt: #0a0f1d;
      --card-bg: rgba(14, 21, 37, 0.75);
      --card-border: rgba(30, 58, 110, 0.45);
      --card-border-glow: rgba(0, 212, 255, 0.35);
      --primary: #00d4ff;
      --primary-hover: #38bdf8;
      --primary-glow: rgba(0, 212, 255, 0.22);
      --emerald: #10b981;
      --emerald-glow: rgba(16, 185, 129, 0.2);
      --amber: #f59e0b;
      --rose: #f43f5e;
      --purple: #8b5cf6;
      --text: #f1f5f9;
      --text-muted: #94a3b8;
      --text-dim: #64748b;
      --font-sans: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
      --font-mono: 'JetBrains Mono', monospace;
    }

    * { box-sizing: border-box; margin: 0; padding: 0; }
    html { scroll-behavior: smooth; }

    body {
      background: var(--bg);
      background-image: 
        radial-gradient(at 0% 0%, rgba(0, 212, 255, 0.08) 0px, transparent 45%),
        radial-gradient(at 100% 40%, rgba(139, 92, 246, 0.07) 0px, transparent 50%),
        radial-gradient(at 50% 100%, rgba(16, 185, 129, 0.06) 0px, transparent 50%);
      color: var(--text);
      font-family: var(--font-sans);
      min-height: 100vh;
      line-height: 1.6;
      overflow-x: hidden;
    }

    .container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 24px;
    }

    /* Navigation */
    header {
      position: sticky;
      top: 0;
      z-index: 100;
      backdrop-filter: blur(16px);
      background: rgba(6, 9, 19, 0.85);
      border-bottom: 1px solid var(--card-border);
      padding: 16px 0;
    }
    .nav-inner {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
      text-decoration: none;
      color: var(--text);
    }
    .brand-icon {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      background: linear-gradient(135deg, #00d4ff, #0284c7);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      box-shadow: 0 0 16px var(--primary-glow);
    }
    .brand-name {
      font-size: 20px;
      font-weight: 800;
      letter-spacing: -0.5px;
      background: linear-gradient(135deg, #ffffff, #94a3b8);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .brand-badge {
      font-size: 10px;
      font-family: var(--font-mono);
      background: rgba(0, 212, 255, 0.15);
      color: var(--primary);
      border: 1px solid rgba(0, 212, 255, 0.3);
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: 600;
    }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 24px;
    }
    .nav-links a {
      color: var(--text-muted);
      text-decoration: none;
      font-size: 14px;
      font-weight: 500;
      transition: color 0.2s;
    }
    .nav-links a:hover {
      color: var(--primary);
    }
    .nav-cta {
      background: linear-gradient(135deg, #00d4ff, #0099ff);
      color: #050b14 !important;
      padding: 8px 18px;
      border-radius: 8px;
      font-weight: 700 !important;
      transition: transform 0.15s, box-shadow 0.15s !important;
      box-shadow: 0 4px 14px var(--primary-glow);
    }
    .nav-cta:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(0, 212, 255, 0.4);
    }

    /* Buttons */
    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 14px 28px;
      border-radius: 10px;
      font-size: 15px;
      font-weight: 700;
      text-decoration: none;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
      border: none;
    }
    .btn-primary {
      background: linear-gradient(135deg, #00d4ff, #0284c7);
      color: #030712;
      box-shadow: 0 4px 20px var(--primary-glow);
    }
    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 28px rgba(0, 212, 255, 0.45);
      background: linear-gradient(135deg, #38bdf8, #0ea5e9);
    }
    .btn-secondary {
      background: rgba(16, 24, 40, 0.85);
      color: var(--text);
      border: 1px solid var(--card-border);
      backdrop-filter: blur(12px);
    }
    .btn-secondary:hover {
      background: rgba(30, 41, 59, 0.85);
      border-color: var(--primary);
      color: var(--primary);
      transform: translateY(-2px);
    }
    .btn-emerald {
      background: linear-gradient(135deg, #10b981, #059669);
      color: #ffffff;
      box-shadow: 0 4px 18px var(--emerald-glow);
    }
    .btn-emerald:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 24px rgba(16, 185, 129, 0.4);
    }

    /* Hero */
    .hero {
      padding: 80px 0 60px;
      text-align: center;
      position: relative;
    }
    .hero-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 6px 16px;
      border-radius: 30px;
      background: rgba(0, 212, 255, 0.1);
      border: 1px solid rgba(0, 212, 255, 0.3);
      font-size: 12px;
      font-family: var(--font-mono);
      color: var(--primary);
      margin-bottom: 24px;
    }
    .pulse-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--emerald);
      box-shadow: 0 0 8px var(--emerald);
      animation: pulse 2s infinite;
    }
    @keyframes pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.4; transform: scale(0.8); }
    }
    .hero h1 {
      font-size: 52px;
      font-weight: 800;
      line-height: 1.15;
      letter-spacing: -1.5px;
      margin-bottom: 20px;
      max-width: 900px;
      margin-left: auto;
      margin-right: auto;
    }
    .hero h1 span.gradient-text {
      background: linear-gradient(135deg, #00d4ff 20%, #38bdf8 50%, #818cf8 80%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .hero p {
      font-size: 18px;
      color: var(--text-muted);
      max-width: 740px;
      margin: 0 auto 36px;
      line-height: 1.7;
    }
    .hero-actions {
      display: flex;
      justify-content: center;
      gap: 16px;
      flex-wrap: wrap;
      margin-bottom: 48px;
    }

    /* Telemetry Ribbon */
    .telemetry-ribbon {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
      max-width: 1000px;
      margin: 0 auto 80px;
    }
    .telemetry-card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 12px;
      padding: 18px;
      display: flex;
      align-items: center;
      gap: 14px;
      backdrop-filter: blur(10px);
    }
    .telemetry-icon {
      font-size: 24px;
      background: rgba(0, 212, 255, 0.1);
      width: 44px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 10px;
      border: 1px solid rgba(0, 212, 255, 0.2);
    }
    .telemetry-label {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--text-dim);
      font-weight: 600;
    }
    .telemetry-val {
      font-size: 15px;
      font-weight: 700;
      color: var(--text);
    }

    /* Section Headers */
    .section-header {
      text-align: center;
      margin-bottom: 50px;
    }
    .section-tag {
      font-family: var(--font-mono);
      font-size: 12px;
      color: var(--primary);
      text-transform: uppercase;
      letter-spacing: 1px;
      font-weight: 600;
      margin-bottom: 10px;
      display: inline-block;
    }
    .section-title {
      font-size: 36px;
      font-weight: 800;
      letter-spacing: -1px;
      margin-bottom: 14px;
    }
    .section-subtitle {
      font-size: 16px;
      color: var(--text-muted);
      max-width: 620px;
      margin: 0 auto;
    }

    /* 5 Pillars Grid */
    .pillars-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
      gap: 24px;
      margin-bottom: 100px;
    }
    .pillar-card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 32px;
      position: relative;
      transition: all 0.25s ease;
      backdrop-filter: blur(12px);
    }
    .pillar-card:hover {
      transform: translateY(-4px);
      border-color: var(--primary);
      box-shadow: 0 12px 30px rgba(0, 212, 255, 0.12);
    }
    .pillar-num {
      position: absolute;
      top: 24px;
      right: 24px;
      font-family: var(--font-mono);
      font-size: 14px;
      font-weight: 700;
      color: var(--text-dim);
    }
    .pillar-icon {
      font-size: 32px;
      width: 56px;
      height: 56px;
      border-radius: 12px;
      background: rgba(0, 212, 255, 0.1);
      border: 1px solid rgba(0, 212, 255, 0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;
    }
    .pillar-card h3 {
      font-size: 20px;
      font-weight: 700;
      margin-bottom: 12px;
      color: #fff;
    }
    .pillar-card p {
      color: var(--text-muted);
      font-size: 14px;
      line-height: 1.65;
      margin-bottom: 16px;
    }
    .pillar-badge-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
    .pillar-badge {
      font-family: var(--font-mono);
      font-size: 11px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      padding: 3px 8px;
      border-radius: 6px;
      color: var(--primary);
    }

    /* Comparison Table */
    .comparison-section {
      margin-bottom: 100px;
    }
    .table-container {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      overflow-x: auto;
      backdrop-filter: blur(12px);
    }
    table.comp-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
      font-size: 14px;
    }
    table.comp-table th, table.comp-table td {
      padding: 16px 20px;
      border-bottom: 1px solid var(--card-border);
    }
    table.comp-table th {
      background: rgba(10, 15, 29, 0.85);
      font-weight: 700;
      color: var(--text);
      font-size: 15px;
    }
    table.comp-table th.pasa-col {
      color: var(--primary);
      background: rgba(0, 212, 255, 0.08);
      border-top: 2px solid var(--primary);
    }
    table.comp-table td.pasa-col {
      background: rgba(0, 212, 255, 0.04);
      font-weight: 600;
      color: #ffffff;
    }
    .badge-check {
      color: var(--emerald);
      font-weight: 700;
    }
    .badge-cross {
      color: var(--rose);
      font-weight: 700;
    }

    /* Pricing Section */
    .pricing-section {
      margin-bottom: 100px;
    }
    .pricing-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 24px;
      align-items: stretch;
    }
    .pricing-card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 36px 28px;
      display: flex;
      flex-direction: column;
      position: relative;
      transition: all 0.25s ease;
      backdrop-filter: blur(12px);
    }
    .pricing-card:hover {
      transform: translateY(-4px);
    }
    .pricing-card.featured {
      border-color: var(--primary);
      box-shadow: 0 0 30px rgba(0, 212, 255, 0.18);
      background: linear-gradient(180deg, rgba(14, 25, 48, 0.9), rgba(10, 16, 32, 0.9));
    }
    .featured-tag {
      position: absolute;
      top: -12px;
      left: 50%;
      transform: translateX(-50%);
      background: linear-gradient(135deg, #00d4ff, #0284c7);
      color: #030712;
      font-size: 11px;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 1px;
      padding: 4px 14px;
      border-radius: 20px;
    }
    .pricing-name {
      font-size: 20px;
      font-weight: 700;
      margin-bottom: 8px;
    }
    .pricing-desc {
      font-size: 13px;
      color: var(--text-muted);
      margin-bottom: 24px;
      min-height: 40px;
    }
    .pricing-price {
      font-size: 40px;
      font-weight: 800;
      color: #fff;
      display: flex;
      align-items: baseline;
      gap: 4px;
      margin-bottom: 24px;
    }
    .pricing-price span.period {
      font-size: 14px;
      color: var(--text-dim);
      font-weight: 500;
    }
    .pricing-features {
      list-style: none;
      margin-bottom: 32px;
      flex-grow: 1;
    }
    .pricing-features li {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      font-size: 13px;
      color: var(--text-muted);
      margin-bottom: 12px;
    }
    .pricing-features li svg {
      flex-shrink: 0;
      margin-top: 2px;
      color: var(--emerald);
    }

    /* License Checker Card */
    .checker-section {
      background: linear-gradient(135deg, rgba(15, 23, 42, 0.8), rgba(10, 15, 30, 0.8));
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 40px;
      max-width: 800px;
      margin: 0 auto 100px;
      backdrop-filter: blur(12px);
    }
    .checker-input-group {
      display: flex;
      gap: 12px;
      margin-top: 20px;
      flex-wrap: wrap;
    }
    .checker-input {
      flex: 1;
      min-width: 260px;
      background: rgba(6, 9, 19, 0.85);
      border: 1px solid var(--card-border);
      border-radius: 10px;
      padding: 14px 18px;
      color: var(--text);
      font-family: var(--font-mono);
      font-size: 14px;
      outline: none;
      transition: border-color 0.2s;
    }
    .checker-input:focus {
      border-color: var(--primary);
    }
    .checker-result {
      margin-top: 20px;
      padding: 16px;
      border-radius: 10px;
      display: none;
      font-size: 14px;
    }

    /* Download Box */
    .download-card {
      background: radial-gradient(circle at 100% 0%, rgba(0, 212, 255, 0.12), transparent 50%),
                  linear-gradient(135deg, rgba(14, 21, 37, 0.95), rgba(7, 10, 20, 0.95));
      border: 1px solid var(--card-border-glow);
      border-radius: 20px;
      padding: 48px;
      margin-bottom: 100px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 32px;
      flex-wrap: wrap;
    }
    .download-info h3 {
      font-size: 28px;
      font-weight: 800;
      margin-bottom: 10px;
    }
    .download-meta {
      font-family: var(--font-mono);
      font-size: 13px;
      color: var(--text-muted);
      margin-bottom: 16px;
    }
    .hash-badge {
      background: rgba(0, 0, 0, 0.4);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      padding: 10px 14px;
      font-family: var(--font-mono);
      font-size: 12px;
      color: #38bdf8;
      word-break: break-all;
      max-width: 580px;
    }

    /* Modal */
    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(3, 6, 12, 0.85);
      backdrop-filter: blur(12px);
      z-index: 1000;
      display: none;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }
    .modal-dialog {
      background: #0b1120;
      border: 1px solid var(--card-border-glow);
      border-radius: 20px;
      width: 100%;
      max-width: 540px;
      padding: 36px;
      position: relative;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.7);
    }
    .modal-close {
      position: absolute;
      top: 20px;
      right: 20px;
      background: transparent;
      border: none;
      color: var(--text-dim);
      font-size: 24px;
      cursor: pointer;
      line-height: 1;
    }
    .modal-close:hover { color: #fff; }
    .form-group {
      margin-bottom: 18px;
    }
    .form-label {
      display: block;
      font-size: 13px;
      font-weight: 600;
      margin-bottom: 8px;
      color: var(--text);
    }
    .form-control {
      width: 100%;
      background: rgba(6, 9, 19, 0.9);
      border: 1px solid var(--card-border);
      border-radius: 10px;
      padding: 12px 16px;
      color: #fff;
      font-size: 14px;
      font-family: inherit;
    }
    .form-control:focus {
      outline: none;
      border-color: var(--primary);
    }
    .key-box {
      background: rgba(0, 0, 0, 0.5);
      border: 1px dashed var(--primary);
      border-radius: 10px;
      padding: 16px;
      text-align: center;
      font-family: var(--font-mono);
      font-size: 16px;
      color: var(--primary);
      margin: 16px 0;
      word-break: break-all;
    }

    /* FAQ */
    .faq-grid {
      max-width: 800px;
      margin: 0 auto 100px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .faq-item {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 12px;
      padding: 24px;
    }
    .faq-item h4 {
      font-size: 17px;
      margin-bottom: 8px;
      color: #fff;
    }
    .faq-item p {
      font-size: 14px;
      color: var(--text-muted);
      line-height: 1.6;
    }

    /* Footer */
    footer {
      border-top: 1px solid var(--card-border);
      padding: 40px 0;
      text-align: center;
      color: var(--text-dim);
      font-size: 13px;
    }
    .footer-links {
      display: flex;
      justify-content: center;
      gap: 20px;
      margin-bottom: 16px;
    }
    .footer-links a {
      color: var(--text-muted);
      text-decoration: none;
      transition: color 0.2s;
    }
    .footer-links a:hover {
      color: var(--primary);
    }

    @media (max-width: 768px) {
      .hero h1 { font-size: 36px; }
      .nav-links { display: none; }
      .download-card { padding: 28px; }
      .hero-actions { flex-direction: column; }
    }
  </style>
</head>
<body>

  <!-- Top Navigation -->
  <header>
    <div class="container nav-inner">
      <a href="/pasa/" class="brand">
        <div class="brand-icon">🛡️</div>
        <div>
          <span class="brand-name">PASA Sentinel</span>
          <span class="brand-badge">v${version} PRO</span>
        </div>
      </a>
      <div class="nav-links">
        <a href="#pillars">Defense Pillars</a>
        <a href="#comparison">Why PASA</a>
        <a href="#pricing">Pricing</a>
        <a href="#checker">License Validator</a>
        <a href="#faq">FAQ</a>
        <a href="admin" target="_blank">Web Console</a>
        <a href="#download" class="nav-cta">Download Free Trial</a>
      </div>
    </div>
  </header>

  <main class="container">

    <!-- Hero -->
    <section class="hero">
      <div class="hero-badge">
        <span class="pulse-dot"></span>
        <span>ED25519 VERIFIED • ZERO TELEMETRY CLOUD • 100% SOVEREIGN</span>
      </div>
      <h1>
        The Sovereign Anti-Theft & <br>
        <span class="gradient-text">Stealth Defense System</span> for Android
      </h1>
      <p>
        Ordinary trackers fail the second a thief disables Wi-Fi, ejects your SIM card, or turns on Airplane Mode. 
        PASA operates silently on <b>Direct Telegram C2</b> and <b>Dual-Channel Cellular SMS</b> — capturing forensic evidence, 
        trapping thieves with accelerometer sensors, and locking down your device irreversibly.
      </p>
      <div class="hero-actions">
        <a href="#download" class="btn btn-primary">
          <span>📥 Download Free Trial</span>
          <span style="font-size: 11px; opacity: 0.8;">(v${version})</span>
        </a>
        <button class="btn btn-emerald" onclick="openCheckout('PRO_LIFETIME', 29.99)">
          <span>💎 Buy Pro Lifetime ($29.99)</span>
        </button>
        <a href="https://t.me/pasa_agent_bot" target="_blank" class="btn btn-secondary">
          <span>✈️ Telegram C2 Demo</span>
        </a>
      </div>

      <!-- Telemetry Ribbon -->
      <div class="telemetry-ribbon">
        <div class="telemetry-card">
          <div class="telemetry-icon">⚡</div>
          <div>
            <div class="telemetry-label">Command Latency</div>
            <div class="telemetry-val">&lt; 350ms Direct</div>
          </div>
        </div>
        <div class="telemetry-card">
          <div class="telemetry-icon">📡</div>
          <div>
            <div class="telemetry-label">C2 Redundancy</div>
            <div class="telemetry-val">Data + Raw Cellular SMS</div>
          </div>
        </div>
        <div class="telemetry-card">
          <div class="telemetry-icon">🔒</div>
          <div>
            <div class="telemetry-label">Cryptographic Core</div>
            <div class="telemetry-val">Ed25519 & AES-256 GCM</div>
          </div>
        </div>
        <div class="telemetry-card">
          <div class="telemetry-icon">🛡️</div>
          <div>
            <div class="telemetry-label">Third-Party Tracking</div>
            <div class="telemetry-val">0% Pure Sovereign</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 5 Pillars Showcase -->
    <section id="pillars">
      <div class="section-header">
        <span class="section-tag">Sovereign Architecture</span>
        <h2 class="section-title">The 5 Pillars of Mobile Defense</h2>
        <p class="section-subtitle">
          Engineered for high-threat scenarios, covert recovery, and unbreakable physical security.
        </p>
      </div>

      <div class="pillars-grid">
        <!-- Pillar 1 -->
        <div class="pillar-card">
          <span class="pillar-num">01</span>
          <div class="pillar-icon">✈️</div>
          <h3>Instant Telegram C2</h3>
          <p>
            Control your phone from any Telegram chat in real time. Dispatch silent camera captures, 1080p video, 
            ambient microphone forensics, siren alarms, and clipboard extractions with instant inline buttons.
          </p>
          <div class="pillar-badge-list">
            <span class="pillar-badge">Front/Back Photo</span>
            <span class="pillar-badge">60s Audio</span>
            <span class="pillar-badge">Live Map GPS</span>
          </div>
        </div>

        <!-- Pillar 2 -->
        <div class="pillar-card">
          <span class="pillar-num">02</span>
          <div class="pillar-icon">📡</div>
          <h3>Offline Cellular SMS Fallback</h3>
          <p>
            When mobile data and Wi-Fi are switched off, PASA's background broadcast receiver listens on raw cellular SMS. 
            Text <code>PASA &lt;password&gt; /locate</code> from any phone; PASA texts back high-accuracy Google Maps coordinates.
          </p>
          <div class="pillar-badge-list">
            <span class="pillar-badge">Zero Internet Needed</span>
            <span class="pillar-badge">SMS Reverse Ping</span>
            <span class="pillar-badge">Password Protected</span>
          </div>
        </div>

        <!-- Pillar 3 -->
        <div class="pillar-card">
          <span class="pillar-num">03</span>
          <div class="pillar-icon">🏃</div>
          <h3>Autonomous Edge Sensor Traps</h3>
          <p>
            Hardware sensors react at the speed of silicon. If snatched from your hand (&gt;2.6G accelerometer spike) 
            or unplugged from a charger while locked, PASA immediately sounds an alarm, locks the screen, and takes photos.
          </p>
          <div class="pillar-badge-list">
            <span class="pillar-badge">Snatch-and-Grab</span>
            <span class="pillar-badge">Charger Tamper</span>
            <span class="pillar-badge">Intruder Snaps</span>
          </div>
        </div>

        <!-- Pillar 4 -->
        <div class="pillar-card">
          <span class="pillar-num">04</span>
          <div class="pillar-icon">🕶️</div>
          <h3>Fake Shutdown Blackout</h3>
          <p>
            Tricks thieves into believing the phone is dead. Displays authentic power-down animation, vibrations, 
            and complete screen blackout while secretly maintaining GPS tracking, audio surveillance, and Telegram dispatch.
          </p>
          <div class="pillar-badge-list">
            <span class="pillar-badge">Stealth Blackout</span>
            <span class="pillar-badge">Background Watchdog</span>
            <span class="pillar-badge">Remote Wake</span>
          </div>
        </div>

        <!-- Pillar 5 -->
        <div class="pillar-card">
          <span class="pillar-num">05</span>
          <div class="pillar-icon">🛡️</div>
          <h3>Enterprise Device Owner & Shredder</h3>
          <p>
            Leverages Android Enterprise Device Owner Kiosk policies to prevent uninstallation, settings tampering, or 
            safe-mode bypasses. Cryptographic file shredder executes multi-pass PRNG noise + zero-fill on sensitive files.
          </p>
          <div class="pillar-badge-list">
            <span class="pillar-badge">Anti-Uninstall</span>
            <span class="pillar-badge">PRNG File Shred</span>
            <span class="pillar-badge">Emergency Duress PIN</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Comparison Table -->
    <section id="comparison" class="comparison-section">
      <div class="section-header">
        <span class="section-tag">Direct Comparison</span>
        <h2 class="section-title">Why Google "Find My Device" Fails</h2>
        <p class="section-subtitle">
          See how PASA performs under real-world theft conditions compared to legacy tools.
        </p>
      </div>

      <div class="table-container">
        <table class="comp-table">
          <thead>
            <tr>
              <th>Security Capability</th>
              <th class="pasa-col">🛡️ PASA Sentinel Pro</th>
              <th>Google Find My Device</th>
              <th>Commercial Spyware ($50/mo)</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><b>Offline Command Channel (Zero Internet)</b></td>
              <td class="pasa-col"><span class="badge-check">✓ YES</span> (Cellular SMS C2)</td>
              <td><span class="badge-cross">✗ FAILS</span> (Requires active data)</td>
              <td><span class="badge-cross">✗ FAILS</span> (Requires active data)</td>
            </tr>
            <tr>
              <td><b>Stealth Camera & Mic Capture</b></td>
              <td class="pasa-col"><span class="badge-check">✓ YES</span> (Silent photo/video/mic)</td>
              <td><span class="badge-cross">✗ NO</span> (Ring only)</td>
              <td><span class="badge-check">✓ YES</span> (Slow, insecure servers)</td>
            </tr>
            <tr>
              <td><b>Autonomous Anti-Snatch Acceleration Trap</b></td>
              <td class="pasa-col"><span class="badge-check">✓ YES</span> (Hardware sensor edge)</td>
              <td><span class="badge-cross">✗ NO</span></td>
              <td><span class="badge-cross">✗ NO</span></td>
            </tr>
            <tr>
              <td><b>Fake Shutdown Blackout Surveillance</b></td>
              <td class="pasa-col"><span class="badge-check">✓ YES</span> (Covert background mode)</td>
              <td><span class="badge-cross">✗ NO</span></td>
              <td><span class="badge-cross">✗ NO</span></td>
            </tr>
            <tr>
              <td><b>Sovereign Privacy (No 3rd-Party Tracking)</b></td>
              <td class="pasa-col"><span class="badge-check">✓ 100% PRIVATE</span> (Direct C2)</td>
              <td><span class="badge-cross">✗ Big Tech Cloud</span></td>
              <td><span class="badge-cross">✗ Shady Third-Party Logs</span></td>
            </tr>
            <tr>
              <td><b>Anti-Uninstall Enterprise Protection</b></td>
              <td class="pasa-col"><span class="badge-check">✓ YES</span> (Device Owner Kiosk)</td>
              <td><span class="badge-cross">✗ Basic Admin</span></td>
              <td><span class="badge-cross">✗ Easily Detected & Bypassed</span></td>
            </tr>
            <tr>
              <td><b>Emergency Duress SOS PIN</b></td>
              <td class="pasa-col"><span class="badge-check">✓ YES</span> (Silent photo & SOS alert)</td>
              <td><span class="badge-cross">✗ NO</span></td>
              <td><span class="badge-cross">✗ NO</span></td>
            </tr>
            <tr>
              <td><b>Cost / Subscription</b></td>
              <td class="pasa-col"><b>$14.99/yr or $29.99 Lifetime</b></td>
              <td>Free (Fails in real theft)</td>
              <td>$480.00 / year recurring</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Pricing Cards -->
    <section id="pricing" class="pricing-section">
      <div class="section-header">
        <span class="section-tag">Transparent Pricing</span>
        <h2 class="section-title">Invest in Uncompromising Protection</h2>
        <p class="section-subtitle">
          Activate your sovereign security agent today. Instant key delivery and direct Telegram bot integration.
        </p>
      </div>

      <div class="pricing-grid">
        <!-- 7-Day Trial -->
        <div class="pricing-card">
          <div class="pricing-name">7-Day Free Trial</div>
          <div class="pricing-desc">Full feature evaluation for a single device. Zero commitment.</div>
          <div class="pricing-price">$0 <span class="period">/ 7 days</span></div>
          <ul class="pricing-features">
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              1 Android Device Slot
            </li>
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              Full Telegram Bot C2
            </li>
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              Offline Cellular SMS C2
            </li>
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              Autonomous Traps & Shredder
            </li>
          </ul>
          <a href="#download" class="btn btn-secondary">Download Free APK</a>
        </div>

        <!-- Pro Annual -->
        <div class="pricing-card">
          <div class="pricing-name">Pro Annual</div>
          <div class="pricing-desc">Annual continuous defense with real-time updates and priority C2.</div>
          <div class="pricing-price">$14.99 <span class="period">/ year</span></div>
          <ul class="pricing-features">
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              1 Android Device Slot
            </li>
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              Continuous In-App OTA Updates
            </li>
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              Tactical Map Fleet Radar Access
            </li>
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              24/7 Dual-Channel C2
            </li>
          </ul>
          <button class="btn btn-secondary" onclick="openCheckout('PRO_ANNUAL', 14.99)">Get Pro Annual</button>
        </div>

        <!-- Pro Lifetime (Featured) -->
        <div class="pricing-card featured">
          <div class="featured-tag">🔥 MOST POPULAR</div>
          <div class="pricing-name">Pro Lifetime</div>
          <div class="pricing-desc">The sovereign owner package. One-time payment, lifetime security.</div>
          <div class="pricing-price">$29.99 <span class="period">/ one-time</span></div>
          <ul class="pricing-features">
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              <b>Up to 3 Devices</b>
            </li>
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              <b>Lifetime OTA Updates Guaranteed</b>
            </li>
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              VIP Developer Support & Setup Aid
            </li>
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              All Future 5-Pillar Features Included
            </li>
          </ul>
          <button class="btn btn-primary" onclick="openCheckout('PRO_LIFETIME', 29.99)">Claim Pro Lifetime</button>
        </div>

        <!-- Family / Fleet -->
        <div class="pricing-card">
          <div class="pricing-name">Family / Fleet</div>
          <div class="pricing-desc">Centralized sovereign defense for family fleets and small teams.</div>
          <div class="pricing-price">$49.99 <span class="period">/ one-time</span></div>
          <ul class="pricing-features">
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              <b>Up to 10 Android Devices</b>
            </li>
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              Lifetime Updates for All 10 Slots
            </li>
            <li>
              <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
              Unified Fleet Tactical Map Radar
            </li>
          </ul>
          <button class="btn btn-secondary" onclick="openCheckout('PRO_ENTERPRISE', 49.99)">Get Fleet License</button>
        </div>
      </div>
    </section>

    <!-- License Key Validator -->
    <section id="checker">
      <div class="checker-section">
        <div style="text-align: center;">
          <h3 style="font-size: 24px; font-weight: 700; margin-bottom: 8px;">🔑 License Status Validator</h3>
          <p style="color: var(--text-muted); font-size: 14px;">Already purchased? Enter your license key or registered email to check active slots and expiration.</p>
        </div>
        <div class="checker-input-group">
          <input type="text" id="lookupQuery" class="checker-input" placeholder="PASA-PRO-XXXX-XXXX-XXXX or your@email.com" />
          <button class="btn btn-primary" onclick="lookupLicense()" style="padding: 14px 22px;">Verify License</button>
        </div>
        <div id="checkerResult" class="checker-result"></div>
      </div>
    </section>

    <!-- Download Card -->
    <section id="download" class="download-card">
      <div class="download-info">
        <div style="font-family: var(--font-mono); font-size: 12px; color: var(--primary); font-weight: 600; margin-bottom: 6px;">VERIFIED BINARY RELEASE</div>
        <h3>PASA Sentinel for Android v${version}</h3>
        <div class="download-meta">
          Build: <b>#${versionCode}</b> &bull; Size: <b>${fileSizeMb} MB</b> &bull; Min Android: <b>8.0 (API 26)</b> &bull; Recommended: <b>Android 10 - 15</b>
        </div>
        <div style="font-size: 13px; color: var(--text-muted); margin-bottom: 12px;">SHA-256 Checksum:</div>
        <div class="hash-badge">${sha256}</div>
      </div>
      <div>
        <a href="api/app/download/${downloadFilename}" class="btn btn-primary" style="padding: 18px 36px; font-size: 16px;">
          <span>📥 Download APK (${fileSizeMb} MB)</span>
        </a>
        <div style="text-align: center; margin-top: 10px;">
          <a href="https://github.com/rbr48/pasa" target="_blank" style="color: var(--text-dim); font-size: 12px; text-decoration: none;">View Source Code on GitHub &rarr;</a>
        </div>
      </div>
    </section>

    <!-- FAQ -->
    <section id="faq">
      <div class="section-header">
        <span class="section-tag">Common Inquiries</span>
        <h2 class="section-title">Frequently Asked Questions</h2>
      </div>

      <div class="faq-grid">
        <div class="faq-item">
          <h4>Does PASA require root access?</h4>
          <p>No. PASA is built using standard and Android Enterprise Device Admin APIs. All features (silent camera, microphone, GPS, lock, SMS C2, fake shutdown) work without root.</p>
        </div>
        <div class="faq-item">
          <h4>How does Offline SMS C2 work?</h4>
          <p>If the device has no mobile data or Wi-Fi, you can text <code>PASA &lt;masterPassword&gt; &lt;command&gt;</code> (e.g. <code>PASA secret123 /locate</code>) from any phone. PASA intercepts the message in the background and sends back an SMS with Google Maps coordinates.</p>
        </div>
        <div class="faq-item">
          <h4>Can an intruder simply uninstall the app?</h4>
          <p>PASA registers as an Android Device Administrator. Android disables the "Uninstall" button for active Device Admins. Furthermore, if provisioned as Device Owner, the app cannot be uninstalled or stopped even in Safe Mode.</p>
        </div>
        <div class="faq-item">
          <h4>What happens after the 7-day trial?</h4>
          <p>After 7 days, advanced telemetry and forensic triggers request a valid Pro License key. Your phone data is never locked, and you can upgrade at any time with a Pro Annual ($14.99) or Lifetime ($29.99) key.</p>
        </div>
      </div>
    </section>

  </main>

  <!-- Instant Checkout / License Generator Modal -->
  <div id="checkoutModal" class="modal-overlay">
    <div class="modal-dialog">
      <button class="modal-close" onclick="closeCheckout()">&times;</button>
      <div id="checkoutStepForm">
        <div style="font-family: var(--font-mono); font-size: 11px; color: var(--primary); text-transform: uppercase; margin-bottom: 6px;">SOVEREIGN CHECKOUT</div>
        <h3 id="checkoutPlanTitle" style="font-size: 22px; font-weight: 800; margin-bottom: 6px;">Pro Lifetime License</h3>
        <p style="color: var(--text-muted); font-size: 13px; margin-bottom: 20px;">Instant key generation and delivery to your email.</p>
        
        <form id="purchaseForm" onsubmit="handlePurchaseSubmit(event)">
          <input type="hidden" id="selectedTier" value="PRO_LIFETIME" />
          
          <div class="form-group">
            <label class="form-label">Your Email Address</label>
            <input type="email" id="customerEmail" class="form-control" placeholder="name@example.com" required />
          </div>

          <div class="form-group">
            <label class="form-label">Payment Method</label>
            <select id="paymentProvider" class="form-control">
              <option value="card">Credit / Debit Card (Stripe Instant)</option>
              <option value="crypto">USDT / BTC / Crypto</option>
              <option value="bkash">bKash / Nagad (Bangladesh)</option>
              <option value="instant_demo">Instant Sovereign Key Generator (Commercial Demo)</option>
            </select>
          </div>

          <div style="display: flex; justify-content: space-between; align-items: center; margin: 24px 0 16px;">
            <span style="color: var(--text-muted); font-size: 14px;">Total Due:</span>
            <span id="checkoutPriceTag" style="font-size: 24px; font-weight: 800; color: #fff;">$29.99</span>
          </div>

          <button type="submit" id="submitOrderBtn" class="btn btn-primary" style="width: 100%;">
            <span>Generate & Authorize License Key &rarr;</span>
          </button>
        </form>
      </div>

      <!-- Success Step -->
      <div id="checkoutStepSuccess" style="display: none; text-align: center;">
        <div style="font-size: 40px; margin-bottom: 10px;">🎉</div>
        <h3 style="font-size: 22px; font-weight: 800; margin-bottom: 8px; color: var(--emerald);">License Issued Successfully!</h3>
        <p style="color: var(--text-muted); font-size: 13px;">Save your key and activate it in your Telegram bot or Android app:</p>
        
        <div id="generatedKeyDisplay" class="key-box">PASA-PRO-XXXX-XXXX-XXXX</div>
        
        <div style="display: flex; gap: 10px; justify-content: center; margin-bottom: 20px;">
          <button class="btn btn-secondary" onclick="copyLicenseKey()" style="padding: 10px 18px; font-size: 13px;">
            📋 Copy Key
          </button>
          <a id="telegramActivateLink" href="https://t.me/pasa_agent_bot" target="_blank" class="btn btn-primary" style="padding: 10px 18px; font-size: 13px;">
            ✈️ Activate in Telegram
          </a>
        </div>

        <div style="background: rgba(255,255,255,0.04); border-radius: 10px; padding: 12px; font-size: 12px; color: var(--text-dim); text-align: left; font-family: var(--font-mono);">
          <b>Quick Activation Command:</b><br>
          <code id="activateCommandSample">/license activate PASA-PRO-XXXX-XXXX</code>
        </div>
      </div>
    </div>
  </div>

  <footer>
    <div class="container">
      <div class="footer-links">
        <a href="admin">Admin Console</a>
        <a href="health">API Status</a>
        <a href="https://github.com/rbr48/pasa" target="_blank">GitHub</a>
        <a href="#checker">License Check</a>
        <a href="#pricing">Purchase</a>
      </div>
      <p>&copy; 2026 PASA (Private Android Security Agent). Sovereign mobile defense architecture.</p>
    </div>
  </footer>

  <script>
    function openCheckout(tier, price) {
      document.getElementById('selectedTier').value = tier;
      document.getElementById('checkoutPriceTag').textContent = '$' + price.toFixed(2);
      let title = 'Pro Annual License';
      if (tier === 'PRO_LIFETIME') title = 'Pro Lifetime (3 Devices)';
      if (tier === 'PRO_ENTERPRISE') title = 'Fleet License (10 Devices)';
      document.getElementById('checkoutPlanTitle').textContent = title;
      document.getElementById('checkoutStepForm').style.display = 'block';
      document.getElementById('checkoutStepSuccess').style.display = 'none';
      document.getElementById('checkoutModal').style.display = 'flex';
    }

    function closeCheckout() {
      document.getElementById('checkoutModal').style.display = 'none';
    }

    async function handlePurchaseSubmit(e) {
      e.preventDefault();
      const email = document.getElementById('customerEmail').value;
      const tier = document.getElementById('selectedTier').value;
      const provider = document.getElementById('paymentProvider').value;
      const btn = document.getElementById('submitOrderBtn');

      btn.disabled = true;
      btn.innerHTML = '<span>⏳ Issuing Sovereign Key...</span>';

      try {
        const res = await fetch('api/license/purchase', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, tier, provider })
        });
        const data = await res.json();
        if (data.ok && data.license) {
          document.getElementById('checkoutStepForm').style.display = 'none';
          document.getElementById('checkoutStepSuccess').style.display = 'block';
          document.getElementById('generatedKeyDisplay').textContent = data.license.key;
          document.getElementById('activateCommandSample').textContent = '/license activate ' + data.license.key;
          document.getElementById('telegramActivateLink').href = 'https://t.me/pasa_agent_bot?start=' + data.license.key;
        } else {
          alert('Purchase error: ' + (data.description || 'Could not generate license.'));
        }
      } catch (err) {
        alert('Connection error: ' + err.message);
      } finally {
        btn.disabled = false;
        btn.innerHTML = '<span>Generate & Authorize License Key &rarr;</span>';
      }
    }

    function copyLicenseKey() {
      const key = document.getElementById('generatedKeyDisplay').textContent;
      navigator.clipboard.writeText(key).then(() => {
        alert('Copied to clipboard: ' + key);
      });
    }

    async function lookupLicense() {
      const query = document.getElementById('lookupQuery').value.trim();
      const resDiv = document.getElementById('checkerResult');
      if (!query) {
        alert('Please enter a license key or email address.');
        return;
      }
      resDiv.style.display = 'block';
      resDiv.innerHTML = '<span style="color: var(--primary);">⏳ Querying license ledger...</span>';

      try {
        const res = await fetch('api/license/lookup', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ query })
        });
        const data = await res.json();
        if (data.ok && data.license) {
          const lic = data.license;
          const daysText = lic.daysLeft > 9000 ? 'Permanent Lifetime' : lic.daysLeft + ' days remaining';
          resDiv.style.background = 'rgba(16, 185, 129, 0.15)';
          resDiv.style.border = '1px solid var(--emerald)';
          resDiv.innerHTML = \`
            <div style="font-weight: 700; color: var(--emerald); margin-bottom: 4px;">✅ Active License Record Found</div>
            <div style="font-family: var(--font-mono); font-size: 13px;">
              <b>Key:</b> \${lic.key}<br>
              <b>Tier:</b> \${lic.tier} (\${daysText})<br>
              <b>Device Slots:</b> \${lic.activatedCount} of \${lic.maxDevices} used<br>
              <b>Status:</b> \${lic.status}
            </div>
          \`;
        } else {
          resDiv.style.background = 'rgba(244, 63, 94, 0.15)';
          resDiv.style.border = '1px solid var(--rose)';
          resDiv.innerHTML = '<div style="color: var(--rose);">❌ No active license record found. Please verify your input or purchase a key.</div>';
        }
      } catch (err) {
        resDiv.innerHTML = '<div style="color: var(--rose);">Error querying license: ' + err.message + '</div>';
      }
    }
  </script>
</body>
</html>`;
}

module.exports = { renderCommercialLandingPage };
