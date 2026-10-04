package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart application lockout command for Device Owner mode.
 * Supports keyword targets: 'gallery', 'phone'/'dialer', 'files'/'storage', or package/app name.
 *
 * Usage:
 *   /lock_app <gallery|phone|files|package|app_name>
 *   /unlock_app <gallery|phone|files|package|app_name>
 *   /lock_app status
 */
@Singleton
class AppLockCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val freezeCommand: FreezeCommand
) : Command {

    override val name = "/lock_app"
    override val description = "Lock or unlock Gallery, Phone/Dialer, Files, or sensitive apps [Device Owner]"
    override val usage = "/lock_app <gallery|phone|files|target> | /unlock_app <target>"

    companion object {
        private const val TAG = "PASA_AppLock"

        private val KNOWN_GALLERIES = listOf(
            "com.google.android.apps.photos",
            "com.sec.android.gallery3d",
            "com.miui.gallery",
            "com.coloros.gallery3d",
            "com.huawei.photos",
            "com.oneplus.gallery",
            "com.android.gallery3d",
            "com.android.gallery"
        )

        private val KNOWN_DIALERS = listOf(
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.dialer",
            "com.android.phone",
            "com.huawei.android.dialer"
        )

        private val KNOWN_FILE_MANAGERS = listOf(
            "com.google.android.apps.nbu.files", // Files by Google
            "com.sec.android.app.myfiles",        // Samsung My Files
            "com.mi.android.globalFileexplorer",  // Xiaomi File Manager
            "com.coloros.filemanager",            // OPPO / Realme File Manager
            "com.huawei.hidisk",                  // Huawei File Manager
            "com.oneplus.filemanager",            // OnePlus File Manager
            "com.motorola.filemanager",           // Motorola File Manager
            "pl.solidexplorer2",                  // Solid Explorer
            "com.lonelycatgames.Xplore",          // X-plore
            "com.alphainventor.filemanager"       // File Manager+
        )

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
                message = "❌ <b>Device Owner Required:</b> Locking system apps requires Knox Device Owner privileges.\n" +
                        "Run <code>/device_owner</code> for activation instructions."
            )
        }

        if (args.isEmpty()) {
            return CommandResult(
                success = false,
                message = """
                    🧊 <b>Smart App Lockout Suite</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    Shield core system apps or user apps instantly from access.

                    ⚠️ <b>Usage:</b>
                    • <code>/lock_app gallery</code> — Lock photo gallery
                    • <code>/lock_app phone</code> — Lock cellular dialer
                    • <code>/lock_app files</code> — Lock file manager / storage
                    • <code>/lock_app &lt;package or name&gt;</code> — Lock any app (e.g. <code>binance</code>)
                    • <code>/unlock_app &lt;target&gt;</code> — Restore locked application
                    • <code>/lock_app status</code> — List currently locked apps
                """.trimIndent()
            )
        }

        val first = args.first().lowercase()
        return when (first) {
            "status", "list" -> freezeCommand.listFrozenApps()
            "unlock", "restore" -> {
                val subTarget = args.drop(1).joinToString(" ").trim()
                if (subTarget.isBlank()) {
                    CommandResult(false, "❓ <b>Usage:</b> <code>/unlock_app &lt;gallery|phone|files|target&gt;</code>")
                } else {
                    executeUnlock(listOf(subTarget))
                }
            }
            else -> {
                val target = args.joinToString(" ").trim()
                executeLock(target)
            }
        }
    }

    suspend fun executeLock(target: String): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(false, "❌ <b>Device Owner Required:</b> Run <code>/device_owner</code>.")
        }

        if (target.startsWith("/")) {
            return CommandResult(
                success = false,
                message = "❌ <b>Invalid Target:</b> '<code>$target</code>' is a command, not an application.\n" +
                        "To lock an app, use: <code>/lock_app gallery</code>, <code>phone</code>, <code>files</code>, or a package name."
            )
        }

        val packagesToLock = resolveTargetPackages(target)
        if (packagesToLock.isEmpty()) {
            return CommandResult(
                success = false,
                message = "❌ <b>App Not Found:</b> Could not detect an installed application matching '<code>$target</code>'.\n" +
                        "Send <code>/apps</code> to inspect installed packages."
            )
        }

        // Filter out protected system core packages
        val eligiblePackages = packagesToLock.filter {
            it != context.packageName && !PROTECTED_PACKAGES.contains(it)
        }

        if (eligiblePackages.isEmpty()) {
            return CommandResult(
                success = false,
                message = "⛔ <b>Protected System Core:</b> The target application (<code>$target</code>) is an essential Android system service (Storage Access Framework / System Core). Locking it would disable system file pickers or core OS features."
            )
        }

        val lockedList = mutableListOf<String>()
        val pm = context.packageManager

        for (pkg in eligiblePackages) {
            val (ok, _) = PasaDeviceAdmin.setAppHidden(context, pkg, true)
            if (ok) {
                prefs.addFrozenPackage(pkg)
                val label = try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (_: Exception) { pkg }
                lockedList.add("<b>$label</b> (<code>$pkg</code>)")
            }
        }

        return if (lockedList.isNotEmpty()) {
            CommandResult(
                success = true,
                message = """
                    🔒 <b>Application Locked &amp; Concealed</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    The following target(s) are now frozen and invisible on the device:
                    ${lockedList.joinToString("\n• ", prefix = "• ")}

                    🛡️ <i>The app cannot be opened from the launcher or app drawer. All user data is preserved safely.</i>
                    💡 <i>To restore access: <code>/unlock_app $target</code></i>
                """.trimIndent()
            )
        } else {
            CommandResult(false, "❌ <b>Lock Failed:</b> Device Owner was unable to hide package.")
        }
    }

    suspend fun executeUnlock(args: List<String>): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(false, "❌ <b>Device Owner Required:</b> Run <code>/device_owner</code>.")
        }

        val target = args.joinToString(" ").trim()
        if (target.isBlank()) {
            return CommandResult(false, "❓ <b>Usage:</b> <code>/unlock_app &lt;gallery|phone|files|target&gt;</code>")
        }

        if (target.startsWith("/")) {
            return CommandResult(
                success = false,
                message = "❌ <b>Invalid Target:</b> '<code>$target</code>' is a command, not an application.\n" +
                        "To unlock an app, use: <code>/unlock_app gallery</code>, <code>phone</code>, <code>files</code>, or a package name."
            )
        }

        val packagesToUnlock = resolveTargetPackages(target, checkFrozenFirst = true)
        val pm = context.packageManager
        val unlockedList = mutableListOf<String>()

        for (pkg in packagesToUnlock) {
            val (ok, _) = PasaDeviceAdmin.setAppHidden(context, pkg, false)
            if (ok) {
                prefs.removeFrozenPackage(pkg)
                val label = try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (_: Exception) { pkg }
                unlockedList.add("<b>$label</b> (<code>$pkg</code>)")
            }
        }

        return if (unlockedList.isNotEmpty()) {
            CommandResult(
                success = true,
                message = """
                    ☀️ <b>Application Unlocked &amp; Restored</b>
                    ━━━━━━━━━━━━━━━━━━━━
                    The following target(s) have been restored to the launcher:
                    ${unlockedList.joinToString("\n• ", prefix = "• ")}

                    ✅ <i>The app is once again visible and fully accessible.</i>
                """.trimIndent()
            )
        } else {
            // Fallback to freezeCommand standard unfreeze
            freezeCommand.executeUnfreeze(args)
        }
    }

    private fun resolveTargetPackages(query: String, checkFrozenFirst: Boolean = false): List<String> {
        val q = query.lowercase().trim()
        val pm = context.packageManager

        // Check keyword matches
        when (q) {
            "gallery", "photos", "photo", "pictures", "images" -> {
                val matches = mutableListOf<String>()
                // 1. Check known gallery packages
                for (pkg in KNOWN_GALLERIES) {
                    if (isInstalledOrFrozen(pkg)) matches.add(pkg)
                }
                // 2. Query intent activities for viewing images
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                }
                val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                for (act in activities) {
                    val pkg = act.activityInfo.packageName
                    if (pkg != context.packageName && !matches.contains(pkg)) {
                        matches.add(pkg)
                    }
                }
                if (matches.isNotEmpty()) return matches
            }
            "phone", "call", "dialer", "dial" -> {
                val matches = mutableListOf<String>()
                // 1. TelecomManager default dialer
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    val defDialer = telecom?.defaultDialerPackage
                    if (!defDialer.isNullOrBlank() && defDialer != context.packageName) {
                        matches.add(defDialer)
                    }
                }
                // 2. Known dialers
                for (pkg in KNOWN_DIALERS) {
                    if (isInstalledOrFrozen(pkg) && !matches.contains(pkg)) matches.add(pkg)
                }
                // 3. Dial intent query
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:123"))
                val activities = pm.queryIntentActivities(dialIntent, PackageManager.MATCH_ALL)
                for (act in activities) {
                    val pkg = act.activityInfo.packageName
                    if (pkg != context.packageName && !matches.contains(pkg)) {
                        matches.add(pkg)
                    }
                }
                if (matches.isNotEmpty()) return matches
            }
            "files", "storage", "filemanager", "myfiles", "documents" -> {
                val matches = mutableListOf<String>()
                for (pkg in KNOWN_FILE_MANAGERS) {
                    if (isInstalledOrFrozen(pkg) && !matches.contains(pkg)) matches.add(pkg)
                }
                if (matches.isNotEmpty()) return matches
            }
        }

        // Check if query directly matches a frozen package
        if (checkFrozenFirst) {
            val frozenMatch = prefs.frozenPackages.find {
                it.contains(q, ignoreCase = true)
            }
            if (frozenMatch != null) return listOf(frozenMatch)
        }

        // Fuzzy match installed packages
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installed) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (app.packageName.equals(q, ignoreCase = true) || label == q) {
                return listOf(app.packageName)
            }
        }
        for (app in installed) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (app.packageName.contains(q, ignoreCase = true) || label.contains(q)) {
                return listOf(app.packageName)
            }
        }

        return emptyList()
    }

    private fun isInstalledOrFrozen(pkg: String): Boolean {
        if (prefs.frozenPackages.contains(pkg)) return true
        return try {
            context.packageManager.getApplicationInfo(pkg, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
