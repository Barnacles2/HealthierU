package com.example.healthieru.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: Long): Flow<User?>

    @Query("SELECT * FROM users ORDER BY name")
    fun observeAll(): Flow<List<User>>

    @Query("UPDATE users SET name = :name, phone = :phone, age = :age, healthGoal = :goal WHERE id = :id")
    suspend fun updateProfile(id: Long, name: String, phone: String, age: Int, goal: String)

    @Query("UPDATE users SET followUpStatus = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)
}