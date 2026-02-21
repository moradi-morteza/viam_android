package com.app.viam.ui.parts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.model.PartCategory
import com.app.viam.data.model.PartCategoryRequest
import com.app.viam.data.model.User
import com.app.viam.data.repository.AuthResult
import com.app.viam.data.repository.PartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PartCategoryUiState(
    val categories: List<PartCategory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // inline form (edit or create)
    val showForm: Boolean = false,
    val editingCategory: PartCategory? = null,  // null = create mode
    val formName: String = "",
    val formDescription: String = "",
    val formNameError: String? = null,
    val isFormSaving: Boolean = false,
    val formError: String? = null,
    // delete
    val deleteConfirmId: Int? = null,
    val isDeleting: Boolean = false
)

class PartCategoryViewModel(
    private val repository: PartRepository,
    val currentUser: User
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartCategoryUiState())
    val uiState: StateFlow<PartCategoryUiState> = _uiState.asStateFlow()

    val canCreate: Boolean get() = currentUser.isAdmin() || currentUser.hasPermission("create-part-categories")
    val canEdit: Boolean get() = currentUser.isAdmin() || currentUser.hasPermission("edit-part-categories")
    val canDelete: Boolean get() = currentUser.isAdmin() || currentUser.hasPermission("delete-part-categories")

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getPartCategories()) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, categories = result.data)
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

    fun onCreateClicked() {
        _uiState.update {
            it.copy(showForm = true, editingCategory = null, formName = "", formDescription = "", formNameError = null, formError = null)
        }
    }

    fun onEditClicked(category: PartCategory) {
        _uiState.update {
            it.copy(showForm = true, editingCategory = category, formName = category.name, formDescription = category.description ?: "", formNameError = null, formError = null)
        }
    }

    fun onFormDismiss() {
        _uiState.update { it.copy(showForm = false, editingCategory = null, formNameError = null, formError = null) }
    }

    fun onFormNameChange(v: String) = _uiState.update { it.copy(formName = v, formNameError = null) }
    fun onFormDescriptionChange(v: String) = _uiState.update { it.copy(formDescription = v) }

    fun onFormSave() {
        val state = _uiState.value
        if (state.formName.isBlank()) {
            _uiState.update { it.copy(formNameError = "نام دسته‌بندی الزامی است") }
            return
        }
        val request = PartCategoryRequest(
            name = state.formName.trim(),
            description = state.formDescription.ifBlank { null }
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isFormSaving = true, formError = null) }
            val result = if (state.editingCategory != null)
                repository.updatePartCategory(state.editingCategory.id, request)
            else
                repository.createPartCategory(request)
            when (result) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isFormSaving = false, showForm = false, editingCategory = null) }
                    load()
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isFormSaving = false, formError = result.message)
                }
                is AuthResult.NetworkError -> _uiState.update {
                    it.copy(isFormSaving = false, formError = "اتصال به اینترنت برقرار نیست")
                }
            }
        }
    }

    fun onDeleteClicked(id: Int) = _uiState.update { it.copy(deleteConfirmId = id) }
    fun onDeleteDismissed() = _uiState.update { it.copy(deleteConfirmId = null) }

    fun onDeleteConfirmed() {
        val id = _uiState.value.deleteConfirmId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteConfirmId = null) }
            when (val result = repository.deletePartCategory(id)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    load()
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

    class Factory(
        private val repository: PartRepository,
        private val currentUser: User
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PartCategoryViewModel(repository, currentUser) as T
    }
}
