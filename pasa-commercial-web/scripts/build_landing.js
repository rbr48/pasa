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
<title>PASA Sentinel — Sovereign Android Mobile Defense &amp; Anti-Theft Intelligence</title>
<meta name="description" content="PASA Sentinel: Knox-Grade Device Owner anti-theft defense system. Zero cloud storage, direct-to-Telegram evidence, air-gapped SMS C2, and anti-uninstall security for Android 8.0–16.">
<meta name="theme-color" content="#030712">
<link rel="icon" href="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ef4444' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z'/%3E%3C/svg%3E">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500;600;700&family=Tiro+Bangla:ital@0;1&display=swap" rel="stylesheet">
<style>
  :root {
    --bg-dark: #030712;
    --bg-card: #0b1120;
    --bg-card-hover: #0f172a;
    --surface: #0e1726;
    --border: rgba(255, 255, 255, 0.08);
    --border-hover: rgba(6, 182, 212, 0.4);
    --border-crimson: rgba(239, 68, 68, 0.35);

    --crimson: #ef4444;
    --crimson-glow: rgba(239, 68, 68, 0.25);
    --cyan: #06b6d4;
    --cyan-glow: rgba(6, 182, 212, 0.25);
    --amber: #f59e0b;
    --emerald: #10b981;

    --text: #ffffff;
    --text-dim: #e2e8f0;       /* High-contrast WCAG AAA */
    --text-faint: #94a3b8;     /* Clean secondary labels */
    --text-muted: #64748b;

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
    background: var(--bg-dark);
    color: var(--text);
    line-height: 1.6;
    -webkit-font-smoothing: antialiased;
    overflow-x: hidden;
  }

  /* Anchor Clearance (Header Collision Prevention) */
  section[id], div[id] {
    scroll-margin-top: 90px;
  }

  .wrap { max-width: 1160px; margin: 0 auto; padding: 0 24px; position: relative; z-index: 10; }
  .wrap-narrow { max-width: 820px; margin: 0 auto; padding: 0 24px; position: relative; z-index: 10; }

  /* ── Top Status Strip ── */
  .top-strip {
    background: #040816;
    border-bottom: 1px solid var(--border);
    padding: 8px 0; font-family: var(--mono); font-size: 11.5px;
    color: var(--text-faint); position: relative; z-index: 101;
  }
  .top-strip-inner {
    display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px;
  }
  .pulse-dot {
    width: 8px; height: 8px; border-radius: 50%; background: var(--emerald);
    box-shadow: 0 0 10px var(--emerald); display: inline-block;
  }
  .top-status-badge {
    display: inline-flex; align-items: center; gap: 7px; color: var(--text); font-weight: 600;
  }
  .top-specs {
    display: flex; align-items: center; gap: 16px; font-size: 11px;
  }
  .top-specs span { color: var(--cyan); }

  /* ── Smart Sticky Navigation ── */
  nav.nav {
    position: sticky; top: 0; z-index: 100;
    background: rgba(3, 7, 18, 0.94); backdrop-filter: blur(16px);
    border-bottom: 1px solid var(--border);
  }
  .nav-inner {
    display: flex; align-items: center; justify-content: space-between; height: 64px;
  }
  .brand {
    display: flex; align-items: center; gap: 10px; text-decoration: none; color: var(--text);
  }
  .brand-icon {
    width: 34px; height: 34px; border-radius: 8px;
    background: linear-gradient(135deg, rgba(239, 68, 68, 0.25), rgba(6, 182, 212, 0.2));
    border: 1px solid var(--border-crimson); display: flex; align-items: center; justify-content: center;
  }
  .brand-icon svg { width: 18px; height: 18px; color: var(--crimson); }
  .brand-text { font-weight: 800; font-size: 16px; letter-spacing: -0.01em; }
  .brand-tag { font-family: var(--mono); font-size: 9px; color: var(--cyan); letter-spacing: 0.08em; display: block; }

  .nav-menu { display: flex; align-items: center; gap: 6px; }
  .nav-link {
    color: var(--text-faint); text-decoration: none; font-size: 13.5px; font-weight: 500;
    padding: 8px 12px; border-radius: 6px; transition: all 0.2s ease;
  }
  .nav-link:hover { color: var(--text); background: rgba(255,255,255,0.06); }

  .nav-actions { display: flex; align-items: center; gap: 12px; }

  /* Language Switcher */
  .lang-switcher {
    display: flex; align-items: center; background: rgba(15, 23, 42, 0.9);
    border: 1px solid var(--border); border-radius: 8px; padding: 2px;
  }
  .lang-btn {
    background: transparent; border: none; color: var(--text-faint);
    font-family: var(--mono); font-size: 11px; font-weight: 700;
    padding: 5px 10px; border-radius: 6px; cursor: pointer; transition: all 0.2s;
  }
  .lang-btn.active {
    background: var(--crimson); color: #fff; box-shadow: 0 0 10px rgba(239, 68, 68, 0.4);
  }
  .lang-btn:hover:not(.active) { color: var(--text); }

  .nav-cta {
    background: linear-gradient(135deg, #ef4444, #dc2626); color: #fff;
    padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 700;
    text-decoration: none; display: inline-flex; align-items: center; gap: 6px;
    box-shadow: 0 0 16px rgba(239, 68, 68, 0.35); transition: all 0.2s;
  }
  .nav-cta:hover { transform: translateY(-1px); box-shadow: 0 0 22px rgba(239, 68, 68, 0.6); }

  /* Mobile Drawer */
  .mobile-toggle {
    display: none; background: none; border: 1px solid var(--border); color: var(--text);
    padding: 7px 10px; border-radius: 6px; cursor: pointer;
  }
  .mobile-drawer {
    position: fixed; top: 0; right: -290px; width: 280px; height: 100vh;
    background: #060b18; border-left: 1px solid var(--border); z-index: 200;
    padding: 24px; display: flex; flex-direction: column; gap: 16px;
    transition: right 0.3s ease; box-shadow: -10px 0 40px rgba(0,0,0,0.8);
  }
  .mobile-drawer.open { right: 0; }
  .mobile-drawer-hdr { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border); padding-bottom: 16px; }
  .mobile-drawer a { color: var(--text-dim); text-decoration: none; font-size: 15px; font-weight: 500; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.04); }
  .mobile-drawer a:hover { color: var(--cyan); }
  .drawer-overlay {
    position: fixed; inset: 0; background: rgba(0,0,0,0.7); z-index: 190; display: none;
  }
  .drawer-overlay.open { display: block; }

  @media (max-width: 960px) {
    .nav-menu { display: none; }
    .mobile-toggle { display: block; }
    .top-specs { display: none; }
  }

  /* ── Hero Section (Focused & Clean) ── */
  .hero {
    padding: 80px 0 70px; position: relative; text-align: center;
    background: radial-gradient(ellipse at 50% 10%, rgba(6, 182, 212, 0.12) 0%, rgba(3, 7, 18, 0.98) 70%);
    border-bottom: 1px solid var(--border);
  }
  .badge-tag {
    display: inline-flex; align-items: center; gap: 8px;
    padding: 5px 14px; border-radius: 20px; font-family: var(--mono); font-size: 11px;
    font-weight: 700; color: var(--cyan); background: rgba(6, 182, 212, 0.08);
    border: 1px solid var(--border-hover); margin-bottom: 22px;
  }
  .hero-title {
    font-size: clamp(30px, 4.8vw, 54px); font-weight: 900; line-height: 1.18;
    letter-spacing: -0.02em; margin-bottom: 18px; color: var(--text);
  }
  .hero-sub {
    font-size: clamp(15.5px, 1.9vw, 18px); color: var(--text-dim); max-width: 740px;
    margin: 0 auto 34px; line-height: 1.6;
  }
  .hero-cta-group {
    display: flex; flex-wrap: wrap; justify-content: center; align-items: center; gap: 14px;
    margin-bottom: 44px;
  }
  .btn-primary {
    background: linear-gradient(135deg, #ef4444 0%, #b91c1c 100%);
    color: #fff; padding: 13px 28px; border-radius: 10px; font-weight: 700; font-size: 15px;
    text-decoration: none; display: inline-flex; align-items: center; gap: 8px;
    box-shadow: 0 0 20px rgba(239, 68, 68, 0.4); border: 1px solid rgba(255,255,255,0.15);
    transition: all 0.2s ease;
  }
  .btn-primary:hover {
    transform: translateY(-2px); box-shadow: 0 0 30px rgba(239, 68, 68, 0.65);
  }
  .btn-ghost {
    background: transparent; color: var(--text-faint); padding: 13px 22px;
    border-radius: 10px; font-weight: 600; font-size: 14px; text-decoration: none;
    display: inline-flex; align-items: center; gap: 6px; border: 1px solid var(--border);
    transition: all 0.2s ease;
  }
  .btn-ghost:hover {
    color: var(--text); border-color: var(--cyan); background: rgba(6, 182, 212, 0.06);
  }

  /* Specs Bar */
  .specs-bar {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px;
    background: rgba(11, 17, 32, 0.85); border: 1px solid var(--border);
    border-radius: 12px; padding: 16px 20px; max-width: 940px; margin: 0 auto;
    text-align: left;
  }
  .spec-item {
    border-left: 2px solid var(--cyan); padding-left: 12px;
  }
  .spec-item.crimson { border-left-color: var(--crimson); }
  .spec-item.emerald { border-left-color: var(--emerald); }
  .spec-label { font-family: var(--mono); font-size: 10px; color: var(--text-faint); text-transform: uppercase; }
  .spec-val { font-size: 13.5px; font-weight: 700; color: var(--text); margin-top: 2px; }

  /* ── Section Structure ── */
  .section { padding: 80px 0; border-bottom: 1px solid var(--border); position: relative; }
  .section-hdr { text-align: center; margin-bottom: 48px; }
  .section-tag {
    font-family: var(--mono); font-size: 11px; font-weight: 700; color: var(--cyan);
    letter-spacing: 0.1em; text-transform: uppercase; margin-bottom: 10px; display: inline-block;
  }
  .section-title {
    font-size: clamp(24px, 3.4vw, 36px); font-weight: 800; letter-spacing: -0.01em; margin-bottom: 12px;
  }
  .section-lede {
    font-size: 15.5px; color: var(--text-dim); max-width: 680px; margin: 0 auto; line-height: 1.6;
  }

  /* ── 3 Architectural Pillars ── */
  .pillars-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px;
  }
  .pillar-card {
    background: var(--bg-card); border: 1px solid var(--border);
    border-radius: 14px; padding: 30px 26px; transition: all 0.25s ease;
  }
  .pillar-card:hover {
    border-color: var(--border-hover); transform: translateY(-3px);
    box-shadow: 0 12px 30px rgba(6, 182, 212, 0.1);
  }
  .pillar-icon {
    width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center;
    background: rgba(6, 182, 212, 0.1); border: 1px solid var(--border-hover); margin-bottom: 18px;
    color: var(--cyan);
  }
  .pillar-icon.crimson {
    background: rgba(239, 68, 68, 0.1); border-color: var(--border-crimson); color: var(--crimson);
  }
  .pillar-icon.emerald {
    background: rgba(16, 185, 129, 0.1); border-color: rgba(16, 185, 129, 0.35); color: var(--emerald);
  }
  .pillar-title { font-size: 18px; font-weight: 700; margin-bottom: 10px; color: var(--text); }
  .pillar-desc { font-size: 14px; color: var(--text-dim); line-height: 1.6; }

  /* ── 6 Core Defense Capabilities ── */
  .features-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px;
  }
  .feature-card {
    background: var(--bg-card); border: 1px solid var(--border);
    border-radius: 12px; padding: 24px 22px; transition: all 0.25s ease;
  }
  .feature-card:hover {
    border-color: rgba(255, 255, 255, 0.18); background: var(--bg-card-hover);
  }
  .feature-header {
    display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;
  }
  .feature-tag {
    font-family: var(--mono); font-size: 10px; font-weight: 700; color: var(--cyan);
    padding: 2px 7px; border-radius: 4px; background: rgba(6, 182, 212, 0.08); border: 1px solid var(--border-hover);
  }
  .feature-title { font-size: 16px; font-weight: 700; margin-bottom: 8px; color: var(--text); }
  .feature-desc { font-size: 13.5px; color: var(--text-dim); line-height: 1.55; }

  /* ── Comparison Table ── */
  .table-container {
    background: var(--bg-card); border: 1px solid var(--border);
    border-radius: 12px; overflow-x: auto; -webkit-overflow-scrolling: touch;
  }
  .comp-table {
    width: 100%; border-collapse: collapse; text-align: left; font-size: 13.5px;
  }
  .comp-table th {
    background: #090f1f; padding: 16px 20px; font-weight: 700; border-bottom: 1px solid var(--border);
    color: var(--text); font-family: var(--mono); font-size: 12px;
  }
  .comp-table td {
    padding: 15px 20px; border-bottom: 1px solid var(--border); color: var(--text-dim);
  }
  .comp-table tr:last-child td { border-bottom: none; }
  .comp-table tr:hover td { background: rgba(255, 255, 255, 0.02); }
  .comp-highlight {
    color: var(--cyan); font-weight: 700; background: rgba(6, 182, 212, 0.04);
  }
  .check-icon { color: var(--emerald); font-weight: 700; }
  .cross-icon { color: var(--crimson); font-weight: 700; }

  /* ── Master Technical FAQ (Searchable & Categorized) ── */
  .faq-controls {
    max-width: 820px; margin: 0 auto 30px; display: flex; flex-direction: column; gap: 14px;
  }
  .faq-search {
    width: 100%; background: #0b1120; border: 1px solid var(--border);
    border-radius: 10px; padding: 13px 18px; color: var(--text); font-size: 14.5px;
    outline: none; font-family: var(--font); transition: border-color 0.2s;
  }
  .faq-search:focus { border-color: var(--cyan); }
  .faq-pills {
    display: flex; flex-wrap: wrap; gap: 8px; justify-content: center;
  }
  .faq-pill {
    background: rgba(11, 17, 32, 0.8); border: 1px solid var(--border);
    color: var(--text-faint); padding: 6px 12px; border-radius: 20px; font-size: 12px;
    cursor: pointer; transition: all 0.2s; font-weight: 500;
  }
  .faq-pill.active {
    background: var(--cyan); color: #030712; font-weight: 700; border-color: var(--cyan);
  }
  .faq-pill:hover:not(.active) { color: var(--text); }

  .faq-list {
    max-width: 820px; margin: 0 auto; display: flex; flex-direction: column; gap: 10px;
  }
  .faq-item {
    background: var(--bg-card); border: 1px solid var(--border);
    border-radius: 10px; overflow: hidden; transition: border-color 0.2s;
  }
  .faq-item.open { border-color: var(--border-hover); background: #0c1426; }
  .faq-q {
    padding: 18px 20px; display: flex; align-items: center; justify-content: space-between;
    cursor: pointer; gap: 14px; user-select: none; font-weight: 600; font-size: 15px;
  }
  .faq-q:hover { color: var(--cyan); }
  .faq-icon { color: var(--cyan); transition: transform 0.2s; font-family: var(--mono); }
  .faq-item.open .faq-icon { transform: rotate(45deg); color: var(--crimson); }
  .faq-a {
    padding: 0 20px 18px; color: var(--text-dim); font-size: 14px; line-height: 1.65;
    display: none; border-top: 1px solid rgba(255, 255, 255, 0.04); padding-top: 14px;
  }

  /* ── Pricing & Licensing (Equal Height & Baseline Alignment) ── */
  .pricing-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px;
    align-items: stretch; margin-bottom: 48px;
  }
  .pricing-card {
    background: var(--bg-card); border: 1px solid var(--border);
    border-radius: 14px; padding: 32px 24px; display: flex; flex-direction: column;
    justify-content: space-between; height: 100%; position: relative; transition: all 0.25s ease;
  }
  .pricing-card:hover { transform: translateY(-3px); }
  .pricing-card.featured {
    border-color: var(--border-crimson);
    background: linear-gradient(180deg, rgba(239, 68, 68, 0.06) 0%, #0b1120 100%);
    box-shadow: 0 10px 30px rgba(239, 68, 68, 0.15);
  }
  .pricing-badge {
    position: absolute; top: -12px; left: 50%; transform: translateX(-50%);
    background: var(--crimson); color: #fff; font-family: var(--mono);
    font-size: 10px; font-weight: 700; padding: 3px 12px; border-radius: 20px;
    letter-spacing: 0.06em;
  }
  .pricing-card-body {
    flex: 1; display: flex; flex-direction: column;
  }
  .plan-name { font-size: 20px; font-weight: 800; margin-bottom: 6px; }
  .plan-desc { font-size: 13.5px; color: var(--text-faint); margin-bottom: 20px; min-height: 40px; }
  .plan-price { font-size: 38px; font-weight: 900; line-height: 1; margin-bottom: 4px; color: var(--text); }
  .plan-currency { font-size: 17px; color: var(--text-faint); font-weight: 600; }
  .plan-period { font-size: 12.5px; color: var(--cyan); margin-bottom: 22px; font-family: var(--mono); }
  
  .plan-features { list-style: none; margin-bottom: 26px; flex: 1; }
  .plan-features li {
    font-size: 13px; color: var(--text-dim); margin-bottom: 11px;
    display: flex; align-items: flex-start; gap: 8px; line-height: 1.45;
  }
  .plan-features svg { width: 16px; height: 16px; min-width: 16px; color: var(--emerald); margin-top: 2px; }

  .pricing-card a.btn-primary,
  .pricing-card a.btn-ghost {
    margin-top: auto; width: 100%; justify-content: center; text-align: center;
  }

  /* ── 3-Step License Activation Roadmap ── */
  .delivery-steps {
    background: #090f1f; border: 1px solid var(--border);
    border-radius: 12px; padding: 24px; margin-bottom: 32px;
  }
  .delivery-steps-title {
    font-size: 15px; font-weight: 700; margin-bottom: 18px; text-align: center;
    color: var(--text); font-family: var(--mono);
  }
  .steps-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px;
  }
  .step-box {
    background: rgba(11, 17, 32, 0.8); border: 1px solid var(--border);
    border-radius: 10px; padding: 18px 16px; text-align: left;
  }
  .step-num {
    width: 24px; height: 24px; border-radius: 6px; background: var(--cyan);
    color: #030712; font-weight: 800; font-size: 12px; display: inline-flex;
    align-items: center; justify-content: center; margin-bottom: 10px; font-family: var(--mono);
  }
  .step-box h4 { font-size: 14px; font-weight: 700; margin-bottom: 6px; color: var(--text); }
  .step-box p { font-size: 12.5px; color: var(--text-dim); line-height: 1.5; }

  /* ── Payment Channels Strip (Untouched as requested) ── */
  .payment-channels {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px;
  }
  .pay-method {
    background: #090f1f; border: 1px solid var(--border);
    border-radius: 10px; padding: 16px; display: flex; gap: 12px; align-items: center;
  }
  .pay-icon {
    width: 40px; height: 40px; border-radius: 8px; min-width: 40px;
    display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.06);
  }
  .pay-title { font-weight: 700; font-size: 13.5px; color: var(--text); }
  .pay-sub { font-family: var(--mono); font-size: 11.5px; color: var(--cyan); margin-top: 2px; }
  .pay-action-btn {
    font-size: 11px; font-weight: 700; text-decoration: none; padding: 4px 10px;
    border-radius: 4px; display: inline-flex; align-items: center; gap: 4px; margin-top: 6px;
  }

  /* ── Smart Floating Concierge Pill ── */
  .floating-concierge {
    position: fixed; bottom: 18px; right: 18px; z-index: 95;
    display: flex; flex-direction: column; align-items: flex-end; gap: 8px;
  }
  .concierge-pill {
    background: #0d1527; border: 1px solid var(--border-hover); color: #fff;
    padding: 8px 14px; border-radius: 24px; font-size: 12.5px; font-weight: 600;
    display: flex; align-items: center; gap: 7px; cursor: pointer;
    box-shadow: 0 6px 24px rgba(0,0,0,0.6); transition: all 0.2s ease;
  }
  .concierge-pill:hover { background: #14203d; border-color: var(--cyan); }
  .concierge-card {
    background: #090f1f; border: 1px solid var(--border); border-radius: 10px;
    padding: 12px; box-shadow: 0 14px 36px rgba(0,0,0,0.7); display: none;
    flex-direction: column; gap: 8px; width: 210px; backdrop-filter: blur(12px);
  }
  .concierge-card.open { display: flex; }
  .concierge-link {
    display: flex; align-items: center; gap: 8px; padding: 7px 10px; border-radius: 6px;
    text-decoration: none; font-size: 12.5px; font-weight: 600; color: #fff;
  }
  .concierge-link.wa { background: rgba(37, 211, 102, 0.15); color: #25D366; }
  .concierge-link.tg { background: rgba(34, 158, 217, 0.15); color: #229ed9; }

  /* ── Footer ── */
  footer {
    background: #02040a; border-top: 1px solid var(--border); padding: 50px 0 30px;
  }
  .footer-inner {
    display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 20px;
    margin-bottom: 24px;
  }
  .footer-links { display: flex; gap: 18px; font-size: 13px; }
  .footer-links a { color: var(--text-faint); text-decoration: none; transition: color 0.15s; }
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
      <div class="brand-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
      </div>
      <div>
        <div class="brand-text">PASA SENTINEL</div>
        <span class="brand-tag">SOVEREIGN DEFENSE</span>
      </div>
    </a>

    <!-- Desktop Navigation -->
    <div class="nav-menu">
      <a href="#pillars" class="nav-link" data-i18n="nav_pillars">Architecture</a>
      <a href="#features" class="nav-link" data-i18n="nav_features">Capabilities</a>
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
  <a href="#pillars" onclick="toggleMobileDrawer()" data-i18n="nav_pillars">Architecture</a>
  <a href="#features" onclick="toggleMobileDrawer()" data-i18n="nav_features">Capabilities</a>
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
      <a href="#pillars" class="btn-ghost">
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
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
        </div>
        <h3 class="pillar-title" data-i18n="p1_title">Knox Device Owner Permanence</h3>
        <p class="pillar-desc" data-i18n="p1_desc">
          Configured as system supervisor. Android itself grays out the "Uninstall" and "Force Stop" buttons. Blocks Safe Mode booting, factory resets, developer options, and notification shade pulldown.
        </p>
      </div>

      <!-- Pillar 2 -->
      <div class="pillar-card">
        <div class="pillar-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
        </div>
        <h3 class="pillar-title" data-i18n="p2_title">Strategy 1: 100% Zero-Storage</h3>
        <p class="pillar-desc" data-i18n="p2_desc">
          Zero photos, zero GPS tracks, and zero audio recordings are stored on our servers or cloud databases. Evidence streams directly to your private Telegram bot and is shredded from device RAM immediately.
        </p>
      </div>

      <!-- Pillar 3 -->
      <div class="pillar-card">
        <div class="pillar-icon emerald">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z"/></svg>
        </div>
        <h3 class="pillar-title" data-i18n="p3_title">Air-Gapped Cellular SMS C2</h3>
        <p class="pillar-desc" data-i18n="p3_desc">
          Maintains full remote control even when mobile data, Wi-Fi, and location are shut off. Authenticated via RFC 6238 TOTP tokens or master PIN, returning live GPS and status pins via cellular SMS.
        </p>
      </div>
    </div>
  </div>
</section>

<!-- ── 6 CORE DEFENSE CAPABILITIES ── -->
<section id="features" class="section">
  <div class="wrap">
    <div class="section-hdr">
      <span class="section-tag" data-i18n="feat_tag">HARDWARE DEFENSE CAPABILITIES</span>
      <h2 class="section-title" data-i18n="feat_title">Authentic Anti-Theft Countermeasures</h2>
      <p class="section-lede" data-i18n="feat_lede">
        Battle-tested mechanisms built specifically to counteract real-world criminal tactics.
      </p>
    </div>

    <div class="features-grid">
      <!-- 1 -->
      <div class="feature-card">
        <div class="feature-header">
          <span class="feature-tag">FORENSICS</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="2"><path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/><circle cx="12" cy="13" r="4"/></svg>
        </div>
        <h4 class="feature-title" data-i18n="f1_title">Zero-Blackout Headless Camera</h4>
        <p class="feature-desc" data-i18n="f1_desc">
          Captures high-resolution front/rear photos and 16kHz ambient audio recordings silently without screen flash, shutter audio, or camera preview popups.
        </p>
      </div>

      <!-- 2 -->
      <div class="feature-card">
        <div class="feature-header">
          <span class="feature-tag">SECURITY</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
        </div>
        <h4 class="feature-title" data-i18n="f2_title">Hardware Escrow Token PIN Reset</h4>
        <p class="feature-desc" data-i18n="f2_desc">
          On Android 14, 15, and 16, Google removed standard password reset APIs. PASA uses Knox cryptographic Escrow Tokens to remotely change lockscreen PINs over the air.
        </p>
      </div>

      <!-- 3 -->
      <div class="feature-card">
        <div class="feature-header">
          <span class="feature-tag">TELEPHONY</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
        </div>
        <h4 class="feature-title" data-i18n="f3_title">SIM Eject Lockdown &amp; Caller ID Trap</h4>
        <p class="feature-desc" data-i18n="f3_desc">
          Instantly locks down the device if the owner SIM is ejected. When the thief inserts their own SIM, PASA secretly dispatches an SMS exposing the thief's phone number via Caller ID.
        </p>
      </div>

      <!-- 4 -->
      <div class="feature-card">
        <div class="feature-header">
          <span class="feature-tag">HARDWARE</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="2"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/></svg>
        </div>
        <h4 class="feature-title" data-i18n="f4_title">Hardware USB Pin Killswitch</h4>
        <p class="feature-desc" data-i18n="f4_desc">
          Android 12+ physical USB data severing (<code>setUsbDataSignalingEnabled(false)</code>) blocks Cellebrite, GrayKey forensic extraction boxes, and EDL flashing while allowing AC power charging.
        </p>
      </div>

      <!-- 5 -->
      <div class="feature-card">
        <div class="feature-header">
          <span class="feature-tag">ANTI-TAMPER</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        </div>
        <h4 class="feature-title" data-i18n="f5_title">Anti-EDL Dead Man's Switch</h4>
        <p class="feature-desc" data-i18n="f5_desc">
          An autonomous local countdown timer triggers if the device is held in an RF-shielded Faraday bag without owner contact, wiping cryptographic keys before chip-off extraction can occur.
        </p>
      </div>

      <!-- 6 -->
      <div class="feature-card">
        <div class="feature-header">
          <span class="feature-tag">DECEPTION</span>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="2"><path d="M18.36 6.64a9 9 0 11-12.73 0M12 2v10"/></svg>
        </div>
        <h4 class="feature-title" data-i18n="f6_title">Fake Power-Down Canvas</h4>
        <p class="feature-desc" data-i18n="f6_desc">
          Renders authentic OEM power-off animations then drops brightness to 0-nit black canvas. Screen taps covertly capture mugshots and transmit GPS pins while appearing completely dead.
        </p>
      </div>
    </div>
  </div>
</section>

<!-- ── ARCHITECTURAL COMPARISON MATRIX ── -->
<section id="comparison" class="section">
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
        <button class="faq-pill active" onclick="setFaqCategory('all')">All Questions (31)</button>
        <button class="faq-pill" onclick="setFaqCategory('general')">General</button>
        <button class="faq-pill" onclick="setFaqCategory('storage')">Zero-Storage</button>
        <button class="faq-pill" onclick="setFaqCategory('knox')">Device Owner</button>
        <button class="faq-pill" onclick="setFaqCategory('sim')">SIM Defense</button>
        <button class="faq-pill" onclick="setFaqCategory('edl')">Hardware &amp; EDL</button>
        <button class="faq-pill" onclick="setFaqCategory('battery')">Battery</button>
        <button class="faq-pill" onclick="setFaqCategory('legal')">Police &amp; Legal</button>
      </div>
    </div>

    <div class="faq-list" id="faqAccordion">
      <!-- Injected via JS -->
    </div>
  </div>
</section>

<!-- ── PRICING & LICENSING ── -->
<section id="pricing" class="section">
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

<!-- ── FOOTER ── -->
<footer>
  <div class="wrap">
    <div class="footer-inner">
      <div>
        <div style="font-weight:800;font-size:15px;color:var(--text);margin-bottom:4px;">PASA SENTINEL</div>
        <p style="font-size:12.5px;color:var(--text-faint);max-width:500px;" data-i18n="footer_bio">
          Sovereign Android anti-theft defense &amp; covert intelligence agent. Zero Google Play dependencies, zero cloud media storage, Knox-grade uninstallation lockout. Engineered by Izhaan Intellect.
        </p>
      </div>
      <div class="footer-links">
        <a href="#pillars" data-i18n="nav_pillars">Architecture</a>
        <a href="#features" data-i18n="nav_features">Capabilities</a>
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
    <div style="font-family:var(--mono);font-size:11px;color:var(--text-faint);display:flex;justify-content:space-between;align-items:center;">
      <span>LIVE ASSISTANCE</span>
      <span onclick="toggleConcierge()" style="cursor:pointer;font-size:16px;">&times;</span>
    </div>
    <a href="https://wa.me/8801762033445" target="_blank" rel="noopener" class="concierge-link wa">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91c0 1.87.52 3.63 1.42 5.14L2 22l5.09-1.53a9.87 9.87 0 004.95 1.32h.01c5.46 0 9.9-4.45 9.9-9.9C21.95 6.45 17.5 2 12.04 2z"/></svg>
      <span>WhatsApp Concierge</span>
    </a>
    <a href="https://t.me/Pas_agent_bot" target="_blank" rel="noopener" class="concierge-link tg">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0z"/></svg>
      <span>Telegram Official Bot</span>
    </a>
  </div>
  <div class="concierge-pill" onclick="toggleConcierge()">
    <span class="pulse-dot" style="background:#25D366;box-shadow:0 0 8px #25D366;"></span>
    <span data-i18n="floating_help">Live Assistance</span>
  </div>
</div>

<!-- ── CLIENT SCRIPT & BILINGUAL LOGIC ── -->
<script>
const rawFaq = ${JSON.stringify(faqData)};

const translations = {
  en: {
    top_status: "PASA SENTINEL ONLINE // v3.5.1 (BUILD 47)",
    nav_pillars: "Architecture",
    nav_features: "Capabilities",
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

    pillars_tag: "FOUNDATIONAL SECURITY PILLARS",
    pillars_title: "Engineered Without Google Play Compromises",
    pillars_lede: "Standard Play Store apps are restricted by third-party sandbox policies. PASA Sentinel operates at the Device Owner supervisor layer with zero external dependencies.",
    p1_title: "Knox Device Owner Permanence",
    p1_desc: "Configured as system supervisor. Android itself grays out the 'Uninstall' and 'Force Stop' buttons. Blocks Safe Mode booting, factory resets, developer options, and notification shade pulldown.",
    p2_title: "Strategy 1: 100% Zero-Storage",
    p2_desc: "Zero photos, zero GPS tracks, and zero audio recordings are stored on our servers or cloud databases. Evidence streams directly to your private Telegram bot and is shredded from device RAM immediately.",
    p3_title: "Air-Gapped Cellular SMS C2",
    p3_desc: "Maintains full remote control even when mobile data, Wi-Fi, and location are shut off. Authenticated via RFC 6238 TOTP tokens or master PIN, returning live GPS and status pins via cellular SMS.",

    feat_tag: "HARDWARE DEFENSE CAPABILITIES",
    feat_title: "Authentic Anti-Theft Countermeasures",
    feat_lede: "Battle-tested mechanisms built specifically to counteract real-world criminal tactics.",
    f1_title: "Zero-Blackout Headless Camera",
    f1_desc: "Captures high-resolution front/rear photos and 16kHz ambient audio recordings silently without screen flash, shutter audio, or camera preview popups.",
    f2_title: "Hardware Escrow Token PIN Reset",
    f2_desc: "On Android 14, 15, and 16, Google removed standard password reset APIs. PASA uses Knox cryptographic Escrow Tokens to remotely change lockscreen PINs over the air.",
    f3_title: "SIM Eject Lockdown & Caller ID Trap",
    f3_desc: "Instantly locks down the device if the owner SIM is ejected. When the thief inserts their own SIM, PASA secretly dispatches an SMS exposing the thief's phone number via Caller ID.",
    f4_title: "Hardware USB Pin Killswitch",
    f4_desc: "Android 12+ physical USB data severing (setUsbDataSignalingEnabled(false)) blocks Cellebrite, GrayKey forensic extraction boxes, and EDL flashing while allowing AC power charging.",
    f5_title: "Anti-EDL Dead Man's Switch",
    f5_desc: "An autonomous local countdown timer triggers if the device is held in an RF-shielded Faraday bag without owner contact, wiping cryptographic keys before chip-off extraction can occur.",
    f6_title: "Fake Power-Down Canvas",
    f6_desc: "Renders authentic OEM power-off animations then drops brightness to 0-nit black canvas. Screen taps covertly capture mugshots and transmit GPS pins while appearing completely dead.",

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
    nav_pillars: "আর্কিটেকচার",
    nav_features: "ফিচারসমূহ",
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

    pillars_tag: "মৌলিক নিরাপত্তা স্তম্ভ",
    pillars_title: "গুগল প্লে স্টোরের দুর্বলতা ছাড়াই নির্মিত",
    pillars_lede: "প্লে স্টোরের সাধারণ অ্যাপগুলো স্যান্ডবক্স পলিসিতে বন্দি। পাসা সেন্টিনেল সরাসরি অ্যান্ড্রয়েডের ডিভাইস ওনার সুপারভাইজার লেয়ারে কাজ করে।",
    p1_title: "নক্স ডিভাইস ওনার সুরক্ষা",
    p1_desc: "সিস্টেম সুপারভাইজার হিসেবে নিযুক্ত। অ্যান্ড্রয়েড নিজেই আনইন্সটল ও ফোর্স স্টপ বাটন নিষ্ক্রিয় করে দেয়। সেফ মোড বুট, ফ্যাক্টরি রিসেট ও নোটিফিকেশন ড্রয়ার সম্পূর্ণ লক থাকে।",
    p2_title: "স্ট্র্যাটেজি ১: ১০০% জিরো-স্টোরেজ",
    p2_desc: "আমাদের সার্ভার বা কোনো ক্লাউড ডাটাবেজে ব্যবহারকারীর একটি ছবি, জিপিএস ট্র্যাক বা অডিও ফাইলও সংরক্ষণ করা হয় না। সমস্ত প্রমাণ সরাসরি আপনার নিজস্ব টেলিগ্রাম বটে চলে যায়।",
    p3_title: "এয়ার-গ্যাপড সেলুলার এসএমএস সি২",
    p3_desc: "মোবাইল ডাটা, ওয়াইফাই এবং লোকেশন বন্ধ থাকলেও সেলুলার নেটওয়ার্কের মাধ্যমে ফোন সম্পূর্ণ নিয়ন্ত্রণে থাকে। ক্রিপ্টোগ্রাফিক TOTP ও মাস্টার পিন দিয়ে এসএমএস কমান্ড পরিচালিত হয়।",

    feat_tag: "হার্ডওয়্যার ডিফেন্স ক্ষমতা",
    feat_title: "বাস্তবধর্মী অ্যান্টি-থেফট প্রতিরক্ষা ব্যবস্থা",
    feat_lede: "পেশাদার চোরের বাস্তব কৌশল প্রতিহত করার জন্য বিশেষভাবে তৈরি হার্ডওয়্যার-লেভেল ব্যবস্থা।",
    f1_title: "জিরো-ব্ল্যাকআউট হেডলেস ক্যামেরা",
    f1_desc: "ডিসপ্লে না জ্বালিয়ে, কোনো সাউন্ড বা প্রিভিউ পপআপ ছাড়াই সাইলেন্টলি চোরের ফ্রন্ট ও ব্যাক ক্যামেরার ছবি এবং পারিপার্শ্বিক অডিও সরাসরি টেলিগ্রামে পাঠায়।",
    f2_title: "হার্ডওয়্যার এসক্রো টোকেন পিন রিসেট",
    f2_desc: "অ্যান্ড্রয়েড ১৪, ১৫ ও ১৬-তে গুগল পাসওয়ার্ড রিসেট এপিআই বন্ধ করে দিয়েছে। পাসা নক্স ক্রিপ্টোগ্রাফিক এসক্রো টোকেনের মাধ্যমে দূর থেকেই স্ক্রিন লক পিন পরিবর্তন করতে পারে।",
    f3_title: "সিম ইজেক্ট লকডাউন ও কলার আইডি ট্র্যাপ",
    f3_desc: "সিম কার্ড খোলার সাথে সাথে ফোন লক হয়ে যায়। চোর যখন তার নিজের সিম কার্ড ফোনে ঢোকায়, পাসা গোপনে একটি এসএমএস পাঠিয়ে কলার আইডির মাধ্যমে চোরের আসল ফোন নম্বর ফাঁস করে দেয়।",
    f4_title: "হার্ডওয়্যার ইউএসবি ডেটা পিন কিলসুইচ",
    f4_desc: "অ্যান্ড্রয়েড ১২+ ফিজিক্যাল ইউএসবি ডেটা পিন বন্ধ করে দেয় (setUsbDataSignalingEnabled(false))। ফলে চার্জিং ঠিক রেখেও সেলিব্রাইট ও ফ্ল্যাশিং বক্স সম্পূর্ণ অকেজো হয়ে পড়ে।",
    f5_title: "অ্যান্টি-ইডিএল ডেড ম্যান সুইচ",
    f5_desc: "ফোনকে ফ্যারাডে ব্যাগে বা সিগন্যাল-রোধী খাঁচায় আটকে রাখলে নির্দিষ্ট সময় পর পাসা স্বয়ংক্রিয়ভাবে ক্রিপ্টোগ্রাফিক চাবিগুলো মুছে ফেলে হার্ডওয়্যার ডেটা সুরক্ষিত রাখে।",
    f6_title: "ফেক পাওয়ার-ডাউন ক্যানভাস",
    f6_desc: "অফিসিয়াল শাটডাউন অ্যানিমেশন দেখিয়ে স্ক্রিনের ব্রাইটনেস ০-নিট করে দেয়। ফোন সম্পূর্ণ বন্ধ মনে হলেও ব্যাকগ্রাউন্ডে ক্যামেরা ও জিপিএস পুরোদমে সক্রিয় থাকে।",

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

function setLanguage(lang) {
  currentLang = lang;
  document.documentElement.setAttribute('data-lang', lang);
  document.documentElement.lang = lang;

  document.getElementById('btnEn').classList.toggle('active', lang === 'en');
  document.getElementById('btnBn').classList.toggle('active', lang === 'bn');

  const dict = translations[lang];
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (dict[key]) {
      el.textContent = dict[key];
    }
  });

  const searchInput = document.getElementById('faqSearch');
  if (searchInput) {
    searchInput.placeholder = lang === 'bn' 
      ? 'প্রশ্ন খুঁজুন (যেমন: নক্স, ব্যাটারি, পুলিশ, রুট, সিম)...' 
      : 'Search questions (e.g., Knox, battery, police, root, SIM)...';
  }

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

function copyBinanceUid() {
  navigator.clipboard.writeText('756303714').then(() => {
    alert(currentLang === 'bn' ? 'Binance UID: 756303714 কপি করা হয়েছে!' : 'Binance UID: 756303714 copied to clipboard!');
  });
}

function setFaqCategory(cat) {
  currentFaqCategory = cat;
  document.querySelectorAll('.faq-pill').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('onclick').includes("'" + cat + "'"));
  });
  renderFaq();
}

function filterFaq() {
  renderFaq();
}

function renderFaq() {
  const container = document.getElementById('faqAccordion');
  if (!container) return;

  const query = (document.getElementById('faqSearch')?.value || '').toLowerCase().trim();
  const isBn = currentLang === 'bn';

  const filtered = rawFaq.filter(item => {
    if (currentFaqCategory !== 'all' && item.category !== currentFaqCategory) {
      return false;
    }
    if (!query) return true;
    const qText = (isBn ? item.qBn : item.qEn).toLowerCase();
    const aText = (isBn ? item.aBn : item.aEn).toLowerCase();
    return qText.includes(query) || aText.includes(query);
  });

  if (filtered.length === 0) {
    container.innerHTML = \`<div style="text-align:center;padding:30px;color:var(--text-faint);font-size:14px;">
      \${isBn ? 'কোনো ফলাফল পাওয়া যায়নি।' : 'No matching questions found.'}
    </div>\`;
    return;
  }

  container.innerHTML = filtered.map((item, idx) => {
    const q = isBn ? item.qBn : item.qEn;
    const a = isBn ? item.aBn : item.aEn;
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
  item.classList.toggle('open');
  ans.style.display = isOpen ? 'none' : 'block';
}

document.addEventListener('DOMContentLoaded', () => {
  const saved = localStorage.getItem('pasa_lang') || 'en';
  setLanguage(saved);
});
</script>

</body>
</html>`;
}

const html = generateHtml();
fs.writeFileSync(targetHtmlPath, html, 'utf8');
console.log(`Successfully wrote ${html.length} bytes to ${targetHtmlPath}`);
