package com.app.viam.ui.warehouse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.viam.R
import com.app.viam.data.model.Row
import com.app.viam.data.model.Shelf
import com.app.viam.data.model.Zone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseStructureScreen(
    viewModel: WarehouseStructureViewModel,
    canCreateZones: Boolean,
    canEditZones: Boolean,
    canDeleteZones: Boolean,
    canCreateShelves: Boolean,
    canEditShelves: Boolean,
    canDeleteShelves: Boolean,
    canCreateRows: Boolean,
    canEditRows: Boolean,
    canDeleteRows: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Active dialog
    uiState.dialog?.let { dialog ->
        when (dialog) {
            is StructureDialog.AddZone, is StructureDialog.EditZone ->
                ZoneDialog(
                    initial = (dialog as? StructureDialog.EditZone)?.zone,
                    isSaving = uiState.isSaving,
                    error = uiState.dialogError,
                    onDismiss = viewModel::closeDialog,
                    onSave = { name, desc -> viewModel.saveZone(name, desc) }
                )
            is StructureDialog.DeleteZone ->
                DeleteDialog(
                    title = stringResource(R.string.structure_delete_zone_title),
                    message = stringResource(R.string.structure_delete_zone_msg, dialog.zone.name),
                    error = uiState.dialogError,
                    isSaving = uiState.isSaving,
                    onDismiss = viewModel::closeDialog,
                    onConfirm = { viewModel.deleteZone(dialog.zone) }
                )
            is StructureDialog.AddShelf, is StructureDialog.EditShelf ->
                ShelfDialog(
                    initial = (dialog as? StructureDialog.EditShelf)?.shelf,
                    preselectedZone = when (dialog) {
                        is StructureDialog.AddShelf -> dialog.zone
                        is StructureDialog.EditShelf -> dialog.shelf.zone
                        else -> null
                    },
                    zones = uiState.zones,
                    isSaving = uiState.isSaving,
                    error = uiState.dialogError,
                    onDismiss = viewModel::closeDialog,
                    onSave = { zoneId, name, desc -> viewModel.saveShelf(zoneId, name, desc) }
                )
            is StructureDialog.DeleteShelf ->
                DeleteDialog(
                    title = stringResource(R.string.structure_delete_shelf_title),
                    message = stringResource(R.string.structure_delete_shelf_msg, dialog.shelf.name),
                    error = uiState.dialogError,
                    isSaving = uiState.isSaving,
                    onDismiss = viewModel::closeDialog,
                    onConfirm = { viewModel.deleteShelf(dialog.shelf) }
                )
            is StructureDialog.AddRow, is StructureDialog.EditRow ->
                RowDialog(
                    initial = (dialog as? StructureDialog.EditRow)?.row,
                    preselectedShelf = when (dialog) {
                        is StructureDialog.AddRow -> dialog.shelf
                        is StructureDialog.EditRow -> dialog.row.shelf
                        else -> null
                    },
                    zones = uiState.zones,
                    shelvesForPicker = uiState.shelvesForPicker,
                    isLoadingShelves = uiState.isLoadingShelvesForPicker,
                    isSaving = uiState.isSaving,
                    error = uiState.dialogError,
                    onZoneSelected = { viewModel.loadShelvesForZone(it) },
                    onDismiss = viewModel::closeDialog,
                    onSave = { shelfId, name, desc -> viewModel.saveRow(shelfId, name, desc) }
                )
            is StructureDialog.DeleteRow ->
                DeleteDialog(
                    title = stringResource(R.string.structure_delete_row_title),
                    message = stringResource(R.string.structure_delete_row_msg, dialog.row.name),
                    error = uiState.dialogError,
                    isSaving = uiState.isSaving,
                    onDismiss = viewModel::closeDialog,
                    onConfirm = { viewModel.deleteRow(dialog.row) }
                )
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (canCreateZones) {
                FloatingActionButton(onClick = { viewModel.openDialog(StructureDialog.AddZone) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.structure_add_zone))
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::load,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.zones.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.zones.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
                uiState.zones.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.structure_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.zones, key = { it.id }) { zone ->
                            ZoneCard(
                                zone = zone,
                                isExpanded = zone.id in uiState.expandedZoneIds,
                                expandedShelfIds = uiState.expandedShelfIds,
                                canEditZone = canEditZones,
                                canDeleteZone = canDeleteZones,
                                canCreateShelf = canCreateShelves,
                                canEditShelf = canEditShelves,
                                canDeleteShelf = canDeleteShelves,
                                canCreateRow = canCreateRows,
                                canEditRow = canEditRows,
                                canDeleteRow = canDeleteRows,
                                onToggle = { viewModel.toggleZone(zone.id) },
                                onToggleShelf = { viewModel.toggleShelf(it) },
                                onAddShelf = { viewModel.openDialog(StructureDialog.AddShelf(zone)) },
                                onEditZone = { viewModel.openDialog(StructureDialog.EditZone(zone)) },
                                onDeleteZone = { viewModel.openDialog(StructureDialog.DeleteZone(zone)) },
                                onEditShelf = { viewModel.openDialog(StructureDialog.EditShelf(it)) },
                                onDeleteShelf = { viewModel.openDialog(StructureDialog.DeleteShelf(it)) },
                                onAddRow = { shelf -> viewModel.openDialog(StructureDialog.AddRow(shelf)) },
                                onEditRow = { viewModel.openDialog(StructureDialog.EditRow(it)) },
                                onDeleteRow = { viewModel.openDialog(StructureDialog.DeleteRow(it)) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ── Zone card ──────────────────────────────────────────────────────────────────

@Composable
private fun ZoneCard(
    zone: Zone,
    isExpanded: Boolean,
    expandedShelfIds: Set<Int>,
    canEditZone: Boolean,
    canDeleteZone: Boolean,
    canCreateShelf: Boolean,
    canEditShelf: Boolean,
    canDeleteShelf: Boolean,
    canCreateRow: Boolean,
    canEditRow: Boolean,
    canDeleteRow: Boolean,
    onToggle: () -> Unit,
    onToggleShelf: (Int) -> Unit,
    onAddShelf: () -> Unit,
    onEditZone: () -> Unit,
    onDeleteZone: () -> Unit,
    onEditShelf: (Shelf) -> Unit,
    onDeleteShelf: (Shelf) -> Unit,
    onAddRow: (Shelf) -> Unit,
    onEditRow: (Row) -> Unit,
    onDeleteRow: (Row) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Zone header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(start = 12.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Arrow on left
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (isExpanded) 0f else -90f)
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Warehouse,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        zone.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!zone.description.isNullOrBlank()) {
                        Text(
                            zone.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val shelfCount = zone.shelves?.size ?: 0
                if (shelfCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text("$shelfCount", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                // ⋮ menu on right
                if (canEditZone || canDeleteZone || canCreateShelf) {
                    OverflowMenu(
                        addLabel = if (canCreateShelf) stringResource(R.string.structure_add_shelf) else null,
                        canEdit = canEditZone,
                        canDelete = canDeleteZone,
                        onAdd = onAddShelf,
                        onEdit = onEditZone,
                        onDelete = onDeleteZone
                    )
                }
            }

            // Expanded shelves
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    val shelves = zone.shelves.orEmpty()
                    if (shelves.isEmpty()) {
                        Text(
                            stringResource(R.string.structure_no_shelves),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 52.dp, top = 12.dp, bottom = 12.dp)
                        )
                    } else {
                        shelves.forEach { shelf ->
                            ShelfRow(
                                shelf = shelf,
                                isExpanded = shelf.id in expandedShelfIds,
                                canEditShelf = canEditShelf,
                                canDeleteShelf = canDeleteShelf,
                                canCreateRow = canCreateRow,
                                canEditRow = canEditRow,
                                canDeleteRow = canDeleteRow,
                                onToggle = { onToggleShelf(shelf.id) },
                                onEdit = { onEditShelf(shelf) },
                                onDelete = { onDeleteShelf(shelf) },
                                onAddRow = { onAddRow(shelf) },
                                onEditRow = onEditRow,
                                onDeleteRow = onDeleteRow
                            )
                        }
                    }
                    if (canCreateShelf) {
                        TextButton(
                            onClick = onAddShelf,
                            modifier = Modifier.padding(start = 36.dp, bottom = 6.dp, top = 2.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.structure_add_shelf),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Shelf row ──────────────────────────────────────────────────────────────────

@Composable
private fun ShelfRow(
    shelf: Shelf,
    isExpanded: Boolean,
    canEditShelf: Boolean,
    canDeleteShelf: Boolean,
    canCreateRow: Boolean,
    canEditRow: Boolean,
    canDeleteRow: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddRow: () -> Unit,
    onEditRow: (Row) -> Unit,
    onDeleteRow: (Row) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(start = 20.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Arrow on left
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (isExpanded) 0f else -90f)
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Filled.ViewModule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    shelf.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!shelf.description.isNullOrBlank()) {
                    Text(
                        shelf.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val rowCount = shelf.rows?.size ?: 0
            if (rowCount > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("$rowCount", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.width(4.dp))
            }
            // ⋮ menu on right
            if (canEditShelf || canDeleteShelf || canCreateRow) {
                OverflowMenu(
                    addLabel = if (canCreateRow) stringResource(R.string.structure_add_row) else null,
                    canEdit = canEditShelf,
                    canDelete = canDeleteShelf,
                    onAdd = onAddRow,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                val rows = shelf.rows.orEmpty()
                if (rows.isEmpty()) {
                    Text(
                        stringResource(R.string.structure_no_rows),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 68.dp, top = 8.dp, bottom = 8.dp)
                    )
                } else {
                    rows.forEach { row ->
                        RowItem(
                            row = row,
                            canEditRow = canEditRow,
                            canDeleteRow = canDeleteRow,
                            onEdit = { onEditRow(row) },
                            onDelete = { onDeleteRow(row) }
                        )
                    }
                }
                if (canCreateRow) {
                    TextButton(
                        onClick = onAddRow,
                        modifier = Modifier.padding(start = 52.dp, bottom = 6.dp, top = 2.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.structure_add_row),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

// ── Row item ───────────────────────────────────────────────────────────────────

@Composable
private fun RowItem(
    row: Row,
    canEditRow: Boolean,
    canDeleteRow: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 54.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Filled.TableRows,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            if (!row.description.isNullOrBlank()) {
                Text(
                    row.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val boxCount = row.boxesCount
        if (boxCount != null && boxCount > 0) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    "$boxCount جعبه",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        // ⋮ menu on right (no "Add" sub-item for rows)
        if (canEditRow || canDeleteRow) {
            OverflowMenu(
                addLabel = null,
                canEdit = canEditRow,
                canDelete = canDeleteRow,
                onAdd = {},
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

// ── Overflow (⋮) menu ──────────────────────────────────────────────────────────

@Composable
private fun OverflowMenu(
    addLabel: String?,
    canEdit: Boolean,
    canDelete: Boolean,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (addLabel != null) {
                DropdownMenuItem(
                    text = { Text(addLabel) },
                    leadingIcon = { Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp)) },
                    onClick = { expanded = false; onAdd() }
                )
            }
            if (canEdit) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit)) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Edit,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { expanded = false; onEdit() }
                )
            }
            if (canDelete) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = { expanded = false; onDelete() }
                )
            }
        }
    }
}

// ── Dialogs ────────────────────────────────────────────────────────────────────

@Composable
private fun ZoneDialog(
    initial: Zone?,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) stringResource(R.string.structure_add_zone)
                else stringResource(R.string.structure_edit_zone)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.form_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.parts_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, description.ifBlank { null }) },
                enabled = name.isNotBlank() && !isSaving
            ) {
                Text(if (isSaving) stringResource(R.string.loading) else stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShelfDialog(
    initial: Shelf?,
    preselectedZone: Zone?,
    zones: List<Zone>,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (zoneId: Int, name: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var selectedZone by remember { mutableStateOf(preselectedZone ?: zones.firstOrNull()) }
    var zoneExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) stringResource(R.string.structure_add_shelf)
                else stringResource(R.string.structure_edit_shelf)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                ExposedDropdownMenuBox(expanded = zoneExpanded, onExpandedChange = { zoneExpanded = it }) {
                    OutlinedTextField(
                        value = selectedZone?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.warehouse_zone)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = zoneExpanded, onDismissRequest = { zoneExpanded = false }) {
                        zones.forEach { zone ->
                            DropdownMenuItem(
                                text = { Text(zone.name) },
                                onClick = { selectedZone = zone; zoneExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.form_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text(stringResource(R.string.parts_description)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedZone?.let { onSave(it.id, name, description.ifBlank { null }) } },
                enabled = name.isNotBlank() && selectedZone != null && !isSaving
            ) {
                Text(if (isSaving) stringResource(R.string.loading) else stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowDialog(
    initial: Row?,
    preselectedShelf: Shelf?,
    zones: List<Zone>,
    shelvesForPicker: List<Shelf>,
    isLoadingShelves: Boolean,
    isSaving: Boolean,
    error: String?,
    onZoneSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: (shelfId: Int, name: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    val initialZone = preselectedShelf?.zone
    var selectedZone by remember { mutableStateOf(initialZone) }
    var selectedShelf by remember { mutableStateOf(preselectedShelf) }
    var zoneExpanded by remember { mutableStateOf(false) }
    var shelfExpanded by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(selectedZone?.id) {
        selectedZone?.let { onZoneSelected(it.id) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) stringResource(R.string.structure_add_row)
                else stringResource(R.string.structure_edit_row)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                ExposedDropdownMenuBox(expanded = zoneExpanded, onExpandedChange = { zoneExpanded = it }) {
                    OutlinedTextField(
                        value = selectedZone?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.warehouse_zone)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = zoneExpanded, onDismissRequest = { zoneExpanded = false }) {
                        zones.forEach { zone ->
                            DropdownMenuItem(
                                text = { Text(zone.name) },
                                onClick = {
                                    selectedZone = zone
                                    selectedShelf = null
                                    zoneExpanded = false
                                    onZoneSelected(zone.id)
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = shelfExpanded, onExpandedChange = { shelfExpanded = it }) {
                    OutlinedTextField(
                        value = if (isLoadingShelves) stringResource(R.string.loading)
                                else selectedShelf?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.warehouse_shelf)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shelfExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        enabled = selectedZone != null && !isLoadingShelves
                    )
                    ExposedDropdownMenu(expanded = shelfExpanded, onDismissRequest = { shelfExpanded = false }) {
                        shelvesForPicker.forEach { shelf ->
                            DropdownMenuItem(
                                text = { Text(shelf.name) },
                                onClick = { selectedShelf = shelf; shelfExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.form_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text(stringResource(R.string.parts_description)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedShelf?.let { onSave(it.id, name, description.ifBlank { null }) } },
                enabled = name.isNotBlank() && selectedShelf != null && !isSaving
            ) {
                Text(if (isSaving) stringResource(R.string.loading) else stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun DeleteDialog(
    title: String,
    message: String,
    error: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message)
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSaving) {
                Text(
                    if (isSaving) stringResource(R.string.loading) else stringResource(R.string.delete),
                    color = if (!isSaving) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text(stringResource(R.string.cancel)) }
        }
    )
}
