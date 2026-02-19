package com.app.viam.ui.parts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.model.Part
import com.app.viam.data.model.User
import com.app.viam.data.repository.AuthResult
import com.app.viam.data.repository.PartRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PartListUiState(
    val parts: List<Part> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val deleteConfirmId: Int? = null,
    val isDeleting: Boolean = false,
    val navigateToCreate: Boolean = false,
    val navigateToEdit: Part? = null
)

@OptIn(FlowPreview::class)
class PartListViewModel(
    private val repository: PartRepository,
    val currentUser: User
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartListUiState())
    val uiState: StateFlow<PartListUiState> = _uiState.asStateFlow()

    private val _searchFlow = MutableStateFlow("")

    init {
        loadParts()
        viewModelScope.launch {
            _searchFlow
                .debounce(350)
                .distinctUntilChanged()
                .collect { query -> loadParts(query) }
        }
    }

    fun loadParts(search: String = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getParts(search)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, parts = result.data)
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is AuthResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, error = "اتصال به اینترنت برقرار نیست")
                }
            }
        }
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchFlow.value = query
    }

    fun onCreateClicked() = _uiState.update { it.copy(navigateToCreate = true) }
    fun onCreateNavigated() = _uiState.update { it.copy(navigateToCreate = false) }

    fun onEditClicked(part: Part) = _uiState.update { it.copy(navigateToEdit = part) }
    fun onEditNavigated() = _uiState.update { it.copy(navigateToEdit = null) }

    fun onDeleteClicked(partId: Int) = _uiState.update { it.copy(deleteConfirmId = partId) }
    fun onDeleteDismissed() = _uiState.update { it.copy(deleteConfirmId = null) }

    fun onDeleteConfirmed() {
        val id = _uiState.value.deleteConfirmId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteConfirmId = null) }
            when (val result = repository.deletePart(id)) {
                is AuthResult.Success -> {
                    _uiState.update { state ->
                        state.copy(isDeleting = false, parts = state.parts.filter { it.id != id })
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

    val canCreate: Boolean get() = currentUser.isAdmin() || currentUser.hasPermission("manage-parts")
    val canEdit: Boolean get() = currentUser.isAdmin() || currentUser.hasPermission("manage-parts")
    val canDelete: Boolean get() = currentUser.isAdmin() || currentUser.hasPermission("manage-parts")

    class Factory(
        private val repository: PartRepository,
        private val currentUser: User
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PartListViewModel(repository, currentUser) as T
    }
}
