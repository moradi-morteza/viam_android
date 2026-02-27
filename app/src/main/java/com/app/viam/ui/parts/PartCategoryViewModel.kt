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

/** A PartCategory entry enriched with its full ancestor path for display in the dropdown */
data class PartCategoryTreeItem(
    val id: Int,
    val name: String,
    val fullPath: String   // e.g. "الکترونیک / برد مدار / مقاومت"
)

data class PartCategoryUiState(
    val categories: List<PartCategory> = emptyList(),
    // id → full ancestor path, e.g. "الکترونیک / برد مدار / مقاومت"
    val fullPathMap: Map<Int, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // inline form (edit or create)
    val showForm: Boolean = false,
    val editingCategory: PartCategory? = null,
    val formName: String = "",
    val formDescription: String = "",
    val formParentId: Int? = null,
    val formNameError: String? = null,
    val isFormSaving: Boolean = false,
    val formError: String? = null,
    // available parents for the dropdown (tree-ordered, excludes self+descendants when editing)
    val parentOptions: List<PartCategoryTreeItem> = emptyList(),
    val isParentOptionsLoading: Boolean = false,
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

    // Cached flat list of all categories (for building the parent tree)
    private var allCategories: List<PartCategory> = emptyList()

    val canCreate: Boolean get() = currentUser.isAdmin() || currentUser.hasPermission("create-part-categories")
    val canEdit: Boolean get() = currentUser.isAdmin() || currentUser.hasPermission("edit-part-categories")
    val canDelete: Boolean get() = currentUser.isAdmin() || currentUser.hasPermission("delete-part-categories")

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getPartCategories()) {
                is AuthResult.Success -> {
                    allCategories = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            categories = result.data,
                            fullPathMap = buildFullPathMap(result.data)
                        )
                    }
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

    // ── Path helpers ─────────────────────────────────────────────────────────

    /**
     * Build a map of id → full ancestor path string ("A / B / C") for every
     * category in the flat list, walking parentId links up to any depth.
     */
    private fun buildFullPathMap(items: List<PartCategory>): Map<Int, String> {
        val byId = items.associateBy { it.id }

        fun pathFor(cat: PartCategory): String {
            val parts = mutableListOf<String>()
            var current: PartCategory? = cat
            while (current != null) {
                parts.add(0, current.name)
                current = current.parentId?.let { byId[it] }
            }
            return parts.joinToString(" / ")
        }

        return items.associate { it.id to pathFor(it) }
    }

    // ── Tree helpers ──────────────────────────────────────────────────────────

    /** Depth-first ordered flat list with full-path strings, excluding excludedIds */
    private fun buildTreeList(
        items: List<PartCategory>,
        excludedIds: Set<Int>,
        pathMap: Map<Int, String>,
        parentId: Int? = null
    ): List<PartCategoryTreeItem> {
        val result = mutableListOf<PartCategoryTreeItem>()
        val children = items
            .filter { (it.parentId ?: -1).let { pid -> if (parentId == null) pid == -1 else pid == parentId } &&
                !excludedIds.contains(it.id) }
            .sortedWith(compareBy { it.name })
        for (child in children) {
            result.add(PartCategoryTreeItem(
                id = child.id,
                name = child.name,
                fullPath = pathMap[child.id] ?: child.name
            ))
            result.addAll(buildTreeList(items, excludedIds, pathMap, child.id))
        }
        return result
    }

    private fun getDescendantIds(categoryId: Int, items: List<PartCategory>): Set<Int> {
        val ids = mutableSetOf<Int>()
        val children = items.filter { it.parentId == categoryId }
        for (child in children) {
            ids.add(child.id)
            ids.addAll(getDescendantIds(child.id, items))
        }
        return ids
    }

    private fun buildParentOptions(editingId: Int?): List<PartCategoryTreeItem> {
        val excluded = if (editingId != null) {
            setOf(editingId) + getDescendantIds(editingId, allCategories)
        } else {
            emptySet()
        }
        val pathMap = buildFullPathMap(allCategories)
        return buildTreeList(allCategories, excluded, pathMap)
    }

    // ── Form actions ──────────────────────────────────────────────────────────

    fun onCreateClicked() {
        _uiState.update {
            it.copy(
                showForm = true,
                editingCategory = null,
                formName = "",
                formDescription = "",
                formParentId = null,
                formNameError = null,
                formError = null,
                parentOptions = buildParentOptions(null)
            )
        }
    }

    fun onEditClicked(category: PartCategory) {
        _uiState.update {
            it.copy(
                showForm = true,
                editingCategory = category,
                formName = category.name,
                formDescription = category.description ?: "",
                formParentId = category.parentId,
                formNameError = null,
                formError = null,
                parentOptions = buildParentOptions(category.id)
            )
        }
    }

    fun onFormDismiss() {
        _uiState.update {
            it.copy(showForm = false, editingCategory = null, formNameError = null, formError = null)
        }
    }

    fun onFormNameChange(v: String) = _uiState.update { it.copy(formName = v, formNameError = null) }
    fun onFormDescriptionChange(v: String) = _uiState.update { it.copy(formDescription = v) }
    fun onFormParentSelected(id: Int?) = _uiState.update { it.copy(formParentId = id) }

    fun onFormSave() {
        val state = _uiState.value
        if (state.formName.isBlank()) {
            _uiState.update { it.copy(formNameError = "نام دسته‌بندی الزامی است") }
            return
        }
        val request = PartCategoryRequest(
            name = state.formName.trim(),
            description = state.formDescription.ifBlank { null },
            parentId = state.formParentId
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
