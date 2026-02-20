package com.app.viam.ui.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.model.Box
import com.app.viam.data.model.Zone
import com.app.viam.data.model.User
import com.app.viam.data.repository.AuthResult
import com.app.viam.data.repository.WarehouseRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WarehouseListUiState(
    val boxes: List<Box> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val canLoadMore: Boolean = false,
    val currentPage: Int = 1,
    val total: Int = 0,
    val error: String? = null,
    val searchQuery: String = "",
    // Active filter
    val activeFilterZoneId: Int? = null,
    val activeFilterZoneName: String? = null,
    val activeFilterShelfName: String? = null,
    val activeFilterRowId: Int? = null,
    val activeFilterRowName: String? = null,
    // Filter sheet
    val showFilterSheet: Boolean = false,
    val treeData: List<Zone> = emptyList(),
    val isTreeLoading: Boolean = false,
    // Navigation
    val navigateToDetail: Int? = null
) {
    val hasActiveFilter: Boolean get() = activeFilterZoneId != null || activeFilterRowId != null
}

@OptIn(FlowPreview::class)
class WarehouseListViewModel(
    private val repository: WarehouseRepository,
    val currentUser: User
) : ViewModel() {

    private val _uiState = MutableStateFlow(WarehouseListUiState())
    val uiState: StateFlow<WarehouseListUiState> = _uiState.asStateFlow()

    private val _searchFlow = MutableStateFlow("")

    companion object {
        const val PER_PAGE = 20
    }

    private enum class LoadMode { INITIAL, APPEND, REFRESH }

    init {
        loadBoxes(page = 1, mode = LoadMode.INITIAL)
        viewModelScope.launch {
            _searchFlow
                .debounce(350)
                .distinctUntilChanged()
                .collect { _ -> loadBoxes(page = 1, mode = LoadMode.INITIAL) }
        }
    }

    private fun loadBoxes(page: Int, mode: LoadMode) {
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

            val s = _uiState.value
            when (val result = repository.getBoxes(
                search = s.searchQuery.ifBlank { null },
                page = page,
                perPage = PER_PAGE,
                zoneId = s.activeFilterZoneId,
                rowId = s.activeFilterRowId
            )) {
                is AuthResult.Success -> {
                    val newItems = result.data.data
                    val accumulated = if (mode == LoadMode.APPEND)
                        _uiState.value.boxes + newItems
                    else
                        newItems
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            isRefreshing = false,
                            boxes = accumulated,
                            currentPage = result.data.currentPage,
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
        loadBoxes(page = state.currentPage + 1, mode = LoadMode.APPEND)
    }

    fun onRefresh() = loadBoxes(page = 1, mode = LoadMode.REFRESH)

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchFlow.value = query
    }

    // Filter sheet
    fun onFilterClicked() {
        _uiState.update { it.copy(showFilterSheet = true) }
        if (_uiState.value.treeData.isEmpty()) loadTree()
    }

    fun onFilterDismissed() = _uiState.update { it.copy(showFilterSheet = false) }

    fun onFilterByZone(zoneId: Int, zoneName: String) {
        _uiState.update {
            it.copy(
                activeFilterZoneId = zoneId,
                activeFilterZoneName = zoneName,
                activeFilterShelfName = null,
                activeFilterRowId = null,
                activeFilterRowName = null,
                showFilterSheet = false
            )
        }
        loadBoxes(page = 1, mode = LoadMode.INITIAL)
    }

    fun onFilterByRow(rowId: Int, rowName: String, shelfName: String, zoneId: Int, zoneName: String) {
        _uiState.update {
            it.copy(
                activeFilterZoneId = zoneId,
                activeFilterZoneName = zoneName,
                activeFilterShelfName = shelfName,
                activeFilterRowId = rowId,
                activeFilterRowName = rowName,
                showFilterSheet = false
            )
        }
        loadBoxes(page = 1, mode = LoadMode.INITIAL)
    }

    fun onClearFilter() {
        _uiState.update {
            it.copy(
                activeFilterZoneId = null,
                activeFilterZoneName = null,
                activeFilterShelfName = null,
                activeFilterRowId = null,
                activeFilterRowName = null,
                showFilterSheet = false
            )
        }
        loadBoxes(page = 1, mode = LoadMode.INITIAL)
    }

    private fun loadTree() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTreeLoading = true) }
            when (val result = repository.getWarehouseTree()) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isTreeLoading = false, treeData = result.data)
                }
                else -> _uiState.update { it.copy(isTreeLoading = false) }
            }
        }
    }

    // Navigation
    fun onBoxClicked(boxId: Int) = _uiState.update { it.copy(navigateToDetail = boxId) }
    fun onDetailNavigated() = _uiState.update { it.copy(navigateToDetail = null) }

    fun onErrorDismissed() = _uiState.update { it.copy(error = null) }

    val canViewWarehouse: Boolean get() =
        currentUser.isAdmin() || currentUser.hasPermission("view-warehouse")

    class Factory(
        private val repository: WarehouseRepository,
        private val currentUser: User
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WarehouseListViewModel(repository, currentUser) as T
    }
}
