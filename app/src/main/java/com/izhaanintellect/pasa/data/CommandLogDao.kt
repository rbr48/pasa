package com.izhaanintellect.pasa.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for command execution logs.
 */
@Dao
interface CommandLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CommandLog): Long

    @Query("SELECT * FROM command_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 50): List<CommandLog>

    @Query("SELECT * FROM command_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<CommandLog>>

    @Query("SELECT COUNT(*) FROM command_logs")
    suspend fun getCount(): Int

    @Query("DELETE FROM command_logs")
    suspend fun clearAll()

    @Query("DELETE FROM command_logs WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}
