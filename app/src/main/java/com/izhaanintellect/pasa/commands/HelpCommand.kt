package com.izhaanintellect.pasa.commands

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Displays the comprehensive help menu and command documentation.
 */
@Singleton
class HelpCommand @Inject constructor() : Command {

    override val name = "/help"
    override val description = "List all available PASA commands"
    override val usage = "/help"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val helpText = """
            🛡️ <b>PASA Sentinel (Private Android Security Agent)</b>
            ━━━━━━━━━━━━━━━━━━━━
            
            🔐 <b>Emergency Containment</b>
            • <code>/lock</code> — Lock device immediately
            • <code>/lock_message &lt;text&gt;</code> — Set lock screen banner
            • <code>/lock_pin &lt;pin&gt;</code> — Lock with emergency 4-8 digit PIN
            • <code>/set_os_pin &lt;pin&gt;</code> — Overwrite hardware OS lock PIN [Device Owner]
            • <code>/unlock</code> — Release lock and restore device
            • <code>/wipe</code> — Emergency factory reset (Requires Auth)
            • <code>/wipe_confirm &lt;pass&gt;</code> — Confirm device wipe
            • <code>/set_master_pin &lt;pin&gt;</code> — Update emergency Master PIN remotely
            
            👑 <b>Enterprise Device Owner Defense</b>
            • <code>/device_owner</code> — Full enterprise telemetry dashboard
            • <code>/antitamper on|off|status</code> — Safe boot, airplane mode, factory reset lock
            • <code>/usb_lock on|off|status</code> — Kill USB data pins (AC charge only) [Android 12+]
            • <code>/notification hide|show|toggle</code> — Hide/restore notification tray [Android 6+]
            • <code>/self_heal</code> — Lock permissions permanently as unrevokable
            • <code>/freeze &lt;app&gt;</code> — Vanish banking/private apps into shadow vault
            • <code>/unfreeze &lt;app&gt;</code> — Restore hidden application
            • <code>/frozen</code> — List all hidden/quarantined applications
            • <code>/biometrics on|off</code> — Duress biometric killswitch (forces Master PIN)
            • <code>/dns quad9|cloudflare|adguard|off|status</code> — System-wide encrypted DNS-over-TLS
            • <code>/reboot</code> — Remotely restart device hardware

            🔒 <b>Advanced Security Monitoring</b>
            • <code>/sim_lock enable|disable|whitelist|alert_action</code> — SIM swap attack prevention
            • <code>/vibrate_pulse [count|sos|location|stop]</code> — Locate device via vibration patterns
            • <code>/pattern_guard enable|disable|threshold|action</code> — Monitor unlock attempts & capture evidence
            • <code>/app_firewall enable|disable|block|whitelist</code> — Block RAT/remote access tools from network
            • <code>/battery_alert enable|disable|threshold|history</code> — Monitor charging patterns & device activity

            🛡️ <b>Tamper-Proof Hardening</b>
            • <code>/tamper_detect enable|disable|scan|status</code> — Detect root, debuggers, hooks, emulators [NEW]
            • <code>/dead_drop enable|disable|upload|status</code> — Backup evidence to encrypted cloud vault [NEW]
            • <code>/harden_boot lock|unlock|status</code> — Lock recovery mode & prevent factory reset [NEW]
            • <code>/factory_reset_defense status|layers|threats</code> — Show factory reset protection details [NEW]

            📍 <b>Location & Safe Zones</b>
            • <code>/locate</code> — Instant high-accuracy GPS fix + Maps pin
            • <code>/tower</code> — Dual-SIM cell tower triangulation & signal RF telemetry
            • <code>/sim [slot]</code> — Display active SIM info, carrier, signal strength
            • <code>/track &lt;minutes&gt;</code> — Continuous periodic tracking
            • <code>/track stop</code> — Deactivate tracking
            • <code>/geofence here 200</code> — Set safe zone & alert on exit [PRO]
            • <code>/geofence status|on|off</code> — Safe zone status & toggle
            • <code>/smssetup</code> — Enroll TOTP for secure offline SMS commands

            📸 <b>Forensics &amp; Media</b>
            • <code>/snap front|back|both</code> — Covert snapshot (dual-camera with "both")
            • <code>/screenshot</code> — Silent full-screen capture [PRO TRIAL]
            • <code>/screen_burst [5-10]</code> — Rapid 5–10 frame montage [PRO TRIAL]
            • <code>/screenrecord &lt;seconds&gt;</code> — MP4 screen recording [PRO]
            • <code>/video front|back &lt;seconds&gt;</code> — Silent camera video (1-60s) [PRO TRIAL]
            • <code>/livestream [front|back] [mins]</code> — Near-live sequential video stream to Telegram [PRO]
            • <code>/stopstream</code> — Stop active live video stream
            • <code>/livestream_diag</code> — Troubleshoot livestream configuration issues
            • <code>/record &lt;seconds&gt;</code> — Ambient microphone recording [PRO TRIAL]
            • <code>/record stop</code> — Stop recording

            📇 <b>Extraction & Audit Logs</b>
            • <code>/history [count]</code> — View recent command execution logs
            • <code>/contacts [search]</code> — Read device address book
            • <code>/call_log [count]</code> — View incoming/outgoing calls
            • <code>/sms_log [count]</code> — View recent SMS messages
            • <code>/sendsms &lt;number&gt; &lt;message&gt;</code> — Send SMS via device SIM
            • <code>/sendsms sim1|sim2 &lt;number&gt; &lt;message&gt;</code> — Send SMS from specific SIM slot
            • <code>/clipboard</code> — Read device clipboard text
            
            🔊 <b>Siren & Display Alerts</b>
            • <code>/ring [seconds]</code> — Max-volume alarm (overrides silent)
            • <code>/ring stop</code> — Silence alarm immediately
            • <code>/message &lt;text&gt;</code> — Fullscreen urgent alert on display
            
            🎭 <b>Deception & Traps</b>
            • <code>/fakeshutdown</code> — Simulated power-off blackout [PRO]
            • <code>/wake</code> — Exit blackout screen deception
            • <code>/duress_pin &lt;pin&gt;</code> — Configure duress distress trigger [PRO]
            • <code>/trap &lt;on|off|status&gt;</code> — Anti-snatch / charger / pocket traps [PRO]
            • <code>/shred &lt;path&gt;</code> — Cryptographically shred sensitive files [PRO]

            📊 <b>Telemetry & System</b>
            • <code>/status</code> — Battery, RAM, storage, uptime, GPS
            • <code>/info</code> — Hardware specs, OS patch, display, apps
            • <code>/network</code> — WiFi SSID, IP, cellular signal
            • <code>/apps</code> — List installed applications
            • <code>/app_uninstall &lt;package&gt;</code> — Uninstall application
            • <code>/selftest</code> — Comprehensive 9-point security &amp; sensor audit
            • <code>/check_update</code> — Check for OTA app updates
            • <code>/update_confirm</code> — Install pending OTA update
            • <code>/hide</code> / <code>/show</code> — Toggle app icon in launcher
            
            ━━━━━━━━━━━━━━━━━━━━
            💡 <i>Commands only respond to the authorized owner.</i>
        """.trimIndent()

        return CommandResult(success = true, message = helpText)
    }
}
