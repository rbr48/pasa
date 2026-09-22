package com.izhaanintellect.pasa.commands

import android.content.Context
import com.izhaanintellect.pasa.admin.PasaDeviceAdmin
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Self-heals and locks all PASA runtime permissions using Device Owner authority.
 */
@Singleton
class SelfHealCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/self_heal"
    override val description = "Auto-grant and permanently lock all permissions [Device Owner]"
    override val usage = "/self_heal"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        if (!PasaDeviceAdmin.isDeviceOwner(context)) {
            return CommandResult(
                success = false,
                message = "❌ <b>Device Owner Required:</b> Self-healing permissions requires Android Device Owner.\n" +
                        "Run <code>/device_owner</code> for activation instructions."
            )
        }

        val results = PasaDeviceAdmin.selfHealPermissions(context)
        val sb = StringBuilder()
        sb.append("🛡️ <b>Self-Healing Permissions Sovereignty</b>\n━━━━━━━━━━━━━━━━━━━━\n")

        var grantedCount = 0
        results.forEach { (perm, ok) ->
            if (ok) grantedCount++
            sb.append(if (ok) "✅ " else "⚠️ ")
            sb.append("<code>$perm</code>: ").append(if (ok) "LOCKED & GRANTED" else "Failed").append("\n")
        }

        sb.append("\n👑 <b>Result:</b> $grantedCount/${results.size} permissions permanently secured.\n")
        sb.append("🔒 <i>These permissions are marked 'Managed by your organization' and cannot be revoked by the user or OS!</i>")

        return CommandResult(success = true, message = sb.toString())
    }
}
