package com.app.viam.ui.warehouse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.viam.R
import com.app.viam.data.model.Row
import com.app.viam.data.model.Shelf
import com.app.viam.data.model.Zone

private sealed class FilterStep {
    data object Zones : FilterStep()
    data class Shelves(val zone: Zone) : FilterStep()
    data class Rows(val zone: Zone, val shelf: Shelf) : FilterStep()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseFilterSheet(
    uiState: WarehouseListUiState,
    onDismiss: () -> Unit,
    onFilterByZone: (zoneId: Int, zoneName: String) -> Unit,
    onFilterByRow: (rowId: Int, rowName: String, shelfName: String, zoneId: Int, zoneName: String) -> Unit,
    onClearFilter: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableStateOf<FilterStep>(FilterStep.Zones) }
    // track direction for slide animation: 1 = forward (drill in), -1 = back
    var direction by remember { mutableIntStateOf(1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally { w -> -direction * w } togetherWith
                        slideOutHorizontally { w -> direction * w }
            },
            label = "filter_step"
        ) { currentStep ->
            FilterPage(
                step = currentStep,
                uiState = uiState,
                onBack = {
                    direction = -1
                    step = when (val s = currentStep) {
                        is FilterStep.Rows -> FilterStep.Shelves(s.zone)
                        else -> FilterStep.Zones
                    }
                },
                onDismiss = onDismiss,
                onClearFilter = {
                    onClearFilter()
                    direction = -1
                    step = FilterStep.Zones
                },
                onSelectZone = { zone ->
                    if (!zone.shelves.isNullOrEmpty()) {
                        direction = 1
                        step = FilterStep.Shelves(zone)
                    } else {
                        onFilterByZone(zone.id, zone.name)
                    }
                },
                onSelectAllInZone = { zone ->
                    onFilterByZone(zone.id, zone.name)
                },
                onSelectShelf = { zone, shelf ->
                    if (!shelf.rows.isNullOrEmpty()) {
                        direction = 1
                        step = FilterStep.Rows(zone, shelf)
                    }
                },
                onSelectRow = { zone, shelf, row ->
                    onFilterByRow(row.id, row.name, shelf.name, zone.id, zone.name)
                }
            )
        }
    }
}

@Composable
private fun FilterPage(
    step: FilterStep,
    uiState: WarehouseListUiState,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onClearFilter: () -> Unit,
    onSelectZone: (Zone) -> Unit,
    onSelectAllInZone: (Zone) -> Unit,
    onSelectShelf: (Zone, Shelf) -> Unit,
    onSelectRow: (Zone, Shelf, Row) -> Unit
) {
    val title = when (step) {
        is FilterStep.Zones -> stringResource(R.string.warehouse_filter_select_zone)
        is FilterStep.Shelves -> stringResource(R.string.warehouse_filter_select_shelf)
        is FilterStep.Rows -> stringResource(R.string.warehouse_filter_select_row)
    }
    val canGoBack = step !is FilterStep.Zones

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canGoBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBackIos,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (uiState.hasActiveFilter) {
                TextButton(onClick = onClearFilter) {
                    Text(
                        text = stringResource(R.string.warehouse_clear_filter),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = null)
            }
        }

        // Breadcrumb strip (shown when past zone level)
        if (step is FilterStep.Shelves || step is FilterStep.Rows) {
            val zoneName = when (step) {
                is FilterStep.Shelves -> step.zone.name
                is FilterStep.Rows -> step.zone.name
                else -> ""
            }
            val shelfName = if (step is FilterStep.Rows) step.shelf.name else null
            BreadcrumbStrip(zoneName = zoneName, shelfName = shelfName)
        }

        HorizontalDivider()

        // Content
        when {
            uiState.isTreeLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            uiState.treeData.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.warehouse_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                when (step) {
                    is FilterStep.Zones -> {
                        val zones = uiState.treeData
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                FilterRow(
                                    label = stringResource(R.string.warehouse_filter_all),
                                    isSelected = !uiState.hasActiveFilter,
                                    hasChildren = false,
                                    onClick = onClearFilter
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            items(zones, key = { it.id }) { zone ->
                                FilterRow(
                                    label = zone.name,
                                    sublabel = stringResource(R.string.warehouse_zone),
                                    isSelected = uiState.activeFilterZoneId == zone.id,
                                    hasChildren = !zone.shelves.isNullOrEmpty(),
                                    onClick = { onSelectZone(zone) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                    is FilterStep.Shelves -> {
                        val shelves = step.zone.shelves.orEmpty()
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                FilterRow(
                                    label = stringResource(R.string.warehouse_filter_all_in_zone, step.zone.name),
                                    isSelected = uiState.activeFilterZoneId == step.zone.id && uiState.activeFilterRowId == null,
                                    hasChildren = false,
                                    onClick = { onSelectAllInZone(step.zone) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            items(shelves, key = { it.id }) { shelf ->
                                FilterRow(
                                    label = shelf.name,
                                    sublabel = stringResource(R.string.warehouse_shelf),
                                    isSelected = false,
                                    hasChildren = !shelf.rows.isNullOrEmpty(),
                                    onClick = { onSelectShelf(step.zone, shelf) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                    is FilterStep.Rows -> {
                        val rows = step.shelf.rows.orEmpty()
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(rows, key = { it.id }) { row ->
                                FilterRow(
                                    label = row.name,
                                    sublabel = if (row.boxesCount != null && row.boxesCount > 0)
                                        "${row.boxesCount} ${stringResource(R.string.warehouse_box_count)}"
                                    else stringResource(R.string.warehouse_row),
                                    isSelected = uiState.activeFilterRowId == row.id,
                                    hasChildren = false,
                                    onClick = { onSelectRow(step.zone, step.shelf, row) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbStrip(
    zoneName: String,
    shelfName: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BreadcrumbChip(text = zoneName, isLast = shelfName == null)
        if (shelfName != null) {
            Text(
                text = " › ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BreadcrumbChip(text = shelfName, isLast = true)
        }
    }
}

@Composable
private fun BreadcrumbChip(text: String, isLast: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isLast) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
            color = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FilterRow(
    label: String,
    sublabel: String = "",
    isSelected: Boolean,
    hasChildren: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection dot / check
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            if (sublabel.isNotEmpty()) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (hasChildren) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
