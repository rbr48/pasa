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
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&family=JetBrains+Mono:wght@500;600;700&family=Tiro+Bangla:ital@0;1&display=swap" rel="stylesheet">
<style>
  :root {
    /* ── Light Theme Palette ── */
    --bg-canvas: #f8fafc;        /* slate-50 */
    --bg-surface: #ffffff;       /* Pure white */
    --bg-muted: #f1f5f9;         /* slate-100 */
    --bg-subtle: #e2e8f0;        /* slate-200 */

    --border: #e2e8f0;           /* slate-200 */
    --border-hover: #cbd5e1;     /* slate-300 */
    --border-primary: #dc2626;   /* red-600 */
    --border-cyan: #0284c7;      /* sky-600 */

    --crimson: #dc2626;          /* red-600 */
    --crimson-hover: #b91c1c;    /* red-700 */
    --crimson-subtle: #fef2f2;   /* red-50 */
    --cyan: #0284c7;             /* sky-600 */
    --cyan-subtle: #f0f9ff;      /* sky-50 */
    --amber: #d97706;            /* amber-600 */
    --amber-subtle: #fffbeb;     /* amber-50 */
    --emerald: #059669;          /* emerald-600 */
    --emerald-subtle: #ecfdf5;   /* emerald-50 */

    /* High-Contrast WCAG AAA Typography */
    --text: #0f172a;             /* slate-900: Deep charcoal */
    --text-dim: #334155;         /* slate-700: Body copy */
    --text-faint: #64748b;       /* slate-500: Auxiliary */
    --text-muted: #94a3b8;       /* slate-400 */

    --shadow-sm: 0 1px 3px rgba(0,0,0,0.05), 0 1px 2px rgba(0,0,0,0.03);
    --shadow-md: 0 4px 14px rgba(0,0,0,0.05), 0 1px 3px rgba(0,0,0,0.03);
    --shadow-lg: 0 10px 25px -4px rgba(0,0,0,0.08), 0 4px 10px -2px rgba(0,0,0,0.04);

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
    scroll-padding-top: 90px;
  }
  
  body {
    font-family: var(--font);
    background: var(--bg-canvas);
    color: var(--text);
    line-height: 1.6;
    -webkit-font-smoothing: antialiased;
    overflow-x: hidden;
  }

  /* Anchor Clearance */
  section[id], div[id] {
    scroll-margin-top: 90px;
  }

  .wrap { max-width: 1180px; margin: 0 auto; padding: 0 24px; position: relative; z-index: 10; }
  .wrap-narrow { max-width: 860px; margin: 0 auto; padding: 0 24px; position: relative; z-index: 10; }

  /* ── Top Status Strip ── */
  .top-strip {
    background: #ffffff;
    border-bottom: 1px solid var(--border);
    padding: 8px 0; font-family: var(--mono); font-size: 11.5px;
    color: var(--text-faint); position: relative; z-index: 101;
  }
  .top-strip-inner {
    display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px;
  }
  .pulse-dot {
    width: 8px; height: 8px; border-radius: 50%; background: var(--emerald);
    box-shadow: 0 0 8px var(--emerald); display: inline-block;
  }
  .top-status-badge {
    display: flex; align-items: center; gap: 8px; font-weight: 600; color: var(--text);
  }
  .top-specs { display: flex; gap: 16px; font-weight: 600; }
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
    height: 70px;
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
    color: var(--crimson); background: var(--crimson-subtle);
    padding: 2px 6px; border-radius: 4px; border: 1px solid #fecaca;
    letter-spacing: 0.05em; display: inline-block;
  }
  .nav-menu {
    display: flex; align-items: center; gap: 20px;
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
    display: flex; background: var(--bg-muted); padding: 3px; border-radius: 8px;
    border: 1px solid var(--border);
  }
  .lang-btn {
    border: none; background: transparent; padding: 4px 10px; font-size: 12px;
    font-weight: 700; color: var(--text-faint); border-radius: 6px; cursor: pointer;
    transition: all 0.15s; font-family: var(--font);
  }
  .lang-btn.active {
    background: #ffffff; color: var(--text); box-shadow: var(--shadow-sm);
  }
  .lang-btn:hover:not(.active) { color: var(--text); }

  .nav-cta {
    background: var(--crimson); color: #fff; padding: 8px 16px; border-radius: 8px;
    font-size: 13px; font-weight: 700; text-decoration: none; display: flex; align-items: center;
    gap: 6px; transition: background 0.15s;
  }
  .nav-cta:hover { background: var(--crimson-hover); }

  /* Mobile Drawer */
  .mobile-toggle {
    display: none; background: none; border: none; cursor: pointer; color: var(--text);
  }
  .mobile-drawer {
    position: fixed; top: 0; right: -280px; width: 280px; height: 100vh;
    background: #ffffff; border-left: 1px solid var(--border); z-index: 1001;
    padding: 24px; display: flex; flex-direction: column; gap: 16px;
    transition: right 0.3s cubic-bezier(0.16, 1, 0.3, 1); box-shadow: var(--shadow-lg);
  }
  .mobile-drawer.open { right: 0; }
  .mobile-drawer-hdr {
    display: flex; justify-content: space-between; align-items: center; padding-bottom: 12px;
    border-bottom: 1px solid var(--border);
  }
  .mobile-drawer a {
    text-decoration: none; color: var(--text); font-weight: 600; font-size: 15px; padding: 6px 0;
  }
  .drawer-overlay {
    position: fixed; inset: 0; background: rgba(15, 23, 42, 0.4); z-index: 1000;
    opacity: 0; pointer-events: none; transition: opacity 0.3s;
  }
  .drawer-overlay.open { opacity: 1; pointer-events: auto; }

  @media (max-width: 900px) {
    .nav-menu { display: none; }
    .mobile-toggle { display: block; }
    .top-specs { display: none; }
  }

  /* ── Hero Section ── */
  .hero {
    padding: 70px 0 50px; text-align: center; position: relative;
    background: radial-gradient(circle at 50% 20%, rgba(2, 132, 199, 0.05), transparent 70%);
  }
  .badge-tag {
    display: inline-flex; align-items: center; gap: 8px; font-family: var(--mono);
    font-size: 11px; font-weight: 700; color: var(--cyan); background: var(--cyan-subtle);
    border: 1px solid #bae6fd; padding: 4px 12px; border-radius: 20px; margin-bottom: 22px;
  }
  .hero-title {
    font-size: clamp(28px, 4.5vw, 50px); font-weight: 900; line-height: 1.18;
    letter-spacing: -0.02em; margin-bottom: 18px; color: var(--text);
  }
  .hero-sub {
    font-size: clamp(15px, 1.8vw, 17.5px); color: var(--text-dim); max-width: 760px;
    margin: 0 auto 32px; line-height: 1.6;
  }
  .hero-cta-group {
    display: flex; flex-wrap: wrap; justify-content: center; align-items: center; gap: 14px;
    margin-bottom: 40px;
  }
  .btn-primary {
    background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
    color: #fff; padding: 13px 26px; border-radius: 10px; font-weight: 700; font-size: 14.5px;
    text-decoration: none; display: inline-flex; align-items: center; gap: 8px;
    box-shadow: 0 4px 14px rgba(220, 38, 38, 0.3); border: 1px solid rgba(255,255,255,0.15);
    transition: all 0.2s ease;
  }
  .btn-primary:hover {
    transform: translateY(-2px); box-shadow: 0 6px 20px rgba(220, 38, 38, 0.45);
  }
  .btn-ghost {
    background: #ffffff; color: var(--text-dim); padding: 13px 22px;
    border-radius: 10px; font-weight: 600; font-size: 14px; text-decoration: none;
    display: inline-flex; align-items: center; gap: 6px; border: 1px solid var(--border);
    box-shadow: var(--shadow-sm); transition: all 0.2s ease;
  }
  .btn-ghost:hover {
    color: var(--cyan); border-color: var(--cyan); background: var(--cyan-subtle);
  }

  /* Specs Bar */
  .specs-bar {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px;
    background: #ffffff; border: 1px solid var(--border);
    border-radius: 12px; padding: 18px 22px; max-width: 980px; margin: 0 auto;
    text-align: left; box-shadow: var(--shadow-sm);
  }
  .spec-item {
    border-left: 3px solid var(--cyan); padding-left: 12px;
  }
  .spec-item.crimson { border-left-color: var(--crimson); }
  .spec-item.emerald { border-left-color: var(--emerald); }
  .spec-label { font-family: var(--mono); font-size: 10.5px; color: var(--text-faint); text-transform: uppercase; font-weight: 600; }
  .spec-val { font-size: 13px; font-weight: 700; color: var(--text); margin-top: 3px; }

  /* ── Section Structure ── */
  .section { padding: 75px 0; border-bottom: 1px solid var(--border); position: relative; }
  .section-hdr { text-align: center; margin-bottom: 44px; }
  .section-tag {
    font-family: var(--mono); font-size: 11px; font-weight: 700; color: var(--cyan);
    letter-spacing: 0.1em; text-transform: uppercase; margin-bottom: 10px; display: inline-block;
  }
  .section-title {
    font-size: clamp(23px, 3.2vw, 34px); font-weight: 800; letter-spacing: -0.01em; margin-bottom: 12px; color: var(--text);
  }
  .section-lede {
    font-size: 15px; color: var(--text-dim); max-width: 720px; margin: 0 auto; line-height: 1.6;
  }

  /* ── 6 Core Customer Value Guarantees ── */
  .benefits-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px;
  }
  .benefit-card {
    background: #ffffff; border: 1px solid var(--border); border-radius: 12px;
    padding: 24px 22px; box-shadow: var(--shadow-sm); transition: all 0.2s ease;
    display: flex; flex-direction: column; justify-content: space-between;
  }
  .benefit-card:hover {
    border-color: #cbd5e1; transform: translateY(-2px); box-shadow: var(--shadow-md);
  }
  .benefit-card-hdr {
    display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;
  }
  .benefit-icon {
    width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center;
    background: var(--cyan-subtle); border: 1px solid #bae6fd; color: var(--cyan);
  }
  .benefit-icon.crimson { background: var(--crimson-subtle); border-color: #fecaca; color: var(--crimson); }
  .benefit-icon.emerald { background: var(--emerald-subtle); border-color: #a7f3d0; color: var(--emerald); }
  .benefit-icon.amber { background: var(--amber-subtle); border-color: #fde68a; color: var(--amber); }
  .benefit-num {
    font-family: var(--mono); font-size: 11px; font-weight: 700; color: var(--text-faint);
  }
  .benefit-title { font-size: 16px; font-weight: 700; color: var(--text); margin-bottom: 8px; }
  .benefit-desc { font-size: 13.5px; color: var(--text-dim); line-height: 1.55; }

  /* ── 3 Architectural Pillars ── */
  .pillars-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px;
  }
  .pillar-card {
    background: #ffffff; border: 1px solid var(--border);
    border-radius: 14px; padding: 30px 24px; transition: all 0.25s ease;
    box-shadow: var(--shadow-sm);
  }
  .pillar-card:hover {
    border-color: var(--border-hover); transform: translateY(-3px); box-shadow: var(--shadow-md);
  }
  .pillar-icon {
    width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center;
    background: var(--cyan-subtle); border: 1px solid #bae6fd; margin-bottom: 18px; color: var(--cyan);
  }
  .pillar-icon.crimson { background: var(--crimson-subtle); border-color: #fecaca; color: var(--crimson); }
  .pillar-icon.emerald { background: var(--emerald-subtle); border-color: #a7f3d0; color: var(--emerald); }
  .pillar-title { font-size: 18px; font-weight: 700; margin-bottom: 10px; color: var(--text); }
  .pillar-desc { font-size: 14px; color: var(--text-dim); line-height: 1.6; }

  /* ── Interactive 8-Hub Defense Explorer ── */
  .hub-controls {
    max-width: 1080px; margin: 0 auto 32px; display: flex; flex-direction: column; gap: 14px;
  }
  .hub-tabs {
    display: flex; flex-wrap: wrap; gap: 8px; justify-content: center;
  }
  .hub-tab {
    background: #ffffff; border: 1px solid var(--border); color: var(--text-dim);
    padding: 8px 16px; border-radius: 20px; font-size: 12.5px; font-weight: 600;
    cursor: pointer; transition: all 0.2s; display: inline-flex; align-items: center; gap: 8px;
    box-shadow: var(--shadow-sm);
  }
  .hub-tab:hover:not(.active) { color: var(--text); background: var(--bg-muted); }
  .hub-tab.active {
    background: var(--cyan); color: #ffffff; border-color: var(--cyan);
    box-shadow: 0 2px 10px rgba(2, 132, 199, 0.28);
  }
  .hub-badge {
    background: rgba(0,0,0,0.06); padding: 2px 6px; border-radius: 10px; font-size: 11px; font-family: var(--mono);
  }
  .hub-tab.active .hub-badge {
    background: rgba(255,255,255,0.25); color: #ffffff;
  }
  .features-grid-rich {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px;
  }
  .feature-card-rich {
    background: #ffffff; border: 1px solid var(--border); border-radius: 12px;
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
  .feature-title { font-size: 15.5px; font-weight: 700; margin-bottom: 6px; color: var(--text); }
  .feature-desc { font-size: 13px; color: var(--text-dim); line-height: 1.5; }
  .threat-box {
    background: #fef2f2; border: 1px solid #fee2e2; border-left: 3px solid var(--crimson);
    border-radius: 6px; padding: 8px 10px; margin-top: 10px; font-size: 12px; color: #991b1b;
    line-height: 1.45;
  }
  .threat-title {
    font-weight: 700; font-family: var(--mono); font-size: 10px; text-transform: uppercase;
    margin-bottom: 2px; display: flex; align-items: center; gap: 5px; color: var(--crimson);
  }
  .cmd-badges {
    display: flex; flex-wrap: wrap; gap: 5px; margin-top: 10px; align-items: center;
  }
  .cmd-badge {
    background: var(--bg-muted); border: 1px solid var(--border); color: var(--text);
    padding: 2px 7px; border-radius: 5px; font-family: var(--mono); font-size: 11px; font-weight: 600;
    cursor: pointer; transition: all 0.15s;
  }
  .cmd-badge:hover {
    background: var(--cyan-subtle); border-color: var(--cyan); color: var(--cyan);
  }

  /* ── 86-Command Interactive Matrix ── */
  .cmd-controls {
    max-width: 900px; margin: 0 auto 24px; display: flex; flex-direction: column; gap: 12px;
  }
  .cmd-search {
    width: 100%; background: #ffffff; border: 1px solid var(--border-hover);
    border-radius: 10px; padding: 12px 18px; color: var(--text); font-size: 14px;
    outline: none; font-family: var(--font); transition: all 0.2s; box-shadow: var(--shadow-sm);
  }
  .cmd-search:focus { border-color: var(--cyan); box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.12); }
  .cmd-pills {
    display: flex; flex-wrap: wrap; gap: 7px; justify-content: center;
  }
  .cmd-pill {
    background: #ffffff; border: 1px solid var(--border); color: var(--text-dim);
    padding: 6px 13px; border-radius: 20px; font-size: 12px; font-weight: 600;
    cursor: pointer; transition: all 0.2s; box-shadow: var(--shadow-sm);
  }
  .cmd-pill:hover:not(.active) { color: var(--text); background: var(--bg-muted); }
  .cmd-pill.active {
    background: var(--crimson); color: #ffffff; border-color: var(--crimson);
    box-shadow: 0 2px 8px rgba(220, 38, 38, 0.25);
  }
  .cmd-status-wrap {
    display: flex; justify-content: center; margin-bottom: 12px;
  }
  .cmd-search-status {
    font-family: var(--mono); font-size: 12px; color: var(--cyan); font-weight: 600;
    background: var(--cyan-subtle); padding: 5px 14px; border-radius: 20px;
    border: 1px solid #bae6fd;
  }
  .cmd-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 12px;
  }
  .cmd-card {
    background: #ffffff; border: 1px solid var(--border); border-radius: 9px;
    padding: 13px 15px; display: flex; flex-direction: column; justify-content: space-between;
    transition: all 0.15s ease; box-shadow: var(--shadow-sm);
  }
  .cmd-card:hover { border-color: var(--cyan); box-shadow: var(--shadow-md); }
  .cmd-hdr {
    display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; gap: 6px;
  }
  .cmd-token {
    font-family: var(--mono); font-size: 13px; font-weight: 700; color: var(--cyan);
    display: flex; align-items: center; gap: 6px;
  }
  .cmd-params {
    font-family: var(--mono); font-size: 11px; font-weight: 500; color: var(--text-faint);
  }
  .cmd-channel-tags {
    display: flex; gap: 4px;
  }
  .tag-tg {
    background: #e0f2fe; color: #0369a1; font-family: var(--mono); font-size: 9px;
    font-weight: 700; padding: 2px 5px; border-radius: 4px; border: 1px solid #bae6fd;
  }
  .tag-sms {
    background: #ecfdf5; color: #047857; font-family: var(--mono); font-size: 9px;
    font-weight: 700; padding: 2px 5px; border-radius: 4px; border: 1px solid #a7f3d0;
  }
  .cmd-body {
    font-size: 12.5px; color: var(--text-dim); line-height: 1.45; margin-bottom: 10px;
  }
  .cmd-footer {
    display: flex; align-items: center; justify-content: space-between; font-size: 10.5px;
    font-family: var(--mono); color: var(--text-faint); border-top: 1px dashed var(--border);
    padding-top: 8px;
  }
  .btn-copy-cmd {
    background: var(--bg-muted); border: 1px solid var(--border); border-radius: 5px;
    padding: 2px 7px; font-family: var(--mono); font-size: 10.5px; font-weight: 600;
    color: var(--text-dim); cursor: pointer; display: inline-flex; align-items: center; gap: 4px;
    transition: all 0.15s;
  }
  .btn-copy-cmd:hover { background: var(--cyan-subtle); color: var(--cyan); border-color: var(--cyan); }

  /* ── Compact Expand / Collapse Button Container ── */
  .expand-btn-container {
    grid-column: 1 / -1; display: flex; justify-content: center; margin-top: 22px;
  }
  .btn-expand-more {
    background: #ffffff; border: 1.5px solid var(--border-hover); border-radius: 30px;
    padding: 10px 24px; font-size: 13px; font-weight: 700; color: var(--text);
    cursor: pointer; display: inline-flex; align-items: center; gap: 8px;
    box-shadow: var(--shadow-sm); transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    font-family: var(--font);
  }
  .btn-expand-more:hover {
    background: var(--bg-muted); border-color: var(--cyan); color: var(--cyan);
    box-shadow: var(--shadow-md); transform: translateY(-1px);
  }

  /* ── Comparison Table ── */
  .table-container {
    background: #ffffff; border: 1px solid var(--border);
    border-radius: 12px; overflow-x: auto; -webkit-overflow-scrolling: touch;
    box-shadow: var(--shadow-sm);
  }
  .comp-table {
    width: 100%; border-collapse: collapse; text-align: left; font-size: 13.5px;
  }
  .comp-table th {
    background: #f8fafc; padding: 16px 20px; font-weight: 700; border-bottom: 1px solid var(--border);
    color: var(--text); font-family: var(--mono); font-size: 12px;
  }
  .comp-table td {
    padding: 15px 20px; border-bottom: 1px solid var(--border); color: var(--text-dim);
  }
  .comp-table tr:last-child td { border-bottom: none; }
  .comp-table tr:hover td { background: #f8fafc; }
  .comp-highlight {
    color: var(--cyan); font-weight: 700; background: var(--cyan-subtle);
  }
  .check-icon { color: var(--emerald); font-weight: 800; font-size: 15px; }
  .cross-icon { color: var(--crimson); font-weight: 800; font-size: 15px; }

  /* ── Master Technical FAQ ── */
  .faq-controls {
    max-width: 860px; margin: 0 auto 30px; display: flex; flex-direction: column; gap: 14px;
  }
  .faq-search {
    width: 100%; background: #ffffff; border: 1px solid var(--border-hover);
    border-radius: 10px; padding: 13px 18px; color: var(--text); font-size: 14.5px;
    outline: none; font-family: var(--font); transition: all 0.2s; box-shadow: var(--shadow-sm);
  }
  .faq-search:focus { border-color: var(--cyan); box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.12); }
  .faq-pills {
    display: flex; flex-wrap: wrap; gap: 8px; justify-content: center;
  }
  .faq-pill {
    background: #ffffff; border: 1px solid var(--border);
    color: var(--text-dim); padding: 7px 14px; border-radius: 20px; font-size: 12.5px;
    cursor: pointer; transition: all 0.2s; font-weight: 600; box-shadow: var(--shadow-sm);
  }
  .faq-pill.active {
    background: var(--cyan); color: #ffffff; border-color: var(--cyan);
    box-shadow: 0 2px 8px rgba(2, 132, 199, 0.25);
  }
  .faq-pill:hover:not(.active) { color: var(--text); background: var(--bg-muted); }

  .faq-list {
    max-width: 860px; margin: 0 auto; display: flex; flex-direction: column; gap: 12px;
  }
  .faq-item {
    background: #ffffff; border: 1px solid var(--border);
    border-radius: 12px; overflow: hidden; transition: all 0.2s ease;
    box-shadow: var(--shadow-sm);
  }
  .faq-item:hover { border-color: #cbd5e1; }
  .faq-item.open {
    border-color: var(--cyan);
    box-shadow: 0 4px 14px rgba(2, 132, 199, 0.08);
  }
  .faq-q {
    padding: 18px 22px; display: flex; align-items: center; justify-content: space-between;
    cursor: pointer; gap: 14px; user-select: none; font-weight: 700; font-size: 15px;
    color: var(--text);
  }
  .faq-q:hover { color: var(--cyan); }
  .faq-icon {
    font-family: var(--mono); font-size: 18px; font-weight: 700; color: var(--cyan);
    transition: transform 0.2s; min-width: 20px; text-align: right;
  }
  .faq-item.open .faq-icon { transform: rotate(45deg); color: var(--crimson); }
  .faq-a {
    padding: 0 22px 20px; color: var(--text-dim); font-size: 14px; line-height: 1.65;
    display: none; border-top: 1px solid var(--bg-muted); padding-top: 14px;
  }
  .faq-a p { margin-bottom: 10px; }
  .faq-a ul, .faq-a ol { padding-left: 20px; margin-bottom: 10px; }
  .faq-a li { margin-bottom: 4px; }
  .faq-a code {
    background: var(--bg-muted); padding: 2px 6px; border-radius: 4px;
    font-family: var(--mono); font-size: 12.5px; color: var(--cyan);
  }

  /* ── Pricing & Licensing ── */
  .pricing-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px;
    margin-bottom: 48px;
  }
  .pricing-card {
    background: #ffffff; border: 1px solid var(--border); border-radius: 14px;
    padding: 32px 26px; display: flex; flex-direction: column; justify-content: space-between;
    box-shadow: var(--shadow-sm); position: relative; transition: all 0.25s ease;
  }
  .pricing-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); }
  .pricing-card.featured {
    border-color: var(--crimson); box-shadow: 0 8px 24px rgba(220, 38, 38, 0.1);
  }
  .pricing-badge {
    position: absolute; top: -12px; left: 50%; transform: translateX(-50%);
    background: var(--crimson); color: #fff; font-family: var(--mono); font-size: 10.5px;
    font-weight: 700; padding: 4px 12px; border-radius: 12px; letter-spacing: 0.05em;
  }
  .plan-name { font-size: 20px; font-weight: 800; color: var(--text); margin-bottom: 6px; }
  .plan-desc { font-size: 13.5px; color: var(--text-faint); margin-bottom: 20px; }
  .plan-price { font-size: 32px; font-weight: 900; color: var(--text); margin-bottom: 4px; }
  .plan-currency { font-size: 14px; font-weight: 600; color: var(--text-faint); }
  .plan-period { font-size: 12px; color: var(--text-faint); font-family: var(--mono); margin-bottom: 24px; }
  .plan-features { list-style: none; margin-bottom: 28px; display: flex; flex-direction: column; gap: 10px; }
  .plan-features li {
    font-size: 13.5px; color: var(--text-dim); display: flex; align-items: center; gap: 10px;
  }
  .plan-features svg { width: 16px; height: 16px; color: var(--emerald); flex-shrink: 0; }

  /* 3-Step License Activation Roadmap */
  .delivery-steps {
    background: #ffffff; border: 1px solid var(--border); border-radius: 14px;
    padding: 32px; box-shadow: var(--shadow-sm); margin-bottom: 40px;
  }
  .delivery-steps-title {
    font-family: var(--mono); font-size: 12px; font-weight: 700; color: var(--cyan);
    letter-spacing: 0.1em; text-transform: uppercase; margin-bottom: 20px; text-align: center;
  }
  .steps-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 20px;
  }
  .step-box {
    background: var(--bg-canvas); border: 1px solid var(--border); border-radius: 10px;
    padding: 18px; text-align: center;
  }
  .step-num {
    width: 28px; height: 28px; border-radius: 50%; background: var(--cyan); color: #fff;
    display: inline-flex; align-items: center; justify-content: center; font-weight: 800;
    font-size: 13px; margin-bottom: 10px;
  }
  .step-box h4 { font-size: 15px; font-weight: 700; margin-bottom: 6px; color: var(--text); }
  .step-box p { font-size: 12.5px; color: var(--text-dim); line-height: 1.5; }

  /* Payment Channels Strip */
  .payment-channels {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px;
  }
  .pay-method {
    background: #ffffff; border: 1px solid var(--border); border-radius: 12px;
    padding: 18px 20px; display: flex; align-items: center; gap: 14px; box-shadow: var(--shadow-sm);
  }
  .pay-icon {
    width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center;
    justify-content: center; flex-shrink: 0;
  }
  .pay-title { font-size: 14px; font-weight: 700; color: var(--text); margin-bottom: 2px; }
  .pay-sub { font-size: 11.5px; color: var(--text-faint); font-family: var(--mono); margin-bottom: 6px; }
  .pay-action-btn {
    font-size: 12px; font-weight: 700; text-decoration: none; padding: 4px 10px;
    border-radius: 6px; display: inline-block;
  }

  /* ── Floating Concierge ── */
  .floating-concierge {
    position: fixed; bottom: 24px; right: 24px; z-index: 999;
    display: flex; flex-direction: column; align-items: flex-end; gap: 10px;
  }
  .concierge-pill {
    background: #ffffff; border: 1px solid var(--border); border-radius: 24px;
    padding: 10px 18px; display: flex; align-items: center; gap: 8px; font-weight: 700;
    font-size: 13px; color: var(--text); cursor: pointer; box-shadow: var(--shadow-lg);
    transition: all 0.2s;
  }
  .concierge-pill:hover { border-color: var(--cyan); color: var(--cyan); }
  .concierge-card {
    display: none; background: #ffffff; border: 1px solid var(--border); border-radius: 12px;
    padding: 14px; box-shadow: var(--shadow-lg); flex-direction: column; gap: 8px; width: 220px;
  }
  .concierge-card.open { display: flex; }
  .concierge-link {
    display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: 8px;
    text-decoration: none; font-size: 13px; font-weight: 600; color: #fff;
  }
  .concierge-link.wa { background: #25D366; }
  .concierge-link.tg { background: #229ed9; }

  /* ── Toast Notification ── */
  .toast {
    position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%) translateY(100px);
    background: #0f172a; color: #ffffff; padding: 10px 20px; border-radius: 8px;
    font-size: 13px; font-weight: 600; box-shadow: 0 10px 25px rgba(0,0,0,0.25);
    display: flex; align-items: center; gap: 8px; z-index: 10000; opacity: 0;
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); pointer-events: none;
  }
  .toast.show { transform: translateX(-50%) translateY(0); opacity: 1; }

  /* ── Footer ── */
  footer {
    background: #ffffff; border-top: 1px solid var(--border); padding: 50px 0 30px;
  }
  .footer-inner {
    display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 20px;
    margin-bottom: 24px;
  }
  .footer-links { display: flex; gap: 18px; font-size: 13px; flex-wrap: wrap; }
  .footer-links a { color: var(--text-faint); text-decoration: none; font-weight: 500; transition: color 0.15s; }
  .footer-links a:hover { color: var(--cyan); }
  .footer-copy { font-size: 12px; color: var(--text-faint); font-family: var(--mono); }
