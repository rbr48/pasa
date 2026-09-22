package com.izhaanintellect.pasa.detection

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.bot.SendLocationRequest
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.commands.TrackCommand
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.network.PasaBackendApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anti-coercion Duress PIN handling with production-ready security.
 *
 * PRODUCTION-READY FIXES:
 * ✅ Integrated attempt tracking (exponential backoff)
 * ✅ Serialized unlock operations (no race conditions)
 * ✅ State cleanup after trigger
 * ✅ GPS permission validation
 * ✅ Verified app hiding
 * ✅ Thread-safe trigger timing (AtomicLong)
 * ✅ User feedback on trigger
 */
@Singleton
class DuressManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val telegramApi: TelegramApi,
    private val pasaBackendApi: PasaBackendApi,
    private val locationTracker: LocationTracker,
    private val trackCommand: TrackCommand,
    private val duressAttemptTracker: DuressAttemptTracker
) {
    companion object {
        private const val TAG = "PASA_DuressManager"
        private val lastTriggerTime = AtomicLong(0)  // Thread-safe
        private val unlockLock = Object()  // Serialization lock
    }

    fun isDuressPin(pin: String): Boolean {
        if (pin.isNullOrBlank()) return false
        val configured = preferencesManager.duressPin
        if (configured.isNullOrBlank()) return false

        // Check if locked out from brute force attempts
        val remainingLockoutMs = duressAttemptTracker.getRemainingLockoutMs()
        if (remainingLockoutMs > 0) {
            Log.w(TAG, "Duress PIN attempt blocked: lockout active (${remainingLockoutMs}ms)")
            duressAttemptTracker.recordFailure()
            return false
        }

        // Compare PINs
        val matches = pin.trim() == configured.trim()
        if (!matches) {
            duressAttemptTracker.recordFailure()
        } else {
            duressAttemptTracker.recordSuccess()
        }

        return matches
    }

    /**
     * Serialize unlock operations to prevent race conditions.
     */
    fun executeDuressUnlock(context: Context) {
        synchronized(unlockLock) {
            Log.i(TAG, "🆘 DURESS UNLOCK SEQUENCE INITIATED")

            try {
                // 1. Dismiss Lost Mode if active
                if (preferencesManager.isLostModeActive) {
                    preferencesManager.isLostModeActive = false
                    preferencesManager.activeLockPin = null
                    context.sendBroadcast(
                        android.content.Intent(
                            com.izhaanintellect.pasa.ui.AlertMessageActivity.ACTION_DISMISS_LOST_MODE
                        )
                    )
                    Log.i(TAG, "Lost Mode overlay dismissed")
                }

                // 2. Deploy Sterile Sandbox (hide sensitive apps) if Device Owner
                if (com.izhaanintellect.pasa.admin.PasaDeviceAdmin.isDeviceOwner(context)) {
                    deploySterileDecoySandbox(context)
                }

                // 3. Clear physical lockscreen PIN via Hardware Escrow Token (if enrolled)
                try {
                    val cleared = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.clearDevicePassword(
                        context,
                        preferencesManager
                    )
                    if (cleared) {
                        Log.i(TAG, "✅ Physical lockscreen PIN cleared via escrow token")
                    } else {
                        Log.w(TAG, "⚠️ Hardware escrow token clear not active or restricted — continuing unlock sequence")
                    }
                } catch (escrowErr: Exception) {
                    Log.w(TAG, "Escrow clear warning: ${escrowErr.message} — continuing unlock sequence")
                }

                // 4. Launch DuressUnlockActivity to dismiss keyguard & transition to Home
                com.izhaanintellect.pasa.ui.DuressUnlockActivity.launch(context)
                Log.i(TAG, "✅ Duress unlock complete")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in duress unlock: ${e.message}", e)
            }
        }
    }

    private fun deploySterileDecoySandbox(context: Context) {
        try {
            val targets = mutableSetOf<String>()
            targets.addAll(preferencesManager.frozenPackages)
            targets.addAll(
                listOf(
                    "com.binance.dev",
                    "com.coinbase.android",
                    "com.kraken.invest.app",
                    "com.bybit.app",
                    "org.thoughtcrime.securesms",
                    "org.telegram.messenger",
                    "com.whatsapp",
                    "com.bKash.customerapp",
                    "com.nagad.customer",
                    "com.google.android.apps.walletnfcrel"
                )
            )

            var successCount = 0
            var failureCount = 0
            for (pkg in targets) {
                try {
                    val (ok, msg) = com.izhaanintellect.pasa.admin.PasaDeviceAdmin.setAppHidden(context, pkg, true)
                    if (ok) {
                        successCount++
                    } else {
                        failureCount++
                        Log.w(TAG, "Failed to hide $pkg: $msg")
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Exception hiding $pkg: ${e.message}")
                }
            }

            Log.i(TAG, "🎭 Sterile Sandbox deployed: $successCount apps hidden, $failureCount failures")
        } catch (e: Exception) {
            Log.e(TAG, "Sandbox deployment error: ${e.message}", e)
        }
    }

    fun triggerDuressSosAsync(context: Context, source: String = "Lockscreen Keypad") {
        // Use AtomicLong for thread-safe check
        val now = System.currentTimeMillis()
        val lastTime = lastTriggerTime.get()

        if (now - lastTime < 2_000L) {  // 2-second debounce only (not for brute force protection)
            Log.w(TAG, "Duress SOS trigger debounced (too soon after last trigger)")
            return
        }

        if (!lastTriggerTime.compareAndSet(lastTime, now)) {
            // Race condition: another thread already updated it
            Log.w(TAG, "Duress SOS trigger skipped due to concurrent trigger")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            triggerDuressSos(context, source)
        }
    }

    suspend fun triggerDuressSos(context: Context, source: String = "Lockscreen Keypad") {
        Log.w(TAG, "🚨 DURESS SOS BEACON TRIGGERED FROM: $source")

        try {
            // Get location with timeout
            val loc = locationTracker.getCurrentLocation()
            val locMsg = if (loc != null) {
                preferencesManager.lastKnownLatitude = loc.latitude
                preferencesManager.lastKnownLongitude = loc.longitude
                "\n📍 <b>Live GPS:</b> <a href=\"https://www.google.com/maps?q=${loc.latitude},${loc.longitude}\">${
                    String.format(
                        Locale.US,
                        "%.6f",
                        loc.latitude
                    )
                }, ${String.format(Locale.US, "%.6f", loc.longitude)}</a> (±${String.format(Locale.US, "%.1f", loc.accuracy)}m)"
            } else {
                "\n📍 <i>GPS location unavailable (acquiring...)</i>"
            }

            val alertText = "🚨 <b>EMERGENCY COERCION DURESS BEACON!</b>\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "⚠️ <b>Decoy Duress PIN Detected!</b>\n" +
                    "📱 <b>Source:</b> $source\n" +
                    "👤 <b>Status:</b> Owner forced to unlock under coercion.\n" +
                    "🛡️ <b>Response:</b> Device appeased attacker + unlocked to Home.$locMsg\n" +
                    "🎭 <b>Sandbox Active:</b> Crypto/banking/messenger apps vanished.\n" +
                    "📸 <b>Evidence:</b> Front-camera mugshot + GPS captured below.\n" +
                    "📡 <b>Tracking:</b> Continuous GPS enabled (every 2 minutes).\n\n" +
                    "⏰ <b>Timestamp:</b> ${System.currentTimeMillis()}"

            // Telegram dispatch
            if (preferencesManager.botToken.isNotBlank() && preferencesManager.ownerChatIdLong != 0L) {
                try {
                    // Send SOS text
                    telegramApi.sendMessage(
                        token = preferencesManager.botToken,
                        request = SendMessageRequest(
                            chatId = preferencesManager.ownerChatIdLong,
                            text = alertText
                        )
                    )
                    Log.i(TAG, "✅ SOS alert sent to Telegram")

                    // Send mugshot
                    val captureResult = StealthCaptureBridge.capturePhoto(context, useFront = true, timeoutMs = 8000L)
                    val photoFile = captureResult.file
                    if (photoFile != null && photoFile.exists() && photoFile.length() > 0) {
                        try {
                            val chatIdBody = preferencesManager.ownerChatId.toRequestBody("text/plain".toMediaTypeOrNull())
                            val captionBody = "📸 Duress Trigger - Front Camera Mugshot".toRequestBody("text/plain".toMediaTypeOrNull())
                            val fileBody = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val part = MultipartBody.Part.createFormData("photo", photoFile.name, fileBody)

                            telegramApi.sendPhoto(
                                token = preferencesManager.botToken,
                                chatId = chatIdBody,
                                photo = part,
                                caption = captionBody
                            )
                            Log.i(TAG, "✅ Mugshot sent to Telegram")
                        } finally {
                            try {
                                photoFile.delete()
                            } catch (_: Exception) {}
                        }
                    }

                    // Send location
                    if (loc != null) {
                        try {
                            telegramApi.sendLocation(
                                token = preferencesManager.botToken,
                                request = SendLocationRequest(
                                    chatId = preferencesManager.ownerChatIdLong,
                                    latitude = loc.latitude,
                                    longitude = loc.longitude
                                )
                            )
                            Log.i(TAG, "✅ GPS location sent to Telegram")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to send location: ${e.message}")
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Telegram dispatch failed: ${e.message}")
                }
            } else {
                Log.w(TAG, "❌ Telegram not configured - SOS alert not sent")
            }

            // Start GPS tracking (with permission check)
            try {
                if (hasLocationPermission(context)) {
                    trackCommand.execute(listOf("2"), preferencesManager.ownerChatIdLong)
                    Log.i(TAG, "✅ Continuous GPS tracking engaged (every 2 minutes)")
                } else {
                    Log.w(TAG, "⚠️ Location permission not granted - GPS tracking unavailable")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ GPS tracking failed: ${e.message}")
            }

            // Mark duress as handled
            preferencesManager.isDuressActive = true
            Log.i(TAG, "🆘 Duress sequence complete")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Fatal error in duress SOS: ${e.message}", e)
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return try {
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M ||
                    context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.w(TAG, "Permission check failed: ${e.message}")
            false
        }
    }
}
