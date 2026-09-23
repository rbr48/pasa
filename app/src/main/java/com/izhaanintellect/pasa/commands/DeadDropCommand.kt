package com.izhaanintellect.pasa.commands

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy /dead_drop alias redirected directly to DeadManSwitchCommand (/deadman).
 */
@Singleton
class DeadDropCommand @Inject constructor(
    private val deadManSwitchCommand: DeadManSwitchCommand
) : Command {

    override val name = "/dead_drop"
    override val description = "Anti-EDL offline auto-destruct timer (alias of /deadman)"
    override val usage = "/dead_drop [enable|disable|hours <1-72>|status|heartbeat]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        return deadManSwitchCommand.execute(args, chatId)
    }
}
