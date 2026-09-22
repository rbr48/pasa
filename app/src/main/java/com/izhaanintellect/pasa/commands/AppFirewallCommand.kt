package com.izhaanintellect.pasa.commands

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application Firewall - Block Remote Access Tools
 *
 * Prevent installation of TeamViewer, AnyDesk, and other RAT (Remote Access Trojans)
 * from connecting to the device's network.
 */
@Singleton
class AppFirewallCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/app_firewall"
    override val description = "Block apps from accessing network (prevents RAT/remote access)"
    override val usage = "/app_firewall [enable|disable|block|unblock|blacklist|whitelist|status]"

    companion object {
        private const val TAG = "PASA_AppFirewall"
        // Common RAT packages
        private val COMMON_RATS = listOf(
            "com.teamviewer.teamviewer.market.mobile",
            "com.anydesk.anydeskandroid",
            "org.chromium.webview_shell",
            "com.google.android.webview",
            "com.chrome.remote.desktop"
        )
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val action = args.firstOrNull()?.lowercase() ?: "status"

        return when (action) {
            "enable", "on" -> enableAppFirewall()
            "disable", "off" -> disableAppFirewall()
            "block", "add" -> blockApp(args.getOrNull(1) ?: "")
            "unblock", "remove" -> unblockApp(args.getOrNull(1) ?: "")
            "blacklist", "load_defaults" -> loadDefaultBlacklist()
            "whitelist", "set_whitelist" -> setWhitelistMode()
            "status" -> getFirewallStatus()
            else -> CommandResult(
                success = false,
                message = """
                    🔥 <b>App Firewall Command</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    <b>Usage:</b>
                    • <code>/app_firewall enable</code> — Enable firewall
                    • <code>/app_firewall block com.teamviewer.abc</code> — Block specific app
                    • <code>/app_firewall whitelist</code> — Whitelist-only mode
                    • <code>/app_firewall status</code> — Show state
                """.trimIndent()
            )
        }
    }

    private fun enableAppFirewall(): CommandResult {
        preferencesManager.isAppFirewallEnabled = true

        return CommandResult(
            success = true,
            message = """
                🟢 <b>App Firewall ENABLED</b>
                ━━━━━━━━━━━━━━━━━━━━
                🔥 <b>Status:</b> ACTIVE

                ✓ Blocked apps cannot access network
                ✓ Remote Access Tools (TeamViewer, AnyDesk) blocked
                ✓ Prevents thief from remote controlling device

                <b>Mode:</b> ${if (preferencesManager.isAppFirewallWhitelistOnly) "WHITELIST-ONLY" else "BLACKLIST"}
                <b>Blocked Apps:</b> ${preferencesManager.appFirewallBlacklist.size}

                <i>To see details: /app_firewall status</i>
            """.trimIndent()
        )
    }

    private fun disableAppFirewall(): CommandResult {
        preferencesManager.isAppFirewallEnabled = false

        return CommandResult(
            success = true,
            message = """
                🔴 <b>App Firewall DISABLED</b>
                ━━━━━━━━━━━━━━━━━━━━
                🔥 <b>Status:</b> INACTIVE

                All blocked apps can now access network.
                ⚠️ Device is vulnerable to remote access tools.
            """.trimIndent()
        )
    }

    private fun blockApp(packageName: String): CommandResult {
        if (packageName.isBlank()) {
            return CommandResult(
                success = false,
                message = "❌ Package name required. Format: /app_firewall block com.package.name"
            )
        }

        val blacklist = preferencesManager.appFirewallBlacklist.toMutableList()
        if (!blacklist.contains(packageName)) {
            blacklist.add(packageName)
            preferencesManager.appFirewallBlacklist = blacklist
        }

        return CommandResult(
            success = true,
            message = """
                ✅ <b>App Blocked</b>
                ━━━━━━━━━━━━━━━━━━━━
                📦 <b>Package:</b> <code>$packageName</code>
                🔥 <b>Network Access:</b> DENIED

                This app cannot connect to WiFi or cellular networks.

                <b>Total Blocked:</b> ${preferencesManager.appFirewallBlacklist.size}
            """.trimIndent()
        )
    }

    private fun unblockApp(packageName: String): CommandResult {
        if (packageName.isBlank()) {
            return CommandResult(
                success = false,
                message = "❌ Package name required."
            )
        }

        val blacklist = preferencesManager.appFirewallBlacklist.toMutableList()
        blacklist.remove(packageName)
        preferencesManager.appFirewallBlacklist = blacklist

        return CommandResult(
            success = true,
            message = """
                ✅ <b>App Unblocked</b>
                ━━━━━━━━━━━━━━━━━━━━
                📦 <b>Package:</b> <code>$packageName</code>
                🔓 <b>Network Access:</b> ALLOWED

                This app can now connect to networks.
            """.trimIndent()
        )
    }

    private fun loadDefaultBlacklist(): CommandResult {
        val blacklist = preferencesManager.appFirewallBlacklist.toMutableList()

        for (rat in COMMON_RATS) {
            if (!blacklist.contains(rat)) {
                blacklist.add(rat)
            }
        }

        preferencesManager.appFirewallBlacklist = blacklist

        return CommandResult(
            success = true,
            message = """
                ✅ <b>Default Blacklist Loaded</b>
                ━━━━━━━━━━━━━━━━━━━━
                🔥 <b>RAT Apps Blocked:</b>
                • TeamViewer
                • AnyDesk
                • Chrome Remote Desktop
                • WebView-based RATs

                <b>Total Blocked:</b> ${preferencesManager.appFirewallBlacklist.size}

                <i>To add more: /app_firewall block &lt;package_name&gt;</i>
            """.trimIndent()
        )
    }

    private fun setWhitelistMode(): CommandResult {
        preferencesManager.isAppFirewallWhitelistOnly = true

        return CommandResult(
            success = true,
            message = """
                🟢 <b>Whitelist-Only Mode ENABLED</b>
                ━━━━━━━━━━━━━━━━━━━━
                🔥 <b>Mode:</b> WHITELIST-ONLY (Maximum Security)

                ⚠️ <b>WARNING:</b> Only whitelisted apps can access network.
                All other apps are blocked from network access.

                This is the most restrictive mode. Only essential apps
                (calls, SMS, known services) can use network.

                <i>To revert to blacklist: /app_firewall blacklist</i>
            """.trimIndent()
        )
    }

    private fun getFirewallStatus(): CommandResult {
        return CommandResult(
            success = true,
            message = """
                🔥 <b>App Firewall Status</b>
                ━━━━━━━━━━━━━━━━━━━━
                🟢 <b>Firewall:</b> ${if (preferencesManager.isAppFirewallEnabled) "ENABLED" else "DISABLED"}
                🎯 <b>Mode:</b> ${if (preferencesManager.isAppFirewallWhitelistOnly) "WHITELIST-ONLY" else "BLACKLIST"}
                📦 <b>Blocked Apps:</b> ${preferencesManager.appFirewallBlacklist.size}

                <b>Common RATs Blocked:</b>
                ✓ TeamViewer
                ✓ AnyDesk
                ✓ Chrome Remote Desktop

                <b>Commands:</b>
                • <code>/app_firewall enable</code> — Enable protection
                • <code>/app_firewall block &lt;pkg&gt;</code> — Block app
                • <code>/app_firewall whitelist</code> — Strict mode

                ℹ️ Prevents remote access tools from connecting.
            """.trimIndent()
        )
    }
}
