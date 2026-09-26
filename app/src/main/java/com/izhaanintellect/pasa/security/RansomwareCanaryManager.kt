package com.izhaanintellect.pasa.security

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RansomwareCanaryManager.
 * Deploys cryptographic canary tripwire files in high-value storage directories.
 * If mobile ransomware attempts to encrypt, overwrite, or delete user files,
 * the canary triggers an immediate emergency freeze across all third-party apps
 * via Knox Device Owner setPackagesSuspended, halting data loss in its tracks.
 */
@Singleton
class RansomwareCanaryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val telegramApi: TelegramApi
) {
    companion object {
        private const val TAG = "PASA_CanaryGuard"
        private const val CANARY_CONTENT = "PASA_SENTINEL_SECURITY_CANARY_TRIPWIRE_V1_DO_NOT_MODIFY"
    }

    private val observers = mutableListOf<FileObserver>()
    private var isArmed = false
    private val canaryFiles = mutableListOf<File>()

    private fun getExpectedHash(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(CANARY_CONTENT.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun armCanaryTrap(): Pair<Boolean, String> {
        if (!prefs.isCanaryGuardArmed) {
            prefs.isCanaryGuardArmed = true
        }

        canaryFiles.clear()
        val dirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        )

        var plantedCount = 0
        for (dir in dirs) {
            try {
                if (!dir.exists()) dir.mkdirs()
                val canary = File(dir, ".pasa_canary_integrity.dat")
                canary.writeText(CANARY_CONTENT)
                canaryFiles.add(canary)
                plantedCount++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to plant canary in ${dir.absolutePath}: ${e.message}")
            }
        }

        startFileObservers()
        isArmed = true
        return Pair(true, "Planted $plantedCount canary tripwires across Documents, Downloads, and Pictures.")
    }

    fun disarmCanaryTrap(): Pair<Boolean, String> {
        prefs.isCanaryGuardArmed = false
        stopFileObservers()
        isArmed = false
        return Pair(true, "Canary tripwire monitoring stopped.")
    }

    fun startMonitoring() {
        if (prefs.isCanaryGuardArmed) {
            armCanaryTrap()
        }
    }

    private fun startFileObservers() {
        stopFileObservers()

        val dirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        )

        for (dir in dirs) {
            if (!dir.exists()) continue
            try {
                val mask = FileObserver.MODIFY or FileObserver.DELETE or FileObserver.MOVED_FROM or FileObserver.MOVED_TO
                val observer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    object : FileObserver(dir, mask) {
                        override fun onEvent(event: Int, path: String?) {
                            checkEvent(event, dir, path)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    object : FileObserver(dir.absolutePath, mask) {
                        override fun onEvent(event: Int, path: String?) {
                            checkEvent(event, dir, path)
                        }
                    }
                }
                observer.startWatching()
                observers.add(observer)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to watch ${dir.absolutePath}: ${e.message}")
            }
        }
    }

    private fun stopFileObservers() {
        for (obs in observers) {
            try {
                obs.stopWatching()
            } catch (_: Exception) {}
        }
        observers.clear()
    }

    private fun checkEvent(event: Int, dir: File, path: String?) {
        if (path == null) return
        if (path.contains(".pasa_canary_integrity.dat")) {
            val canary = File(dir, path)
            var breached = false
            var reason = ""

            if (!canary.exists()) {
                breached = true
                reason = "Canary file deleted or moved"
            } else {
                try {
                    val content = canary.readText()
                    if (content != CANARY_CONTENT) {
                        breached = true
                        reason = "Canary content modified / encrypted"
                    }
                } catch (e: Exception) {
                    breached = true
                    reason = "Canary file corrupted / unreadable (${e.message})"
                }
            }

            if (breached) {
                triggerTripwireLockdown(canary.absolutePath, reason)
            }
        }
    }

    private fun triggerTripwireLockdown(filePath: String, reason: String) {
        Log.e(TAG, "🚨 RANSOMWARE TRIPWIRE TRIGGERED on $filePath: $reason")

        // 1. Suspend all third-party applications via Device Owner
        val suspended = if (PasaDeviceAdmin.isDeviceOwner(context)) {
            PasaDeviceAdmin.suspendAllThirdPartyApps(context, true)
        } else emptyList()

        // 2. Alert Telegram
        if (prefs.isConfigured()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alertText = "🚨 <b>RANSOMWARE TRIPWIRE TRIGGERED!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "⚠️ <b>Reason:</b> $reason\n" +
                            "📁 <b>Target:</b> <code>$filePath</code>\n\n" +
                            "🛑 <b>Emergency Action:</b> All ${suspended.size} third-party applications have been SUSPENDED by Knox Device Owner.\n" +
                            "🔒 <i>Mass encryption stopped dead in its tracks.</i>\n\n" +
                            "💡 <i>To verify and restore after inspection:</i>\n" +
                            "<code>/canary_guard &lt;master_password&gt; reset</code>"

                    telegramApi.sendMessage(
                        token = prefs.botToken,
                        request = SendMessageRequest(
                            chatId = prefs.ownerChatIdLong,
                            text = alertText
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send canary alert: ${e.message}")
                }
            }
        }
    }

    fun auditIntegrity(): Pair<Boolean, String> {
        var intact = 0
        var missingOrCorrupted = 0
        val sb = StringBuilder()

        for (canary in canaryFiles) {
            if (canary.exists() && canary.readText() == CANARY_CONTENT) {
                intact++
                sb.append("• <code>${canary.name}</code> in ${canary.parentFile?.name}: 🟢 INTACT\n")
            } else {
                missingOrCorrupted++
                sb.append("• <code>${canary.name}</code> in ${canary.parentFile?.name}: 🔴 BREACHED\n")
            }
        }

        val success = missingOrCorrupted == 0
        return Pair(success, sb.toString())
    }
}
