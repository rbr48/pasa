package com.izhaanintellect.pasa.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Detects SIM card replacements and reports network provider change to the owner.
 */
@AndroidEntryPoint
class SIMChangeReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var telegramApi: TelegramApi
    @Inject lateinit var locationTracker: LocationTracker

    companion object {
        private const val TAG = "PASA_SIM"
        private var lastSimState: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.SIM_STATE_CHANGED") return

        val simState = intent.getStringExtra("ss") ?: return
        if (simState != "LOADED" || simState == lastSimState) return
        lastSimState = simState

        Log.w(TAG, "SIM state changed to LOADED — inspecting provider")

        if (!preferencesManager.isConfigured()) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                // Compare current SIM with stored SIM
                var currentSimId: String? = null
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    currentSimId = telephonyManager.simSerialNumber ?: telephonyManager.subscriberId
                }
                
                val knownSimId = preferencesManager.knownSimId
                if (currentSimId != null && knownSimId != null && currentSimId == knownSimId) {
                    Log.i(TAG, "SIM state is LOADED but SIM ID matches known SIM. No alert needed.")
                    return@launch
                }
                
                // Update known SIM so we don't alert again for this same new SIM on next reboot
                if (currentSimId != null) {
                    preferencesManager.knownSimId = currentSimId
                } else if (knownSimId == null) {
                    // Both are null (no permission), can't reliably detect change without permission
                    // So we might still trigger once if we can't read it, but ideally we skip if we can't verify
                    Log.w(TAG, "Cannot read SIM ID (permission missing). Proceeding with alert as precaution.")
                }

                val operatorName = telephonyManager.networkOperatorName ?: "Unknown"
                val simOperator = telephonyManager.simOperatorName ?: "Unknown"
                val countryCode = telephonyManager.simCountryIso?.uppercase() ?: "Unknown"

                val locationStr = try {
                    val location = locationTracker.getCurrentLocation()
                    if (location != null) {
                        "\n📍 Location: ${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}" +
                                "\n🗺️ https://maps.google.com/maps?q=${location.latitude},${location.longitude}"
                    } else {
                        "\n📍 Location: Unavailable"
                    }
                } catch (e: Exception) {
                    "\n📍 Location: Error"
                }

                telegramApi.sendMessage(
                    token = preferencesManager.botToken,
                    request = SendMessageRequest(
                        chatId = preferencesManager.ownerChatIdLong,
                        text = """
                            🚨 <b>SIM CARD CHANGED DETECTED!</b>
                            
                            A SIM card event has occurred on your device:
                            📞 Operator: <b>$operatorName</b>
                            📱 Provider: <b>$simOperator</b>
                            🌍 Country: <b>$countryCode</b>
                            $locationStr
                            
                            ⚠️ If you did not perform this change, your device may be stolen.
                            Lock immediately using <code>/lock</code> or wipe with <code>/wipe</code>.
                        """.trimIndent()
                    )
                )

                Log.i(TAG, "SIM change alert dispatched to owner")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SIM change alert", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
