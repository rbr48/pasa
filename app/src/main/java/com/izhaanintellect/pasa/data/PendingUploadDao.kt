package com.izhaanintellect.pasa.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing pending evidence upload operations.
 */
@Dao
interface PendingUploadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(upload: PendingUpload)

    @Update
    suspend fun update(upload: PendingUpload)

    @Query("SELECT * FROM pending_uploads WHERE id = :id")
    suspend fun getById(id: String): PendingUpload?

    @Query("SELECT * FROM pending_uploads WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingUpload>

    @Query("SELECT * FROM pending_uploads ORDER BY createdAt DESC LIMIT 50")
    fun getAllFlow(): Flow<List<PendingUpload>>

    @Query("DELETE FROM pending_uploads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_uploads WHERE status = 'COMPLETED' AND createdAt < :cutoff")
    suspend fun purgeCompletedOlderThan(cutoff: Long)
}