</style>
</head>
<body>

<!-- Top Status Strip -->
<div class="top-strip">
  <div class="wrap top-strip-inner">
    <div class="top-status-badge">
      <span class="pulse-dot"></span>
      <span data-i18n="top_status">PASA SENTINEL ONLINE // v3.5.1 (BUILD 47)</span>
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
        <span class="brand-tag">SOVEREIGN DEFENSE</span>
      </div>
    </a>

    <!-- Desktop Navigation -->
    <div class="nav-menu">
      <a href="#benefits" class="nav-link" data-i18n="nav_benefits">Advantages</a>
      <a href="#pillars" class="nav-link" data-i18n="nav_pillars">Architecture</a>
      <a href="#features" class="nav-link" data-i18n="nav_features">Capabilities</a>
      <a href="#commands" class="nav-link" data-i18n="nav_commands">86 C2 Commands</a>
      <a href="#comparison" class="nav-link" data-i18n="nav_comparison">Comparison</a>
      <a href="#faq" class="nav-link" data-i18n="nav_faq">Master FAQ</a>
      <a href="#pricing" class="nav-link" data-i18n="nav_pricing">Licensing</a>
    </div>

    <!-- Actions -->
    <div class="nav-actions">
      <div class="lang-switcher">
        <button class="lang-btn active" id="btnEn" onclick="setLanguage('en')">EN</button>
        <button class="lang-btn" id="btnBn" onclick="setLanguage('bn')">বাংলা</button>
      </div>
      <a href="/releases/pasa-latest.apk" class="nav-cta">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 3v12m0 0-4-4m4 4 4-4M4 21h16"/></svg>
        <span data-i18n="nav_download">Download APK</span>
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
  <a href="#benefits" onclick="toggleMobileDrawer()" data-i18n="nav_benefits">Advantages</a>
  <a href="#pillars" onclick="toggleMobileDrawer()" data-i18n="nav_pillars">Architecture</a>
  <a href="#features" onclick="toggleMobileDrawer()" data-i18n="nav_features">Capabilities</a>
  <a href="#commands" onclick="toggleMobileDrawer()" data-i18n="nav_commands">86 C2 Commands</a>
  <a href="#comparison" onclick="toggleMobileDrawer()" data-i18n="nav_comparison">Comparison</a>
  <a href="#faq" onclick="toggleMobileDrawer()" data-i18n="nav_faq">Master FAQ</a>
  <a href="#pricing" onclick="toggleMobileDrawer()" data-i18n="nav_pricing">Licensing</a>
  <div style="margin-top:auto;padding-top:20px;">
    <a href="/releases/pasa-latest.apk" class="btn-primary" style="width:100%;justify-content:center;" data-i18n="nav_download">
      Download APK (v3.5.1)
    </a>
  </div>
