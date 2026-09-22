package com.izhaanintellect.pasa.update

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.izhaanintellect.pasa.PasaApp
import com.izhaanintellect.pasa.R
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.network.PasaBackendApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtaUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pasaBackendApi: PasaBackendApi,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "PASA_OTA"
        private const val OTA_DIR = "ota"
    }

    data class UpdateCheckResult(
        val updateAvailable: Boolean,
        val versionName: String? = null,
        val versionCode: Int? = null,
        val changelog: String? = null,
        val fileSize: Long? = null,
        val downloadUrl: String? = null,
        val sha256: String? = null,
        val message: String? = null
    )

    data class UpdateResult(
        val success: Boolean,
        val message: String
    )

    private fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current version code", e)
            0
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val currentCode = getCurrentVersionCode()
            Log.i(TAG, "Checking for OTA update. Current versionCode: $currentCode")

            val response = pasaBackendApi.checkForUpdate(currentCode)

            if (response.ok && response.updateAvailable && response.latest != null) {
                Log.i(TAG, "Update available: v${response.latest.versionName} (code ${response.latest.versionCode})")
                UpdateCheckResult(
                    updateAvailable = true,
                    versionName = response.latest.versionName,
                    versionCode = response.latest.versionCode,
                    changelog = response.latest.changelog,
                    fileSize = response.latest.fileSize,
                    downloadUrl = response.latest.downloadUrl,
                    sha256 = response.latest.sha256
                )
            } else {
                Log.i(TAG, "App is up to date (versionCode: $currentCode)")
                UpdateCheckResult(
                    updateAvailable = false,
                    message = "App is up to date (v${getCurrentVersionName()}, code $currentCode)"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "OTA update check failed", e)
            UpdateCheckResult(
                updateAvailable = false,
                message = "Update check failed: ${e.message}"
            )
        }
    }

    suspend fun downloadAndInstall(downloadUrl: String, expectedSha256: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val otaDir = File(context.filesDir, OTA_DIR)
            if (!otaDir.exists()) otaDir.mkdirs()

            // Clean old OTA files
            otaDir.listFiles()?.forEach { it.delete() }

            val apkFile = File(otaDir, "pasa-update.apk")
            val fullUrl = if (downloadUrl.startsWith("http://", ignoreCase = true) || downloadUrl.startsWith("https://", ignoreCase = true)) {
                downloadUrl
            } else {
                val base = preferencesManager.serverUrl.trimEnd('/')
                val path = downloadUrl.trimStart('/')
                "$base/$path"
            }

            Log.i(TAG, "Downloading APK from: $fullUrl")

            // Download using OkHttp
            val client = OkHttpClient.Builder().build()
            val request = Request.Builder().url(fullUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext UpdateResult(false, "Download failed: HTTP ${response.code}")
            }

            response.body?.byteStream()?.use { input ->
                FileOutputStream(apkFile).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            } ?: return@withContext UpdateResult(false, "Download failed: empty response body")

            Log.i(TAG, "APK downloaded: ${apkFile.length()} bytes")

            // Verify SHA-256
            val digest = MessageDigest.getInstance("SHA-256")
            apkFile.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }

            if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                apkFile.delete()
                Log.e(TAG, "SHA-256 mismatch! Expected: $expectedSha256, Got: $actualSha256")
                return@withContext UpdateResult(false, "APK integrity check failed (SHA-256 mismatch)")
            }

            Log.i(TAG, "SHA-256 verified: $actualSha256")

            // Install
            if (PasaDeviceAdmin.isDeviceOwner(context)) {
                silentInstall(apkFile)
            } else {
                promptInstall(apkFile)
            }

            UpdateResult(true, "Update downloaded and install triggered")
        } catch (e: Exception) {
            Log.e(TAG, "OTA download/install failed", e)
            UpdateResult(false, "Update failed: ${e.message}")
        }
    }

    private fun promptInstall(apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(installIntent)
            Log.i(TAG, "Prompted user for APK installation directly")
        } catch (e: Exception) {
            Log.w(TAG, "Direct activity launch blocked or failed: ${e.message}")
        }

        // Always also post a high-priority notification with PendingIntent
        // Ensures user can tap to install even if Android 14-16 Background Activity Launch (BAL)
        // restrictions intercepted the direct activity launch when screen was locked/off
        try {
            val pendingIntent = PendingIntent.getActivity(
                context,
                2026,
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val nm = context.getSystemService(NotificationManager::class.java)
            val notif = NotificationCompat.Builder(context, PasaApp.ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("📦 PASA Update Ready")
                .setContentText("Tap here to complete installation of PASA update")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            nm?.notify(2026, notif)
            Log.i(TAG, "Posted high-priority update notification")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to post update notification: ${e.message}")
        }
    }

    private fun silentInstall(apkFile: File) {
        try {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            params.setAppPackageName(context.packageName)

            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)

            session.openWrite("pasa_ota", 0, apkFile.length()).use { out ->
                apkFile.inputStream().use { input ->
                    input.copyTo(out)
                }
                session.fsync(out)
            }

            val intent = Intent("com.izhaanintellect.pasa.OTA_INSTALL_RESULT").apply {
                setPackage(context.packageName)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, flags
            )

            // Pre-schedule an exact AlarmManager alarm to resurrect PasaService 10s after install
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                val watchdogIntent = Intent(context, com.izhaanintellect.pasa.detection.PasaWatchdogReceiver::class.java).apply {
                    action = com.izhaanintellect.pasa.detection.PasaWatchdogReceiver.ACTION_WATCHDOG_RESTART
                }
                val watchdogPending = PendingIntent.getBroadcast(
                    context,
                    998,
                    watchdogIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager?.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 10000L,
                    watchdogPending
                )
                Log.i(TAG, "Pre-scheduled watchdog resurrection alarm via AlarmManager in 10s")
            } catch (e: Exception) {
                Log.w(TAG, "Could not pre-schedule watchdog alarm: ${e.message}")
            }

            session.commit(pendingIntent.intentSender)
            Log.i(TAG, "Silent install session committed (Device Owner mode)")
        } catch (e: Exception) {
            Log.w(TAG, "Silent install failed, falling back to prompt install", e)
            promptInstall(apkFile)
        }
    }
}
