package com.example.healthieru.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctor_notes")
data class DoctorNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val author: String,
    val category: String,
    val text: String,
    val date: Long,
    val structuredData: String? = null // Holds JSON for Diet/Workout tables
)