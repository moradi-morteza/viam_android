package com.app.viam.ui.personnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.model.User
import com.app.viam.data.repository.AuthResult
import com.app.viam.data.repository.PersonnelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonnelListUiState(
    val staffList: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val deleteConfirmId: Int? = null,        // ID of staff pending delete confirmation
    val isDeleting: Boolean = false,
    val navigateToCreate: Boolean = false,
    val navigateToEdit: User? = null          // staff to edit
)

class PersonnelListViewModel(
    private val repository: PersonnelRepository,
    val currentUser: User
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonnelListUiState())
    val uiState: StateFlow<PersonnelListUiState> = _uiState.asStateFlow()

    init {
        loadStaffs()
    }

    fun loadStaffs(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            when (val result = repository.getStaffs()) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, staffList = result.data)
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = result.message)
                }
                is AuthResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = "اتصال به اینترنت برقرار نیست")
                }
            }
        }
    }

    fun onRefresh() = loadStaffs(isRefresh = true)

    fun onCreateClicked() = _uiState.update { it.copy(navigateToCreate = true) }
    fun onCreateNavigated() = _uiState.update { it.copy(navigateToCreate = false) }

    fun onEditClicked(staff: User) = _uiState.update { it.copy(navigateToEdit = staff) }
    fun onEditNavigated() = _uiState.update { it.copy(navigateToEdit = null) }

    fun onDeleteClicked(staffId: Int) = _uiState.update { it.copy(deleteConfirmId = staffId) }
    fun onDeleteDismissed() = _uiState.update { it.copy(deleteConfirmId = null) }

    fun onDeleteConfirmed() {
        val id = _uiState.value.deleteConfirmId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteConfirmId = null) }
            when (val result = repository.deleteStaff(id)) {
                is AuthResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isDeleting = false,
                            staffList = state.staffList.filter { it.id != id }
                        )
                    }
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isDeleting = false, error = result.message)
                }
                is AuthResult.NetworkError -> _uiState.update {
                    it.copy(isDeleting = false, error = "اتصال به اینترنت برقرار نیست")
                }
            }
        }
    }

    fun onErrorDismissed() = _uiState.update { it.copy(error = null) }

    // Permission helpers based on the logged-in user
    val canCreate: Boolean get() = currentUser.isAdmin() || currentUser.hasPermission("create-personnel")
    val canEdit: Boolean get() = currentUser.isAdmin() || currentUser.hasPermission("edit-personnel")
    val canDelete: Boolean get() = currentUser.isAdmin() || currentUser.hasPermission("delete-personnel")

    class Factory(
        private val repository: PersonnelRepository,
        private val currentUser: User
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PersonnelListViewModel(repository, currentUser) as T
    }
}
