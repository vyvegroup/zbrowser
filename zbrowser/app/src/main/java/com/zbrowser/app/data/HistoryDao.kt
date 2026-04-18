package com.zbrowser.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history WHERE visitedAt >= :startTime AND visitedAt <= :endTime ORDER BY visitedAt DESC")
    fun getHistoryByTimeRange(startTime: Long, endTime: Long): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%'")
    fun searchHistory(query: String): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history WHERE url = :url ORDER BY visitedAt DESC LIMIT 1")
    suspend fun getLatestVisit(url: String): HistoryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry): Long

    @Delete
    suspend fun delete(entry: HistoryEntry)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE visitedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun getCount(): Int
}
