// Upload full command menu to Telegram Bot API
const token = '8815969412:AAEN_BqiCldZVza93qApCbGn5hTrcAW9HxA';

const commandsList = [
  { command: "menu", description: "📱 Open interactive touchscreen control panel" },
  { command: "help", description: "📖 Show full help manual & command guide" },
  { command: "status", description: "📊 Live battery, storage, RAM & sensor telemetry" },
  { command: "selftest", description: "🩺 Run 9-point security, GPS & sensor audit" },
  { command: "info", description: "ℹ️ Hardware specs, SIM details & OS version" },
  { command: "locate", description: "📍 Acquire instant GPS fix & Google Maps pin" },
  { command: "tower", description: "📡 Cell tower triangulation & signal RF telemetry" },
  { command: "track", description: "🛰️ Start continuous live GPS tracking" },
  { command: "track_stop", description: "🛑 Stop continuous GPS tracking" },
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
  { command: "lock_pin", description: "🔑 Lock phone with custom 4-8 digit PIN" },
  { command: "set_os_pin", description: "🔐 Overwrite hardware OS lockscreen PIN (Device Owner)" },
  { command: "unlock", description: "🔓 Dismiss Lost Mode & unlock device screen" },
  { command: "fakeshutdown", description: "🕶️ Fake shutdown: blackout screen & silent traps" },
  { command: "wake", description: "☀️ Restore device from Fake Shutdown blackout" },
  { command: "ring", description: "🚨 Trigger maximum volume emergency siren" },
  { command: "ring_stop", description: "🔇 Silence active emergency alarm siren" },
  { command: "message", description: "📢 Display urgent fullscreen alert on device" },
  { command: "duress_pin", description: "🆘 Set decoy coercion PIN for emergency SOS" },
  { command: "trap", description: "🛡️ Arm sensor traps (snatch, charger, pocket)" },
  { command: "shred", description: "🗑️ Cryptographically shred sensitive files" },
  { command: "device_owner", description: "👑 Check Device Owner & Kiosk hardware lock" },
  { command: "dns", description: "🛡️ Enforce system-wide Private DNS-over-TLS (Device Owner)" },
  { command: "reboot", description: "🔄 Remotely restart phone hardware (Device Owner)" },
  { command: "stealth", description: "👁️ Toggle PASA app icon in launcher" },
  { command: "hide", description: "🔇 Hide PASA app icon from phone launcher" },
  { command: "show", description: "👁️ Restore PASA app icon to phone launcher" },
  { command: "contacts", description: "👥 Search device address book contacts" },
  { command: "call_log", description: "📞 View incoming and outgoing call history" },
  { command: "sms_log", description: "💬 View recent SMS inbox messages" },
  { command: "history", description: "📜 View recent command audit execution trail" },
  { command: "network", description: "🌐 Current IP, Wi-Fi SSID & cell carrier info" },
  { command: "apps", description: "📦 List installed applications" },
  { command: "app_uninstall", description: "❌ Silently uninstall package (Device Owner)" },
  { command: "smssetup", description: "📲 Enroll TOTP for secure offline SMS commands" },
  { command: "license", description: "🔑 Check Pro license status or activate key" },
  { command: "check_update", description: "🔄 Check for OTA app updates" },
  { command: "update_confirm", description: "⚡ Download and install pending OTA update" },
  { command: "wipe", description: "⚠️ Emergency remote factory reset (requires auth)" },
  { command: "wipe_confirm", description: "💥 Confirm remote factory reset with password" }
];

async function main() {
  console.log(`Setting ${commandsList.length} commands on bot...`);
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
