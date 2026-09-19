package com.izhaanintellect.pasa.ui

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.izhaanintellect.pasa.databinding.ActivityAlertMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Full-screen lockscreen activity for displaying urgent owner messages and lost-device alerts.
 * Configured with showWhenLocked and turnScreenOn to pop up prominently over keyguard.
 */
class AlertMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertMessageBinding

    companion object {
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_PHONE = "extra_phone"
        private const val NOTIFICATION_ID = 2001

        fun createIntent(context: Context, message: String, phone: String? = null): Intent {
            return Intent(context, AlertMessageActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_MESSAGE, message)
                phone?.let { putExtra(EXTRA_PHONE, it) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        configureLockScreenFlags()
        super.onCreate(savedInstanceState)

        binding = ActivityAlertMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val messageText = intent.getStringExtra(EXTRA_MESSAGE) ?: "Please return this device to its owner."
        val explicitPhone = intent.getStringExtra(EXTRA_PHONE)

        binding.tvMessageContent.text = messageText

        val timeFormat = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
        binding.tvTimestamp.text = "Received: ${timeFormat.format(Date())}"

        // Extract phone number from message if not explicitly provided
        val phoneNumber = explicitPhone ?: extractPhoneNumber(messageText)

        if (!phoneNumber.isNullOrBlank()) {
            binding.btnCallOwner.visibility = View.VISIBLE
            binding.btnCallOwner.text = "📞 Call Owner: $phoneNumber"
            binding.btnCallOwner.setOnClickListener {
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(dialIntent)
                } catch (e: Exception) {
                    // Fallback if dialer fails
                }
            }
        } else {
            binding.btnCallOwner.visibility = View.GONE
        }

        binding.btnDismiss.setOnClickListener {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID)
            finish()
        }

        playAlertChime()
    }

    private fun configureLockScreenFlags() {
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

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager?.requestDismissKeyguard(this, null)
        }
    }

    private fun playAlertChime() {
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)
            ringtone?.play()
        } catch (_: Exception) {}
    }

    private fun extractPhoneNumber(text: String): String? {
        val pattern = Pattern.compile("(\\+?[0-9]{7,15})")
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }
}
