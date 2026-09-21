package com.izhaanintellect.pasa.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * Headless transparent activity that runs on top of the lockscreen to immediately
 * request Android Keyguard dismissal and transition the device to the launcher.
 */
class DuressUnlockActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PASA_DuressUnlock"

        fun launch(context: Context) {
            try {
                val intent = Intent(context, DuressUnlockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch DuressUnlockActivity", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "DuressUnlockActivity created - requesting keyguard dismissal")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error configuring window flags: ${e.message}")
        }

        dismissKeyguardAndGoHome()
    }

    private fun dismissKeyguardAndGoHome() {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && km != null) {
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    Log.i(TAG, "Keyguard dismissal succeeded!")
                    goToHome()
                }

                override fun onDismissError() {
                    Log.w(TAG, "Keyguard dismissal error, forcing Home launch")
                    goToHome()
                }

                override fun onDismissCancelled() {
                    Log.w(TAG, "Keyguard dismissal cancelled, forcing Home launch")
                    goToHome()
                }
            })
        } else {
            goToHome()
        }
    }

    private fun goToHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch home intent: ${e.message}")
        } finally {
            finish()
            overridePendingTransition(0, 0)
        }
    }
}
