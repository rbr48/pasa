package com.izhaanintellect.pasa.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for PASA.
 * Secured with SQLCipher encryption using Keystore-derived keys.
 */
@Database(
    entities = [CommandLog::class, PendingUpload::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    /** Provides access to command log operations. */
    abstract fun commandLogDao(): CommandLogDao

    /** Provides access to pending upload operations. */
    abstract fun pendingUploadDao(): PendingUploadDao
}