</div>

<!-- ── HERO SECTION ── -->
<section class="hero">
  <div class="wrap">
    <div class="badge-tag">
      <span class="pulse-dot"></span>
      <span data-i18n="hero_badge">SOVEREIGN MOBILE DEFENSE // ANDROID 8.0 – 16</span>
    </div>
    <h1 class="hero-title" data-i18n="hero_title">YOUR PHONE WILL NEVER SURRENDER.</h1>
    <p class="hero-sub" data-i18n="hero_sub">
      When standard trackers go blind in 10 seconds, PASA Sentinel enforces Knox-grade hardware lockdown, captures perpetrator forensics headlessly, and protects your sovereign personal data.
    </p>

    <div class="hero-cta-group">
      <a href="/releases/pasa-latest.apk" class="btn-primary">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 3v12m0 0-4-4m4 4 4-4M4 21h16"/></svg>
        <span data-i18n="hero_cta_apk">Download Tactical APK (v3.5.1)</span>
      </a>
      <a href="#features" class="btn-ghost">
        <span data-i18n="hero_cta_explore">Explore Capabilities</span> ↓
      </a>
    </div>

    <!-- Quick Telemetry Specs Bar -->
    <div class="specs-bar">
      <div class="spec-item crimson">
        <div class="spec-label" data-i18n="spec_uninstall">Uninstallation Defense</div>
        <div class="spec-val" data-i18n="spec_val_knox">Knox Supervisor (Permanent)</div>
      </div>
      <div class="spec-item">
        <div class="spec-label" data-i18n="spec_storage">Surveillance Storage</div>
        <div class="spec-val" data-i18n="spec_val_zerostore">0-Cloud Trails (Telegram Direct)</div>
      </div>
      <div class="spec-item emerald">
        <div class="spec-label" data-i18n="spec_c2">C2 Control Channels</div>
        <div class="spec-val">Telegram + Air-Gapped SMS</div>
      </div>
      <div class="spec-item">
        <div class="spec-label" data-i18n="spec_crypto">Cryptographic Trust</div>
        <div class="spec-val">StrongBox TEE + Ed25519</div>
      </div>
    </div>
  </div>
