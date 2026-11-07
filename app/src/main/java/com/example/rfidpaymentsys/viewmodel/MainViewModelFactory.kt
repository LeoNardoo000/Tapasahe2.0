package com.example.rfidpaymentsys.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.rfidpaymentsys.data.local.SessionStore
import com.example.rfidpaymentsys.data.repository.UserRepository

class MainViewModelFactory(
    private val repository: UserRepository,
    private val sessionStore: SessionStore? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, sessionStore = sessionStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}