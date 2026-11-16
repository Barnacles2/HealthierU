package com.example.healthieru

import android.app.Application
import com.example.healthieru.data.local.AppDatabase
import com.example.healthieru.data.repository.HealthRepository

class HealthierUApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }

    val repository by lazy {
        HealthRepository(database.healthDao(), database.userDao())
    }
}