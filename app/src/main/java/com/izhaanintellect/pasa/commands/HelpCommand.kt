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
            • <code>/wipe</code> — Emergency factory reset (Requires Auth)
            • <code>/wipe_confirm &lt;pass&gt;</code> — Confirm device wipe
            
            📍 <b>Location & Tracking</b>
            • <code>/locate</code> — Instant high-accuracy GPS fix + Maps pin
            • <code>/track &lt;minutes&gt;</code> — Continuous periodic tracking
            • <code>/track stop</code> — Deactivate tracking
            
            📸 <b>Visual & Audio Forensics</b>
            • <code>/snap front</code> — Front selfie camera photo
            • <code>/snap back</code> — Rear camera photo
            • <code>/video front &lt;seconds&gt;</code> — Front camera video (1-60s)
            • <code>/video back &lt;seconds&gt;</code> — Rear camera video (1-60s)
            • <code>/record &lt;seconds&gt;</code> — Ambient microphone recording
            • <code>/record stop</code> — Stop recording
            
            🔊 <b>Siren & Alarm</b>
            • <code>/ring</code> — Max-volume alarm (60s, overrides silent)
            • <code>/ring &lt;seconds&gt;</code> — Custom alarm duration
            • <code>/ring stop</code> — Silence alarm immediately
            • <code>/message &lt;text&gt;</code> — Display urgent message on screen
            
            📊 <b>Telemetry & Diagnostics</b>
            • <code>/status</code> — Battery, RAM, storage, uptime, GPS
            • <code>/info</code> — Hardware specs, OS patch, display, apps
            • <code>/network</code> — WiFi SSID, speed, IP, carrier details
            • <code>/clipboard</code> — Read device clipboard text
            • <code>/apps</code> — List installed applications
            • <code>/app_uninstall &lt;package&gt;</code> — Remove an application
            
            🔇 <b>Stealth</b>
            • <code>/hide</code> — Hide app icon from drawer
            • <code>/show</code> — Unhide app icon
            
            ━━━━━━━━━━━━━━━━━━━━
            💡 <i>Commands only respond to the authorized owner.</i>
        """.trimIndent()

        return CommandResult(success = true, message = helpText)
    }
}
