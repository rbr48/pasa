package com.izhaanintellect.pasa.commands

import java.io.File

/**
 * Result returned by a command execution.
 */
data class CommandResult(
    val success: Boolean,
    val message: String,
    val photoFile: File? = null,
    val audioFile: File? = null,
    val videoFile: File? = null,
    val location: Pair<Double, Double>? = null
)

/**
 * Base interface for all remote commands executable via Telegram.
 */
interface Command {
    val name: String
    val description: String
    val usage: String

    suspend fun execute(args: List<String>, chatId: Long): CommandResult
}
