package com.app.viam.ui.personnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.model.CreateStaffRequest
import com.app.viam.data.model.Permission
import com.app.viam.data.model.Role
import com.app.viam.data.model.UpdateStaffRequest
import com.app.viam.data.model.User
import com.app.viam.data.repository.AuthResult
import com.app.viam.data.repository.PersonnelRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffFormUiState(
    // Basic fields
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val email: String = "",
    val mobile: String = "",
    val address: String = "",
    // Role & permissions
    val roles: List<Role> = emptyList(),
    val isRolesLoading: Boolean = false,
    val selectedRoleId: Int? = null,
    val permissionsByCategory: Map<String, List<Permission>> = emptyMap(),
    val isPermissionsLoading: Boolean = false,
    // IDs of permissions directly assigned (not via role)
    val directPermissionIds: Set<Int> = emptySet(),
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
) {
    /** Permission IDs granted by the currently selected role */
    val rolePermissionIds: Set<Int>
        get() {
            val role = roles.firstOrNull { it.id == selectedRoleId } ?: return emptySet()
            return role.permissions.map { it.id }.toSet()
        }

    /** A permission is "enabled" if it comes from the role OR is directly assigned */
    fun isPermissionEnabled(permId: Int) =
        rolePermissionIds.contains(permId) || directPermissionIds.contains(permId)

    fun isFromRole(permId: Int) = rolePermissionIds.contains(permId)
}

class StaffFormViewModel(
    private val repository: PersonnelRepository,
    val editingStaff: User?
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

    init {
        loadRolesAndPermissions()
    }

    private fun loadRolesAndPermissions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRolesLoading = true, isPermissionsLoading = true) }

            val rolesDeferred = async { repository.getRoles() }
            val permsDeferred = async { repository.getPermissionsGrouped() }

            val rolesResult = rolesDeferred.await()
            val permsResult = permsDeferred.await()

            val roles = if (rolesResult is AuthResult.Success) rolesResult.data else emptyList()
            val perms = if (permsResult is AuthResult.Success) permsResult.data else emptyMap()

            _uiState.update { state ->
                // Pre-fill role and direct permissions when editing
                val selectedRoleId: Int?
                val directPermissionIds: Set<Int>
                if (editingStaff != null) {
                    // The backend returns all user permissions in permissions list.
                    // We need to load the staff detail to get direct_permissions separately.
                    // For now, pre-select based on what the staff's current permissions contain
                    // that are not in any matched role.
                    // We'll fetch the staff detail to get role and direct permissions.
                    selectedRoleId = null  // will be set after detail fetch below
                    directPermissionIds = emptySet()
                } else {
                    selectedRoleId = null
                    directPermissionIds = emptySet()
                }
                state.copy(
                    roles = roles,
                    isRolesLoading = false,
                    permissionsByCategory = perms,
                    isPermissionsLoading = false,
                    selectedRoleId = selectedRoleId,
                    directPermissionIds = directPermissionIds
                )
            }

            // For edit mode, fetch full staff detail to get role + direct permissions
            if (editingStaff != null) {
                fetchStaffDetail(editingStaff.id)
            }
        }
    }

    private suspend fun fetchStaffDetail(id: Int) {
        val result = repository.getStaff(id)
        if (result is AuthResult.Success) {
            val staff = result.data
            // Backend returns role as a string name — match against loaded roles list
            val matchedRole = _uiState.value.roles.firstOrNull { it.name == staff.role }
            val rolePermIds = matchedRole?.permissions?.map { it.id }?.toSet() ?: emptySet()
            // Direct permissions = staff permissions NOT in the role
            val directIds = staff.permissions
                .map { it.id }
                .filter { !rolePermIds.contains(it) }
                .toSet()
            _uiState.update {
                it.copy(
                    selectedRoleId = matchedRole?.id,
                    directPermissionIds = directIds
                )
            }
        }
    }

    // --- Field handlers ---
    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = null, generalError = null) }
    fun onUsernameChange(v: String) = _uiState.update { it.copy(username = v, usernameError = null, generalError = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, passwordError = null, generalError = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null) }
    fun onMobileChange(v: String) = _uiState.update { it.copy(mobile = v, mobileError = null) }
    fun onAddressChange(v: String) = _uiState.update { it.copy(address = v) }
    fun onTogglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    fun onSaveNavigated() = _uiState.update { it.copy(isSaveSuccess = false) }
    fun onErrorDismissed() = _uiState.update { it.copy(generalError = null) }

    fun onRoleSelected(roleId: Int?) {
        _uiState.update { it.copy(selectedRoleId = roleId) }
    }

    fun onTogglePermission(permId: Int) {
        _uiState.update { state ->
            // Cannot toggle role-based permissions
            if (state.isFromRole(permId)) return@update state
            val updated = if (state.directPermissionIds.contains(permId)) {
                state.directPermissionIds - permId
            } else {
                state.directPermissionIds + permId
            }
            state.copy(directPermissionIds = updated)
        }
    }

    fun onSaveClicked() {
        val s = _uiState.value

        if (s.name.isBlank()) { _uiState.update { it.copy(nameError = "نام الزامی است") }; return }
        if (s.username.isBlank()) { _uiState.update { it.copy(usernameError = "نام کاربری الزامی است") }; return }
        if (!isEditMode && s.password.isBlank()) { _uiState.update { it.copy(passwordError = "رمز عبور الزامی است") }; return }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            val directPermIds = s.directPermissionIds.toList()

            val result = if (isEditMode) {
                repository.updateStaff(
                    editingStaff!!.id,
                    UpdateStaffRequest(
                        name = s.name,
                        username = s.username,
                        password = s.password.ifBlank { null },
                        email = s.email.ifBlank { null },
                        mobile = s.mobile.ifBlank { null },
                        address = s.address.ifBlank { null },
                        roleId = s.selectedRoleId,
                        permissions = directPermIds
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
                        address = s.address.ifBlank { null },
                        roleId = s.selectedRoleId,
                        permissions = directPermIds
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
