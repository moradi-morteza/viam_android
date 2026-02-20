package com.app.viam.ui.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.model.Box
import com.app.viam.data.repository.AuthResult
import com.app.viam.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoxDetailUiState(
    val box: Box? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class BoxDetailViewModel(
    private val repository: WarehouseRepository,
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

    fun onErrorDismissed() = _uiState.update { it.copy(error = null) }

    class Factory(
        private val repository: WarehouseRepository,
        private val boxId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BoxDetailViewModel(repository, boxId) as T
    }
}
