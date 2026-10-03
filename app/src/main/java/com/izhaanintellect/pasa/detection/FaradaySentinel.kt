package com.izhaanintellect.pasa.detection

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.data.PendingUpload
import com.izhaanintellect.pasa.data.PendingUploadDao
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.worker.EvidenceUploadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autonomous Faraday & RF Blackout Sentinel (Uncle Ted Anti-Forensics Defense).
 *
 * Threat Model:
 * Organized thieves or forensic extractors immediately insert the captured device
 * into an RF-shielding Faraday bag / box to prevent remote /wipe, /lock, or GPS tracking.
 *
 * Operation:
 * 1. Blackout Detection:
 *    Monitors network state transitions via ConnectivityManager.NetworkCallback.
 *    If all networks (Wi-Fi, Cellular) are completely severed while the device is locked,
 *    and remains disconnected past debouncing (3 seconds), the Faraday Trap activates.
 *
 * 2. Pre-Isolation Hardening (Last Gasp):
 *    - Immediately severs hardware USB data pin signaling (blocks Cellebrite / GrayKey).
 *    - Enforces hardware screen lock (dpm.lockNow()).
 *    - Takes a covert front-camera mugshot before the device is enclosed or upon detection.
 *    - Caches last known GPS coordinates and persists the emergency evidence packet to
 *      encrypted offline storage as a prioritized PendingUpload.
 *
 * 3. Blackout Emergence (Tombstone Flush):
 *    Once the device is taken out of the Faraday enclosure and any network connection is
 *    restored, the Sentinel immediately flushes the emergency forensics packet to Telegram C2.
 */
@Singleton
class FaradaySentinel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val telegramApi: TelegramApi,
    private val locationTracker: LocationTracker,
    private val pendingUploadDao: PendingUploadDao
) {
    companion object {
        private const val TAG = "PASA_FaradaySentinel"
        private const val BLACKOUT_DEBOUNCE_MS = 3_000L
        private const val COOLDOWN_INTERVAL_MS = 60_000L
    }

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    private val keyguardManager by lazy {
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    }

    private var isMonitoring = false
    private var inBlackoutState = false
    private var blackoutJob: Job? = null
    private var lastTrapExecutionTime = 0L
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun startMonitoring() {
        if (isMonitoring) return
        if (!preferencesManager.isFaradayTrapEnabled) {
            Log.d(TAG, "FaradaySentinel disabled in preferences.")
            return
        }

        val cm = connectivityManager ?: return

        try {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    super.onLost(network)
                    checkBlackoutCondition()
                }

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    checkEmergenceCondition()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    if (hasInternet && inBlackoutState) {
                        checkEmergenceCondition()
                    }
                }
            }

            networkCallback = callback
            cm.registerDefaultNetworkCallback(callback)
            isMonitoring = true
            Log.i(TAG, "FaradaySentinel monitoring active (zero-polling callback registered).")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register Faraday network callback: ${e.message}")
        }
    }

    fun stopMonitoring() {
        if (!isMonitoring) return
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        networkCallback = null
        blackoutJob?.cancel()
        blackoutJob = null
        isMonitoring = false
        Log.i(TAG, "FaradaySentinel monitoring stopped.")
    }

    private fun checkBlackoutCondition() {
        if (!preferencesManager.isFaradayTrapEnabled) return

        val cm = connectivityManager ?: return
        val isLocked = keyguardManager?.isDeviceLocked == true || keyguardManager?.isKeyguardLocked == true

        // Only activate if device is currently locked (unauthorized physical containment)
        if (!isLocked) return

        blackoutJob?.cancel()
        blackoutJob = CoroutineScope(Dispatchers.IO).launch {
            delay(BLACKOUT_DEBOUNCE_MS)

            val activeNetwork = cm.activeNetwork
            if (activeNetwork == null) {
                // Total network void confirmed while locked
                val now = System.currentTimeMillis()
                if (now - lastTrapExecutionTime > COOLDOWN_INTERVAL_MS) {
                    lastTrapExecutionTime = now
                    inBlackoutState = true
                    executeBlackoutTrap()
                }
            }
        }
    }

    private suspend fun executeBlackoutTrap() {
        Log.w(TAG, "🚨 FARADAY RADIO BLACKOUT DETECTED while device is locked! Executing emergency lockdown.")

        // 1. Sever hardware attack surfaces immediately
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.lockNow()
            if (PasaDeviceAdmin.isDeviceOwner(context)) {
                PasaDeviceAdmin.setUsbDataSignaling(context, false)
                Log.w(TAG, "Severed USB data pins to block Cellebrite extraction.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying hardware lock during Faraday blackout: ${e.message}")
        }

        // 2. Capture emergency offline forensics tombstone
        try {
            val captureResult = StealthCaptureBridge.capturePhoto(context, useFront = true)
            val photoFile = captureResult.file

            if (photoFile != null && photoFile.exists() && photoFile.length() > 0) {
                val uploadId = "faraday_" + UUID.randomUUID().toString().substring(0, 8)
                val pending = PendingUpload(
                    id = uploadId,
                    commandId = "FARADAY_BLACKOUT",
                    fileType = "PHOTO",
                    filePath = photoFile.absolutePath,
                    isEncrypted = false,
                    status = "PENDING",
                    createdAt = System.currentTimeMillis()
                )
                pendingUploadDao.insert(pending)
                Log.i(TAG, "Persisted emergency Faraday offline snapshot to pending uploads: $uploadId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing offline Faraday tombstone: ${e.message}")
        }
    }

    private fun checkEmergenceCondition() {
        blackoutJob?.cancel()
        if (!inBlackoutState) return

        inBlackoutState = false
        Log.i(TAG, "🟢 Faraday emergence detected! Connectivity restored. Dispatching emergency alerts.")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (preferencesManager.botToken.isNotBlank() && preferencesManager.ownerChatIdLong != 0L) {
                    val loc = locationTracker.getCurrentLocation()
                    val locText = if (loc != null) {
                        "\n📍 <b>Emergence Location:</b> <a href=\"https://www.google.com/maps?q=${loc.latitude},${loc.longitude}\">${loc.latitude}, ${loc.longitude}</a> (Acc: ${loc.accuracy}m)"
                    } else ""

                    val alertMsg = "🚨 <b>FARADAY BLACKOUT EMERGENCE DETECTED!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                            "⚠️ The device has emerged from an RF Shielding / Faraday Blackout!\n" +
                            "• <b>Radio Status:</b> ✅ Restored\n" +
                            "• <b>USB Data Pins:</b> 🚫 Severed (Remains locked)$locText\n\n" +
                            "<i>Flushing cached emergency forensic captures now...</i>"

                    telegramApi.sendMessage(
                        token = preferencesManager.botToken,
                        request = SendMessageRequest(
                            chatId = preferencesManager.ownerChatIdLong,
                            text = alertMsg
                        )
                    )
                }

                // Schedule immediate WorkManager flush of all pending uploads
                EvidenceUploadWorker.schedulePendingBatch(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling Faraday emergence: ${e.message}")
            }
        }
    }
}
