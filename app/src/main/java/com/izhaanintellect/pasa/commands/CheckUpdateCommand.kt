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
        @Volatile private var cachedUpdate: OtaUpdateManager.UpdateCheckResult? = null
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (args.firstOrNull()?.equals("confirm", ignoreCase = true) == true) {
            return confirmInstall()
        }

        Log.i(TAG, "Checking for OTA update via Telegram command")

        val currentVersion = otaUpdateManager.getCurrentVersionName()
        val checkResult = otaUpdateManager.checkForUpdate()

        if (!checkResult.updateAvailable || checkResult.downloadUrl == null || checkResult.sha256 == null) {
            cachedUpdate = null
            return CommandResult(
                success = true,
                message = "✅ <b>PASA is up to date</b>\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "📱 <b>Current Version:</b> v$currentVersion\n" +
                        "<i>No newer version available on the server.</i>"
            )
        }

        cachedUpdate = checkResult

        val sizeDisplay = checkResult.fileSize?.let {
            val mb = it / (1024.0 * 1024.0)
            String.format("%.1f MB", mb)
        } ?: "unknown"

        val changelogSection = if (!checkResult.changelog.isNullOrBlank()) {
            val safeChangelog = checkResult.changelog
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("&amp;lt;", "&lt;")
                .replace("&amp;gt;", "&gt;")
                .replace("&amp;amp;", "&amp;")
            "\n📋 <b>Changelog:</b> $safeChangelog"
        } else ""

        return CommandResult(
            success = true,
            message = "🔄 <b>OTA Update Available</b>\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "📱 <b>Current:</b> v$currentVersion\n" +
                    "🆕 <b>New:</b> v${checkResult.versionName} (code ${checkResult.versionCode})\n" +
                    "📦 <b>Size:</b> $sizeDisplay\n" +
                    "🔒 <b>SHA-256:</b> <code>${checkResult.sha256.take(16)}...</code>$changelogSection\n\n" +
                    "<i>To proceed with download & installation, send:</i>\n" +
                    "<code>/update_confirm</code>"
        )
    }

    suspend fun confirmInstall(): CommandResult {
        var update = cachedUpdate
        if (update == null) {
            val check = otaUpdateManager.checkForUpdate()
            if (check.updateAvailable && check.downloadUrl != null && check.sha256 != null) {
                update = check
                cachedUpdate = update
            }
        }

        if (update == null || update.downloadUrl.isNullOrBlank() || update.sha256.isNullOrBlank()) {
            return CommandResult(
                success = false,
                message = "🔄 <b>No Pending Update</b>\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "Please run <code>/check_update</code> first."
            )
        }

        var installResult = otaUpdateManager.downloadAndInstall(update.downloadUrl!!, update.sha256!!)

        // If download failed (e.g. 404 or stale link), invalidate cache and retry ONCE with fresh server metadata
        if (!installResult.success) {
            Log.w(TAG, "Install failed with cached URL (${installResult.message}), re-querying latest release...")
            cachedUpdate = null
            val freshCheck = otaUpdateManager.checkForUpdate()
            if (freshCheck.updateAvailable && !freshCheck.downloadUrl.isNullOrBlank() && !freshCheck.sha256.isNullOrBlank()) {
                update = freshCheck
                cachedUpdate = freshCheck
                installResult = otaUpdateManager.downloadAndInstall(freshCheck.downloadUrl, freshCheck.sha256)
            }
        }

        return if (installResult.success) {
            cachedUpdate = null
            CommandResult(
                success = true,
                message = "🔄 <b>Installing Update v${update.versionName}</b>\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "✅ APK downloaded and SHA-256 verified.\n" +
                        "📲 Package installer triggered on device."
            )
        } else {
            cachedUpdate = null
            CommandResult(
                success = false,
                message = "❌ <b>Installation Failed</b>\n" +
                        "━━━━━━━━━━━━━━━━━━━━\n" +
                        "Error: ${installResult.message}"
            )
        }
    }
}
