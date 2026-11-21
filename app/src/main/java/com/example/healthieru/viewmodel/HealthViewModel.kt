package com.example.healthieru.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthieru.data.SessionManager
import com.example.healthieru.data.local.DoctorNote
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.data.local.User
import com.example.healthieru.data.repository.HealthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.healthieru.data.remote.RetrofitClient

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModel(
    private val repo: HealthRepository,
    private val session: SessionManager
) : ViewModel() {

    private val userId = MutableStateFlow<Long?>(if (session.isSignedIn()) session.userId else null)

    val currentUser: StateFlow<User?> = userId
        .flatMapLatest { id -> if (id == null) flowOf(null) else repo.observeUser(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val records: StateFlow<List<HealthRecord>> = userId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observeRecords(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<DoctorNote>> = userId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observeNotes(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPatients: StateFlow<List<User>> = repo.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestRecords: StateFlow<List<HealthRecord>> = repo.observeLatestForAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------- account ----------
    fun register(user: User, onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch { onResult(repo.register(user)) }
    }

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = repo.login(email, password)
            if (user != null) {
                session.userId = user.id
                userId.value = user.id
            }
            onResult(user != null)
        }
    }

    fun logout() {
        session.clear()
        userId.value = null
    }

    fun updateProfile(name: String, phone: String, age: Int, goal: String) {
        val id = userId.value ?: return
        viewModelScope.launch { repo.updateProfile(id, name.trim(), phone.trim(), age, goal.trim()) }
    }

    fun deleteAccount(onDone: () -> Unit) {
        viewModelScope.launch {
            userId.value?.let { repo.deleteUser(it) }
            logout()
            onDone()
        }
    }

    // ---------- records ----------
    suspend fun latestRecord(): HealthRecord? = userId.value?.let { repo.latestOnce(it) }

    fun addRecord(
        weight: Double, height: Double, heartRate: Int, activity: String,
        exercise: Int, fruitVeg: Int, sleep: Double
    ) {
        val id = userId.value ?: return
        viewModelScope.launch {
            repo.addRecord(
                HealthRecord(
                    userId = id, date = System.currentTimeMillis(),
                    weightKg = weight, heightCm = height, heartRate = heartRate,
                    activityLevel = activity, exerciseMinutes = exercise,
                    fruitVeg = fruitVeg, sleepHours = sleep
                )
            )
        }
    }
    private val _tip = MutableStateFlow<String?>(null)
    val tip: StateFlow<String?> = _tip

    fun loadTip() {
        viewModelScope.launch {
            try {
                _tip.value = RetrofitClient.adviceApi.getAdvice().slip.advice
            } catch (e: Exception) {
                _tip.value = "Tip unavailable. Check your internet connection."
            }
        }
    }

    fun editRecord(recordId: Long, weight: Double, heartRate: Int) {
        val id = userId.value ?: return
        viewModelScope.launch { repo.editRecord(recordId, id, weight, heartRate) }
    }

    fun deleteRecord(recordId: Long) {
        val id = userId.value ?: return
        viewModelScope.launch { repo.deleteRecord(recordId, id) }
    }

    // ---------- doctor actions ----------
    fun patient(id: Long): Flow<User?> = repo.observeUser(id)
    fun patientRecords(id: Long): Flow<List<HealthRecord>> = repo.observeRecords(id)
    fun patientNotes(id: Long): Flow<List<DoctorNote>> = repo.observeNotes(id)

    fun setStatus(patientId: Long, status: String) {
        viewModelScope.launch { repo.setStatus(patientId, status) }
    }

    // We keep this for any older screens that might still call it
    fun addNote(patientId: Long, category: String, text: String) {
        if (text.isBlank()) return
        addStructuredNote(patientId, category, text, null)
    }

    // UPDATED: Builds the DoctorNote directly without needing the Doctor class
    fun addStructuredNote(patientId: Long, category: String, text: String, structuredData: String?) {
        viewModelScope.launch {
            val note = DoctorNote(
                userId = patientId,
                author = "Dr. Amina Otieno", // You can replace this with a dynamic variable if you have one
                category = category,
                text = text,
                date = System.currentTimeMillis(),
                structuredData = structuredData
            )
            repo.addNote(note)
        }
    }

    fun editNote(patientId: Long, noteId: Long, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { repo.editNote(noteId, patientId, text.trim()) }
    }

    fun deleteNote(patientId: Long, noteId: Long) {
        viewModelScope.launch { repo.deleteNote(noteId, patientId) }
    }
}