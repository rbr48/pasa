package com.izhaanintellect.pasa.commands

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lists installed applications and launches uninstallation prompts.
 */
@Singleton
class AppManageCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/apps"
    override val description = "List installed user applications with pagination & search"
    override val usage = "/apps [page_num] | /apps all | /apps search <query> | /app_uninstall <package>"

    companion object {
        private const val PAGE_SIZE = 35
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val firstArg = args.firstOrNull()?.lowercase()?.trim()

        // 1. Explicit or package uninstallation
        if (firstArg == "uninstall") {
            val pkg = args.getOrNull(1)?.trim()
            return if (!pkg.isNullOrBlank()) {
                uninstallApp(pkg)
            } else {
                CommandResult(success = false, message = "❌ Missing package name. Usage: <code>/app_uninstall &lt;package&gt;</code>")
            }
        } else if (firstArg != null && firstArg.contains(".") && !firstArg.startsWith("page")) {
            // Invoked with package name: /app_uninstall com.example.app
            return uninstallApp(args[0].trim())
        }

        val pm = context.packageManager
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        // 2. Full text inventory export
        if (firstArg == "all" || firstArg == "export" || firstArg == "file" || firstArg == "full") {
            return exportAllApps(allApps, pm)
        }

        // 3. Search query
        if (firstArg == "search" || firstArg == "find") {
            val query = args.drop(1).joinToString(" ").lowercase().trim()
            if (query.isBlank()) {
                return CommandResult(success = false, message = "❌ Please specify search keyword. Usage: <code>/apps search &lt;name&gt;</code>")
            }
            val matches = allApps.filter { app ->
                val label = pm.getApplicationLabel(app).toString().lowercase()
                label.contains(query) || app.packageName.lowercase().contains(query)
            }
            return renderAppList(matches, pm, page = 1, totalMatches = matches.size, isSearch = true, query = query)
        }

        // 4. Paginated list
        val targetPage = when {
            firstArg?.toIntOrNull() != null -> firstArg.toInt()
            firstArg == "page" && args.size > 1 -> args[1].toIntOrNull() ?: 1
            else -> 1
        }

        return renderAppList(allApps, pm, page = targetPage, totalMatches = allApps.size, isSearch = false)
    }

    private fun renderAppList(
        apps: List<ApplicationInfo>,
        pm: PackageManager,
        page: Int,
        totalMatches: Int,
        isSearch: Boolean,
        query: String = ""
    ): CommandResult {
        if (apps.isEmpty()) {
            return if (isSearch) {
                CommandResult(success = true, message = "🔍 No installed user apps matching \"<b>$query</b>\".")
            } else {
                CommandResult(success = true, message = "📦 No user applications installed.")
            }
        }

        val totalPages = maxOf(1, (apps.size + PAGE_SIZE - 1) / PAGE_SIZE)
        val validPage = page.coerceIn(1, totalPages)
        val startIndex = (validPage - 1) * PAGE_SIZE
        val pagedApps = apps.drop(startIndex).take(PAGE_SIZE)

        val sb = StringBuilder()
        if (isSearch) {
            sb.appendLine("🔍 <b>App Search Results ($totalMatches matches)</b>")
        } else {
            sb.appendLine("📦 <b>Installed User Apps (${apps.size})</b> — Page $validPage of $totalPages")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        pagedApps.forEachIndexed { idx, app ->
            val num = startIndex + idx + 1
            val label = pm.getApplicationLabel(app)
            sb.appendLine("$num. <b>$label</b>")
            sb.appendLine("   <code>${app.packageName}</code>")
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        val navItems = mutableListOf<String>()
        if (validPage > 1) {
            navItems.add("👈 <code>/apps ${validPage - 1}</code>")
        }
        if (validPage < totalPages) {
            navItems.add("👉 <code>/apps ${validPage + 1}</code>")
        }
        if (navItems.isNotEmpty()) {
            sb.appendLine("📄 <b>Page $validPage/$totalPages:</b> ${navItems.joinToString(" • ")}")
        }

        sb.appendLine("💾 Full list: <code>/apps export</code> • 🔍 Search: <code>/apps search &lt;name&gt;</code>")
        sb.appendLine("🗑️ To remove: <code>/app_uninstall &lt;package&gt;</code>")

        return CommandResult(success = true, message = sb.toString())
    }

    private fun exportAllApps(apps: List<ApplicationInfo>, pm: PackageManager): CommandResult {
        return try {
            val file = java.io.File(context.cacheDir, "pasa_installed_apps_${System.currentTimeMillis()}.txt")
            file.printWriter().use { out ->
                out.println("================================================================================")
                out.println("🛡️ PASA Sentinel — Complete Installed User Applications Inventory")
                out.println("Total Applications: ${apps.size}")
                out.println("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
                out.println("================================================================================")
                out.println()
                apps.forEachIndexed { i, app ->
                    val label = pm.getApplicationLabel(app)
                    val ver = try {
                        val pi = pm.getPackageInfo(app.packageName, 0)
                        "${pi.versionName ?: "N/A"} (${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pi)})"
                    } catch (_: Exception) {
                        "Unknown"
                    }
                    out.println("${i + 1}. $label")
                    out.println("   Package: ${app.packageName}")
                    out.println("   Version: $ver")
                    out.println()
                }
            }

            CommandResult(
                success = true,
                message = "📋 <b>Complete Apps Inventory (${apps.size} apps)</b>\n━━━━━━━━━━━━━━━━━━━━\nFull text catalog generated and attached below.",
                documentFile = file
            )
        } catch (e: Exception) {
            CommandResult(success = false, message = "❌ Failed to export apps catalog: ${e.message}")
        }
    }

    private fun uninstallApp(packageName: String): CommandResult {
        return try {
            try {
                context.packageManager.getPackageInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                return CommandResult(success = false, message = "❌ Package not found: <code>$packageName</code>")
            }

            if (com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(context)) {
                val (ok, text) = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.silentUninstall(context, packageName)
                return CommandResult(success = ok, message = text)
            }

            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(
                success = true,
                message = "🗑️ Uninstallation dialog prompted on device for: <code>$packageName</code>\n(Tip: Grant Device Owner for 100% silent removal)"
            )
        } catch (e: Exception) {
            CommandResult(success = false, message = "❌ Failed to initiate uninstall: ${e.message}")
        }
    }
}
