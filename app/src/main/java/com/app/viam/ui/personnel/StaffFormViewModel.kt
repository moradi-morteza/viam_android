package com.app.viam.ui.personnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.model.CreateStaffRequest
import com.app.viam.data.model.UpdateStaffRequest
import com.app.viam.data.model.User
import com.app.viam.data.repository.AuthResult
import com.app.viam.data.repository.PersonnelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffFormUiState(
    // Fields
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val email: String = "",
    val mobile: String = "",
    val address: String = "",
    // UI state
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isSaveSuccess: Boolean = false,
    val generalError: String? = null,
    // Field errors from backend
    val nameError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val emailError: String? = null,
    val mobileError: String? = null
)

class StaffFormViewModel(
    private val repository: PersonnelRepository,
    val editingStaff: User?       // null = create mode, non-null = edit mode
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        if (editingStaff != null) {
            StaffFormUiState(
                name = editingStaff.name,
                username = editingStaff.username,
                email = editingStaff.email ?: "",
                mobile = editingStaff.mobile ?: "",
                address = editingStaff.address ?: ""
            )
        } else {
            StaffFormUiState()
        }
    )
    val uiState: StateFlow<StaffFormUiState> = _uiState.asStateFlow()

    val isEditMode: Boolean get() = editingStaff != null

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = null, generalError = null) }
    fun onUsernameChange(v: String) = _uiState.update { it.copy(username = v, usernameError = null, generalError = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, passwordError = null, generalError = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null) }
    fun onMobileChange(v: String) = _uiState.update { it.copy(mobile = v, mobileError = null) }
    fun onAddressChange(v: String) = _uiState.update { it.copy(address = v) }
    fun onTogglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    fun onSaveNavigated() = _uiState.update { it.copy(isSaveSuccess = false) }
    fun onErrorDismissed() = _uiState.update { it.copy(generalError = null) }

    fun onSaveClicked() {
        val s = _uiState.value

        // Client-side validation
        if (s.name.isBlank()) { _uiState.update { it.copy(nameError = "نام الزامی است") }; return }
        if (s.username.isBlank()) { _uiState.update { it.copy(usernameError = "نام کاربری الزامی است") }; return }
        if (!isEditMode && s.password.isBlank()) { _uiState.update { it.copy(passwordError = "رمز عبور الزامی است") }; return }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            val result = if (isEditMode) {
                repository.updateStaff(
                    editingStaff!!.id,
                    UpdateStaffRequest(
                        name = s.name,
                        username = s.username,
                        password = s.password.ifBlank { null },
                        email = s.email.ifBlank { null },
                        mobile = s.mobile.ifBlank { null },
                        address = s.address.ifBlank { null }
                    )
                )
            } else {
                repository.createStaff(
                    CreateStaffRequest(
                        name = s.name,
                        username = s.username,
                        password = s.password,
                        email = s.email.ifBlank { null },
                        mobile = s.mobile.ifBlank { null },
                        address = s.address.ifBlank { null }
                    )
                )
            }

            when (result) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, isSaveSuccess = true) }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = if (result.fieldErrors.isNullOrEmpty()) result.message else null,
                            nameError = result.fieldErrors?.get("name")?.firstOrNull(),
                            usernameError = result.fieldErrors?.get("username")?.firstOrNull(),
                            passwordError = result.fieldErrors?.get("password")?.firstOrNull(),
                            emailError = result.fieldErrors?.get("email")?.firstOrNull(),
                            mobileError = result.fieldErrors?.get("mobile")?.firstOrNull()
                        )
                    }
                }
                is AuthResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, generalError = "اتصال به اینترنت برقرار نیست")
                }
            }
        }
    }

    class Factory(
        private val repository: PersonnelRepository,
        private val editingStaff: User?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StaffFormViewModel(repository, editingStaff) as T
    }
}
