package com.izhaanintellect.pasa.util

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.KeyEvent

/**
 * Manages hardware key interception and lock-task mode for security activities.
 */
object InputGuard {

    private const val TAG = "PASA_InputGuard"

    /**
     * Enters lock-task mode on the given activity.
     */
    fun enterLockTask(activity: Activity) {
        try {
            activity.startLockTask()
            Log.i(TAG, "Entered Lock Task mode")
        } catch (e: Exception) {
            Log.w(TAG, "Could not enter Lock Task mode: ${e.message}")
        }
    }

    /**
     * Exits lock-task mode on the given activity.
     */
    fun exitLockTask(activity: Activity) {
        try {
            activity.stopLockTask()
            Log.i(TAG, "Stopped Lock Task mode")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping lock task: ${e.message}")
        }
    }

    /**
     * Returns true if the given key event should be consumed (suppressed) during screen guard.
     */
    fun shouldConsumeKey(keyCode: Int): Boolean {
        return keyCode in setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_HEADSETHOOK
        )
    }

    /**
     * Dismisses any system dialogs that may be overlaying the security screen.
     */
    fun dismissSystemOverlays(activity: Activity) {
        try {
            @Suppress("DEPRECATION")
            activity.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        } catch (_: Exception) {}
    }
}
