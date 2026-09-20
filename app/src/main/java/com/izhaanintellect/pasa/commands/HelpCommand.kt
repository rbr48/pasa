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
            🛡️ <b>PASA (Private Android Security Agent)</b>
            ━━━━━━━━━━━━━━━━━━━━
            
            🔐 <b>Emergency Containment</b>
            • <code>/lock</code> — Lock device immediately
            • <code>/lock_message &lt;text&gt;</code> — Set lock screen banner
            • <code>/unlock</code> — Release lock and restore device
            • <code>/wipe</code> — Emergency factory reset (Requires Auth)
            • <code>/wipe_confirm &lt;pass&gt;</code> — Confirm device wipe
            • <code>/device_owner</code> — Inspect/enable Device Owner lockdown
            
            📍 <b>Location & Safe Zones</b>
            • <code>/locate</code> — Instant high-accuracy GPS fix + Maps pin
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
            • <code>/record &lt;seconds&gt;</code> — Ambient microphone recording [PRO TRIAL]
            • <code>/record stop</code> — Stop recording

            📇 <b>Extraction & Audit Logs</b>
            • <code>/history [count]</code> — View recent command execution logs
            • <code>/contacts [search]</code> — Read device address book
            • <code>/call_log [count]</code> — View incoming/outgoing calls
            • <code>/sms_log [count]</code> — View recent SMS messages
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
