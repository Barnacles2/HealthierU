package com.example.healthieru.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: DoctorNote): Long

    @Query("SELECT * FROM doctor_notes WHERE userId = :userId ORDER BY id DESC")
    fun observeForUser(userId: Long): Flow<List<DoctorNote>>

    @Query("UPDATE doctor_notes SET text = :text WHERE id = :noteId AND userId = :userId")
    suspend fun updateText(noteId: Long, userId: Long, text: String): Int

    @Query("DELETE FROM doctor_notes WHERE id = :noteId AND userId = :userId")
    suspend fun delete(noteId: Long, userId: Long): Int
}