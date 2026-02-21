package com.app.viam.ui.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.model.Box
import com.app.viam.data.model.TransactionRequest
import com.app.viam.data.repository.AuthResult
import com.app.viam.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanTransactUiState(
    val isLoadingBox: Boolean = false,
    val box: Box? = null,
    val error: String? = null,
    val showTransactionSheet: Boolean = false,
    val isSubmitting: Boolean = false,
    val transactionError: String? = null,
    val transactionSuccess: Boolean = false,
)

class ScanTransactViewModel(
    private val repository: WarehouseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanTransactUiState())
    val uiState: StateFlow<ScanTransactUiState> = _uiState.asStateFlow()

    /**
     * Called when a QR code is scanned. Expected format: "box-{id}" (e.g. "box-42").
     */
    fun onQrScanned(raw: String) {
        val trimmed = raw.trim()
        val boxId = if (trimmed.startsWith("box-", ignoreCase = true)) {
            trimmed.substring(4).toIntOrNull()
        } else {
            trimmed.toIntOrNull() // fallback: plain numeric id
        }
        if (boxId == null) {
            _uiState.update { it.copy(error = "کیوآر کد نامعتبر است") }
            return
        }
        loadBox { repository.getBox(boxId) }
    }

    /**
     * Called when the user manually types a box code and taps Search.
     */
    fun onCodeEntered(code: String) {
        if (code.isBlank()) return
        loadBox { repository.getBoxByCode(code.trim()) }
    }

    private fun loadBox(fetch: suspend () -> AuthResult<Box>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBox = true, error = null, box = null, showTransactionSheet = false) }
            when (val result = fetch()) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoadingBox = false, box = result.data, showTransactionSheet = true)
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isLoadingBox = false, error = result.message)
                }
                is AuthResult.NetworkError -> _uiState.update {
                    it.copy(isLoadingBox = false, error = "خطا در اتصال به سرور")
                }
            }
        }
    }

    fun submitTransaction(request: TransactionRequest) {
        val box = _uiState.value.box ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, transactionError = null) }
            when (val result = repository.createTransaction(box.id, request)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isSubmitting = false, transactionSuccess = true, showTransactionSheet = false, box = null)
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isSubmitting = false, transactionError = result.message)
                }
                is AuthResult.NetworkError -> _uiState.update {
                    it.copy(isSubmitting = false, transactionError = "خطا در اتصال به سرور")
                }
            }
        }
    }

    fun onDismissSheet() {
        _uiState.update { it.copy(showTransactionSheet = false, box = null) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    fun onTransactionErrorDismissed() {
        _uiState.update { it.copy(transactionError = null) }
    }

    fun onSuccessConsumed() {
        _uiState.update { it.copy(transactionSuccess = false) }
    }

    /** Reset for a new scan/search cycle (e.g. after success snackbar is shown). */
    fun resetForNewScan() {
        _uiState.update { ScanTransactUiState() }
    }

    class Factory(private val repo: WarehouseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ScanTransactViewModel(repo) as T
    }
}
