package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remotely freezes/hides applications into a stealth vault or restores them.
 * Ideal for shielding banking, crypto, or private communication apps.
 */
@Singleton
class FreezeCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : Command {

    override val name = "/freeze"
    override val description = "Freeze/hide sensitive apps from system and launcher [Device Owner]"
    override val usage = "/freeze <package|name> | /unfreeze <package|name> | /frozen"

    companion object {
        val PROTECTED_PACKAGES = setOf(
            "com.google.android.documentsui",
            "com.android.documentsui",
            "com.android.systemui",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller"
        )
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> App freezing and shadow vault requires Device Owner privileges.\n" +
                        "Run <code>/device_owner</code> for activation instructions."
            )
        }

        if (args.isEmpty()) {
            return listFrozenApps()
        }

        val subCmd = args.first().lowercase()
        return when (subCmd) {
            "list", "status" -> listFrozenApps()
            "unfreeze", "restore", "unhide" -> {
                val target = args.drop(1).joinToString(" ").trim()
                if (target.isBlank()) {
                    CommandResult(false, "❓ <b>Usage:</b> <code>/unfreeze &lt;package_name or app_name&gt;</code>")
                } else {
                    handleUnfreeze(target)
                }
            }
            else -> {
                // If the first argument is not a known subcommand, treat all args as the app target to freeze
                val target = args.joinToString(" ").trim()
                handleFreeze(target)
            }
        }
    }

    suspend fun executeUnfreeze(args: List<String>): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(false, "❌ <b>Device Owner Required:</b> Run <code>/device_owner</code>.")
        }
        val target = args.joinToString(" ").trim()
        if (target.isBlank()) {
            return CommandResult(false, "❓ <b>Usage:</b> <code>/unfreeze &lt;package_name or app_name&gt;</code>")
        }
        return handleUnfreeze(target)
    }

    suspend fun listFrozenApps(): CommandResult {
        val frozen = prefs.frozenPackages
        if (frozen.isEmpty()) {
            return CommandResult(
                success = true,
                message = "📦 <b>Shadow App Vault: Empty</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "No applications are currently frozen.\n\n" +
                        "💡 <i>To hide an app, use <code>/freeze &lt;name or package&gt;</code> (e.g. <code>/freeze binance</code>)</i>"
            )
        }

        val pm = context.packageManager
        val sb = StringBuilder("📦 <b>Shadow App Vault (Frozen Applications)</b>\n━━━━━━━━━━━━━━━━━━━━\n")
        frozen.forEach { pkg ->
            val label = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                "Unknown"
            }
            sb.append("• <b>$label</b>\n  <code>$pkg</code>\n")
        }
        sb.append("\n💡 <i>To restore an app, use <code>/unfreeze &lt;package&gt;</code></i>")

        return CommandResult(success = true, message = sb.toString())
    }

    private fun handleFreeze(target: String): CommandResult {
        val resolved = resolvePackage(target)
            ?: return CommandResult(
                success = false,
                message = "❌ <b>App Not Found:</b> Could not find an installed application matching '<code>$target</code>'.\n" +
                        "Send <code>/apps</code> to inspect installed packages."
            )

        val (pkg, label) = resolved
        if (pkg == context.packageName || PROTECTED_PACKAGES.contains(pkg)) {
            return CommandResult(
                success = false,
                message = "⛔ <b>Protected System Core:</b> '<code>$pkg</code>' is an essential Android system service (Storage Access Framework / System Core). Freezing it would disable core OS functions like system file pickers."
            )
        }

        val (ok, text) = PasaDeviceAdmin.setAppHidden(context, pkg, true)
        return if (ok) {
            prefs.addFrozenPackage(pkg)
            CommandResult(
                success = true,
                message = "🧊 <b>Application Frozen & Vanished</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "• App: <b>$label</b>\n" +
                        "• Package: <code>$pkg</code>\n" +
                        "• State: <b>HIDDEN & FROZEN</b>\n\n" +
                        "🛡️ <i>The app is now completely invisible on the phone's home screen, app drawer, and search. " +
                        "All data is safe. Restore anytime using <code>/unfreeze $pkg</code></i>"
            )
        } else {
            CommandResult(false, text)
        }
    }

    private fun handleUnfreeze(target: String): CommandResult {
        val resolvedPkg = if (target.contains(".")) {
            target
        } else {
            // Check in frozen list first
            prefs.frozenPackages.find { it.contains(target, ignoreCase = true) }
                ?: resolvePackage(target)?.first
                ?: target
        }

        val (ok, text) = PasaDeviceAdmin.setAppHidden(context, resolvedPkg, false)
        return if (ok) {
            prefs.removeFrozenPackage(resolvedPkg)
            CommandResult(
                success = true,
                message = "☀️ <b>Application Restored & Unfrozen</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "• Package: <code>$resolvedPkg</code>\n" +
                        "• State: <b>VISIBLE & ACTIVE</b>\n\n" +
                        "✅ <i>The app icon and launcher access have been restored.</i>"
            )
        } else {
            CommandResult(false, text)
        }
    }

    private fun resolvePackage(query: String): Pair<String, String>? {
        val pm = context.packageManager
        // 1. Direct package match
        try {
            val info = pm.getApplicationInfo(query, 0)
            val label = pm.getApplicationLabel(info).toString()
            return Pair(query, label)
        } catch (_: Exception) {}

        // 2. Fuzzy search by label or package
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installed) {
            val label = pm.getApplicationLabel(app).toString()
            if (label.contains(query, ignoreCase = true) || app.packageName.contains(query, ignoreCase = true)) {
                return Pair(app.packageName, label)
            }
        }
        return null
    }
}
