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
            • <code>/camera_lock on|off|status</code> — Hardware camera killswitch (anti-spy)
            • <code>/bluetooth_lock on|off|status</code> — Disallow Bluetooth pairing & file sharing
            • <code>/mic_mute on|off|status</code> — Hardware master audio mute (HAL level)
            • <code>/lockscreen_info &lt;text&gt;|clear</code> — Pin contact banner to OS lockscreen
            • <code>/autolock &lt;sec&gt;|default</code> — Enforce screen inactivity timeout policy
            • <code>/wifi_connect &lt;ssid&gt; [pass]</code> — Emergency Wi-Fi provisioning while locked
            • <code>/security_audit</code> — Inspect Linux kernel OS security audit logs
            • <code>/notification hide|show|toggle</code> — Hide/restore notification tray [Android 6+]
            • <code>/self_heal</code> — Lock permissions permanently as unrevokable
            • <code>/freeze &lt;app&gt;</code> — Vanish banking/private apps into shadow vault
            • <code>/unfreeze &lt;app&gt;</code> — Restore hidden application
            • <code>/frozen</code> — List all hidden/quarantined applications
            • <code>/lock_app &lt;gallery|phone|files|target&gt;</code> — Smart app freeze &amp; lockout [NEW]
            • <code>/unlock_app &lt;target&gt;</code> — Restore locked application [NEW]
            • <code>/biometrics on|off</code> — Duress biometric killswitch (forces Master PIN)
            • <code>/dns quad9|cloudflare|adguard|off|status</code> — System-wide encrypted DNS-over-TLS
            • <code>/reboot</code> — Remotely restart device hardware

            🔒 <b>Advanced Security Monitoring</b>
            • <code>/sim_lock enable|disable|whitelist|alert_action</code> — SIM swap attack prevention
            • <code>/vibrate_pulse [count|sos|location|stop]</code> — Locate device via vibration patterns
            • <code>/pattern_guard enable|disable|threshold|action</code> — Monitor unlock attempts &amp; capture evidence
            • <code>/app_firewall enable|disable|block|whitelist</code> — Block RAT/remote access tools from network
            • <code>/battery_alert enable|disable|threshold|history</code> — Monitor charging patterns &amp; device activity

            🛡️ <b>Tamper-Proof Hardening</b>
            • <code>/tamper_detect enable|disable|scan|status</code> — Detect root, debuggers, hooks, emulators [NEW]
            • <code>/dead_drop enable|disable|upload|status</code> — Backup evidence to encrypted cloud vault [NEW]
            • <code>/harden_boot lock|unlock|status</code> — Lock recovery mode &amp; prevent factory reset [NEW]
            • <code>/factory_reset_defense status|layers|threats</code> — Show factory reset protection details [NEW]

            📍 <b>Location &amp; Safe Zones</b>
            • <code>/locate</code> — Instant high-accuracy GPS fix + Maps pin
            • <code>/tower</code> — Dual-SIM cell tower triangulation &amp; signal RF telemetry
            • <code>/sim [slot]</code> — Display active SIM info, carrier, signal strength
            • <code>/track &lt;minutes&gt;</code> — Continuous periodic tracking
            • <code>/track stop</code> — Deactivate tracking
            • <code>/geofence here 200</code> — Set safe zone &amp; alert on exit [PRO]
            • <code>/geofence status|on|off</code> — Safe zone status &amp; toggle
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
            • <code>/gallery_latest [count]</code> — Extract recent camera roll photos [NEW]
            • <code>/getfile &lt;#|name|path&gt;</code> — Download file by number (from /list_files) or path (up to 50MB) [NEW]
            • <code>/list_files [dir|shortcut] [--all]</code> — Browse storage with 1-tap download numbers [NEW]

            📇 <b>Extraction, Telephony &amp; Audit Logs</b>
            • <code>/call &lt;number&gt; [speaker]</code> — Remotely place outbound cellular phone call [NEW]
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
            • <code>/trap &lt;on|off|status&gt;</code> — Anti-snatch / charger / pocket / thermal traps [PRO]
            • <code>/thermal [on|off|threshold|status]</code> — Anti-EDL heat-gun anomaly trap [NEW]
            • <code>/deadman [enable|disable|hours|status]</code> — Anti-EDL offline auto-destruct timer [NEW]
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
            • <code>/license [activate &lt;key&gt;]</code> — Check tier & activate Pro key
            • <code>/hide</code> / <code>/show</code> — Toggle app icon in launcher
            
            📱 <b>Air-Gapped Cellular SMS Fallback</b>
            • <code>/sms_help</code> — Comprehensive guide & copyable templates for offline SMS control
            • Format: <code>PASA &lt;pin&gt; &lt;command&gt; [args]</code>
            • Examples: <code>PASA 5892 /locate</code> | <code>PASA 5892 /status</code> | <code>PASA 5892 /lock</code>
            • Device Owner: <code>PASA 5892 /camera_lock on</code> | <code>PASA 5892 /usb_lock on</code> | <code>PASA 5892 /reboot</code>
            • <code>/smssetup</code> — Enroll TOTP for secure offline rotating 6-digit passcodes

            ━━━━━━━━━━━━━━━━━━━━
            💡 <i>Commands only respond to the authorized owner.</i>
        """.trimIndent()

        return CommandResult(success = true, message = helpText)
    }

    /**
     * Dedicated SMS Fallback Command Manual & Cheatsheet
     */
    fun executeSmsHelp(): CommandResult {
        val text = """
            📲 <b>PASA Sentinel — Air-Gapped SMS Command Manual</b>
            ━━━━━━━━━━━━━━━━━━━━━━━━━━
            When cellular data or Wi-Fi is disabled, severed, or jammed, PASA Sentinel operates air-gapped via direct GSM cellular SMS messages.
            
            🔑 <b>Authentication Syntax:</b>
            <code>PASA &lt;6-digit-TOTP-or-MasterPIN&gt; &lt;command&gt; [args]</code>
            
            <i>Example (Master PIN: 5892):</i>
            • <code>PASA 5892 /locate</code>
            • <code>PASA 5892 /status</code>
            • <code>PASA 5892 /camera_lock on</code>
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━
            📡 <b>Location & Diagnostics:</b>
            • <code>PASA &lt;pin&gt; /locate</code> — Overrides hardware GPS on & returns Maps pin
            • <code>PASA &lt;pin&gt; /status</code> — Battery %, charging state, lock status & DO posture
            
            🚨 <b>Emergency Lockdown & Sirens:</b>
            • <code>PASA &lt;pin&gt; /lock [pin]</code> — Lock device screen with custom or default PIN
            • <code>PASA &lt;pin&gt; /unlock</code> — Release screen lock & dismiss Lost Mode
            • <code>PASA &lt;pin&gt; /ring [30|60]</code> — Sound full-volume alarm siren (overrides silent)
            • <code>PASA &lt;pin&gt; /fakeshutdown</code> — 0-nit blackout screen deception
            • <code>PASA &lt;pin&gt; /wake</code> — Restore display from blackout
            
            👑 <b>Knox Device Owner Killswitches:</b>
            • <code>PASA &lt;pin&gt; /camera_lock on|off</code> — Hardware camera killswitch (anti-spy)
            • <code>PASA &lt;pin&gt; /bluetooth_lock on|off</code> — Disallow Bluetooth pairing & file transfer
            • <code>PASA &lt;pin&gt; /mic_mute on|off</code> — Hardware master audio mute (HAL level)
            • <code>PASA &lt;pin&gt; /usb_lock on|off</code> — Disable USB data pins (blocks forensic boxes)
            • <code>PASA &lt;pin&gt; /wifi_connect &lt;ssid&gt; [pass]</code> — Connect Wi-Fi while locked
            • <code>PASA &lt;pin&gt; /lockscreen_info &lt;msg&gt;</code> — Pin contact message to lockscreen
            • <code>PASA &lt;pin&gt; /autolock &lt;sec&gt;</code> — Enforce screen inactivity autolock timeout
            • <code>PASA &lt;pin&gt; /reboot</code> — Remotely restart phone hardware
            • <code>PASA &lt;pin&gt; /security_audit</code> — Query low-level kernel security logs
            • <code>PASA &lt;pin&gt; /app_uninstall &lt;pkg&gt;</code> — Silently uninstall spyware or RAT
            • <code>PASA &lt;pin&gt; /lock_app &lt;gallery|phone|files|target&gt;</code> — Freeze target app [NEW]
            • <code>PASA &lt;pin&gt; /unlock_app &lt;target&gt;</code> — Restore target app [NEW]
            
            📞 <b>Air-Gapped Telephony &amp; Outbound Calls:</b>
            • <code>PASA &lt;pin&gt; /call &lt;number&gt; [speaker]</code> — Place outbound cellular phone call [NEW]
            • <code>PASA &lt;pin&gt; /sendsms &lt;number&gt; &lt;msg&gt;</code> — Dispatch SMS via device SIM
            
            🛡️ <b>System Security &amp; Remote Wipe:</b>
            • <code>PASA &lt;pin&gt; /antitamper on|off</code> — Safe boot, airplane mode &amp; reset lock
            • <code>PASA &lt;pin&gt; /biometrics on|off</code> — Disable fingerprint/face unlock under coercion
            • <code>PASA &lt;pin&gt; /set_master_pin &lt;new&gt;</code> — Remotely rotate master PIN
            • <code>PASA &lt;pin&gt; /wipe</code> — 2-step authenticated factory reset
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━
            💡 <b>Dual-SIM Routing:</b> Outbound SMS replies are automatically routed through the exact SIM card slot that received the incoming command.
            📲 <b>TOTP Enrollment:</b> Run <code>/smssetup</code> to link your authenticator app for rotating one-time offline authentication!
        """.trimIndent()
        return CommandResult(success = true, message = text)
    }
}
