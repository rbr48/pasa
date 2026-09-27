package com.izhaanintellect.pasa.util

import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

/**
 * Manages display brightness and immersive UI configuration for security activities.
 */
object DisplayGuard {

    private const val TAG = "PASA_DisplayGuard"

    /**
     * Dims the display to minimum brightness.
     */
    fun dimToMinimum(window: Window) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 0.001f
        window.attributes = layoutParams
    }

    /**
     * Restores default system brightness.
     */
    fun restoreBrightness(window: Window) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams
    }

    /**
     * Configures full immersive mode hiding all system bars and enabling lockscreen overlay.
     */
    fun configureImmersive(window: Window, decorView: View, setShowWhenLocked: (() -> Unit)? = null, setTurnScreenOn: (() -> Unit)? = null) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked?.invoke()
                setTurnScreenOn?.invoke()
            }

            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.let {
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error configuring immersive mode: ${e.message}")
        }
    }
}
