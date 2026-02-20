package com.app.viam.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.model.User
import com.app.viam.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val user: User? = null,
    val isLoggingOut: Boolean = false,
    val isLoggedOut: Boolean = false
)

class HomeViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.userFlow.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun refreshMe() {
        viewModelScope.launch {
            authRepository.fetchMe()
        }
    }

    fun onLogoutNavigated() = _uiState.update { it.copy(isLoggedOut = false) }

    fun onLogoutConfirmed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false, isLoggedOut = true) }
        }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository) as T
    }
}
