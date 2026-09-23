// pasa-commercial-web/scripts/build_manual.js
const fs = require('fs');
const path = require('path');

const enMdPath = path.resolve(__dirname, '../../USER_MANUAL_EN.md');
const bnMdPath = path.resolve(__dirname, '../../USER_MANUAL_BN.md');

const enMd = fs.readFileSync(enMdPath, 'utf8');
const bnMd = fs.readFileSync(bnMdPath, 'utf8');

// Also copy markdown files to public directory so they can be downloaded directly
const publicDir = path.resolve(__dirname, '../public');
fs.copyFileSync(enMdPath, path.join(publicDir, 'USER_MANUAL_EN.md'));
fs.copyFileSync(bnMdPath, path.join(publicDir, 'USER_MANUAL_BN.md'));
console.log('Copied raw markdown files to public/');

// Simple, robust Markdown to HTML converter
function mdToHtml(md) {
  const lines = md.split('\n');
  let html = [];
  let inCodeBlock = false;
  let codeLang = '';
  let codeBuffer = [];
  let inTable = false;
  let tableHeaderDone = false;
  let tableRows = [];
  let inList = false;
  let listType = 'ul';

  function closeList() {
    if (inList) {
      html.push(`</${listType}>`);
      inList = false;
    }
  }

  function closeTable() {
    if (inTable) {
      html.push('<div class="table-wrap"><table>');
      html.push(tableRows.join('\n'));
      html.push('</table></div>');
      inTable = false;
      tableHeaderDone = false;
      tableRows = [];
    }
  }

  for (let i = 0; i < lines.length; i++) {
    let line = lines[i];

    // Code blocks
    if (line.trim().startsWith('```')) {
      if (inCodeBlock) {
        closeList();
        closeTable();
        const codeContent = codeBuffer.join('\n')
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;');
        html.push(`<div class="code-block"><div class="code-head"><span>${codeLang || 'COMMAND'}</span><button class="copy-btn" onclick="copyCode(this)">Copy</button></div><pre><code>${codeContent}</code></pre></div>`);
        inCodeBlock = false;
        codeBuffer = [];
        codeLang = '';
      } else {
        closeList();
        closeTable();
        inCodeBlock = true;
        codeLang = line.trim().slice(3).trim();
      }
      continue;
    }

    if (inCodeBlock) {
      codeBuffer.push(line);
      continue;
    }

    // Tables
    if (line.trim().startsWith('|') && line.trim().endsWith('|')) {
      closeList();
      if (!inTable) {
        inTable = true;
        tableRows = [];
        tableHeaderDone = false;
      }

      // Check if separator line
      if (line.includes('---')) {
        tableHeaderDone = true;
        continue;
      }

      const cells = line.split('|').slice(1, -1).map(c => c.trim());
      if (!tableHeaderDone) {
        tableRows.push('<thead><tr>' + cells.map(c => `<th>${formatInline(c)}</th>`).join('') + '</tr></thead><tbody>');
      } else {
        tableRows.push('<tr>' + cells.map(c => `<td>${formatInline(c)}</td>`).join('') + '</tr>');
      }
      continue;
    } else if (inTable) {
      tableRows.push('</tbody>');
      closeTable();
    }

    // Horizontal Rule
    if (line.trim() === '---' || line.trim() === '***') {
      closeList();
      closeTable();
      html.push('<hr class="divider">');
      continue;
    }

    // Headers
    if (line.startsWith('# ')) {
      closeList();
      closeTable();
      const text = line.slice(2).trim();
      const id = slugify(text);
      html.push(`<h1 id="${id}" class="doc-h1">${formatInline(text)}</h1>`);
      continue;
    }
    if (line.startsWith('## ')) {
      closeList();
      closeTable();
      const text = line.slice(3).trim();
      const id = slugify(text);
      html.push(`<h2 id="${id}" class="doc-h2"><span class="h-anchor">#</span>${formatInline(text)}</h2>`);
      continue;
    }
    if (line.startsWith('### ')) {
      closeList();
      closeTable();
      const text = line.slice(4).trim();
      const id = slugify(text);
      html.push(`<h3 id="${id}" class="doc-h3">${formatInline(text)}</h3>`);
      continue;
    }
    if (line.startsWith('#### ')) {
      closeList();
      closeTable();
      const text = line.slice(5).trim();
      const id = slugify(text);
      html.push(`<h4 id="${id}" class="doc-h4">${formatInline(text)}</h4>`);
      continue;
    }

    // Blockquotes
    if (line.startsWith('> ')) {
      closeList();
      closeTable();
      html.push(`<blockquote class="doc-quote">${formatInline(line.slice(2).trim())}</blockquote>`);
      continue;
    }

    // Unordered Lists
    if (line.trim().startsWith('* ') || line.trim().startsWith('- ')) {
      closeTable();
      if (!inList || listType !== 'ul') {
        closeList();
        html.push('<ul class="doc-list">');
        inList = true;
        listType = 'ul';
      }
      const itemText = line.trim().slice(2).trim();
      html.push(`<li>${formatInline(itemText)}</li>`);
      continue;
    }

    // Ordered Lists
    const numMatch = line.trim().match(/^(\d+)\.\s+(.*)$/);
    if (numMatch) {
      closeTable();
      if (!inList || listType !== 'ol') {
        closeList();
        html.push('<ol class="doc-list-num">');
        inList = true;
        listType = 'ol';
      }
      html.push(`<li>${formatInline(numMatch[2].trim())}</li>`);
      continue;
    }

    // Empty lines
    if (!line.trim()) {
      closeList();
      closeTable();
      continue;
    }

    // Paragraph
    closeList();
    closeTable();
    html.push(`<p class="doc-p">${formatInline(line.trim())}</p>`);
  }

  closeList();
  if (inTable) {
    tableRows.push('</tbody>');
    closeTable();
  }

  return html.join('\n');
}

