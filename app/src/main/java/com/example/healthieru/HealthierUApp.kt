package com.example.healthieru

import android.app.Application
import com.example.healthieru.data.local.AppDatabase
import com.example.healthieru.data.repository.HealthRepository
import com.google.android.material.color.DynamicColors

class HealthierUApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    val database by lazy { AppDatabase.getDatabase(this) }

    val repository by lazy {
        HealthRepository(database.healthDao(), database.userDao())
    }
}