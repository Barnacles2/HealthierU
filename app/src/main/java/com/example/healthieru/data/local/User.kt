package com.example.healthieru.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Int = 1,   // single row, because the app has one profile
    val name: String,
    val age: Int
)