const fs = require('fs');
const path = require('path');
const { faqData } = require('./scratch_faq_data.js');

const targetHtmlPath = path.join(__dirname, '..', 'public', 'index.html');

function generateHtml() {
  return `<!DOCTYPE html>
<html lang="en" data-lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PASA Sentinel — Sovereign Mobile Defense &amp; Anti-Theft Intelligence</title>
<meta name="description" content="PASA Sentinel: Knox-Grade Device Owner mobile defense system with 86 Telegram C2 commands, air-gapped SMS fallback, covert forensics, and zero-storage architecture for Android 8.0–16.">
<meta name="theme-color" content="#030712">
<link rel="icon" href="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ef4444' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z'/%3E%3C/svg%3E">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600;700&family=Tiro+Bangla:ital@0;1&display=swap" rel="stylesheet">
<style>
  :root {
    --bg-dark: #030712;
    --bg-void: #060b18;
    --surface: #0a1024;
    --surface-elevated: #0f1738;
    --surface-card: rgba(13, 20, 44, 0.78);
    --border: rgba(255, 255, 255, 0.1);
    --border-glow: rgba(6, 182, 212, 0.35);
    --border-crimson: rgba(239, 68, 68, 0.35);

    /* Cyber & Threat Palette */
    --crimson: #ef4444;
    --crimson-glow: rgba(239, 68, 68, 0.2);
    --crimson-deep: #991b1b;
    --cyan: #06b6d4;
    --cyan-glow: rgba(6, 182, 212, 0.2);
    --amber: #f59e0b;
    --amber-glow: rgba(245, 158, 11, 0.2);
    --emerald: #10b981;
    --emerald-glow: rgba(16, 185, 129, 0.2);

    /* WCAG AA / AAA High Contrast Typography */
    --text: #ffffff;
    --text-dim: #cbd5e1;       /* slate-300: High legibility */
    --text-faint: #94a3b8;     /* slate-400: Subtitles */
    --text-muted: #64748b;     /* slate-500: Auxiliary info */

    --font: 'Inter', -apple-system, sans-serif;
    --font-bn: 'Tiro Bangla', 'Inter', -apple-system, sans-serif;
    --mono: 'JetBrains Mono', monospace;
  }

  [data-lang="bn"] body {
    font-family: var(--font-bn);
  }

  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  html {
    scroll-behavior: smooth;
  }
  
  /* Global Anchor Collision Fix: Sticky Nav Offset */
  section[id], div[id] {
    scroll-margin-top: 110px;
  }

  body {
    font-family: var(--font);
    background: var(--bg-dark);
    color: var(--text);
    line-height: 1.65;
    -webkit-font-smoothing: antialiased;
    overflow-x: hidden;
  }

  /* Scanline & Grid Effect */
  .hud-grid {
    position: fixed; inset: 0; pointer-events: none; z-index: 1;
    background-image: 
      linear-gradient(rgba(6, 182, 212, 0.02) 1px, transparent 1px),
      linear-gradient(90deg, rgba(6, 182, 212, 0.02) 1px, transparent 1px);
    background-size: 44px 44px;
  }

  /* Refined Top Status Bar (Clean & Professional) */
  .top-ticker {
    background: #040816;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    padding: 7px 0; font-family: var(--mono); font-size: 11.5px;
    color: var(--text-dim); position: relative; z-index: 101;
  }
  .ticker-wrap {
    overflow: hidden; white-space: nowrap; flex: 1; max-width: 920px; margin: 0 auto;
  }
  .ticker-content {
    display: inline-block; animation: marquee 35s linear infinite;
  }
  @keyframes marquee {
    0% { transform: translateX(0); }
    100% { transform: translateX(-50%); }
  }

  /* Layout Containers */
  .wrap { max-width: 1200px; margin: 0 auto; padding: 0 24px; position: relative; z-index: 10; }
  .wrap-narrow { max-width: 860px; margin: 0 auto; padding: 0 24px; position: relative; z-index: 10; }

  /* Navigation */
  nav.nav {
    position: sticky; top: 0; z-index: 100;
    background: rgba(3, 7, 18, 0.92); backdrop-filter: blur(20px);
    border-bottom: 1px solid var(--border);
  }
  .nav-inner {
    display: flex; align-items: center; justify-content: space-between; height: 64px;
  }
  .brand {
    display: flex; align-items: center; gap: 12px; text-decoration: none; color: var(--text);
  }
  .brand-badge {
    width: 38px; height: 38px; border-radius: 8px;
    background: linear-gradient(135deg, rgba(239, 68, 68, 0.25), rgba(6, 182, 212, 0.2));
    border: 1px solid var(--border-crimson); display: flex; align-items: center; justify-content: center;
  }
  .brand-badge svg { width: 22px; height: 22px; color: var(--crimson); }
  .brand-title {
    font-weight: 800; font-size: 17px; letter-spacing: -0.01em; display: flex; flex-direction: column;
  }
  .brand-sub {
    font-family: var(--mono); font-size: 9.5px; color: var(--cyan); letter-spacing: 0.1em; text-transform: uppercase;
  }

  /* Streamlined Desktop Menu (Max 5 items + Dropdown) */
  .nav-menu { display: flex; align-items: center; gap: 4px; }
  .nav-link {
    color: var(--text-dim); text-decoration: none; font-size: 13.5px; font-weight: 500;
    padding: 8px 12px; border-radius: 6px; transition: all 0.2s;
  }
  .nav-link:hover { color: var(--text); background: rgba(255,255,255,0.06); }

  /* Dropdown for Secondary Links */
  .nav-dropdown { position: relative; display: inline-block; }
  .nav-dropdown-btn {
    color: var(--text-dim); background: none; border: none; font-size: 13.5px; font-weight: 500;
    padding: 8px 12px; border-radius: 6px; cursor: pointer; display: flex; align-items: center; gap: 4px;
    transition: all 0.2s; font-family: var(--font);
  }
  .nav-dropdown-btn:hover { color: var(--text); background: rgba(255,255,255,0.06); }
  .nav-dropdown-menu {
    position: absolute; top: calc(100% + 6px); left: 0; min-width: 200px;
    background: #090f24; border: 1px solid var(--border); border-radius: 10px;
    padding: 8px 0; box-shadow: 0 16px 36px rgba(0,0,0,0.6); display: none; z-index: 120;
    backdrop-filter: blur(16px);
  }
  .nav-dropdown:hover .nav-dropdown-menu,
  .nav-dropdown-menu:hover { display: block; }
  .nav-dropdown-item {
    display: block; padding: 9px 18px; color: var(--text-dim); text-decoration: none;
    font-size: 13px; font-weight: 500; transition: all 0.15s;
  }
  .nav-dropdown-item:hover { background: rgba(6, 182, 212, 0.12); color: var(--cyan); }

  /* Mobile Hamburger Toggle & Drawer */
  .mobile-toggle {
    display: none; background: none; border: 1px solid var(--border); color: var(--text);
    padding: 7px 10px; border-radius: 6px; cursor: pointer;
  }
  .mobile-drawer {
    position: fixed; top: 0; right: -290px; width: 280px; height: 100vh;
    background: #060b18; border-left: 1px solid var(--border); z-index: 200;
    padding: 24px; display: flex; flex-direction: column; gap: 16px;
    transition: right 0.3s cubic-bezier(0.16, 1, 0.3, 1); box-shadow: -10px 0 40px rgba(0,0,0,0.8);
  }
  .mobile-drawer.open { right: 0; }
  .mobile-drawer-hdr { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border); padding-bottom: 16px; }
  .mobile-drawer a { color: var(--text-dim); text-decoration: none; font-size: 15px; font-weight: 500; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.04); }
  .mobile-drawer a:hover { color: var(--cyan); }
  .drawer-overlay {
    position: fixed; inset: 0; background: rgba(0,0,0,0.7); z-index: 190; display: none;
  }
  .drawer-overlay.open { display: block; }

  @media (max-width: 992px) {
    .nav-menu { display: none; }
    .mobile-toggle { display: block; }
  }

  /* Language Switcher */
  .lang-switcher {
    display: flex; align-items: center; background: rgba(15, 23, 50, 0.85);
    border: 1px solid var(--border-glow); border-radius: 8px; padding: 3px; gap: 2px;
  }
  .lang-btn {
    background: transparent; border: none; color: var(--text-dim);
    font-family: var(--mono); font-size: 11px; font-weight: 700;
    padding: 5px 11px; border-radius: 6px; cursor: pointer; transition: all 0.2s;
    display: flex; align-items: center; gap: 5px;
  }
  .lang-btn.active {
    background: linear-gradient(135deg, var(--crimson), #b91c1c);
    color: #fff; box-shadow: 0 0 12px rgba(239, 68, 68, 0.4);
  }
  .lang-btn:hover:not(.active) { color: var(--text); background: rgba(255,255,255,0.06); }

  .cta-nav {
    background: linear-gradient(135deg, #ef4444, #dc2626); color: #fff;
    padding: 8px 18px; border-radius: 8px; font-size: 13px; font-weight: 700;
    text-decoration: none; display: flex; align-items: center; gap: 6px;
    box-shadow: 0 0 16px rgba(239, 68, 68, 0.35); transition: all 0.2s;
  }
  .cta-nav:hover { transform: translateY(-1px); box-shadow: 0 0 24px rgba(239, 68, 68, 0.6); }

  /* ── Hero Section (Radar + Focused Value Propositions) ── */
  .hero {
    position: relative; padding: 90px 0 80px; overflow: hidden;
    background: radial-gradient(circle at 50% 30%, rgba(15, 23, 60, 0.6) 0%, rgba(3, 7, 18, 0.95) 70%);
  }

  /* Radar Sweep Visual */
  .radar-container {
    position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
    width: 720px; height: 720px; pointer-events: none; z-index: 0; opacity: 0.38;
  }
  .radar-ring {
    position: absolute; inset: 0; margin: auto; border-radius: 50%;
    border: 1px solid rgba(6, 182, 212, 0.2);
  }
  .radar-ring:nth-child(1) { width: 220px; height: 220px; }
  .radar-ring:nth-child(2) { width: 420px; height: 420px; }
  .radar-ring:nth-child(3) { width: 620px; height: 620px; }
  .radar-ring:nth-child(4) { width: 720px; height: 720px; border-color: rgba(239, 68, 68, 0.25); border-style: dashed; }
  .radar-crosshair-x {
    position: absolute; top: 50%; left: 0; width: 100%; height: 1px;
    background: linear-gradient(90deg, transparent, rgba(6, 182, 212, 0.25), transparent);
  }
  .radar-crosshair-y {
    position: absolute; top: 0; left: 50%; width: 1px; height: 100%;
    background: linear-gradient(180deg, transparent, rgba(6, 182, 212, 0.25), transparent);
  }
  .radar-sweep {
    position: absolute; top: 0; left: 0; width: 100%; height: 100%; border-radius: 50%;
    background: conic-gradient(from 0deg, rgba(6, 182, 212, 0.2) 0deg, rgba(6, 182, 212, 0.04) 45deg, transparent 90deg);
    animation: radarSweep 7s linear infinite;
  }
  @keyframes radarSweep {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
  .radar-blip {
    position: absolute; width: 8px; height: 8px; border-radius: 50%; background: var(--crimson);
    box-shadow: 0 0 10px var(--crimson); animation: blip 2.5s infinite;
  }
  .radar-blip.b1 { top: 32%; left: 68%; }
  .radar-blip.b2 { top: 62%; left: 28%; animation-delay: 1.2s; }
  @keyframes blip {
    0%, 100% { opacity: 0.2; transform: scale(0.8); }
    50% { opacity: 1; transform: scale(1.3); }
  }

  /* Hero Content */
  .hero-content {
    position: relative; z-index: 10; text-align: center; max-width: 940px; margin: 0 auto;
  }
  .dossier-tag {
    display: inline-flex; align-items: center; gap: 8px;
    background: rgba(6, 182, 212, 0.08); border: 1px solid var(--border-glow);
    color: var(--cyan); padding: 6px 14px; border-radius: 20px;
    font-family: var(--mono); font-size: 11.5px; font-weight: 700; letter-spacing: 0.06em;
    margin-bottom: 22px; text-transform: uppercase;
  }
  .pulse-dot {
    width: 7px; height: 7px; border-radius: 50%; background: var(--emerald);
    box-shadow: 0 0 8px var(--emerald); animation: pulseDot 1.4s infinite;
  }
  @keyframes pulseDot {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.4; transform: scale(0.7); }
  }

  .hero h1 {
    font-size: clamp(32px, 5.2vw, 58px); font-weight: 900; line-height: 1.18;
    letter-spacing: -0.02em; margin-bottom: 20px;
    background: linear-gradient(180deg, #ffffff 40%, #cbd5e1 100%);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  }
  .hero-sub {
    font-size: clamp(16px, 2vw, 19px); color: var(--text-dim); line-height: 1.6;
    max-width: 780px; margin: 0 auto 30px;
  }

  /* 3 Core Value Proposition Badges */
  .hero-pillars {
    display: flex; flex-wrap: wrap; justify-content: center; gap: 12px; margin-bottom: 36px;
  }
  .hero-pillar {
    background: rgba(15, 23, 50, 0.7); border: 1px solid var(--border);
    border-radius: 8px; padding: 8px 14px; font-size: 13px; font-weight: 600;
    color: var(--text); display: flex; align-items: center; gap: 8px; backdrop-filter: blur(8px);
  }
  .hero-pillar svg { width: 16px; height: 16px; color: var(--cyan); }

  .hero-actions {
    display: flex; flex-wrap: wrap; justify-content: center; gap: 16px; margin-bottom: 44px;
  }
  .btn-primary {
    background: linear-gradient(135deg, #ef4444 0%, #b91c1c 100%);
    color: #fff; padding: 14px 28px; border-radius: 10px; font-weight: 700; font-size: 15px;
    text-decoration: none; display: inline-flex; align-items: center; gap: 10px;
    box-shadow: 0 0 24px rgba(239, 68, 68, 0.4); border: 1px solid rgba(255,255,255,0.15);
    transition: all 0.25s ease;
  }
  .btn-primary:hover {
    transform: translateY(-2px); box-shadow: 0 0 36px rgba(239, 68, 68, 0.65);
  }
  .btn-secondary {
    background: rgba(15, 23, 50, 0.75); color: var(--text); padding: 14px 28px;
    border-radius: 10px; font-weight: 600; font-size: 15px; text-decoration: none;
    display: inline-flex; align-items: center; gap: 10px; border: 1px solid var(--border-glow);
    transition: all 0.25s ease; backdrop-filter: blur(10px);
  }
  .btn-secondary:hover {
    background: rgba(20, 30, 65, 0.95); border-color: var(--cyan); transform: translateY(-2px);
  }

  /* Live HUD Banner */
  .hud-status-strip {
    background: rgba(10, 16, 36, 0.9); border: 1px solid var(--border);
    border-radius: 12px; padding: 16px 24px; max-width: 980px; margin: 0 auto;
    display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4); backdrop-filter: blur(16px);
  }
  .hud-stat-item {
    display: flex; flex-direction: column; gap: 4px; text-align: left;
    padding-left: 12px; border-left: 2px solid var(--border-glow);
  }
  .hud-stat-item.crimson { border-left-color: var(--crimson); }
  .hud-stat-item.amber { border-left-color: var(--amber); }
  .hud-stat-label {
    font-family: var(--mono); font-size: 10.5px; color: var(--text-faint); text-transform: uppercase; letter-spacing: 0.08em;
  }
  .hud-stat-val {
    font-size: 14.5px; font-weight: 700; color: var(--text); display: flex; align-items: center; gap: 6px;
  }
  .status-badge {
    font-family: var(--mono); font-size: 10px; padding: 2px 6px; border-radius: 4px;
    background: rgba(16, 185, 129, 0.15); color: var(--emerald); font-weight: 700;
  }

  /* ── Interactive Live Forensic Terminal Simulator (CID Console) ── */
  .terminal-section {
    padding: 80px 0; background: linear-gradient(180deg, var(--bg-void) 0%, #030611 100%);
    border-top: 1px solid var(--border); border-bottom: 1px solid var(--border);
  }
  .terminal-card {
    background: #02040a; border: 1px solid var(--border-glow); border-radius: 14px;
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.7), 0 0 20px rgba(6, 182, 212, 0.12);
    overflow: hidden; max-width: 960px; margin: 0 auto;
  }
  .terminal-hdr {
    background: rgba(15, 23, 50, 0.95); padding: 12px 18px; border-bottom: 1px solid var(--border);
    display: flex; align-items: center; justify-content: space-between;
  }
  .terminal-dots { display: flex; gap: 8px; }
  .terminal-dot { width: 11px; height: 11px; border-radius: 50%; }
  .terminal-dot.r { background: #ef4444; }
  .terminal-dot.y { background: #f59e0b; }
  .terminal-dot.g { background: #10b981; }
  .terminal-title {
    font-family: var(--mono); font-size: 12px; font-weight: 700; color: var(--cyan); letter-spacing: 0.05em;
  }
  .terminal-chip-bar {
    background: rgba(6, 11, 24, 0.85); padding: 10px 16px; border-bottom: 1px solid rgba(255,255,255,0.06);
    display: flex; gap: 8px; align-items: center; overflow-x: auto; white-space: nowrap; -webkit-overflow-scrolling: touch;
  }
  .term-chip {
    background: rgba(15, 23, 50, 0.85); border: 1px solid var(--border); color: var(--text-dim);
    font-family: var(--mono); font-size: 11.5px; padding: 5px 11px; border-radius: 6px;
    cursor: pointer; transition: all 0.15s; flex-shrink: 0;
  }
  .term-chip:hover {
    border-color: var(--cyan); color: var(--cyan); background: rgba(6, 182, 212, 0.12);
  }
  .terminal-screen {
    padding: 22px; min-height: 220px; max-height: 360px; overflow-y: auto;
    font-family: var(--mono); font-size: 13px; line-height: 1.6; color: #38bdf8;
    background: radial-gradient(circle at 50% 50%, rgba(6, 182, 212, 0.03) 0%, transparent 80%);
  }
  .term-line { margin-bottom: 6px; }
  .term-prompt { color: #f8fafc; font-weight: 700; }
  .term-success { color: #34d399; }
  .term-alert { color: #f87171; }
  .term-warn { color: #fbbf24; }
  .terminal-input-row {
    display: flex; align-items: center; gap: 10px; padding: 12px 18px;
    background: rgba(10, 16, 36, 0.95); border-top: 1px solid var(--border);
  }
  .term-input {
    flex: 1; background: transparent; border: none; outline: none;
    font-family: var(--mono); font-size: 13.5px; color: #fff;
  }

  /* ── 10-Second Thief Timeline Comparison ── */
  .timeline-section { padding: 90px 0; }
  .comp-split {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 28px;
    margin-top: 48px;
  }
  .comp-col {
    border-radius: 14px; padding: 32px 26px; border: 1px solid var(--border);
    background: var(--surface-card); backdrop-filter: blur(14px); position: relative;
  }
  .comp-col.vulnerable {
    border-color: var(--border-crimson);
    background: linear-gradient(180deg, rgba(239, 68, 68, 0.06) 0%, rgba(15, 23, 50, 0.8) 100%);
  }
  .comp-col.protected {
    border-color: var(--border-glow);
    background: linear-gradient(180deg, rgba(6, 182, 212, 0.06) 0%, rgba(15, 23, 50, 0.8) 100%);
  }
  .comp-header {
    display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px;
    padding-bottom: 16px; border-bottom: 1px solid var(--border);
  }
  .comp-badge {
    font-family: var(--mono); font-size: 11px; font-weight: 800; padding: 4px 10px;
    border-radius: 4px; text-transform: uppercase;
  }
  .timeline-steps { list-style: none; display: flex; flex-direction: column; gap: 18px; }
  .timeline-step { display: flex; gap: 14px; align-items: flex-start; }
  .step-time {
    font-family: var(--mono); font-size: 11.5px; font-weight: 800; min-width: 48px;
    padding: 3px 6px; border-radius: 4px; text-align: center;
  }
  .step-content h4 { font-size: 14.5px; font-weight: 700; margin-bottom: 4px; }
  .step-content p { font-size: 13.5px; color: var(--text-dim); line-height: 1.5; }

  /* ── Knox Enterprise Defense Matrix (Comparison Table) ── */
  .matrix-section {
    padding: 85px 0; background: rgba(6, 11, 24, 0.6); border-top: 1px solid var(--border);
  }
  .matrix-scroll-hint {
    display: none; font-family: var(--mono); font-size: 11.5px; color: var(--cyan);
    text-align: right; margin-bottom: 8px;
  }
  @media (max-width: 820px) {
    .matrix-scroll-hint { display: block; }
  }
  .matrix-table-wrap {
    overflow-x: auto; margin-top: 24px; border-radius: 12px; border: 1px solid var(--border);
    -webkit-overflow-scrolling: touch;
  }
  .matrix-table {
    width: 100%; border-collapse: collapse; text-align: left; font-size: 13.5px; min-width: 700px;
  }
  .matrix-table th, .matrix-table td {
    padding: 16px 20px; border-bottom: 1px solid var(--border);
  }
  .matrix-table th {
    background: rgba(15, 23, 50, 0.95); font-family: var(--mono); font-size: 12px;
    font-weight: 700; color: var(--text); letter-spacing: 0.05em; text-transform: uppercase;
  }
  .matrix-table th:first-child, .matrix-table td:first-child {
    position: sticky; left: 0; z-index: 5;
    background: #090f24; border-right: 1px solid var(--border-glow); min-width: 180px;
  }
  .matrix-table tr:hover td { background: rgba(255, 255, 255, 0.03); }
  .matrix-table td.col-pasa {
    background: rgba(6, 182, 212, 0.06); font-weight: 700; color: #fff;
    border-left: 1px solid var(--border-glow); border-right: 1px solid var(--border-glow);
  }
  .matrix-table th.col-pasa {
    background: rgba(6, 182, 212, 0.15); color: var(--cyan);
    border-left: 1px solid var(--border-glow); border-right: 1px solid var(--border-glow);
  }
  .check-icon { color: var(--emerald); font-weight: 800; font-size: 16px; margin-right: 6px; }
  .cross-icon { color: var(--crimson); font-weight: 800; font-size: 16px; margin-right: 6px; }

  /* ── Interactive Vulnerability Self-Assessment Widget ── */
  .audit-section {
    padding: 85px 0; background: linear-gradient(180deg, var(--bg-dark) 0%, var(--bg-void) 100%);
  }
  .audit-box {
    background: var(--surface-card); border: 1px solid var(--border-crimson);
    border-radius: 16px; padding: 36px 30px; max-width: 860px; margin: 0 auto;
    box-shadow: 0 16px 40px rgba(0,0,0,0.5); backdrop-filter: blur(14px);
  }
  .audit-item {
    display: flex; align-items: center; justify-content: space-between; gap: 16px;
    padding: 18px 0; border-bottom: 1px solid rgba(255,255,255,0.06);
  }
  .audit-item:last-child { border-bottom: none; }
  .audit-q { font-size: 15px; font-weight: 600; color: var(--text); }
  .audit-toggle {
    display: flex; gap: 6px; background: rgba(10, 16, 36, 0.8); padding: 4px; border-radius: 8px;
    border: 1px solid var(--border);
  }
  .audit-btn {
    background: transparent; border: none; color: var(--text-dim); font-family: var(--mono);
    font-size: 11.5px; font-weight: 700; padding: 5px 12px; border-radius: 6px; cursor: pointer;
  }
  .audit-btn.active.yes { background: #ef4444; color: #fff; }
  .audit-btn.active.no { background: #10b981; color: #fff; }
  .audit-result-bar {
    margin-top: 24px; padding: 18px; border-radius: 10px; background: rgba(3, 7, 18, 0.7);
    border: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between;
    flex-wrap: wrap; gap: 14px;
  }
  .audit-score-num { font-size: 26px; font-weight: 900; color: var(--crimson); font-family: var(--mono); }

  /* ── Crime Scene Investigation Cases (CID Vibe) ── */
  .section-hdr { text-align: center; margin-bottom: 50px; position: relative; }
  .section-tag {
    font-family: var(--mono); font-size: 11px; font-weight: 700; color: var(--cyan);
    letter-spacing: 0.12em; text-transform: uppercase; margin-bottom: 12px; display: inline-block;
  }
  .section-title {
    font-size: clamp(26px, 3.8vw, 40px); font-weight: 800; letter-spacing: -0.01em; margin-bottom: 14px;
  }
  .section-lede { font-size: 16px; color: var(--text-dim); max-width: 680px; margin: 0 auto; line-height: 1.6; }

  .cases-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px;
    margin-bottom: 80px;
  }
  .case-card {
    background: var(--surface-card); border: 1px solid var(--border);
    border-radius: 14px; padding: 28px 24px; position: relative; overflow: hidden;
    transition: all 0.3s ease; backdrop-filter: blur(12px); display: flex; flex-direction: column;
  }
  .case-card:hover {
    transform: translateY(-4px); border-color: var(--border-glow);
    box-shadow: 0 14px 40px rgba(6, 182, 212, 0.12);
  }
  .case-card.danger:hover {
    border-color: var(--border-crimson);
    box-shadow: 0 14px 40px rgba(239, 68, 68, 0.15);
  }
  .case-badge-row {
    display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px;
  }
  .case-id {
    font-family: var(--mono); font-size: 11px; font-weight: 700; color: var(--cyan);
    padding: 3px 8px; border-radius: 4px; background: rgba(6, 182, 212, 0.1); border: 1px solid var(--border-glow);
  }
  .case-threat {
    font-family: var(--mono); font-size: 10px; font-weight: 700; color: var(--crimson);
    text-transform: uppercase; letter-spacing: 0.05em; display: flex; align-items: center; gap: 5px;
  }
  .case-title { font-size: 18px; font-weight: 700; margin-bottom: 12px; line-height: 1.35; }
  .case-desc { font-size: 14px; color: var(--text-dim); line-height: 1.6; margin-bottom: 18px; flex: 1; }
  .case-counter {
    background: rgba(3, 7, 18, 0.6); border: 1px solid var(--border);
    border-radius: 8px; padding: 12px 14px; font-family: var(--mono); font-size: 12px;
  }
  .case-counter-title {
    color: var(--emerald); font-weight: 700; margin-bottom: 4px; display: flex; align-items: center; gap: 6px;
  }
  .case-counter-desc { color: var(--text-dim); font-size: 11.5px; line-height: 1.5; }

  /* ── 86 C2 Commands Hub Console ── */
  .c2-section {
    padding: 85px 0; background: linear-gradient(180deg, var(--bg-dark) 0%, var(--bg-void) 100%);
    border-top: 1px solid var(--border); border-bottom: 1px solid var(--border);
  }
  .hub-tabs { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin-bottom: 36px; }
  .hub-tab {
    background: rgba(15, 23, 50, 0.6); border: 1px solid var(--border); color: var(--text-dim);
    padding: 9px 18px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer;
    transition: all 0.2s; font-family: var(--mono); display: flex; align-items: center; gap: 8px;
  }
  .hub-tab.active {
    background: rgba(6, 182, 212, 0.15); border-color: var(--cyan); color: var(--text);
    box-shadow: 0 0 16px rgba(6, 182, 212, 0.25);
  }
  .hub-tab:hover:not(.active) { background: rgba(255,255,255,0.06); color: var(--text); }
  .hub-pill-badge { background: rgba(255,255,255,0.1); padding: 2px 6px; border-radius: 10px; font-size: 10.5px; }

  .c2-hub-panel { display: none; animation: fadeIn 0.3s ease; }
  .c2-hub-panel.active { display: block; }
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(6px); }
    to { opacity: 1; transform: translateY(0); }
  }

  .cmd-chips-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
  .cmd-chip {
    background: rgba(10, 16, 36, 0.7); border: 1px solid var(--border);
    border-radius: 8px; padding: 12px 14px; transition: all 0.2s;
  }
  .cmd-chip:hover { border-color: var(--border-glow); background: rgba(15, 23, 50, 0.9); }
  .cmd-name {
    font-family: var(--mono); font-size: 13px; font-weight: 700; color: var(--cyan);
    margin-bottom: 4px; display: flex; align-items: center; justify-content: space-between;
  }
  .cmd-scope {
    font-size: 9.5px; padding: 1px 5px; border-radius: 3px; background: rgba(239, 68, 68, 0.15);
    color: var(--crimson); text-transform: uppercase;
  }
  .cmd-desc { font-size: 12px; color: var(--text-dim); line-height: 1.45; }

  /* ── Master FAQ Section (Accordion + Search + Filter) ── */
  .faq-section { padding: 90px 0 120px; position: relative; }
  .faq-filter-bar {
    max-width: 900px; margin: 0 auto 36px; display: flex; flex-direction: column; gap: 16px;
  }
  .faq-search-box { position: relative; width: 100%; }
  .faq-search-input {
    width: 100%; background: rgba(15, 23, 50, 0.8); border: 1px solid var(--border);
    border-radius: 12px; padding: 14px 20px 14px 48px; color: var(--text);
    font-size: 15px; font-family: var(--font); outline: none; transition: all 0.2s;
  }
  .faq-search-input:focus { border-color: var(--cyan); box-shadow: 0 0 20px rgba(6, 182, 212, 0.2); }
  .faq-search-icon {
    position: absolute; left: 18px; top: 50%; transform: translateY(-50%);
    width: 18px; height: 18px; color: var(--text-faint); pointer-events: none;
  }
  .faq-cat-pills { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
  .cat-pill {
    background: rgba(10, 16, 36, 0.6); border: 1px solid var(--border);
    color: var(--text-dim); padding: 7px 14px; border-radius: 20px; font-size: 12.5px;
    cursor: pointer; transition: all 0.2s; font-weight: 500;
  }
  .cat-pill.active {
    background: var(--cyan); color: #030712; font-weight: 700; border-color: var(--cyan);
    box-shadow: 0 0 12px rgba(6, 182, 212, 0.35);
  }
  .cat-pill:hover:not(.active) { background: rgba(255,255,255,0.08); color: var(--text); }

  .faq-accordion { max-width: 900px; margin: 0 auto; display: flex; flex-direction: column; gap: 12px; }
  .faq-item {
    background: var(--surface-card); border: 1px solid var(--border);
    border-radius: 12px; overflow: hidden; transition: all 0.25s ease;
  }
  .faq-item:hover { border-color: rgba(255, 255, 255, 0.15); }
  .faq-item.open {
    border-color: var(--border-glow); background: rgba(15, 23, 50, 0.85);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
  }
  .faq-question {
    padding: 20px 24px; display: flex; align-items: center; justify-content: space-between;
    cursor: pointer; gap: 16px; user-select: none;
  }
  .faq-q-text { font-size: 16px; font-weight: 600; line-height: 1.45; color: var(--text); }
  .faq-toggle-icon {
    width: 20px; height: 20px; min-width: 20px; color: var(--cyan); transition: transform 0.25s ease;
  }
  .faq-item.open .faq-toggle-icon { transform: rotate(180deg); color: var(--crimson); }
  .faq-answer {
    padding: 0 24px 22px; color: var(--text-dim); font-size: 14.5px; line-height: 1.7;
    display: none; border-top: 1px solid rgba(255, 255, 255, 0.05); margin-top: 4px; padding-top: 16px;
  }
  .faq-item.open .faq-answer { display: block; }
  .faq-answer p { margin-bottom: 12px; }
  .faq-answer p:last-child { margin-bottom: 0; }
  .faq-answer ul, .faq-answer ol { margin-left: 20px; margin-bottom: 12px; }
  .faq-answer li { margin-bottom: 6px; }
  .faq-answer code {
    font-family: var(--mono); font-size: 12.5px; background: rgba(6, 182, 212, 0.1);
    color: var(--cyan); padding: 2px 6px; border-radius: 4px;
  }
  .faq-answer pre {
    background: rgba(3, 7, 18, 0.8); border: 1px solid var(--border);
    border-radius: 8px; padding: 12px 14px; overflow-x: auto; margin-bottom: 12px;
  }
  .faq-answer pre code { background: transparent; padding: 0; color: #38bdf8; }

  /* ── Pricing & Licensing (Perfect Card Heights & Baseline Alignment) ── */
  .pricing-section {
    padding: 90px 0; background: rgba(6, 11, 24, 0.8); border-top: 1px solid var(--border);
  }
  .pricing-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(290px, 1fr)); gap: 24px;
    margin-top: 48px; align-items: stretch;
  }
  .pricing-card {
    background: var(--surface-card); border: 1px solid var(--border);
    border-radius: 16px; padding: 36px 28px; display: flex; flex-direction: column;
    justify-content: space-between; height: 100%; position: relative; backdrop-filter: blur(14px);
    transition: all 0.3s ease;
  }
  .pricing-card:hover { transform: translateY(-4px); }
  .pricing-card.featured {
    border-color: var(--crimson);
    box-shadow: 0 16px 50px rgba(239, 68, 68, 0.2);
    background: linear-gradient(180deg, rgba(239, 68, 68, 0.08) 0%, rgba(15, 23, 50, 0.85) 100%);
  }
  .pricing-featured-badge {
    position: absolute; top: -13px; left: 50%; transform: translateX(-50%);
    background: linear-gradient(135deg, #ef4444, #b91c1c); color: #fff;
    font-family: var(--mono); font-size: 11px; font-weight: 800; padding: 4px 14px;
    border-radius: 20px; letter-spacing: 0.08em; text-transform: uppercase;
    box-shadow: 0 0 16px rgba(239, 68, 68, 0.5);
  }
  .pricing-plan { font-size: 20px; font-weight: 800; margin-bottom: 8px; }
  .pricing-desc { font-size: 13.5px; color: var(--text-dim); margin-bottom: 24px; min-height: 42px; }
  .pricing-price { font-size: 42px; font-weight: 900; line-height: 1; margin-bottom: 6px; color: var(--text); }
  .pricing-currency { font-size: 18px; color: var(--text-dim); font-weight: 600; }
  .pricing-period { font-size: 13px; color: var(--text-faint); margin-bottom: 24px; }
  
  /* Features list grows to fill space, locking buttons to baseline */
  .pricing-features { list-style: none; margin-bottom: 28px; flex: 1; }
  .pricing-features li {
    font-size: 13.5px; color: var(--text-dim); margin-bottom: 12px;
    display: flex; align-items: flex-start; gap: 10px;
  }
  .pricing-features li svg { width: 16px; height: 16px; min-width: 16px; color: var(--emerald); margin-top: 3px; }

  /* Payment Channels Strip (Untouched as requested) */
  .payment-channels {
    margin-top: 48px; background: rgba(10, 16, 36, 0.8); border: 1px solid var(--border);
    border-radius: 14px; padding: 26px; display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px;
  }
  .pay-method {
    display: flex; align-items: center; gap: 14px; background: rgba(15, 23, 50, 0.6);
    border: 1px solid rgba(255,255,255,0.06); padding: 14px 18px; border-radius: 10px;
    transition: all 0.2s;
  }
  .pay-method:hover { border-color: var(--border-glow); background: rgba(15, 23, 50, 0.9); }
  .pay-icon {
    width: 40px; height: 40px; min-width: 40px; border-radius: 8px;
    display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.06);
  }
  .pay-title { font-weight: 700; font-size: 14px; color: var(--text); }
  .pay-sub { font-family: var(--mono); font-size: 12px; color: var(--cyan); margin-top: 2px; }
  .pay-action-btn {
    font-size: 11px; font-weight: 700; text-decoration: none; padding: 4px 10px;
    border-radius: 4px; display: inline-flex; align-items: center; gap: 4px; margin-top: 6px;
  }

  /* Toast Notification */
  .toast {
    position: fixed; bottom: 85px; left: 50%; transform: translateX(-50%) translateY(30px);
    background: #06b6d4; color: #02040a; font-family: var(--mono); font-size: 12.5px; font-weight: 700;
    padding: 10px 22px; border-radius: 8px; box-shadow: 0 10px 25px rgba(0,0,0,0.6);
    opacity: 0; pointer-events: none; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); z-index: 1000;
  }
  .toast.show { opacity: 1; transform: translateX(-50%) translateY(0); }

  /* ── Sleek, Non-Obstructive Floating Concierge Widget ── */
  .floating-concierge {
    position: fixed; bottom: 22px; right: 22px; z-index: 95;
    display: flex; flex-direction: column; align-items: flex-end; gap: 10px;
  }
  .concierge-pill {
    background: #0f1738; border: 1px solid var(--border-glow); color: #fff;
    padding: 9px 16px; border-radius: 30px; font-size: 13px; font-weight: 600;
    display: flex; align-items: center; gap: 8px; cursor: pointer;
    box-shadow: 0 8px 30px rgba(0,0,0,0.6); transition: all 0.25s ease;
  }
  .concierge-pill:hover {
    background: #152250; border-color: var(--cyan); transform: translateY(-2px);
  }
  .concierge-card {
    background: #0a1128; border: 1px solid var(--border); border-radius: 12px;
    padding: 14px; box-shadow: 0 16px 40px rgba(0,0,0,0.7); display: none;
    flex-direction: column; gap: 10px; width: 220px; backdrop-filter: blur(14px);
  }
  .concierge-card.open { display: flex; animation: fadeIn 0.2s ease; }
  .concierge-link {
    display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: 8px;
    text-decoration: none; font-size: 13px; font-weight: 600; color: #fff; transition: background 0.15s;
  }
  .concierge-link.wa { background: rgba(37, 211, 102, 0.15); color: #25D366; }
  .concierge-link.wa:hover { background: rgba(37, 211, 102, 0.25); }
  .concierge-link.tg { background: rgba(34, 158, 217, 0.15); color: #229ed9; }
  .concierge-link.tg:hover { background: rgba(34, 158, 217, 0.25); }

  /* ── Footer ── */
  footer {
    background: #02040a; border-top: 1px solid var(--border); padding: 60px 0 40px;
    position: relative; z-index: 10;
  }
  .footer-grid { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 40px; margin-bottom: 40px; }
  @media (max-width: 768px) {
    .footer-grid { grid-template-columns: 1fr; gap: 30px; }
    .hero { padding: 60px 0 50px; }
    .radar-container { width: 380px; height: 380px; }
    .radar-ring:nth-child(3), .radar-ring:nth-child(4) { display: none; }
  }
  .footer-col h5 {
    font-size: 14px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em;
    color: var(--text); margin-bottom: 16px; font-family: var(--mono);
  }
  .footer-col ul { list-style: none; }
  .footer-col li { margin-bottom: 10px; }
  .footer-col a { color: var(--text-dim); text-decoration: none; font-size: 13.5px; transition: color 0.15s; }
  .footer-col a:hover { color: var(--cyan); }
  .footer-bottom {
    border-top: 1px solid rgba(255, 255, 255, 0.06); padding-top: 24px;
    display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 14px;
    font-size: 12px; color: var(--text-faint); font-family: var(--mono);
  }
</style>
</head>
<body>

<div class="hud-grid"></div>

<!-- Top Executive Status Bar (Streamlined) -->
<div class="top-ticker">
  <div class="wrap" style="display:flex;align-items:center;width:100%;">
    <div style="display:flex;align-items:center;gap:7px;min-width:190px;">
      <span class="pulse-dot"></span>
      <span style="font-weight:700;color:var(--text);" data-i18n="top_status">PASA SENTINEL ONLINE</span>
    </div>
    <div class="ticker-wrap">
      <div class="ticker-content" id="tickerMsg">
        KNOX DEVICE OWNER ARCHITECTURE • STRATEGY 1: ZERO CLOUD MEDIA STORAGE • ENCRYPTED HARDWARE ESCROW TOKENS • AIR-GAPPED CELLULAR SMS FALLBACK • ANTI-EDL/BROM BOOTROM KILLSWITCH • 
      </div>
    </div>
    <div style="min-width:130px;text-align:right;font-size:11px;color:var(--cyan);">
      BUILD 47 (v3.5.1)
    </div>
  </div>
</div>

<!-- Navigation -->
<nav class="nav">
  <div class="wrap nav-inner">
    <a href="#" class="brand">
      <div class="brand-badge">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
      </div>
      <div class="brand-title">
        <span>PASA SENTINEL</span>
        <span class="brand-sub">Sovereign Defense</span>
      </div>
    </a>

    <!-- Clean Desktop Menu: 4 primary items + More Dropdown -->
    <div class="nav-menu">
      <a href="#matrix" class="nav-link" data-i18n="nav_matrix">Defense Matrix</a>
      <a href="#timeline" class="nav-link" data-i18n="nav_timeline">10s Defense</a>
      <a href="#simulator" class="nav-link" data-i18n="nav_terminal">C2 Terminal</a>
      <a href="#faq" class="nav-link" data-i18n="nav_faq">Forensic FAQ</a>
      <a href="#pricing" class="nav-link" data-i18n="nav_pricing">Licensing</a>

      <div class="nav-dropdown">
        <button class="nav-dropdown-btn">
          <span data-i18n="nav_more">More</span> ▾
        </button>
        <div class="nav-dropdown-menu">
          <a href="#scenarios" class="nav-dropdown-item" data-i18n="nav_cases">Crime Cases</a>
          <a href="#commands" class="nav-dropdown-item" data-i18n="nav_c2">86 C2 Commands</a>
          <a href="#audit" class="nav-dropdown-item" data-i18n="nav_audit">Vulnerability Audit</a>
        </div>
      </div>
    </div>

    <div style="display:flex;align-items:center;gap:12px;">
      <!-- Language Toggle Switch -->
      <div class="lang-switcher">
        <button class="lang-btn active" id="btnEn" onclick="setLanguage('en')">EN</button>
        <button class="lang-btn" id="btnBn" onclick="setLanguage('bn')">বাংলা</button>
      </div>
      <a href="/releases/pasa-latest.apk" class="cta-nav">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 3v12m0 0-4-4m4 4 4-4M4 21h16"/></svg>
        <span data-i18n="nav_download">Download APK</span>
      </a>

      <!-- Mobile Hamburger Button -->
      <button class="mobile-toggle" onclick="toggleMobileDrawer()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
      </button>
    </div>
  </div>
</nav>

<!-- Mobile Slide-In Drawer -->
<div class="drawer-overlay" id="drawerOverlay" onclick="toggleMobileDrawer()"></div>
<div class="mobile-drawer" id="mobileDrawer">
  <div class="mobile-drawer-hdr">
    <span style="font-weight:800;font-size:16px;">PASA SENTINEL</span>
    <button onclick="toggleMobileDrawer()" style="background:none;border:none;color:#fff;font-size:22px;cursor:pointer;">&times;</button>
  </div>
  <a href="#matrix" onclick="toggleMobileDrawer()" data-i18n="nav_matrix">Defense Matrix</a>
  <a href="#timeline" onclick="toggleMobileDrawer()" data-i18n="nav_timeline">10s Defense</a>
  <a href="#simulator" onclick="toggleMobileDrawer()" data-i18n="nav_terminal">C2 Terminal</a>
  <a href="#scenarios" onclick="toggleMobileDrawer()" data-i18n="nav_cases">Crime Cases</a>
  <a href="#commands" onclick="toggleMobileDrawer()" data-i18n="nav_c2">86 C2 Commands</a>
  <a href="#audit" onclick="toggleMobileDrawer()" data-i18n="nav_audit">Vulnerability Audit</a>
  <a href="#faq" onclick="toggleMobileDrawer()" data-i18n="nav_faq">Forensic FAQ</a>
  <a href="#pricing" onclick="toggleMobileDrawer()" data-i18n="nav_pricing">Licensing</a>
  <div style="margin-top:auto;padding-top:20px;">
    <a href="/releases/pasa-latest.apk" class="btn-primary" style="justify-content:center;width:100%;font-size:13.5px;" data-i18n="nav_download">
      Download APK (v3.5.1)
    </a>
  </div>
</div>

<!-- HERO SECTION -->
<section class="hero">
  <!-- Radar Sweep HUD -->
  <div class="radar-container">
    <div class="radar-ring"></div>
    <div class="radar-ring"></div>
    <div class="radar-ring"></div>
    <div class="radar-ring"></div>
    <div class="radar-crosshair-x"></div>
    <div class="radar-crosshair-y"></div>
    <div class="radar-sweep"></div>
    <div class="radar-blip b1"></div>
    <div class="radar-blip b2"></div>
  </div>

  <div class="wrap hero-content">
    <div class="dossier-tag">
      <span class="pulse-dot"></span>
      <span data-i18n="hero_badge">SOVEREIGN MOBILE DEFENSE // ANDROID 8.0 – 16</span>
    </div>

    <h1 data-i18n="hero_title">YOUR PHONE WILL NEVER BE SURRENDERED.</h1>
    <p class="hero-sub" data-i18n="hero_sub">
      When ordinary trackers go blind in 10 seconds, PASA Sentinel engages Knox-grade hardware lockdown, hunts the perpetrator's real identity, and protects your sovereign personal data.
    </p>

    <!-- 3 Core Architectural Pillars -->
    <div class="hero-pillars">
      <div class="hero-pillar">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
        <span data-i18n="pillar_1">Knox Device Owner (Immutable)</span>
      </div>
      <div class="hero-pillar">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
        <span data-i18n="pillar_2">Zero-Cloud Media Storage</span>
      </div>
      <div class="hero-pillar">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z"/></svg>
        <span data-i18n="pillar_3">Air-Gapped Cellular SMS C2</span>
      </div>
    </div>

    <div class="hero-actions">
      <a href="/releases/pasa-latest.apk" class="btn-primary">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 3v12m0 0-4-4m4 4 4-4M4 21h16"/></svg>
        <span data-i18n="hero_cta_apk">Download Tactical APK (v3.5.1)</span>
      </a>
      <a href="#simulator" class="btn-secondary">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="4 17 10 11 4 5"/><line x1="12" y1="19" x2="20" y2="19"/></svg>
        <span data-i18n="hero_cta_terminal">Launch C2 Terminal</span>
      </a>
    </div>

    <!-- Live Telemetry Status Strip -->
    <div class="hud-status-strip">
      <div class="hud-stat-item crimson">
        <span class="hud-stat-label" data-i18n="stat_anti_uninstall">UNINSTALLATION DEFENSE</span>
        <span class="hud-stat-val">
          <span data-i18n="stat_val_knox">Knox Supervisor</span>
          <span class="status-badge">IMMUTABLE</span>
        </span>
      </div>
      <div class="hud-stat-item">
        <span class="hud-stat-label" data-i18n="stat_zero_storage">SURVEILLANCE STORAGE</span>
        <span class="hud-stat-val">
          <span data-i18n="stat_val_zerostore">0-Cloud Trails</span>
          <span class="status-badge" style="background:rgba(6,182,212,0.15);color:var(--cyan);">TLS 1.3</span>
        </span>
      </div>
      <div class="hud-stat-item amber">
        <span class="hud-stat-label" data-i18n="stat_c2_channels">C2 CONTROL CHANNELS</span>
        <span class="hud-stat-val">
          <span>Telegram + SMS</span>
          <span class="status-badge" style="background:rgba(245,158,11,0.15);color:var(--amber);">AIR-GAPPED</span>
        </span>
      </div>
      <div class="hud-stat-item">
        <span class="hud-stat-label" data-i18n="stat_hardware_escrow">HARDWARE ESCROW</span>
        <span class="hud-stat-val">
          <span data-i18n="stat_val_strongbox">StrongBox / TEE</span>
          <span class="status-badge">ARMED</span>
        </span>
      </div>
    </div>
  </div>
</section>

<!-- ── KNOX ENTERPRISE DEFENSE MATRIX ── -->
<section id="matrix" class="matrix-section">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="matrix_section_tag">HARDWARE ARCHITECTURE COMPARISON</span>
      <h2 class="section-title" data-i18n="matrix_section_title">Knox Device Owner vs Standard Apps</h2>
      <p class="section-lede" data-i18n="matrix_section_lede">
        Why Google Play Store policies prevent ordinary apps from ever matching PASA Sentinel's supervisor privileges.
      </p>
    </div>

    <div class="matrix-scroll-hint" data-i18n="matrix_scroll_hint">
      👉 Scroll horizontally to compare all platforms
    </div>

    <div class="matrix-table-wrap">
      <table class="matrix-table">
        <thead>
          <tr>
            <th data-i18n="th_feature">Security Capability</th>
            <th data-i18n="th_google">Google Find My Device</th>
            <th data-i18n="th_play">Play Store Anti-Theft</th>
            <th class="col-pasa">PASA SENTINEL v3.5.1</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><strong data-i18n="mat_1_title">Zero Root Requirement</strong><br><span style="font-size:12px;color:var(--text-faint);" data-i18n="mat_1_sub">Retains full Knox &amp; StrongBox Keystore integrity</span></td>
            <td><span class="check-icon">✔</span> Yes</td>
            <td><span class="cross-icon">✖</span> Often requires root</td>
            <td class="col-pasa"><span class="check-icon">✔</span> 100% Zero-Root (Device Owner)</td>
          </tr>
          <tr>
            <td><strong data-i18n="mat_2_title">Lockscreen Status Bar &amp; Airplane Block</strong><br><span style="font-size:12px;color:var(--text-faint);" data-i18n="mat_2_sub">Prevents quick settings offline tampering</span></td>
            <td><span class="cross-icon">✖</span> Not possible</td>
            <td><span class="cross-icon">✖</span> Bypassed in 2 seconds</td>
            <td class="col-pasa"><span class="check-icon">✔</span> Enforced (setStatusBarDisabled)</td>
          </tr>
          <tr>
            <td><strong data-i18n="mat_3_title">Air-Gapped Cellular SMS C2 Fallback</strong><br><span style="font-size:12px;color:var(--text-faint);" data-i18n="mat_3_sub">Operates when internet &amp; Wi-Fi are disconnected</span></td>
            <td><span class="cross-icon">✖</span> Dead without internet</td>
            <td><span class="cross-icon">✖</span> Banned by Play Store</td>
            <td class="col-pasa"><span class="check-icon">✔</span> TOTP Dynamic Cellular SMS C2</td>
          </tr>
          <tr>
            <td><strong data-i18n="mat_4_title">Remote Hardware Lockscreen PIN Reset</strong><br><span style="font-size:12px;color:var(--text-faint);" data-i18n="mat_4_sub">Android 14, 15, 16 OS PIN override</span></td>
            <td><span class="cross-icon">✖</span> Deprecated by Google</td>
            <td><span class="cross-icon">✖</span> Completely broken</td>
            <td class="col-pasa"><span class="check-icon">✔</span> Cryptographic Escrow Tokens</td>
          </tr>
          <tr>
            <td><strong data-i18n="mat_5_title">Hardware USB Data Pin Killswitch</strong><br><span style="font-size:12px;color:var(--text-faint);" data-i18n="mat_5_sub">Neutralizes Cellebrite &amp; GrayKey extraction</span></td>
            <td><span class="cross-icon">✖</span> No</td>
            <td><span class="cross-icon">✖</span> No</td>
            <td class="col-pasa"><span class="check-icon">✔</span> Android 12+ UsbDataSignaling</td>
          </tr>
          <tr>
            <td><strong data-i18n="mat_6_title">Anti-Uninstall &amp; Safe Mode Immunity</strong><br><span style="font-size:12px;color:var(--text-faint);" data-i18n="mat_6_sub">Thief cannot delete or force-stop</span></td>
            <td><span class="cross-icon">✖</span> Standard OS user mode</td>
            <td><span class="cross-icon">✖</span> Removable in Safe Mode</td>
            <td class="col-pasa"><span class="check-icon">✔</span> Grayed-out "Managed by Org"</td>
          </tr>
          <tr>
            <td><strong data-i18n="mat_7_title">Surveillance Privacy &amp; Storage Policy</strong><br><span style="font-size:12px;color:var(--text-faint);" data-i18n="mat_7_sub">Who sees your photos, audio, and GPS?</span></td>
            <td>Central Google Cloud Logs</td>
            <td>Third-Party Cloud Servers</td>
            <td class="col-pasa"><span class="check-icon">✔</span> <strong>Strategy 1: 0-Cloud Storage</strong></td>
          </tr>
          <tr>
            <td><strong data-i18n="mat_8_title">Pricing Model</strong><br><span style="font-size:12px;color:var(--text-faint);" data-i18n="mat_8_sub">Recurring costs vs permanent sovereignty</span></td>
            <td>Free (Barebones)</td>
            <td>$60–$120 / year recurring</td>
            <td class="col-pasa"><span class="check-icon">✔</span> <strong>$25 (৳3,000) Lifetime License</strong></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</section>

<!-- ── 10-SECOND THIEF TIMELINE COMPARISON ── -->
<section id="timeline" class="timeline-section">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="time_section_tag">THE FIRST 10 SECONDS OF A THEFT</span>
      <h2 class="section-title" data-i18n="time_section_title">Why Standard Handsets Fail in 10 Seconds</h2>
      <p class="section-lede" data-i18n="time_section_lede">
        The moment a phone is stolen, the clock starts ticking. Here is the forensic difference between normal security and PASA Sentinel.
      </p>
    </div>

    <div class="comp-split">
      <!-- Vulnerable Side -->
      <div class="comp-col vulnerable">
        <div class="comp-header">
          <div>
            <h3 style="font-size:18px;font-weight:800;color:var(--crimson);" data-i18n="time_vuln_title">Standard Android / Trackers</h3>
            <div style="font-size:12.5px;color:var(--text-dim);" data-i18n="time_vuln_sub">Google Find My Device &amp; Play Store Apps</div>
          </div>
          <span class="comp-badge" style="background:rgba(239,68,68,0.15);color:var(--crimson);border:1px solid var(--border-crimson);">CRITICAL DEFECT</span>
        </div>

        <ul class="timeline-steps">
          <li class="timeline-step">
            <span class="step-time" style="background:rgba(239,68,68,0.15);color:var(--crimson);">00:03s</span>
            <div class="step-content">
              <h4 data-i18n="time_v1_title">Notification Drawer Pulled Down</h4>
              <p data-i18n="time_v1_desc">Thief swipes down from lockscreen, toggles Airplane Mode. Internet severed. Google Find My Device is now completely blind.</p>
            </div>
          </li>
          <li class="timeline-step">
            <span class="step-time" style="background:rgba(239,68,68,0.15);color:var(--crimson);">00:08s</span>
            <div class="step-content">
              <h4 data-i18n="time_v2_title">Power Button Pressed &amp; Held</h4>
              <p data-i18n="time_v2_desc">Thief shuts down handset. Operating system terminates. All standard apps and background location services go dead.</p>
            </div>
          </li>
          <li class="timeline-step">
            <span class="step-time" style="background:rgba(239,68,68,0.15);color:var(--crimson);">00:30s</span>
            <div class="step-content">
              <h4 data-i18n="time_v3_title">SIM Card Extracted &amp; Discarded</h4>
              <p data-i18n="time_v3_desc">Physical SIM card thrown into gutter. Device has zero cellular network connectivity and cannot be phoned.</p>
            </div>
          </li>
          <li class="timeline-step">
            <span class="step-time" style="background:rgba(239,68,68,0.15);color:var(--crimson);">02:00m</span>
            <div class="step-content">
              <h4 data-i18n="time_v4_title">Safe Mode Reboot &amp; Forensic Wipe</h4>
              <p data-i18n="time_v4_desc">Thief boots into Safe Mode or connects forensic cable. Play Store security apps are easily uninstalled or data extracted.</p>
            </div>
          </li>
        </ul>
      </div>

      <!-- Protected Side -->
      <div class="comp-col protected">
        <div class="comp-header">
          <div>
            <h3 style="font-size:18px;font-weight:800;color:var(--cyan);" data-i18n="time_prot_title">PASA Sentinel Autonomous Shield</h3>
            <div style="font-size:12.5px;color:var(--text-dim);" data-i18n="time_prot_sub">Knox Device Owner &amp; Hardware Containment</div>
          </div>
          <span class="comp-badge" style="background:rgba(6,182,212,0.15);color:var(--cyan);border:1px solid var(--border-glow);">ARMED 100%</span>
        </div>

        <ul class="timeline-steps">
          <li class="timeline-step">
            <span class="step-time" style="background:rgba(6,182,212,0.15);color:var(--cyan);">00:01s</span>
            <div class="step-content">
              <h4 data-i18n="time_p1_title">2.65G Kinetic Snatch Triggered</h4>
              <p data-i18n="time_p1_desc">Violent vector acceleration locks screen into Knox Kiosk within 30ms. Status bar pull-down and Airplane Mode permanently blocked.</p>
            </div>
          </li>
          <li class="timeline-step">
            <span class="step-time" style="background:rgba(6,182,212,0.15);color:var(--cyan);">00:08s</span>
            <div class="step-content">
              <h4 data-i18n="time_p2_title">Fake Shutdown Deception</h4>
              <p data-i18n="time_p2_desc">Power button triggers authentic OEM power-down animation into 0-nit pitch black screen. Thief assumes phone is dead while cameras and GPS run.</p>
            </div>
          </li>
          <li class="timeline-step">
            <span class="step-time" style="background:rgba(6,182,212,0.15);color:var(--cyan);">00:30s</span>
            <div class="step-content">
              <h4 data-i18n="time_p3_title">Foreign SIM Caller ID Trap</h4>
              <p data-i18n="time_p3_desc">When thief inserts their SIM, PASA uses that SIM's radio to secretly text emergency contacts—revealing the thief's phone number on Caller ID!</p>
            </div>
          </li>
          <li class="timeline-step">
            <span class="step-time" style="background:rgba(6,182,212,0.15);color:var(--cyan);">02:00m</span>
            <div class="step-content">
              <h4 data-i18n="time_p4_title">Hardware USB &amp; Thermal Killswitch</h4>
              <p data-i18n="time_p4_desc">USB Data Pin Killswitch severs physical D+/D- pins against forensic boxes. Blower heat-gun triggers Thermal Trap and Dead Man auto-destruct.</p>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </div>
</section>

<!-- ── LIVE INTERACTIVE C2 TERMINAL SIMULATOR ── -->
<section id="simulator" class="terminal-section">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="term_section_tag">LIVE CID INTELLIGENCE SIMULATOR</span>
      <h2 class="section-title" data-i18n="term_section_title">Interactive C2 Command Console</h2>
      <p class="section-lede" data-i18n="term_section_lede">
        Execute real defense commands and inspect live simulated hardware responses. See how PASA operates in active crime containment.
      </p>
    </div>

    <div class="terminal-card">
      <div class="terminal-hdr">
        <div class="terminal-dots">
          <div class="terminal-dot r"></div>
          <div class="terminal-dot y"></div>
          <div class="terminal-dot g"></div>
        </div>
        <div class="terminal-title">PASA-SENTINEL-C2 // TACTICAL NODE v3.5.1 [CONNECTED]</div>
        <div style="font-family:var(--mono);font-size:11px;color:var(--emerald);">TLS_AES_256_GCM_SHA384</div>
      </div>

      <div class="terminal-chip-bar">
        <span style="font-family:var(--mono);font-size:11px;color:var(--text-faint);flex-shrink:0;" data-i18n="term_chips_label">Quick Commands:</span>
        <button class="term-chip" onclick="simulateCmd('/locate')">/locate</button>
        <button class="term-chip" onclick="simulateCmd('/snap front')">/snap front</button>
        <button class="term-chip" onclick="simulateCmd('/usb_lock on')">/usb_lock on</button>
        <button class="term-chip" onclick="simulateCmd('/thermal')">/thermal</button>
        <button class="term-chip" onclick="simulateCmd('/deadman')">/deadman</button>
        <button class="term-chip" onclick="simulateCmd('/fakeshutdown')">/fakeshutdown</button>
        <button class="term-chip" onclick="simulateCmd('/freeze bkash')">/freeze bkash</button>
        <button class="term-chip" onclick="clearTerminal()" style="color:var(--crimson);border-color:rgba(239,68,68,0.3);">[Clear]</button>
      </div>

      <div class="terminal-screen" id="termScreen">
        <div class="term-line" style="color:var(--text-faint);">// PASA Sentinel Autonomous Cyber-Forensic Terminal Ready.</div>
        <div class="term-line" style="color:var(--text-faint);">// Click any quick command above or type a command below.</div>
        <div class="term-line" style="color:var(--cyan);margin-top:10px;">[KERNEL] Device Owner initialized with supervisor clearance.</div>
        <div class="term-line term-success">[ASTRA] Hardware Keystore enrolled with StrongBox TEE.</div>
      </div>

      <div class="terminal-input-row">
        <span class="term-prompt">sentinel@pasa:~$</span>
        <input type="text" id="termInput" class="term-input" placeholder="Type a command (e.g. /locate, /snap, /usb_lock on)..." onkeydown="handleTermKey(event)">
        <button onclick="submitTermInput()" class="term-chip" style="color:var(--cyan);border-color:var(--cyan);padding:6px 14px;">ENTER ↵</button>
      </div>
    </div>
  </div>
</section>

<!-- ── CRIME SCENE CASES / INVESTIGATION SCENARIOS ── -->
<section id="scenarios" style="padding: 90px 0;">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="case_section_tag">TACTICAL INCIDENT LOGS // CID DOSSIERS</span>
      <h2 class="section-title" data-i18n="case_section_title">Real Theft Attacks. Lethal Defense Countermeasures.</h2>
      <p class="section-lede" data-i18n="case_section_lede">
        Standard apps fail the moment a thief touches airplane mode or power-off. Here is how PASA Sentinel turns the hunter into the hunted.
      </p>
    </div>

    <div class="cases-grid">
      <!-- Case 01 -->
      <div class="case-card danger">
        <div class="case-badge-row">
          <span class="case-id">CASE #01 // SNATCH</span>
          <span class="case-threat"><span class="pulse-dot" style="background:var(--crimson);"></span> THREAT: HIGH SPEED</span>
        </div>
        <h3 class="case-title" data-i18n="case1_title">The Street Snatch &amp; 2.65G Shockwave</h3>
        <p class="case-desc" data-i18n="case1_desc">
          A thief on a speeding motorbike violently grabs the handset from your fingers and accelerates away into heavy traffic.
        </p>
        <div class="case-counter">
          <div class="case-counter-title">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            <span data-i18n="case1_resp_title">PASA AUTONOMOUS REACTION</span>
          </div>
          <div class="case-counter-desc" data-i18n="case1_resp_desc">
            Accelerometer vector evaluates √(x²+y²+z²) &gt; 26.0 m/s². Within 30ms, PASA clamps screen into Knox Kiosk, disables status bar, snaps perpetrator's face, and dispatches GPS beacons to Telegram.
          </div>
        </div>
      </div>

      <!-- Case 02 -->
      <div class="case-card">
        <div class="case-badge-row">
          <span class="case-id">CASE #02 // SIM TRAP</span>
          <span class="case-threat" style="color:var(--cyan);"><span class="pulse-dot" style="background:var(--cyan);box-shadow:0 0 8px var(--cyan);"></span> THREAT: ISOLATION</span>
        </div>
        <h3 class="case-title" data-i18n="case2_title">The SIM Tray Ejection &amp; Foreign SIM Trap</h3>
        <p class="case-desc" data-i18n="case2_desc">
          The perpetrator ejects your SIM card to kill 4G data, then later inserts their own SIM card to test if the phone works.
        </p>
        <div class="case-counter">
          <div class="case-counter-title">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            <span data-i18n="case2_resp_title">PASA AUTONOMOUS REACTION</span>
          </div>
          <div class="case-counter-desc" data-i18n="case2_resp_desc">
            SIM ejection instantly triggers Kiosk lockdown. When the thief inserts their foreign SIM, PASA uses that SIM's radio to secretly text the owner's emergency contact—instantly revealing the thief's phone number via Caller ID!
          </div>
        </div>
      </div>

      <!-- Case 03 -->
      <div class="case-card danger">
        <div class="case-badge-row">
          <span class="case-id">CASE #03 // LAB ATTACK</span>
          <span class="case-threat"><span class="pulse-dot" style="background:var(--crimson);"></span> THREAT: BOOTROM FLASH</span>
        </div>
        <h3 class="case-title" data-i18n="case3_title">The Underground Lab &amp; EDL 9008 Attack</h3>
        <p class="case-desc" data-i18n="case3_desc">
          The phone is brought to a technician to short motherboard test points, enter Qualcomm EDL or MediaTek BROM mode, and flash custom firmware.
        </p>
        <div class="case-counter">
          <div class="case-counter-title">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            <span data-i18n="case3_resp_title">PASA AUTONOMOUS REACTION</span>
          </div>
          <div class="case-counter-desc" data-i18n="case3_resp_desc">
            Blower heat-gun loosening glue triggers the Thermal Anomaly Trap (&gt;48°C) killing USB data pins. The Dead Man's Switch local hardware timer auto-destructs Keystores, rendering personal data 100% irrecoverable.
          </div>
        </div>
      </div>

      <!-- Case 04 -->
      <div class="case-card">
        <div class="case-badge-row">
          <span class="case-id">CASE #04 // DURESS</span>
          <span class="case-threat" style="color:var(--amber);"><span class="pulse-dot" style="background:var(--amber);box-shadow:0 0 8px var(--amber);"></span> THREAT: COERCION</span>
        </div>
        <h3 class="case-title" data-i18n="case4_title">Armed Duress &amp; Sterile Decoy Sandbox</h3>
        <p class="case-desc" data-i18n="case4_desc">
          You are cornered at gunpoint or weapon-point and physically forced to unlock the device under immediate threat of violence.
        </p>
        <div class="case-counter">
          <div class="case-counter-title">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            <span data-i18n="case4_resp_title">PASA AUTONOMOUS REACTION</span>
          </div>
          <div class="case-counter-desc" data-i18n="case4_resp_desc">
            Enter your secret Decoy Duress PIN. The phone unlocks into an authentic empty sandbox: banking apps and crypto wallets instantly vanish via Device Owner, while silent mugshots and SOS beacons transmit to Telegram.
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- ── 86 C2 COMMANDS HUB CONSOLE ── -->
<section id="commands" class="c2-section">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="c2_section_tag">6-HUB INTERACTIVE TELEGRAM C2 CONSOLE</span>
      <h2 class="section-title" data-i18n="c2_section_title">86 Modular Telegram C2 Directives</h2>
      <p class="section-lede" data-i18n="c2_section_lede">
        Complete sovereign control from your personal Telegram bot or air-gapped cellular SMS. Categorized across 6 tactical defense hubs.
      </p>
    </div>

    <!-- Hub Navigation Tabs -->
    <div class="hub-tabs">
      <button class="hub-tab active" onclick="switchHub('hub-loc')">📍 <span data-i18n="hub_loc">Location &amp; RF</span> <span class="hub-pill-badge">7</span></button>
      <button class="hub-tab" onclick="switchHub('hub-cam')">📸 <span data-i18n="hub_cam">Covert Forensics</span> <span class="hub-pill-badge">13</span></button>
      <button class="hub-tab" onclick="switchHub('hub-lock')">🚨 <span data-i18n="hub_lock">Lockdown &amp; Siren</span> <span class="hub-pill-badge">12</span></button>
      <button class="hub-tab" onclick="switchHub('hub-owner')">👑 <span data-i18n="hub_owner">Knox Device Owner</span> <span class="hub-pill-badge">19</span></button>
      <button class="hub-tab" onclick="switchHub('hub-trap')">🛡️ <span data-i18n="hub_trap">Sensor Traps</span> <span class="hub-pill-badge">10</span></button>
      <button class="hub-tab" onclick="switchHub('hub-sys')">📇 <span data-i18n="hub_sys">Extraction &amp; Telephony</span> <span class="hub-pill-badge">25</span></button>
    </div>

    <!-- Hub Panels -->
    <!-- 1. Location -->
    <div id="hub-loc" class="c2-hub-panel active">
      <div class="cmd-chips-grid">
        <div class="cmd-chip"><div class="cmd-name">/locate <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_locate">Forces satellite GNSS hardware active and returns live Google Maps pin.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/tower <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_tower">Triangulates indoor location via MCC, MNC, LAC/TAC and 4G/5G Cell-IDs.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/sim <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_sim">Scans active SIM slots, carrier operators, signal strengths, and IMSI status.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/sim_lock <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_sim_lock">Configures emergency outbound SMS alert recipient upon unauthorized SIM insertion.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/track <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_track">Initiates recurring high-frequency GPS tracking stream with live breadcrumbs.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/track_stop <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_track_stop">Terminates active tracking session and releases GNSS wake-locks.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/geofence <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_geofence">Establishes hardware perimeter fence; triggers alarm if perimeter breached.</div></div>
      </div>
    </div>

    <!-- 2. Forensics -->
    <div id="hub-cam" class="c2-hub-panel">
      <div class="cmd-chips-grid">
        <div class="cmd-chip"><div class="cmd-name">/snap [front|back] <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_snap">Zero-blackout headless camera snapshot. Zero shutter sound, zero flash.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/screenshot <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_screenshot">Non-intrusive full display screen capture via Accessibility Service.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/screen_burst <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_screen_burst">Captures 5-shot high-speed sequential burst stitched into a composite forensic grid.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/screenrecord <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_screenrecord">Records covert MP4 video of the thief using your phone's display.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/video [front|back] <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_video">Stealth 10-60 second video recording using front or back lenses.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/record &lt;sec&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_record">16kHz AAC high-fidelity ambient microphone wiretap.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/livestream <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_livestream">Initiates recurring live photo/video telemetry loop into Telegram chat.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/stopstream <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_stopstream">Halts ongoing livestream and frees hardware camera pipelines.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/clipboard <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_clipboard">Extracts current text copied to the Android system clipboard buffer.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/gallery_latest <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_gallery_latest">Transmits the last 5 photos stored in the device's internal camera roll.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/getfile &lt;path&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_getfile">Downloads any confidential file up to 50MB directly to your Telegram.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/list_files &lt;dir&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_list_files">Explores storage directories and inspects filesystem folders remotely.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/livestream_diag <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_livestream_diag">Diagnostics for CameraX frame pipelines and hardware encoder limits.</div></div>
      </div>
    </div>

    <!-- 3. Lockdown -->
    <div id="hub-lock" class="c2-hub-panel">
      <div class="cmd-chips-grid">
        <div class="cmd-chip"><div class="cmd-name">/lock <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_lock">Engages instant Knox Kiosk Lost Mode with 0-feature lock task restrictions.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/lock_pin &lt;pin&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_lock_pin">Deploys custom interactive PIN keypad challenge to unlock the screen.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/set_os_pin &lt;pin&gt; <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_set_os_pin">Hardware Escrow Token PIN reset. Overrides Android lockscreen PIN at TEE level.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/unlock <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_unlock">Dismisses Kiosk Lost Mode and restores genuine owner access.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/fakeshutdown <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_fakeshutdown">Simulates authentic OEM power-down animation into 0-nit pitch black screen canvas.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/wake <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_wake">Dismisses Fake Shutdown black canvas and wakes the display.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/ring [sec] <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_ring">Forces 100% hardware audio volume siren that pierces Do-Not-Disturb modes.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/ring_stop <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_ring_stop">Silences ongoing acoustic alert.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/vibrate_pulse <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_vibrate_pulse">Tactile covert vibration pulses (SOS, continuous, rhythmic) without loud audio.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/message &lt;txt&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_message">Pins a full-screen unclosable emergency message broadcast across display.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/lockscreen_info <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_lockscreen_info">Sets immutable emergency owner return contact info on Android lockscreen.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/autolock &lt;ms&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_autolock">Enforces aggressive lockscreen timeout policy upon screen inactivity.</div></div>
      </div>
    </div>

    <!-- 4. Device Owner -->
    <div id="hub-owner" class="c2-hub-panel">
      <div class="cmd-chips-grid">
        <div class="cmd-chip"><div class="cmd-name">/antitamper <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_antitamper">Enforces DISALLOW_SAFE_BOOT, DISALLOW_AIRPLANE_MODE, and blocks factory resets.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/usb_lock <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_usb_lock">Hardware USB data pin killswitch. Neutralizes Cellebrite, GrayKey, and BadUSB.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/camera_lock <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_camera_lock">Hardware camera killswitch. Blocks all cameras system-wide at HAL level.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/bluetooth_lock <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_bluetooth_lock">Deactivates and blocks all Bluetooth pairings and wireless file transfers.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/mic_mute <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_mic_mute">Hardware microphone mute. Suppresses all system audio input.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/freeze &lt;pkg&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_freeze">Shadow App Vault: Completely conceals banking and private apps from launcher.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/unfreeze &lt;pkg&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_unfreeze">Restores frozen applications with 100% of data intact.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/frozen <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_frozen">Lists all currently hidden applications in the Shadow Vault.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/biometrics [off|on] <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_biometrics">Deactivates fingerprint and 3D face recognition on lockscreen to prevent coercion.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/dns &lt;host&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_dns">Enforces system-wide encrypted DNS-over-TLS (Quad9/Cloudflare) across all networks.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/app_firewall <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_app_firewall">OS network isolation firewall. Cuts off internet traffic for suspicious RATs or apps.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/notification [hide|show] <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_notification">Permanent notification tray suppression on Android 13+. Ghost status.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/self_heal <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_self_heal">Re-locks Camera, GPS, Mic, SMS permissions as permanent "Managed by Organization".</div></div>
        <div class="cmd-chip"><div class="cmd-name">/wifi_connect <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_wifi_connect">Provisions and connects device to specified Wi-Fi network while locked.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/security_audit <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_security_audit">Inspects low-level Linux kernel SecurityLog for ADB shell connections and tampering.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/app_uninstall &lt;pkg&gt; <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_app_uninstall">Silent PackageInstaller uninstaller. Removes apps without user confirmation.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/lock_app &lt;pkg&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_lock_app">Pins immediate security challenge before specific app can be opened.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/unlock_app &lt;pkg&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_unlock_app">Releases app challenge.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/device_owner <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_device_owner">Verifies active Knox Device Owner supervisor status and enrolled policies.</div></div>
      </div>
    </div>

    <!-- 5. Traps -->
    <div id="hub-trap" class="c2-hub-panel">
      <div class="cmd-chips-grid">
        <div class="cmd-chip"><div class="cmd-name">/trap snatch [on|off] <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_trap_snatch">Evaluates 2.65G violent acceleration vectors. Immediate kiosk lock upon theft.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/trap pocket [on|off] <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_trap_pocket">Proximity sensor extraction trap. Triggers alert if removed from pocket unauthorized.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/trap charger [on|off] <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_trap_charger">Triggers instant siren and mugshot capture if charging cable is detached.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/thermal <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_thermal">Heat-Gun Anomaly Trap. Detects heat (&gt;48°C) from technician blowers in lab attacks.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/deadman <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_deadman">Anti-EDL/BROM Dead Man's Switch. Hardware countdown timer executes crypto wipe if isolated.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/duress_pin <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_duress_pin">Configures Decoy Duress PIN to unlock sterile decoy OS under weapon-point threat.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/pattern_guard <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_pattern_guard">Captures covert front-facing perpetrator selfie after 3 failed PIN attempts.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/shred &lt;path&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_shred">Executes DoD 5220.22-M multi-pass cryptographic file destruction.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/battery_alert <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_battery_alert">Proactive battery telemetry alerts owner on critical drain or charger disconnect.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/harden_boot <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_harden_boot">Permanently locks OEM Bootloader flashing and blocks Fastboot unlock exploits.</div></div>
      </div>
    </div>

    <!-- 6. Extraction & Maintenance -->
    <div id="hub-sys" class="c2-hub-panel">
      <div class="cmd-chips-grid">
        <div class="cmd-chip"><div class="cmd-name">/call &lt;number&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_call">Silently dials out emergency voice phone call to owner's monitoring handset.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/sendsms &lt;num&gt; &lt;msg&gt; <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_sendsms">Dispatches outbound SMS directly via cellular radio. Reveals device phone number.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/contacts <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_contacts">Exports contacts database for remote backup after theft.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/call_log <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_call_log">Transmits incoming, outgoing, and missed call telephony records.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/sms_log <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_sms_log">Extracts incoming SMS inbox telemetry.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/status <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_status">Comprehensive health report: Battery, network, TEE hardware, license, services.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/selftest <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_selftest">Performs 12-point automated diagnostic self-test across all subsystem sensors.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/reboot <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_reboot">Device Owner privileged hardware reboot command.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/network <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_network">Detailed Wi-Fi SSID, cellular radio, and public IP network telemetry.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/apps <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_apps">Enumerates all installed third-party and system application packages.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/smssetup <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_smssetup">Generates QR code for RFC 6238 TOTP enrollment with Google Authenticator.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/sms_help <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_sms_help">Air-gapped cellular SMS cheatsheet with 1-tap copyable command templates.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/license <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_license">Inspects cryptographic Ed25519 digital certificate status and active tier.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/check_update <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_check_update">Checks VPS control plane for signed OTA APK updates.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/update_confirm <span class="cmd-scope">BOT</span></div><div class="cmd-desc" data-i18n="c_update_confirm">Self-downloads and installs authenticated OTA APK silently via Device Owner.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/wipe <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_wipe">Requests two-stage confirmation challenge before factory wipe.</div></div>
        <div class="cmd-chip"><div class="cmd-name">/wipe_confirm &lt;PIN&gt; <span class="cmd-scope">SMS/BOT</span></div><div class="cmd-desc" data-i18n="c_wipe_confirm">Executes hardware cryptographic wipe, destroying all data and keys instantly.</div></div>
      </div>
    </div>
  </div>
</section>

<!-- ── INTERACTIVE VULNERABILITY AUDIT ── -->
<section id="audit" class="audit-section">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="audit_section_tag">DEVICE SECURITY DIAGNOSTIC</span>
      <h2 class="section-title" data-i18n="audit_section_title">Test Your Handset's Theft Vulnerability</h2>
      <p class="section-lede" data-i18n="audit_section_lede">
        Answer 4 questions about your phone's current settings to calculate your real-world vulnerability index.
      </p>
    </div>

    <div class="audit-box">
      <div class="audit-item">
        <div class="audit-q" data-i18n="audit_q1">1. Can anyone swipe down your notification shade from the lockscreen and turn on Airplane Mode?</div>
        <div class="audit-toggle">
          <button class="audit-btn active yes" onclick="setAudit(1, true, this)">YES</button>
          <button class="audit-btn no" onclick="setAudit(1, false, this)">NO</button>
        </div>
      </div>
      <div class="audit-item">
        <div class="audit-q" data-i18n="audit_q2">2. Can someone press and hold your power button to completely shut down your phone without your PIN?</div>
        <div class="audit-toggle">
          <button class="audit-btn active yes" onclick="setAudit(2, true, this)">YES</button>
          <button class="audit-btn no" onclick="setAudit(2, false, this)">NO</button>
        </div>
      </div>
      <div class="audit-item">
        <div class="audit-q" data-i18n="audit_q3">3. If a thief ejects your SIM card, do you lose all ability to track or communicate with the device?</div>
        <div class="audit-toggle">
          <button class="audit-btn active yes" onclick="setAudit(3, true, this)">YES</button>
          <button class="audit-btn no" onclick="setAudit(3, false, this)">NO</button>
        </div>
      </div>
      <div class="audit-item">
        <div class="audit-q" data-i18n="audit_q4">4. If forced at weapon-point to unlock your screen, will your banking apps (bKash/crypto) be visible?</div>
        <div class="audit-toggle">
          <button class="audit-btn active yes" onclick="setAudit(4, true, this)">YES</button>
          <button class="audit-btn no" onclick="setAudit(4, false, this)">NO</button>
        </div>
      </div>

      <div class="audit-result-bar">
        <div>
          <div style="font-family:var(--mono);font-size:11px;color:var(--text-faint);text-transform:uppercase;" data-i18n="audit_risk_label">CURRENT THEFT VULNERABILITY INDEX:</div>
          <div class="audit-score-num" id="auditScore">94% CRITICAL RISK</div>
        </div>
        <a href="#pricing" class="btn-primary" style="padding:10px 20px;font-size:13.5px;" data-i18n="audit_cta">
          Neutralize Vulnerability with PASA
        </a>
      </div>
    </div>
  </div>
</section>

<!-- ── MASTER FAQ SECTION (Accordion + Search + Filter) ── -->
<section id="faq" class="faq-section">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="faq_section_tag">MASTER DEFENSE KNOWLEDGEBASE</span>
      <h2 class="section-title" data-i18n="faq_section_title">Comprehensive Forensic &amp; Technical FAQ</h2>
      <p class="section-lede" data-i18n="faq_section_lede">
        Every skeptical question answered. From chip-level EDL/BROM attacks to legal police admissibility and zero-storage privacy guarantees.
      </p>
    </div>

    <div class="faq-filter-bar">
      <!-- Search Input -->
      <div class="faq-search-box">
        <svg class="faq-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input type="text" id="faqSearch" class="faq-search-input" placeholder="Search technical questions (e.g. 'EDL', 'SIM', 'bKash', 'Battery', 'Cellebrite')..." oninput="filterFaq()">
      </div>

      <!-- Category Filter Pills -->
      <div class="faq-cat-pills">
        <button class="cat-pill active" onclick="filterCategory('all', this)" data-i18n="cat_all">All Categories</button>
        <button class="cat-pill" onclick="filterCategory('general', this)" data-i18n="cat_general">General</button>
        <button class="cat-pill" onclick="filterCategory('privacy', this)" data-i18n="cat_privacy">Zero-Storage &amp; Privacy</button>
        <button class="cat-pill" onclick="filterCategory('device-owner', this)" data-i18n="cat_owner">Device Owner</button>
        <button class="cat-pill" onclick="filterCategory('sim', this)" data-i18n="cat_sim">SIM Defense</button>
        <button class="cat-pill" onclick="filterCategory('hardware', this)" data-i18n="cat_hardware">Extreme Hardware &amp; EDL</button>
        <button class="cat-pill" onclick="filterCategory('lockscreen', this)" data-i18n="cat_lockscreen">Escrow PIN</button>
        <button class="cat-pill" onclick="filterCategory('forensics', this)" data-i18n="cat_forensics">Covert Forensics</button>
        <button class="cat-pill" onclick="filterCategory('vault', this)" data-i18n="cat_vault">Shadow Vault</button>
        <button class="cat-pill" onclick="filterCategory('sms', this)" data-i18n="cat_sms">Air-Gapped SMS</button>
        <button class="cat-pill" onclick="filterCategory('traps', this)" data-i18n="cat_traps">Sensor Traps</button>
        <button class="cat-pill" onclick="filterCategory('battery', this)" data-i18n="cat_battery">Battery &amp; OS</button>
        <button class="cat-pill" onclick="filterCategory('legal', this)" data-i18n="cat_legal">Legal &amp; Police</button>
      </div>

      <div style="display:flex;justify-content:flex-end;gap:12px;margin-top:4px;">
        <button onclick="toggleAllFaq(true)" style="background:none;border:none;color:var(--cyan);font-family:var(--mono);font-size:11.5px;cursor:pointer;" data-i18n="btn_expand_all">[ + Expand All ]</button>
        <button onclick="toggleAllFaq(false)" style="background:none;border:none;color:var(--text-dim);font-family:var(--mono);font-size:11.5px;cursor:pointer;" data-i18n="btn_collapse_all">[ - Collapse All ]</button>
      </div>
    </div>

    <!-- Accordion Container (Dynamic JS Render) -->
    <div class="faq-accordion" id="faqAccordion">
      <!-- Injected via JavaScript from faqData -->
    </div>
  </div>
</section>

<!-- ── PRICING & LICENSING DOSSIER ── -->
<section id="pricing" class="pricing-section">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="pricing_section_tag">SOVEREIGN COMMERCIAL LICENSING</span>
      <h2 class="section-title" data-i18n="pricing_section_title">Zero Recurring Traps. Permanent Ownership.</h2>
      <p class="section-lede" data-i18n="pricing_section_lede">
        We do not believe in predatory monthly subscriptions. Secure your hardware once with Ed25519 cryptographic certification.
      </p>
    </div>

    <div class="pricing-grid">
      <!-- Trial -->
      <div class="pricing-card">
        <div>
          <h3 class="pricing-plan" data-i18n="plan_eval_title">Tactical Evaluation</h3>
          <p class="pricing-desc" data-i18n="plan_eval_desc">Test core telemetry and verification on your personal hardware.</p>
          <div class="pricing-price">FREE</div>
          <div class="pricing-period" data-i18n="plan_eval_period">3-Day Evaluation Period</div>
          <ul class="pricing-features">
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_eval_1">Essential Telegram C2 Commands</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_eval_2">Headless Camera Capture Test</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_eval_3">GPS &amp; Cell Tower Telemetry</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_eval_4">Community Telegram Support</span></li>
          </ul>
        </div>
        <a href="https://t.me/Pas_agent_bot" target="_blank" class="btn-secondary" style="justify-content:center;font-size:13.5px;" data-i18n="btn_start_eval">
          Activate via Bot
        </a>
      </div>

      <!-- Pro Lifetime (Featured) -->
      <div class="pricing-card featured">
        <div class="pricing-featured-badge" data-i18n="badge_most_popular">MOST POPULAR DEFENSE</div>
        <div>
          <h3 class="pricing-plan" style="color:var(--crimson);" data-i18n="plan_pro_title">Pro Lifetime Shield</h3>
          <p class="pricing-desc" data-i18n="plan_pro_desc">Complete sovereign defense suite for 1 Android device forever.</p>
          <div class="pricing-price">$25 <span class="pricing-currency">/ ৳3,000</span></div>
          <div class="pricing-period" data-i18n="plan_pro_period">One-time payment • Lifetime OTA Updates</div>
          <ul class="pricing-features">
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <strong><span data-i18n="f_pro_1">All 86 Telegram C2 Commands</span></strong></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <strong><span data-i18n="f_pro_2">Knox-Grade Device Owner Provisioning</span></strong></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_pro_3">Hardware Escrow Token PIN Reset</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_pro_4">Anti-EDL/BROM Dead Man's Switch</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_pro_5">SIM Ejection Foreign Number Trap</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_pro_6">1-on-1 Personal Remote Setup Onboarding</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <strong><span data-i18n="f_pro_7">24-Hour 100% Refund Guarantee</span></strong></li>
          </ul>
        </div>
        <a href="https://wa.me/8801762033445?text=Hello%20PASA%20Team%2C%20I%20want%20to%20activate%20PASA%20Pro%20Lifetime" target="_blank" class="btn-primary" style="justify-content:center;font-size:14px;" data-i18n="btn_buy_pro">
          Claim Lifetime License
        </a>
      </div>

      <!-- Enterprise Fleet -->
      <div class="pricing-card">
        <div>
          <h3 class="pricing-plan" data-i18n="plan_ent_title">Enterprise Fleet</h3>
          <p class="pricing-desc" data-i18n="plan_ent_desc">VIP executive defense, corporate fleets, and high-risk field agents.</p>
          <div class="pricing-price">$99 <span class="pricing-currency">/ ৳11,500</span></div>
          <div class="pricing-period" data-i18n="plan_ent_period">5 Devices Pack • Dedicated Control Node</div>
          <ul class="pricing-features">
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_ent_1">5x Pro Lifetime Device Licenses</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_ent_2">Dedicated Private Relay Server Node</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_ent_3">Zero-Knowledge Fleet Management</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_ent_4">Direct WhatsApp &amp; Telegram Hotline</span></li>
          </ul>
        </div>
        <a href="https://wa.me/8801762033445?text=Hello%20PASA%20Team%2C%20I%20am%20interested%20in%20Enterprise%20Fleet" target="_blank" class="btn-secondary" style="justify-content:center;font-size:13.5px;" data-i18n="btn_contact_ent">
          Contact Concierge
        </a>
      </div>
    </div>

    <!-- Payment Channels Details Strip (Untouched as requested) -->
    <div class="payment-channels">
      <!-- Binance Pay -->
      <div class="pay-method">
        <div class="pay-icon" style="color:#F0B90B;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L4 6v12l8 4 8-4V6l-8-4zm0 2.24l5.88 2.94v9.64L12 19.76l-5.88-2.94V7.18L12 4.24z"/></svg>
        </div>
        <div style="flex:1;">
          <div class="pay-title">Binance Pay (Crypto)</div>
          <div class="pay-sub" id="binanceUidText">UID: 756303714 (RBR48)</div>
          <button onclick="copyBinanceUid()" class="pay-action-btn" style="background:rgba(240,185,11,0.15);color:#F0B90B;border:1px solid rgba(240,185,11,0.3);cursor:pointer;" data-i18n="btn_copy_uid">
            Copy UID
          </button>
        </div>
      </div>

      <!-- bKash (Number removed, asking to contact) -->
      <div class="pay-method">
        <div class="pay-icon" style="color:#E2136E;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="10"/><path d="M8 12l3 3 5-5" stroke="#fff" stroke-width="2" fill="none"/></svg>
        </div>
        <div style="flex:1;">
          <div class="pay-title" data-i18n="bkash_title">bKash Payment (Bangladesh)</div>
          <div class="pay-sub" style="color:var(--amber);" data-i18n="bkash_sub">Contact for official bKash account</div>
          <a href="https://wa.me/8801762033445?text=Hello%20PASA%2C%20please%20send%20me%20the%20official%20bKash%20payment%20number%20for%20license%20activation" target="_blank" class="pay-action-btn" style="background:rgba(226,19,110,0.15);color:#E2136E;border:1px solid rgba(226,19,110,0.3);" data-i18n="bkash_action_btn">
            Request Number via WhatsApp
          </a>
        </div>
      </div>

      <!-- WhatsApp Concierge -->
      <div class="pay-method">
        <div class="pay-icon" style="color:#25D366;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91c0 1.87.52 3.63 1.42 5.14L2 22l5.09-1.53a9.87 9.87 0 004.95 1.32h.01c5.46 0 9.9-4.45 9.9-9.9C21.95 6.45 17.5 2 12.04 2z"/></svg>
        </div>
        <div style="flex:1;">
          <div class="pay-title" data-i18n="wa_title">WhatsApp Concierge</div>
          <div class="pay-sub">+880 1762-033445</div>
          <a href="https://wa.me/8801762033445" target="_blank" class="pay-action-btn" style="background:rgba(37,211,102,0.15);color:#25D366;border:1px solid rgba(37,211,102,0.3);" data-i18n="wa_action_btn">
            Chat Direct
          </a>
        </div>
      </div>

      <!-- Telegram Bot -->
      <div class="pay-method">
        <div class="pay-icon" style="color:#229ed9;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0z"/></svg>
        </div>
        <div style="flex:1;">
          <div class="pay-title">Official Telegram Bot</div>
          <div class="pay-sub">@Pas_agent_bot</div>
          <a href="https://t.me/Pas_agent_bot" target="_blank" class="pay-action-btn" style="background:rgba(34,158,217,0.15);color:#229ed9;border:1px solid rgba(34,158,217,0.3);">
            Open Bot
          </a>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- Toast for Copy Notification -->
<div id="toast" class="toast">COPIED TO CLIPBOARD</div>

<!-- FOOTER -->
<footer>
  <div class="wrap">
    <div class="footer-grid">
      <div class="footer-col">
        <div class="brand" style="margin-bottom:14px;">
          <div class="brand-badge">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
          </div>
          <div class="brand-title">
            <span>PASA SENTINEL</span>
            <span class="brand-sub">Sovereign Defense Systems</span>
          </div>
        </div>
        <p style="font-size:13.5px;color:var(--text-dim);line-height:1.6;max-width:340px;" data-i18n="footer_bio">
          Sovereign Android anti-theft defense &amp; covert intelligence agent. Zero Google Play dependencies, zero cloud media storage, Knox-grade uninstallation lockout. Engineered by Izhaan Intellect.
        </p>
        <div style="margin-top:14px;font-family:var(--mono);font-size:11px;color:var(--cyan);">
          SHA-256: 0c8f62dd8934d3b73e12d965742da29e643bdc157bc859e5b6aa7454409ad57a
        </div>
      </div>

      <div class="footer-col">
        <h5 data-i18n="footer_resources">Resources</h5>
        <ul>
          <li><a href="/releases/pasa-latest.apk" style="color:var(--crimson);font-weight:700;" data-i18n="footer_dl_apk">Download Latest APK (v3.5.1)</a></li>
          <li><a href="#matrix" data-i18n="nav_matrix">Defense Comparison Matrix</a></li>
          <li><a href="#timeline" data-i18n="nav_timeline">10-Second Thief Timeline</a></li>
          <li><a href="#simulator" data-i18n="nav_terminal">C2 Terminal Simulator</a></li>
          <li><a href="#scenarios" data-i18n="nav_cases">Crime Cases</a></li>
          <li><a href="#commands" data-i18n="nav_c2">86 C2 Commands</a></li>
          <li><a href="#faq" data-i18n="nav_faq">Master Technical FAQ</a></li>
          <li><a href="#pricing" data-i18n="nav_pricing">Commercial Licensing</a></li>
        </ul>
      </div>

      <div class="footer-col">
        <h5 data-i18n="footer_legal">Contact &amp; Legal</h5>
        <ul>
          <li><a href="mailto:support@izhaanintellect.fun">support@izhaanintellect.fun</a></li>
          <li><a href="https://wa.me/8801762033445" target="_blank">WhatsApp: +880 1762-033445</a></li>
          <li><a href="https://t.me/Pas_agent_bot" target="_blank">Telegram: @Pas_agent_bot</a></li>
          <li><a href="/terms" target="_blank">Terms of Service &amp; EULA</a></li>
          <li><a href="/privacy" target="_blank">Privacy &amp; Zero-Storage Policy</a></li>
        </ul>
      </div>
    </div>

    <div class="footer-bottom">
      <div>&copy; 2026 Izhaan Intellect &amp; PASA Sentinel. All rights reserved. Strategy 1 Zero-Storage Architecture.</div>
      <div style="display:flex;gap:16px;">
        <a href="/terms" style="color:var(--text-faint);text-decoration:none;">Terms &amp; EULA</a>
        <a href="/privacy" style="color:var(--text-faint);text-decoration:none;">Privacy Policy</a>
        <a href="https://github.com/rbr48/pasa" target="_blank" style="color:var(--text-faint);text-decoration:none;">GitHub</a>
      </div>
    </div>
  </div>
</footer>

<!-- Sleek, Non-Obstructive Floating Concierge Widget -->
<div class="floating-concierge">
  <div class="concierge-card" id="conciergeCard">
    <div style="font-family:var(--mono);font-size:11px;color:var(--text-faint);display:flex;justify-content:space-between;align-items:center;">
      <span>LIVE ASSISTANCE</span>
      <span onclick="toggleConcierge()" style="cursor:pointer;font-size:16px;">&times;</span>
    </div>
    <a href="https://wa.me/8801762033445" target="_blank" rel="noopener" class="concierge-link wa">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91c0 1.87.52 3.63 1.42 5.14L2 22l5.09-1.53a9.87 9.87 0 004.95 1.32h.01c5.46 0 9.9-4.45 9.9-9.9C21.95 6.45 17.5 2 12.04 2z"/></svg>
      <span>WhatsApp Concierge</span>
    </a>
    <a href="https://t.me/Pas_agent_bot" target="_blank" rel="noopener" class="concierge-link tg">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0z"/></svg>
      <span>Telegram Official Bot</span>
    </a>
  </div>
  <div class="concierge-pill" onclick="toggleConcierge()">
    <span class="pulse-dot" style="background:#25D366;box-shadow:0 0 8px #25D366;"></span>
    <span data-i18n="floating_help">Live Assistance</span>
  </div>
</div>

<!-- SCRIPTS & BILINGUAL LOGIC -->
<script>
const rawFaq = ${JSON.stringify(faqData)};

const translations = {
  en: {
    top_status: "PASA SENTINEL ONLINE",
    nav_matrix: "Defense Matrix",
    nav_timeline: "10s Defense",
    nav_terminal: "C2 Terminal",
    nav_faq: "Forensic FAQ",
    nav_pricing: "Licensing",
    nav_more: "More",
    nav_cases: "Crime Cases",
    nav_c2: "86 C2 Commands",
    nav_audit: "Vulnerability Audit",
    nav_download: "Download APK",

    hero_badge: "SOVEREIGN MOBILE DEFENSE // ANDROID 8.0 – 16",
    hero_title: "YOUR PHONE WILL NEVER BE SURRENDERED.",
    hero_sub: "When ordinary trackers go blind in 10 seconds, PASA Sentinel engages Knox-grade hardware lockdown, hunts the perpetrator's real identity, and protects your sovereign personal data.",
    hero_cta_apk: "Download Tactical APK (v3.5.1)",
    hero_cta_terminal: "Launch C2 Terminal",
    pillar_1: "Knox Device Owner (Immutable)",
    pillar_2: "Zero-Cloud Media Storage",
    pillar_3: "Air-Gapped Cellular SMS C2",

    stat_anti_uninstall: "UNINSTALLATION DEFENSE",
    stat_val_knox: "Knox Supervisor",
    stat_zero_storage: "SURVEILLANCE STORAGE",
    stat_val_zerostore: "0-Cloud Trails",
    stat_c2_channels: "C2 CONTROL CHANNELS",
    stat_hardware_escrow: "HARDWARE ESCROW",
    stat_val_strongbox: "StrongBox / TEE",

    term_section_tag: "LIVE CID INTELLIGENCE SIMULATOR",
    term_section_title: "Interactive C2 Command Console",
    term_section_lede: "Execute real defense commands and inspect live simulated hardware responses. See how PASA operates in active crime containment.",
    term_chips_label: "Quick Commands:",

    time_section_tag: "THE FIRST 10 SECONDS OF A THEFT",
    time_section_title: "Why Standard Handsets Fail in 10 Seconds",
    time_section_lede: "The moment a phone is stolen, the clock starts ticking. Here is the forensic difference between normal security and PASA Sentinel.",
    time_vuln_title: "Standard Android / Trackers",
    time_vuln_sub: "Google Find My Device & Play Store Apps",
    time_v1_title: "Notification Drawer Pulled Down",
    time_v1_desc: "Thief swipes down from lockscreen, toggles Airplane Mode. Internet severed. Google Find My Device is now completely blind.",
    time_v2_title: "Power Button Pressed & Held",
    time_v2_desc: "Thief shuts down handset. Operating system terminates. All standard apps and background location services go dead.",
    time_v3_title: "SIM Card Extracted & Discarded",
    time_v3_desc: "Physical SIM card thrown into gutter. Device has zero cellular network connectivity and cannot be phoned.",
    time_v4_title: "Safe Mode Reboot & Forensic Wipe",
    time_v4_desc: "Thief boots into Safe Mode or connects forensic cable. Play Store security apps are easily uninstalled or data extracted.",

    time_prot_title: "PASA Sentinel Autonomous Shield",
    time_prot_sub: "Knox Device Owner & Hardware Containment",
    time_p1_title: "2.65G Kinetic Snatch Triggered",
    time_p1_desc: "Violent vector acceleration locks screen into Knox Kiosk within 30ms. Status bar pull-down and Airplane Mode permanently blocked.",
    time_p2_title: "Fake Shutdown Deception",
    time_p2_desc: "Power button triggers authentic OEM power-down animation into 0-nit pitch black screen. Thief assumes phone is dead while cameras and GPS run.",
    time_p3_title: "Foreign SIM Caller ID Trap",
    time_p3_desc: "When thief inserts their SIM, PASA uses that SIM's radio to secretly text emergency contacts—revealing the thief's phone number on Caller ID!",
    time_p4_title: "Hardware USB & Thermal Killswitch",
    time_p4_desc: "USB Data Pin Killswitch severs physical D+/D- pins against forensic boxes. Blower heat-gun triggers Thermal Trap and Dead Man auto-destruct.",

    matrix_section_tag: "HARDWARE ARCHITECTURE COMPARISON",
    matrix_section_title: "Knox Device Owner vs Standard Apps",
    matrix_section_lede: "Why Google Play Store policies prevent ordinary apps from ever matching PASA Sentinel's supervisor privileges.",
    matrix_scroll_hint: "👉 Scroll horizontally to compare all platforms",
    th_feature: "Security Capability",
    th_google: "Google Find My Device",
    th_play: "Play Store Anti-Theft",
    mat_1_title: "Zero Root Requirement",
    mat_1_sub: "Retains full Knox & StrongBox Keystore integrity",
    mat_2_title: "Lockscreen Status Bar & Airplane Block",
    mat_2_sub: "Prevents quick settings offline tampering",
    mat_3_title: "Air-Gapped Cellular SMS C2 Fallback",
    mat_3_sub: "Operates when internet & Wi-Fi are disconnected",
    mat_4_title: "Remote Hardware Lockscreen PIN Reset",
    mat_4_sub: "Android 14, 15, 16 OS PIN override",
    mat_5_title: "Hardware USB Data Pin Killswitch",
    mat_5_sub: "Neutralizes Cellebrite & GrayKey extraction",
    mat_6_title: "Anti-Uninstall & Safe Mode Immunity",
    mat_6_sub: "Thief cannot delete or force-stop",
    mat_7_title: "Surveillance Privacy & Storage Policy",
    mat_7_sub: "Who sees your photos, audio, and GPS?",
    mat_8_title: "Pricing Model",
    mat_8_sub: "Recurring costs vs permanent sovereignty",

    audit_section_tag: "DEVICE SECURITY DIAGNOSTIC",
    audit_section_title: "Test Your Handset's Theft Vulnerability",
    audit_section_lede: "Answer 4 questions about your phone's current settings to calculate your real-world vulnerability index.",
    audit_q1: "1. Can anyone swipe down your notification shade from the lockscreen and turn on Airplane Mode?",
    audit_q2: "2. Can someone press and hold your power button to completely shut down your phone without your PIN?",
    audit_q3: "3. If a thief ejects your SIM card, do you lose all ability to track or communicate with the device?",
    audit_q4: "4. If forced at weapon-point to unlock your screen, will your banking apps (bKash/crypto) be visible?",
    audit_risk_label: "CURRENT THEFT VULNERABILITY INDEX:",
    audit_cta: "Neutralize Vulnerability with PASA",

    case_section_tag: "TACTICAL INCIDENT LOGS // CID DOSSIERS",
    case_section_title: "Real Theft Attacks. Lethal Defense Countermeasures.",
    case_section_lede: "Standard apps fail the moment a thief touches airplane mode or power-off. Here is how PASA Sentinel turns the hunter into the hunted.",
    case1_title: "The Street Snatch & 2.65G Shockwave",
    case1_desc: "A thief on a speeding motorbike violently grabs the handset from your fingers and accelerates away into heavy traffic.",
    case1_resp_title: "PASA AUTONOMOUS REACTION",
    case1_resp_desc: "Accelerometer vector evaluates √(x²+y²+z²) > 26.0 m/s². Within 30ms, PASA clamps screen into Knox Kiosk, disables status bar, snaps perpetrator's face, and dispatches GPS beacons to Telegram.",
    case2_title: "The SIM Tray Ejection & Foreign SIM Trap",
    case2_desc: "The perpetrator ejects your SIM card to kill 4G data, then later inserts their own SIM card to test if the phone works.",
    case2_resp_title: "PASA AUTONOMOUS REACTION",
    case2_resp_desc: "SIM ejection instantly triggers Kiosk lockdown. When the thief inserts their foreign SIM, PASA uses that SIM's radio to secretly text the owner's emergency contact—instantly revealing the thief's phone number via Caller ID!",
    case3_title: "The Underground Lab & EDL 9008 Attack",
    case3_desc: "The phone is brought to a technician to short motherboard test points, enter Qualcomm EDL or MediaTek BROM mode, and flash custom firmware.",
    case3_resp_title: "PASA AUTONOMOUS REACTION",
    case3_resp_desc: "Blower heat-gun loosening glue triggers the Thermal Anomaly Trap (>48°C) killing USB data pins. The Dead Man's Switch local hardware timer auto-destructs Keystores, rendering personal data 100% irrecoverable.",
    case4_title: "Armed Duress & Sterile Decoy Sandbox",
    case4_desc: "You are cornered at gunpoint or weapon-point and physically forced to unlock the device under immediate threat of violence.",
    case4_resp_title: "PASA AUTONOMOUS REACTION",
    case4_resp_desc: "Enter your secret Decoy Duress PIN. The phone unlocks into an authentic empty sandbox: banking apps and crypto wallets instantly vanish via Device Owner, while silent mugshots and SOS beacons transmit to Telegram.",

    c2_section_tag: "6-HUB INTERACTIVE TELEGRAM C2 CONSOLE",
    c2_section_title: "86 Modular Telegram C2 Directives",
    c2_section_lede: "Complete sovereign control from your personal Telegram bot or air-gapped cellular SMS. Categorized across 6 tactical defense hubs.",
    hub_loc: "Location & RF",
    hub_cam: "Covert Forensics",
    hub_lock: "Lockdown & Siren",
    hub_owner: "Knox Device Owner",
    hub_trap: "Sensor Traps",
    hub_sys: "Extraction & Telephony",

    faq_section_tag: "MASTER DEFENSE KNOWLEDGEBASE",
    faq_section_title: "Comprehensive Forensic & Technical FAQ",
    faq_section_lede: "Every skeptical question answered. From chip-level EDL/BROM attacks to legal police admissibility and zero-storage privacy guarantees.",
    faq_search_placeholder: "Search technical questions (e.g. 'EDL', 'SIM', 'bKash', 'Battery', 'Cellebrite')...",
    cat_all: "All Categories",
    cat_general: "General",
    cat_privacy: "Zero-Storage & Privacy",
    cat_owner: "Device Owner",
    cat_sim: "SIM Defense",
    cat_hardware: "Extreme Hardware & EDL",
    cat_lockscreen: "Escrow PIN",
    cat_forensics: "Covert Forensics",
    cat_vault: "Shadow Vault",
    cat_sms: "Air-Gapped SMS",
    cat_traps: "Sensor Traps",
    cat_battery: "Battery & OS",
    cat_legal: "Legal & Police",
    btn_expand_all: "[ + Expand All ]",
    btn_collapse_all: "[ - Collapse All ]",

    pricing_section_tag: "SOVEREIGN COMMERCIAL LICENSING",
    pricing_section_title: "Zero Recurring Traps. Permanent Ownership.",
    pricing_section_lede: "We do not believe in predatory monthly subscriptions. Secure your hardware once with Ed25519 cryptographic certification.",
    badge_most_popular: "MOST POPULAR DEFENSE",
    plan_eval_title: "Tactical Evaluation",
    plan_eval_desc: "Test core telemetry and verification on your personal hardware.",
    plan_eval_period: "3-Day Evaluation Period",
    f_eval_1: "Essential Telegram C2 Commands",
    f_eval_2: "Headless Camera Capture Test",
    f_eval_3: "GPS & Cell Tower Telemetry",
    f_eval_4: "Community Telegram Support",
    btn_start_eval: "Activate via Bot",

    plan_pro_title: "Pro Lifetime Shield",
    plan_pro_desc: "Complete sovereign defense suite for 1 Android device forever.",
    plan_pro_period: "One-time payment • Lifetime OTA Updates",
    f_pro_1: "All 86 Telegram C2 Commands",
    f_pro_2: "Knox-Grade Device Owner Provisioning",
    f_pro_3: "Hardware Escrow Token PIN Reset",
    f_pro_4: "Anti-EDL/BROM Dead Man's Switch",
    f_pro_5: "SIM Ejection Foreign Number Trap",
    f_pro_6: "1-on-1 Personal Remote Setup Onboarding",
    f_pro_7: "24-Hour 100% Refund Guarantee",
    btn_buy_pro: "Claim Lifetime License",

    plan_ent_title: "Enterprise Fleet",
    plan_ent_desc: "VIP executive defense, corporate fleets, and high-risk field agents.",
    plan_ent_period: "5 Devices Pack • Dedicated Control Node",
    f_ent_1: "5x Pro Lifetime Device Licenses",
    f_ent_2: "Dedicated Private Relay Server Node",
    f_ent_3: "Zero-Knowledge Fleet Management",
    f_ent_4: "Direct WhatsApp & Telegram Hotline",
    btn_contact_ent: "Contact Concierge",

    btn_copy_uid: "Copy UID",
    bkash_title: "bKash Payment (Bangladesh)",
    bkash_sub: "Contact for official bKash account",
    bkash_action_btn: "Request Number via WhatsApp",
    wa_title: "WhatsApp Concierge",
    wa_action_btn: "Chat Direct",
    floating_help: "Live Assistance",

    footer_bio: "Sovereign Android anti-theft defense & covert intelligence agent. Zero Google Play dependencies, zero cloud media storage, Knox-grade uninstallation lockout. Engineered by Izhaan Intellect.",
    footer_resources: "Resources",
    footer_legal: "Contact & Legal",
    footer_dl_apk: "Download Latest APK (v3.5.1)"
  },
  bn: {
    top_status: "পাসা সেন্টিনেল অনলাইন",
    nav_matrix: "সুরক্ষা তুলনা",
    nav_timeline: "১০ সে. ডিফেন্স",
    nav_terminal: "সি২ টার্মিনাল",
    nav_faq: "প্রশ্নোত্তর",
    nav_pricing: "লাইসেন্সিং",
    nav_more: "আরও",
    nav_cases: "ক্রাইম কেস",
    nav_c2: "৮৬টি সি২ হাব",
    nav_audit: "নিরাপত্তা পরীক্ষা",
    nav_download: "এপিকে ডাউনলোড",

    hero_badge: "সার্বভৌম মোবাইল ডিফেন্স // অ্যান্ড্রয়েড ৮.০ – ১৬",
    hero_title: "আপনার ফোন আর কখনোই চোরের কাছে আত্মসমর্পণ করবে না।",
    hero_sub: "চুরি হওয়ার ১০ সেকেন্ডের মধ্যে যখন সাধারণ ট্র্যাকার অন্ধ হয়ে যায়—পাসা সেন্টিনেল নক্স-গ্রেড হার্ডওয়্যার লকডাউন চাপিয়ে অপরাধীর আসল পরিচয় শিকার করে এবং আপনার ব্যক্তিগত ডেটা রক্ষা করে।",
    hero_cta_apk: "ট্যাকটিক্যাল এপিকে ডাউনলোড (v3.5.1)",
    hero_cta_terminal: "সি২ টার্মিনাল ওপেন করুন",
    pillar_1: "নক্স ডিভাইস ওনার (স্থায়ী আনইন্সটল প্রতিরোধ)",
    pillar_2: "জিরো-ক্লাউড মিডিয়া স্টোরেজ (১০০% প্রাইভেট)",
    pillar_3: "এয়ার-গ্যাপড সেলুলার এসএমএস সি২",

    stat_anti_uninstall: "স্থায়ী আনইন্সটল প্রতিরোধ",
    stat_val_knox: "নক্স সুপারভাইজার",
    stat_zero_storage: "সার্ভেইল্যান্স স্টোরেজ",
    stat_val_zerostore: "০-ক্লাউড ট্রেইল",
    stat_c2_channels: "সি২ নিয়ন্ত্রণ চ্যানেল",
    stat_hardware_escrow: "হার্ডওয়্যার এসক্রো",
    stat_val_strongbox: "স্ট্রংবক্স / টিইই",

    term_section_tag: "লাইভ সিআইডি ইন্টেলিজেন্স সিমুলেটর",
    term_section_title: "ইন্টারঅ্যাক্টিভ সি২ কমান্ড কনসোল",
    term_section_lede: "সরাসরি প্রতিরক্ষামূলক কমান্ড রান করে দেখুন কীভাবে পাসা হার্ডওয়্যার ও ওএস লেভেলে প্রতিটি হুমকি প্রতিহত করে।",
    term_chips_label: "কুইক কমান্ডস:",

    time_section_tag: "চুরির প্রথম ১০ সেকেন্ডের দৃশ্যপট",
    time_section_title: "কেন সাধারণ ফোন মাত্র ১০ সেকেন্ডে অন্ধ হয়ে যায়",
    time_section_lede: "ফোন চুরি হওয়া মাত্রই সেকেন্ডের খেলা শুরু হয়। দেখুন সাধারণ ফোনের সাথে পাসা সেন্টিনেলের কারিগরি পার্থক্য।",
    time_vuln_title: "সাধারণ অ্যান্ড্রয়েড ও ট্র্যাকার অ্যাপ",
    time_vuln_sub: "গুগল ফাইন্ড মাই ডিভাইস ও প্লে স্টোরের সাধারণ অ্যাপ",
    time_v1_title: "লকস্ক্রিন থেকে স্ট্যাটাস বার নামানো",
    time_v1_desc: "চোর স্ক্রিন সোয়াইপ করে এয়ারপ্লেন মোড অন করে দেয়। ইন্টারনেট বিচ্ছিন্ন হয়ে গুগল ফাইন্ড মাই ডিভাইস সম্পূর্ণ অকেজো হয়ে যায়।",
    time_v2_title: "পাওয়ার বাটন চেপে বন্ধ করে দেওয়া",
    time_v2_desc: "চোর পাওয়ার বাটন চেপে ফোন অফ করে দেয়। অপারেটিং সিস্টেম শাটডাউন হয়ে সমস্ত সাধারণ অ্যাপ বন্ধ হয়ে যায়।",
    time_v3_title: "সিম কার্ড খুলে ফেলে দেওয়া",
    time_v3_desc: "ফিজিক্যাল সিম খুলে ফেলে দিলে ফোনে সেলুলার ডাটা বন্ধ হয়ে যায় এবং ফোন আর কোনো কল বা এসএমএস পায় না।",
    time_v4_title: "সেফ মোডে বুট বা পিসিতে ডেটা ডাম্প",
    time_v4_desc: "চোর সেফ মোডে অন করে অ্যাপ আনইন্সটল করে দেয় অথবা ইউএসবি কেবল লাগিয়ে কম্পিউটারে ব্যক্তিগত তথ্য চুরি করে।",

    time_prot_title: "পাসা সেন্টিনেল অটোনোমাস শিল্ড",
    time_prot_sub: "নক্স ডিভাইস ওনার ও হার্ডওয়্যার কনটেইনমেন্ট",
    time_p1_title: "২.৬৫জি কাইনেটিক স্ন্যাচ ডিটেকশন",
    time_p1_desc: "হঠাৎ হ্যাঁচকা টান লাগামাত্রই ৩০ মিলিসেকেন্ডে কিয়স্ক লকডাউন। স্ট্যাটাস বার ড্রয়ার ও এয়ারপ্লেন মোড স্থায়ীভাবে ওএস লেভেলে ব্লক।",
    time_p2_title: "ফেক শাটডাউন বিভ্রান্তিকর ব্ল্যাকআউট",
    time_p2_desc: "পাওয়ার বাটন চাপলে আসল শাটডাউন অ্যানিমেশন দেখিয়ে ০-নিট কালো স্ক্রিনে চলে যায়। চোর মনে করে ফোন বন্ধ, অথচ ব্যাকগ্রাউন্ডে ক্যামেরা ও জিপিএস সচল।",
    time_p3_title: "চোরের নিজের সিম কার্ডের ফাঁদ",
    time_p3_desc: "চোর নিজের সিম কার্ড ঢোকানোমাত্রই পাসা ওই সিমের রেডিও দিয়ে স্বয়ংক্রিয় জরুরি এসএমএস পাঠায়—যাতে কলার আইডিতে চোরের নম্বর সরাসরি ফাঁস হয়ে যায়!",
    time_p4_title: "হার্ডওয়্যার ইউএসবি ও থার্মাল কিলসুইচ",
    time_p4_desc: "কম্পিউটারের কেবল লাগালেও ইউএসবি ডেটা পিন কিলসুইচ ডেটা লাইন বন্ধ রাখে। ব্যাক-কভার খুলতে হিটগান লাগালে ডেড ম্যানস সুইচ কি-স্টোর মুছে ফেলে।",

    matrix_section_tag: "হার্ডওয়্যার আর্কিটেকচার তুলনা",
    matrix_section_title: "নক্স ডিভাইস ওনার বনাম সাধারণ অ্যাপস",
    matrix_section_lede: "কেন গুগল প্লে স্টোরের পলিসির কারণে সাধারণ কোনো অ্যাপ কখনোই পাসা সেন্টিনেলের সমকক্ষ হতে পারে না।",
    matrix_scroll_hint: "👉 সম্পূর্ণ তুলনা দেখতে ডানে-বামে সোয়াইপ করুন",
    th_feature: "নিরাপত্তা ফিচার",
    th_google: "গুগল ফাইন্ড মাই ডিভাইস",
    th_play: "প্লে স্টোর অ্যান্টি-থেফট",
    mat_1_title: "জিরো রুট পারমিশন",
    mat_1_sub: "নক্স ও স্ট্রংবক্স হার্ডওয়্যার অরিজিনাল থাকে",
    mat_2_title: "লকস্ক্রিন স্ট্যাটাস বার ও এয়ারপ্লেন ব্লক",
    mat_2_sub: "ইন্টারনেট অফ করার সুযোগ বন্ধ রাখে",
    mat_3_title: "এয়ার-গ্যাপড সেলুলার এসএমএস সি২",
    mat_3_sub: "ইন্টারনেট ও ওয়াই-ফাই ছাড়া এসএমএসে নিয়ন্ত্রণ",
    mat_4_title: "রিমোট হার্ডওয়্যার লকস্ক্রিন পিন রিসেট",
    mat_4_sub: "অ্যান্ড্রয়েড ১৪, ১৫, ১৬ ওএস পিন ওভাররাইড",
    mat_5_title: "হার্ডওয়্যার ইউএসবি ডেটা পিন কিলসুইচ",
    mat_5_sub: "Cellebrite ও GrayKey ক্যাবল ডেটা চুরি বন্ধ",
    mat_6_title: "স্থায়ী আনইন্সটল ও সেফ মোড প্রতিরোধ",
    mat_6_sub: "চোর কোনোভাবেই অ্যাপ মুছতে পারে না",
    mat_7_title: "প্রাইভেসি ও নজরদারি ডেটা সংরক্ষণ",
    mat_7_sub: "আপনার ছবি, অডিও ও লোকেশন কে দেখতে পারে?",
    mat_8_title: "প্রাইসিং মডেল ও মূল্য",
    mat_8_sub: "মাসিক সাবস্ক্রিপশন বনাম আজীবন মালিকানা",

    audit_section_tag: "ডিভাইস সিকিউরিটি ডায়াগনস্টিক",
    audit_section_title: "আপনার ফোনের চুরি ঝুঁকি পরীক্ষা করুন",
    audit_section_lede: "আপনার ফোনের বর্তমান সেটিংস যাচাই করে আসল চুরি ঝুঁকি স্কোর কত তা জেনে নিন।",
    audit_q1: "১. লকস্ক্রিন থেকে যে কেউ কি স্ট্যাটাস বার নামিয়ে এয়ারপ্লেন মোড অন করে দিতে পারে?",
    audit_q2: "২. আপনার স্ক্রিন লক পিন ছাড়াই কি পাওয়ার বাটন চেপে যে কেউ ফোন পুরোপুরি বন্ধ করতে পারে?",
    audit_q3: "৩. চোর আপনার সিম কার্ড খুলে ফেললে কি ফোন ট্র্যাক করার বা যোগাযোগ করার সমস্ত উপায় বন্ধ হয়ে যায়?",
    audit_q4: "৪. অস্ত্রের মুখে জোরপূর্বক স্ক্রিন আনলক করালে কি আপনার বিকাশ ও ব্যাংকিং অ্যাপ সরাসরি চোরের সামনে উন্মুক্ত হয়ে যাবে?",
    audit_risk_label: "আপনার বর্তমান ডিভাইস চুরি ঝুঁকি:",
    audit_cta: "পাসার মাধ্যমে এই ঝুঁকি শূন্যে নামান",

    case_section_tag: "ট্যাকটিক্যাল ক্রাইম লগ // সিআইডি কেস ফাইল",
    case_section_title: "বাস্তব চুরি আক্রমণ। ভয়ংকর প্রতিরক্ষামূলক প্রতিরোধ।",
    case_section_lede: "চোর এয়ারপ্লেন মোড বা পাওয়ার অফ করার সাথে সাথেই সাধারণ সব অ্যাপ অকেজো হয়ে যায়। দেখুন কীভাবে পাসা চোরকে নিজেই ফাঁদে ফেলে।",
    case1_title: "রাস্তায় চলন্ত ছিনতাই ও ২.৬৫জি শকওয়েভ",
    case1_desc: "দ্রুতগতির মোটরসাইকেল থেকে ছিনতাইকারী হাত থেকে ফোন হ্যাঁচকা টান মেরে তীব্র গতিতে পালিয়ে যায়।",
    case1_resp_title: "পাসার অটোনোমাস কাউন্টার-অ্যাকশন",
    case1_resp_desc: "অ্যাক্সেলেরোমিটার ভেক্টর √(x²+y²+z²) > ২৬.০ মি/সে² ত্বরণ শনাক্ত করে। ৩০ মিলি-সেকেন্ডের মধ্যে নক্স কিয়স্ক লকডাউন সক্রিয় হয়, স্ট্যাটাস বার বন্ধ হয় এবং অপরাধীর সেলফি তুলে জিপিএস টেলিগ্রামে পাঠায়।",
    case2_title: "সিম ছুড়ে ফেলে দেওয়া ও চোরের সিম ফাঁদ",
    case2_desc: "ইন্টারনেট বন্ধ করতে চোর ফিজিক্যাল সিম কার্ড খুলে ফেলে দেয়, পরবর্তীতে ফোন পরীক্ষা করতে নিজের নতুন সিম ফোনে ঢোকায়।",
    case2_resp_title: "পাসার অটোনোমাস কাউন্টার-অ্যাকশন",
    case2_resp_desc: "সিম খোলা মাত্রই কিয়স্ক লকডাউন। যখনই চোর নিজের সিম ঢোকায়, পাসা চোরের সিমের সেলুলার নেটওয়ার্ক দিয়ে মালিকের ব্যাকআপ নম্বরে স্বয়ংক্রিয় এসএমএস পাঠায়—যাতে কলার আইডিতে চোরের আসল ফোন নম্বর সরাসরি ফাঁস হয়ে যায়!",
    case3_title: "ল্যাব টেস্ট পয়েন্ট ও ইডিএল ৯০০৮ ফ্ল্যাশ আক্রমণ",
    case3_desc: "চোর ফোনটি টেকনিশিয়ান ল্যাবে নিয়ে ব্যাক-কভার খুলে মাদারবোর্ডের টেস্ট পয়েন্ট শর্ট করে কোয়ালকম EDL বা মিডিয়াটেক BROM মোডে ফেলে রম ফ্ল্যাশ করতে চায়।",
    case3_resp_title: "পাসার অটোনোমাস কাউন্টার-অ্যাকশন",
    case3_resp_desc: "গ্লু গলাতে ব্লোয়ার বা হিটগান লাগালে থার্মাল অ্যানোমালি ট্র্যাপ (>৪৮°সে.) ইউএসবি পিন কিল করে। আর ডেড ম্যানস সুইচের লোকাল হার্ডওয়্যার টাইমার স্বয়ংক্রিয়ভাবে কি-স্টোর ডিলিট করে দেয়—একটি বাইট ডেটাও চুরি করা সম্ভব নয়।",
    case4_title: "অস্ত্রের মুখে জোরপূর্বক আনলক ও স্টেরিল ডিকয় ওএস",
    case4_desc: "ছিনতাইকারী বা ডাকাত অস্ত্রের মুখে জিম্মি করে শারীরিক সহিংসতার ভয় দেখিয়ে স্ক্রিন আনলক করতে বাধ্য করে।",
    case4_resp_title: "পাসার অটোনোমাস কাউন্টার-অ্যাকশন",
    case4_resp_desc: "আসল পিনের বদলে আপনার সিক্রেট 'ডুরেস পিন' চাপুন। ফোন একটি নকল/ফাঁকা ওএস স্যান্ডবক্সে ওপেন হবে; বিকাশ, ব্যাংক ও হোয়াটসঅ্যাপ পলিসি লেভেলে সম্পূর্ণ ভ্যানিশ হয়ে যাবে এবং ব্যাকগ্রাউন্ডে জরুরি এসওএস পাঠাবে।",

    c2_section_tag: "৬-হাব ইন্টারঅ্যাক্টিভ টেলিগ্রাম সি২ কনসোল",
    c2_section_title: "৮৬টি মডিউলার টেলিগ্রাম কমান্ড",
    c2_section_lede: "আপনার নিজস্ব টেলিগ্রাম বট অথবা অফলাইন এসএমএস দিয়ে সম্পূর্ণ সার্বভৌম নিয়ন্ত্রণ। ৬টি স্পেশালাইজড হাবে বিভক্ত।",
    hub_loc: "লোকেশন ও আরএফ",
    hub_cam: "স্টিলথ নজরদারি",
    hub_lock: "লকডাউন ও সাইরেন",
    hub_owner: "নক্স ডিভাইস ওনার",
    hub_trap: "সেন্সর ট্র্যাপ",
    hub_sys: "এক্সট্র্যাকশন ও টেলিফোনি",

    faq_section_tag: "মাস্টার ডিফেন্স নলেজবেজ",
    faq_section_title: "পূর্ণাঙ্গ কারিগরি প্রশ্নোত্তর ও ফরেনসিক গাইড",
    faq_section_lede: "প্রতিটি সংশয়ী প্রশ্নের স্পষ্ট উত্তর। চিপ-লেভেল EDL/BROM আক্রমণ থেকে শুরু করে আইনি গ্রহণযোগ্যতা এবং জিরো-স্টোরেজ প্রাইভেসি গ্যারান্টি।",
    faq_search_placeholder: "প্রশ্ন খুঁজুন (যেমন: 'EDL', 'সিম', 'বিকাশ', 'ব্যাটারি', 'Cellebrite', 'পুলিশ')...",
    cat_all: "সব প্রশ্ন",
    cat_general: "সাধারণ",
    cat_privacy: "জিরো-স্টোরেজ ও প্রাইভেসি",
    cat_owner: "ডিভাইস ওনার",
    cat_sim: "সিম ডিফেন্স",
    cat_hardware: "চরম হার্ডওয়্যার ও EDL",
    cat_lockscreen: "এসক্রো পিন",
    cat_forensics: "স্টিলথ ফরেনসিক্স",
    cat_vault: "শ্যাডো ভল্ট",
    cat_sms: "অফলাইন এসএমএস",
    cat_traps: "সেন্সর ট্র্যাপ",
    cat_battery: "ব্যাটারি ও ওএস",
    cat_legal: "আইন ও পুলিশ",
    btn_expand_all: "[ + সব খুলুন ]",
    btn_collapse_all: "[ - সব বন্ধ করুন ]",

    pricing_section_tag: "সার্বভৌম কমার্শিয়াল লাইসেন্সিং",
    pricing_section_title: "কোনো মাসিক সাবস্ক্রিপশন ট্র্যাপ নেই। স্থায়ী মালিকানা।",
    pricing_section_lede: "আমরা প্রতি মাসে অর্থ কাটার পক্ষপাতী নই। একবার ক্রিপ্টোগ্রাফিক Ed25519 লাইসেন্স কিনুন এবং আজীবন সুরক্ষিত থাকুন।",
    badge_most_popular: "সর্বাধিক জনপ্রিয় ডিফেন্স",
    plan_eval_title: "ট্যাকটিক্যাল মূল্যায়ন",
    plan_eval_desc: "আপনার নিজস্ব হ্যান্ডসেটে মূল টেলিমেট্রি ও যাচাইকরণ পরীক্ষা করুন।",
    plan_eval_period: "৩-দিনের ফ্রি ট্রায়াল পিরিয়ড",
    f_eval_1: "মূল টেলিগ্রাম সি২ কমান্ডসমূহ",
    f_eval_2: "হেডলেস ক্যামেরা ক্যাপচার টেস্ট",
    f_eval_3: "জিপিএস ও সেল টাওয়ার টেলিমেট্রি",
    f_eval_4: "কমিউনিটি টেলিগ্রাম সাপোর্ট",
    btn_start_eval: "বটের মাধ্যমে শুরু করুন",

    plan_pro_title: "প্রো লাইফটাইম শিল্ড",
    plan_pro_desc: "১টি অ্যান্ড্রয়েড ফোনের জন্য আজীবন সম্পূর্ণ সার্বভৌম সুরক্ষা স্যুট।",
    plan_pro_period: "এককালীন পেমেন্ট • আজীবন আনলিমিটেড আপডেট",
    f_pro_1: "সবকটি ৮৬টি টেলিগ্রাম সি২ কমান্ড",
    f_pro_2: "নক্স-গ্রেড ডিভাইস ওনার প্রোভিশনিং",
    f_pro_3: "হার্ডওয়্যার এসক্রো টোকেন পিন রিসেট",
    f_pro_4: "অ্যান্টি-EDL/BROM ডেড ম্যানস সুইচ",
    f_pro_5: "সিম ইজেকশন চোরের নম্বর ডিটেকশন ট্র্যাপ",
    f_pro_6: "১-অন-১ ব্যক্তিগত রিমোট সেটআপ সহায়তা",
    f_pro_7: "২৪-ঘণ্টা শতভাগ মানি-ব্যাক গ্যারান্টি",
    btn_buy_pro: "লাইফটাইম লাইসেন্স নিন",

    plan_ent_title: "এন্টারপ্রাইজ ফ্লিট",
    plan_ent_desc: "ভিআইপি এক্সিকিউটিভ সুরক্ষা, করপোরেট ফ্লিট ও ফিল্ড এজেন্টদের জন্য।",
    plan_ent_period: "৫টি ডিভাইস প্যাক • ডেডিকেটেড কন্ট্রোল নোড",
    f_ent_1: "৫টি প্রো লাইফটাইম ডিভাইস লাইসেন্স",
    f_ent_2: "ডেডিকেটেড প্রাইভেট রিলে সার্ভার নোড",
    f_ent_3: "জিরো-নলেজ ফ্লিট ম্যানেজমেন্ট",
    f_ent_4: "সরাসরি হোয়াটসঅ্যাপ ও টেলিগ্রাম হটলাইন",
    btn_contact_ent: "যোগাযোগ করুন",

    btn_copy_uid: "ইউআইডি কপি করুন",
    bkash_title: "বিকাশ পেমেন্ট (বাংলাদেশ)",
    bkash_sub: "অফিসিয়াল বিকাশ নম্বরের জন্য যোগাযোগ করুন",
    bkash_action_btn: "হোয়াটসঅ্যাপে নম্বর চান",
    wa_title: "হোয়াটসঅ্যাপ কনসিয়ার্জ",
    wa_action_btn: "সরাসরি চ্যাট",
    floating_help: "লাইভ সহায়তা",

    footer_bio: "সার্বভৌম অ্যান্ড্রয়েড অ্যান্টি-থেফট ডিফেন্স ও গোপন ইন্টেলিজেন্স এজেন্ট। গুগল প্লে স্টোর ও ক্লাউড স্টোরেজের ওপর শূন্য নির্ভরতা। নক্স-গ্রেড ডিভাইস ওনার সুরক্ষা। প্রস্তুতকারক: ইজহান ইন্টেলেকট।",
    footer_resources: "রিসোর্স",
    footer_legal: "যোগাযোগ ও আইনি",
    footer_dl_apk: "সর্বশেষ এপিকে ডাউনলোড (v3.5.1)"
  }
};

let currentLang = 'en';
let activeCategory = 'all';

function setLanguage(lang) {
  currentLang = lang;
  document.documentElement.setAttribute('data-lang', lang);
  localStorage.setItem('pasa_lang', lang);

  document.getElementById('btnEn').classList.toggle('active', lang === 'en');
  document.getElementById('btnBn').classList.toggle('active', lang === 'bn');

  // Translate static data-i18n items
  const dict = translations[lang] || translations.en;
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (dict[key]) {
      el.innerHTML = dict[key];
    }
  });

  const searchInput = document.getElementById('faqSearch');
  if (searchInput && dict.faq_search_placeholder) {
    searchInput.placeholder = dict.faq_search_placeholder;
  }

  // Re-render FAQ with selected language
  renderFaq();
}

function renderFaq() {
  const container = document.getElementById('faqAccordion');
  if (!container) return;

  const query = (document.getElementById('faqSearch')?.value || '').toLowerCase().trim();
  container.innerHTML = '';

  let count = 0;
  rawFaq.forEach((item, idx) => {
    // Category filter
    if (activeCategory !== 'all' && item.category !== activeCategory) {
      return;
    }

    const q = currentLang === 'bn' ? item.qBn : item.qEn;
    const a = currentLang === 'bn' ? item.aBn : item.aEn;

    // Search query filter
    if (query) {
      const qMatch = q.toLowerCase().includes(query);
      const aMatch = a.toLowerCase().includes(query);
      if (!qMatch && !aMatch) return;
    }

    count++;
    const itemEl = document.createElement('div');
    itemEl.className = 'faq-item';
    itemEl.id = 'faq-item-' + idx;
    itemEl.innerHTML = \`
      <div class="faq-question" onclick="toggleFaq(\${idx})">
        <div class="faq-q-text">\${q}</div>
        <svg class="faq-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
      </div>
      <div class="faq-answer">\${a}</div>
    \`;
    container.appendChild(itemEl);
  });

  if (count === 0) {
    container.innerHTML = \`<div style="text-align:center;padding:40px;color:var(--text-faint);font-family:var(--mono);">
      \${currentLang === 'bn' ? 'কোনো প্রশ্নোত্তর পাওয়া যায়নি।' : 'No matching questions found.'}
    </div>\`;
  }
}

function toggleFaq(idx) {
  const item = document.getElementById('faq-item-' + idx);
  if (item) {
    item.classList.toggle('open');
  }
}

function toggleAllFaq(open) {
  document.querySelectorAll('.faq-item').forEach(el => {
    el.classList.toggle('open', open);
  });
}

function filterCategory(cat, btn) {
  activeCategory = cat;
  document.querySelectorAll('.cat-pill').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');
  renderFaq();
}

function filterFaq() {
  renderFaq();
}

function switchHub(hubId) {
  document.querySelectorAll('.c2-hub-panel').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.hub-tab').forEach(t => t.classList.remove('active'));

  const target = document.getElementById(hubId);
  if (target) target.classList.add('active');

  const btn = Array.from(document.querySelectorAll('.hub-tab')).find(b => b.getAttribute('onclick').includes(hubId));
  if (btn) btn.classList.add('active');
}

// ── Mobile Menu Drawer ──
function toggleMobileDrawer() {
  const drawer = document.getElementById('mobileDrawer');
  const overlay = document.getElementById('drawerOverlay');
  if (drawer && overlay) {
    drawer.classList.toggle('open');
    overlay.classList.toggle('open');
  }
}

// ── Floating Concierge Widget ──
function toggleConcierge() {
  const card = document.getElementById('conciergeCard');
  if (card) {
    card.classList.toggle('open');
  }
}

// ── Interactive Terminal Simulator Engine ──
const simulatedResponses = {
  '/locate': [
    { text: '[COMMAND] /locate - Satellite GNSS fix requested', cls: 'term-prompt' },
    { text: '[HAL] DevicePolicyManager energizing GNSS chipset (forced on)...', cls: 'term-line' },
    { text: '[SATELLITE] 14 SVs locked (Galileo E01-E12 + GPS PRN 18/24) | Fix: 3D_GNSS', cls: 'term-line' },
    { text: '[COORDS] Lat: 23.8103° N, Lon: 90.4125° E (Accuracy: 1.4m CEP)', cls: 'term-success' },
    { text: '[MAPS] https://maps.google.com/?q=23.8103,90.4125', cls: 'term-line' },
    { text: '[TLS 1.3] Dispatched direct-to-Telegram bot | Local RAM buffer shredded.', cls: 'term-success' }
  ],
  '/snap front': [
    { text: '[COMMAND] /snap front - Stealth mugshot requested', cls: 'term-prompt' },
    { text: '[CAM-X] CameraX headless pipeline bound to ServiceLifecycleOwner', cls: 'term-line' },
    { text: '[OPTIC] Front lens triggered: 0ms display blackout | Shutter audio muted', cls: 'term-line' },
    { text: '[CAPTURE] 12.2MP forensic snapshot captured into RAM buffer', cls: 'term-success' },
    { text: '[TELEGRAM] sendPhoto executed via authenticated bot API', cls: 'term-success' },
    { text: '[ZERO-STORE] Local temp bitmap cryptographically overwritten & purged.', cls: 'term-success' }
  ],
  '/usb_lock on': [
    { text: '[COMMAND] /usb_lock on - Physical data line sever initiated', cls: 'term-prompt' },
    { text: '[DPM] Calling setUsbDataSignalingEnabled(false) [Android 12+ API 31]', cls: 'term-line' },
    { text: '[HAL] Physical USB D+ & D- data signaling pins TERMINATED.', cls: 'term-alert' },
    { text: '[STATUS] AC Charging: ALLOWED | Data Transfer: DISABLED', cls: 'term-line' },
    { text: '[FORENSIC] GrayKey, Cellebrite, BadUSB, and PC ADB neutralized.', cls: 'term-success' }
  ],
  '/thermal': [
    { text: '[COMMAND] /thermal - Heat-Gun Anomaly Trap status', cls: 'term-prompt' },
    { text: '[SENSOR] Battery thermal register: 34.2°C (Ambient Normal)', cls: 'term-line' },
    { text: '[THRESHOLD] Trap Armed at >= 48.0°C (Detects lab glue loosening blowers)', cls: 'term-warn' },
    { text: '[TRIGGER ACTION] On breach: USB data pins cut + Kiosk lock + Mugshot + Telegram SOS', cls: 'term-line' }
  ],
  '/deadman': [
    { text: '[COMMAND] /deadman - Anti-EDL/BROM Dead Man\\'s Switch', cls: 'term-prompt' },
    { text: '[HARDWARE TIMER] Autonomous on-device countdown: 05h 42m remaining', cls: 'term-warn' },
    { text: '[RESET EVENT] Owner unlock or Telegram/SMS command refreshes to 06h 00m', cls: 'term-line' },
    { text: '[KILL-ACTION] On expiry: Automated cryptographic wipe (dpm.wipeData(0))', cls: 'term-alert' }
  ],
  '/fakeshutdown': [
    { text: '[COMMAND] /fakeshutdown - Engaging blackout deception canvas', cls: 'term-prompt' },
    { text: '[ANIMATION] Simulating genuine OEM power-off dialog & shutdown logo...', cls: 'term-line' },
    { text: '[CANVAS] Display brightness dropped to 0-nit pitch black (FLAG_FULLSCREEN)', cls: 'term-alert' },
    { text: '[TRAP] Screen touch listeners armed: stealth selfies + GPS beacons on any touch', cls: 'term-success' },
    { text: '[WAKE] Send /wake via Telegram or SMS to exit blackout canvas', cls: 'term-line' }
  ],
  '/freeze bkash': [
    { text: '[COMMAND] /freeze bkash - Shadow App Vault isolation', cls: 'term-prompt' },
    { text: '[DPM] Invoking setApplicationHidden(\"com.bKash.customerapp\", true)', cls: 'term-line' },
    { text: '[STATUS] Package hidden from launcher, drawer, and process table.', cls: 'term-alert' },
    { text: '[DATA] Zero data lost. App completely invisible to thief/coercer.', cls: 'term-success' }
  ]
};

function simulateCmd(cmd) {
  const screen = document.getElementById('termScreen');
  if (!screen) return;

  const responses = simulatedResponses[cmd] || [
    { text: '[COMMAND] ' + cmd, cls: 'term-prompt' },
    { text: '[EXECUTOR] Command accepted by PasaService background daemon', cls: 'term-line' },
    { text: '[STATUS] Directive executed with Device Owner supervisor authority', cls: 'term-success' }
  ];

  responses.forEach((r, i) => {
    setTimeout(() => {
      const line = document.createElement('div');
      line.className = 'term-line ' + (r.cls || '');
      line.textContent = r.text;
      screen.appendChild(line);
      screen.scrollTop = screen.scrollHeight;
    }, i * 160);
  });
}

function handleTermKey(e) {
  if (e.key === 'Enter') {
    submitTermInput();
  }
}

function submitTermInput() {
  const input = document.getElementById('termInput');
  if (!input) return;
  const val = input.value.trim();
  if (!val) return;
  input.value = '';
  simulateCmd(val);
}

function clearTerminal() {
  const screen = document.getElementById('termScreen');
  if (screen) {
    screen.innerHTML = '<div class=\"term-line\" style=\"color:var(--text-faint);\">// Terminal buffer cleared. Ready for input.</div>';
  }
}

// ── Vulnerability Self-Test Engine ──
const auditState = { 1: true, 2: true, 3: true, 4: true };

function setAudit(qNum, isYes, btn) {
  auditState[qNum] = isYes;
  const parent = btn.parentElement;
  parent.querySelectorAll('.audit-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');

  // Calculate score
  let yesCount = 0;
  for (let k in auditState) {
    if (auditState[k]) yesCount++;
  }

  const scoreEl = document.getElementById('auditScore');
  if (!scoreEl) return;

  if (yesCount === 4) {
    scoreEl.textContent = '94% CRITICAL RISK';
    scoreEl.style.color = '#ef4444';
  } else if (yesCount === 3) {
    scoreEl.textContent = '72% HIGH RISK';
    scoreEl.style.color = '#f59e0b';
  } else if (yesCount === 2) {
    scoreEl.textContent = '48% MODERATE RISK';
    scoreEl.style.color = '#f59e0b';
  } else if (yesCount === 1) {
    scoreEl.textContent = '24% LOW RISK';
    scoreEl.style.color = '#06b6d4';
  } else {
    scoreEl.textContent = '0% SECURED (PASA ENFORCED)';
    scoreEl.style.color = '#34d399';
  }
}

// ── Copy Binance UID ──
function copyBinanceUid() {
  const uid = '756303714';
  navigator.clipboard.writeText(uid).then(() => {
    showToast('BINANCE UID 756303714 COPIED');
  }).catch(() => {
    showToast('UID: 756303714');
  });
}

function showToast(msg) {
  const t = document.getElementById('toast');
  if (!t) return;
  t.textContent = msg;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 2200);
}

// Initialize on DOM load
window.addEventListener('DOMContentLoaded', () => {
  const savedLang = localStorage.getItem('pasa_lang') || 'en';
  setLanguage(savedLang);
});
</script>
</body>
</html>
`;
}

const htmlOutput = generateHtml();
fs.writeFileSync(targetHtmlPath, htmlOutput, 'utf8');
console.log('Successfully wrote', htmlOutput.length, 'bytes to', targetHtmlPath);
