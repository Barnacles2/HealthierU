package com.example.healthieru.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val age: Int,
    val weight: Double,    // Add this
    val height: Double,    // Add this
    val heartRate: Int,    // Add this
    val bmi: Double        // Add this
)