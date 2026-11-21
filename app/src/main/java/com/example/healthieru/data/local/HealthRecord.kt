package com.example.healthieru.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_records",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val date: Long,
    val weightKg: Double,
    val heightCm: Double,
    val heartRate: Int,
    val activityLevel: String,
    val exerciseMinutes: Int,
    val fruitVeg: Int,
    val sleepHours: Double
)