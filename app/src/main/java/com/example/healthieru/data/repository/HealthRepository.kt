package com.example.healthieru.data.repository

import com.example.healthieru.data.local.DoctorNote
import com.example.healthieru.data.local.HealthDao
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.data.local.NoteDao
import com.example.healthieru.data.local.User
import com.example.healthieru.data.local.UserDao
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.util.Calendar

class HealthRepository(
    private val userDao: UserDao,
    private val healthDao: HealthDao,
    private val noteDao: NoteDao
) {
    private fun hash(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun sameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
                ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    // ---------- users ----------
    suspend fun register(user: User): Result<Long> =
        if (userDao.findByEmail(user.email.trim()) != null)
            Result.failure(IllegalArgumentException("An account with this email already exists."))
        else Result.success(userDao.insert(user.copy(email = user.email.trim(), password = hash(user.password))))

    suspend fun login(email: String, password: String): User? {
        val user = userDao.findByEmail(email.trim()) ?: return null
        return if (user.password == hash(password)) user else null
    }

    fun observeUser(id: Long): Flow<User?> = userDao.observeById(id)
    fun observeAllUsers(): Flow<List<User>> = userDao.observeAll()

    suspend fun updateProfile(id: Long, name: String, phone: String, age: Int, goal: String) =
        userDao.updateProfile(id, name, phone, age, goal)

    suspend fun setStatus(id: Long, status: String) = userDao.setStatus(id, status)

    // Records and notes are removed automatically (CASCADE)
    suspend fun deleteUser(id: Long) = userDao.deleteById(id)

    // ---------- health records ----------
    fun observeRecords(userId: Long): Flow<List<HealthRecord>> = healthDao.observeForUser(userId)
    fun observeLatestForAll(): Flow<List<HealthRecord>> = healthDao.observeLatestForAll()
    suspend fun latestOnce(userId: Long): HealthRecord? = healthDao.latestOnce(userId)

    /** A second save on the same day replaces the first. */
    suspend fun addRecord(record: HealthRecord) {
        val latest = healthDao.latestOnce(record.userId)
        if (latest != null && sameDay(latest.date, record.date)) healthDao.delete(latest.id, record.userId)
        healthDao.insert(record)
    }

    suspend fun editRecord(recordId: Long, userId: Long, weight: Double, heartRate: Int) =
        healthDao.updateMeasurements(recordId, userId, weight, heartRate)

    suspend fun deleteRecord(recordId: Long, userId: Long) = healthDao.delete(recordId, userId)

    // ---------- doctor notes ----------
    fun observeNotes(userId: Long): Flow<List<DoctorNote>> = noteDao.observeForUser(userId)
    suspend fun addNote(note: DoctorNote) = noteDao.insert(note)
    suspend fun editNote(noteId: Long, userId: Long, text: String) = noteDao.updateText(noteId, userId, text)
    suspend fun deleteNote(noteId: Long, userId: Long) = noteDao.delete(noteId, userId)

    // ---------- demo patient (created once, only if the database is empty) ----------
    suspend fun seedDemoIfEmpty() {
        if (userDao.count() > 0) return
        val id = userDao.insert(
            User(
                name = "Wanjiru Kamau", email = "wanjiru@mail.com", phone = "0711111111",
                password = hash("1234"), age = 28, healthGoal = "Lose 5 kg"
            )
        )
        val day = 24L * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        listOf(
            28 to (76.0 to 84), 21 to (75.2 to 82), 14 to (74.1 to 81),
            7 to (73.0 to 79), 0 to (72.0 to 78)
        ).forEach { (daysAgo, values) ->
            healthDao.insert(
                HealthRecord(
                    userId = id, date = now - daysAgo * day,
                    weightKg = values.first, heightCm = 165.0, heartRate = values.second,
                    activityLevel = "Moderate", exerciseMinutes = 20, fruitVeg = 3, sleepHours = 6.0
                )
            )
        }
    }
}