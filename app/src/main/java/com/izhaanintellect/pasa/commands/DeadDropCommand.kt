package com.izhaanintellect.pasa.commands

import android.content.Context
import android.util.Log
import com.izhaanintellect.pasa.data.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dead-Drop Backup - DISABLED (Not Implemented)
 *
 * ⚠️ SECURITY NOTICE:
 * This feature was non-functional and has been disabled.
 * Cloud backup functionality is not currently implemented.
 * Evidence is NOT backed up to cloud storage.
 *
 * Do NOT rely on this command for evidence preservation.
 * Always maintain manual backups of critical evidence.
 */
@Singleton
class DeadDropCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Command {

    override val name = "/dead_drop"
    override val description = "Cloud backup (DISABLED - not implemented)"
    override val usage = "/dead_drop"

    companion object {
        private const val TAG = "PASA_DeadDrop_DISABLED"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        Log.w(TAG, "Dead-Drop command invoked but feature is disabled (not implemented)")

        return CommandResult(
            success = false,
            message = """
                🚫 <b>Dead-Drop Backup - DISABLED</b>
                ━━━━━━━━━━━━━━━━━━━━
                ⚠️ <b>IMPORTANT SECURITY NOTICE</b>

                This feature has been <b>DISABLED</b> because:
                ❌ Cloud backup functionality was NOT IMPLEMENTED
                ❌ Evidence was NEVER actually uploaded to cloud
                ❌ Users were misled into believing data was backed up
                ❌ Permanent data loss would occur if device was wiped

                <b>What This Means:</b>
                • Evidence is NOT backed up to cloud storage
                • Device wipe = permanent evidence loss
                • Evidence is only stored locally on device

                <b>Recommended Action:</b>
                Maintain manual backups of critical evidence via:
                • USB cable export to secure computer
                • Encrypted external storage
                • Manual download from Telegram chat history

                For feature implementation status, contact support.
            """.trimIndent()
        )
    }
}
