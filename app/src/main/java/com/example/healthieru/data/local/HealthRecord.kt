package com.example.healthieru.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_records")
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,          // e.g. "2026-11-16"
    val steps: Int,
    val waterLitres: Double,
    val caloriesIntake: Int
)