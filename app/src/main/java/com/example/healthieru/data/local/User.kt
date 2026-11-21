package com.example.healthieru.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.healthieru.model.Person

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    override val name: String,
    override val email: String,
    override val phone: String,
    val password: String,
    val age: Int,
    val healthGoal: String,
    val followUpStatus: String = "Not reviewed"
) : Person