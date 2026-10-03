package com.izhaanintellect.pasa.service

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.izhaanintellect.pasa.R
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.camera.StealthCaptureBridge
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.location.LocationTracker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

/**
 * Decoy Airplane Mode Quick Settings Tile (Honeypot Trap).
 *
 * When a thief or unauthorized party pulls down the notification shade and taps
 * the "Airplane mode" tile to sever remote tracking, this decoy tile illuminates
 * in the active state (giving the visual illusion of disconnected radios), while
 * keeping Wi-Fi, Mobile Data, and GPS 100% active.
 *
 * Behind the scenes, it immediately captures a covert front-camera mugshot,
 * records precise GPS coordinates, dispatches an emergency intrusion alert to
 * Telegram C2, and locks the screen + USB data pins.
 */
@AndroidEntryPoint
class FakeAirplaneTileService : TileService() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var telegramApi: TelegramApi
    @Inject lateinit var locationTracker: LocationTracker

    companion object {
        private const val TAG = "PASA_FakeAirplaneTile"
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileDisplay(preferencesManager.isFakeAirplaneActive)
    }

    override fun onClick() {
        super.onClick()
        triggerHapticFeedback()

        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguard?.isDeviceLocked == true || keyguard?.isKeyguardLocked == true

        val currentlyActive = preferencesManager.isFakeAirplaneActive
        val nextActive = !currentlyActive
        preferencesManager.isFakeAirplaneActive = nextActive
        updateTileDisplay(nextActive)

        if (nextActive) {
            Log.w(TAG, "🚨 Decoy Airplane Mode tile ACTIVATED by user (device locked=$isLocked). Executing honeypot trap!")
            executeHoneypotTrap(isLocked)
        } else {
            Log.i(TAG, "Decoy Airplane Mode tile deactivated.")
        }
    }

    private fun updateTileDisplay(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_airplane_mode)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_flight)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (active) "On" else "Off"
        }
        tile.updateTile()
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(50)
                }
            }
        } catch (_: Exception) {}
    }

    private fun executeHoneypotTrap(isLocked: Boolean) {
        // Enforce immediate screen lock & hardware USB pin cut if Device Owner
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.lockNow()
            if (PasaDeviceAdmin.isDeviceOwner(this)) {
                PasaDeviceAdmin.setUsbDataSignaling(this, false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply instant lock or USB pin killswitch: ${e.message}")
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val loc = locationTracker.getCurrentLocation()
                val locText = if (loc != null) {
                    "\n📍 <b>Trap Location:</b> <a href=\"https://www.google.com/maps?q=${loc.latitude},${loc.longitude}\">${loc.latitude}, ${loc.longitude}</a> (Acc: ${loc.accuracy}m)"
                } else "\n📍 <b>Trap Location:</b> Fetching background coordinates..."

                val alertText = "🚨 <b>HONEYPOT DEFENSE TRIGGERED!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                        "⚠️ A perpetrator tapped the decoy <b>Airplane Mode</b> tile in Quick Settings!\n" +
                        "• <b>Keyguard State:</b> ${if (isLocked) "🔒 Locked" else "🔓 Unlocked"}\n" +
                        "• <b>Radio Status:</b> 🟢 <b>100% ACTIVE</b> (Covert deception active)\n" +
                        "• <b>USB Data Pins:</b> 🚫 <b>SEVERED</b> (Anti-forensics engaged)$locText\n\n" +
                        "<i>Front camera mugshot capture initiated immediately.</i>"

                if (preferencesManager.botToken.isNotBlank() && preferencesManager.ownerChatIdLong != 0L) {
                    telegramApi.sendMessage(
                        token = preferencesManager.botToken,
                        request = SendMessageRequest(
                            chatId = preferencesManager.ownerChatIdLong,
                            text = alertText
                        )
                    )
                }

                // Covert snapshot of perpetrator attempting to tap Airplane Mode
                val captureResult = StealthCaptureBridge.capturePhoto(applicationContext, useFront = true)
                captureResult.file?.let { photoFile ->
                    if (photoFile.exists() && photoFile.length() > 0 && preferencesManager.botToken.isNotBlank()) {
                        val chatIdBody = preferencesManager.ownerChatId.toRequestBody("text/plain".toMediaTypeOrNull())
                        val captionBody = "🚨 Decoy Airplane Mode Perp Capture".toRequestBody("text/plain".toMediaTypeOrNull())
                        val fileBody = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData("photo", photoFile.name, fileBody)

                        telegramApi.sendPhoto(
                            token = preferencesManager.botToken,
                            chatId = chatIdBody,
                            photo = part,
                            caption = captionBody
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing honeypot trap actions: ${e.message}", e)
            }
        }
    }
}