</section>

<!-- ── 6 CORE CUSTOMER VALUE GUARANTEES ── -->
<section id="benefits" class="section" style="background:#ffffff;">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="benefits_tag">WHY PASA SENTINEL // CORE ADVANTAGES</span>
      <h2 class="section-title" data-i18n="benefits_title">6 Operational Guarantees No Other App Can Match</h2>
      <p class="section-lede" data-i18n="benefits_lede">
        Engineered specifically to solve the exact ways modern phone thieves and hostile actors exploit standard tracking apps.
      </p>
    </div>

    <div class="benefits-grid">
      <!-- 1 -->
      <div class="benefit-card">
        <div class="benefit-card-hdr">
          <div class="benefit-icon crimson">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
          </div>
          <span class="benefit-num">#01</span>
        </div>
        <h4 class="benefit-title" data-i18n="b1_title">Permanent Anti-Uninstall</h4>
        <p class="benefit-desc" data-i18n="b1_desc">
          Android Device Owner OS lockout grays out Uninstall and Force Stop. Safe Mode and recovery resets are neutralized.
        </p>
      </div>

      <!-- 2 -->
      <div class="benefit-card">
        <div class="benefit-card-hdr">
          <div class="benefit-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
          </div>
          <span class="benefit-num">#02</span>
        </div>
        <h4 class="benefit-title" data-i18n="b2_title">100% Zero-Storage Privacy</h4>
        <p class="benefit-desc" data-i18n="b2_desc">
          Zero photos, zero GPS tracks, zero recordings saved on servers. Everything streams direct to your Telegram and shreds from RAM.
        </p>
      </div>

      <!-- 3 -->
      <div class="benefit-card">
        <div class="benefit-card-hdr">
          <div class="benefit-icon emerald">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z"/></svg>
          </div>
          <span class="benefit-num">#03</span>
        </div>
        <h4 class="benefit-title" data-i18n="b3_title">Air-Gapped Cellular SMS C2</h4>
        <p class="benefit-desc" data-i18n="b3_desc">
          Maintains full command authority even when mobile data, Wi-Fi, and location are shut off, using cryptographic TOTP tokens.
        </p>
      </div>

      <!-- 4 -->
      <div class="benefit-card">
        <div class="benefit-card-hdr">
          <div class="benefit-icon amber">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>
          </div>
          <span class="benefit-num">#04</span>
        </div>
        <h4 class="benefit-title" data-i18n="b4_title">Hardware-Level Killswitches</h4>
        <p class="benefit-desc" data-i18n="b4_desc">
          Sever physical USB data pins to block Cellebrite forensic extraction boxes. Mute cameras and microphones at HAL level.
        </p>
      </div>

      <!-- 5 -->
      <div class="benefit-card">
        <div class="benefit-card-hdr">
          <div class="benefit-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18.36 6.64a9 9 0 11-12.73 0M12 2v10"/></svg>
          </div>
          <span class="benefit-num">#05</span>
        </div>
        <h4 class="benefit-title" data-i18n="b5_title">Duress Decoy Sandbox OS</h4>
        <p class="benefit-desc" data-i18n="b5_desc">
          Under armed coercion, entering your secret Decoy PIN unlocks a sterile decoy system while secretly broadcasting an emergency SOS.
        </p>
      </div>

      <!-- 6 -->
      <div class="benefit-card">
        <div class="benefit-card-hdr">
          <div class="benefit-icon emerald">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
          </div>
          <span class="benefit-num">#06</span>
        </div>
        <h4 class="benefit-title" data-i18n="b6_title">Lifetime Sovereign License</h4>
        <p class="benefit-desc" data-i18n="b6_desc">
          Zero monthly or yearly subscription traps. One single cryptographic key protects your device for a lifetime.
        </p>
      </div>
    </div>
  </div>
</section>

<!-- ── 3 ARCHITECTURAL PILLARS ── -->
<section id="pillars" class="section">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="pillars_tag">FOUNDATIONAL SECURITY PILLARS</span>
      <h2 class="section-title" data-i18n="pillars_title">Engineered Without Google Play Compromises</h2>
      <p class="section-lede" data-i18n="pillars_lede">
        Standard Play Store apps are restricted by third-party sandbox policies. PASA Sentinel operates at the Device Owner supervisor layer with zero external dependencies.
      </p>
    </div>

    <div class="pillars-grid">
      <!-- Pillar 1 -->
      <div class="pillar-card">
        <div class="pillar-icon crimson">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
        </div>
        <h3 class="pillar-title" data-i18n="p1_title">Knox Device Owner Permanence</h3>
        <p class="pillar-desc" data-i18n="p1_desc">
          Configured as system supervisor. Android itself grays out the "Uninstall" and "Force Stop" buttons. Blocks Safe Mode booting, factory resets, developer options, and notification shade pulldown.
        </p>
      </div>

      <!-- Pillar 2 -->
      <div class="pillar-card">
        <div class="pillar-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
        </div>
        <h3 class="pillar-title" data-i18n="p2_title">Strategy 1: 100% Zero-Storage</h3>
        <p class="pillar-desc" data-i18n="p2_desc">
          Zero photos, zero GPS tracks, and zero audio recordings are stored on our servers or cloud databases. Evidence streams directly to your private Telegram bot and is shredded from device RAM immediately.
        </p>
      </div>

      <!-- Pillar 3 -->
      <div class="pillar-card">
        <div class="pillar-icon emerald">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z"/></svg>
        </div>
        <h3 class="pillar-title" data-i18n="p3_title">Air-Gapped Cellular SMS C2</h3>
        <p class="pillar-desc" data-i18n="p3_desc">
          Maintains full remote control even when mobile data, Wi-Fi, and location are shut off. Authenticated via RFC 6238 TOTP tokens or master PIN, returning live GPS and status pins via cellular SMS.
        </p>
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

<!-- ── INTERACTIVE 86-COMMAND C2 MATRIX ── -->
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

    <div class="cmd-status-wrap">
      <div id="commandSearchStatus" class="cmd-search-status" style="display:none;"></div>
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

