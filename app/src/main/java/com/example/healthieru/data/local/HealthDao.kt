package com.example.healthieru.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Insert
    suspend fun insert(record: HealthRecord): Long

    @Query("SELECT * FROM health_records WHERE userId = :userId ORDER BY date ASC")
    fun observeForUser(userId: Long): Flow<List<HealthRecord>>

    @Query("SELECT * FROM health_records WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    suspend fun latestOnce(userId: Long): HealthRecord?

    @Query("SELECT * FROM health_records WHERE id IN (SELECT MAX(id) FROM health_records GROUP BY userId)")
    fun observeLatestForAll(): Flow<List<HealthRecord>>

    @Query("UPDATE health_records SET weightKg = :weight, heartRate = :heartRate WHERE id = :recordId AND userId = :userId")
    suspend fun updateMeasurements(recordId: Long, userId: Long, weight: Double, heartRate: Int): Int

    @Query("DELETE FROM health_records WHERE id = :recordId AND userId = :userId")
    suspend fun delete(recordId: Long, userId: Long): Int
}