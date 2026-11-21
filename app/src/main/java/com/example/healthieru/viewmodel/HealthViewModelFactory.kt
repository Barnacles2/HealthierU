package com.example.healthieru.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.healthieru.data.SessionManager
import com.example.healthieru.data.repository.HealthRepository

class HealthViewModelFactory(
    private val repository: HealthRepository,
    private val session: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HealthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HealthViewModel(repository, session) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}