function slugify(text) {
  return text.toLowerCase()
    .replace(/[^\w\u0980-\u09FF\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-');
}

function formatInline(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    // bold
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // italic
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    // inline code
    .replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
    // links
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" class="doc-link" target="_blank" rel="noopener">$1</a>');
}

const enHtml = mdToHtml(enMd);
const bnHtml = mdToHtml(bnMd);

console.log('Converted EN markdown to HTML (' + enHtml.length + ' chars)');
console.log('Converted BN markdown to HTML (' + bnHtml.length + ' chars)');

// Extract TOC from headings
function extractToc(md) {
  const lines = md.split('\n');
  const toc = [];
  for (let line of lines) {
    if (line.startsWith('## ')) {
      const title = line.slice(3).trim();
      toc.push({ level: 2, title, id: slugify(title) });
    } else if (line.startsWith('### ')) {
      const title = line.slice(4).trim();
      toc.push({ level: 3, title, id: slugify(title) });
    }
  }
  return toc;
}

const enToc = extractToc(enMd);
const bnToc = extractToc(bnMd);

const template = `<!DOCTYPE html>
<html lang="en" data-lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PASA Sentinel — Official User Manual &amp; Operational Field Guide</title>
<meta name="description" content="Comprehensive technical user manual and operational field guide for PASA Sentinel (Private Android Security Agent) v3.5.5 in English and Bengali.">
<meta name="theme-color" content="#05070d">
<link rel="icon" type="image/png" href="/assets/img/logo.png">
<link rel="apple-touch-icon" href="/assets/img/logo.png">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Chakra+Petch:wght@400;500;600;700&family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500;600;700&family=Tiro+Bangla:ital@0;1&display=swap" rel="stylesheet">
<style>
:root{
  --bg:#05070d;
  --bg-2:#080b13;
  --panel:#0a0e18;
  --panel-2:#0d1220;
  --panel-3:#111827;

  --line:rgba(255,255,255,.06);
  --line-2:rgba(255,255,255,.12);
  --line-3:rgba(255,255,255,.24);

  --red:#ff2e55;
  --red-2:#ff6b83;
  --cyan:#29e0ff;
  --cyan-2:#7ceeff;
  --green:#22e07a;
  --amber:#ffb020;

  --txt:#eaf0f8;
  --txt-2:#96a1b8;
  --txt-3:#5a657d;

  --font:'Inter',-apple-system,sans-serif;
  --display:'Chakra Petch','Inter',sans-serif;
  --mono:'JetBrains Mono',monospace;
  --font-bn:'Tiro Bangla','Inter',serif;

  --sidebar-w:300px;
}

[data-lang="bn"] body{font-family:var(--font-bn)}
[data-lang="bn"] h1,[data-lang="bn"] h2,[data-lang="bn"] h3,[data-lang="bn"] h4{font-family:var(--font-bn);letter-spacing:0}

*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
html{scroll-behavior:smooth;scroll-padding-top:80px}
body{
  font-family:var(--font);
  background:var(--bg);
  color:var(--txt);
  font-size:15px;
  line-height:1.7;
  overflow-x:hidden;
}

/* Ambient background */
.bg-fx{position:fixed;inset:0;z-index:0;pointer-events:none}
.bg-fx::before{
  content:'';position:absolute;inset:0;
  background:radial-gradient(900px 500px at 15% -5%,rgba(255,46,85,.07),transparent 60%),
             radial-gradient(900px 600px at 95% 8%,rgba(41,224,255,.06),transparent 60%);
}

/* Top Navigation Bar */
.top-bar{
  position:sticky;top:0;z-index:100;
  background:rgba(5,7,13,.88);backdrop-filter:blur(16px);-webkit-backdrop-filter:blur(16px);
  border-bottom:1px solid var(--line-2);
  height:64px;display:flex;align-items:center;justify-content:space-between;
  padding:0 24px;
}
.brand{
  display:flex;align-items:center;gap:12px;text-decoration:none;color:var(--txt);
}
.brand img{width:32px;height:32px;border-radius:6px}
.brand-title{
  font-family:var(--display);font-size:15px;font-weight:700;letter-spacing:.06em;
  display:flex;flex-direction:column;
}
.brand-title span{font-size:10px;color:var(--cyan);font-weight:600;letter-spacing:.14em}

.top-actions{display:flex;align-items:center;gap:12px}
.btn{
  display:inline-flex;align-items:center;gap:6px;
  padding:7px 14px;border-radius:6px;font-size:12.5px;font-weight:600;
  text-decoration:none;transition:all .15s ease;cursor:pointer;border:none;
}
.btn-outline{
  background:transparent;border:1px solid var(--line-3);color:var(--txt-2);
}
.btn-outline:hover{border-color:var(--cyan);color:#fff;background:rgba(41,224,255,.05)}
.btn-red{
  background:var(--red);color:#fff;
}
.btn-red:hover{background:#ff476a}

.lang-switch{
  display:inline-flex;background:var(--panel-2);border:1px solid var(--line-2);border-radius:6px;padding:2px;
}
.lang-btn{
  padding:4px 10px;font-size:11.5px;font-weight:700;background:transparent;border:none;
  color:var(--txt-3);border-radius:4px;cursor:pointer;transition:all .15s ease;
}
.lang-btn.active{background:var(--cyan);color:#05070d}

/* Layout */
.manual-wrapper{
  display:flex;max-width:1440px;margin:0 auto;position:relative;z-index:1;min-height:calc(100vh - 64px);
}

/* Sidebar TOC */
.sidebar{
  width:var(--sidebar-w);flex-shrink:0;position:sticky;top:64px;height:calc(100vh - 64px);
  overflow-y:auto;padding:24px 16px;border-right:1px solid var(--line);
  scrollbar-width:thin;scrollbar-color:var(--line-2) transparent;
}
.sidebar-title{
  font-family:var(--display);font-size:11px;font-weight:700;letter-spacing:.14em;
  color:var(--txt-3);margin-bottom:12px;text-transform:uppercase;display:flex;align-items:center;gap:6px;
}
.sidebar-search{
  margin-bottom:16px;position:relative;
}
.sidebar-search input{
  width:100%;padding:8px 12px 8px 30px;background:var(--panel);border:1px solid var(--line-2);
  border-radius:6px;color:var(--txt);font-size:12.5px;font-family:inherit;
}
.sidebar-search input:focus{outline:none;border-color:var(--cyan)}
.sidebar-search svg{position:absolute;left:10px;top:50%;transform:translateY(-50%);color:var(--txt-3)}

.toc-nav{display:flex;flex-direction:column;gap:3px}
.toc-link{
  display:block;padding:6px 10px;border-radius:4px;font-size:13px;color:var(--txt-2);
  text-decoration:none;line-height:1.4;transition:all .12s ease;
}
.toc-link.lvl-3{padding-left:22px;font-size:12px;color:var(--txt-3)}
.toc-link:hover{color:var(--cyan);background:rgba(41,224,255,.04)}
.toc-link.active{color:#fff;background:rgba(41,224,255,.1);border-left:2px solid var(--cyan)}

/* Content Area */
.content-area{
  flex:1;min-width:0;padding:40px 48px 100px;
}
.content-container{max-width:920px;margin:0 auto}

/* Markdown Rendered Typography */
.doc-h1{
  font-family:var(--display);font-size:28px;font-weight:700;letter-spacing:.03em;
  color:#fff;margin:0 0 12px;line-height:1.3;
}
.doc-h2{
  font-family:var(--display);font-size:20px;font-weight:700;color:var(--cyan);
  margin:48px 0 16px;padding-top:20px;border-top:1px solid var(--line);
  display:flex;align-items:center;gap:8px;
}
.doc-h2 .h-anchor{color:var(--txt-3);font-size:16px;font-weight:400}
.doc-h3{
  font-family:var(--display);font-size:16px;font-weight:600;color:var(--txt);
  margin:28px 0 12px;
}
.doc-h4{
  font-size:14.5px;font-weight:600;color:var(--txt-2);margin:20px 0 8px;
}
.doc-p{margin-bottom:16px;color:var(--txt-2);font-size:14.5px}
.doc-p strong{color:#fff}
.doc-quote{
  margin:20px 0;padding:12px 18px;border-left:3px solid var(--amber);
  background:rgba(255,176,32,.04);color:#ffd480;border-radius:0 6px 6px 0;
  font-size:13.5px;
}
.divider{border:0;height:1px;background:var(--line-2);margin:36px 0}

/* Lists */
.doc-list,.doc-list-num{margin:0 0 18px 24px;color:var(--txt-2);font-size:14.5px}
.doc-list li,.doc-list-num li{margin-bottom:6px}
.doc-list li strong,.doc-list-num li strong{color:#fff}

/* Inline Code & Links */
.inline-code{
  background:rgba(255,255,255,.08);color:var(--cyan-2);font-family:var(--mono);
  font-size:13px;padding:2px 6px;border-radius:4px;border:1px solid var(--line-2);
}
.doc-link{color:var(--cyan);text-decoration:none;border-bottom:1px dashed rgba(41,224,255,.4)}
.doc-link:hover{border-bottom-style:solid}

/* Code Blocks */
.code-block{
  margin:20px 0;background:#030509;border:1px solid var(--line-2);border-radius:8px;
  overflow:hidden;box-shadow:0 8px 24px rgba(0,0,0,.4);
}
.code-head{
  display:flex;align-items:center;justify-content:space-between;
  padding:8px 14px;background:var(--panel-2);border-bottom:1px solid var(--line);
  font-family:var(--mono);font-size:11px;color:var(--txt-3);text-transform:uppercase;
  letter-spacing:.08em;
}
.copy-btn{
  background:transparent;border:1px solid var(--line-3);color:var(--txt-2);
  padding:3px 8px;border-radius:4px;font-size:10.5px;cursor:pointer;
  transition:all .15s ease;font-family:inherit;
}
.copy-btn:hover{background:var(--cyan);color:#05070d;border-color:var(--cyan)}
.code-block pre{
  padding:16px;overflow-x:auto;font-family:var(--mono);font-size:13px;
  line-height:1.6;color:#a8b4cc;
}

/* Tables */
.table-wrap{
  margin:24px 0;overflow-x:auto;border:1px solid var(--line-2);border-radius:8px;
  background:var(--panel);
}
table{width:100%;border-collapse:collapse;font-size:13px;text-align:left}
thead th{
  background:var(--panel-2);color:#fff;padding:12px 14px;font-weight:600;
  border-bottom:1px solid var(--line-2);font-family:var(--display);letter-spacing:.04em;
}
tbody td{
  padding:10px 14px;border-bottom:1px solid var(--line);color:var(--txt-2);
}
tbody tr:last-child td{border-bottom:none}
tbody tr:hover td{background:rgba(255,255,255,.02);color:#fff}

/* Floating Action Button (Scroll to top) */
.scroll-top{
  position:fixed;bottom:24px;right:24px;width:40px;height:40px;border-radius:50%;
  background:var(--panel-2);border:1px solid var(--line-3);color:var(--txt);
  display:flex;align-items:center;justify-content:center;cursor:pointer;
  opacity:0;pointer-events:none;transition:all .2s ease;z-index:90;
}
.scroll-top.show{opacity:1;pointer-events:auto}
.scroll-top:hover{background:var(--cyan);color:#05070d}

/* Mobile Responsiveness */
@media(max-width:960px){
  .sidebar{display:none}
  .content-area{padding:24px 16px 80px}
  .doc-h1{font-size:22px}
  .doc-h2{font-size:18px}
  .top-bar{padding:0 16px}
  .btn-download-text{display:none}
}
</style>
</head>
<body>

<div class="bg-fx"></div>

<!-- Top Navigation -->
<header class="top-bar">
  <a href="/" class="brand">
    <img src="/assets/img/logo.png" alt="PASA">
    <div class="brand-title">
      PASA SENTINEL
      <span id="headerSub">FIELD MANUAL // v3.5.5</span>
    </div>
  </a>

  <div class="top-actions">
    <a href="/" class="btn btn-outline">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
      <span>Console</span>
    </a>
    <a href="/releases/pasa-latest.apk" class="btn btn-red">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>
      <span class="btn-download-text">Download APK</span>
    </a>
    <div class="lang-switch">
      <button class="lang-btn active" id="btnEn" onclick="switchLang('en')">EN</button>
      <button class="lang-btn" id="btnBn" onclick="switchLang('bn')">বাং</button>
    </div>
  </div>
</header>

<div class="manual-wrapper">
  <!-- Sidebar Navigation -->
  <aside class="sidebar">
    <div class="sidebar-title">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
      <span id="tocTitle">Table of Contents</span>
    </div>
    <div class="sidebar-search">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
      <input type="text" id="searchInput" placeholder="Filter sections..." oninput="filterToc()">
    </div>
    <nav class="toc-nav" id="tocContainer"></nav>

    <div style="margin-top:24px;padding-top:16px;border-top:1px solid var(--line);font-size:11.5px;color:var(--txt-3)">
      <div style="font-weight:600;color:var(--txt-2);margin-bottom:6px">Verification & Audit:</div>
      <div>• <a href="/AUDIT_TRAIL.md" target="_blank" class="doc-link">AUDIT_TRAIL.md</a></div>
      <div>• <a href="https://www.virustotal.com/gui/file/ddec8d582f8d691dc830d08caa898e15fea8c26ba26131c1aa2e82fbd6db5e92" target="_blank" rel="noopener" class="doc-link" style="color:var(--green);font-weight:600">VirusTotal Clean (70+ AV) ↗</a></div>
      <div style="margin-top:10px;font-weight:600;color:var(--txt-2);margin-bottom:6px">Raw Documents:</div>
      <div>• <a href="/USER_MANUAL_EN.md" target="_blank" class="doc-link">USER_MANUAL_EN.md</a></div>
      <div>• <a href="/USER_MANUAL_BN.md" target="_blank" class="doc-link">USER_MANUAL_BN.md</a></div>
    </div>
  </aside>

  <!-- Main Content -->
  <main class="content-area">
    <div class="content-container">
      <div id="contentEn" style="display:block">
        ${enHtml}
      </div>
      <div id="contentBn" style="display:none">
        ${bnHtml}
      </div>
    </div>
  </main>
</div>

<button class="scroll-top" id="scrollTopBtn" onclick="window.scrollTo({top:0,behavior:'smooth'})">
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 15l-6-6-6 6"/></svg>
</button>

<script>
const enTocData = ${JSON.stringify(enToc)};
const bnTocData = ${JSON.stringify(bnToc)};
let currentLang = 'en';

function renderToc() {
  const container = document.getElementById('tocContainer');
  const data = currentLang === 'en' ? enTocData : bnTocData;
  container.innerHTML = data.map(item => \`
    <a href="#\${item.id}" class="toc-link lvl-\${item.level}" data-id="\${item.id}">\${item.title}</a>
  \`).join('');
}

function switchLang(lang) {
  currentLang = lang;
  document.documentElement.setAttribute('data-lang', lang);
  document.getElementById('btnEn').classList.toggle('active', lang === 'en');
  document.getElementById('btnBn').classList.toggle('active', lang === 'bn');
  document.getElementById('contentEn').style.display = lang === 'en' ? 'block' : 'none';
  document.getElementById('contentBn').style.display = lang === 'bn' ? 'block' : 'none';
  document.getElementById('tocTitle').innerText = lang === 'en' ? 'Table of Contents' : 'ম্যানুয়াল সূচিপত্র';
  document.getElementById('searchInput').placeholder = lang === 'en' ? 'Filter sections...' : 'বিষয়বস্তু খুঁজুন...';
  document.getElementById('headerSub').innerText = lang === 'en' ? 'FIELD MANUAL // v3.5.5' : 'ফিল্ড ম্যানুয়াল // v৩.৫.৫';
  renderToc();
}

function copyCode(btn) {
  const code = btn.closest('.code-block').querySelector('code').innerText;
  navigator.clipboard.writeText(code).then(() => {
    const orig = btn.innerText;
    btn.innerText = 'Copied!';
    setTimeout(() => { btn.innerText = orig; }, 1800);
  });
}

function filterToc() {
  const query = document.getElementById('searchInput').value.toLowerCase();
  document.querySelectorAll('.toc-link').forEach(link => {
    link.style.display = link.innerText.toLowerCase().includes(query) ? 'block' : 'none';
  });
}

// Scroll spy & Scroll-to-top button
window.addEventListener('scroll', () => {
  const topBtn = document.getElementById('scrollTopBtn');
  if (window.scrollY > 400) {
    topBtn.classList.add('show');
  } else {
    topBtn.classList.remove('show');
  }

  // Active heading spy
  const headings = document.querySelectorAll(currentLang === 'en' ? '#contentEn h2, #contentEn h3' : '#contentBn h2, #contentBn h3');
  let activeId = '';
  headings.forEach(h => {
    const rect = h.getBoundingClientRect();
    if (rect.top <= 120) {
      activeId = h.id;
    }
  });
  if (activeId) {
    document.querySelectorAll('.toc-link').forEach(link => {
      link.classList.toggle('active', link.getAttribute('data-id') === activeId);
    });
  }
});

// Initialize TOC
renderToc();
</script>

</body>
</html>`;

const outPath = path.join(publicDir, 'manual.html');
fs.writeFileSync(outPath, template, 'utf8');
console.log('Successfully generated: ' + outPath + ' (' + (fs.statSync(outPath).size / 1024).toFixed(1) + ' KB)');
