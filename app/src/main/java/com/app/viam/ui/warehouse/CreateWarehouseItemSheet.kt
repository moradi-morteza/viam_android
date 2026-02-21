package com.app.viam.ui.warehouse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.viam.R
import com.app.viam.data.model.Part
import com.app.viam.data.model.Row
import com.app.viam.data.model.Shelf
import com.app.viam.data.model.Zone
import com.app.viam.ui.common.PartPickerSheet

/** Which item type the user wants to create */
enum class CreateItemType {
    ZONE, SHELF, ROW, BOX
}

/**
 * Reusable bottom sheet for creating a warehouse item (Zone / Shelf / Row / Box).
 *
 * The sheet adapts its fields based on [initialType].
 * Cascading dropdowns: Zone → Shelf → Row (loaded via callbacks to ViewModel).
 *
 * @param initialType          Which tab is pre-selected.
 * @param zones                Available zones (for Shelf/Row/Box selectors).
 * @param shelvesForZone       Shelves filtered by selected zone (for Row/Box selectors).
 * @param rowsForShelf         Rows filtered by selected shelf (for Box selector).
 * @param isLoadingShelves     Whether shelves are being fetched.
 * @param isLoadingRows        Whether rows are being fetched.
 * @param isSubmitting         Whether the create call is in flight.
 * @param error                Server error to display.
 * @param onZoneSelected       Called when user picks a zone — ViewModel loads shelves.
 * @param onShelfSelected      Called when user picks a shelf — ViewModel loads rows.
 * @param onDismiss            Called on close without submit.
 * @param onCreateZone         name, description
 * @param onCreateShelf        zoneId, name, description
 * @param onCreateRow          shelfId, name, description
 * @param onCreateBox          rowId, code, partId, description
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWarehouseItemSheet(
    initialType: CreateItemType = CreateItemType.ZONE,
    zones: List<Zone> = emptyList(),
    shelvesForZone: List<Shelf> = emptyList(),
    rowsForShelf: List<Row> = emptyList(),
    isLoadingShelves: Boolean = false,
    isLoadingRows: Boolean = false,
    isSubmitting: Boolean,
    error: String?,
    onZoneSelected: (Zone) -> Unit,
    onShelfSelected: (Shelf) -> Unit,
    onDismiss: () -> Unit,
    onCreateZone: (name: String, description: String?) -> Unit,
    onCreateShelf: (zoneId: Int, name: String, description: String?) -> Unit,
    onCreateRow: (shelfId: Int, name: String, description: String?) -> Unit,
    onCreateBox: (rowId: Int, code: String, partId: Int?, description: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember { mutableStateOf(initialType) }

    // Shared fields
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Cascading selections
    var selectedZone by remember { mutableStateOf<Zone?>(null) }
    var selectedShelf by remember { mutableStateOf<Shelf?>(null) }
    var selectedRow by remember { mutableStateOf<Row?>(null) }

    // Box-specific
    var boxCode by remember { mutableStateOf("") }
    var selectedPart by remember { mutableStateOf<Part?>(null) }
    var showPartPicker by remember { mutableStateOf(false) }

    if (showPartPicker) {
        PartPickerSheet(
            onDismiss = { showPartPicker = false },
            onSelect = { part ->
                selectedPart = part
                showPartPicker = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .imePadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.warehouse_create_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Type selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreateItemType.entries.forEach { type ->
                    val label = when (type) {
                        CreateItemType.ZONE -> stringResource(R.string.warehouse_zone)
                        CreateItemType.SHELF -> stringResource(R.string.warehouse_shelf)
                        CreateItemType.ROW -> stringResource(R.string.warehouse_row)
                        CreateItemType.BOX -> stringResource(R.string.warehouse_box_code_short)
                    }
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            name = ""
                            description = ""
                            boxCode = ""
                            selectedPart = null
                        },
                        label = {
                            Text(
                                label,
                                fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Error banner
            AnimatedVisibility(
                visible = error != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // --- Cascading dropdowns (SHELF needs zone, ROW needs zone+shelf, BOX needs zone+shelf+row) ---
            if (selectedType == CreateItemType.SHELF ||
                selectedType == CreateItemType.ROW ||
                selectedType == CreateItemType.BOX
            ) {
                FormLabel(stringResource(R.string.warehouse_zone) + " *")
                Spacer(modifier = Modifier.height(6.dp))
                ZoneDropdown(
                    zones = zones,
                    selected = selectedZone,
                    onSelect = { zone ->
                        selectedZone = zone
                        selectedShelf = null
                        selectedRow = null
                        onZoneSelected(zone)
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (selectedType == CreateItemType.ROW || selectedType == CreateItemType.BOX) {
                FormLabel(stringResource(R.string.warehouse_shelf) + " *")
                Spacer(modifier = Modifier.height(6.dp))
                ShelfDropdown(
                    shelves = shelvesForZone,
                    selected = selectedShelf,
                    enabled = selectedZone != null && !isLoadingShelves,
                    isLoading = isLoadingShelves,
                    onSelect = { shelf ->
                        selectedShelf = shelf
                        selectedRow = null
                        onShelfSelected(shelf)
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (selectedType == CreateItemType.BOX) {
                FormLabel(stringResource(R.string.warehouse_row) + " *")
                Spacer(modifier = Modifier.height(6.dp))
                RowDropdown(
                    rows = rowsForShelf,
                    selected = selectedRow,
                    enabled = selectedShelf != null && !isLoadingRows,
                    isLoading = isLoadingRows,
                    onSelect = { row -> selectedRow = row }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // --- Name field (all types) or Code field (BOX) ---
            if (selectedType == CreateItemType.BOX) {
                FormLabel(stringResource(R.string.warehouse_box_code) + " *")
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = boxCode,
                    onValueChange = { boxCode = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.warehouse_box_code_hint)) }
                )
            } else {
                FormLabel(stringResource(R.string.form_name) + " *")
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Part picker (BOX only) ---
            if (selectedType == CreateItemType.BOX) {
                FormLabel(stringResource(R.string.parts_name))
                Spacer(modifier = Modifier.height(6.dp))
                PartPickerField(
                    selectedPart = selectedPart,
                    onPickerOpen = { showPartPicker = true },
                    onClear = { selectedPart = null }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // --- Description ---
            FormLabel(stringResource(R.string.parts_description))
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                placeholder = { Text(stringResource(R.string.transaction_description_hint)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Action buttons ---
            val isValid = when (selectedType) {
                CreateItemType.ZONE -> name.isNotBlank()
                CreateItemType.SHELF -> selectedZone != null && name.isNotBlank()
                CreateItemType.ROW -> selectedZone != null && selectedShelf != null && name.isNotBlank()
                CreateItemType.BOX -> selectedZone != null && selectedShelf != null && selectedRow != null && boxCode.isNotBlank()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        val desc = description.ifBlank { null }
                        when (selectedType) {
                            CreateItemType.ZONE -> onCreateZone(name.trim(), desc)
                            CreateItemType.SHELF -> selectedZone?.let { z ->
                                onCreateShelf(z.id, name.trim(), desc)
                            }
                            CreateItemType.ROW -> selectedShelf?.let { s ->
                                onCreateRow(s.id, name.trim(), desc)
                            }
                            CreateItemType.BOX -> selectedRow?.let { r ->
                                onCreateBox(r.id, boxCode.trim(), selectedPart?.id, desc)
                            }
                        }
                    },
                    modifier = Modifier.weight(2f),
                    enabled = isValid && !isSubmitting
                ) {
                    Text(
                        if (isSubmitting) stringResource(R.string.transaction_saving)
                        else stringResource(R.string.save)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneDropdown(
    zones: List<Zone>,
    selected: Zone?,
    onSelect: (Zone) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(stringResource(R.string.warehouse_select_zone)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            zones.forEach { zone ->
                DropdownMenuItem(
                    text = { Text(zone.name) },
                    onClick = {
                        onSelect(zone)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShelfDropdown(
    shelves: List<Shelf>,
    selected: Shelf?,
    enabled: Boolean,
    isLoading: Boolean,
    onSelect: (Shelf) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = when {
                isLoading -> stringResource(R.string.loading)
                else -> selected?.name ?: ""
            },
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            placeholder = { Text(stringResource(R.string.warehouse_select_shelf)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            shelves.forEach { shelf ->
                DropdownMenuItem(
                    text = { Text(shelf.name) },
                    onClick = {
                        onSelect(shelf)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowDropdown(
    rows: List<Row>,
    selected: Row?,
    enabled: Boolean,
    isLoading: Boolean,
    onSelect: (Row) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = when {
                isLoading -> stringResource(R.string.loading)
                else -> selected?.name ?: ""
            },
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            placeholder = { Text(stringResource(R.string.warehouse_select_row)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            rows.forEach { row ->
                DropdownMenuItem(
                    text = { Text(row.name) },
                    onClick = {
                        onSelect(row)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PartPickerField(
    selectedPart: Part?,
    onPickerOpen: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPickerOpen() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (selectedPart != null) {
                    Text(
                        text = selectedPart.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (!selectedPart.sku.isNullOrBlank()) {
                        Text(
                            text = selectedPart.sku,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.part_picker_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selectedPart != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
