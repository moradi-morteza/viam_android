package com.app.viam.ui.parts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.model.Part
import com.app.viam.data.model.PartCategory
import com.app.viam.data.model.PartRequest
import com.app.viam.data.repository.AuthResult
import com.app.viam.data.repository.PartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PartFormUiState(
    val sku: String = "",
    val name: String = "",
    val unit: String = "",
    val description: String = "",
    val selectedCategoryId: Int? = null,
    val categories: List<PartCategory> = emptyList(),
    val isCategoriesLoading: Boolean = false,
    val nameError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val isSaveSuccess: Boolean = false
)

class PartFormViewModel(
    private val repository: PartRepository,
    private val editPart: Part?
) : ViewModel() {

    val isEditMode = editPart != null

    private val _uiState = MutableStateFlow(
        PartFormUiState(
            sku = editPart?.sku ?: "",
            name = editPart?.name ?: "",
            unit = editPart?.unit ?: "",
            description = editPart?.description ?: "",
            selectedCategoryId = editPart?.category?.id
        )
    )
    val uiState: StateFlow<PartFormUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCategoriesLoading = true) }
            when (val result = repository.getPartCategories()) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isCategoriesLoading = false, categories = result.data)
                }
                else -> _uiState.update { it.copy(isCategoriesLoading = false) }
            }
        }
    }

    fun onSkuChange(v: String) = _uiState.update { it.copy(sku = v) }
    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = null) }
    fun onUnitChange(v: String) = _uiState.update { it.copy(unit = v) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onCategorySelected(id: Int?) = _uiState.update { it.copy(selectedCategoryId = id) }
    fun onSaveNavigated() = _uiState.update { it.copy(isSaveSuccess = false) }

    fun onSaveClicked() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "نام قطعه الزامی است") }
            return
        }
        val request = PartRequest(
            sku = state.sku.ifBlank { null },
            name = state.name.trim(),
            description = state.description.ifBlank { null },
            unit = state.unit.ifBlank { null },
            partCategoryId = state.selectedCategoryId
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            val result = if (isEditMode)
                repository.updatePart(editPart!!.id, request)
            else
                repository.createPart(request)
            when (result) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, isSaveSuccess = true) }
                is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, generalError = result.message) }
                is AuthResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, generalError = "اتصال به اینترنت برقرار نیست")
                }
            }
        }
    }

    class Factory(
        private val repository: PartRepository,
        private val editPart: Part?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PartFormViewModel(repository, editPart) as T
    }
}
