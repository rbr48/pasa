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
 * Duress unlock activity - handles device unlock sequence after duress PIN detected.
 *
 * PRODUCTION-READY FIXES:
 * ✅ Serialized unlock operations (no race conditions)
 * ✅ Single execution path (no parallel gestures)
 * ✅ Proper sequencing with delays
 * ✅ Fallback to home if keyguard dismiss fails
 */
class DuressUnlockActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PASA_DuressUnlock"
        private val unlockLock = Object()

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
                Log.e(TAG, "❌ Failed to launch DuressUnlockActivity: ${e.message}")
            }
        }
    }

    private var dismissed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "🔓 DuressUnlockActivity: Requesting keyguard dismissal")

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
            Log.w(TAG, "⚠️ Window configuration error: ${e.message}")
        }

        // Serialize unlock to prevent race conditions
        synchronized(unlockLock) {
            dismissKeyguardAndGoHome()
        }
    }

    private fun dismissKeyguardAndGoHome() {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var homeDispatched = false

        fun triggerHome() {
            if (!homeDispatched) {
                homeDispatched = true
                goToHome()
            }
        }

        // Safety fallback: if dismissal callback stalls, force launch Home after 800ms
        val fallbackRunnable = Runnable {
            Log.d(TAG, "Keyguard dismissal timeout — forcing Home launch")
            triggerHome()
        }
        mainHandler.postDelayed(fallbackRunnable, 800L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && km != null) {
            Log.d(TAG, "Requesting keyguard dismissal via API 26+ method")
            try {
                km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        Log.i(TAG, "✅ Keyguard dismissal succeeded")
                        mainHandler.removeCallbacks(fallbackRunnable)
                        triggerHome()
                    }

                    override fun onDismissError() {
                        Log.w(TAG, "⚠️ Keyguard dismissal error — launching Home fallback")
                        mainHandler.removeCallbacks(fallbackRunnable)
                        triggerHome()
                    }

                    override fun onDismissCancelled() {
                        Log.w(TAG, "⚠️ Keyguard dismissal cancelled — launching Home fallback")
                        mainHandler.removeCallbacks(fallbackRunnable)
                        triggerHome()
                    }
                })
            } catch (e: Exception) {
                Log.w(TAG, "Keyguard dismissal exception: ${e.message}")
                mainHandler.removeCallbacks(fallbackRunnable)
                triggerHome()
            }
        } else {
            mainHandler.removeCallbacks(fallbackRunnable)
            triggerHome()
        }
    }

    private fun goToHome() {
        try {
            Log.d(TAG, "Launching Home screen")
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(homeIntent)
            Log.i(TAG, "✅ Home screen launched")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to launch home: ${e.message}")
        } finally {
            // Always finish this activity cleanly
            finish()
            overridePendingTransition(0, 0)
        }
    }
}
