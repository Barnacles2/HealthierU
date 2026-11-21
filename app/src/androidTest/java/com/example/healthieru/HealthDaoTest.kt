package com.example.healthieru

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.healthieru.data.local.AppDatabase
import com.example.healthieru.data.local.HealthRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sample() = HealthRecord(
        id = 0, date = "1 Jan 08:00", weightKg = 70.0, heartRate = 72, caloriesIntake = 5
    )

    @Test
    fun insert_thenRead_returnsRecord() = runBlocking {
        val dao = db.healthDao()
        dao.insert(sample())
        val records = dao.getAllRecords().first()
        assertEquals(1, records.size)
        assertEquals(70.0, records[0].weightKg, 0.001)
    }

    @Test
    fun delete_removesRecord() = runBlocking {
        val dao = db.healthDao()
        dao.insert(sample())
        dao.delete(dao.getAllRecords().first()[0])
        assertTrue(dao.getAllRecords().first().isEmpty())
    }
}