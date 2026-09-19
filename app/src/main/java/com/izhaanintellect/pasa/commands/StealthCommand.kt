package com.izhaanintellect.pasa.commands

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import com.izhaanintellect.pasa.ui.SetupActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Toggles launcher icon stealth visibility.
 */
@Singleton
class StealthCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/hide"
    override val description = "Hide or unhide app launcher icon"
    override val usage = "/hide | /show"

    companion object {
        private const val TAG = "PASA_Stealth"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val sub = args.firstOrNull()?.lowercase()

        return when (sub) {
            "show", "unhide" -> setLauncherVisible(true)
            "hide" -> setLauncherVisible(false)
            else -> setLauncherVisible(!preferencesManager.isStealthMode)
        }
    }

    private fun setLauncherVisible(visible: Boolean): CommandResult {
        return try {
            val componentName = ComponentName(context, SetupActivity::class.java)
            val newState = if (visible) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            context.packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP
            )
            preferencesManager.isStealthMode = !visible
            Log.i(TAG, "Launcher icon visibility set to: $visible")

            if (visible) {
                CommandResult(success = true, message = "👁️ PASA launcher icon restored to app drawer.")
            } else {
                CommandResult(
                    success = true,
                    message = "🔇 PASA launcher icon hidden from app drawer.\n" +
                            "Send <code>/show</code> at any time via Telegram to restore the icon."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle icon visibility", e)
            CommandResult(success = false, message = "❌ Could not toggle launcher visibility: ${e.message}")
        }
    }
}
