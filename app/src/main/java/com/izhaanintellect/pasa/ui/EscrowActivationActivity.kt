package com.izhaanintellect.pasa.ui

import android.app.Activity
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import com.izhaanintellect.pasa.bot.SendMessageRequest
import com.izhaanintellect.pasa.bot.TelegramApi
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.databinding.ActivityEscrowActivationBinding
import com.izhaanintellect.pasa.network.PasaBackendApi
import com.izhaanintellect.pasa.util.SecurityActivityLauncher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

/**
 * Prompts user with Android system's native ConfirmDeviceCredential dialog to bind
 * and arm the Keyguard Synthetic Password escrow token for remote OS PIN resets.
 */
@AndroidEntryPoint
class EscrowActivationActivity : AppCompatActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var telegramApi: TelegramApi
    @Inject lateinit var pasaBackendApi: PasaBackendApi

    private lateinit var binding: ActivityEscrowActivationBinding
    private var pendingPin: String? = null

    companion object {
        private const val TAG = "PASA_EscrowActivation"
        const val EXTRA_PENDING_PIN = "extra_pending_pin"
        const val NOTIFICATION_ID = 2005

        fun createIntent(context: Context, pendingPin: String? = null): Intent {
            return Intent(context, EscrowActivationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                pendingPin?.let { putExtra(EXTRA_PENDING_PIN, it) }
            }
        }
    }

    private suspend fun dispatchTelegramNotification(message: String) {
        var relayed = false
        if (preferencesManager.useBackendServer) {
            try {
                val deviceIdBody = preferencesManager.deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                val msgBody = message.toRequestBody("text/plain".toMediaTypeOrNull())
                val resp = pasaBackendApi.sendDeviceResponse(
                    deviceId = deviceIdBody,
                    commandId = null,
                    message = msgBody,
                    photo = null,
                    audio = null,
                    video = null,
                    evidence = null,
                    latitude = null,
                    longitude = null
                )
                relayed = resp.ok
                Log.i(TAG, "Escrow status notification relayed via backend: ${resp.ok}")
            } catch (e: Exception) {
                Log.w(TAG, "Backend relay failed, falling back to direct bot: ${e.message}")
            }
        }

        if (!relayed && preferencesManager.botToken.isNotBlank()) {
            try {
                telegramApi.sendMessage(
                    preferencesManager.botToken,
                    SendMessageRequest(
                        preferencesManager.ownerChatIdLong,
                        message
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Direct Telegram notification failed: ${e.message}")
            }
        }
    }

    private val confirmCredentialLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = PasaDeviceAdmin.getComponentName(this)
        val isActive = dpm.isResetPasswordTokenActive(component)
        Log.i(TAG, "confirmCredential finished with resultCode=${result.resultCode}, isResetPasswordTokenActive=$isActive")

        SecurityActivityLauncher.dismissNotification(this, NOTIFICATION_ID)

        if (result.resultCode == Activity.RESULT_OK || isActive) {
            Toast.makeText(this, "✅ Hardware Escrow Token Armed!", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val pending = pendingPin
                    if (!pending.isNullOrBlank()) {
                        val (success, msg) = PasaDeviceAdmin.resetDevicePassword(this@EscrowActivationActivity, pending, preferencesManager)
                        if (success) {
                            dispatchTelegramNotification(
                                "🔐 <b>OS Lockscreen PIN Updated Successfully!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                        "✅ Hardware escrow token verified and armed.\n" +
                                        "🔑 <b>New Hardware PIN:</b> <code>$pending</code>\n\n" +
                                        "<i>Your phone's lockscreen PIN has been permanently updated. Future PIN resets can now be executed 100% remotely!</i>"
                            )
                        } else {
                            dispatchTelegramNotification(
                                "⚠️ <b>Token Armed, but applying new PIN returned:</b> $msg"
                            )
                        }
                    } else {
                        dispatchTelegramNotification(
                            "✅ <b>Hardware Escrow Token Successfully Armed!</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                                    "Android Keyguard has authorized remote password management.\n\n" +
                                    "You can now remotely change your device lockscreen PIN anytime by sending <code>/set_os_pin &lt;pin&gt;</code>."
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying Telegram after escrow activation", e)
                }
            }
        } else {
            Toast.makeText(this, "⚠️ Credential confirmation was not completed", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        } catch (_: Exception) {}

        binding = ActivityEscrowActivationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pendingPin = intent.getStringExtra(EXTRA_PENDING_PIN)

        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = PasaDeviceAdmin.getComponentName(this)

        if (!dpm.isDeviceOwnerApp(packageName)) {
            Toast.makeText(this, "Device Owner permission required", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 1. Ensure the persistent 32-byte token is enrolled with Keyguard/LockSettingsService
        PasaDeviceAdmin.ensureResetPasswordToken(this, preferencesManager)

        // 2. If already active, process pending PIN immediately
        if (dpm.isResetPasswordTokenActive(component)) {
            Log.i(TAG, "Escrow token already active, applying pending PIN if present")
            val pending = pendingPin
            if (!pending.isNullOrBlank()) {
                val (success, msg) = PasaDeviceAdmin.resetDevicePassword(this, pending, preferencesManager)
                CoroutineScope(Dispatchers.IO).launch {
                    dispatchTelegramNotification(
                        if (success) "🔐 <b>OS Lockscreen PIN Updated!</b>\nNew PIN: <code>$pending</code>"
                        else "❌ <b>OS Password Reset Failed:</b> $msg"
                    )
                }
            }
            SecurityActivityLauncher.dismissNotification(this, NOTIFICATION_ID)
            finish()
            return
        }

        // 3. Launch the Android native Keyguard Credential Confirmation Prompt
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            km.createConfirmDeviceCredentialIntent(
                "PASA Guardian Verification",
                "Enter your current lockscreen PIN to authorize remote hardware password resets."
            )
        } else {
            km.createConfirmDeviceCredentialIntent(
                "PASA Guardian Verification",
                "Confirm your current lockscreen credentials"
            )
        }

        if (confirmIntent != null) {
            confirmCredentialLauncher.launch(confirmIntent)
        } else {
            Log.w(TAG, "No secure lockscreen set on device")
            Toast.makeText(this, "No secure lockscreen currently set on device", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
