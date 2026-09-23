const fs = require('fs');
const path = require('path');
const { faqData } = require('./scratch_faq_data.js');
const { featureHubs, commandsMatrix } = require('./features_data.js');

const targetHtmlPath = path.join(__dirname, '..', 'public', 'index.html');

function generateHtml() {
  return `<!DOCTYPE html>
<html lang="en" data-lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PASA Sentinel — Sovereign Android Mobile Defense &amp; Anti-Theft Intelligence</title>
<meta name="description" content="PASA Sentinel: Knox-Grade Device Owner anti-theft mobile defense. Zero cloud storage, direct-to-Telegram evidence, air-gapped SMS C2, and anti-uninstall security for Android 8.0–16.">
<meta name="theme-color" content="#ffffff">
<link rel="icon" type="image/png" href="/assets/img/logo.png">
<link rel="apple-touch-icon" href="/assets/img/logo.png">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&family=JetBrains+Mono:wght@500;600;700;800&family=Tiro+Bangla:ital@0;1&display=swap" rel="stylesheet">
<style>
  :root {
    /* ── Light Canvas Palette ── */
    --bg-canvas: #f8fafc;
    --bg-surface: #ffffff;
    --bg-muted: #f1f5f9;
    --bg-subtle: #e2e8f0;

    --border: #cbd5e1;
    --border-hover: #94a3b8;
    --border-primary: #dc2626;
    --border-cyan: #0284c7;

    --crimson: #dc2626;
    --crimson-hover: #b91c1c;
    --crimson-subtle: #fef2f2;
    --cyan: #0284c7;
    --cyan-subtle: #f0f9ff;
    --amber: #d97706;
    --amber-subtle: #fffbeb;
    --emerald: #059669;
    --emerald-subtle: #ecfdf5;

    /* High-Contrast WCAG AAA Typography */
    --text: #0f172a;
    --text-dim: #1e293b;
    --text-faint: #475569;
    --text-muted: #64748b;

    /* Cinematic Cyber-Forensic Colors */
    --hud-bg: #090d16;
    --hud-surface: #0f172a;
    --hud-border: #1e293b;
    --hud-cyan: #00f0ff;
    --hud-crimson: #ff003c;
    --hud-emerald: #00ff88;
    --hud-amber: #ffb703;

    --shadow-sm: 0 1px 3px rgba(15, 23, 42, 0.05), 0 1px 2px rgba(15, 23, 42, 0.03);
    --shadow-md: 0 4px 14px rgba(15, 23, 42, 0.07), 0 1px 3px rgba(15, 23, 42, 0.04);
    --shadow-lg: 0 10px 25px -4px rgba(15, 23, 42, 0.1), 0 4px 10px -2px rgba(15, 23, 42, 0.05);

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
    scroll-padding-top: 80px;
  }
  
  body {
    font-family: var(--font);
    background: var(--bg-canvas);
    color: var(--text);
    line-height: 1.58;
    -webkit-font-smoothing: antialiased;
    overflow-x: hidden;
  }

  /* Anchor Clearance */
  section[id], div[id] {
    scroll-margin-top: 80px;
  }

  .wrap { max-width: 1200px; margin: 0 auto; padding: 0 24px; position: relative; z-index: 10; }
  .wrap-narrow { max-width: 860px; margin: 0 auto; padding: 0 24px; position: relative; z-index: 10; }

  /* ── Top Status Strip ── */
  .top-strip {
    background: #ffffff;
    border-bottom: 1px solid var(--border);
    padding: 7px 0; font-family: var(--mono); font-size: 11.5px;
    color: var(--text-faint); position: relative; z-index: 101;
  }
  .top-strip-inner {
    display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;
  }
  .pulse-dot {
    width: 8px; height: 8px; border-radius: 50%; background: var(--emerald);
    box-shadow: 0 0 8px var(--emerald); display: inline-block; flex-shrink: 0;
    animation: pulse-glow 2s infinite ease-in-out;
  }
  .pulse-dot.crimson {
    background: var(--crimson);
    box-shadow: 0 0 8px var(--crimson);
  }
  @keyframes pulse-glow {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.4; transform: scale(0.85); }
  }
  .top-status-badge {
    display: flex; align-items: center; gap: 8px; font-weight: 700; color: var(--text); letter-spacing: 0.02em;
  }
  .top-specs { display: flex; gap: 18px; font-weight: 700; }
  .top-specs span { color: var(--cyan); }

  /* ── Navigation ── */
  .nav {
    position: sticky; top: 0; z-index: 100;
    background: rgba(255, 255, 255, 0.94);
    backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
    border-bottom: 1px solid var(--border);
  }
  .nav-inner {
    display: flex; align-items: center; justify-content: space-between;
    height: 68px;
  }
  .brand {
    display: flex; align-items: center; gap: 12px; text-decoration: none; color: var(--text);
  }
  .brand-logo-img {
    width: 36px; height: 36px; border-radius: 8px; object-fit: cover;
  }
  .brand-text {
    font-weight: 800; font-size: 16px; letter-spacing: -0.01em; color: var(--text);
  }
  .brand-tag {
    font-family: var(--mono); font-size: 9.5px; font-weight: 700;
    color: var(--text-faint); background: var(--bg-muted);
    padding: 2px 6px; border-radius: 4px; border: 1px solid var(--border);
    letter-spacing: 0.06em; display: inline-block; text-transform: uppercase;
  }
  .nav-menu {
    display: flex; align-items: center; gap: 18px;
  }
  .nav-link {
    font-size: 13.5px; font-weight: 600; color: var(--text-dim); text-decoration: none;
    transition: color 0.15s ease;
  }
  .nav-link:hover { color: var(--cyan); }

  .nav-actions {
    display: flex; align-items: center; gap: 12px;
  }
  
  /* Language Switcher */
  .lang-switcher {
    display: flex; background: #f1f5f9; padding: 3px; border-radius: 8px;
    border: 1px solid var(--border);
  }
  .lang-btn {
    background: transparent; border: none; font-size: 11.5px; font-weight: 700;
    padding: 4px 10px; border-radius: 6px; cursor: pointer; color: var(--text-faint);
    transition: all 0.15s ease; font-family: var(--mono);
  }
  .lang-btn.active {
    background: #ffffff; color: var(--text);
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1);
  }

  /* Sound FX Toggle Pill */
  .sound-btn {
    background: var(--bg-muted); border: 1px solid var(--border);
    color: var(--text-dim); font-family: var(--mono); font-size: 11.5px; font-weight: 700;
    padding: 6px 12px; border-radius: 8px; cursor: pointer;
    display: inline-flex; align-items: center; gap: 6px; transition: all 0.15s ease;
  }
  .sound-btn:hover { background: #ffffff; border-color: var(--cyan); color: var(--cyan); }
  .sound-btn.active {
    background: rgba(2, 132, 199, 0.1); border-color: var(--cyan); color: var(--cyan);
  }

  .nav-cta-secondary {
    background: var(--bg-muted); color: var(--text); border: 1.5px solid #cbd5e1;
    padding: 7px 14px; border-radius: 8px; font-size: 12.5px; font-weight: 700;
    text-decoration: none; display: inline-flex; align-items: center; gap: 6px;
    transition: all 0.15s ease;
  }
  .nav-cta-secondary:hover {
    background: #ffffff; border-color: var(--cyan); color: var(--cyan);
    box-shadow: 0 2px 6px rgba(2, 132, 199, 0.12);
  }

  .mobile-toggle {
    display: none; background: transparent; border: none; cursor: pointer;
    padding: 6px; color: var(--text);
  }

  /* Mobile Drawer */
  .drawer-overlay {
    position: fixed; inset: 0; background: rgba(15, 23, 42, 0.4); z-index: 1000;
    opacity: 0; pointer-events: none; transition: opacity 0.25s ease;
  }
  .drawer-overlay.open { opacity: 1; pointer-events: auto; }
  .mobile-drawer {
    position: fixed; top: 0; right: 0; width: 290px; height: 100%;
    background: #ffffff; z-index: 1001; padding: 24px; display: flex; flex-direction: column;
    gap: 16px; box-shadow: -4px 0 20px rgba(0,0,0,0.15); transform: translateX(100%);
    transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  }
  .mobile-drawer.open { transform: translateX(0); }
  .mobile-drawer-hdr {
    display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;
  }
  .mobile-drawer a {
    text-decoration: none; color: var(--text); font-weight: 600; font-size: 15px;
    padding: 8px 0; border-bottom: 1px solid var(--border);
  }

  @media (max-width: 1024px) {
    .nav-menu { display: none; }
    .mobile-toggle { display: block; }
    .top-strip-inner { justify-content: center; text-align: center; }
    .top-specs { display: none; }
  }

  /* ── HERO SECTION: 2-COLUMN TACTICAL FORENSICS ── */
  .hero {
    padding: 50px 0 46px; position: relative;
    background: radial-gradient(circle at 50% 10%, rgba(2, 132, 199, 0.05), transparent 70%);
    border-bottom: 1px solid var(--border);
    perspective: 1200px;
  }
  .hero-grid {
    display: grid; grid-template-columns: 1.15fr 1fr; gap: 36px; align-items: center;
  }
  @media (max-width: 960px) {
    .hero-grid { grid-template-columns: 1fr; gap: 40px; }
    .hero-col-left { text-align: center; }
    .hero-cta-group { justify-content: center; }
    .specs-bar-compact { justify-content: center; }
  }

  .badge-tag {
    display: inline-flex; align-items: center; gap: 8px; font-family: var(--mono);
    font-size: 11px; font-weight: 700; color: var(--cyan); background: var(--cyan-subtle);
    border: 1.5px solid #bae6fd; padding: 5px 12px; border-radius: 20px; margin-bottom: 18px;
    letter-spacing: 0.03em;
  }
  .hero-title {
    font-size: clamp(28px, 4vw, 44px); font-weight: 900; line-height: 1.15;
    letter-spacing: -0.02em; margin-bottom: 16px; color: var(--text);
  }
  .hero-sub {
    font-size: clamp(14.5px, 1.6vw, 16.5px); color: var(--text-dim);
    margin-bottom: 24px; line-height: 1.6; font-weight: 450;
  }
  .hero-cta-group {
    display: flex; flex-wrap: wrap; align-items: center; gap: 12px;
    margin-bottom: 24px;
  }
  .btn-primary {
    background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
    color: #fff; padding: 12px 24px; border-radius: 9px; font-weight: 700; font-size: 14px;
    text-decoration: none; display: inline-flex; align-items: center; gap: 8px;
    box-shadow: 0 4px 14px rgba(220, 38, 38, 0.32); border: 1px solid rgba(255,255,255,0.2);
    transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  }
  .btn-primary:hover {
    transform: translateY(-2px); box-shadow: 0 6px 20px rgba(220, 38, 38, 0.48);
  }
  .btn-setup {
    background: #ffffff; color: var(--cyan); padding: 12px 20px;
    border-radius: 9px; font-weight: 700; font-size: 14px; text-decoration: none;
    display: inline-flex; align-items: center; gap: 8px; border: 1.5px solid var(--cyan);
    box-shadow: 0 1px 3px rgba(2, 132, 199, 0.1); transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  }
  .btn-setup:hover {
    background: var(--cyan-subtle); box-shadow: 0 4px 14px rgba(2, 132, 199, 0.2);
    transform: translateY(-2px);
  }

  /* HIGH-VOLTAGE CRIME SIMULATION TRIGGER BUTTON */
  .btn-simulate-snatch {
    background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
    color: #fca5a5; border: 1.5px solid #ef4444; padding: 12px 20px;
    border-radius: 9px; font-family: var(--mono); font-size: 13px; font-weight: 800;
    letter-spacing: 0.04em; cursor: pointer; display: inline-flex; align-items: center; gap: 10px;
    box-shadow: 0 4px 14px rgba(239, 68, 68, 0.25); transition: all 0.2s ease;
    animation: btn-pulse-border 2.5s infinite ease-in-out;
  }
  @keyframes btn-pulse-border {
    0%, 100% { box-shadow: 0 0 12px rgba(239, 68, 68, 0.25); border-color: #ef4444; }
    50% { box-shadow: 0 0 20px rgba(239, 68, 68, 0.55); border-color: #f87171; }
  }
  .btn-simulate-snatch:hover {
    transform: translateY(-2px); background: #000; color: #fff;
    box-shadow: 0 6px 24px rgba(239, 68, 68, 0.6);
  }

  /* Compact Telemetry Strip */
  .specs-bar-compact {
    display: flex; flex-wrap: wrap; gap: 10px; font-family: var(--mono); font-size: 11px;
  }
  .spec-pill {
    background: #ffffff; border: 1px solid var(--border); padding: 5px 10px;
    border-radius: 6px; display: inline-flex; align-items: center; gap: 6px;
    box-shadow: var(--shadow-sm); font-weight: 600;
  }
  .spec-pill strong { color: var(--text); }
  .spec-pill.crimson strong { color: var(--crimson); }
  .spec-pill.emerald strong { color: var(--emerald); }

  /* ── 3D HOLOGRAPHIC FORENSIC PHONE SIMULATOR ── */
  .phone-stage-wrapper {
    perspective: 1200px; display: flex; justify-content: center;
  }
  .phone-mockup-3d {
    width: 320px; height: 500px; background: #090d16;
    border-radius: 38px; border: 4px solid #334155;
    box-shadow: 0 25px 60px -12px rgba(15, 23, 42, 0.6), 0 0 0 1px rgba(0, 240, 255, 0.2), inset 0 0 14px rgba(0, 0, 0, 0.8);
    position: relative; overflow: hidden; transform-style: preserve-3d;
    transition: transform 0.2s cubic-bezier(0.1, 0.9, 0.2, 1);
  }
  .phone-notch {
    position: absolute; top: 10px; left: 50%; transform: translateX(-50%);
    width: 90px; height: 16px; background: #020617; border-radius: 10px;
    z-index: 30; display: flex; align-items: center; justify-content: center; gap: 6px;
    border: 1px solid #1e293b;
  }
  .notch-cam-dot {
    width: 6px; height: 6px; border-radius: 50%; background: #1e293b;
    border: 1px solid #38bdf8;
  }
  .notch-speaker {
    width: 34px; height: 3px; border-radius: 2px; background: #334155;
  }

  /* Night-Vision Viewport */
  .phone-screen {
    position: absolute; inset: 0; border-radius: 34px; overflow: hidden;
    background: #020617; display: flex; flex-direction: column;
  }
  #forensicCamCanvas {
    position: absolute; inset: 0; width: 100%; height: 100%; z-index: 2;
  }
  .screen-overlay-scanlines {
    position: absolute; inset: 0; pointer-events: none; z-index: 10;
    background: linear-gradient(rgba(18, 16, 16, 0) 50%, rgba(0, 0, 0, 0.45) 50%),
                linear-gradient(90deg, rgba(255, 0, 0, 0.04), rgba(0, 255, 0, 0.02), rgba(0, 0, 255, 0.04));
    background-size: 100% 3px, 6px 100%;
  }

  /* High-Voltage Camera Flash */
  .camera-flash-overlay {
    position: absolute; inset: 0; background: #ffffff; z-index: 50;
    opacity: 0; pointer-events: none; transition: opacity 0.05s ease-out;
  }
  .camera-flash-overlay.flashing {
    opacity: 0.95;
  }

  /* Screen Shake during Snatch */
  .phone-mockup-3d.shaking {
    animation: phone-snatch-shake 0.45s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
  }
  @keyframes phone-snatch-shake {
    10%, 90% { transform: translate3d(-3px, 2px, 0) rotate(-1deg); }
    20%, 80% { transform: translate3d(4px, -3px, 0) rotate(1.5deg); }
    30%, 50%, 70% { transform: translate3d(-5px, 3px, 0) rotate(-2deg); }
    40%, 60% { transform: translate3d(5px, -2px, 0) rotate(2deg); }
  }

  /* HUD Telemetry Overlay on Phone */
  .phone-hud-overlay {
    position: absolute; inset: 0; padding: 34px 18px 18px; z-index: 15;
    display: flex; flex-direction: column; justify-content: space-between;
    pointer-events: none; font-family: var(--mono);
  }
  .hud-stat-badge {
    display: flex; align-items: center; justify-content: space-between;
    font-size: 10px; color: #94a3b8; background: rgba(15, 23, 42, 0.7);
    padding: 4px 8px; border-radius: 6px; border: 1px solid rgba(56, 189, 248, 0.2);
    backdrop-filter: blur(4px);
  }
  .hud-stat-badge span.red { color: #f87171; font-weight: 700; }
  .hud-stat-badge span.cyan { color: #38bdf8; font-weight: 700; }

  /* CAUGHT RED-HANDED STAMP */
  .stamp-cinematic {
    position: absolute; top: 48%; left: 50%;
    transform: translate(-50%, -50%) rotate(-13deg) scale(0);
    border: 3.5px solid #ef4444; color: #ef4444;
    padding: 6px 14px; border-radius: 6px; font-family: var(--mono);
    font-size: 13.5px; font-weight: 900; letter-spacing: 0.08em;
    background: rgba(15, 23, 42, 0.9); box-shadow: 0 0 24px rgba(239, 68, 68, 0.75);
    z-index: 25; pointer-events: none; text-align: center; white-space: nowrap;
    transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }
  .stamp-cinematic.stamped {
    transform: translate(-50%, -50%) rotate(-13deg) scale(1);
  }

  /* Floating Telegram Alert Notification Card */
  .telegram-alert-card {
    position: fixed; top: 88px; right: 24px; z-index: 10000;
    background: #0f172a; border: 1.5px solid #38bdf8; border-radius: 12px;
    padding: 14px 18px; width: 340px; box-shadow: 0 20px 40px rgba(0,0,0,0.5), 0 0 20px rgba(2, 132, 199, 0.3);
    color: #f8fafc; font-family: var(--font); transform: translateY(-160px); opacity: 0;
    transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  }
  .telegram-alert-card.show {
    transform: translateY(0); opacity: 1;
  }
  .tg-card-hdr {
    display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;
    font-size: 12px; color: #38bdf8; font-family: var(--mono); font-weight: 700;
  }
  .tg-card-body {
    display: flex; gap: 12px; align-items: center;
  }
  .tg-suspect-thumb {
    width: 52px; height: 52px; border-radius: 8px; border: 1.5px solid #ef4444;
    background: #020617; display: flex; align-items: center; justify-content: center;
    color: #ef4444; flex-shrink: 0; position: relative; overflow: hidden;
  }
  .tg-card-text { font-size: 12.5px; line-height: 1.4; color: #cbd5e1; }
  .tg-card-text strong { color: #f87171; }
  .tg-card-meta { font-family: var(--mono); font-size: 10.5px; color: #94a3b8; margin-top: 4px; }

  /* ── DETECTIVE EVIDENCE WALL / MISSION DOSSIER ── */
  .evidence-board {
    background: #ffffff; border: 1.5px solid var(--border); border-radius: 16px;
    padding: 36px 30px; box-shadow: var(--shadow-sm); position: relative;
  }
  .evidence-grid {
    display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px;
  }
  @media (max-width: 960px) {
    .evidence-grid { grid-template-columns: repeat(2, 1fr); }
  }
  @media (max-width: 540px) {
    .evidence-grid { grid-template-columns: 1fr; }
  }
  .evidence-pin {
    background: #f8fafc; border: 1.5px solid var(--border); border-radius: 12px;
    padding: 22px 18px; transition: all 0.2s ease; position: relative;
    display: flex; flex-direction: column; justify-content: space-between;
  }
  .evidence-pin:hover {
    border-color: var(--cyan); transform: translateY(-3px); box-shadow: var(--shadow-md);
  }
  .evidence-tag {
    font-family: var(--mono); font-size: 10.5px; font-weight: 800;
    color: var(--crimson); text-transform: uppercase; margin-bottom: 8px;
    display: flex; align-items: center; justify-content: space-between;
  }
  .evidence-title { font-size: 15.5px; font-weight: 700; color: var(--text); margin-bottom: 6px; }
  .evidence-desc { font-size: 13px; color: var(--text-dim); line-height: 1.5; }

  /* ── 1-CLICK DEVICE OWNER SETUP SECTION ── */
  .setup-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 18px;
    margin-bottom: 28px;
  }
  .setup-card {
    background: #ffffff; border: 1.5px solid var(--border); border-radius: 12px;
    padding: 24px 20px; position: relative; box-shadow: var(--shadow-sm);
    transition: all 0.2s ease; display: flex; flex-direction: column;
  }
  .setup-card:hover {
    border-color: var(--cyan); transform: translateY(-2px); box-shadow: var(--shadow-md);
  }
  .setup-step-num {
    position: absolute; top: 16px; right: 18px; font-family: var(--mono);
    font-size: 12px; font-weight: 800; color: var(--text-faint);
    background: var(--bg-muted); width: 26px; height: 26px; border-radius: 50%;
    display: flex; align-items: center; justify-content: center; border: 1px solid var(--border);
  }
  .setup-icon {
    width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center;
    justify-content: center; background: var(--crimson-subtle); border: 1px solid #fecaca;
    color: var(--crimson); margin-bottom: 14px;
  }
  .setup-icon.cyan { background: var(--cyan-subtle); border-color: #bae6fd; color: var(--cyan); }
  .setup-icon.emerald { background: var(--emerald-subtle); border-color: #a7f3d0; color: var(--emerald); }
  .setup-card-title {
    font-size: 16px; font-weight: 700; color: var(--text); margin-bottom: 6px;
  }
  .setup-card-desc {
    font-size: 13px; color: var(--text-dim); line-height: 1.5;
  }
  .setup-cta-banner {
    background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
    border: 1.5px solid var(--border); border-radius: 12px; padding: 22px 28px;
    display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px;
    box-shadow: var(--shadow-sm);
  }
  .setup-cta-badge {
    display: inline-flex; align-items: center; gap: 8px; font-family: var(--mono);
    font-size: 10.5px; font-weight: 700; color: var(--cyan); margin-bottom: 4px;
  }
  .setup-cta-title {
    font-size: 18px; font-weight: 800; color: var(--text); margin-bottom: 4px;
  }
  .setup-cta-desc {
    font-size: 13px; color: var(--text-dim); max-width: 600px; line-height: 1.45;
  }

  /* ── 8-HUB INTERACTIVE DEFENSE EXPLORER ── */
  .hub-controls {
    max-width: 1100px; margin: 0 auto 24px;
  }
  .hub-tabs {
    display: flex; flex-wrap: wrap; gap: 8px; justify-content: center;
  }
  .hub-tab {
    background: #ffffff; border: 1px solid var(--border); color: var(--text-dim);
    padding: 7px 14px; border-radius: 18px; font-size: 12px; font-weight: 600;
    cursor: pointer; transition: all 0.15s; display: inline-flex; align-items: center; gap: 6px;
    box-shadow: var(--shadow-sm);
  }
  .hub-tab:hover:not(.active) { color: var(--text); background: var(--bg-muted); }
  .hub-tab.active {
    background: var(--cyan); color: #ffffff; border-color: var(--cyan);
    box-shadow: 0 2px 8px rgba(2, 132, 199, 0.25);
  }
  .features-grid-rich {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px;
  }
  .feature-card-rich {
    background: #ffffff; border: 1.5px solid var(--border); border-radius: 12px;
    padding: 18px 20px; display: flex; flex-direction: column; justify-content: space-between;
    box-shadow: var(--shadow-sm); transition: all 0.2s ease;
  }
  .feature-card-rich:hover {
    border-color: #cbd5e1; transform: translateY(-2px); box-shadow: var(--shadow-md);
  }
  .feature-header {
    display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px;
  }
  .feature-tag {
    font-family: var(--mono); font-size: 10px; font-weight: 700; color: var(--cyan);
    padding: 2px 7px; border-radius: 4px; background: var(--cyan-subtle); border: 1px solid #bae6fd;
  }
  .feature-title { font-size: 15px; font-weight: 700; margin-bottom: 6px; color: var(--text); }
  .feature-desc { font-size: 13px; color: var(--text-dim); line-height: 1.5; margin-bottom: 12px; }
  .threat-box {
    background: var(--crimson-subtle); border: 1px solid #fecaca; border-radius: 8px;
    padding: 10px 12px; font-size: 12px; color: #991b1b; line-height: 1.45; margin-bottom: 12px;
  }
  .threat-title {
    font-weight: 700; font-family: var(--mono); margin-bottom: 3px;
    display: flex; align-items: center; gap: 6px; font-size: 11px;
  }
  .cmd-badges { display: flex; flex-wrap: wrap; gap: 6px; }
  .cmd-badge {
    background: #f1f5f9; border: 1px solid var(--border); color: var(--text-dim);
    font-family: var(--mono); font-size: 11px; font-weight: 700; padding: 3px 8px;
    border-radius: 6px; cursor: pointer; transition: all 0.15s;
  }
  .cmd-badge:hover { background: #e2e8f0; color: var(--cyan); border-color: var(--cyan); }

  /* ── 86-COMMAND TACTICAL TERMINAL ── */
  .cmd-controls {
    max-width: 900px; margin: 0 auto 20px; display: flex; flex-direction: column; gap: 12px;
  }
  .cmd-search {
    width: 100%; padding: 12px 18px; border-radius: 10px; border: 1.5px solid var(--border);
    font-size: 13.5px; background: #ffffff; color: var(--text); outline: none;
    box-shadow: var(--shadow-sm); transition: border-color 0.15s;
  }
  .cmd-search:focus { border-color: var(--cyan); box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15); }
  .cmd-pills {
    display: flex; flex-wrap: wrap; gap: 6px; justify-content: center;
  }
  .cmd-pill {
    background: #ffffff; border: 1px solid var(--border); color: var(--text-dim);
    padding: 5px 12px; border-radius: 16px; font-size: 11.5px; font-weight: 600;
    cursor: pointer; transition: all 0.15s;
  }
  .cmd-pill:hover:not(.active) { background: var(--bg-muted); }
  .cmd-pill.active {
    background: #0f172a; color: #ffffff; border-color: #0f172a;
  }
  .cmd-grid {
    display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px;
  }
  .cmd-card-compact {
    background: #ffffff; border: 1px solid var(--border); border-radius: 8px;
    padding: 10px 14px; box-shadow: var(--shadow-sm); transition: all 0.15s ease;
    display: flex; flex-direction: column; justify-content: space-between;
  }
  .cmd-card-compact:hover {
    border-color: var(--cyan); box-shadow: var(--shadow-md);
  }
  .cmd-token-row {
    display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px;
  }
  .cmd-token-text {
    font-family: var(--mono); font-weight: 800; font-size: 13px; color: var(--text);
  }
  .cmd-copy-icon {
    cursor: pointer; color: var(--text-faint); padding: 3px; border-radius: 4px;
  }
  .cmd-copy-icon:hover { color: var(--cyan); background: var(--cyan-subtle); }
  .cmd-desc-text {
    font-size: 12px; color: var(--text-dim); line-height: 1.4;
  }

  /* ── COMPACT COMPARISON MATRIX ── */
  .table-container {
    overflow-x: auto; background: #ffffff; border: 1.5px solid var(--border);
    border-radius: 12px; box-shadow: var(--shadow-sm);
  }
  .comp-table {
    width: 100%; border-collapse: collapse; text-align: left; font-size: 13px;
  }
  .comp-table th, .comp-table td {
    padding: 12px 16px; border-bottom: 1px solid var(--border);
  }
  .comp-table th {
    background: #f8fafc; font-weight: 700; color: var(--text);
    font-family: var(--mono); font-size: 11.5px; text-transform: uppercase;
  }
  .comp-highlight {
    background: rgba(2, 132, 199, 0.04); font-weight: 700; color: var(--cyan);
  }
  .check-icon { color: var(--emerald); font-weight: 800; }
  .cross-icon { color: var(--crimson); font-weight: 800; }

  /* ── SMART CATEGORY-TABBED FAQ ── */
  .faq-controls {
    max-width: 860px; margin: 0 auto 20px; display: flex; flex-direction: column; gap: 12px;
  }
  .faq-search {
    width: 100%; padding: 12px 18px; border-radius: 10px; border: 1.5px solid var(--border);
    font-size: 13.5px; background: #ffffff; color: var(--text); outline: none;
    box-shadow: var(--shadow-sm); transition: border-color 0.15s;
  }
  .faq-search:focus { border-color: var(--cyan); box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15); }
  .faq-pills {
    display: flex; flex-wrap: wrap; gap: 6px; justify-content: center;
  }
  .faq-pill {
    background: #ffffff; border: 1px solid var(--border); color: var(--text-dim);
    padding: 6px 14px; border-radius: 16px; font-size: 12px; font-weight: 600;
    cursor: pointer; transition: all 0.15s; box-shadow: var(--shadow-sm);
  }
  .faq-pill:hover:not(.active) { background: var(--bg-muted); }
  .faq-pill.active {
    background: var(--cyan); color: #ffffff; border-color: var(--cyan);
    box-shadow: 0 2px 8px rgba(2, 132, 199, 0.25);
  }
  .faq-list {
    max-width: 860px; margin: 0 auto; display: flex; flex-direction: column; gap: 10px;
  }
  .faq-item {
    background: #ffffff; border: 1.5px solid var(--border); border-radius: 10px;
    overflow: hidden; box-shadow: var(--shadow-sm); transition: border-color 0.15s;
  }
  .faq-item.open { border-color: var(--cyan); }
  .faq-q {
    padding: 16px 20px; font-weight: 700; font-size: 14.5px; color: var(--text);
    cursor: pointer; display: flex; justify-content: space-between; align-items: center; gap: 14px;
    user-select: none;
  }
  .faq-q:hover { background: #f8fafc; }
  .faq-icon {
    font-family: var(--mono); font-size: 16px; font-weight: 700; color: var(--text-faint);
    transition: transform 0.2s;
  }
  .faq-item.open .faq-icon { transform: rotate(45deg); color: var(--cyan); }
  .faq-a {
    display: none; padding: 0 20px 18px; font-size: 13.5px; color: var(--text-dim);
    line-height: 1.6; border-top: 1px dashed var(--border); padding-top: 14px;
  }
  .faq-a p { margin-bottom: 10px; }
  .faq-a ol, .faq-a ul { padding-left: 20px; margin-bottom: 10px; }
  .faq-a li { margin-bottom: 4px; }
  .faq-a code {
    background: #f1f5f9; padding: 2px 6px; border-radius: 4px;
    font-family: var(--mono); font-size: 12px; color: #b91c1c; border: 1px solid var(--border);
  }

  /* ── PRICING & LICENSING ── */
  .pricing-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px;
    max-width: 1080px; margin: 0 auto 36px;
  }
  .plan-card {
    background: #ffffff; border: 1.5px solid var(--border); border-radius: 14px;
    padding: 28px 24px; box-shadow: var(--shadow-sm); display: flex; flex-direction: column;
    justify-content: space-between; position: relative; transition: all 0.2s ease;
  }
  .plan-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
  .plan-card.popular {
    border-color: var(--crimson); box-shadow: 0 4px 18px rgba(220, 38, 38, 0.12);
  }
  .plan-badge {
    position: absolute; top: -11px; left: 50%; transform: translateX(-50%);
    background: var(--crimson); color: #ffffff; font-family: var(--mono);
    font-size: 10px; font-weight: 800; padding: 3px 12px; border-radius: 12px;
    letter-spacing: 0.05em; text-transform: uppercase;
  }
  .plan-title { font-size: 18px; font-weight: 800; color: var(--text); margin-bottom: 4px; }
  .plan-desc { font-size: 13px; color: var(--text-faint); margin-bottom: 16px; }
  .plan-price-wrap { margin-bottom: 20px; border-bottom: 1px solid var(--border); padding-bottom: 16px; }
  .plan-price { font-size: 32px; font-weight: 900; color: var(--text); line-height: 1; }
  .plan-price-sub { font-size: 12px; color: var(--text-faint); font-family: var(--mono); margin-top: 4px; }
  .plan-features { list-style: none; display: flex; flex-direction: column; gap: 8px; font-size: 13px; margin-bottom: 24px; }
  .plan-features li { display: flex; align-items: center; gap: 8px; color: var(--text-dim); }
  .plan-features li span { color: var(--emerald); font-weight: 800; }
  .btn-plan {
    width: 100%; padding: 11px; border-radius: 8px; font-weight: 700; font-size: 13.5px;
    text-align: center; text-decoration: none; cursor: pointer; transition: all 0.15s;
    border: none; display: block;
  }
  .btn-plan.primary { background: var(--crimson); color: #fff; box-shadow: 0 2px 10px rgba(220, 38, 38, 0.28); }
  .btn-plan.primary:hover { background: var(--crimson-hover); }
  .btn-plan.outline { background: #ffffff; color: var(--text); border: 1.5px solid var(--border); }
  .btn-plan.outline:hover { border-color: var(--cyan); color: var(--cyan); background: var(--cyan-subtle); }

  /* Payment Channels Strip */
  .payment-channels {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 14px;
    max-width: 1080px; margin: 0 auto;
  }
  .pay-method {
    background: #ffffff; border: 1.5px solid var(--border); border-radius: 12px;
    padding: 16px 18px; display: flex; align-items: center; gap: 14px; box-shadow: var(--shadow-sm);
  }
  .pay-icon {
    width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center;
    justify-content: center; flex-shrink: 0;
  }
  .pay-title { font-size: 13.5px; font-weight: 700; color: var(--text); margin-bottom: 2px; }
  .pay-sub { font-size: 11px; color: var(--text-faint); font-family: var(--mono); margin-bottom: 4px; }
  .pay-action-btn {
    font-size: 11.5px; font-weight: 700; text-decoration: none; padding: 4px 10px;
    border-radius: 6px; display: inline-block;
  }

  /* ── Floating Concierge ── */
  .floating-concierge {
    position: fixed; bottom: 20px; right: 20px; z-index: 999;
    display: flex; flex-direction: column; align-items: flex-end; gap: 8px;
  }
  .concierge-pill {
    background: #ffffff; border: 1.5px solid #cbd5e1; border-radius: 20px;
    padding: 8px 14px; display: flex; align-items: center; gap: 8px; font-weight: 700;
    font-size: 12.5px; color: var(--text); cursor: pointer; box-shadow: 0 4px 14px rgba(15, 23, 42, 0.12);
    transition: all 0.15s ease;
  }
  .concierge-pill:hover {
    border-color: var(--cyan); color: var(--cyan); transform: translateY(-2px);
  }
  .concierge-card {
    display: none; background: #ffffff; border: 1.5px solid #cbd5e1; border-radius: 12px;
    padding: 14px; box-shadow: var(--shadow-lg); flex-direction: column; gap: 8px; width: 220px;
  }
  .concierge-card.open { display: flex; }
  .concierge-link {
    display: flex; align-items: center; gap: 8px; padding: 7px 12px; border-radius: 6px;
    text-decoration: none; font-size: 12.5px; font-weight: 600; color: #fff;
  }
  .concierge-link.wa { background: #25D366; }
  .concierge-link.tg { background: #229ed9; }

  /* ── Toast ── */
  .toast {
    position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%) translateY(100px);
    background: #0f172a; color: #ffffff; padding: 10px 20px; border-radius: 8px;
    font-size: 13px; font-weight: 600; box-shadow: 0 10px 25px rgba(0,0,0,0.25);
    display: flex; align-items: center; gap: 8px; z-index: 10000; opacity: 0;
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); pointer-events: none;
  }
  .toast.show { transform: translateX(-50%) translateY(0); opacity: 1; }

  /* ── Section Structure ── */
  .section { padding: 56px 0; border-bottom: 1px solid var(--border); position: relative; }
  .section-hdr { text-align: center; margin-bottom: 30px; }
  .section-tag {
    font-family: var(--mono); font-size: 11px; font-weight: 700; color: var(--cyan);
    letter-spacing: 0.1em; text-transform: uppercase; margin-bottom: 8px; display: inline-block;
  }
  .section-title {
    font-size: clamp(22px, 3vw, 32px); font-weight: 800; letter-spacing: -0.01em; margin-bottom: 10px; color: var(--text);
  }
  .section-lede {
    font-size: 14.5px; color: var(--text-dim); max-width: 680px; margin: 0 auto; line-height: 1.55;
  }

  /* ── Footer ── */
  footer {
    background: #ffffff; border-top: 1px solid var(--border); padding: 40px 0 24px;
  }
  .footer-inner {
    display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 20px;
    margin-bottom: 20px;
  }
  .footer-links { display: flex; gap: 16px; font-size: 13px; flex-wrap: wrap; }
  .footer-links a { color: var(--text-faint); text-decoration: none; font-weight: 500; }
  .footer-links a:hover { color: var(--cyan); }
  .footer-copy { font-size: 11.5px; color: var(--text-faint); font-family: var(--mono); }
</style>
</head>
<body>

<!-- Top Status Strip -->
<div class="top-strip">
  <div class="wrap top-strip-inner">
    <div class="top-status-badge">
      <span class="pulse-dot"></span>
      <span data-i18n="top_status">PASA SENTINEL ONLINE // SOVEREIGN C2 ACTIVE</span>
    </div>
    <div class="top-specs">
      <div>ANTI-UNINSTALL: <span>KNOX SUPERVISOR</span></div>
      <div>STORAGE: <span>0-CLOUD TRAILS</span></div>
      <div>FALLBACK: <span>AIR-GAPPED SMS</span></div>
    </div>
  </div>
</div>

<!-- Navigation -->
<nav class="nav">
  <div class="wrap nav-inner">
    <a href="#" class="brand">
      <img src="/assets/img/logo.png" alt="PASA Sentinel Logo" class="brand-logo-img">
      <div>
        <div class="brand-text">PASA SENTINEL</div>
      </div>
      <span class="brand-tag">SOVEREIGN C2</span>
    </a>

    <!-- Desktop Navigation Menu -->
    <div class="nav-menu">
      <a href="#simulator" class="nav-link" data-i18n="nav_sim">Forensic Lab</a>
      <a href="#dossier" class="nav-link" data-i18n="nav_dossier">Evidence Wall</a>
      <a href="#setup-kit" class="nav-link" data-i18n="nav_setup_kit">Setup Kit</a>
      <a href="#features" class="nav-link" data-i18n="nav_features">Capabilities</a>
      <a href="#commands" class="nav-link" data-i18n="nav_commands">86 Commands</a>
      <a href="#comparison" class="nav-link" data-i18n="nav_comparison">Comparison</a>
      <a href="#faq" class="nav-link" data-i18n="nav_faq">Master FAQ</a>
      <a href="#pricing" class="nav-link" data-i18n="nav_pricing">Licensing</a>
    </div>

    <!-- Actions -->
    <div class="nav-actions">
      <!-- Sound FX Toggle -->
      <button class="sound-btn" id="btnSoundToggle" onclick="toggleSoundFx()" title="Toggle Cinematic Sound Effects">
        <span id="soundIcon">🔇</span> <span id="soundLabel">SOUND: OFF</span>
      </button>

      <div class="lang-switcher">
        <button class="lang-btn active" id="btnEn" onclick="setLanguage('en')">EN</button>
        <button class="lang-btn" id="btnBn" onclick="setLanguage('bn')">বাংলা</button>
      </div>
      <a href="/releases/pasa-latest.apk" class="nav-cta-secondary" title="Download Tactical APK">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 3v12m0 0-4-4m4 4 4-4M4 21h16"/></svg>
        <span data-i18n="nav_download_pill">Get APK</span>
      </a>
      <button class="mobile-toggle" onclick="toggleMobileDrawer()" aria-label="Open Navigation">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
      </button>
    </div>
  </div>
</nav>

<!-- Mobile Drawer -->
<div class="drawer-overlay" id="drawerOverlay" onclick="toggleMobileDrawer()"></div>
<div class="mobile-drawer" id="mobileDrawer">
  <div class="mobile-drawer-hdr">
    <div class="brand-text">PASA SENTINEL</div>
    <span onclick="toggleMobileDrawer()" style="cursor:pointer;font-size:22px;">&times;</span>
  </div>
  <a href="#simulator" onclick="toggleMobileDrawer()" data-i18n="nav_sim">Forensic Lab</a>
  <a href="#dossier" onclick="toggleMobileDrawer()" data-i18n="nav_dossier">Evidence Wall</a>
  <a href="#setup-kit" onclick="toggleMobileDrawer()" data-i18n="nav_setup_kit">Setup Kit</a>
  <a href="#features" onclick="toggleMobileDrawer()" data-i18n="nav_features">Capabilities</a>
  <a href="#commands" onclick="toggleMobileDrawer()" data-i18n="nav_commands">86 Commands</a>
  <a href="#comparison" onclick="toggleMobileDrawer()" data-i18n="nav_comparison">Comparison</a>
  <a href="#faq" onclick="toggleMobileDrawer()" data-i18n="nav_faq">Master FAQ</a>
  <a href="#pricing" onclick="toggleMobileDrawer()" data-i18n="nav_pricing">Licensing</a>
  <div style="margin-top:auto;padding-top:16px;">
    <a href="/releases/pasa-latest.apk" class="btn-primary" style="width:100%;justify-content:center;" data-i18n="hero_cta_apk">
      Download Tactical APK
    </a>
  </div>
</div>

<!-- ── HERO SECTION: 3D FORENSIC PHONE & CINEMATIC ACTION TRIGGER ── -->
<section class="hero" id="simulator">
  <div class="wrap">
    <div class="hero-grid">
      <!-- Left Column: Core Value Proposition & CTAs -->
      <div class="hero-col-left">
        <div class="badge-tag">
          <span class="pulse-dot"></span>
          <span data-i18n="hero_badge">SOVEREIGN MOBILE DEFENSE // ANDROID 8.0 – 16</span>
        </div>
        <h1 class="hero-title" data-i18n="hero_title">YOUR PHONE WILL NEVER SURRENDER.</h1>
        <p class="hero-sub" data-i18n="hero_sub">
          When standard trackers go blind in 10 seconds, PASA Sentinel enforces Knox-grade hardware lockdown, captures perpetrator forensics headlessly, and protects your sovereign personal data.
        </p>

        <!-- Live Snatch Trigger Banner -->
        <div style="margin-bottom: 22px;">
          <button class="btn-simulate-snatch" onclick="runSnatchSimulation()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
            <span data-i18n="btn_sim_label">🚨 SIMULATE PHONE SNATCH INTERCEPT</span>
          </button>
        </div>

        <div class="hero-cta-group">
          <a href="/releases/pasa-latest.apk" class="btn-primary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 3v12m0 0-4-4m4 4 4-4M4 21h16"/></svg>
            <span data-i18n="hero_cta_apk">Download Tactical APK</span>
          </a>
          <a href="/releases/PASA-Device-Owner-Setup-Kit.zip" class="btn-setup" title="Download Windows Device Owner Setup Kit">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
            <span data-i18n="hero_cta_setup">Device Owner Setup Kit (PC)</span>
          </a>
        </div>

        <div class="specs-bar-compact">
          <div class="spec-pill crimson">
            <span>🛡️</span> <span>UNINSTALL:</span> <strong data-i18n="pill_knox">KNOX IMMUTABLE</strong>
          </div>
          <div class="spec-pill">
            <span>☁️</span> <span>STORAGE:</span> <strong data-i18n="pill_zero">0-CLOUD RESIDUE</strong>
          </div>
          <div class="spec-pill emerald">
            <span>📶</span> <span>C2 LINK:</span> <strong data-i18n="pill_sms">TELEGRAM + AIR-GAP SMS</strong>
          </div>
        </div>
      </div>

      <!-- Right Column: 3D Holographic Forensic Phone Simulator -->
      <div class="hero-col-right">
        <div class="phone-stage-wrapper" id="phoneStage">
          <div class="phone-mockup-3d" id="phoneMockup">
            <!-- Hardware Notch -->
            <div class="phone-notch">
              <div class="notch-cam-dot"></div>
              <div class="notch-speaker"></div>
            </div>

            <!-- Screen Viewport -->
            <div class="phone-screen">
              <div class="camera-flash-overlay" id="camFlash"></div>
              <div class="screen-overlay-scanlines"></div>
              <canvas id="forensicCamCanvas" width="320" height="500"></canvas>

              <!-- Rubber Stamp -->
              <div class="stamp-cinematic" id="stampCinematic">
                <div style="font-size:13px;letter-spacing:0.06em;" data-i18n="stamp_top">★ CAUGHT RED-HANDED ★</div>
                <div style="font-size:10px;font-weight:700;margin-top:2px;" data-i18n="stamp_sub">// হাতেনাতে অপরাধী আটক //</div>
                <div style="font-size:8.5px;color:#fca5a5;font-family:var(--mono);margin-top:2px;">EVIDENCE #PAS-8492 // 99.8% MATCH</div>
              </div>

              <!-- HUD Diagnostics Strip -->
              <div class="phone-hud-overlay">
                <div class="hud-stat-badge">
                  <span>TRAP: <span class="red" id="hudStatTrap">ARMED // 2.65G</span></span>
                  <span id="hudStatTime">00:01.48s</span>
                </div>

                <div class="hud-stat-badge" style="background:rgba(2,6,23,0.85);">
                  <span>GNSS: <span class="cyan">23.8103°N, 90.4125°E</span></span>
                  <span>USB: <span class="red" id="hudStatUsb">KILL [0V]</span></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- Floating Telegram Push Notification Card -->
<div class="telegram-alert-card" id="telegramAlertCard">
  <div class="tg-card-hdr">
    <div style="display:flex;align-items:center;gap:6px;">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="#38bdf8"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69a.2.2 0 00-.05-.18c-.06-.05-.14-.03-.21-.02-.09.02-1.49.95-4.22 2.79-.4.27-.76.41-1.08.4-.36-.01-1.04-.2-1.55-.37-.63-.2-1.12-.31-1.08-.66.02-.18.27-.36.75-.55 2.92-1.27 4.86-2.11 5.83-2.51 2.78-1.16 3.35-1.36 3.73-1.36.08 0 .27.02.39.12.1.08.13.19.14.27-.01.06.01.24 0 .38z"/></svg>
      <span>TELEGRAM • PASA SENTINEL C2</span>
    </div>
    <span onclick="dismissTgAlert()" style="cursor:pointer;color:#94a3b8;font-size:15px;">&times;</span>
  </div>
  <div class="tg-card-body">
    <div class="tg-suspect-thumb">
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
    </div>
    <div>
      <div class="tg-card-text">
        <strong data-i18n="tg_alert_title">🚨 CRITICAL THEFT INTERCEPT:</strong><br>
        <span data-i18n="tg_alert_desc">Snatch trap triggered! Mugshot captured &amp; RAM shredded.</span>
      </div>
      <div class="tg-card-meta">
        📍 23.8103°N, 90.4125°E • <span style="color:#10b981;">TELEGRAM STREAM [OK]</span>
      </div>
    </div>
  </div>
</div>

<!-- ── DETECTIVE EVIDENCE WALL (MISSION DOSSIER) ── -->
<section id="dossier" class="section" style="background:#ffffff;">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="dossier_tag">FORENSIC CRIME EVIDENCE WALL</span>
      <h2 class="section-title" data-i18n="dossier_title">4 Physical Countermeasures That Outsmart Professional Thieves</h2>
      <p class="section-lede" data-i18n="dossier_lede">
        Standard Play Store apps run in restricted sandboxes and depend entirely on continuous internet. PASA Sentinel operates at the Device Owner supervisor layer.
      </p>
    </div>

    <div class="evidence-board">
      <div class="evidence-grid">
        <!-- Exhibit A: Knox Anti-Uninstall -->
        <div class="evidence-pin">
          <div>
            <div class="evidence-tag">
              <span>EXHIBIT A // UNINSTALL</span>
              <span>🔒 100% IMMUTABLE</span>
            </div>
            <h3 class="evidence-title" data-i18n="d1_title">Knox Device Owner Permanence</h3>
            <p class="evidence-desc" data-i18n="d1_desc">
              Configured as system supervisor. Android itself disables "Uninstall" and "Force Stop". Safe Mode, factory resets, and notification shade pulldowns are permanently neutralized.
            </p>
          </div>
          <div style="margin-top:14px;font-family:var(--mono);font-size:11px;color:var(--cyan);font-weight:700;">
            POL-DPM-LOCK_TASK: IMMUTABLE
          </div>
        </div>

        <!-- Exhibit B: Zero-Storage RAM Shred -->
        <div class="evidence-pin">
          <div>
            <div class="evidence-tag">
              <span>EXHIBIT B // PRIVACY</span>
              <span>✨ 0 CLOUD TRAILS</span>
            </div>
            <h3 class="evidence-title" data-i18n="d2_title">Strategy 1: 100% Zero-Storage</h3>
            <p class="evidence-desc" data-i18n="d2_desc">
              Zero photos, zero GPS tracks, zero audio recordings saved on servers. Media streams direct to your private Telegram bot and is shredded from device RAM immediately.
            </p>
          </div>
          <div style="margin-top:14px;font-family:var(--mono);font-size:11px;color:var(--emerald);font-weight:700;">
            RAM BUFFER SHRED: PURGED
          </div>
        </div>

        <!-- Exhibit C: Air-Gapped Cellular SMS -->
        <div class="evidence-pin">
          <div>
            <div class="evidence-tag">
              <span>EXHIBIT C // OFFLINE C2</span>
              <span>📶 AIR-GAPPED SMS</span>
            </div>
            <h3 class="evidence-title" data-i18n="d3_title">Air-Gapped Cellular SMS C2</h3>
            <p class="evidence-desc" data-i18n="d3_desc">
              Maintains full command authority even when mobile data, Wi-Fi, and location are shut off. Authenticated via RFC 6238 TOTP tokens or master PIN, returning live GPS pins via cellular SMS.
            </p>
          </div>
          <div style="margin-top:14px;font-family:var(--mono);font-size:11px;color:var(--cyan);font-weight:700;">
            RFC 6238 TOTP: ACTIVE
          </div>
        </div>

        <!-- Exhibit D: Decoy OS Coercion -->
        <div class="evidence-pin">
          <div>
            <div class="evidence-tag">
              <span>EXHIBIT D // COERCION</span>
              <span>🎭 STERILE DECOY</span>
            </div>
            <h3 class="evidence-title" data-i18n="d4_title">Armed Coercion Decoy Sandbox</h3>
            <p class="evidence-desc" data-i18n="d4_desc">
              Under gunpoint or checkpoint coercion, entering your secret Decoy PIN instantly unlocks a sterile decoy phone while covertly snapping the coercer's mugshot and transmitting an emergency SOS.
            </p>
          </div>
          <div style="margin-top:14px;font-family:var(--mono);font-size:11px;color:var(--amber);font-weight:700;">
            DURESS PIN: DECOY ACTIVE
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- ── 1-CLICK DEVICE OWNER SETUP KIT ── -->
<section id="setup-kit" class="section">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="setup_tag">WINDOWS 1-CLICK PROVISIONING WIZARD</span>
      <h2 class="section-title" data-i18n="setup_title">Empower Your Android With Unbreakable Device Owner Privileges</h2>
      <p class="section-lede" data-i18n="setup_lede">
        Zero command-line expertise required. Our guided Windows setup kit automatically handles ADB pairing, cleans account restrictions, and grants Knox Device Owner supervisor permission in under 60 seconds.
      </p>
    </div>

    <!-- 3-Step Setup Process Grid -->
    <div class="setup-grid">
      <div class="setup-card">
        <div class="setup-step-num">1</div>
        <div class="setup-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
        </div>
        <h3 class="setup-card-title" data-i18n="setup_s1_title">Install Tactical APK</h3>
        <p class="setup-card-desc" data-i18n="setup_s1_desc">
          Download and install PASA Sentinel directly on your Android device (Android 8.0–16). Open the app and grant required permissions.
        </p>
      </div>

      <div class="setup-card">
        <div class="setup-step-num">2</div>
        <div class="setup-icon cyan">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v8m0 0-3-3m3 3 3-3"/><path d="M4 14v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4"/><rect x="8" y="10" width="8" height="4" rx="1"/></svg>
        </div>
        <h3 class="setup-card-title" data-i18n="setup_s2_title">Download PC Setup Kit</h3>
        <p class="setup-card-desc" data-i18n="setup_s2_desc">
          Download the lightweight Windows ZIP kit on your PC. Extract it to any folder and double-click <strong>PASA Device Owner Setup.bat</strong>.
        </p>
      </div>

      <div class="setup-card">
        <div class="setup-step-num">3</div>
        <div class="setup-icon emerald">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><polyline points="9 12 11 14 15 10"/></svg>
        </div>
        <h3 class="setup-card-title" data-i18n="setup_s3_title">Plug In &amp; Activate</h3>
        <p class="setup-card-desc" data-i18n="setup_s3_desc">
          Connect your phone via USB with USB Debugging enabled. The automated wizard provisions Device Owner status in one click. Uninstallation is now permanently blocked!
        </p>
      </div>
    </div>

    <!-- Setup Kit CTA Banner -->
    <div class="setup-cta-banner">
      <div class="setup-cta-info">
        <div class="setup-cta-badge">
          <span class="pulse-dot"></span>
          <span>OFFICIAL WINDOWS UTILITY // COMPATIBLE WITH WIN 10 &amp; 11</span>
        </div>
        <h3 class="setup-cta-title" data-i18n="setup_banner_title">Ready to Lock Down Your Hardware?</h3>
        <p class="setup-cta-desc" data-i18n="setup_banner_desc">
          Includes automated Google platform-tools ADB downloader, account checker, and uninstallation supervisor provisioning.
        </p>
      </div>
      <div class="setup-cta-actions">
        <a href="/releases/PASA-Device-Owner-Setup-Kit.zip" class="btn-primary" style="white-space:nowrap;">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          <span data-i18n="btn_download_kit">Download Windows Setup Kit (.ZIP)</span>
        </a>
      </div>
    </div>
  </div>
</section>

<!-- ── INTERACTIVE 8-HUB DEFENSE EXPLORER ── -->
<section id="features" class="section" style="background:#ffffff;">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="feat_tag">TACTICAL DEFENSE ARSENAL</span>
      <h2 class="section-title" data-i18n="feat_title">8-Hub Covert Defense Capabilities</h2>
      <p class="section-lede" data-i18n="feat_lede">
        Battle-tested countermeasures built specifically to defeat professional criminal theft, forensic extractions, and armed robbery.
      </p>
    </div>

    <!-- Hub Category Switcher Tabs -->
    <div class="hub-controls">
      <div class="hub-tabs" id="hubTabs">
        <!-- Rendered dynamically by JS -->
      </div>
    </div>

    <!-- Dynamic Feature Cards Grid -->
    <div class="features-grid-rich" id="featureCardsContainer">
      <!-- Rendered dynamically by JS -->
    </div>
  </div>
</section>

<!-- ── INTERACTIVE 86-COMMAND C2 TERMINAL ── -->
<section id="commands" class="section">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="cmd_tag">COMMAND CONSOLE REPERTOIRE</span>
      <h2 class="section-title" data-i18n="cmd_title">86 Telegram &amp; Air-Gapped SMS Commands</h2>
      <p class="section-lede" data-i18n="cmd_lede">
        Every single hardware defensive action is remotely triggerable via Telegram Bot C2 or cellular SMS. Click any command to copy its syntax.
      </p>
    </div>

    <div class="cmd-controls">
      <input type="text" id="commandSearch" class="cmd-search" placeholder="Search 86 C2 commands (e.g. snap, usb_lock, duress, sim_lock, locate, deadman)..." oninput="filterCommands()">
      <div class="cmd-pills" id="commandPills">
        <!-- Rendered dynamically by JS -->
      </div>
    </div>

    <div class="cmd-grid" id="commandsContainer">
      <!-- Rendered dynamically by JS -->
    </div>
  </div>
</section>

<!-- ── ARCHITECTURAL COMPARISON MATRIX ── -->
<section id="comparison" class="section" style="background:#ffffff;">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="comp_tag">ARCHITECTURAL COMPARISON</span>
      <h2 class="section-title" data-i18n="comp_title">PASA Sentinel vs Traditional Solutions</h2>
      <p class="section-lede" data-i18n="comp_lede">
        Why standard Google and third-party tracking apps fail during real-world phone theft scenarios.
      </p>
    </div>

    <div class="table-container">
      <table class="comp-table">
        <thead>
          <tr>
            <th data-i18n="th_param">Security Parameter</th>
            <th class="comp-highlight">PASA Sentinel (Device Owner)</th>
            <th data-i18n="th_google">Google Find My Device</th>
            <th data-i18n="th_play">Play Store Anti-Theft Apps</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><strong data-i18n="td_priv">OS Privileges</strong></td>
            <td class="comp-highlight">Device Owner Supervisor</td>
            <td>Normal User Mode</td>
            <td>Standard User Sandbox</td>
          </tr>
          <tr>
            <td><strong data-i18n="td_uninst">Anti-Uninstall Immunity</strong></td>
            <td class="comp-highlight"><span class="check-icon">✓</span> Immutable (OS Lockout)</td>
            <td><span class="cross-icon">✗</span> Account Disconnectable</td>
            <td><span class="cross-icon">✗</span> Removable in Safe Mode</td>
          </tr>
          <tr>
            <td><strong data-i18n="td_offline">Offline SMS C2 (No Internet)</strong></td>
            <td class="comp-highlight"><span class="check-icon">✓</span> Full SMS C2 with TOTP</td>
            <td><span class="cross-icon">✗</span> Requires Internet</td>
            <td><span class="cross-icon">✗</span> Most require Internet</td>
          </tr>
          <tr>
            <td><strong data-i18n="td_storage">Cloud Media Storage</strong></td>
            <td class="comp-highlight"><span class="check-icon">✓</span> Zero (Direct to Telegram)</td>
            <td>N/A (No Photos)</td>
            <td><span class="cross-icon">✗</span> Stored on Company Cloud</td>
          </tr>
          <tr>
            <td><strong data-i18n="td_sim">SIM Ejection Countermeasure</strong></td>
            <td class="comp-highlight"><span class="check-icon">✓</span> Auto-Lock + Thief Caller ID Trap</td>
            <td><span class="cross-icon">✗</span> None (Goes Offline)</td>
            <td><span class="cross-icon">✗</span> Restricted by Android 10+</td>
          </tr>
          <tr>
            <td><strong data-i18n="td_usb">USB Forensic Port Blocker</strong></td>
            <td class="comp-highlight"><span class="check-icon">✓</span> Hardware Data Pin Cut</td>
            <td><span class="cross-icon">✗</span> None</td>
            <td><span class="cross-icon">✗</span> None</td>
          </tr>
          <tr>
            <td><strong data-i18n="td_model">Licensing Model</strong></td>
            <td class="comp-highlight">Lifetime ($25 / ৳3,000)</td>
            <td>Free (Ineffective)</td>
            <td>$30–$60 / Year Recurring</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</section>

<!-- ── MASTER TECHNICAL & LEGAL FAQ (COMPACT TABBED) ── -->
<section id="faq" class="section">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="faq_tag">TECHNICAL &amp; LEGAL DOSSIER</span>
      <h2 class="section-title" data-i18n="faq_title">Frequently Asked Questions</h2>
      <p class="section-lede" data-i18n="faq_lede">
        Straightforward, technical answers to skeptical and critical security questions.
      </p>
    </div>

    <div class="faq-controls">
      <input type="text" id="faqSearch" class="faq-search" placeholder="Search questions (e.g., Knox, battery, police, root, SIM)..." oninput="filterFaq()">
      <div class="faq-pills" id="faqPills">
        <!-- Rendered dynamically by JS -->
      </div>
    </div>

    <div class="faq-list" id="faqAccordion">
      <!-- Injected dynamically via JS -->
    </div>
  </div>
</section>

<!-- ── PRICING & LICENSING ── -->
<section id="pricing" class="section" style="background:#ffffff;">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="price_tag">SOVEREIGN COMMERCIAL LICENSING</span>
      <h2 class="section-title" data-i18n="price_title">Zero Recurring Traps. Permanent Ownership.</h2>
      <p class="section-lede" data-i18n="price_lede">
        We do not believe in predatory monthly subscriptions. Secure your hardware once with an offline Ed25519 cryptographic license.
      </p>
    </div>

    <div class="pricing-grid">
      <!-- 7-Day Tactical Evaluation -->
      <div class="plan-card">
        <div>
          <h3 class="plan-title" data-i18n="plan_eval_title">Tactical Evaluation</h3>
          <p class="plan-desc" data-i18n="plan_eval_desc">Test core telemetry and verification on your personal hardware.</p>
          <div class="plan-price-wrap">
            <div class="plan-price">FREE</div>
            <div class="plan-price-sub" data-i18n="plan_eval_period">7-Day Tactical Evaluation</div>
          </div>
          <ul class="plan-features">
            <li><span>✓</span> <span data-i18n="f_eval_1">Essential Telegram C2 Commands</span></li>
            <li><span>✓</span> <span data-i18n="f_eval_2">Headless Camera Capture Test</span></li>
            <li><span>✓</span> <span data-i18n="f_eval_3">GPS &amp; Cell Tower Telemetry</span></li>
            <li><span>✓</span> <span data-i18n="f_eval_4">Community Telegram Support</span></li>
          </ul>
        </div>
        <a href="https://t.me/pasa_sentinel_bot" target="_blank" class="btn-plan outline" data-i18n="btn_start_eval">
          Activate via Bot
        </a>
      </div>

      <!-- Pro Lifetime Shield (Popular) -->
      <div class="plan-card popular">
        <span class="plan-badge" data-i18n="badge_most_popular">MOST POPULAR DEFENSE</span>
        <div>
          <h3 class="plan-title" data-i18n="plan_pro_title">Pro Lifetime Shield</h3>
          <p class="plan-desc" data-i18n="plan_pro_desc">Complete sovereign defense suite for 1 Android device forever.</p>
          <div class="plan-price-wrap">
            <div class="plan-price">$25 <span style="font-size:16px;color:var(--text-faint);font-weight:600;">/ ৳3,000</span></div>
            <div class="plan-price-sub" data-i18n="plan_pro_period">One-time payment • Lifetime OTA Updates</div>
          </div>
          <ul class="plan-features">
            <li><span>✓</span> <span data-i18n="f_pro_1">All 86 Telegram C2 Commands</span></li>
            <li><span>✓</span> <span data-i18n="f_pro_2">Knox-Grade Device Owner Provisioning</span></li>
            <li><span>✓</span> <span data-i18n="f_pro_3">Hardware Escrow Token PIN Reset</span></li>
            <li><span>✓</span> <span data-i18n="f_pro_4">Anti-EDL/BROM Dead Man's Switch</span></li>
            <li><span>✓</span> <span data-i18n="f_pro_5">SIM Ejection Foreign Number Trap</span></li>
            <li><span>✓</span> <span data-i18n="f_pro_6">1-on-1 Personal Remote Setup Onboarding</span></li>
            <li><span>✓</span> <span data-i18n="f_pro_7">24-Hour 100% Refund Guarantee</span></li>
          </ul>
        </div>
        <a href="https://wa.me/8801728284848?text=Hello%20PASA%20Team%2C%20I%20want%20to%20activate%20PASA%20Pro%20Lifetime%20Shield." target="_blank" class="btn-plan primary" data-i18n="btn_buy_pro">
          Claim Lifetime License
        </a>
      </div>

      <!-- Enterprise Fleet -->
      <div class="plan-card">
        <div>
          <h3 class="plan-title" data-i18n="plan_ent_title">Enterprise Fleet</h3>
          <p class="plan-desc" data-i18n="plan_ent_desc">VIP executive defense, corporate fleets, and high-risk field agents.</p>
          <div class="plan-price-wrap">
            <div class="plan-price">$99 <span style="font-size:16px;color:var(--text-faint);font-weight:600;">/ ৳12,000</span></div>
            <div class="plan-price-sub" data-i18n="plan_ent_period">5 Devices Pack • Dedicated Control Node</div>
          </div>
          <ul class="plan-features">
            <li><span>✓</span> <span data-i18n="f_ent_1">5x Pro Lifetime Device Licenses</span></li>
            <li><span>✓</span> <span data-i18n="f_ent_2">Dedicated Private Relay Server Node</span></li>
            <li><span>✓</span> <span data-i18n="f_ent_3">Zero-Knowledge Fleet Management</span></li>
            <li><span>✓</span> <span data-i18n="f_ent_4">Direct WhatsApp &amp; Telegram Hotline</span></li>
          </ul>
        </div>
        <a href="https://wa.me/8801728284848?text=Hello%20PASA%20Team%2C%20I%20am%20interested%20in%20the%20Enterprise%20Fleet%20pack." target="_blank" class="btn-plan outline" data-i18n="btn_contact_ent">
          Contact Concierge
        </a>
      </div>
    </div>

    <!-- Payment Channels Strip -->
    <div class="payment-channels">
      <div class="pay-method">
        <div class="pay-icon" style="background:#fef9c3;color:#ca8a04;">
          <img src="/binance-logo.svg" alt="Binance Pay" width="26" height="26">
        </div>
        <div>
          <div class="pay-title">Binance Pay (Crypto)</div>
          <div class="pay-sub">UID: 756303714 (RBR48)</div>
          <button onclick="copyBinanceUid()" class="pay-action-btn" style="background:#fef9c3;color:#854d0e;border:1px solid #fef08a;" data-i18n="btn_copy_uid">Copy UID</button>
        </div>
      </div>

      <div class="pay-method">
        <div class="pay-icon" style="background:#fdf2f8;color:#db2777;">
          <img src="/bkash-icon.svg" alt="bKash" width="26" height="26">
        </div>
        <div>
          <div class="pay-title" data-i18n="bkash_title">bKash Payment (Bangladesh)</div>
          <div class="pay-sub" data-i18n="bkash_sub">Official bKash Personal account</div>
          <a href="https://wa.me/8801728284848?text=Hello%2C%20please%20send%20me%20the%20official%20bKash%20number%20for%20PASA%20Sentinel%20license." target="_blank" class="pay-action-btn" style="background:#fce7f3;color:#9d174d;border:1px solid #fbcfe8;" data-i18n="bkash_action_btn">Request Number via WhatsApp</a>
        </div>
      </div>

      <div class="pay-method">
        <div class="pay-icon" style="background:#f0fdf4;color:#16a34a;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
        </div>
        <div>
          <div class="pay-title" data-i18n="wa_title">WhatsApp Concierge</div>
          <div class="pay-sub">+880 1728 284848</div>
          <a href="https://wa.me/8801728284848" target="_blank" class="pay-action-btn" style="background:#dcfce7;color:#15803d;border:1px solid #bbf7d0;" data-i18n="wa_action_btn">Chat Direct</a>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- Floating Concierge -->
<div class="floating-concierge">
  <div class="concierge-card" id="conciergeCard">
    <div style="font-size:12px;font-weight:700;color:var(--text);margin-bottom:4px;" data-i18n="wa_title">Live Concierge</div>
    <a href="https://wa.me/8801728284848" target="_blank" class="concierge-link wa">WhatsApp Concierge</a>
    <a href="https://t.me/rbr_48" target="_blank" class="concierge-link tg">Telegram Developer</a>
  </div>
  <div class="concierge-pill" onclick="toggleConcierge()">
    <span class="pulse-dot"></span>
    <span data-i18n="floating_help">Live Assistance</span>
  </div>
</div>

<!-- Copy Toast Notification -->
<div class="toast" id="cmdToast">
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
  <span id="cmdToastMsg">Copied!</span>
</div>

<!-- Footer -->
<footer>
  <div class="wrap">
    <div class="footer-inner">
      <div class="brand">
        <img src="/assets/img/logo.png" alt="PASA Logo" class="brand-logo-img" style="width:28px;height:28px;">
        <span class="brand-text" style="font-size:15px;">PASA SENTINEL</span>
      </div>
      <div class="footer-links">
        <a href="#simulator" data-i18n="nav_sim">Forensic Lab</a>
        <a href="#dossier" data-i18n="nav_dossier">Evidence Wall</a>
        <a href="#setup-kit" data-i18n="nav_setup_kit">Setup Kit</a>
        <a href="#features" data-i18n="nav_features">Capabilities</a>
        <a href="#commands" data-i18n="nav_commands">86 Commands</a>
        <a href="#faq" data-i18n="nav_faq">Master FAQ</a>
        <a href="#pricing" data-i18n="nav_pricing">Licensing</a>
        <a href="/privacy.html">Privacy Policy</a>
        <a href="/terms.html">Terms of Service</a>
      </div>
    </div>
    <div style="font-size:12px;color:var(--text-faint);line-height:1.6;margin-bottom:12px;" data-i18n="footer_bio">
      Sovereign Android anti-theft defense &amp; covert intelligence agent. Zero Google Play dependencies, zero cloud media storage, Knox-grade uninstallation lockout. Engineered by Izhaan Intellect.
    </div>
    <div class="footer-copy">
      &copy; ${new Date().getFullYear()} Izhaan Intellect. All rights reserved. Sovereign Cybersecurity Division.
    </div>
  </div>
</footer>

<script>
const rawHubs = ${JSON.stringify(featureHubs)};
const rawCommands = ${JSON.stringify(commandsMatrix)};
const rawFaq = ${JSON.stringify(faqData)};

const translations = {
  en: {
    top_status: "PASA SENTINEL ONLINE // SOVEREIGN C2 ACTIVE",
    nav_sim: "Forensic Lab",
    nav_dossier: "Evidence Wall",
    nav_setup_kit: "Setup Kit",
    nav_features: "Capabilities",
    nav_commands: "86 Commands",
    nav_comparison: "Comparison",
    nav_faq: "Master FAQ",
    nav_pricing: "Licensing",
    nav_download: "Download Tactical APK",
    nav_download_pill: "Get APK",

    hero_badge: "SOVEREIGN MOBILE DEFENSE // ANDROID 8.0 – 16",
    hero_title: "YOUR PHONE WILL NEVER SURRENDER.",
    hero_sub: "When standard trackers go blind in 10 seconds, PASA Sentinel enforces Knox-grade hardware lockdown, captures perpetrator forensics headlessly, and protects your sovereign personal data.",
    hero_cta_apk: "Download Tactical APK",
    hero_cta_setup: "Device Owner Setup Kit (PC)",

    pill_knox: "KNOX IMMUTABLE",
    pill_zero: "0-CLOUD RESIDUE",
    pill_sms: "TELEGRAM + AIR-GAP SMS",

    btn_sim_label: "🚨 SIMULATE PHONE SNATCH INTERCEPT",
    stamp_top: "★ CAUGHT RED-HANDED ★",
    stamp_sub: "// PERPETRATOR FACE IDENTIFIED //",

    tg_alert_title: "🚨 CRITICAL THEFT INTERCEPT:",
    tg_alert_desc: "Snatch vector 2.65G detected! Perpetrator mugshot acquired & RAM shredded.",

    dossier_tag: "FORENSIC CRIME EVIDENCE WALL",
    dossier_title: "4 Physical Countermeasures That Outsmart Professional Thieves",
    dossier_lede: "Standard Play Store apps run in restricted sandboxes and depend entirely on continuous internet. PASA Sentinel operates at the Device Owner supervisor layer.",
    d1_title: "Knox Device Owner Permanence",
    d1_desc: "Configured as system supervisor. Android itself disables 'Uninstall' and 'Force Stop'. Safe Mode, factory resets, and notification shade pulldowns are permanently neutralized.",
    d2_title: "Strategy 1: 100% Zero-Storage",
    d2_desc: "Zero photos, zero GPS tracks, zero audio recordings saved on servers. Media streams direct to your private Telegram bot and is shredded from device RAM immediately.",
    d3_title: "Air-Gapped Cellular SMS C2",
    d3_desc: "Maintains full command authority even when mobile data, Wi-Fi, and location are shut off. Authenticated via RFC 6238 TOTP tokens or master PIN, returning live GPS pins via cellular SMS.",
    d4_title: "Armed Coercion Decoy Sandbox",
    d4_desc: "Under gunpoint or checkpoint coercion, entering your secret Decoy PIN instantly unlocks a sterile decoy phone while covertly snapping the coercer's mugshot and transmitting an emergency SOS.",

    setup_tag: "WINDOWS 1-CLICK PROVISIONING WIZARD",
    setup_title: "Empower Your Android With Unbreakable Device Owner Privileges",
    setup_lede: "Zero command-line expertise required. Our guided Windows setup kit automatically handles ADB pairing, cleans account restrictions, and grants Knox Device Owner supervisor permission in under 60 seconds.",
    setup_s1_title: "Install Tactical APK",
    setup_s1_desc: "Download and install PASA Sentinel directly on your Android device (Android 8.0–16). Open the app and grant required permissions.",
    setup_s2_title: "Download PC Setup Kit",
    setup_s2_desc: "Download the lightweight Windows ZIP kit on your PC. Extract it to any folder and double-click PASA Device Owner Setup.bat.",
    setup_s3_title: "Plug In & Activate",
    setup_s3_desc: "Connect your phone via USB with USB Debugging enabled. The automated wizard provisions Device Owner status in one click. Uninstallation is now permanently blocked!",
    setup_banner_title: "Ready to Lock Down Your Hardware?",
    setup_banner_desc: "Includes automated Google platform-tools ADB downloader, account checker, and uninstallation supervisor provisioning.",
    btn_download_kit: "Download Windows Setup Kit (.ZIP)",

    feat_tag: "TACTICAL DEFENSE ARSENAL",
    feat_title: "8-Hub Covert Defense Capabilities",
    feat_lede: "Battle-tested countermeasures built specifically to defeat professional criminal theft, forensic extractions, and armed robbery.",

    cmd_tag: "COMMAND CONSOLE REPERTOIRE",
    cmd_title: "86 Telegram & Air-Gapped SMS Commands",
    cmd_lede: "Every single hardware defensive action is remotely triggerable via Telegram Bot C2 or cellular SMS. Click any command to copy its syntax.",

    comp_tag: "ARCHITECTURAL COMPARISON",
    comp_title: "PASA Sentinel vs Traditional Solutions",
    comp_lede: "Why standard Google and third-party tracking apps fail during real-world phone theft scenarios.",
    th_param: "Security Parameter",
    th_google: "Google Find My Device",
    th_play: "Play Store Anti-Theft Apps",
    td_priv: "OS Privileges",
    td_uninst: "Anti-Uninstall Immunity",
    td_offline: "Offline SMS C2 (No Internet)",
    td_storage: "Cloud Media Storage",
    td_sim: "SIM Ejection Countermeasure",
    td_usb: "USB Forensic Port Blocker",
    td_model: "Licensing Model",

    faq_tag: "TECHNICAL & LEGAL DOSSIER",
    faq_title: "Frequently Asked Questions",
    faq_lede: "Straightforward, technical answers to skeptical and critical security questions.",

    price_tag: "SOVEREIGN COMMERCIAL LICENSING",
    price_title: "Zero Recurring Traps. Permanent Ownership.",
    price_lede: "We do not believe in predatory monthly subscriptions. Secure your hardware once with an offline Ed25519 cryptographic license.",
    badge_most_popular: "MOST POPULAR DEFENSE",
    plan_eval_title: "Tactical Evaluation",
    plan_eval_desc: "Test core telemetry and verification on your personal hardware.",
    plan_eval_period: "7-Day Tactical Evaluation",
    btn_start_eval: "Activate via Bot",
    plan_pro_title: "Pro Lifetime Shield",
    plan_pro_desc: "Complete sovereign defense suite for 1 Android device forever.",
    plan_pro_period: "One-time payment • Lifetime OTA Updates",
    btn_buy_pro: "Claim Lifetime License",
    plan_ent_title: "Enterprise Fleet",
    plan_ent_desc: "VIP executive defense, corporate fleets, and high-risk field agents.",
    plan_ent_period: "5 Devices Pack • Dedicated Control Node",
    btn_contact_ent: "Contact Concierge",

    f_eval_1: "Essential Telegram C2 Commands",
    f_eval_2: "Headless Camera Capture Test",
    f_eval_3: "GPS & Cell Tower Telemetry",
    f_eval_4: "Community Telegram Support",
    f_pro_1: "All 86 Telegram C2 Commands",
    f_pro_2: "Knox-Grade Device Owner Provisioning",
    f_pro_3: "Hardware Escrow Token PIN Reset",
    f_pro_4: "Anti-EDL/BROM Dead Man's Switch",
    f_pro_5: "SIM Ejection Foreign Number Trap",
    f_pro_6: "1-on-1 Personal Remote Setup Onboarding",
    f_pro_7: "24-Hour 100% Refund Guarantee",
    f_ent_1: "5x Pro Lifetime Device Licenses",
    f_ent_2: "Dedicated Private Relay Server Node",
    f_ent_3: "Zero-Knowledge Fleet Management",
    f_ent_4: "Direct WhatsApp & Telegram Hotline",

    btn_copy_uid: "Copy UID",
    bkash_title: "bKash Payment (Bangladesh)",
    bkash_sub: "Contact for official bKash account",
    bkash_action_btn: "Request Number via WhatsApp",
    wa_title: "WhatsApp Concierge",
    wa_action_btn: "Chat Direct",
    floating_help: "Live Assistance",
    footer_bio: "Sovereign Android anti-theft defense & covert intelligence agent. Zero Google Play dependencies, zero cloud media storage, Knox-grade uninstallation lockout. Engineered by Izhaan Intellect."
  },
  bn: {
    top_status: "পাসা সেন্টিনেল অনলাইন // সার্বভৌম ডিফেন্স সক্রিয়",
    nav_sim: "ফরেনসিক ল্যাব",
    nav_dossier: "প্রমাণ ডসিয়ার",
    nav_setup_kit: "সেটআপ কিট",
    nav_features: "ফিচারসমূহ",
    nav_commands: "৮৬টি কমান্ড",
    nav_comparison: "তুলনা",
    nav_faq: "প্রশ্নোত্তর",
    nav_pricing: "লাইসেন্সিং",
    nav_download: "ট্যাকটিক্যাল এপিকে ডাউনলোড",
    nav_download_pill: "এপিকে নিন",

    hero_badge: "সার্বভৌম মোবাইল ডিফেন্স // অ্যান্ড্রয়েড ৮.০ – ১৬",
    hero_title: "আপনার ফোন আর কখনোই আত্মসমর্পণ করবে না।",
    hero_sub: "চুরি হওয়ার ১০ সেকেন্ডের মধ্যে যখন সাধারণ ট্র্যাকার অন্ধ হয়ে যায়—পাসা সেন্টিনেল নক্স-গ্রেড হার্ডওয়্যার লকডাউন চাপিয়ে অপরাধীর আসল পরিচয় শিকার করে এবং আপনার ব্যক্তিগত ডেটা রক্ষা করে।",
    hero_cta_apk: "ট্যাকটিক্যাল এপিকে ডাউনলোড",
    hero_cta_setup: "ডিভাইস ওনার সেটআপ কিট (পিসি)",

    pill_knox: "নক্স ইমিউটেবল",
    pill_zero: "০-ক্লাউড ট্রেইল",
    pill_sms: "টেলিগ্রাম + এসএমএস সি২",

    btn_sim_label: "🚨 লাইভ ছিনতাই প্রতিরোধ পরীক্ষা করুন",
    stamp_top: "★ হাতেনাতে আটক ★",
    stamp_sub: "// অপরাধী চেহারা শনাক্ত //",

    tg_alert_title: "🚨 জরুরি ছিনতাই প্রতিরোধ এলার্ট:",
    tg_alert_desc: "২.৬৫G টান শনাক্ত! অপরাধীর ছবি ক্যাপচার ও র‍্যাম তাৎক্ষণিক ধ্বংস সম্পন্ন।",

    dossier_tag: "ফরেনসিক অপরাধ প্রমাণ ডসিয়ার",
    dossier_title: "৪টি বাস্তব প্রতিরোধ যা পেশাদার চোরদেরও ফাঁদে ফেলে দেয়",
    dossier_lede: "প্লে স্টোরের সাধারণ অ্যাপগুলো স্যান্ডবক্সে বন্দি এবং সার্বক্ষণিক ইন্টারনেটের ওপর নির্ভরশীল। পাসা সরাসরি ডিভাইস ওনার সুপারভাইজার লেয়ারে পরিচালিত হয়।",
    d1_title: "নক্স ডিভাইস ওনার স্থায়ী সুরক্ষা",
    d1_desc: "সিস্টেম সুপারভাইজার হিসেবে নিযুক্ত। অ্যান্ড্রয়েড ওএস নিজেই আনইন্সটল ও ফোর্স স্টপ বাটন নিষ্ক্রিয় রাখে। সেফ মোড বুট বা ফ্যাক্টরি রিসেট পুরোপুরি বন্ধ।",
    d2_title: "স্ট্র্যাটেজি ১: ১০০% জিরো-স্টোরেজ",
    d2_desc: "সার্ভারে ০ বাইট ছবি বা লোকেশন জমা হয় না। প্রমাণ সরাসরি আপনার ব্যক্তিগত টেলিগ্রাম বটে যায় এবং ফোনের মেমোরি থেকে সাথে সাথে ধ্বংস হয়।",
    d3_title: "এয়ার-গ্যাপড সেলুলার এসএমএস সি২",
    d3_desc: "মোবাইল ডাটা, ওয়াইফাই ও জিপিএস বন্ধ থাকলেও সাধারণ বাটন ফোন থেকে এনক্রিপ্টেড এসএমএস পাঠিয়ে স্যাটেলাইট ম্যাপ লোকেশন ও ফুল কন্ট্রোল বজায় থাকে।",
    d4_title: "অস্ত্রের মুখে ডিকয় ওএস স্যান্ডবক্স",
    d4_desc: "ছিনতাইকারী জোর করে পাসওয়ার্ড চাইলে ডিকয় পিন দিলে ব্যাংকিং অ্যাপ ছাড়া খালি ডামি ওএস খোলে এবং গোপনে চোরের ছবি ও লাইভ এসওএস পাঠায়।",

    setup_tag: "উইন্ডোজ ১-ক্লিক প্রোভিশনিং উইজার্ড",
    setup_title: "আপনার ফোনে নিশ্চিত করুন স্থায়ী ডিভাইস ওনার প্রিভিলেজ",
    setup_lede: "কোনো জটিল কমান্ড-লাইন বা কোডিং জ্ঞান ছাড়াই উইন্ডোজ সেটআপ কিটের মাধ্যমে মাত্র ৬০ সেকেন্ডে স্বয়ংক্রিয়ভাবে নক্স ডিভাইস ওনার পারমিশন সেটআপ করুন।",
    setup_s1_title: "ট্যাকটিক্যাল এপিকে ইনস্টল",
    setup_s1_desc: "আপনার অ্যান্ড্রয়েড ফোনে (অ্যান্ড্রয়েড ৮.০–১৬) সরাসরি পাসা সেন্টিনেল এপিকে ডাউনলোড ও ইনস্টল করুন।",
    setup_s2_title: "পিসি সেটআপ কিট ডাউনলোড",
    setup_s2_desc: "কম্পিউটারে উইন্ডোজ সেটআপ কিট জিপ ফাইলটি ডাউনলোড করে আনজিপ করুন এবং PASA Device Owner Setup.bat ফাইলটি রান করুন।",
    setup_s3_title: "ইউএসবি ক্যাবল প্লাগইন ও অ্যাক্টিভেশন",
    setup_s3_desc: "ইউএসবি ডিবাগিং অন করে ক্যাবল দিয়ে ফোন যুক্ত করুন। স্বয়ংক্রিয় উইজার্ড ১-ক্লিকেই পার্মানেন্ট ডিভাইস ওনার নিযুক্ত করবে—আনইন্সটল পুরোপুরি অসম্ভব হয়ে যাবে!",
    setup_banner_title: "আপনার ফোন চিরতরে সুরক্ষিত করতে প্রস্তুত?",
    setup_banner_desc: "স্বয়ংক্রিয় গুগল প্ল্যাটফর্ম-টুলস এডিবি ডাউনলোডার ও অ্যাকাউন্ট চেকার সহ পূর্ণাঙ্গ প্রোভিশনিং উইজার্ড।",
    btn_download_kit: "উইন্ডোজ সেটআপ কিট ডাউনলোড (.ZIP)",

    feat_tag: "ট্যাকটিক্যাল ডিফেন্স ক্ষমতা",
    feat_title: "৮টি অপারেশানাল ডিফেন্স হাবের পূর্ণাঙ্গ ক্ষমতা",
    feat_lede: "পেশাদার চোরের বাস্তব কৌশল, ফরেনসিক ক্যাবল এক্সট্রাকশন এবং অস্ত্রধারী ডাকাতের হাত থেকে বাঁচার নিখুঁত প্রতিরক্ষা ব্যবস্থা।",

    cmd_tag: "পূর্ণাঙ্গ কমান্ড সম্ভার",
    cmd_title: "৮৬টি টেলিগ্রাম ও অফলাইন এসএমএস কমান্ড",
    cmd_lede: "আপনার ব্যক্তিগত টেলিগ্রাম বট অথবা বাটন ফোনের সাধারণ এসএমএস দিয়ে প্রতিটি ফিচার নিয়ন্ত্রণযোগ্য। সিনট্যাক্স কপি করতে যেকোনো কমান্ডে ক্লিক করুন।",

    comp_tag: "আর্কিটেকচার তুলনা",
    comp_title: "পাসা সেন্টিনেল বনাম প্রচলিত সমাধান",
    comp_lede: "বাস্তব চুরির ঘটনায় কেন গুগল এবং সাধারণ ট্র্যাকিং অ্যাপগুলো প্রথম ১০ সেকেন্ডেই ব্যর্থ হয়।",
    th_param: "নিরাপত্তা প্যারামিটার",
    th_google: "গুগল ফাইন্ড মাই ডিভাইস",
    th_play: "প্লে স্টোর অ্যান্টি-থেফট অ্যাপস",
    td_priv: "ওএস প্রিভিলেজ",
    td_uninst: "আনইন্সটল প্রতিরোধ ক্ষমতা",
    td_offline: "অফলাইন এসএমএস সি২ (ইন্টারনেট ছাড়া)",
    td_storage: "ক্লাউড মিডিয়া স্টোরেজ",
    td_sim: "সিম খোলা প্রতিহত করার ব্যবস্থা",
    td_usb: "ইউএসবি ফরেনসিক পোর্ট ব্লকার",
    td_model: "লাইসেন্সিং মডেল",

    faq_tag: "টেকনিক্যাল ও লিগ্যাল ডসিয়ার",
    faq_title: "সাধারণ জিজ্ঞাসা ও প্রশ্নোত্তর",
    faq_lede: "পাসা সেন্টিনেলের কার্যপদ্ধতি সম্পর্কে প্রতিটি প্রশ্নের স্পষ্ট, প্রযুক্তিগত ও আইনি উত্তর।",

    price_tag: "সার্বভৌম কমার্শিয়াল লাইসেন্সিং",
    price_title: "কোনো মাসিক সাবস্ক্রিপশন ট্র্যাপ নেই। স্থায়ী মালিকানা।",
    price_lede: "আমরা প্রতি মাসে বা বছরে ফি কাটার ঘোর বিরোধী। একবার একটি ক্রিপ্টোগ্রাফিক Ed25519 লাইসেন্স কিনুন এবং আজীবনের জন্য সুরক্ষিত থাকুন।",
    badge_most_popular: "সর্বাধিক জনপ্রিয় ডিফেন্স",
    plan_eval_title: "ট্যাকটিক্যাল ট্রায়াল",
    plan_eval_desc: "আপনার নিজস্ব হ্যান্ডসেটে টেলিমেট্রি ও কার্যকারিতা যাচাই করুন।",
    plan_eval_period: "৭ দিনের ট্যাকটিক্যাল ট্রায়াল",
    btn_start_eval: "বটের মাধ্যমে শুরু করুন",
    plan_pro_title: "প্রো লাইফটাইম শিল্ড",
    plan_pro_desc: "১টি অ্যান্ড্রয়েড ডিভাইসের জন্য স্থায়ী ও সার্বভৌম আজীবন প্রতিরক্ষা।",
    plan_pro_period: "এককালীন পেমেন্ট • আজীবন ওটিএ আপডেট",
    btn_buy_pro: "লাইফটাইম লাইসেন্স নিন",
    plan_ent_title: "এন্টারপ্রাইজ ফ্লিট",
    plan_ent_desc: "ভিআইপি এক্সিকিউটিভ ডিফেন্স ও কর্পোরেট ফ্লিট সুরক্ষার জন্য।",
    plan_ent_period: "৫টি ডিভাইসের প্যাক • ডেডিকেটেড কন্ট্রোল নোড",
    btn_contact_ent: "কনসিয়ার্জে যোগাযোগ করুন",

    f_eval_1: "মূল টেলিগ্রাম সি২ কমান্ডসমূহ",
    f_eval_2: "হেডলেস ক্যামেরা ক্যাপচার পরীক্ষা",
    f_eval_3: "জিপিএস ও সেল টাওয়ার টেলিমেট্রি",
    f_eval_4: "কমিউনিটি টেলিগ্রাম সাপোর্ট",
    f_pro_1: "সকল ৮৬টি টেলিগ্রাম সি২ কমান্ড",
    f_pro_2: "নক্স-গ্রেড ডিভাইস ওনার সুপারভাইজার",
    f_pro_3: "হার্ডওয়্যার এসক্রো টোকেন পিন রিসেট",
    f_pro_4: "অ্যান্টি-ইডিএল/বিআরওএম ডেড ম্যান সুইচ",
    f_pro_5: "সিম ইজেক্ট ও চোরের নম্বর ট্র্যাপ",
    f_pro_6: "১-অন-১ রিমোট সেটআপ অনবোর্ডিং সাপোর্ট",
    f_pro_7: "২৪ ঘণ্টার ১০০% মানিব্যাক গ্যারান্টি",
    f_ent_1: "৫টি প্রো লাইফটাইম ডিভাইস লাইসেন্স",
    f_ent_2: "ডেডিকেটেড প্রাইভেট রিলে সার্ভার নোড",
    f_ent_3: "জিরো-নলেজ ফ্লিট ম্যানেজমেন্ট",
    f_ent_4: "সরাসরি হোয়াটসঅ্যাপ ও টেলিগ্রাম হটলাইন",

    btn_copy_uid: "ইউআইডি কপি করুন",
    bkash_title: "বিকাশ পেমেন্ট (বাংলাদেশ)",
    bkash_sub: "অফিসিয়াল বিকাশ নম্বরের জন্য যোগাযোগ করুন",
    bkash_action_btn: "হোয়াটসঅ্যাপে নম্বর চান",
    wa_title: "হোয়াটসঅ্যাপ কনসিয়ার্জ",
    wa_action_btn: "সরাসরি চ্যাট",
    floating_help: "লাইভ সহায়তা",
    footer_bio: "সার্বভৌম অ্যান্ড্রয়েড অ্যান্টি-থেফট ডিফেন্স ও গোপন ইন্টেলিজেন্স এজেন্ট। গুগল প্লে স্টোর ও ক্লাউড স্টোরেজের ওপর শূন্য নির্ভরতা। নক্স-গ্রেড ডিভাইস ওনার সুরক্ষা। প্রস্তুতকারক: ইজহান ইন্টেলেকট।"
  }
};

let currentLang = 'en';
let currentFaqCategory = 'top';
let currentFeatureHub = 'knox';
let currentCommandCategory = 'all';
let isSoundEnabled = false;

/* ── Web Audio API Synthesizer (0 External Downloads) ── */
let audioCtx = null;
function getAudioContext() {
  if (!audioCtx) {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (AudioContext) audioCtx = new AudioContext();
  }
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
  return audioCtx;
}

function toggleSoundFx() {
  isSoundEnabled = !isSoundEnabled;
  const icon = document.getElementById('soundIcon');
  const label = document.getElementById('soundLabel');
  const btn = document.getElementById('btnSoundToggle');
  if (btn) btn.classList.toggle('active', isSoundEnabled);
  if (icon) icon.textContent = isSoundEnabled ? '🔊' : '🔇';
  if (label) label.textContent = isSoundEnabled ? 'SOUND: ON' : 'SOUND: OFF';
  if (isSoundEnabled) {
    getAudioContext();
    playSonarChirp();
  }
}

function playShutterSound() {
  if (!isSoundEnabled) return;
  const ctx = getAudioContext();
  if (!ctx) return;
  const t = ctx.currentTime;

  // Noise burst for mechanical shutter
  const bufferSize = ctx.sampleRate * 0.08;
  const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
  const data = buffer.getChannelData(0);
  for (let i = 0; i < bufferSize; i++) {
    data[i] = Math.random() * 2 - 1;
  }
  const noise = ctx.createBufferSource();
  noise.buffer = buffer;

  const filter = ctx.createBiquadFilter();
  filter.type = 'highpass';
  filter.frequency.setValueAtTime(1000, t);

  const gain = ctx.createGain();
  gain.gain.setValueAtTime(0.7, t);
  gain.gain.exponentialRampToValueAtTime(0.01, t + 0.08);

  noise.connect(filter);
  filter.connect(gain);
  gain.connect(ctx.destination);
  noise.start(t);
}

function playStampThud() {
  if (!isSoundEnabled) return;
  const ctx = getAudioContext();
  if (!ctx) return;
  const t = ctx.currentTime;

  const osc = ctx.createOscillator();
  const gain = ctx.createGain();
  osc.type = 'triangle';
  osc.frequency.setValueAtTime(140, t);
  osc.frequency.exponentialRampToValueAtTime(30, t + 0.25);

  gain.gain.setValueAtTime(0.9, t);
  gain.gain.exponentialRampToValueAtTime(0.01, t + 0.25);

  osc.connect(gain);
  gain.connect(ctx.destination);
  osc.start(t);
  osc.stop(t + 0.25);
}

function playSonarChirp() {
  if (!isSoundEnabled) return;
  const ctx = getAudioContext();
  if (!ctx) return;
  const t = ctx.currentTime;

  const osc = ctx.createOscillator();
  const gain = ctx.createGain();
  osc.type = 'sine';
  osc.frequency.setValueAtTime(1200, t);
  osc.frequency.exponentialRampToValueAtTime(800, t + 0.15);

  gain.gain.setValueAtTime(0.3, t);
  gain.gain.exponentialRampToValueAtTime(0.01, t + 0.15);

  osc.connect(gain);
  gain.connect(ctx.destination);
  osc.start(t);
  osc.stop(t + 0.15);
}

function playAlarmTone() {
  if (!isSoundEnabled) return;
  const ctx = getAudioContext();
  if (!ctx) return;
  const t = ctx.currentTime;

  const osc = ctx.createOscillator();
  const gain = ctx.createGain();
  osc.type = 'sawtooth';
  osc.frequency.setValueAtTime(750, t);
  osc.frequency.setValueAtTime(950, t + 0.08);

  gain.gain.setValueAtTime(0.25, t);
  gain.gain.exponentialRampToValueAtTime(0.01, t + 0.2);

  osc.connect(gain);
  gain.connect(ctx.destination);
  osc.start(t);
  osc.stop(t + 0.2);
}

/* ── 3D Forensic Night-Vision Camera Canvas Animation ── */
let camAnimFrame = null;
let biometricPoints = [];
let scanY = 0;
let isSimulating = false;

function initForensicCam() {
  const canvas = document.getElementById('forensicCamCanvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const w = canvas.width;
  const h = canvas.height;
  const cx = w / 2;
  const cy = h / 2 - 20;

  // Initialize 16 facial biometric landmarks
  biometricPoints = [
    { x: cx - 35, y: cy - 35, base: { x: cx - 35, y: cy - 35 }, name: 'TEMPLE_L' },
    { x: cx + 35, y: cy - 35, base: { x: cx + 35, y: cy - 35 }, name: 'TEMPLE_R' },
    { x: cx - 22, y: cy - 15, base: { x: cx - 22, y: cy - 15 }, name: 'EYE_L' },
    { x: cx + 22, y: cy - 15, base: { x: cx + 22, y: cy - 15 }, name: 'EYE_R' },
    { x: cx, y: cy - 6, base: { x: cx, y: cy - 6 }, name: 'NOSE_BRIDGE' },
    { x: cx - 12, y: cy + 12, base: { x: cx - 12, y: cy + 12 }, name: 'NOSE_L' },
    { x: cx + 12, y: cy + 12, base: { x: cx + 12, y: cy + 12 }, name: 'NOSE_R' },
    { x: cx, y: cy + 15, base: { x: cx, y: cy + 15 }, name: 'NOSE_TIP' },
    { x: cx - 18, y: cy + 34, base: { x: cx - 18, y: cy + 34 }, name: 'LIP_L' },
    { x: cx + 18, y: cy + 34, base: { x: cx + 18, y: cy + 34 }, name: 'LIP_R' },
    { x: cx, y: cy + 42, base: { x: cx, y: cy + 42 }, name: 'LIP_BOT' },
    { x: cx - 38, y: cy + 20, base: { x: cx - 38, y: cy + 20 }, name: 'JAW_L' },
    { x: cx + 38, y: cy + 20, base: { x: cx + 38, y: cy + 20 }, name: 'JAW_R' },
    { x: cx - 25, y: cy + 55, base: { x: cx - 25, y: cy + 55 }, name: 'CHIN_L' },
    { x: cx + 25, y: cy + 55, base: { x: cx + 25, y: cy + 55 }, name: 'CHIN_R' },
    { x: cx, y: cy + 65, base: { x: cx, y: cy + 65 }, name: 'CHIN_TIP' }
  ];

  function renderFrame() {
    ctx.clearRect(0, 0, w, h);

    // Dark cybernetic grid background
    ctx.strokeStyle = 'rgba(0, 240, 255, 0.05)';
    ctx.lineWidth = 1;
    const gridSize = 24;
    for (let x = 0; x < w; x += gridSize) {
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, h); ctx.stroke();
    }
    for (let y = 0; y < h; y += gridSize) {
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke();
    }

    const time = Date.now() / 600;
    const breatheX = Math.sin(time) * 1.5;
    const breatheY = Math.cos(time * 0.8) * 2;

    // Draw Suspect Hooded Head Silhouette
    ctx.save();
    ctx.translate(breatheX, breatheY);

    ctx.fillStyle = 'rgba(15, 23, 42, 0.85)';
    ctx.strokeStyle = isSimulating ? 'rgba(239, 68, 68, 0.5)' : 'rgba(0, 240, 255, 0.3)';
    ctx.lineWidth = 1.5;

    // Hood contour
    ctx.beginPath();
    ctx.ellipse(cx, cy + 10, 68, 88, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    // Suspect Shoulders
    ctx.beginPath();
    ctx.moveTo(cx - 120, h);
    ctx.bezierCurveTo(cx - 90, cy + 100, cx - 60, cy + 90, cx - 40, cy + 85);
    ctx.lineTo(cx + 40, cy + 85);
    ctx.bezierCurveTo(cx + 60, cy + 90, cx + 90, cy + 100, cx + 120, h);
    ctx.fillStyle = 'rgba(2, 6, 23, 0.9)';
    ctx.fill();
    ctx.stroke();

    // Biometric Mesh Connecting Lines
    ctx.strokeStyle = isSimulating ? 'rgba(239, 68, 68, 0.25)' : 'rgba(0, 240, 255, 0.18)';
    ctx.lineWidth = 1;
    for (let i = 0; i < biometricPoints.length - 1; i++) {
      for (let j = i + 1; j < biometricPoints.length; j++) {
        const dx = biometricPoints[i].base.x - biometricPoints[j].base.x;
        const dy = biometricPoints[i].base.y - biometricPoints[j].base.y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 42) {
          ctx.beginPath();
          ctx.moveTo(biometricPoints[i].x, biometricPoints[i].y);
          ctx.lineTo(biometricPoints[j].x, biometricPoints[j].y);
          ctx.stroke();
        }
      }
    }

    // Biometric Tracking Landmark Dots
    biometricPoints.forEach(pt => {
      pt.x = pt.base.x + Math.sin(time + pt.base.y) * 1.2;
      pt.y = pt.base.y + Math.cos(time + pt.base.x) * 1.2;

      ctx.fillStyle = isSimulating ? '#ef4444' : '#00f0ff';
      ctx.beginPath();
      ctx.arc(pt.x, pt.y, 2.5, 0, Math.PI * 2);
      ctx.fill();
    });

    // Face Target Reticle Box
    const boxW = 120;
    const boxH = 150;
    const boxX = cx - boxW / 2;
    const boxY = cy - 40;
    const reticleColor = isSimulating ? '#ef4444' : '#00f0ff';

    ctx.strokeStyle = reticleColor;
    ctx.lineWidth = 2;
    const corner = 16;

    // Top-left
    ctx.beginPath(); ctx.moveTo(boxX, boxY + corner); ctx.lineTo(boxX, boxY); ctx.lineTo(boxX + corner, boxY); ctx.stroke();
    // Top-right
    ctx.beginPath(); ctx.moveTo(boxX + boxW - corner, boxY); ctx.lineTo(boxX + boxW, boxY); ctx.lineTo(boxX + boxW, boxY + corner); ctx.stroke();
    // Bottom-left
    ctx.beginPath(); ctx.moveTo(boxX, boxY + boxH - corner); ctx.lineTo(boxX, boxY + boxH); ctx.lineTo(boxX + corner, boxY + boxH); ctx.stroke();
    // Bottom-right
    ctx.beginPath(); ctx.moveTo(boxX + boxW - corner, boxY + boxH); ctx.lineTo(boxX + boxW, boxY + boxH); ctx.lineTo(boxX + boxW, boxY + boxH - corner); ctx.stroke();

    // Reticle Center Crosshair
    ctx.strokeStyle = reticleColor;
    ctx.beginPath();
    ctx.moveTo(cx - 8, cy); ctx.lineTo(cx + 8, cy);
    ctx.moveTo(cx, cy - 8); ctx.lineTo(cx, cy + 8);
    ctx.stroke();

    // Reticle Label
    ctx.fillStyle = reticleColor;
    ctx.font = '8.5px "JetBrains Mono", monospace';
    ctx.fillText(isSimulating ? 'TARGET LOCKED: 99.8%' : 'FACE RECOGNITION ACTIVE', boxX + 4, boxY - 6);
    ctx.fillText('ISO 1600 • 1/120s • 4K', boxX + 4, boxY + boxH + 12);

    ctx.restore();

    // Vertical Laser Scanning Line
    scanY = (scanY + 2.5) % h;
    const grad = ctx.createLinearGradient(0, scanY - 15, 0, scanY + 15);
    grad.addColorStop(0, 'rgba(0, 240, 255, 0)');
    grad.addColorStop(0.5, isSimulating ? 'rgba(239, 68, 68, 0.45)' : 'rgba(0, 240, 255, 0.35)');
    grad.addColorStop(1, 'rgba(0, 240, 255, 0)');
    ctx.fillStyle = grad;
    ctx.fillRect(0, scanY - 15, w, 30);

    camAnimFrame = requestAnimationFrame(renderFrame);
  }
  renderFrame();
}

/* ── Interactive 3D Parallax Tilt ── */
function init3DParallax() {
  const stage = document.getElementById('phoneStage');
  const mockup = document.getElementById('phoneMockup');
  if (!stage || !mockup) return;

  stage.addEventListener('mousemove', e => {
    const rect = stage.getBoundingClientRect();
    const x = e.clientX - rect.left - rect.width / 2;
    const y = e.clientY - rect.top - rect.height / 2;
    const rotX = (y / (rect.height / 2)) * -14;
    const rotY = (x / (rect.width / 2)) * 14;
    mockup.style.transform = \`perspective(1000px) rotateX(\${rotX}deg) rotateY(\${rotY}deg)\`;
  });

  stage.addEventListener('mouseleave', () => {
    mockup.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg)';
  });
}

/* ── Cinematic Snatch Simulation Sequence ── */
function runSnatchSimulation() {
  if (isSimulating) return;
  isSimulating = true;

  const phone = document.getElementById('phoneMockup');
  const flash = document.getElementById('camFlash');
  const stamp = document.getElementById('stampCinematic');
  const tgCard = document.getElementById('telegramAlertCard');
  const trapStat = document.getElementById('hudStatTrap');
  const usbStat = document.getElementById('hudStatUsb');

  // Step 1: Tremor & Kinetic Vector Alert (T+0.0s)
  if (phone) phone.classList.add('shaking');
  if (trapStat) trapStat.textContent = 'TRIP: 2.65G KINETIC';
  playAlarmTone();

  // Step 2: Covert Camera Flash & Shutter Snap (T+0.6s)
  setTimeout(() => {
    if (flash) flash.classList.add('flashing');
    playShutterSound();
    setTimeout(() => {
      if (flash) flash.classList.remove('flashing');
    }, 120);
  }, 600);

  // Step 3: "CAUGHT RED-HANDED" Slam (T+1.1s)
  setTimeout(() => {
    if (phone) phone.classList.remove('shaking');
    if (stamp) stamp.classList.add('stamped');
    playStampThud();
  }, 1100);

  // Step 4: Knox Kiosk & USB Data Pin Sever (T+1.8s)
  setTimeout(() => {
    if (usbStat) usbStat.textContent = 'SEVERED [0V]';
    playSonarChirp();
  }, 1800);

  // Step 5: Floating Telegram Card Pop-in (T+2.4s)
  setTimeout(() => {
    if (tgCard) tgCard.classList.add('show');
    playSonarChirp();
  }, 2400);

  // Reset simulation state after 6 seconds
  setTimeout(() => {
    if (stamp) stamp.classList.remove('stamped');
    if (trapStat) trapStat.textContent = 'ARMED // 2.65G';
    if (usbStat) usbStat.textContent = 'KILL [0V]';
    isSimulating = false;
  }, 6500);
}

function dismissTgAlert() {
  const tgCard = document.getElementById('telegramAlertCard');
  if (tgCard) tgCard.classList.remove('show');
}

/* ── Language Switcher ── */
function setLanguage(lang) {
  if (lang !== 'en' && lang !== 'bn') lang = 'en';
  currentLang = lang;
  document.documentElement.setAttribute('data-lang', lang);
  document.documentElement.lang = lang;

  const btnEn = document.getElementById('btnEn');
  const btnBn = document.getElementById('btnBn');
  if (btnEn) btnEn.classList.toggle('active', lang === 'en');
  if (btnBn) btnBn.classList.toggle('active', lang === 'bn');

  const dict = translations[lang] || translations.en;
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (key && dict[key] !== undefined) {
      el.textContent = dict[key];
    }
  });

  const searchFaq = document.getElementById('faqSearch');
  if (searchFaq) {
    searchFaq.placeholder = lang === 'bn' 
      ? 'প্রশ্ন খুঁজুন (যেমন: নক্স, ব্যাটারি, পুলিশ, রুট, সিম)...' 
      : 'Search questions (e.g., Knox, battery, police, root, SIM)...';
  }

  const searchCmd = document.getElementById('commandSearch');
  if (searchCmd) {
    searchCmd.placeholder = lang === 'bn'
      ? '৮৬টি কমান্ড খুঁজুন (যেমন: snap, usb_lock, duress, sim_lock, locate, deadman)...'
      : 'Search 86 C2 commands (e.g. snap, usb_lock, duress, sim_lock, locate, deadman)...';
  }

  renderFeatureHubTabs();
  renderFeatureCards();
  renderCommandPills();
  renderCommands();
  renderFaqPills();
  renderFaq();

  try { localStorage.setItem('pasa_lang', lang); } catch (e) {}
}

function toggleMobileDrawer() {
  const drawer = document.getElementById('mobileDrawer');
  const overlay = document.getElementById('drawerOverlay');
  if (drawer && overlay) {
    drawer.classList.toggle('open');
    overlay.classList.toggle('open');
  }
}

function toggleConcierge() {
  const card = document.getElementById('conciergeCard');
  if (card) card.classList.toggle('open');
}

function showToast(msg) {
  const toast = document.getElementById('cmdToast');
  const toastMsg = document.getElementById('cmdToastMsg');
  if (!toast || !toastMsg) return;
  toastMsg.textContent = msg;
  toast.classList.add('show');
  setTimeout(() => { toast.classList.remove('show'); }, 2500);
}

function copyBinanceUid() {
  navigator.clipboard.writeText('756303714').then(() => {
    showToast(currentLang === 'bn' ? 'Binance UID: 756303714 কপি করা হয়েছে!' : 'Binance UID: 756303714 copied to clipboard!');
  });
}

function copyCommand(text) {
  navigator.clipboard.writeText(text).then(() => {
    showToast(currentLang === 'bn' ? text + ' কপি করা হয়েছে!' : text + ' copied to clipboard!');
  });
}

/* ── 8-Hub Capabilities Tab Switcher ── */
function renderFeatureHubTabs() {
  const tabsContainer = document.getElementById('hubTabs');
  if (!tabsContainer || typeof rawHubs === 'undefined') return;
  const isBn = currentLang === 'bn';

  tabsContainer.innerHTML = rawHubs.map(hub => {
    const active = currentFeatureHub === hub.id ? 'active' : '';
    const label = isBn ? hub.nameBn : hub.nameEn;
    return \`<button class="hub-tab \${active}" onclick="setFeatureHub('\${hub.id}')">\${hub.icon} <span>\${label}</span></button>\`;
  }).join('');
}

function setFeatureHub(hubId) {
  currentFeatureHub = hubId;
  renderFeatureHubTabs();
  renderFeatureCards();
}

function renderFeatureCards() {
  const container = document.getElementById('featureCardsContainer');
  if (!container || typeof rawHubs === 'undefined') return;
  const isBn = currentLang === 'bn';

  const hub = rawHubs.find(h => h.id === currentFeatureHub) || rawHubs[0];
  if (!hub || !Array.isArray(hub.cards)) return;

  container.innerHTML = hub.cards.map(c => {
    const title = (isBn ? c.titleBn : c.titleEn) || '';
    const desc = (isBn ? c.descBn : c.descEn) || '';
    const threat = (isBn ? c.threatBn : c.threatEn) || '';
    const threatLabel = isBn ? 'প্রতিরোধ:' : 'Threat Neutralized:';

    let badgesHtml = '';
    if (Array.isArray(c.commands) && c.commands.length > 0) {
      badgesHtml = '<div class="cmd-badges">' + c.commands.map(cmd => {
        const cleanCmd = cmd.split(' ')[0];
        return \`<span class="cmd-badge" onclick="copyCommand('\${cleanCmd}')" title="Click to copy">\${cmd}</span>\`;
      }).join('') + '</div>';
    }

    return \`
      <div class="feature-card-rich">
        <div>
          <div class="feature-header">
            <span class="feature-tag">\${c.tag || 'SECURITY'}</span>
            <div style="color:var(--cyan);display:flex;align-items:center;">\${hub.icon || ''}</div>
          </div>
          <h4 class="feature-title">\${title}</h4>
          <p class="feature-desc">\${desc}</p>
          \${threat ? \`
            <div class="threat-box">
              <div class="threat-title">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                \${threatLabel}
              </div>
              \${threat}
            </div>
          \` : ''}
        </div>
        <div>
          \${badgesHtml}
        </div>
      </div>
    \`;
  }).join('');
}

/* ── 86-Command Tactical Terminal ── */
function renderCommandPills() {
  const pillsContainer = document.getElementById('commandPills');
  if (!pillsContainer || typeof rawCommands === 'undefined') return;
  const isBn = currentLang === 'bn';

  const cats = [
    { id: 'all', en: 'All Commands (86)', bn: 'সকল কমান্ড (৮৬)' },
    { id: 'knox', en: 'Device Owner (18)', bn: 'ডিভাইস ওনার (১৮)' },
    { id: 'forensics', en: 'Covert Forensics (13)', bn: 'গোপন নজরদারি (১৩)' },
    { id: 'location', en: 'Location & RF (8)', bn: 'লোকেশন ও আরএফ (৮)' },
    { id: 'lockdown', en: 'Lockdown (12)', bn: 'লকডাউন (১২)' },
    { id: 'traps', en: 'Traps (10)', bn: 'ট্র্যাপ ও ডিফেন্স (১০)' },
    { id: 'core', en: 'Core (8)', bn: 'কোর সিস্টেম (৮)' }
  ];

  pillsContainer.innerHTML = cats.map(cat => {
    const active = currentCommandCategory === cat.id ? 'active' : '';
    const label = isBn ? cat.bn : cat.en;
    return \`<button class="cmd-pill \${active}" onclick="setCommandCategory('\${cat.id}')">\${label}</button>\`;
  }).join('');
}

function setCommandCategory(catId) {
  currentCommandCategory = catId;
  const searchInput = document.getElementById('commandSearch');
  if (searchInput && searchInput.value) searchInput.value = '';
  renderCommandPills();
  renderCommands();
}

function filterCommands() {
  renderCommands();
}

function renderCommands() {
  const container = document.getElementById('commandsContainer');
  if (!container || typeof rawCommands === 'undefined') return;
  const isBn = currentLang === 'bn';
  const query = (document.getElementById('commandSearch')?.value || '').toLowerCase().trim();

  let filtered = rawCommands;
  if (query.length > 0) {
    filtered = rawCommands.filter(c => {
      const cmdStr = (c.cmd || '').toLowerCase();
      const descEn = (c.descEn || '').toLowerCase();
      const descBn = (c.descBn || '').toLowerCase();
      return cmdStr.includes(query) || descEn.includes(query) || descBn.includes(query);
    });
  } else if (currentCommandCategory !== 'all') {
    filtered = rawCommands.filter(c => c.cat === currentCommandCategory);
  }

  if (filtered.length === 0) {
    container.innerHTML = \`<div style="grid-column:1/-1;text-align:center;padding:24px;color:var(--text-faint);font-size:13px;">
      \${isBn ? 'কোনো কমান্ড খুঁজে পাওয়া যায়নি।' : 'No matching commands found.'}
    </div>\`;
    return;
  }

  container.innerHTML = filtered.map(c => {
    const desc = isBn ? c.descBn : c.descEn;
    const tag = c.sms ? '<span style="font-size:9px;color:var(--emerald);font-weight:700;margin-left:4px;">[SMS]</span>' : '';
    return \`
      <div class="cmd-card-compact">
        <div class="cmd-token-row">
          <div class="cmd-token-text">
            \${c.cmd} \${c.params ? ('<span style="font-size:11px;color:var(--text-faint);font-weight:500;">' + c.params + '</span>') : ''}
            \${tag}
          </div>
          <span class="cmd-copy-icon" onclick="copyCommand('\${c.cmd}')" title="Click to copy">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
          </span>
        </div>
        <div class="cmd-desc-text">\${desc}</div>
      </div>
    \`;
  }).join('');
}

/* ── Smart Category-Tabbed FAQ ── */
function renderFaqPills() {
  const pillsContainer = document.getElementById('faqPills');
  if (!pillsContainer) return;
  const isBn = currentLang === 'bn';

  const categories = [
    { id: 'top', en: '🔥 Top 6 Critical Questions', bn: '🔥 শীর্ষ ৬টি গুরুত্বপূর্ণ প্রশ্ন' },
    { id: 'theft', en: '🕵️ Theft & Traps', bn: '🕵️ চুরি ও ট্র্যাপ ডিফেন্স' },
    { id: 'hardware', en: '🛡️ Hardware & Knox', bn: '🛡️ হার্ডওয়্যার ও নক্স' },
    { id: 'privacy', en: '🔒 Zero-Storage & C2', bn: '🔒 প্রাইভেসি ও কন্ট্রোল' },
    { id: 'legal', en: '📜 Legal & Pricing', bn: '📜 আইনি ও লাইসেন্স' },
    { id: 'all', en: '🌐 All 38 Questions', bn: '🌐 সবগুলো ৩৮টি প্রশ্ন' }
  ];

  pillsContainer.innerHTML = categories.map(cat => {
    const activeClass = currentFaqCategory === cat.id ? 'active' : '';
    const label = isBn ? cat.bn : cat.en;
    return \`<button class="faq-pill \${activeClass}" onclick="setFaqCategory('\${cat.id}')">\${label}</button>\`;
  }).join('');
}

function setFaqCategory(cat) {
  currentFaqCategory = cat;
  renderFaqPills();
  renderFaq();
}

function filterFaq() {
  renderFaq();
}

function renderFaq() {
  const container = document.getElementById('faqAccordion');
  if (!container || typeof rawFaq === 'undefined') return;

  const query = (document.getElementById('faqSearch')?.value || '').toLowerCase().trim();
  const isBn = currentLang === 'bn';

  let filtered = [];
  if (query.length > 0) {
    filtered = rawFaq.filter(item => {
      const qText = ((isBn ? item.qBn : item.qEn) || '').toLowerCase();
      const aText = ((isBn ? item.aBn : item.aEn) || '').toLowerCase();
      return qText.includes(query) || aText.includes(query);
    });
  } else if (currentFaqCategory === 'top') {
    const topIndices = [0, 7, 10, 15, 31, 4];
    filtered = topIndices.map(idx => rawFaq[idx]).filter(Boolean);
  } else if (currentFaqCategory === 'theft') {
    filtered = rawFaq.filter(f => ['traps', 'sim', 'lockscreen'].includes(f.category));
  } else if (currentFaqCategory === 'hardware') {
    filtered = rawFaq.filter(f => ['device-owner', 'hardware', 'battery'].includes(f.category));
  } else if (currentFaqCategory === 'privacy') {
    filtered = rawFaq.filter(f => ['privacy', 'forensics', 'vault', 'sms'].includes(f.category));
  } else if (currentFaqCategory === 'legal') {
    filtered = rawFaq.filter(f => ['general', 'legal'].includes(f.category));
  } else {
    filtered = rawFaq;
  }

  if (filtered.length === 0) {
    container.innerHTML = \`<div style="text-align:center;padding:24px;color:var(--text-faint);font-size:13.5px;">
      \${isBn ? 'কোনো ফলাফল পাওয়া যায়নি।' : 'No matching questions found.'}
    </div>\`;
    return;
  }

  container.innerHTML = filtered.map((item, idx) => {
    const q = (isBn ? item.qBn : item.qEn) || '';
    const a = (isBn ? item.aBn : item.aEn) || '';
    return \`
      <div class="faq-item" id="faqItem_\${idx}">
        <div class="faq-q" onclick="toggleFaq(\${idx})">
          <span>\${q}</span>
          <span class="faq-icon" id="faqIcon_\${idx}">+</span>
        </div>
        <div class="faq-a" id="faqAnswer_\${idx}">
          \${a}
        </div>
      </div>
    \`;
  }).join('');
}

function toggleFaq(idx) {
  const item = document.getElementById('faqItem_' + idx);
  const ans = document.getElementById('faqAnswer_' + idx);
  if (!item || !ans) return;
  const isOpen = item.classList.contains('open');
  item.classList.toggle('open', !isOpen);
  ans.style.display = isOpen ? 'none' : 'block';
}

document.addEventListener('DOMContentLoaded', () => {
  let saved = 'en';
  try {
    const val = localStorage.getItem('pasa_lang');
    if (val === 'bn' || val === 'en') saved = val;
  } catch (e) {}

  setLanguage(saved);
  initForensicCam();
  init3DParallax();
});
</script>

</body>
</html>`;
}

const html = generateHtml();
fs.writeFileSync(targetHtmlPath, html, 'utf8');
console.log(`Successfully wrote ${html.length} bytes to ${targetHtmlPath}`);
