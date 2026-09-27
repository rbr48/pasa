// Upload full command menu to Telegram Bot API
const token = process.env.TELEGRAM_BOT_TOKEN || process.env.PASA_CENTRAL_BOT_TOKEN || process.env.BOT_TOKEN;
if (!token) {
  console.error('Error: TELEGRAM_BOT_TOKEN environment variable required');
  process.exit(1);
}

const commandsList = [
  { command: "menu", description: "📱 Open interactive touchscreen control panel" },
  { command: "help", description: "📖 Show full help manual & command guide" },
  { command: "status", description: "📊 Live battery, storage, RAM & sensor telemetry" },
  { command: "auth", description: "🔓 Authenticate 15-minute administrative session" },
  { command: "logout", description: "🔒 Lock active administrative session" },
  { command: "selftest", description: "🩺 Run 9-point security, GPS & sensor audit" },
  { command: "info", description: "ℹ️ Hardware specs, SIM details & OS version" },
  { command: "locate", description: "📍 Acquire instant GPS fix & Google Maps pin" },
  { command: "tower", description: "📡 Cell tower triangulation & signal RF telemetry" },
  { command: "sim", description: "📶 Active SIM slots, carrier name & signal RF" },
  { command: "sim_lock", description: "🛡️ SIM swap guard & ICCID whitelist lock" },
  { command: "sim_tray_lock", description: "🔒 Cryptographic SIM tray lock (brick upon swap)" },
  { command: "sendsms", description: "✉️ Send outbound SMS directly via SIM slot" },
  { command: "call", description: "📞 Remotely place outbound cellular phone call" },
  { command: "track", description: "🛰️ Start or stop continuous live GPS tracking" },
  { command: "geofence", description: "🌐 Configure safe zone radius & breach alerts" },
  { command: "snap", description: "📸 Capture stealth photo (front, rear, or both)" },
  { command: "livestream", description: "🔴 Stream near-live camera video to Telegram" },
  { command: "stopstream", description: "⏹️ Stop active camera live stream" },
  { command: "screenshot", description: "📱 Silent full-screen capture via Accessibility" },
  { command: "screen_burst", description: "🎞️ Rapid 5-10 frame montage of intruder activity" },
  { command: "screenrecord", description: "🎥 Covert HD MP4 screen recording (5-60s)" },
  { command: "video", description: "📹 Record stealth camera video (1-60s)" },
  { command: "record", description: "🎙️ Record ambient microphone audio clip" },
  { command: "clipboard", description: "📋 Read current device clipboard text" },
  { command: "lock", description: "🔒 Lock screen with custom PIN & emergency banner" },
  { command: "lock_message", description: "💬 Set urgent alert message on lockscreen" },
  { command: "set_os_pin", description: "🔐 Overwrite hardware OS lockscreen PIN (Device Owner)" },
  { command: "set_master_pin", description: "🔑 Set cryptographic master PIN for remote control" },
  { command: "escrow", description: "🔐 Arm or check Knox hardware escrow password token" },
  { command: "duress_pin", description: "🆘 Set decoy coercion PIN for emergency SOS" },
  { command: "unlock", description: "🔓 Dismiss Lost Mode & unlock device screen" },
  { command: "fakeshutdown", description: "🕶️ Fake shutdown: blackout screen & silent traps" },
  { command: "wake", description: "☀️ Restore device from Fake Shutdown blackout" },
  { command: "ring", description: "🚨 Trigger or stop maximum volume emergency siren" },
  { command: "vibrate_pulse", description: "📳 Locate device silently via tactile vibrations" },
  { command: "trap", description: "🛡️ Arm sensor traps (snatch, charger, pocket)" },
  { command: "pattern_guard", description: "👁️ Failed pattern/PIN intrusion monitor & mugshot" },
  { command: "app_firewall", description: "🧱 Block RAT & spyware network outbound telemetry" },
  { command: "battery_alert", description: "🔋 Monitor abnormal drain & charging disconnects" },
  { command: "a11y_shield", description: "🛡️ Accessibility Trojan shield & auto-defense" },
  { command: "usb_autolock", description: "🔌 Locked-state USB killswitch (Cellebrite blocker)" },
  { command: "anti_2g", description: "📡 Anti-2G / IMSI-Catcher Stingray shield" },
  { command: "clipper_guard", description: "🪙 Crypto address clipboard hijacking trap" },
  { command: "app_install_lock", description: "🚫 Sideload & unauthorized APK install lockdown" },
  { command: "canary_guard", description: "🪤 Ransomware canary tripwire honeypot guard" },
  { command: "otp_guard", description: "🛡️ 2FA / OTP notification interception guard" },
  { command: "deadman", description: "💀 Anti-forensic dead man's switch timer" },
  { command: "thermal", description: "🔥 Thermal anomaly trap (anti-EDL/heat gun)" },
  { command: "harden_boot", description: "🔒 Lock recovery mode & prevent unauthorized reset" },
  { command: "tamper_detect", description: "🔍 Scan for root, debuggers, hooks & emulators" },
  { command: "device_owner", description: "👑 Check Device Owner & Kiosk hardware lock" },
  { command: "antitamper", description: "🛡️ Safe boot, airplane mode & factory reset lock" },
  { command: "usb_lock", description: "🔌 Cut USB data signaling pins (charge only)" },
  { command: "camera_lock", description: "📷 Hardware camera killswitch (anti-spy lockout)" },
  { command: "bluetooth_lock", description: "📡 Hardware Bluetooth & sharing killswitch" },
  { command: "mic_mute", description: "🔇 Hardware master audio mute (HAL level)" },
  { command: "lockscreen_info", description: "📱 Pin contact/recovery info to OS lockscreen" },
  { command: "autolock", description: "⏱️ Enforce screen inactivity autolock timeout" },
  { command: "wifi_connect", description: "📶 Emergency Wi-Fi auto-provisioning while locked" },
  { command: "security_audit", description: "📑 Inspect kernel OS security audit logs" },
  { command: "notification", description: "🔕 Permanent notification drawer suppression" },
  { command: "self_heal", description: "✨ Permanently lock app permissions as managed" },
  { command: "freeze", description: "🧊 Vanish banking & private apps into shadow vault" },
  { command: "unfreeze", description: "🔥 Restore hidden applications to launcher" },
  { command: "frozen", description: "📦 List currently frozen shadow vault apps" },
  { command: "lock_app", description: "🧊 Freeze gallery, phone, files, or sensitive app" },
  { command: "unlock_app", description: "☀️ Restore locked/hidden application" },
  { command: "biometrics", description: "🚫 Biometric coercion killswitch (forces Master PIN)" },
  { command: "dns", description: "🛡️ Enforce system-wide Private DNS-over-TLS" },
  { command: "reboot", description: "🔄 Remotely restart phone hardware (Device Owner)" },
  { command: "stealth", description: "👁️ Hide or restore PASA app icon in launcher" },
  { command: "contacts", description: "👥 Search device address book contacts" },
  { command: "call_log", description: "📞 View incoming and outgoing call history" },
  { command: "sms_log", description: "💬 View recent SMS inbox messages" },
  { command: "history", description: "📜 View recent command audit execution trail" },
  { command: "network", description: "🌐 Current IP, Wi-Fi SSID & cell carrier info" },
  { command: "apps", description: "📦 List installed applications" },
  { command: "app_uninstall", description: "❌ Silently uninstall package (Device Owner)" },
  { command: "gallery_latest", description: "🖼️ Extract recent photos from camera roll" },
  { command: "getfile", description: "📁 Download file from storage directly to Telegram" },
  { command: "list_files", description: "📂 Browse files in device storage directory" },
  { command: "shred", description: "🗑️ Cryptographically shred sensitive files" },
  { command: "smssetup", description: "📲 Enroll TOTP for secure offline SMS commands" },
  { command: "sms_help", description: "📲 Air-gapped cellular SMS command manual & cheat sheet" },
  { command: "license", description: "🔑 Check Pro license status or activate key" },
  { command: "check_update", description: "🔄 Check for OTA app updates" },
  { command: "message", description: "📢 Display urgent fullscreen alert on device" },
  { command: "dead_drop", description: "☁️ Backup evidence to encrypted local/cloud vault" },
  { command: "wipe", description: "⚠️ Emergency remote factory reset (requires auth)" }
];

async function main() {
  console.log(`Setting ${commandsList.length} commands on bot @Pas_agent_bot...`);
  const res = await fetch(`https://api.telegram.org/bot${token}/setMyCommands`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ commands: commandsList })
  });
  const data = await res.json();
  console.log('setMyCommands response:', data);

  const menuRes = await fetch(`https://api.telegram.org/bot${token}/setChatMenuButton`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ menu_button: { type: 'commands' } })
  });
  const menuData = await menuRes.json();
  console.log('setChatMenuButton response:', menuData);
}

main().catch(console.error);
