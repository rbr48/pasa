package com.izhaanintellect.pasa.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.izhaanintellect.pasa.commands.AutostartCommand
import com.izhaanintellect.pasa.util.OemProtectionHelper

/**
 * Handles 1-tap notification clicks to open manufacturer-specific autostart settings.
 */
class OemAutostartReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "PASA_OemReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AutostartCommand.ACTION_LAUNCH_OEM_AUTOSTART) {
            Log.i(TAG, "Opening OEM autostart settings from notification tap")
            OemProtectionHelper.openOemAutostartSettings(context)
        }
    }
}
