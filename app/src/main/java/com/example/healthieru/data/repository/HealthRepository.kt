package com.example.healthieru.data.repository

import com.example.healthieru.data.local.HealthDao
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.data.local.User
import com.example.healthieru.data.local.UserDao
import kotlinx.coroutines.flow.Flow

class HealthRepository(
    private val healthDao: HealthDao,
    private val userDao: UserDao
) {

    // READ
    val allRecords: Flow<List<HealthRecord>> = healthDao.getAllRecords()
    val user: Flow<User?> = userDao.getUser()

    fun getRecord(id: Int): Flow<HealthRecord?> = healthDao.getById(id)

    // CREATE
    suspend fun insert(record: HealthRecord) = healthDao.insert(record)

    // UPDATE
    suspend fun update(record: HealthRecord) = healthDao.update(record)

    // DELETE
    suspend fun delete(record: HealthRecord) = healthDao.delete(record)

    // PROFILE
    suspend fun saveUser(user: User) = userDao.upsert(user)
}