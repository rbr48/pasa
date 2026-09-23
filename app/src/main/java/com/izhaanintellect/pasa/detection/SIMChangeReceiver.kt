package com.izhaanintellect.pasa.detection

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.bot.SendLocationRequest
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import com.izhaanintellect.pasa.network.PasaBackendApi
import com.izhaanintellect.pasa.ui.AlertMessageActivity
import com.izhaanintellect.pasa.util.SecurityActivityLauncher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

/**
 * Detects SIM card replacements, removals, and tampering events.
 *
 * Automatically triggers covert front-camera mugshot capture and dispatches
 * emergency location alerts when a SIM is ejected or replaced.
 */
@AndroidEntryPoint
class SIMChangeReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var telegramApi: TelegramApi
    @Inject lateinit var locationTracker: LocationTracker
    @Inject lateinit var pasaBackendApi: PasaBackendApi

    companion object {
        private const val TAG = "PASA_SIM"
        private var lastSimState: String? = null
    }

    /**
     * Retrieves the device IMEI. Requires Device Owner or READ_PRIVILEGED_PHONE_STATE on Android 10+.
     * Falls back to ANDROID_ID if IMEI is unavailable.
     */
    private fun getDeviceImei(context: Context): String? {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tm?.imei ?: tm?.getImei(0)
            } else {
                @Suppress("DEPRECATION")
                tm?.deviceId
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot read IMEI (requires Device Owner or privileged permission): ${e.message}")
            try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (_: Exception) { null }
        }
    }

    /**
     * Retrieves the phone number associated with the active SIM card.
     * Uses SubscriptionManager on Android 13+, SubscriptionInfo on Android 5.1-12, or TelephonyManager.
     */
    private fun getPhoneNumber(context: Context): String? {
        return try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            var num: String? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                num = subManager?.getPhoneNumber(SubscriptionManager.getDefaultSubscriptionId())
            }
            if (num.isNullOrBlank()) {
                val activeList = subManager?.activeSubscriptionInfoList
                num = activeList?.firstOrNull()?.number
            }
            if (num.isNullOrBlank()) {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                @Suppress("DEPRECATION")
                num = tm?.line1Number
            }
            if (!num.isNullOrBlank()) num else null
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot read phone number: ${e.message}")
            null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.SIM_STATE_CHANGED") return

        val simState = intent.getStringExtra("ss") ?: return
        if (simState == lastSimState) return
        lastSimState = simState

        if (!preferencesManager.isConfigured()) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (simState == "ABSENT") {
                    Log.w(TAG, "🚨 CRITICAL: SIM card removed / ejected!")
                    handleSimRemoved(context)
                } else if (simState == "LOADED") {
                    Log.i(TAG, "SIM state changed to LOADED — inspecting provider")
                    handleSimLoaded(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SIM change event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleSimRemoved(context: Context) {
        // 1. Instant Knox Kiosk Lock & Anti-Tamper Hardening
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = PasaDeviceAdmin.getComponentName(context)
        var doHardened = false
        val lockMsg = "🚨 TAMPER GUARD: SIM CARD EJECTED!\nDevice locked down in Knox Kiosk Lost Mode.\nOwner alerted. Remote-only unlock via Telegram /unlock or SMS."

        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            try {
                if (PasaDeviceAdmin.isDeviceOwner(context)) {
                    // Whitelist package for lock task kiosk mode
                    PasaDeviceAdmin.configureLockTask(context)
                    // Block factory reset, airplane mode, safe boot, media mount
                    PasaDeviceAdmin.setComprehensiveLockdown(context, true)
                    PasaDeviceAdmin.setUninstallBlocked(context, true)
                    // Forcibly power on GNSS hardware chip to track phone
                    PasaDeviceAdmin.forceLocationHardware(context, true)
                    // Lockout notification shade to prevent turning on airplane mode
                    try { dpm.setStatusBarDisabled(adminComponent, true) } catch (_: Exception) {}
                    // Disable keyguard biometrics to force master password or remote unlock
                    try {
                        dpm.setKeyguardDisabledFeatures(
                            adminComponent,
                            DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT or
                            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS or
                                DevicePolicyManager.KEYGUARD_DISABLE_FACE
                            } else { 0 })
                        )
                    } catch (_: Exception) {}
                    try { dpm.setDeviceOwnerLockScreenInfo(adminComponent, lockMsg) } catch (_: Exception) {}
                    doHardened = true
                }

                // Activate Lost Mode state
                preferencesManager.isLostModeActive = true
                preferencesManager.lostModeMessage = lockMsg

                // Launch full-screen Kiosk Lost Mode Activity (which enters startLockTask())
                val alertIntent = AlertMessageActivity.createIntent(
                    context = context,
                    message = lockMsg,
                    enforcePin = false
                )
                SecurityActivityLauncher.launch(
                    context = context,
                    intent = alertIntent,
                    notificationId = AlertMessageActivity.NOTIFICATION_ID,
                    notificationTitle = "🚨 SIM EJECTED: KIOSK LOCKED",
                    notificationText = lockMsg,
                    wakeScreen = true,
                    ongoing = true,
                    silentNotification = false
                )

                // Brief pause before locking keyguard
                delay(250)
                dpm.lockNow()
                Log.i(TAG, "Device screen locked immediately and Knox Kiosk Lost Mode engaged upon physical SIM ejection")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enforce lock/restrictions on SIM removal: ${e.message}")
            }
        }

        // 2. Capture IMEI / device identifier
        val imei = getDeviceImei(context)
        val imeiStr = if (imei != null) "\n📱 <b>Device IMEI:</b> <code>$imei</code>" else ""

        // 3. Capture covert front-camera snapshot with retry mechanism
        var mugshot: java.io.File? = null
        for (attempt in 1..3) {
            mugshot = try {
                StealthCaptureBridge.capturePhoto(context, useFront = true)?.file
            } catch (e: Exception) {
                Log.w(TAG, "Mugshot capture attempt $attempt failed: ${e.message}")
                null
            }
            if (mugshot != null && mugshot.exists() && mugshot.length() > 0) break
            if (attempt < 3) {
                delay(1500) // Wait before retry
            }
        }

        // 4. Get current location
        val locationStr = try {
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                "\n📍 Last GPS Fix: ${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}" +
                        "\n🗺️ https://maps.google.com/maps?q=${location.latitude},${location.longitude}"
            } else {
                "\n📍 Location: Satellite fix pending"
            }
        } catch (_: Exception) {
            "\n📍 Location: Unavailable"
        }

        val locPair = try {
            locationTracker.getCurrentLocation()?.let { Pair(it.latitude, it.longitude) }
        } catch (_: Exception) { null }

        val hardenedInfo = if (doHardened) {
            "\n🔒 <b>Countermeasure:</b> Knox Kiosk Lost Mode engaged, GNSS hardware turned ON, Quick Settings & Status Bar locked."
        } else {
            "\n🔒 <b>Countermeasure:</b> Device screen locked instantly."
        }

        val alertText = """
            🚨 <b>TAMPER ALERT: SIM CARD EJECTED!</b>
            ━━━━━━━━━━━━━━━━━━━━
            The physical SIM card was just removed from your device.$imeiStr$hardenedInfo
            
            📸 Front-camera mugshot capture initiated.
            $locationStr
            
            ⚠️ <i>If you did not eject your SIM, your phone has been stolen!</i>
            Lock immediately using <code>/lock</code> or blackout with <code>/fakeshutdown</code>.
        """.trimIndent()

        dispatchSimAlert(alertText, mugshot, locPair)
    }

    private suspend fun handleSimLoaded(context: Context) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Wrap in try-catch to prevent SecurityException crash on Android 10+
        var currentSimId: String? = null
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            currentSimId = try {
                telephonyManager.simSerialNumber ?: telephonyManager.subscriberId
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot read SIM serial (Android 10+ restriction): ${e.message}")
                "${telephonyManager.simOperatorName ?: "unknown"}_${telephonyManager.simCountryIso ?: "unknown"}"
            }
        }

        val knownSimId = preferencesManager.knownSimId
        val whitelist = preferencesManager.simLockWhitelist

        // Check if SIM is already authorized or whitelisted
        val isWhitelisted = currentSimId != null && (whitelist.contains(currentSimId) || (knownSimId != null && currentSimId == knownSimId))
        if (isWhitelisted) {
            Log.i(TAG, "SIM state is LOADED and matches authorized SIM. No alert needed.")
            return
        }

        // If this is the very first time SIM is detected and knownSimId was empty, enroll it
        if (knownSimId == null && currentSimId != null) {
            preferencesManager.knownSimId = currentSimId
            Log.i(TAG, "Enrolled initial SIM identity: $currentSimId")
            return
        }

        // --- UNAUTHORIZED / FOREIGN SIM INSERTED ---
        Log.w(TAG, "🚨 CRITICAL: Foreign / Unauthorized SIM card inserted into device!")

        // 1. Capture IMEI and phone number
        val imei = getDeviceImei(context) ?: "Unknown"
        val phoneNumber = getPhoneNumber(context)

        val operatorName = telephonyManager.networkOperatorName ?: "Unknown"
        val simOperator = telephonyManager.simOperatorName ?: "Unknown"
        val countryCode = telephonyManager.simCountryIso?.uppercase() ?: "Unknown"

        // 2. Fetch GNSS location
        val location = try { locationTracker.getCurrentLocation() } catch (_: Exception) { null }
        val locationStr = if (location != null) {
            "\n📍 Location: ${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}" +
                    "\n🗺️ https://maps.google.com/maps?q=${location.latitude},${location.longitude}"
        } else {
            "\n📍 Location: Pending satellite fix"
        }
        val locUrl = if (location != null) "https://maps.google.com/maps?q=${location.latitude},${location.longitude}" else "Pending fix"
        val locPair = location?.let { Pair(it.latitude, it.longitude) }

        // 3. SILENT OUTBOUND EMERGENCY SMS TO OWNER
        val emergencyPhone = preferencesManager.emergencyPhone.trim()
        var smsAlertStatus = "⚠️ Not configured (Set with /sim_lock phone &lt;number&gt;)"
        if (emergencyPhone.isNotBlank()) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                } else {
                    SmsManager.getDefault()
                }

                val smsText = "🚨 PASA ALERT: Foreign SIM detected in stolen phone!\n" +
                        "IMEI: $imei\n" +
                        "Carrier: $operatorName ($countryCode)\n" +
                        (if (!phoneNumber.isNullOrBlank()) "SIM Number: $phoneNumber\n" else "") +
                        "GPS: $locUrl\n" +
                        "Reply: PASA <PIN> /locate or /call or /lock"

                val parts = smsManager.divideMessage(smsText)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(emergencyPhone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(emergencyPhone, null, smsText, null, null)
                }
                smsAlertStatus = "✅ Sent to <code>$emergencyPhone</code> (Owner sees thief's Caller ID!)"
                Log.i(TAG, "Emergency outbound SMS successfully dispatched to $emergencyPhone")
            } catch (se: Exception) {
                smsAlertStatus = "❌ Failed to send SMS: ${se.message}"
                Log.e(TAG, "Failed to send emergency outbound SMS: ${se.message}", se)
            }
        }

        // 4. Countermeasure Action (Lock, Kiosk, or Wipe)
        val action = preferencesManager.simLockAlertAction // "lock", "alert", "wipe"
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = PasaDeviceAdmin.getComponentName(context)

        if (action == "wipe") {
            try {
                Log.w(TAG, "🚨 CRITICAL: Executing emergency wipe on foreign SIM insertion")
                dpm?.wipeData(0)
            } catch (we: Exception) {
                Log.e(TAG, "Failed to wipe on foreign SIM: ${we.message}")
            }
        } else if (action == "lock" || preferencesManager.isSimLockEnabled) {
            try {
                if (dpm != null && dpm.isAdminActive(adminComponent)) {
                    if (PasaDeviceAdmin.isDeviceOwner(context)) {
                        PasaDeviceAdmin.configureLockTask(context)
                        PasaDeviceAdmin.setComprehensiveLockdown(context, true)
                        PasaDeviceAdmin.setUninstallBlocked(context, true)
                        PasaDeviceAdmin.forceLocationHardware(context, true)
                        try { dpm.setStatusBarDisabled(adminComponent, true) } catch (_: Exception) {}
                        try {
                            dpm.setKeyguardDisabledFeatures(
                                adminComponent,
                                DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT or
                                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS or
                                    DevicePolicyManager.KEYGUARD_DISABLE_FACE
                                } else { 0 })
                            )
                        } catch (_: Exception) {}
                    }
                    val lockMsg = "🚨 UNAUTHORIZED SIM DETECTED!\nDevice locked down by PASA Sentinel.\nRemote-only unlock via Telegram or SMS."
                    preferencesManager.isLostModeActive = true
                    preferencesManager.lostModeMessage = lockMsg

                    val alertIntent = AlertMessageActivity.createIntent(
                        context = context,
                        message = lockMsg,
                        enforcePin = false
                    )
                    SecurityActivityLauncher.launch(
                        context = context,
                        intent = alertIntent,
                        notificationId = AlertMessageActivity.NOTIFICATION_ID,
                        notificationTitle = "🚨 FOREIGN SIM: KIOSK LOCKED",
                        notificationText = lockMsg,
                        wakeScreen = true,
                        ongoing = true,
                        silentNotification = false
                    )
                    delay(250)
                    dpm.lockNow()
                    Log.i(TAG, "Device Knox Kiosk locked due to foreign SIM detection")
                }
            } catch (le: Exception) {
                Log.w(TAG, "Failed to lock on foreign SIM: ${le.message}")
            }
        }

        // 5. Capture mugshot of perpetrator inserting SIM
        var mugshot: java.io.File? = null
        for (attempt in 1..3) {
            mugshot = try {
                StealthCaptureBridge.capturePhoto(context, useFront = true)?.file
            } catch (e: Exception) {
                Log.w(TAG, "SIM swap mugshot attempt $attempt failed: ${e.message}")
                null
            }
            if (mugshot != null && mugshot.exists() && mugshot.length() > 0) break
            if (attempt < 3) {
                delay(1500)
            }
        }

        val imeiStr = "\n📱 <b>Device IMEI:</b> <code>$imei</code>"
        val phoneStr = if (!phoneNumber.isNullOrBlank()) "\n📞 <b>New Number (SIM):</b> <code>$phoneNumber</code>" else "\n📞 <b>New Number:</b> <i>Unavailable (carrier restricted)</i>"

        val alertText = """
            🚨 <b>TAMPER ALERT: NEW SIM CARD DETECTED!</b>
            ━━━━━━━━━━━━━━━━━━━━$imeiStr$phoneStr
            🏢 <b>New Carrier:</b> $operatorName ($countryCode)
            📱 <b>Provider:</b> $simOperator
            💬 <b>Emergency Outbound SMS:</b> $smsAlertStatus
            📸 Front-camera mugshot capture initiated.
            $locationStr
            
            ⚠️ <i>A foreign SIM card has been inserted into your device. If you did not do this, your phone has been compromised!</i>
            Lock immediately: <code>/lock</code> | Blackout: <code>/fakeshutdown</code> | Wipe: <code>/wipe</code>
        """.trimIndent()

        dispatchSimAlert(alertText, mugshot, locPair)
    }

    private suspend fun dispatchSimAlert(
        message: String,
        photo: java.io.File? = null,
        location: Pair<Double, Double>? = null
    ) {
        // Direct Telegram dispatch (Strategy 1: Zero-Storage, zero server media persistence)
        if (preferencesManager.botToken.isNotBlank() && preferencesManager.ownerChatIdLong != 0L) {
            try {
                telegramApi.sendMessage(
                    token = preferencesManager.botToken,
                    request = SendMessageRequest(
                        chatId = preferencesManager.ownerChatIdLong,
                        text = message
                    )
                )
                if (photo != null && photo.exists() && photo.length() > 0) {
                    val mediaType = "image/jpeg".toMediaTypeOrNull()
                    val requestBody = photo.asRequestBody(mediaType)
                    val photoPart = MultipartBody.Part.createFormData("photo", photo.name, requestBody)
                    val chatIdPart = preferencesManager.ownerChatIdLong.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val captionPart = "📸 Thief mugshot captured upon SIM event".toRequestBody("text/plain".toMediaTypeOrNull())
                    telegramApi.sendPhoto(preferencesManager.botToken, chatIdPart, photoPart, captionPart)
                }
                if (location != null) {
                    try {
                        telegramApi.sendLocation(
                            token = preferencesManager.botToken,
                            request = SendLocationRequest(
                                chatId = preferencesManager.ownerChatIdLong,
                                latitude = location.first,
                                longitude = location.second
                            )
                        )
                    } catch (locErr: Exception) {
                        Log.w(TAG, "Failed to send SIM location pin: ${locErr.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dispatch SIM alert via Telegram: ${e.message}")
            } finally {
                // Immediately shred local mugshot photo
                try {
                    photo?.let { if (it.exists()) it.delete() }
                } catch (_: Exception) {}
            }
        }
    }
}
