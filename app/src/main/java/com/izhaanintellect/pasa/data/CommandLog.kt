package com.izhaanintellect.pasa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a logged command execution in PASA.
 */
@Entity(tableName = "command_logs")
data class CommandLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val command: String,
    val args: String = "",
    val chatId: Long,
    val senderName: String = "",
    val status: String, // "SUCCESS", "FAILED", "REJECTED"
    val response: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
