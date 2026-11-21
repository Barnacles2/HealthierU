package com.example.healthieru.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.healthieru.data.SessionManager
import com.example.healthieru.data.repository.HealthRepository

class HealthViewModelFactory(
    private val repo: HealthRepository,
    private val session: SessionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = HealthViewModel(repo, session) as T
}