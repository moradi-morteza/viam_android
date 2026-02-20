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
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val deleteConfirmId: Int? = null,
    val isDeleting: Boolean = false,
    val navigateToCreate: Boolean = false,
    val navigateToEdit: Part? = null,
    val currentPage: Int = 1,
    val lastPage: Int = 1,
    val total: Int = 0
)

@OptIn(FlowPreview::class)
class PartListViewModel(
    private val repository: PartRepository,
    val currentUser: User
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartListUiState())
    val uiState: StateFlow<PartListUiState> = _uiState.asStateFlow()

    private val _searchFlow = MutableStateFlow("")

    companion object {
        const val PER_PAGE = 20
    }

    init {
        loadParts(page = 1, mode = LoadMode.INITIAL)
        viewModelScope.launch {
            _searchFlow
                .debounce(350)
                .distinctUntilChanged()
                .collect { _ -> loadParts(page = 1, mode = LoadMode.INITIAL) }
        }
    }

    private enum class LoadMode { INITIAL, APPEND, REFRESH }

    private fun loadParts(page: Int, mode: LoadMode) {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || state.isRefreshing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = mode == LoadMode.INITIAL,
                    isLoadingMore = mode == LoadMode.APPEND,
                    isRefreshing = mode == LoadMode.REFRESH,
                    error = null
                )
            }

            val search = _uiState.value.searchQuery
            when (val result = repository.getParts(
                search = search.ifBlank { null },
                page = page,
                perPage = PER_PAGE
            )) {
                is AuthResult.Success -> {
                    val newItems = result.data.data
                    val accumulated = if (mode == LoadMode.APPEND)
                        _uiState.value.parts + newItems
                    else
                        newItems
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            isRefreshing = false,
                            parts = accumulated,
                            currentPage = result.data.currentPage,
                            lastPage = result.data.lastPage,
                            total = result.data.total,
                            canLoadMore = result.data.currentPage < result.data.lastPage
                        )
                    }
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isLoading = false, isLoadingMore = false, isRefreshing = false, error = result.message)
                }
                is AuthResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, isLoadingMore = false, isRefreshing = false, error = "اتصال به اینترنت برقرار نیست")
                }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (!state.canLoadMore || state.isLoadingMore || state.isLoading || state.isRefreshing) return
        loadParts(page = state.currentPage + 1, mode = LoadMode.APPEND)
    }

    fun onRefresh() = loadParts(page = 1, mode = LoadMode.REFRESH)

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
                    _uiState.update { it.copy(isDeleting = false) }
                    loadParts(page = 1, mode = LoadMode.INITIAL)
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