<!-- ── MASTER TECHNICAL & LEGAL FAQ ── -->
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
      <!-- Trial -->
      <div class="pricing-card">
        <div class="pricing-card-body">
          <h3 class="plan-name" data-i18n="plan_eval_title">Tactical Evaluation</h3>
          <p class="plan-desc" data-i18n="plan_eval_desc">Test core telemetry and verification on your personal hardware.</p>
          <div class="plan-price">FREE</div>
          <div class="plan-period" data-i18n="plan_eval_period">3-Day Evaluation Period</div>
          <ul class="plan-features">
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_eval_1">Essential Telegram C2 Commands</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_eval_2">Headless Camera Capture Test</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_eval_3">GPS &amp; Cell Tower Telemetry</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_eval_4">Community Telegram Support</span></li>
          </ul>
        </div>
        <a href="https://t.me/Pas_agent_bot" target="_blank" class="btn-ghost" data-i18n="btn_start_eval">
          Activate via Bot
        </a>
      </div>

      <!-- Pro Lifetime (Featured) -->
      <div class="pricing-card featured">
        <div class="pricing-badge" data-i18n="badge_most_popular">MOST POPULAR DEFENSE</div>
        <div class="pricing-card-body">
          <h3 class="plan-name" style="color:var(--crimson);" data-i18n="plan_pro_title">Pro Lifetime Shield</h3>
          <p class="plan-desc" data-i18n="plan_pro_desc">Complete sovereign defense suite for 1 Android device forever.</p>
          <div class="plan-price">$25 <span class="plan-currency">/ ৳3,000</span></div>
          <div class="plan-period" data-i18n="plan_pro_period">One-time payment • Lifetime OTA Updates</div>
          <ul class="plan-features">
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <strong><span data-i18n="f_pro_1">All 86 Telegram C2 Commands</span></strong></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <strong><span data-i18n="f_pro_2">Knox-Grade Device Owner Provisioning</span></strong></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_pro_3">Hardware Escrow Token PIN Reset</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_pro_4">Anti-EDL/BROM Dead Man's Switch</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_pro_5">SIM Ejection Foreign Number Trap</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_pro_6">1-on-1 Personal Remote Setup Onboarding</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <strong><span data-i18n="f_pro_7">24-Hour 100% Refund Guarantee</span></strong></li>
          </ul>
        </div>
        <a href="https://wa.me/8801762033445?text=Hello%20PASA%20Team%2C%20I%20want%20to%20activate%20PASA%20Pro%20Lifetime" target="_blank" class="btn-primary" data-i18n="btn_buy_pro">
          Claim Lifetime License
        </a>
      </div>

      <!-- Enterprise Fleet -->
      <div class="pricing-card">
        <div class="pricing-card-body">
          <h3 class="plan-name" data-i18n="plan_ent_title">Enterprise Fleet</h3>
          <p class="plan-desc" data-i18n="plan_ent_desc">VIP executive defense, corporate fleets, and high-risk field agents.</p>
          <div class="plan-price">$99 <span class="plan-currency">/ ৳11,500</span></div>
          <div class="plan-period" data-i18n="plan_ent_period">5 Devices Pack • Dedicated Control Node</div>
          <ul class="plan-features">
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_ent_1">5x Pro Lifetime Device Licenses</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_ent_2">Dedicated Private Relay Server Node</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_ent_3">Zero-Knowledge Fleet Management</span></li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> <span data-i18n="f_ent_4">Direct WhatsApp &amp; Telegram Hotline</span></li>
          </ul>
        </div>
        <a href="https://wa.me/8801762033445?text=Hello%20PASA%20Team%2C%20I%20am%20interested%20in%20Enterprise%20Fleet" target="_blank" class="btn-ghost" data-i18n="btn_contact_ent">
          Contact Concierge
        </a>
      </div>
    </div>

    <!-- 3-Step License Activation Roadmap -->
    <div class="delivery-steps">
      <div class="delivery-steps-title" data-i18n="steps_hdr">HOW LICENSE ACTIVATION WORKS</div>
      <div class="steps-grid">
        <div class="step-box">
          <div class="step-num">1</div>
          <h4 data-i18n="step1_title">Complete Payment</h4>
          <p data-i18n="step1_desc">Pay via Binance Pay (Crypto) or request an official bKash account via WhatsApp concierge.</p>
        </div>
        <div class="step-box">
          <div class="step-num">2</div>
          <h4 data-i18n="step2_title">Share TXID / Receipt</h4>
          <p data-i18n="step2_desc">Send your transaction screenshot or TXID directly to our WhatsApp concierge or Telegram bot.</p>
        </div>
        <div class="step-box">
          <div class="step-num">3</div>
          <h4 data-i18n="step3_title">Instant Activation</h4>
          <p data-i18n="step3_desc">Receive your unique Ed25519 digital license key in 5–10 minutes with full setup onboarding support.</p>
        </div>
      </div>
    </div>

    <!-- Payment Channels Details Strip -->
    <div class="payment-channels">
      <!-- Binance Pay -->
      <div class="pay-method">
        <div class="pay-icon" style="background:#FEF6D8;">
          <svg viewBox="0 0 126.61 126.61" width="30" height="30" fill="#F3BA2F">
            <path d="M63.31 0L35.22 28.09l13.62 13.62 14.47-14.47 14.46 14.47 13.63-13.62L63.31 0zM14.47 48.84L0 63.31l14.47 14.47 13.63-13.63L14.47 48.84zm97.67 0l-13.63 13.62 13.63 13.63 14.47-14.47-14.47-14.46zm-48.83 4.14l-14.47 14.47 14.47 14.47 14.47-14.47-14.47-14.47zm0 41.87l-14.47-14.47-13.62 13.62 28.09 28.09 28.09-28.09-13.63-13.62-14.46 14.47z"/>
          </svg>
        </div>
        <div style="flex:1;">
          <div class="pay-title">Binance Pay (Crypto)</div>
          <div class="pay-sub" id="binanceUidText">UID: 756303714 (RBR48)</div>
          <button onclick="copyBinanceUid()" class="pay-action-btn" style="background:#FEF6D8;color:#B45309;border:1px solid #FCD34D;cursor:pointer;" data-i18n="btn_copy_uid">
            Copy UID
          </button>
        </div>
      </div>

      <!-- bKash -->
      <div class="pay-method">
        <div class="pay-icon" style="background:#FDF2F8;">
          <svg viewBox="240 10 235 215" width="32" height="32">
            <path d="M327.99 110.75l12.99 58.4 85.01-43.04z" fill="#d12053"/>
            <path d="M352.16 23.48L328 110.76l98.01 15.35z" fill="#e2136e"/>
            <path d="M248.31 10.7l101.38 12.11-23.97 86.76z" fill="#d12053"/>
            <path d="M247.52 27.76h11.29l31.67 40.5z" fill="#9e1638"/>
            <path d="M428.69 125.55l-29.46-40.77 47.66-8.53z" fill="#d12053"/>
            <path d="M423.77 137.5l3.04-9.07-74.39 37.74z" fill="#e2136e"/>
            <path d="M325.91 113.05l15.52 69.77-46.06 37.46z" fill="#9e1638"/>
            <path d="M442.25 96.97l27.05-.46-19.55-19.89z" fill="#e2136e"/>
          </svg>
        </div>
        <div style="flex:1;">
          <div class="pay-title" data-i18n="bkash_title">bKash Payment (Bangladesh)</div>
          <div class="pay-sub" style="color:var(--amber);" data-i18n="bkash_sub">Contact for official bKash account</div>
          <a href="https://wa.me/8801762033445?text=Hello%20PASA%2C%20please%20send%20me%20the%20official%20bKash%20payment%20number%20for%20license%20activation" target="_blank" class="pay-action-btn" style="background:#FDF2F8;color:#BE185D;border:1px solid #FBCFE8;" data-i18n="bkash_action_btn">
            Request Number via WhatsApp
          </a>
        </div>
      </div>

      <!-- WhatsApp Concierge -->
      <div class="pay-method">
        <div class="pay-icon" style="background:#ECFDF5;">
          <svg viewBox="0 0 32 32" width="30" height="30">
            <circle cx="16" cy="16" r="16" fill="#25D366"/>
            <path d="M16 5.8a10.2 10.2 0 0 0-8.8 15.4L6 26l5-1.3A10.2 10.2 0 1 0 16 5.8zm5.9 14.5c-.3.7-1.5 1.4-2.1 1.4-.6 0-1.3.2-3.8-.8a12.8 12.8 0 0 1-5.1-4.5c-.2-.3-.6-1-.6-1.9 0-.9.5-1.4.7-1.6.2-.2.4-.3.6-.3h.5c.2 0 .4 0 .5.4l.7 1.8c.1.2.1.4 0 .6l-.3.4c-.1.2-.3.3-.4.5-.1.1-.3.3-.1.6.3.5 1 1.7 2.4 2.5 1.5.8 2.3.9 2.6.8.3-.1.9-.7 1.1-1.1.2-.4.4-.3.7-.2.3.1 1.9.9 2.2 1.1.3.1.5.3.5.5s0 .9-.3 1.2z" fill="#fff"/>
          </svg>
        </div>
        <div style="flex:1;">
          <div class="pay-title" data-i18n="wa_title">WhatsApp Concierge</div>
          <div class="pay-sub">+880 1762-033445</div>
          <a href="https://wa.me/8801762033445" target="_blank" class="pay-action-btn" style="background:#ECFDF5;color:#047857;border:1px solid #A7F3D0;" data-i18n="wa_action_btn">
            Chat Direct
          </a>
        </div>
      </div>

      <!-- Telegram Bot -->
      <div class="pay-method">
        <div class="pay-icon" style="background:#F0F9FF;">
          <svg viewBox="0 0 32 32" width="30" height="30">
            <circle cx="16" cy="16" r="16" fill="#229ED9"/>
            <path d="M7.6 15.4l14.2-5.8c.7-.3 1.2.1 1 1l-2.4 11.4c-.2.8-.7 1-1.4.6l-3.7-2.7-1.8 1.7c-.2.2-.4.4-.8.4l.3-3.7 6.7-6c.3-.3-.1-.4-.4-.2l-8.3 5.2-3.6-1.1c-.8-.2-.8-.8.1-1.1z" fill="#fff"/>
          </svg>
        </div>
        <div style="flex:1;">
          <div class="pay-title">Official Telegram Bot</div>
          <div class="pay-sub">@Pas_agent_bot</div>
          <a href="https://t.me/Pas_agent_bot" target="_blank" class="pay-action-btn" style="background:#F0F9FF;color:#0369A1;border:1px solid #BAE6FD;">
            Open Bot
          </a>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- ── FOOTER ── -->
<footer>
  <div class="wrap">
    <div class="footer-inner">
      <div style="display:flex;align-items:center;gap:12px;">
        <img src="/assets/img/logo.png" alt="PASA Sentinel Logo" width="36" height="36" style="border-radius:8px;object-fit:cover;">
        <div>
          <div style="font-weight:800;font-size:15px;color:var(--text);margin-bottom:2px;">PASA SENTINEL</div>
          <p style="font-size:12.5px;color:var(--text-faint);max-width:500px;" data-i18n="footer_bio">
            Sovereign Android anti-theft defense &amp; covert intelligence agent. Zero Google Play dependencies, zero cloud media storage, Knox-grade uninstallation lockout. Engineered by Izhaan Intellect.
          </p>
        </div>
      </div>
      <div class="footer-links">
        <a href="#benefits" data-i18n="nav_benefits">Advantages</a>
        <a href="#pillars" data-i18n="nav_pillars">Architecture</a>
        <a href="#features" data-i18n="nav_features">Capabilities</a>
        <a href="#commands" data-i18n="nav_commands">86 C2 Commands</a>
        <a href="#faq" data-i18n="nav_faq">Master FAQ</a>
        <a href="/terms" target="_blank">Terms &amp; EULA</a>
        <a href="/privacy" target="_blank">Zero-Storage Policy</a>
        <a href="https://github.com/rbr48/pasa" target="_blank">GitHub</a>
      </div>
    </div>
    <div class="footer-copy">
      &copy; 2026 Izhaan Intellect &amp; PASA Sentinel. All rights reserved. Strategy 1 Zero-Storage Guarantee.
    </div>
  </div>
</footer>

