
package com.example.healthieru.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthieru.data.local.HealthRecord
import com.example.healthieru.data.local.User
import com.example.healthieru.data.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.healthieru.data.remote.RetrofitClient

class HealthViewModel(
    private val repository: HealthRepository
) : ViewModel() {

    val allRecords: Flow<List<HealthRecord>> = repository.allRecords

    val user: Flow<User?> = repository.user

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun getRecord(id: Int): Flow<HealthRecord?> {
        return repository.getRecord(id)
    }

    fun insertRecord(record: HealthRecord) {
        viewModelScope.launch {
            try {
                repository.insert(record)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Unable to save health record."
            }
        }
    }

    fun updateRecord(record: HealthRecord) {
        viewModelScope.launch {
            try {
                repository.update(record)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Unable to update health record."
            }
        }
    }

    fun deleteRecord(record: HealthRecord) {
        viewModelScope.launch {
            try {
                repository.delete(record)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Unable to delete health record."
            }
        }
    }

    fun saveUser(user: User, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveUser(user)
                _errorMessage.value = null
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Unable to save user profile."
            }
        }
    }

    fun saveHealthData(
        record: HealthRecord,
        user: User,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.insert(record)
                repository.saveUser(user)
                _errorMessage.value = null
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save health data."
            }
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

    fun clearError() {
        _errorMessage.value = null
    }
}
