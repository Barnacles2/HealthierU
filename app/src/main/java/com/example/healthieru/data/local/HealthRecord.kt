package com.example.healthieru.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_records")
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val weightKg: Double,
    val heartRate: Int,
    val caloriesIntake: Int
)