<!-- ── SMART FLOATING CONCIERGE ── -->
<div class="floating-concierge">
  <div class="concierge-card" id="conciergeCard">
    <div style="font-family:var(--mono);font-size:11px;color:var(--text-faint);display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;">
      <span>LIVE ASSISTANCE</span>
      <span onclick="toggleConcierge()" style="cursor:pointer;font-size:18px;font-weight:700;">&times;</span>
    </div>
    <a href="https://wa.me/8801762033445" target="_blank" rel="noopener" class="concierge-link wa">
      <svg width="18" height="18" viewBox="0 0 32 32">
        <path d="M16 5.8a10.2 10.2 0 0 0-8.8 15.4L6 26l5-1.3A10.2 10.2 0 1 0 16 5.8zm5.9 14.5c-.3.7-1.5 1.4-2.1 1.4-.6 0-1.3.2-3.8-.8a12.8 12.8 0 0 1-5.1-4.5c-.2-.3-.6-1-.6-1.9 0-.9.5-1.4.7-1.6.2-.2.4-.3.6-.3h.5c.2 0 .4 0 .5.4l.7 1.8c.1.2.1.4 0 .6l-.3.4c-.1.2-.3.3-.4.5-.1.1-.3.3-.1.6.3.5 1 1.7 2.4 2.5 1.5.8 2.3.9 2.6.8.3-.1.9-.7 1.1-1.1.2-.4.4-.3.7-.2.3.1 1.9.9 2.2 1.1.3.1.5.3.5.5s0 .9-.3 1.2z" fill="#fff"/>
      </svg>
      <span>WhatsApp Concierge</span>
    </a>
    <a href="https://t.me/Pas_agent_bot" target="_blank" rel="noopener" class="concierge-link tg">
      <svg width="18" height="18" viewBox="0 0 32 32">
        <path d="M7.6 15.4l14.2-5.8c.7-.3 1.2.1 1 1l-2.4 11.4c-.2.8-.7 1-1.4.6l-3.7-2.7-1.8 1.7c-.2.2-.4.4-.8.4l.3-3.7 6.7-6c.3-.3-.1-.4-.4-.2l-8.3 5.2-3.6-1.1c-.8-.2-.8-.8.1-1.1z" fill="#fff"/>
      </svg>
      <span>Telegram Official Bot</span>
    </a>
  </div>
  <div class="concierge-pill" onclick="toggleConcierge()">
    <span class="pulse-dot"></span>
    <span data-i18n="floating_help">Live Assistance</span>
  </div>
</div>

<!-- ── TOAST NOTIFICATION ── -->
<div class="toast" id="cmdToast">
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
  <span id="cmdToastMsg">Command copied to clipboard!</span>
</div>

<!-- ── CLIENT SCRIPT & BILINGUAL LOGIC ── -->
<script>
const rawFaq = ${JSON.stringify(faqData)};
const rawHubs = ${JSON.stringify(featureHubs)};
const rawCommands = ${JSON.stringify(commandsMatrix)};

