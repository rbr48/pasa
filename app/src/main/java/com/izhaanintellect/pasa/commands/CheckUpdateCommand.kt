package com.izhaanintellect.pasa.commands

import android.util.Log
import com.izhaanintellect.pasa.update.OtaUpdateManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckUpdateCommand @Inject constructor(
    private val otaUpdateManager: OtaUpdateManager
) : Command {

    override val name = "/check_update"
    override val description = "Check for OTA app updates"
    override val usage = "/check_update"

    companion object {
        private const val TAG = "PASA_CheckUpdate"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        Log.i(TAG, "Checking for OTA update via Telegram command")

        val currentVersion = otaUpdateManager.getCurrentVersionName()
        val checkResult = otaUpdateManager.checkForUpdate()

        if (!checkResult.updateAvailable) {
            return CommandResult(
                success = true,
                message = "✅ <b>PASA is up to date</b>\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "📱 <b>Current Version:</b> v$currentVersion\n" +
                        "<i>No newer version available on the server.</i>"
            )
        }

        val sizeDisplay = checkResult.fileSize?.let {
            val mb = it / (1024.0 * 1024.0)
            String.format("%.1f MB", mb)
        } ?: "unknown"

        val changelogSection = if (!checkResult.changelog.isNullOrBlank()) {
            "\n📋 <b>Changelog:</b> ${checkResult.changelog}"
        } else ""

        // Auto-download and install
        val downloadUrl = checkResult.downloadUrl
        val sha256 = checkResult.sha256

        if (downloadUrl != null && sha256 != null) {
            val installResult = otaUpdateManager.downloadAndInstall(downloadUrl, sha256)

            return if (installResult.success) {
                CommandResult(
                    success = true,
                    message = "🔄 <b>OTA Update Available & Installing!</b>\n" +
                            "━━━━━━━━━━━━━━━━━━━━\n" +
                            "📱 <b>Current:</b> v$currentVersion\n" +
                            "🆕 <b>New:</b> v${checkResult.versionName} (code ${checkResult.versionCode})\n" +
                            "📦 <b>Size:</b> $sizeDisplay\n" +
                            "🔒 <b>SHA-256:</b> <code>${sha256.take(16)}...</code>$changelogSection\n\n" +
                            "✅ APK downloaded, verified, and install triggered."
                )
            } else {
                CommandResult(
                    success = false,
                    message = "🔄 <b>Update Found But Install Failed</b>\n" +
                            "━━━━━━━━━━━━━━━━━━━━\n" +
                            "🆕 v${checkResult.versionName} (code ${checkResult.versionCode})\n" +
                            "❌ ${installResult.message}"
                )
            }
        }

        return CommandResult(
            success = true,
            message = "🔄 <b>Update Available</b>\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "📱 <b>Current:</b> v$currentVersion\n" +
                    "🆕 <b>New:</b> v${checkResult.versionName} (code ${checkResult.versionCode})\n" +
                    "📦 <b>Size:</b> $sizeDisplay$changelogSection"
        )
    }
}
