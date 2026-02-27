package com.app.viam.ui.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.model.Box
import com.app.viam.data.model.PartCategory
import com.app.viam.data.model.PartRequest
import com.app.viam.data.model.TransactionRequest
import com.app.viam.data.repository.AuthResult
import com.app.viam.data.repository.PartRepository
import com.app.viam.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoxDetailUiState(
    val box: Box? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Transaction sheet
    val showTransactionSheet: Boolean = false,
    val isSubmittingTransaction: Boolean = false,
    val transactionError: String? = null,
    val transactionSuccess: Boolean = false,
    // Delete
    val showDeleteConfirm: Boolean = false,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    // Edit part category sheet
    val showEditCategorySheet: Boolean = false,
    val categories: List<PartCategory> = emptyList(),
    val isCategoriesLoading: Boolean = false,
    val selectedCategoryId: Int? = null,
    val isSavingCategory: Boolean = false,
    val categoryError: String? = null
)

class BoxDetailViewModel(
    private val repository: WarehouseRepository,
    private val partRepository: PartRepository,
    private val boxId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoxDetailUiState())
    val uiState: StateFlow<BoxDetailUiState> = _uiState.asStateFlow()

    init {
        loadBox()
    }

    fun loadBox() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getBox(boxId)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, box = result.data)
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

    fun onAddTransactionClicked() =
        _uiState.update { it.copy(showTransactionSheet = true, transactionError = null) }

    fun onTransactionSheetDismissed() =
        _uiState.update { it.copy(showTransactionSheet = false, transactionError = null) }

    fun submitTransaction(request: TransactionRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingTransaction = true, transactionError = null) }
            when (val result = repository.createTransaction(boxId, request)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingTransaction = false,
                            showTransactionSheet = false,
                            transactionError = null,
                            transactionSuccess = true
                        )
                    }
                    // Reload box so quantity + transaction list are up to date
                    loadBox()
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isSubmittingTransaction = false, transactionError = result.message)
                }
                is AuthResult.NetworkError -> _uiState.update {
                    it.copy(
                        isSubmittingTransaction = false,
                        transactionError = "اتصال به اینترنت برقرار نیست"
                    )
                }
            }
        }
    }

    fun onDeleteClicked() = _uiState.update { it.copy(showDeleteConfirm = true) }
    fun onDeleteDismissed() = _uiState.update { it.copy(showDeleteConfirm = false) }
    fun onDeleteConfirmed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showDeleteConfirm = false) }
            when (val r = repository.deleteBox(boxId)) {
                is AuthResult.Success -> _uiState.update { it.copy(isDeleting = false, deleted = true) }
                is AuthResult.Error -> _uiState.update { it.copy(isDeleting = false, error = r.message) }
                is AuthResult.NetworkError -> _uiState.update { it.copy(isDeleting = false, error = "اتصال به اینترنت برقرار نیست") }
            }
        }
    }

    fun onErrorDismissed() = _uiState.update { it.copy(error = null) }
    fun onTransactionSuccessConsumed() = _uiState.update { it.copy(transactionSuccess = false) }

    // Edit part category
    fun onEditCategoryClicked() {
        val currentCategoryId = _uiState.value.box?.part?.category?.id
        _uiState.update {
            it.copy(
                showEditCategorySheet = true,
                selectedCategoryId = currentCategoryId,
                categoryError = null
            )
        }
        if (_uiState.value.categories.isEmpty()) loadCategories()
    }

    fun onEditCategoryDismissed() =
        _uiState.update { it.copy(showEditCategorySheet = false, categoryError = null) }

    fun onCategorySelected(id: Int?) =
        _uiState.update { it.copy(selectedCategoryId = id) }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCategoriesLoading = true) }
            when (val result = partRepository.getPartCategories()) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isCategoriesLoading = false, categories = result.data)
                }
                else -> _uiState.update { it.copy(isCategoriesLoading = false) }
            }
        }
    }

    fun onSaveCategoryClicked() {
        val part = _uiState.value.box?.part ?: return
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingCategory = true, categoryError = null) }
            val request = PartRequest(
                sku = part.sku,
                name = part.name,
                description = part.description,
                unit = part.unit,
                partCategoryId = state.selectedCategoryId
            )
            when (val result = partRepository.updatePart(part.id, request)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isSavingCategory = false, showEditCategorySheet = false) }
                    loadBox()
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isSavingCategory = false, categoryError = result.message)
                }
                is AuthResult.NetworkError -> _uiState.update {
                    it.copy(isSavingCategory = false, categoryError = "اتصال به اینترنت برقرار نیست")
                }
            }
        }
    }

    class Factory(
        private val repository: WarehouseRepository,
        private val partRepository: PartRepository,
        private val boxId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BoxDetailViewModel(repository, partRepository, boxId) as T
    }
}
