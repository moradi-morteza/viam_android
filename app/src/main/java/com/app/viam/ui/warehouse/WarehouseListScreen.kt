package com.app.viam.ui.warehouse

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.viam.R
import com.app.viam.data.model.Box
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseListScreen(
    viewModel: WarehouseListViewModel,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.navigateToDetail) {
        uiState.navigateToDetail?.let { boxId ->
            viewModel.onDetailNavigated()
            onNavigateToDetail(boxId)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorDismissed()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
            .distinctUntilChanged()
            .collect { nearEnd -> if (nearEnd) viewModel.loadNextPage() }
    }

    if (uiState.showFilterSheet) {
        WarehouseFilterSheet(
            uiState = uiState,
            onDismiss = viewModel::onFilterDismissed,
            onFilterByZone = viewModel::onFilterByZone,
            onFilterByRow = viewModel::onFilterByRow,
            onClearFilter = viewModel::onClearFilter
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButtonPosition = FabPosition.Start,
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onFilterClicked,
                containerColor = if (uiState.hasActiveFilter)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = stringResource(R.string.warehouse_filter),
                    tint = if (uiState.hasActiveFilter)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {

            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChange,
                placeholder = { Text(stringResource(R.string.warehouse_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Active filter info card
            if (uiState.hasActiveFilter) {
                ActiveFilterCard(
                    uiState = uiState,
                    onClear = viewModel::onClearFilter,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                )
            }

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        uiState.boxes.isEmpty() -> {
                            Text(
                                text = stringResource(R.string.warehouse_empty),
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp, end = 16.dp,
                                    top = 4.dp, bottom = 88.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.boxes, key = { it.id }) { box ->
                                    BoxCard(
                                        box = box,
                                        onClick = { viewModel.onBoxClicked(box.id) }
                                    )
                                }

                                if (uiState.isLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                        }
                                    }
                                }

                                if (!uiState.canLoadMore && !uiState.isLoadingMore && uiState.total > 0) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "مجموع ${uiState.total} جعبه",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterCard(
    uiState: WarehouseListUiState,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Build breadcrumb: ناحیه › قفسه › ردیف  (only present parts)
    val parts = buildList {
        uiState.activeFilterZoneName?.let { add(it) }
        if (uiState.activeFilterRowId != null) {
            uiState.activeFilterShelfName?.let { add(it) }
            uiState.activeFilterRowName?.let { add(it) }
        }
    }
    val breadcrumb = parts.joinToString(" › ")

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = breadcrumb,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.warehouse_clear_filter),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun BoxCard(
    box: Box,
    onClick: () -> Unit
) {
    val locationParts = mutableListOf<String>()
    box.row?.shelf?.zone?.name?.let { locationParts.add(it) }
    box.row?.shelf?.name?.let { locationParts.add(it) }
    box.row?.name?.let { locationParts.add(it) }
    val locationText = locationParts.joinToString(" › ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = box.code,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (box.part != null) {
                    Text(
                        text = box.part.name + if (!box.part.sku.isNullOrBlank()) " (${box.part.sku})" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.warehouse_no_part),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (locationText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = locationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (box.quantity == box.quantity.toLong().toDouble())
                               box.quantity.toLong().toString()
                           else box.quantity.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (box.quantity > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.warehouse_quantity),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
