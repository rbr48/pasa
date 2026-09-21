package com.izhaanintellect.pasa.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.KeyguardManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.network.PasaBackendApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

/**
 * Monitors Wi-Fi state changes and network connectivity events.
 * Alerts the owner when Wi-Fi is disabled while the device is locked
 * (potential anti-tracking behaviour by a thief).
 */
@AndroidEntryPoint
class NetworkChangeReceiver : BroadcastReceiver() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var telegramApi: TelegramApi
    @Inject lateinit var pasaBackendApi: PasaBackendApi

    companion object {
        private const val TAG = "PASA_NetChange"
        private var lastAlertTime = 0L
        private const val MIN_ALERT_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!preferencesManager.isConfigured()) return

        val now = System.currentTimeMillis()
        if (now - lastAlertTime < MIN_ALERT_INTERVAL_MS) return

        when (intent.action) {
            WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                if (state == WifiManager.WIFI_STATE_DISABLED) {
                    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (km.isKeyguardLocked) {
                        lastAlertTime = now
                        Log.w(TAG, "Wi-Fi disabled while device is locked!")
                        dispatchAlert(
                            "WIFI_DISABLED",
                            "📡 <b>NETWORK ALERT: Wi-Fi Disabled</b>\n" +
                                    "━━━━━━━━━━━━━━━━━━━━\n" +
                                    "⚠️ Wi-Fi was turned off while the device screen was locked.\n" +
                                    "This may indicate an attempt to prevent tracking.\n\n" +
                                    "<i>Send <code>/locate</code> to check device position or <code>/lock</code> to secure it.</i>"
                        )
                    }
                }
            }
            @Suppress("DEPRECATION")
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                // Only alert on complete loss of connectivity while locked
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                @Suppress("DEPRECATION")
                val activeNetwork = cm.activeNetworkInfo
                if (activeNetwork == null || !activeNetwork.isConnected) {
                    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (km.isKeyguardLocked) {
                        lastAlertTime = now
                        Log.w(TAG, "All network connectivity lost while device is locked!")
                        dispatchAlert(
                            "CONNECTIVITY_LOST",
                            "📡 <b>NETWORK ALERT: All Connectivity Lost</b>\n" +
                                    "━━━━━━━━━━━━━━━━━━━━\n" +
                                    "⚠️ Device has lost all network connectivity (Wi-Fi + Mobile Data) while locked.\n" +
                                    "The device is now unreachable for remote commands.\n\n" +
                                    "<i>If you did not disable connectivity, the device may be compromised.\n" +
                                    "SMS commands (PASA <totp> /locate) will still work via cellular.</i>"
                        )
                    }
                }
            }
        }
    }

    private fun dispatchAlert(alertType: String, message: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (preferencesManager.botToken.isNotBlank() && preferencesManager.ownerChatIdLong != 0L) {
                    telegramApi.sendMessage(
                        token = preferencesManager.botToken,
                        request = SendMessageRequest(
                            chatId = preferencesManager.ownerChatIdLong,
                            text = message
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to deliver network change alert", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
