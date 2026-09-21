package com.izhaanintellect.pasa.commands

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stops an active live video stream initiated via /livestream.
 */
@Singleton
class StopStreamCommand @Inject constructor(
    @ApplicationContext private val context: Context,
    private val liveStreamCommand: LiveStreamCommand
) : Command {

    override val name = "/stopstream"
    override val description = "Stop active live video streaming"
    override val usage = "/stopstream"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        return liveStreamCommand.stopStream()
    }
}
