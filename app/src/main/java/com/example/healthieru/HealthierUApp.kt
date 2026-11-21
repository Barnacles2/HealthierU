package com.example.healthieru

import android.app.Application
import com.example.healthieru.data.SessionManager
import com.example.healthieru.data.local.AppDatabase
import com.example.healthieru.data.repository.HealthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HealthierUApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val session by lazy { SessionManager(this) }
    val repository by lazy {
        HealthRepository(database.userDao(), database.healthDao(), database.noteDao())
    }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch { repository.seedDemoIfEmpty() }
    }
}