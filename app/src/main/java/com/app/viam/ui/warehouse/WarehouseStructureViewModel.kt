package com.app.viam.ui.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.viam.data.model.Row
import com.app.viam.data.model.RowRequest
import com.app.viam.data.model.Shelf
import com.app.viam.data.model.ShelfRequest
import com.app.viam.data.model.Zone
import com.app.viam.data.model.ZoneRequest
import com.app.viam.data.repository.AuthResult
import com.app.viam.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Which dialog is open
sealed interface StructureDialog {
    data object AddZone : StructureDialog
    data class EditZone(val zone: Zone) : StructureDialog
    data class DeleteZone(val zone: Zone) : StructureDialog
    data class AddShelf(val zone: Zone) : StructureDialog
    data class EditShelf(val shelf: Shelf) : StructureDialog
    data class DeleteShelf(val shelf: Shelf) : StructureDialog
    data class AddRow(val shelf: Shelf) : StructureDialog
    data class EditRow(val row: Row) : StructureDialog
    data class DeleteRow(val row: Row) : StructureDialog
}

data class WarehouseStructureUiState(
    val zones: List<Zone> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val expandedZoneIds: Set<Int> = emptySet(),
    val expandedShelfIds: Set<Int> = emptySet(),
    // Dialog state
    val dialog: StructureDialog? = null,
    val isSaving: Boolean = false,
    val dialogError: String? = null,
    // Cascading data needed for AddShelf/AddRow dialogs
    val zonesForPicker: List<Zone> = emptyList(),
    val shelvesForPicker: List<Shelf> = emptyList(),
    val isLoadingShelvesForPicker: Boolean = false
)

class WarehouseStructureViewModel(
    private val repository: WarehouseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WarehouseStructureUiState())
    val uiState: StateFlow<WarehouseStructureUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = repository.getZones()) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, zones = r.data)
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = r.message)
                }
                AuthResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, error = "اتصال به اینترنت برقرار نیست")
                }
            }
        }
    }

    // ── Expand / collapse ────────────────────────────────────────────────────

    fun toggleZone(zoneId: Int) = _uiState.update {
        val expanded = it.expandedZoneIds.toMutableSet()
        if (!expanded.add(zoneId)) expanded.remove(zoneId)
        it.copy(expandedZoneIds = expanded)
    }

    fun toggleShelf(shelfId: Int) = _uiState.update {
        val expanded = it.expandedShelfIds.toMutableSet()
        if (!expanded.add(shelfId)) expanded.remove(shelfId)
        it.copy(expandedShelfIds = expanded)
    }

    // ── Dialog open ──────────────────────────────────────────────────────────

    fun openDialog(dialog: StructureDialog) =
        _uiState.update { it.copy(dialog = dialog, dialogError = null, shelvesForPicker = emptyList()) }

    fun closeDialog() =
        _uiState.update { it.copy(dialog = null, dialogError = null, isSaving = false, shelvesForPicker = emptyList()) }

    // ── Zone CRUD ────────────────────────────────────────────────────────────

    fun saveZone(name: String, description: String?) {
        val dialog = _uiState.value.dialog ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, dialogError = null) }
            val result = when (dialog) {
                is StructureDialog.AddZone ->
                    repository.createZone(ZoneRequest(name, description?.ifBlank { null }))
                is StructureDialog.EditZone ->
                    repository.updateZone(dialog.zone.id, ZoneRequest(name, description?.ifBlank { null }))
                else -> return@launch
            }
            handleSaveResult(result)
        }
    }

    fun deleteZone(zone: Zone) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, dialogError = null) }
            handleSaveResult(repository.deleteZone(zone.id))
        }
    }

    // ── Shelf CRUD ───────────────────────────────────────────────────────────

    fun saveShelf(zoneId: Int, name: String, description: String?) {
        val dialog = _uiState.value.dialog ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, dialogError = null) }
            val result = when (dialog) {
                is StructureDialog.AddShelf ->
                    repository.createShelf(ShelfRequest(zoneId, name, description?.ifBlank { null }))
                is StructureDialog.EditShelf ->
                    repository.updateShelf(dialog.shelf.id, ShelfRequest(zoneId, name, description?.ifBlank { null }))
                else -> return@launch
            }
            handleSaveResult(result)
        }
    }

    fun deleteShelf(shelf: Shelf) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, dialogError = null) }
            handleSaveResult(repository.deleteShelf(shelf.id))
        }
    }

    // ── Row CRUD ─────────────────────────────────────────────────────────────

    fun saveRow(shelfId: Int, name: String, description: String?) {
        val dialog = _uiState.value.dialog ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, dialogError = null) }
            val result = when (dialog) {
                is StructureDialog.AddRow ->
                    repository.createRow(RowRequest(shelfId, name, description?.ifBlank { null }))
                is StructureDialog.EditRow ->
                    repository.updateRow(dialog.row.id, RowRequest(shelfId, name, description?.ifBlank { null }))
                else -> return@launch
            }
            handleSaveResult(result)
        }
    }

    fun deleteRow(row: Row) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, dialogError = null) }
            handleSaveResult(repository.deleteRow(row.id))
        }
    }

    // ── Cascading picker helpers ──────────────────────────────────────────────

    fun loadShelvesForZone(zoneId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingShelvesForPicker = true) }
            when (val r = repository.getShelves(zoneId)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(shelvesForPicker = r.data, isLoadingShelvesForPicker = false)
                }
                else -> _uiState.update { it.copy(isLoadingShelvesForPicker = false) }
            }
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun <T> handleSaveResult(result: AuthResult<T>) {
        when (result) {
            is AuthResult.Success -> {
                _uiState.update { it.copy(isSaving = false, dialog = null, dialogError = null) }
                load() // reload the full tree
            }
            is AuthResult.Error -> _uiState.update {
                it.copy(isSaving = false, dialogError = result.message)
            }
            AuthResult.NetworkError -> _uiState.update {
                it.copy(isSaving = false, dialogError = "اتصال به اینترنت برقرار نیست")
            }
        }
    }

    class Factory(private val repository: WarehouseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WarehouseStructureViewModel(repository) as T
    }
}
