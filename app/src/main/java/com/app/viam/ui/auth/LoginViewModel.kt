package com.app.viam.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.repository.AuthRepository
import com.app.viam.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val generalError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isLoginSuccess: Boolean = false
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) =
        _uiState.update { it.copy(username = value, usernameError = null, generalError = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }

    fun onTogglePasswordVisibility() =
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun onLoginClicked() {
        val state = _uiState.value
        if (state.username.isBlank()) {
            _uiState.update { it.copy(usernameError = "نام کاربری الزامی است") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "رمز عبور الزامی است") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            when (val result = authRepository.login(state.username, state.password)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isLoginSuccess = true)
                }
                is AuthResult.Error -> {
                    val usernameErr = result.fieldErrors?.get("username")?.firstOrNull()
                    val passwordErr = result.fieldErrors?.get("password")?.firstOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = if (usernameErr == null && passwordErr == null) result.message else null,
                            usernameError = usernameErr,
                            passwordError = passwordErr
                        )
                    }
                }
                is AuthResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, generalError = "اتصال به اینترنت برقرار نیست")
                }
            }
        }
    }

    fun onLoginNavigated() = _uiState.update { it.copy(isLoginSuccess = false) }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoginViewModel(repository) as T
    }
}
