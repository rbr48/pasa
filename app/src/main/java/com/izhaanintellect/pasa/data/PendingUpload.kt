package com.izhaanintellect.pasa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an evidence upload queued for reliable dispatch via WorkManager.
 * Ensures offline durability across reboots, network transitions, and OEM battery doze modes.
 */
@Entity(tableName = "pending_uploads")
data class PendingUpload(
    @PrimaryKey
    val id: String,
    val commandId: String,
    val fileType: String, // "PHOTO", "AUDIO", "VIDEO", "EVIDENCE"
    val filePath: String,
    val isEncrypted: Boolean = true,
    val attemptCount: Int = 0,
    val status: String = "PENDING", // "PENDING", "UPLOADING", "COMPLETED", "FAILED"
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
