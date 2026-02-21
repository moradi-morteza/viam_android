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
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
                            ?: uiState.zones.find { it.id == dialog.shelf.zoneId }
                        else -> null
                    },
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
            is StructureDialog.AddRow, is StructureDialog.EditRow -> {
                // Resolve shelf with its parent zone, falling back to uiState.zones tree
                val resolvedShelf = when (dialog) {
                    is StructureDialog.AddRow -> dialog.shelf.let { shelf ->
                        if (shelf.zone != null) shelf
                        else shelf.copy(zone = uiState.zones.find { it.id == shelf.zoneId })
                    }
                    is StructureDialog.EditRow -> dialog.row.shelf?.let { shelf ->
                        if (shelf.zone != null) shelf
                        else shelf.copy(zone = uiState.zones.find { it.id == shelf.zoneId })
                    } ?: uiState.zones.flatMap { it.shelves.orEmpty() }
                        .find { it.id == dialog.row.shelfId }
                        ?.let { shelf -> shelf.copy(zone = uiState.zones.find { it.id == shelf.zoneId }) }
                    else -> null
                }
                RowDialog(
                    initial = (dialog as? StructureDialog.EditRow)?.row,
                    preselectedShelf = resolvedShelf,
                    isSaving = uiState.isSaving,
                    error = uiState.dialogError,
                    onDismiss = viewModel::closeDialog,
                    onSave = { shelfId, name, desc -> viewModel.saveRow(shelfId, name, desc) }
                )
            }
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
        floatingActionButtonPosition = FabPosition.Start,
        floatingActionButton = {
            if (canCreateZones) {
                FloatingActionButton(onClick = { viewModel.openDialog(StructureDialog.AddZone) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.structure_add_zone))
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.zones.isNotEmpty(),
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

// ── Context label (read-only, shown instead of a dropdown when value is pre-known) ──

@Composable
private fun ContextLabel(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
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
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (zoneId: Int, name: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    // Zone is always fixed — either from context (add) or from the shelf being edited
    val fixedZone = preselectedZone ?: initial?.zone

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
                if (fixedZone != null) {
                    ContextLabel(
                        label = stringResource(R.string.warehouse_zone),
                        value = fixedZone.name
                    )
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
                onClick = { fixedZone?.let { onSave(it.id, name, description.ifBlank { null }) } },
                enabled = name.isNotBlank() && fixedZone != null && !isSaving
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
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (shelfId: Int, name: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    // Shelf (and its zone) are always fixed — either from add context or from the row being edited
    val fixedShelf = preselectedShelf

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
                if (fixedShelf != null) {
                    ContextLabel(
                        label = stringResource(R.string.warehouse_zone),
                        value = fixedShelf.zone?.name ?: ""
                    )
                    ContextLabel(
                        label = stringResource(R.string.warehouse_shelf),
                        value = fixedShelf.name
                    )
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
                onClick = { fixedShelf?.let { onSave(it.id, name, description.ifBlank { null }) } },
                enabled = name.isNotBlank() && fixedShelf != null && !isSaving
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
