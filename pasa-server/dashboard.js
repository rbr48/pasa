function renderDashboard() {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>PASA Sentinel Web Control Dashboard</title>
  
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;700&family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
  
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin=""/>
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>

  <style>
    :root {
      --bg: #070a12;
      --card-bg: rgba(16, 24, 40, 0.75);
      --card-border: rgba(30, 58, 110, 0.45);
      --primary: #00d4ff;
      --primary-glow: rgba(0, 212, 255, 0.25);
      --emerald: #10b981;
      --amber: #f59e0b;
      --rose: #f43f5e;
      --text: #f1f5f9;
      --text-muted: #94a3b8;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      background-color: var(--bg);
      color: var(--text);
      font-family: 'Plus Jakarta Sans', sans-serif;
      min-height: 100vh;
      overflow-x: hidden;
    }

    /* Modal styles */
    #login-modal {
      position: fixed;
      top: 0; left: 0; width: 100vw; height: 100vh;
      background: rgba(7, 10, 18, 0.9);
      backdrop-filter: blur(8px);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 9999;
    }
    
    .modal-content {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 40px;
      width: 100%;
      max-width: 400px;
      text-align: center;
      box-shadow: 0 0 40px var(--primary-glow);
    }

    .modal-content h2 {
      margin-bottom: 8px;
      color: var(--primary);
    }
    
    .modal-content p {
      color: var(--text-muted);
      margin-bottom: 24px;
      font-size: 0.9rem;
    }

    .input-group {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    input[type="password"], input[type="text"] {
      width: 100%;
      padding: 12px 16px;
      background: rgba(0, 0, 0, 0.5);
      border: 1px solid var(--card-border);
      border-radius: 8px;
      color: var(--text);
      font-family: 'JetBrains Mono', monospace;
      outline: none;
      transition: all 0.2s;
    }

    input:focus {
      border-color: var(--primary);
      box-shadow: 0 0 0 2px var(--primary-glow);
    }

    button {
      padding: 12px 24px;
      background: var(--primary);
      color: #000;
      border: none;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      font-family: 'Plus Jakarta Sans', sans-serif;
    }

    button:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px var(--primary-glow);
    }
    
    button:active {
      transform: translateY(0);
    }

    .btn-secondary {
      background: rgba(255, 255, 255, 0.1);
      color: var(--text);
    }
    .btn-secondary:hover {
      background: rgba(255, 255, 255, 0.2);
      box-shadow: none;
    }

    .shake {
      animation: shake 0.5s;
    }
    @keyframes shake {
      0%, 100% { transform: translateX(0); }
      10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
      20%, 40%, 60%, 80% { transform: translateX(5px); }
    }
    
    #login-error {
      color: var(--rose);
      margin-top: 12px;
      font-size: 0.85rem;
      display: none;
    }

    /* Dashboard Layout */
    #dashboard {
      display: none;
      padding: 24px;
      max-width: 1600px;
      margin: 0 auto;
    }

    /* Header */
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 32px;
      padding: 16px 24px;
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      backdrop-filter: blur(10px);
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .logo {
      width: 48px;
      height: 48px;
      background: linear-gradient(135deg, var(--primary), #0056b3);
      border-radius: 12px;
      display: flex;
      justify-content: center;
      align-items: center;
      font-weight: 700;
      font-size: 24px;
      color: #fff;
      text-shadow: 0 2px 4px rgba(0,0,0,0.3);
    }

    .header-titles h1 {
      font-size: 1.5rem;
      letter-spacing: -0.5px;
    }

    .header-titles p {
      color: var(--text-muted);
      font-size: 0.85rem;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .connection-badge {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px 16px;
      background: rgba(0,0,0,0.3);
      border-radius: 20px;
      border: 1px solid var(--card-border);
      font-size: 0.9rem;
      font-weight: 500;
    }

    .status-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: var(--rose);
    }
    
    .status-dot.connected {
      background: var(--emerald);
      box-shadow: 0 0 10px var(--emerald);
      animation: pulse 2s infinite;
    }

    @keyframes pulse {
      0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); }
      70% { box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
      100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 24px;
    }
    
    .fleet-stats {
      font-size: 0.9rem;
      color: var(--text-muted);
    }
    .fleet-stats span {
      color: var(--text);
      font-weight: 600;
    }

    /* Stats Row */
    .stats-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 24px;
      margin-bottom: 32px;
    }

    .stat-card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 12px;
      padding: 24px;
      display: flex;
      align-items: center;
      gap: 16px;
      transition: transform 0.2s;
    }
    
    .stat-card:hover {
      transform: translateY(-2px);
    }

    .stat-icon {
      font-size: 2rem;
      width: 56px;
      height: 56px;
      display: flex;
      justify-content: center;
      align-items: center;
      background: rgba(0,0,0,0.3);
      border-radius: 12px;
    }

    .stat-info h3 {
      font-size: 0.85rem;
      color: var(--text-muted);
      margin-bottom: 4px;
    }

    .stat-info .value {
      font-size: 1.5rem;
      font-weight: 700;
      font-family: 'JetBrains Mono', monospace;
    }

    /* Main Grid */
    .main-grid {
      display: grid;
      grid-template-columns: 65% 1fr;
      gap: 24px;
    }

    @media (max-width: 1024px) {
      .main-grid {
        grid-template-columns: 1fr;
      }
    }

    .panel {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 16px;
      padding: 24px;
      backdrop-filter: blur(10px);
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .panel-header {
      font-size: 1.1rem;
      font-weight: 600;
      padding-bottom: 12px;
      border-bottom: 1px solid var(--card-border);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    /* Left Column */
    .device-selector {
      width: 100%;
      padding: 12px 16px;
      background: rgba(0, 0, 0, 0.5);
      border: 1px solid var(--card-border);
      border-radius: 8px;
      color: var(--text);
      font-family: 'Plus Jakarta Sans', sans-serif;
      font-size: 1rem;
      outline: none;
      cursor: pointer;
    }
    
    .device-info {
      display: flex;
      gap: 24px;
      background: rgba(0,0,0,0.2);
      padding: 16px;
      border-radius: 12px;
      border: 1px solid rgba(255,255,255,0.05);
    }
    
    .info-item {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .info-item .label {
      font-size: 0.75rem;
      color: var(--text-muted);
      text-transform: uppercase;
    }
    .info-item .val {
      font-weight: 500;
    }

    .action-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 12px;
    }
    
    .action-btn {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--card-border);
      color: var(--text);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 16px;
    }
    
    .action-btn:hover {
      background: rgba(255, 255, 255, 0.1);
      border-color: var(--primary);
    }
    
    .action-btn i {
      font-size: 1.5rem;
      font-style: normal;
    }

    .custom-cmd {
      display: flex;
      gap: 12px;
    }

    .custom-cmd input {
      flex: 1;
    }

    .feed-container {
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-height: 500px;
      overflow-y: auto;
      padding-right: 8px;
    }

    /* Scrollbar */
    ::-webkit-scrollbar { width: 6px; height: 6px; }
    ::-webkit-scrollbar-track { background: transparent; }
    ::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.2); border-radius: 4px; }
    ::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.3); }

    .feed-item {
      background: rgba(0,0,0,0.3);
      border-radius: 8px;
      padding: 12px;
      border-left: 3px solid var(--card-border);
      font-family: 'JetBrains Mono', monospace;
      font-size: 0.85rem;
    }
    
    .feed-item.sent { border-color: var(--primary); }
    .feed-item.received { border-color: var(--emerald); }
    
    .feed-header {
      display: flex;
      justify-content: space-between;
      color: var(--text-muted);
      margin-bottom: 8px;
      font-size: 0.75rem;
    }
    
    .feed-img {
      max-width: 100%;
      border-radius: 4px;
      cursor: pointer;
      margin-top: 8px;
    }

    /* Right Column */
    #map {
      height: 350px;
      border-radius: 12px;
      border: 1px solid var(--card-border);
      background: #0b0f19;
    }

    #map .leaflet-tile-pane {
      filter: invert(100%) hue-rotate(180deg) brightness(95%) contrast(90%);
    }

    #map .leaflet-container {
      background: #0b0f19;
    }

    .audit-log {
      max-height: 350px;
      overflow-y: auto;
    }
    
    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.85rem;
    }
    
    th {
      text-align: left;
      color: var(--text-muted);
      padding: 12px;
      position: sticky;
      top: 0;
      background: var(--card-bg);
      backdrop-filter: blur(5px);
    }
    
    td {
      padding: 12px;
      border-top: 1px solid rgba(255,255,255,0.05);
    }
    
    .badge {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 0.7rem;
      font-weight: 700;
      letter-spacing: 0.5px;
    }
    .badge.alert { background: rgba(244, 63, 94, 0.2); color: var(--rose); }
    .badge.auth { background: rgba(168, 85, 247, 0.2); color: #c084fc; }
    .badge.command { background: rgba(0, 212, 255, 0.2); color: var(--primary); }
    .badge.register { background: rgba(16, 185, 129, 0.2); color: var(--emerald); }

    /* Lightbox */
    #lightbox {
      display: none;
      position: fixed;
      top: 0; left: 0; width: 100vw; height: 100vh;
      background: rgba(0,0,0,0.9);
      z-index: 10000;
      justify-content: center;
      align-items: center;
      cursor: zoom-out;
    }
    #lightbox img {
      max-width: 90%;
      max-height: 90%;
      border-radius: 8px;
      box-shadow: 0 0 30px rgba(0,0,0,0.5);
    }

    /* Toasts */
    #toast-container {
      position: fixed;
      top: 24px;
      right: 24px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    
    .toast {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      padding: 16px 24px;
      border-radius: 8px;
      backdrop-filter: blur(10px);
      box-shadow: 0 10px 30px rgba(0,0,0,0.5);
      animation: slideIn 0.3s forwards, fadeOut 0.3s 3s forwards;
    }
    .toast.error { border-left: 4px solid var(--rose); }
    .toast.success { border-left: 4px solid var(--emerald); }
    
    @keyframes slideIn {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
    @keyframes fadeOut {
      to { opacity: 0; visibility: hidden; }
    }
  </style>
</head>
<body>

  <!-- Login Modal -->
  <div id="login-modal">
    <div class="modal-content" id="login-box">
      <h2>PASA Authentication</h2>
      <p>Enter administrative access key</p>
      <form id="login-form">
        <div class="input-group">
          <input type="password" id="admin-key" placeholder="Admin Key" autocomplete="off" required>
          <button type="submit">Authenticate</button>
        </div>
        <div id="login-error">Invalid credentials</div>
      </form>
    </div>
  </div>

  <!-- Dashboard -->
  <div id="dashboard">
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <div class="logo">P</div>
        <div class="header-titles">
          <h1>PASA Sentinel</h1>
          <p>Sovereign Control Plane</p>
        </div>
        <div class="connection-badge" id="conn-badge">
          <div class="status-dot" id="conn-dot"></div>
          <span id="conn-text">Disconnected</span>
        </div>
      </div>
      <div class="header-right">
        <div class="fleet-stats">
          Fleet: <span id="online-count">0</span> / <span id="total-count">0</span> Online
        </div>
        <button class="btn-secondary" onclick="logout()">Logout</button>
      </div>
    </header>

    <!-- Stats Row -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon">📱</div>
        <div class="stat-info">
          <h3>Total Devices</h3>
          <div class="value" id="stat-total">0</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="color: var(--emerald);">🟢</div>
        <div class="stat-info">
          <h3>Online Now</h3>
          <div class="value" id="stat-online" style="color: var(--emerald);">0</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">⚡</div>
        <div class="stat-info">
          <h3>Commands Today</h3>
          <div class="value" id="stat-commands">0</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" id="alert-icon">🚨</div>
        <div class="stat-info">
          <h3>Alerts</h3>
          <div class="value" id="stat-alerts">0</div>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="main-grid">
      <!-- Left Column: Device Panel -->
      <div class="panel">
        <div class="panel-header">
          Device Management
        </div>
        
        <select class="device-selector" id="device-select">
          <option value="">Select a device...</option>
        </select>

        <div class="device-info" id="device-info" style="display: none;">
          <div class="info-item">
            <span class="label">Name</span>
            <span class="val" id="info-name">-</span>
          </div>
          <div class="info-item">
            <span class="label">Model</span>
            <span class="val" id="info-model">-</span>
          </div>
          <div class="info-item">
            <span class="label">Battery</span>
            <span class="val" id="info-battery">-</span>
          </div>
          <div class="info-item">
            <span class="label">Last Seen</span>
            <span class="val" id="info-seen">-</span>
          </div>
        </div>

        <div class="action-grid" id="action-grid" style="display: none;">
          <button class="action-btn" onclick="sendQuickAction('/locate')"><i>📍</i>Locate</button>
          <button class="action-btn" onclick="sendQuickAction('/snap')"><i>📸</i>Photo</button>
          <button class="action-btn" onclick="sendQuickAction('/lock')"><i>🔒</i>Lock</button>
          <button class="action-btn" onclick="sendQuickAction('/ring')"><i>🔔</i>Ring</button>
          <button class="action-btn" onclick="sendQuickAction('/screenshot')"><i>📱</i>Screenshot</button>
          <button class="action-btn" onclick="sendQuickAction('/ring_stop')"><i>🛑</i>Siren Stop</button>
        </div>

        <form class="custom-cmd" id="cmd-form" style="display: none;">
          <input type="text" id="cmd-input" placeholder="Enter custom command (e.g. /shell ls)">
          <button type="submit">Send</button>
        </form>

        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 12px;">
          <h3 style="font-size: 0.9rem; color: var(--text-muted);">Response Feed</h3>
          <button class="btn-secondary" style="padding: 4px 12px; font-size: 0.8rem;" onclick="clearFeed()">Clear Feed</button>
        </div>
        <div class="feed-container" id="response-feed">
          <!-- Feed items injected here -->
        </div>
      </div>

      <!-- Right Column: Activity & Map -->
      <div style="display: flex; flex-direction: column; gap: 24px;">
        <div class="panel" style="padding: 16px;">
          <div class="panel-header" style="margin-bottom: 8px;">Location Tracking</div>
          <div id="map"></div>
        </div>

        <div class="panel">
          <div class="panel-header">System Audit Log</div>
          <div class="audit-log">
            <table>
              <thead>
                <tr>
                  <th>Event</th>
                  <th>Detail</th>
                  <th>Device</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody id="audit-tbody">
                <!-- Logs injected here -->
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Photo Lightbox -->
  <div id="lightbox" onclick="hideLightbox()">
    <img id="lightbox-img" src="" alt="Full size photo">
  </div>

  <!-- Toast Container -->
  <div id="toast-container"></div>

  <script>
    // Global State
    let map = null;
    let marker = null;
    let selectedDeviceId = null;
    let refreshInterval = null;

    // Util: Time Ago
    function timeAgo(timestamp) {
      if (!timestamp) return 'Never';
      const seconds = Math.floor((new Date() - new Date(timestamp)) / 1000);
      let interval = seconds / 31536000;
      if (interval > 1) return Math.floor(interval) + "y ago";
      interval = seconds / 2592000;
      if (interval > 1) return Math.floor(interval) + "mo ago";
      interval = seconds / 86400;
      if (interval > 1) return Math.floor(interval) + "d ago";
      interval = seconds / 3600;
      if (interval > 1) return Math.floor(interval) + "h ago";
      interval = seconds / 60;
      if (interval > 1) return Math.floor(interval) + "m ago";
      return Math.floor(seconds) + "s ago";
    }

    // Util: Toast
    function showToast(message, type = 'success') {
      const container = document.getElementById('toast-container');
      const toast = document.createElement('div');
      toast.className = 'toast ' + type;
      toast.innerText = message;
      container.appendChild(toast);
      setTimeout(() => { if (container.contains(toast)) toast.remove(); }, 3500);
    }

    // Init Map
    function initMap() {
      if (map) return;
      map = L.map('map').setView([23.8103, 90.4125], 6);
      // Clean Watermark-Free OpenStreetMap with CSS dark theme filter
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        maxZoom: 19
      }).addTo(map);
    }

    function updateMap(lat, lng) {
      if (!map) initMap();
      const pos = [lat, lng];
      map.setView(pos, 15);
      if (marker) {
        marker.setLatLng(pos);
      } else {
        marker = L.marker(pos).addTo(map);
      }
    }

    // Authentication
    document.getElementById('login-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const key = document.getElementById('admin-key').value;
      const btn = e.target.querySelector('button');
      const errorDiv = document.getElementById('login-error');
      const box = document.getElementById('login-box');
      
      btn.disabled = true;
      btn.innerText = 'Verifying...';
      errorDiv.style.display = 'none';

      try {
        const verifyUrl = new URL('/api/admin/verify', window.location.origin).toString();
        const res = await window.fetch(verifyUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ key })
        });

        if (res.ok) {
          sessionStorage.setItem('pasa_admin_key', key);
          document.getElementById('login-modal').style.display = 'none';
          document.getElementById('dashboard').style.display = 'block';
          initDashboard();
        } else {
          throw new Error('Invalid credentials');
        }
      } catch (err) {
        errorDiv.style.display = 'block';
        box.classList.remove('shake');
        void box.offsetWidth; // trigger reflow
        box.classList.add('shake');
      } finally {
        btn.disabled = false;
        btn.innerText = 'Authenticate';
      }
    });

    function logout() {
      sessionStorage.removeItem('pasa_admin_key');
      if (window._pasaWs) window._pasaWs.close();
      if (refreshInterval) clearInterval(refreshInterval);
      location.reload();
    }

    // Dashboard Initialization
    function initDashboard() {
      initMap();
      fetchDevices();
      fetchLogs();
      connectWebSocket();
      refreshInterval = setInterval(fetchDevices, 10000);
    }

    async function apiFetch(endpoint) {
      try {
        const key = sessionStorage.getItem('pasa_admin_key');
        const url = new URL(endpoint, window.location.origin).toString();
        const res = await window.fetch(url, {
          method: 'GET',
          headers: {
            'Authorization': 'Bearer ' + key,
            'x-pasa-admin-key': key,
            'Accept': 'application/json'
          }
        });
        if (!res.ok) {
          if (res.status === 401 || res.status === 403) logout();
          throw new Error('API Error: ' + res.status);
        }
        return await res.json();
      } catch (err) {
        console.warn('apiFetch warning for ' + endpoint, err);
        throw err;
      }
    }

    // Devices & Stats
    async function fetchDevices() {
      try {
        const data = await apiFetch('/api/admin/devices');
        updateStats(data);
        populateDeviceSelector(data.devices || []);
        
        if (selectedDeviceId) {
          const device = (data.devices || []).find(d => d.id === selectedDeviceId);
          if (device) renderDeviceInfo(device);
        }
      } catch (e) {
        console.error('Fetch devices error:', e);
      }
    }

    function updateStats(data) {
      document.getElementById('stat-total').innerText = data.total || 0;
      document.getElementById('stat-online').innerText = data.online || 0;
      document.getElementById('total-count').innerText = data.total || 0;
      document.getElementById('online-count').innerText = data.online || 0;
      
      const cmds = data.commandsToday || 0;
      document.getElementById('stat-commands').innerText = cmds;
      
      const alerts = data.alerts || 0;
      const alertEl = document.getElementById('stat-alerts');
      const alertIcon = document.getElementById('alert-icon');
      alertEl.innerText = alerts;
      if (alerts > 0) {
        alertEl.style.color = 'var(--rose)';
        alertIcon.style.color = 'var(--rose)';
      } else {
        alertEl.style.color = 'var(--text)';
        alertIcon.style.color = 'var(--text)';
      }
    }

    function populateDeviceSelector(devices) {
      const select = document.getElementById('device-select');
      const currentVal = select.value;
      
      select.innerHTML = '<option value="">Select a device...</option>';

      // Sort online devices (🟢) to the very top, then by most recent lastSeen
      const sorted = [...devices].sort((a, b) => {
        if (a.online && !b.online) return -1;
        if (!a.online && b.online) return 1;
        return (b.lastSeen || 0) - (a.lastSeen || 0);
      });

      sorted.forEach(d => {
        const opt = document.createElement('option');
        opt.value = d.id;
        opt.textContent = \`\${d.name || d.model || 'Unknown'} (\${d.id}) \${d.online ? '🟢' : '⚪'}\`;
        select.appendChild(opt);
      });
      
      if (currentVal && sorted.some(d => d.id === currentVal)) {
        select.value = currentVal;
      } else if (!selectedDeviceId && sorted.length > 0 && sorted[0].online) {
        // Auto-select active online device immediately
        selectedDeviceId = sorted[0].id;
        select.value = selectedDeviceId;
        document.getElementById('device-info').style.display = 'flex';
        document.getElementById('action-grid').style.display = 'grid';
        document.getElementById('cmd-form').style.display = 'flex';
        renderDeviceInfo(sorted[0]);
      }
    }

    document.getElementById('device-select').addEventListener('change', (e) => {
      selectedDeviceId = e.target.value;
      const info = document.getElementById('device-info');
      const actions = document.getElementById('action-grid');
      const form = document.getElementById('cmd-form');
      
      if (selectedDeviceId) {
        info.style.display = 'flex';
        actions.style.display = 'grid';
        form.style.display = 'flex';
        fetchDevices(); // get immediate update for selected
      } else {
        info.style.display = 'none';
        actions.style.display = 'none';
        form.style.display = 'none';
      }
    });

    function renderDeviceInfo(device) {
      document.getElementById('info-name').innerText = device.name || '-';
      document.getElementById('info-model').innerText = device.model || '-';
      document.getElementById('info-battery').innerText = device.battery ? device.battery + '%' : '-';
      document.getElementById('info-seen').innerText = timeAgo(device.lastSeen);
    }

    // Logs
    async function fetchLogs() {
      try {
        const data = await apiFetch('/api/admin/logs');
        const tbody = document.getElementById('audit-tbody');
        tbody.innerHTML = '';
        
        (data.logs || []).forEach(log => {
          const tr = document.createElement('tr');
          const typeCls = (log.type || '').toLowerCase();
          tr.innerHTML = \`
            <td><span class="badge \${typeCls}">\${log.type}</span></td>
            <td>\${log.detail}</td>
            <td style="font-family: 'JetBrains Mono', monospace;">\${log.deviceId || '-'}</td>
            <td>\${timeAgo(log.timestamp)}</td>
          \`;
          tbody.appendChild(tr);
        });
      } catch (e) {
        console.error('Fetch logs error:', e);
      }
    }

    // WebSocket
    function updateConnectionStatus(connected) {
      const dot = document.getElementById('conn-dot');
      const text = document.getElementById('conn-text');
      if (connected) {
        dot.classList.add('connected');
        text.innerText = 'Connected';
      } else {
        dot.classList.remove('connected');
        text.innerText = 'Disconnected';
      }
    }

    function connectWebSocket() {
      const adminKey = sessionStorage.getItem('pasa_admin_key');
      if (!adminKey) return;

      const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
      const ws = new WebSocket(\`\${protocol}//\${location.host}/ws/dashboard?key=\${encodeURIComponent(adminKey)}\`);
      
      ws.onopen = () => {
        updateConnectionStatus(true);
        showToast('Real-time connection established');
      };
      
      ws.onclose = () => {
        updateConnectionStatus(false);
        setTimeout(connectWebSocket, 3000);
      };
      
      ws.onerror = () => {
        // Handle silently, onclose will trigger reconnect
      };
      
      ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data);
          handleWebSocketMessage(msg);
        } catch(e) {
          console.error("WS Parse error", e);
        }
      };
      
      window._pasaWs = ws;
    }

    function handleWebSocketMessage(msg) {
      if (msg.type === 'device_update') {
        fetchDevices(); // trigger refresh
        return;
      }
      if (msg.type === 'new_log') {
        fetchLogs();
        return;
      }

      // If message is for specific device but we have another selected, ignore feed
      if (msg.deviceId && selectedDeviceId && msg.deviceId !== selectedDeviceId) {
        return;
      }

      switch(msg.type) {
        case 'text':
          appendToResponseFeed({ type: 'received', text: msg.text, timestamp: msg.timestamp });
          break;
        case 'photo':
          appendToResponseFeed({ type: 'photo', data: msg.data, timestamp: msg.timestamp });
          break;
        case 'location':
          appendToResponseFeed({ type: 'location', lat: msg.lat, lng: msg.lng, timestamp: msg.timestamp });
          updateMap(msg.lat, msg.lng);
          break;
        case 'error':
          showToast(msg.error || 'Command failed', 'error');
          appendToResponseFeed({ type: 'received', text: 'Error: ' + msg.error, timestamp: msg.timestamp, isError: true });
          break;
      }
    }

    // Commands
    function sendCommand(command, deviceId) {
      const ws = window._pasaWs;
      if (!ws || ws.readyState !== WebSocket.OPEN) {
        showToast('WebSocket not connected', 'error');
        return;
      }
      ws.send(JSON.stringify({
        type: 'command',
        deviceId: deviceId,
        command: command,
        timestamp: Date.now()
      }));
      appendToResponseFeed({ type: 'sent', command, timestamp: Date.now() });
    }

    function sendQuickAction(cmd) {
      if (!selectedDeviceId) return showToast('No device selected', 'error');
      sendCommand(cmd, selectedDeviceId);
    }

    document.getElementById('cmd-form').addEventListener('submit', (e) => {
      e.preventDefault();
      const input = document.getElementById('cmd-input');
      const cmd = input.value.trim();
      if (!cmd || !selectedDeviceId) return;
      
      sendCommand(cmd, selectedDeviceId);
      input.value = '';
    });

    // Feed
    function appendToResponseFeed(item) {
      const feed = document.getElementById('response-feed');
      const div = document.createElement('div');
      div.className = 'feed-item ' + (item.type === 'sent' ? 'sent' : 'received');
      
      let content = '';
      const timeStr = new Date(item.timestamp || Date.now()).toLocaleTimeString();
      
      if (item.type === 'sent') {
        content = \`<div class="feed-header"><span>Command Sent</span><span>\${timeStr}</span></div>
                   <div style="color: var(--primary);">> \${item.command}</div>\`;
      } else if (item.type === 'photo') {
        content = \`<div class="feed-header"><span>Photo Received</span><span>\${timeStr}</span></div>
                   <img src="\${item.data}" class="feed-img" onclick="showLightbox('\${item.data}')" />\`;
      } else if (item.type === 'location') {
        content = \`<div class="feed-header"><span>Location Updated</span><span>\${timeStr}</span></div>
                   <div>📍 Lat: \${item.lat.toFixed(6)}, Lng: \${item.lng.toFixed(6)}</div>\`;
      } else {
        const color = item.isError ? 'var(--rose)' : 'var(--text)';
        content = \`<div class="feed-header"><span>Response</span><span>\${timeStr}</span></div>
                   <div style="white-space: pre-wrap; word-break: break-all; color: \${color};">\${item.text || JSON.stringify(item)}</div>\`;
      }
      
      div.innerHTML = content;
      feed.prepend(div);
    }

    function clearFeed() {
      document.getElementById('response-feed').innerHTML = '';
    }

    // Lightbox
    function showLightbox(src) {
      document.getElementById('lightbox-img').src = src;
      document.getElementById('lightbox').style.display = 'flex';
      
      document.addEventListener('keydown', handleEsc);
    }
    
    function hideLightbox() {
      document.getElementById('lightbox').style.display = 'none';
      document.removeEventListener('keydown', handleEsc);
    }
    
    function handleEsc(e) {
      if (e.key === 'Escape') hideLightbox();
    }

    // Auto-check session on load
    if (sessionStorage.getItem('pasa_admin_key')) {
      document.getElementById('login-modal').style.display = 'none';
      document.getElementById('dashboard').style.display = 'block';
      initDashboard();
    }
  </script>
</body>
</html>`;
}

module.exports = { renderDashboard };
