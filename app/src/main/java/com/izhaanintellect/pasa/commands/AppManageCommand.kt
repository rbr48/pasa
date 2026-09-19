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
    override val description = "List installed user applications"
    override val usage = "/apps | /app_uninstall <package_name>"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val firstArg = args.firstOrNull()?.lowercase()

        if (firstArg == "uninstall") {
            if (args.size > 1) {
                return uninstallApp(args[1])
            } else {
                return CommandResult(success = false, message = "❌ Missing package name. Usage: /apps uninstall <package_name>")
            }
        } else if (firstArg != null) {
            // Assume invoked as /app_uninstall <package_name>
            return uninstallApp(args[0])
        }

        return listInstalledApps()
    }

    private fun listInstalledApps(): CommandResult {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        val sb = StringBuilder()
        sb.appendLine("📦 <b>Installed User Apps (${apps.size})</b>")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        apps.take(40).forEachIndexed { i, app ->
            val label = pm.getApplicationLabel(app)
            sb.appendLine("${i + 1}. <b>$label</b>")
            sb.appendLine("   <code>${app.packageName}</code>")
        }

        if (apps.size > 40) {
            sb.appendLine("\n<i>...and ${apps.size - 40} more apps</i>")
        }

        sb.appendLine()
        sb.appendLine("💡 To remove an app: <code>/app_uninstall &lt;package&gt;</code>")

        return CommandResult(success = true, message = sb.toString())
    }

    private fun uninstallApp(packageName: String): CommandResult {
        return try {
            try {
                context.packageManager.getPackageInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                return CommandResult(success = false, message = "❌ Package not found: <code>$packageName</code>")
            }

            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(
                success = true,
                message = "🗑️ Uninstallation dialog prompted on device for: <code>$packageName</code>"
            )
        } catch (e: Exception) {
            CommandResult(success = false, message = "❌ Failed to initiate uninstall: ${e.message}")
        }
    }
}
