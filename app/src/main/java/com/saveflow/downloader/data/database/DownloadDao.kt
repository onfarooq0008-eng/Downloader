package com.saveflow.downloader.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.saveflow.downloader.domain.models.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert
    suspend fun insert(entity: DownloadEntity): Long

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun get(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun observeById(id: Long): Flow<DownloadEntity?>

    /** Everything that is not finished: queued, running, paused and failed. */
    @Query("SELECT * FROM downloads WHERE status != 'COMPLETED' ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun observeCompleted(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED','DOWNLOADING')")
    fun observeRunning(): Flow<List<DownloadEntity>>

    @Query("UPDATE downloads SET status = :status, error = :error WHERE id = :id")
    suspend fun setStatus(id: Long, status: DownloadStatus, error: String?)

    @Query("UPDATE downloads SET downloadedBytes = :bytes, totalBytes = :total WHERE id = :id")
    suspend fun setProgress(id: Long, bytes: Long, total: Long)

    @Query(
        "UPDATE downloads SET status = :status, savedUri = :uri, fileName = :name, downloadedBytes = :size, " +
            "totalBytes = :size, completedAt = :at, error = NULL WHERE id = :id"
    )
    suspend fun markCompleted(id: Long, status: DownloadStatus, uri: String, name: String, size: Long, at: Long)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM downloads WHERE status = 'COMPLETED'")
    suspend fun deleteCompleted()

    /** Rows left "running" by a killed process become paused so the user can resume them. */
    @Query("UPDATE downloads SET status = 'PAUSED' WHERE status IN ('QUEUED','DOWNLOADING')")
    suspend fun resetInterrupted()
}
