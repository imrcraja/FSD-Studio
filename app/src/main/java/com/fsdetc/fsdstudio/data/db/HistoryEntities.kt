package com.fsdetc.fsdstudio.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** One saved render. Files live in filesDir/history. */
@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** POSE, FLAT or AI. */
    val kind: String,
    val title: String,
    /** Pose name for poses, the prompt for AI renders. */
    val subtitle: String,
    val filePath: String,
    val sourceName: String,
    /** Dedupe key (render URL, source file, or ai:timestamp:seed). */
    val uniqueKey: String,
    val createdAt: Long,
    val width: Int,
    val height: Int,
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryItem>>

    @Query("SELECT * FROM history WHERE uniqueKey = :key LIMIT 1")
    suspend fun findByKey(key: String): HistoryItem?

    @Query("SELECT * FROM history WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): HistoryItem?

    @Insert
    suspend fun insert(item: HistoryItem): Long

    @Update
    suspend fun update(item: HistoryItem)

    @Delete
    suspend fun delete(item: HistoryItem)
}
