package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin.AntiTamperItem
import com.izhaanintellect.pasa.bot.InlineKeyboardButton
import com.izhaanintellect.pasa.bot.InlineKeyboardMarkup
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.security.AuthManager
import com.izhaanintellect.pasa.security.Totp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise Anti-Tamper Suite Command.
 * Controls hardware restrictions (Safe Boot, Airplane Mode, Factory Reset, Network Reset,
 * OTG block, USB file transfer, Location toggle, USB debugging, Notification shade lockout).
 * Supports granular toggle of individual controls as well as bulk arm/disarm.
 * Disarming requires Master Password / Active Session.
 */
@Singleton
class AntiTamperCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val authManager: AuthManager
) : Command {

    override val name = "/antitamper"
    override val description = "Toggle Enterprise Anti-Tamper Hardening Suite [Device Owner]"
    override val usage = "/antitamper [status|on|off] | /antitamper toggle <option> | /antitamper <option> on|off"

    companion object {
        private val BULK_ARM = setOf("on", "enable", "arm")
        private val BULK_DISARM = setOf("off", "disable", "disarm")
        private val STATUS_WORDS = setOf("status", "info", "list")
    }

    private fun resolveKey(raw: String): String? {
        return when (raw.lowercase().trim()) {
            "safeboot", "safe_boot", "safe" -> "safeboot"
            "airplane", "airplanemode", "airplane_mode" -> "airplane"
            "factory_reset", "reset", "wipe_lock", "wipe" -> "factory_reset"
            "network_reset", "net_reset", "network" -> "network_reset"
            "otg", "media", "mount", "sdcard" -> "otg"
            "usb_file", "mtp", "file_transfer", "usb_mtp" -> "usb_file"
            "location", "gps_lock", "location_config", "gps" -> "location"
            "usb_debug", "adb", "debugging" -> "usb_debug"
            "status_bar", "shade", "quick_settings", "statusbar" -> "status_bar"
            else -> null
        }
    }

    private fun verifyCredentials(candidate: String?): Boolean {
        if (authManager.isSessionAuthenticated()) return true
        if (candidate.isNullOrBlank()) return false
        val isPass = authManager.verifySessionOrPassword(candidate)
        val totpSecret = prefs.smsTotpSecret
        val isTotp = totpSecret.isNotBlank() && Totp.verify(totpSecret, candidate, window = 3)
        if (isTotp) authManager.recordSessionAuthenticated()
        return isPass || isTotp
    }

    private fun buildKeyboard(items: List<PasaDeviceAdmin.AntiTamperItem>): InlineKeyboardMarkup {
        val map = items.associateBy { it.key }
        fun icon(key: String): String = if (map[key]?.isLocked == true) "✅" else "🔓"

        return InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("${icon("safeboot")} Safe Boot", callbackData = "cmd:antitamper:toggle:safeboot"),
                    InlineKeyboardButton("${icon("airplane")} Airplane Mode", callbackData = "cmd:antitamper:toggle:airplane")
                ),
                listOf(
                    InlineKeyboardButton("${icon("factory_reset")} Factory Reset", callbackData = "cmd:antitamper:toggle:factory_reset"),
                    InlineKeyboardButton("${icon("network_reset")} Network Reset", callbackData = "cmd:antitamper:toggle:network_reset")
                ),
                listOf(
                    InlineKeyboardButton("${icon("otg")} OTG Mount", callbackData = "cmd:antitamper:toggle:otg"),
                    InlineKeyboardButton("${icon("usb_file")} USB File MTP", callbackData = "cmd:antitamper:toggle:usb_file")
                ),
                listOf(
                    InlineKeyboardButton("${icon("location")} Location Lock", callbackData = "cmd:antitamper:toggle:location"),
                    InlineKeyboardButton("${icon("usb_debug")} USB Debug ADB", callbackData = "cmd:antitamper:toggle:usb_debug")
                ),
                listOf(
                    InlineKeyboardButton("${icon("status_bar")} Notification Shade Lock", callbackData = "cmd:antitamper:toggle:status_bar")
                ),
                listOf(
                    InlineKeyboardButton("🔒 ARM ALL", callbackData = "cmd:antitamper:on"),
                    InlineKeyboardButton("🔓 DISARM ALL", callbackData = "cmd:antitamper:off")
                ),
                listOf(
                    InlineKeyboardButton("🔄 Refresh", callbackData = "cmd:antitamper:status"),
                    InlineKeyboardButton("🔙 Hub Menu", callbackData = "menu:device_owner_hub")
                )
            )
        )
    }

    private fun buildStatusMessage(items: List<PasaDeviceAdmin.AntiTamperItem>, headerNote: String? = null): String {
        val lockedCount = items.count { it.isLocked }
        val total = items.size
        val sb = StringBuilder()
        if (!headerNote.isNullOrBlank()) {
            sb.append(headerNote).append("\n\n")
        }
        sb.append("🛡️ <b>Enterprise Anti-Tamper Hardening Suite</b>\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("• Overall Security: <b>$lockedCount/$total Protections Active</b>\n\n")
        items.forEach { item ->
            val icon = if (item.isLocked) "✅ <b>Locked:</b>" else "🔓 <i>Allowed:</i>"
            sb.append("$icon ${item.label}\n")
        }
        sb.append("\n💡 <i>Tap any option below to toggle individually, or use ARM / DISARM ALL.</i>")
        return sb.toString()
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> Enterprise Anti-Tamper controls require Device Owner privileges.\n" +
                        "Run <code>/device_owner</code> for activation instructions."
            )
        }

        // Clean args: lowercase strings
        val cleanArgs = args.map { it.trim() }.filter { it.isNotBlank() }

        // Detect potential password candidate (any word that is not a command keyword or known key)
        val knownKeywords = setOf("on", "enable", "arm", "off", "disable", "disarm", "status", "info", "list", "toggle")
        val candidate = cleanArgs.firstOrNull { arg ->
            val lower = arg.lowercase()
            lower !in knownKeywords && resolveKey(lower) == null
        }

        // Detect target key if any
        val targetKey = cleanArgs.mapNotNull { resolveKey(it) }.firstOrNull()

        // Detect action
        val hasArm = cleanArgs.any { it.lowercase() in BULK_ARM }
        val hasDisarm = cleanArgs.any { it.lowercase() in BULK_DISARM }
        val hasToggle = cleanArgs.any { it.lowercase() == "toggle" } || (targetKey != null && !hasArm && !hasDisarm)

        // Case 1: Granular Target Key specified
        if (targetKey != null) {
            val items = PasaDeviceAdmin.getAntiTamperItems(context)
            val currentItem = items.firstOrNull { it.key == targetKey }

            val shouldEnable = when {
                hasArm -> true
                hasDisarm -> false
                hasToggle -> currentItem?.isLocked != true // if currently locked, toggle to false
                else -> true
            }

            // Disarming requires password / active session
            if (!shouldEnable && authManager.hasMasterPassword() && !verifyCredentials(candidate)) {
                return CommandResult(
                    success = false,
                    message = """
                        🔑 <b>Disarm Anti-Tamper Option (Zero-Trust Guard)</b>
                        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        Unlocking <b>${currentItem?.label ?: targetKey}</b> requires Master Password or active session.

                        <b>Syntax:</b> <code>/antitamper &lt;password&gt; toggle $targetKey</code>
                        <b>Example:</b> <code>/antitamper MySecretPass123 toggle $targetKey</code>
                    """.trimIndent(),
                    replyMarkup = buildKeyboard(items)
                )
            }

            val (ok, text) = PasaDeviceAdmin.setIndividualAntiTamper(context, targetKey, shouldEnable)
            val updatedItems = PasaDeviceAdmin.getAntiTamperItems(context)
            prefs.antiTamperEnabled = updatedItems.any { it.isLocked }

            val header = if (ok) "⚡ <b>Updated:</b> $text" else "❌ <b>Error:</b> $text"
            return CommandResult(
                success = ok,
                message = buildStatusMessage(updatedItems, header),
                replyMarkup = buildKeyboard(updatedItems)
            )
        }

        // Case 2: Bulk ARM ALL
        if (hasArm) {
            val results = PasaDeviceAdmin.applyAntiTamperSuite(context, true)
            prefs.antiTamperEnabled = true
            val updatedItems = PasaDeviceAdmin.getAntiTamperItems(context)

            val header = "🛡️ <b>Enterprise Anti-Tamper Suite: ALL ARMED</b>\n" +
                    "🔒 <i>Safe boot, Airplane mode, and Factory reset are now physically blocked!</i>"
            return CommandResult(
                success = true,
                message = buildStatusMessage(updatedItems, header),
                replyMarkup = buildKeyboard(updatedItems)
            )
        }

        // Case 3: Bulk DISARM ALL
        if (hasDisarm) {
            if (authManager.hasMasterPassword() && !verifyCredentials(candidate)) {
                val currentItems = PasaDeviceAdmin.getAntiTamperItems(context)
                return CommandResult(
                    success = false,
                    message = """
                        🔑 <b>Disarm Anti-Tamper Suite (Zero-Trust Guard)</b>
                        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        Disarming enterprise hardware protections requires your Master Password or active session.

                        <b>Syntax:</b> <code>/antitamper &lt;password&gt; off</code>
                        <b>Example:</b> <code>/antitamper MySecretPass123 off</code>
                    """.trimIndent(),
                    replyMarkup = buildKeyboard(currentItems)
                )
            }

            val results = PasaDeviceAdmin.applyAntiTamperSuite(context, false)
            prefs.antiTamperEnabled = false
            val updatedItems = PasaDeviceAdmin.getAntiTamperItems(context)

            val header = "⚠️ <b>Enterprise Anti-Tamper Suite: ALL DISARMED</b>\n" +
                    "ℹ️ <i>Device hardware restrictions restored to normal.</i>"
            return CommandResult(
                success = true,
                message = buildStatusMessage(updatedItems, header),
                replyMarkup = buildKeyboard(updatedItems)
            )
        }

        // Case 4: Status / Default Menu
        val currentItems = PasaDeviceAdmin.getAntiTamperItems(context)
        return CommandResult(
            success = true,
            message = buildStatusMessage(currentItems),
            replyMarkup = buildKeyboard(currentItems)
        )
    }
}