const translations = {
  en: {
    top_status: "PASA SENTINEL ONLINE // v3.5.1 (BUILD 47)",
    nav_benefits: "Advantages",
    nav_pillars: "Architecture",
    nav_features: "Capabilities",
    nav_commands: "86 C2 Commands",
    nav_comparison: "Comparison",
    nav_faq: "Master FAQ",
    nav_pricing: "Licensing",
    nav_download: "Download APK",

    hero_badge: "SOVEREIGN MOBILE DEFENSE // ANDROID 8.0 – 16",
    hero_title: "YOUR PHONE WILL NEVER SURRENDER.",
    hero_sub: "When standard trackers go blind in 10 seconds, PASA Sentinel enforces Knox-grade hardware lockdown, captures perpetrator forensics headlessly, and protects your sovereign personal data.",
    hero_cta_apk: "Download Tactical APK (v3.5.1)",
    hero_cta_explore: "Explore Capabilities",

    spec_uninstall: "Uninstallation Defense",
    spec_val_knox: "Knox Supervisor (Permanent)",
    spec_storage: "Surveillance Storage",
    spec_val_zerostore: "0-Cloud Trails (Telegram Direct)",
    spec_c2: "C2 Control Channels",
    spec_crypto: "Cryptographic Trust",

    benefits_tag: "WHY PASA SENTINEL // CORE ADVANTAGES",
    benefits_title: "6 Operational Guarantees No Other App Can Match",
    benefits_lede: "Engineered specifically to solve the exact ways modern phone thieves and hostile actors exploit standard tracking apps.",
    b1_title: "Permanent Anti-Uninstall",
    b1_desc: "Android Device Owner OS lockout grays out Uninstall and Force Stop. Safe Mode and recovery resets are neutralized.",
    b2_title: "100% Zero-Storage Privacy",
    b2_desc: "Zero photos, zero GPS tracks, zero recordings saved on servers. Everything streams direct to your Telegram and shreds from RAM.",
    b3_title: "Air-Gapped Cellular SMS C2",
    b3_desc: "Maintains full command authority even when mobile data, Wi-Fi, and location are shut off, using cryptographic TOTP tokens.",
    b4_title: "Hardware-Level Killswitches",
    b4_desc: "Sever physical USB data pins to block Cellebrite forensic extraction boxes. Mute cameras and microphones at HAL level.",
    b5_title: "Duress Decoy Sandbox OS",
    b5_desc: "Under armed coercion, entering your secret Decoy PIN unlocks a sterile decoy system while secretly broadcasting an emergency SOS.",
    b6_title: "Lifetime Sovereign License",
    b6_desc: "Zero monthly or yearly subscription traps. One single cryptographic key protects your device for a lifetime.",

    pillars_tag: "FOUNDATIONAL SECURITY PILLARS",
    pillars_title: "Engineered Without Google Play Compromises",
    pillars_lede: "Standard Play Store apps are restricted by third-party sandbox policies. PASA Sentinel operates at the Device Owner supervisor layer with zero external dependencies.",
    p1_title: "Knox Device Owner Permanence",
    p1_desc: "Configured as system supervisor. Android itself grays out the 'Uninstall' and 'Force Stop' buttons. Blocks Safe Mode booting, factory resets, developer options, and notification shade pulldown.",
    p2_title: "Strategy 1: 100% Zero-Storage",
    p2_desc: "Zero photos, zero GPS tracks, and zero audio recordings are stored on our servers or cloud databases. Evidence streams directly to your private Telegram bot and is shredded from device RAM immediately.",
    p3_title: "Air-Gapped Cellular SMS C2",
    p3_desc: "Maintains full remote control even when mobile data, Wi-Fi, and location are shut off. Authenticated via RFC 6238 TOTP tokens or master PIN, returning live GPS and status pins via cellular SMS.",

    feat_tag: "TACTICAL DEFENSE ARSENAL",
    feat_title: "8-Hub Covert Defense Capabilities",
    feat_lede: "Battle-tested countermeasures built specifically to defeat professional criminal theft, forensic extractions, and armed robbery.",
    hub_all: "All Capabilities",
    threat_label: "Neutralizes Attack:",
    commands_label: "Commands:",

    cmd_tag: "COMMAND CONSOLE REPERTOIRE",
    cmd_title: "86 Telegram & Air-Gapped SMS Commands",
    cmd_lede: "Every single hardware defensive action is remotely triggerable via Telegram Bot C2 or cellular SMS. Click any command to copy its syntax.",
    cmd_all: "All Commands",
    cmd_cat_core: "Core & Diagnostics",
    cmd_cat_knox: "Device Owner Suite",
    cmd_cat_location: "Location & Cellular",
    cmd_cat_forensics: "Covert Forensics",
    cmd_cat_lockdown: "Lockdown & Alert",
    cmd_cat_traps: "Traps & Deception",
    cmd_cat_telephony: "Telephony & Extraction",
    cmd_cat_system: "System & Maintenance",

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
    plan_eval_period: "3-Day Evaluation Period",
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

    steps_hdr: "HOW LICENSE ACTIVATION WORKS",
    step1_title: "Complete Payment",
    step1_desc: "Pay via Binance Pay (Crypto) or request an official bKash account via WhatsApp concierge.",
    step2_title: "Share TXID / Receipt",
    step2_desc: "Send your transaction screenshot or TXID directly to our WhatsApp concierge or Telegram bot.",
    step3_title: "Instant Activation",
    step3_desc: "Receive your unique Ed25519 digital license key in 5–10 minutes with full setup onboarding support.",

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
    top_status: "পাসা সেন্টিনেল অনলাইন // v3.5.1 (বিল্ড ৪৭)",
    nav_benefits: "সুবিধাসমূহ",
    nav_pillars: "আর্কিটেকচার",
    nav_features: "ফিচারসমূহ",
    nav_commands: "৮৬টি কমান্ড",
    nav_comparison: "তুলনা",
    nav_faq: "প্রশ্নোত্তর",
    nav_pricing: "লাইসেন্সিং",
    nav_download: "এপিকে ডাউনলোড",

    hero_badge: "সার্বভৌম মোবাইল ডিফেন্স // অ্যান্ড্রয়েড ৮.০ – ১৬",
    hero_title: "আপনার ফোন আর কখনোই আত্মসমর্পণ করবে না।",
    hero_sub: "চুরি হওয়ার ১০ সেকেন্ডের মধ্যে যখন সাধারণ ট্র্যাকার অন্ধ হয়ে যায়—পাসা সেন্টিনেল নক্স-গ্রেড হার্ডওয়্যার লকডাউন চাপিয়ে অপরাধীর আসল পরিচয় শিকার করে এবং আপনার ব্যক্তিগত ডেটা রক্ষা করে।",
    hero_cta_apk: "ট্যাকটিক্যাল এপিকে ডাউনলোড (v3.5.1)",
    hero_cta_explore: "ফিচারসমূহ দেখুন",

    spec_uninstall: "আনইন্সটল প্রতিরোধ",
    spec_val_knox: "নক্স সুপারভাইজার (স্থায়ী)",
    spec_storage: "সার্ভিল্যান্স স্টোরেজ",
    spec_val_zerostore: "০-ক্লাউড ট্রেইল (টেলিগ্রাম ডিরেক্ট)",
    spec_c2: "সি২ নিয়ন্ত্রণ চ্যানেল",
    spec_crypto: "ক্রিপ্টোগ্রাফিক ট্রাস্ট",

    benefits_tag: "কেন পাসা সেন্টিনেল? // মৌলিক সুবিধাসমূহ",
    benefits_title: "৬টি অপারেশনাল গ্যারান্টি যা অন্য কোনো অ্যাপে সম্ভব নয়",
    benefits_lede: "পেশাদার চোর বা ছিনতাইকারীরা যেভাবে সাধারণ ট্র্যাকিং অ্যাপ অকেজো করে দেয়—তা সমূলে প্রতিহত করার জন্য বিশেষভাবে নির্মিত।",
    b1_title: "স্থায়ী আনইন্সটল প্রতিরোধ",
    b1_desc: "অ্যান্ড্রয়েড ডিভাইস ওনার ওএস নিজেই আনইন্সটল ও ফোর্স স্টপ বাটন নিষ্ক্রিয় রাখে। সেফ মোড বা রিকভারি দিয়ে মোছা অসম্ভব।",
    b2_title: "১০০% জিরো-স্টোরেজ প্রাইভেসি",
    b2_desc: "সার্ভার বা ক্লাউডে ০ বাইট ছবি বা লোকেশন জমা হয় না। সরাসরি আপনার নিজস্ব টেলিগ্রামে যায় এবং ফোন থেকে সাথে সাথে মেমোরি ধ্বংস হয়।",
    b3_title: "অফলাইন এয়ার-গ্যাপড এসএমএস সি২",
    b3_desc: "মোবাইল ডাটা, ওয়াইফাই ও জিপিএস বন্ধ থাকলেও সাধারণ বাটন ফোন থেকে এসএমএস পাঠিয়ে স্যাটেলাইট ম্যাপ লোকেশন ও ফুল কন্ট্রোল বজায় থাকে।",
    b4_title: "হার্ডওয়্যার-লেভেল কিলসুইচ",
    b4_desc: "চার্জিং ঠিক রেখে ইউএসবি ডেটা পিন বন্ধ করে দেয়—ফলে কোনো সার্ভিসিং দোকানে ক্যাবল বা ফরেনসিক ডিভাইস দিয়ে ডেটা চুরি করা যায় না।",
    b5_title: "অস্ত্রের মুখে ডিকয় ওএস",
    b5_desc: "ছিনতাইকারী জোর করে পাসওয়ার্ড চাইলে ডিকয় পিন দিলে ব্যাংকিং অ্যাপ ছাড়া খালি ডামি ওএস খোলে এবং গোপনে চোরের ছবি ও এসওএস চলে যায়।",
    b6_title: "আজীবন স্থায়ী মালিকানা",
    b6_desc: "কোনো মাসিক বা বার্ষিক সাবস্ক্রিপশন ট্র্যাপ নেই। একবার এককালীন লাইসেন্স নিয়ে সারাজীবন নিশ্চিন্তে ব্যবহার করুন।",

    pillars_tag: "মৌলিক নিরাপত্তা স্তম্ভ",
    pillars_title: "গুগল প্লে স্টোরের দুর্বলতা ছাড়াই নির্মিত",
    pillars_lede: "প্লে স্টোরের সাধারণ অ্যাপগুলো স্যান্ডবক্স পলিসিতে বন্দি। পাসা সেন্টিনেল সরাসরি অ্যান্ড্রয়েডের ডিভাইস ওনার সুপারভাইজার লেয়ারে কাজ করে।",
    p1_title: "নক্স ডিভাইস ওনার সুরক্ষা",
    p1_desc: "সিস্টেম সুপারভাইজার হিসেবে নিযুক্ত। অ্যান্ড্রয়েড নিজেই আনইন্সটল ও ফোর্স স্টপ বাটন নিষ্ক্রিয় করে দেয়। সেফ মোড বুট, ফ্যাক্টরি রিসেট ও নোটিফিকেশন ড্রয়ার সম্পূর্ণ লক থাকে।",
    p2_title: "স্ট্র্যাটেজি ১: ১০০% জিরো-স্টোরেজ",
    p2_desc: "আমাদের সার্ভার বা কোনো ক্লাউড ডাটাবেজে ব্যবহারকারীর একটি ছবি, জিপিএস ট্র্যাক বা অডিও ফাইলও সংরক্ষণ করা হয় না। সমস্ত প্রমাণ সরাসরি আপনার নিজস্ব টেলিগ্রাম বটে চলে যায়।",
    p3_title: "এয়ার-গ্যাপড সেলুলার এসএমএস সি২",
    p3_desc: "মোবাইল ডাটা, ওয়াইফাই এবং লোকেশন বন্ধ থাকলেও সেলুলার নেটওয়ার্কের মাধ্যমে ফোন সম্পূর্ণ নিয়ন্ত্রণে থাকে। ক্রিপ্টোগ্রাফিক TOTP ও মাস্টার পিন দিয়ে এসএমএস কমান্ড পরিচালিত হয়।",

    feat_tag: "ট্যাকটিক্যাল ডিফেন্স ক্ষমতা",
    feat_title: "৮টি অপারেশানাল ডিফেন্স হাবের পূর্ণাঙ্গ ক্ষমতা",
    feat_lede: "পেশাদার চোরের বাস্তব কৌশল, ফরেনসিক ক্যাবল এক্সট্রাকশন এবং অস্ত্রধারী ডাকাতের হাত থেকে বাঁচার নিখুঁত প্রতিরক্ষা ব্যবস্থা।",
    hub_all: "সকল ফিচারসমূহ",
    threat_label: "যে আক্রমণ প্রতিহত করে:",
    commands_label: "সংশ্লিষ্ট কমান্ড:",

    cmd_tag: "পূর্ণাঙ্গ কমান্ড সম্ভার",
    cmd_title: "৮৬টি টেলিগ্রাম ও অফলাইন এসএমএস কমান্ড",
    cmd_lede: "আপনার ব্যক্তিগত টেলিগ্রাম বট অথবা বাটন ফোনের সাধারণ এসএমএস দিয়ে প্রতিটি ফিচার নিয়ন্ত্রণযোগ্য। সিনট্যাক্স কপি করতে যেকোনো কমান্ডে ক্লিক করুন।",
    cmd_all: "সকল কমান্ড",
    cmd_cat_core: "কোর ও ডায়াগনস্টিকস",
    cmd_cat_knox: "ডিভাইস ওনার স্যুট",
    cmd_cat_location: "লোকেশন ও সেলুলার",
    cmd_cat_forensics: "গোপন নজরদারি",
    cmd_cat_lockdown: "লকডাউন ও অ্যালার্ট",
    cmd_cat_traps: "ট্র্যাপ ও প্রতারণা",
    cmd_cat_telephony: "টেলিফোনি ও এক্সট্রাকশন",
    cmd_cat_system: "সিস্টেম ও রক্ষণাবেক্ষণ",

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
    plan_eval_period: "৩ দিনের পূর্ণ মূল্যায়ন মেয়াদ",
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

    steps_hdr: "লাইসেন্স যেভাবে সক্রিয় করবেন",
    step1_title: "পেমেন্ট সম্পন্ন করুন",
    step1_desc: "Binance Pay (Crypto) অথবা হোয়াটসঅ্যাপ কনসিয়ার্জে অফিসিয়াল বিকাশ নম্বর নিয়ে ট্রান্সফার করুন।",
    step2_title: "TXID / স্ক্রিনশট পাঠান",
    step2_desc: "পেমেন্টের স্ক্রিনশট বা ট্রানজ্যাকশন আইডি আমাদের হোয়াটসঅ্যাপ বা টেলিগ্রাম বটে শেয়ার করুন।",
    step3_title: "তাৎক্ষণিক অ্যাক্টিভেশন",
    step3_desc: "৫–১০ মিনিটের মধ্যে আপনার ব্যক্তিগত Ed25519 ক্রিপ্টোগ্রাফিক কি পেয়ে যাবেন এবং আজীবন আপডেট সচল হবে।",

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
let currentFaqCategory = 'all';
let currentFeatureHub = 'knox';
let currentCommandCategory = 'core';
let showAllFeatures = false;
let showAllCommands = false;

function setLanguage(lang) {
  if (lang !== 'en' && lang !== 'bn') {
    lang = 'en';
  }
  currentLang = lang;
  document.documentElement.setAttribute('data-lang', lang);
  document.documentElement.lang = lang;

  const btnEn = document.getElementById('btnEn');
  const btnBn = document.getElementById('btnBn');
  if (btnEn) btnEn.classList.toggle('active', lang === 'en');
  if (btnBn) btnBn.classList.toggle('active', lang === 'bn');

  const dict = (typeof translations !== 'undefined' && translations && translations[lang]) 
    ? translations[lang] 
    : ((typeof translations !== 'undefined' && translations && translations.en) ? translations.en : {});

  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (key && dict && dict[key] !== undefined) {
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

  try { renderFeatureHubTabs(); } catch(e) { console.error('Hub tabs err:', e); }
  try { renderFeatureCards(); } catch(e) { console.error('Hub cards err:', e); }
  try { renderCommandPills(); } catch(e) { console.error('Cmd pills err:', e); }
  try { renderCommands(); } catch(e) { console.error('Commands err:', e); }
  try { renderFaqPills(); } catch(e) { console.error('FAQ pills err:', e); }
  try { renderFaq(); } catch(e) { console.error('FAQ err:', e); }

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

/* ── Interactive Feature Hubs Logic ── */
function renderFeatureHubTabs() {
  const tabsContainer = document.getElementById('hubTabs');
  if (!tabsContainer || typeof rawHubs === 'undefined') return;
  const isBn = currentLang === 'bn';

  let totalCards = 0;
  rawHubs.forEach(h => { totalCards += (h.cards ? h.cards.length : 0); });

  const hubOrder = ['knox', 'forensics', 'deception', 'location', 'traps', 'sms', 'privacy', 'diagnostics'];
  const tabs = [];

  hubOrder.forEach(hid => {
    const h = rawHubs.find(item => item.id === hid);
    if (h) {
      const count = h.cards ? h.cards.length : 0;
      tabs.push({
        id: h.id,
        label: (isBn ? h.nameBn : h.nameEn) + ' (' + count + ')'
      });
    }
  });

  tabs.push({
    id: 'all',
    label: isBn ? 'সকল ফিচার (' + totalCards + ')' : 'All Capabilities (' + totalCards + ')'
  });

  tabsContainer.innerHTML = tabs.map(tab => {
    const active = currentFeatureHub === tab.id ? 'active' : '';
    return \`<button class="hub-tab \${active}" onclick="setFeatureHub('\${tab.id}')">\${tab.label}</button>\`;
  }).join('');
}

function setFeatureHub(hubId) {
  currentFeatureHub = hubId;
  showAllFeatures = false;
  renderFeatureHubTabs();
  renderFeatureCards();
}

function toggleExpandFeatures() {
  showAllFeatures = !showAllFeatures;
  renderFeatureCards();
  if (!showAllFeatures) {
    document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' });
  }
}

function renderFeatureCards() {
  const container = document.getElementById('featureCardsContainer');
  if (!container || typeof rawHubs === 'undefined') return;
  const isBn = currentLang === 'bn';

  let allMatchingCards = [];
  rawHubs.forEach(hub => {
    if (currentFeatureHub !== 'all' && hub.id !== currentFeatureHub) return;
    if (Array.isArray(hub.cards)) {
      hub.cards.forEach(c => {
        allMatchingCards.push({ ...c, hubIcon: hub.icon, hubName: isBn ? hub.nameBn : hub.nameEn });
      });
    }
  });

  let displayCards = allMatchingCards;
  let showExpandBtn = false;

  if (currentFeatureHub === 'all' && !showAllFeatures && allMatchingCards.length > 9) {
    displayCards = allMatchingCards.slice(0, 9);
    showExpandBtn = true;
  }

  const cardsHtml = displayCards.map(c => {
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
            <div style="color:var(--cyan);display:flex;align-items:center;">\${c.hubIcon || ''}</div>
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

  let expandBtnHtml = '';
  if (currentFeatureHub === 'all') {
    if (showExpandBtn) {
      const remaining = allMatchingCards.length - 9;
      expandBtnHtml = \`
        <div class="expand-btn-container">
          <button class="btn-expand-more" onclick="toggleExpandFeatures()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
            \${isBn ? ('সবগুলো ৪৫টি ফিচার দেখুন (' + remaining + 'টি আরও)') : ('Show All 45 Capabilities (' + remaining + ' More)')}
          </button>
        </div>
      \`;
    } else if (showAllFeatures && allMatchingCards.length > 9) {
      expandBtnHtml = \`
        <div class="expand-btn-container">
          <button class="btn-expand-more" onclick="toggleExpandFeatures()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="15" x2="12" y2="9"/><line x1="6" y1="15" x2="12" y2="9"/></svg>
            \${isBn ? 'সংক্ষেপ করুন' : 'Show Less'}
          </button>
        </div>
      \`;
    }
  }

  container.innerHTML = cardsHtml + expandBtnHtml;
}

/* ── 86-Command Matrix Logic ── */
function renderCommandPills() {
  const pillsContainer = document.getElementById('commandPills');
  if (!pillsContainer || typeof rawCommands === 'undefined') return;
  const isBn = currentLang === 'bn';

  const catMap = {
    core: { en: 'Core (' + rawCommands.filter(c => c.cat === 'core').length + ')', bn: 'কোর (' + rawCommands.filter(c => c.cat === 'core').length + ')' },
    knox: { en: 'Device Owner (' + rawCommands.filter(c => c.cat === 'knox').length + ')', bn: 'ডিভাইস ওনার (' + rawCommands.filter(c => c.cat === 'knox').length + ')' },
    forensics: { en: 'Covert Forensics (' + rawCommands.filter(c => c.cat === 'forensics').length + ')', bn: 'ফরেনসিক (' + rawCommands.filter(c => c.cat === 'forensics').length + ')' },
    location: { en: 'Location & RF (' + rawCommands.filter(c => c.cat === 'location').length + ')', bn: 'লোকেশন ও আরএফ (' + rawCommands.filter(c => c.cat === 'location').length + ')' },
    lockdown: { en: 'Lockdown (' + rawCommands.filter(c => c.cat === 'lockdown').length + ')', bn: 'লকডাউন (' + rawCommands.filter(c => c.cat === 'lockdown').length + ')' },
    traps: { en: 'Traps & Deception (' + rawCommands.filter(c => c.cat === 'traps').length + ')', bn: 'ট্র্যাপ ও ডিফেন্স (' + rawCommands.filter(c => c.cat === 'traps').length + ')' },
    telephony: { en: 'Telephony (' + rawCommands.filter(c => c.cat === 'telephony').length + ')', bn: 'টেলিফোনি (' + rawCommands.filter(c => c.cat === 'telephony').length + ')' },
    system: { en: 'System (' + rawCommands.filter(c => c.cat === 'system').length + ')', bn: 'সিস্টেম (' + rawCommands.filter(c => c.cat === 'system').length + ')' },
    all: { en: 'All Commands (' + rawCommands.length + ')', bn: 'সকল কমান্ড (' + rawCommands.length + ')' }
  };

  const cats = ['core', 'knox', 'forensics', 'location', 'lockdown', 'traps', 'telephony', 'system', 'all'];

  pillsContainer.innerHTML = cats.map(cat => {
    const active = currentCommandCategory === cat ? 'active' : '';
    const label = isBn ? (catMap[cat]?.bn || cat) : (catMap[cat]?.en || cat);
    return \`<button class="cmd-pill \${active}" onclick="setCommandCategory('\${cat}')">\${label}</button>\`;
  }).join('');
}

function setCommandCategory(catId) {
  currentCommandCategory = catId;
  showAllCommands = false;
  const searchInput = document.getElementById('commandSearch');
  if (searchInput && searchInput.value) {
    searchInput.value = '';
  }
  renderCommandPills();
  renderCommands();
}

function filterCommands() {
  renderCommands();
}

function toggleExpandCommands() {
  showAllCommands = !showAllCommands;
  renderCommands();
  if (!showAllCommands) {
    document.getElementById('commands')?.scrollIntoView({ behavior: 'smooth' });
  }
}

function renderCommands() {
  const container = document.getElementById('commandsContainer');
  const statusEl = document.getElementById('commandSearchStatus');
  if (!container || typeof rawCommands === 'undefined') return;
  const isBn = currentLang === 'bn';
  const query = (document.getElementById('commandSearch')?.value || '').toLowerCase().trim();

  let filtered = [];
  const isSearchActive = query.length > 0;

  if (isSearchActive) {
    filtered = rawCommands.filter(c => {
      if (!c) return false;
      const cmdStr = (c.cmd || '').toLowerCase();
      const descEn = (c.descEn || '').toLowerCase();
      const descBn = (c.descBn || '').toLowerCase();
      const params = (c.params || '').toLowerCase();
      const cat = (c.cat || '').toLowerCase();
      return cmdStr.includes(query) || descEn.includes(query) || descBn.includes(query) || params.includes(query) || cat.includes(query);
    });

    if (statusEl) {
      statusEl.style.display = 'inline-block';
      statusEl.innerHTML = isBn 
        ? ('অনুসন্ধান ফলাফল: <strong>' + filtered.length + 'টি</strong> কমান্ড পাওয়া গেছে ("' + query + '")')
        : ('Search Results: Found <strong>' + filtered.length + '</strong> matching commands for "' + query + '"');
    }
  } else {
    if (statusEl) {
      statusEl.style.display = 'none';
      statusEl.textContent = '';
    }
    filtered = rawCommands.filter(c => {
      if (!c) return false;
      if (currentCommandCategory !== 'all' && c.cat !== currentCommandCategory) return false;
      return true;
    });
  }

  if (filtered.length === 0) {
    container.innerHTML = \`<div style="grid-column:1/-1;text-align:center;padding:36px;color:var(--text-faint);font-size:14.5px;">
      \${isBn ? 'কোনো কমান্ড খুঁজে পাওয়া যায়নি।' : 'No matching commands found.'}
    </div>\`;
    return;
  }

  let displayCommands = filtered;
  let showExpandBtn = false;

  if (!isSearchActive && currentCommandCategory === 'all' && !showAllCommands && filtered.length > 12) {
    displayCommands = filtered.slice(0, 12);
    showExpandBtn = true;
  }

  const cardsHtml = displayCommands.map(c => {
    const desc = isBn ? c.descBn : c.descEn;
    const tgBadge = c.tg ? '<span class="tag-tg">TELEGRAM</span>' : '';
    const smsBadge = c.sms ? '<span class="tag-sms">SMS</span>' : '';

    return \`
      <div class="cmd-card">
        <div>
          <div class="cmd-hdr">
            <div class="cmd-token">
              \${c.cmd}
              \${c.params ? ('<span class="cmd-params">' + c.params + '</span>') : ''}
            </div>
            <div class="cmd-channel-tags">\${tgBadge}\${smsBadge}</div>
          </div>
          <div class="cmd-body">\${desc}</div>
        </div>
        <div class="cmd-footer">
          <span style="text-transform:uppercase;">CAT: \${c.cat}</span>
          <button class="btn-copy-cmd" onclick="copyCommand('\${c.cmd}')">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
            \${isBn ? 'কপি' : 'Copy'}
          </button>
        </div>
      </div>
    \`;
  }).join('');

  let expandBtnHtml = '';
  if (!isSearchActive && currentCommandCategory === 'all') {
    if (showExpandBtn) {
      const remaining = filtered.length - 12;
      expandBtnHtml = \`
        <div class="expand-btn-container">
          <button class="btn-expand-more" onclick="toggleExpandCommands()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
            \${isBn ? ('সবগুলো ৮৬টি কমান্ড দেখুন (' + remaining + 'টি আরও)') : ('Show All 86 Commands (' + remaining + ' More)')}
          </button>
        </div>
      \`;
    } else if (showAllCommands && filtered.length > 12) {
      expandBtnHtml = \`
        <div class="expand-btn-container">
          <button class="btn-expand-more" onclick="toggleExpandCommands()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="15" x2="12" y2="9"/><line x1="6" y1="15" x2="12" y2="9"/></svg>
            \${isBn ? 'সংক্ষেপ করুন' : 'Show Less'}
          </button>
        </div>
      \`;
    }
  }

  container.innerHTML = cardsHtml + expandBtnHtml;
}

/* ── FAQ Logic ── */
function renderFaqPills() {
  const pillsContainer = document.getElementById('faqPills');
  if (!pillsContainer || typeof rawFaq === 'undefined' || !Array.isArray(rawFaq)) return;

  const isBn = currentLang === 'bn';
  const categories = [
    { id: 'all', en: 'All Questions (' + rawFaq.length + ')', bn: 'সবগুলো প্রশ্ন (' + rawFaq.length + ')' }
  ];

  const seen = new Set();
  rawFaq.forEach(f => {
    if (f && f.category && !seen.has(f.category)) {
      seen.add(f.category);
      categories.push({
        id: f.category,
        en: f.categoryNameEn || f.category,
        bn: f.categoryNameBn || f.category
      });
    }
  });

  pillsContainer.innerHTML = categories.map(cat => {
    const label = isBn ? cat.bn : cat.en;
    const activeClass = currentFaqCategory === cat.id ? 'active' : '';
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
  if (!container || typeof rawFaq === 'undefined' || !Array.isArray(rawFaq)) return;

  const query = (document.getElementById('faqSearch')?.value || '').toLowerCase().trim();
  const isBn = currentLang === 'bn';

  const filtered = rawFaq.filter(item => {
    if (!item) return false;
    if (currentFaqCategory !== 'all' && item.category !== currentFaqCategory) {
      return false;
    }
    if (!query) return true;
    const qText = ((isBn ? item.qBn : item.qEn) || '').toLowerCase();
    const aText = ((isBn ? item.aBn : item.aEn) || '').toLowerCase();
    return qText.includes(query) || aText.includes(query);
  });

  if (filtered.length === 0) {
    container.innerHTML = \`<div style="text-align:center;padding:36px;color:var(--text-faint);font-size:14.5px;">
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
    if (val === 'bn' || val === 'en') {
      saved = val;
    }
  } catch (e) {}
  setLanguage(saved);
});
</script>

</body>
</html>`;
}

const html = generateHtml();
fs.writeFileSync(targetHtmlPath, html, 'utf8');
console.log(`Successfully wrote ${html.length} bytes to ${targetHtmlPath}`);
